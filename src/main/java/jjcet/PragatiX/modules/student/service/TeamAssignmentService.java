package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.repository.*;
import jjcet.PragatiX.enums.TeamRole;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class TeamAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(TeamAssignmentService.class);

    private final StudentRepository studentRepository;
    private final TeamRepository teamRepository;
    private final StageTeamRepository stageTeamRepository;
    private final jjcet.PragatiX.admin.service.LeadershipSyncService leadershipSyncService;

    @PersistenceContext
    private EntityManager entityManager;

    public TeamAssignmentService(StudentRepository studentRepository,
            TeamRepository teamRepository,
            StageTeamRepository stageTeamRepository,
            jjcet.PragatiX.admin.service.LeadershipSyncService leadershipSyncService) {
        this.studentRepository = studentRepository;
        this.teamRepository = teamRepository;
        this.stageTeamRepository = stageTeamRepository;
        this.leadershipSyncService = leadershipSyncService;
    }

    @Transactional
    public void assignTeamOnPromotion(Student student, ActivityStage nextStage) {
        if (student == null || nextStage == null) {
            return;
        }

        int stageOrder = nextStage.getDisplayOrder();
        log.info("TEAM ASSIGNMENT: Processing progression for student {} to Stage (displayOrder={})",
                student.getRegNo(), stageOrder);

        // STAGE 1 — MANUAL TEAM CREATION ONLY
        if (stageOrder <= 1) {
            log.info("Stage 1 (displayOrder=1) is strictly MANUAL team creation. Skipping auto-assignment for student {}",
                    student.getRegNo());
            return;
        }

        // STAGE 2 — THRESHOLD-CROSSING ORDER + ZIGZAG
        if (stageOrder == 2) {
            handleStage2ThresholdZigzagAssignment(student, nextStage);
            return;
        }

        // STAGE 3+ — INHERIT STAGE 2 TEAM LINEAGE
        if (stageOrder >= 3) {
            handleStage3LineageAssignment(student, nextStage);
        }
    }

    /**
     * STAGE 2 — Automatic Deterministic Zigzag Assignment based on Persisted Threshold Sequence
     */
    private void handleStage2ThresholdZigzagAssignment(Student student, ActivityStage nextStage) {
        Long deptId = student.getDepartment() != null ? student.getDepartment().getId() : null;
        Long secId = student.getSection() != null ? student.getSection().getId() : null;
        String yearStr = Team.resolveCanonicalYearOfStudy(student.getYear());

        if (deptId == null || yearStr == null) {
            log.warn("Cannot perform Stage 2 team assignment for student {}: Missing department or year",
                    student.getRegNo());
            return;
        }

        // Synchronize on section/class monitor to ensure atomic monotonic sequence generation
        String lockKey = (deptId + "_" + (secId != null ? secId : 0) + "_" + yearStr).intern();
        synchronized (lockKey) {
            // Check if student already has a Stage 2 sequence and assigned Stage 2 team (Idempotency)
            if (student.getPromotionOrder() != null && student.getTeam() != null) {
                StageTeam existingSt = stageTeamRepository.findByStageIdAndTeamId(nextStage.getId(), student.getTeam().getId()).orElse(null);
                if (existingSt != null) {
                    log.info("Student {} already assigned to Stage 2 Team {} with sequence {}. Idempotent skip.",
                            student.getRegNo(), student.getTeam().getName(), student.getPromotionOrder());
                    return;
                }
            }

            // 1. Persist atomic, monotonic threshold-crossing sequence for Stage 2
            if (student.getPromotionOrder() == null) {
                int nextSeq = studentRepository.findMaxPromotionOrderByClass(deptId, secId, yearStr, 2) + 1;
                student.setPromotionOrder(nextSeq);
                student.setPromotionTimestamp(LocalDateTime.now());
                student = studentRepository.save(student);
                log.info("STAGE 2 SEQUENCE: Assigned persistent sequence #{} to student {}", nextSeq, student.getRegNo());
            }

            int sequenceNumber = student.getPromotionOrder();

            // 2. Retrieve or create the 6 Stage 2 Teams for this section/class
            List<Team> stage2Teams = getOrCreateStage2Teams(nextStage, student, deptId, secId, yearStr);
            if (stage2Teams.size() < 6) {
                log.error("Failed to resolve 6 Stage 2 teams for dept={}, sec={}, year={}", deptId, secId, yearStr);
                return;
            }

            // 3. Apply exact Zigzag / Snake formula based on sequenceNumber (1-based)
            int idx = sequenceNumber - 1; // 0-based
            int block = idx / 6;
            int pos = idx % 6;

            TeamRole role;
            if (block == 0) {
                role = TeamRole.CAPTAIN;
            } else if (block == 1) {
                role = TeamRole.VICE_CAPTAIN;
            } else {
                role = TeamRole.MEMBER;
            }

            int teamIndex;
            if (block % 2 == 0) {
                // Forward (Teams 0..5 -> Team 1..6)
                teamIndex = pos;
            } else {
                // Backward (Teams 5..0 -> Team 6..1)
                teamIndex = 5 - pos;
            }

            Team targetTeam = stage2Teams.get(teamIndex);
            StageTeam stageTeam = stageTeamRepository.findByStageIdAndTeamId(nextStage.getId(), targetTeam.getId()).orElse(null);

            // 4. Assign student to target team in Stage 2
            student.setTeam(targetTeam);
            if (targetTeam.getMembers() == null) {
                targetTeam.setMembers(new java.util.HashSet<>());
            }
            targetTeam.getMembers().add(student);

            if (role == TeamRole.CAPTAIN) {
                targetTeam.setCaptain(student);
                if (stageTeam != null) {
                    stageTeam.setCaptain(student);
                }
                leadershipSyncService.syncLeadership(targetTeam, student, targetTeam.getViceCaptain());
            } else if (role == TeamRole.VICE_CAPTAIN) {
                targetTeam.setViceCaptain(student);
                if (stageTeam != null) {
                    stageTeam.setViceCaptain(student);
                }
                leadershipSyncService.syncLeadership(targetTeam, targetTeam.getCaptain(), student);
            }

            teamRepository.save(targetTeam);
            if (stageTeam != null) {
                stageTeamRepository.save(stageTeam);
            }
            studentRepository.save(student);

            // Sync team_members table
            if (entityManager != null) {
                try {
                    entityManager.createNativeQuery(
                            "INSERT INTO team_members (team_id, student_id) VALUES (:tid, :sid) " +
                                    "ON DUPLICATE KEY UPDATE team_id = :tid")
                            .setParameter("tid", targetTeam.getId())
                            .setParameter("sid", student.getId())
                            .executeUpdate();
                } catch (Exception ignored) {
                }
            }

            log.info("STAGE 2 ASSIGNMENT: Student {} (Seq #{}) -> Team '{}' as {}",
                    student.getRegNo(), sequenceNumber, targetTeam.getName(), role);
        }
    }

    /**
     * Resolves or creates the 6 Stage 2 teams for a class, using stable StageTeam links.
     */
    private List<Team> getOrCreateStage2Teams(ActivityStage stage, Student student, Long deptId, Long secId, String yearStr) {
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            char teamLetter = (char) ('A' + i);
            String teamName = stage.getStageName() + " - Team " + teamLetter;

            Team team = findNextStageTeam(teamName, deptId, secId, yearStr);
            if (team == null) {
                team = new Team();
                team.setName(teamName);
                team.setSize(10);
                team.setDepartment(student.getDepartment());
                team.setSection(student.getSection());
                team.setYear(yearStr);
                team = teamRepository.save(team);
            }

            StageTeam st = stageTeamRepository.findByStageIdAndTeamId(stage.getId(), team.getId()).orElse(null);
            if (st == null) {
                st = new StageTeam();
                st.setStage(stage);
                st.setTeam(team);
                stageTeamRepository.save(st);
            }
            teams.add(team);
        }
        return teams;
    }

    /**
     * STAGE 3+ — Inherit Stage 2 Team Lineage & Assign Role by Progression Order within that Team
     */
    private void handleStage3LineageAssignment(Student student, ActivityStage nextStage) {
        Team stage2Team = student.getTeam();

        // If student has no Stage 2 team lineage: DO NOT run generic snake. Log and leave unassigned.
        if (stage2Team == null) {
            log.error("INTEGRITY ERROR: Student {} ({}) reached Stage {} without a Stage 2 team lineage. Leaving unassigned.",
                    student.getRegNo(), student.getFullName(), nextStage.getDisplayOrder());
            return;
        }

        Long deptId = student.getDepartment() != null ? student.getDepartment().getId() : null;
        Long secId = student.getSection() != null ? student.getSection().getId() : null;
        String yearStr = Team.resolveCanonicalYearOfStudy(student.getYear());

        String baseName = extractBaseTeamName(stage2Team.getName());
        String newTeamName = nextStage.getStageName() + " - " + baseName;

        Team newTeam = findNextStageTeam(newTeamName, deptId, secId, yearStr);
        if (newTeam == null) {
            newTeam = new Team();
            newTeam.setName(newTeamName);
            newTeam.setSize(10);
            newTeam.setDepartment(student.getDepartment());
            newTeam.setSection(student.getSection());
            newTeam.setYear(yearStr);
            newTeam = teamRepository.save(newTeam);
        }

        StageTeam newStageTeam = stageTeamRepository.findByStageIdAndTeamId(nextStage.getId(), newTeam.getId()).orElse(null);
        if (newStageTeam == null) {
            newStageTeam = new StageTeam();
            newStageTeam.setStage(nextStage);
            newStageTeam.setTeam(newTeam);
            newStageTeam = stageTeamRepository.save(newStageTeam);
        }

        boolean isCaptainAssigned = newTeam.getCaptain() != null || newStageTeam.getCaptain() != null;
        boolean isViceCaptainAssigned = newTeam.getViceCaptain() != null || newStageTeam.getViceCaptain() != null;

        student.setTeam(newTeam);
        if (newTeam.getMembers() == null) {
            newTeam.setMembers(new java.util.HashSet<>());
        }
        newTeam.getMembers().add(student);
        student.setPromotionTimestamp(LocalDateTime.now());

        // Stage 3 Role Assignment: 1st promoted in this team = Captain, 2nd = Vice Captain, 3rd+ = Member
        TeamRole role = TeamRole.MEMBER;
        if (!isCaptainAssigned) {
            role = TeamRole.CAPTAIN;
            newTeam.setCaptain(student);
            newStageTeam.setCaptain(student);
            leadershipSyncService.syncLeadership(newTeam, student, newTeam.getViceCaptain());
            log.info("STAGE 3 LEADERSHIP: 1st promoted student {} in '{}' is assigned CAPTAIN",
                    student.getRegNo(), newTeamName);
        } else if (!isViceCaptainAssigned && (newTeam.getCaptain() == null || !newTeam.getCaptain().getId().equals(student.getId()))) {
            role = TeamRole.VICE_CAPTAIN;
            newTeam.setViceCaptain(student);
            newStageTeam.setViceCaptain(student);
            leadershipSyncService.syncLeadership(newTeam, newTeam.getCaptain(), student);
            log.info("STAGE 3 LEADERSHIP: 2nd promoted student {} in '{}' is assigned VICE CAPTAIN",
                    student.getRegNo(), newTeamName);
        } else {
            log.info("STAGE 3 LEADERSHIP: Student {} in '{}' is assigned MEMBER",
                    student.getRegNo(), newTeamName);
        }

        teamRepository.save(newTeam);
        stageTeamRepository.save(newStageTeam);
        studentRepository.save(student);

        // Sync team_members table
        if (entityManager != null) {
            try {
                entityManager.createNativeQuery(
                        "INSERT INTO team_members (team_id, student_id) VALUES (:tid, :sid) " +
                                "ON DUPLICATE KEY UPDATE team_id = :tid")
                        .setParameter("tid", newTeam.getId())
                        .setParameter("sid", student.getId())
                        .executeUpdate();
            } catch (Exception ignored) {
            }
        }
    }

    public Team findNextStageTeam(String name, Long deptId, Long secId, String yearStr) {
        return teamRepository.findExactTeam(name, deptId, secId, yearStr).orElse(null);
    }

    private String extractBaseTeamName(String oldName) {
        if (oldName == null) return "Team A";
        if (oldName.contains("- Team")) {
            return oldName.substring(oldName.indexOf("- Team") + 2).trim();
        } else if (oldName.startsWith("Team")) {
            return oldName;
        }
        return oldName;
    }
}
