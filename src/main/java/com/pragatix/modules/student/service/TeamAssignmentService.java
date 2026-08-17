package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.repository.*;
import jjcet.PragatiX.enums.TeamRole;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class TeamAssignmentService {

    private final StudentRepository studentRepository;
    private final TeamRepository teamRepository;
    private final StageTeamRepository stageTeamRepository;
    private final jjcet.PragatiX.admin.service.CaptainSelectionService captainSelectionService;
    private final jjcet.PragatiX.admin.service.TeamCleanupService teamCleanupService;
    private final jjcet.PragatiX.admin.service.LeadershipSyncService leadershipSyncService;

    @PersistenceContext
    private EntityManager entityManager;

    public TeamAssignmentService(StudentRepository studentRepository,
            TeamRepository teamRepository,
            StageTeamRepository stageTeamRepository,
            jjcet.PragatiX.admin.service.CaptainSelectionService captainSelectionService,
            jjcet.PragatiX.admin.service.TeamCleanupService teamCleanupService,
            jjcet.PragatiX.admin.service.LeadershipSyncService leadershipSyncService) {
        this.studentRepository = studentRepository;
        this.teamRepository = teamRepository;
        this.stageTeamRepository = stageTeamRepository;
        this.captainSelectionService = captainSelectionService;
        this.teamCleanupService = teamCleanupService;
        this.leadershipSyncService = leadershipSyncService;
    }

    @Transactional
    public void assignTeamOnPromotion(Student student, ActivityStage nextStage) {
        System.out.println("TEAM ASSIGNMENT: Promotion Started for " + student.getRegNo());

        // Stage 2 must ignore the previous team and strictly follow promotion order.
        boolean isStage2 = nextStage.getDisplayOrder() == 2
                || nextStage.getStageName().toLowerCase().contains("stage 2");

        if (isStage2 || student.getTeam() == null) {
            handleInitialTeamAssignment(student, nextStage);
        } else {
            // Stage 3+ preserves lineage from Stage 2.
            promoteStudentToNextStage(student, nextStage);
        }
    }

    @Transactional
    public void promoteStudentToNextStage(Student student, ActivityStage nextStage) {
        Team oldTeam = student.getTeam();
        if (oldTeam == null)
            return;

        System.out.println("=====================================================");
        System.out.println("PROMOTION LOG:");
        System.out.println("Student: " + student.getRegNo() + " (" + student.getFullName() + ")");
        System.out.println("Current Stage: " + student.getStage());
        System.out.println("Current Team: " + oldTeam.getName());
        System.out.println("Destination Stage: " + nextStage.getDisplayOrder());

        String baseName = extractBaseTeamName(oldTeam.getName());
        String newTeamName = nextStage.getStageName() + " - " + baseName;

        System.out.println("Destination Team: " + newTeamName);

        Team newTeam = createNextStageTeamIfRequired(newTeamName, student, nextStage);

        removeStudentFromAllOldTeams(student);

        boolean isCaptainAssigned = newTeam.getCaptain() != null;
        boolean isViceCaptainAssigned = false;

        StageTeam newStageTeam = stageTeamRepository.findByStageIdAndTeamId(nextStage.getId(), newTeam.getId())
                .orElse(null);
        if (newStageTeam != null) {
            isViceCaptainAssigned = newStageTeam.getViceCaptain() != null;
        }

        moveStudent(student, newTeam, nextStage);

        // Assign leadership strictly by promotion order within this new team
        if (!isCaptainAssigned) {
            leadershipSyncService.syncLeadership(newTeam, student, newTeam.getViceCaptain());
            System.out.println("STAGE 3+ LEADERSHIP: First promoted in " + newTeamName + " is now Captain -> "
                    + student.getRegNo());
        } else if (!isViceCaptainAssigned) {
            leadershipSyncService.syncLeadership(newTeam, newTeam.getCaptain(), student);
            System.out.println("STAGE 3+ LEADERSHIP: Second promoted in " + newTeamName + " is now Vice Captain -> "
                    + student.getRegNo());
        } else {
            System.out.println(
                    "STAGE 3+ LEADERSHIP: Standard member assigned to " + newTeamName + " -> " + student.getRegNo());
        }

        System.out.println("Student Moved: YES");
        System.out.println("TEAM ASSIGNMENT: Promotion Success for " + student.getRegNo());
        System.out.println("=====================================================");
    }

    public Team createNextStageTeamIfRequired(String newTeamName, Student student, ActivityStage nextStage) {
        Long deptId = student.getDepartment() != null ? student.getDepartment().getId() : null;
        Long secId = student.getSection() != null ? student.getSection().getId() : null;
        String yearStr = jjcet.PragatiX.entity.Team.resolveCanonicalYearOfStudy(student.getYear());

        Team newTeam = findNextStageTeam(newTeamName, deptId, secId, yearStr);
        if (newTeam == null) {
            newTeam = new Team();
            newTeam.setName(newTeamName);
            newTeam.setSize(10);
            newTeam.setDepartment(student.getDepartment());
            newTeam.setSection(student.getSection());
            newTeam.setYear(yearStr);
            newTeam.setCreatedBy(null);
            newTeam = teamRepository.save(newTeam);

            // Create StageTeam link
            StageTeam st = new StageTeam();
            st.setStage(nextStage);
            st.setTeam(newTeam);
            stageTeamRepository.save(st);

            System.out.println("Team Created: YES");
            System.out.println("StageTeam Created: YES");
            System.out.println("TEAM ASSIGNMENT: Created new team " + newTeamName);
        }
        return newTeam;
    }

    public Team findNextStageTeam(String name, Long deptId, Long secId, String yearStr) {
        return teamRepository.findExactTeam(name, deptId, secId, yearStr).orElse(null);
    }

    public void moveStudent(Student student, Team newTeam, ActivityStage nextStage) {
        // Ensure student is unlinked from any previous team
        if (student.getTeam() != null && !student.getTeam().getId().equals(newTeam.getId())) {
            removeStudentFromOldStage(student, student.getTeam());
        }

        student.setTeam(newTeam);
        addStudentToStageTeam(student, newTeam);

        student.setPromotionTimestamp(LocalDateTime.now());
        studentRepository.save(student);

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

    public void addStudentToStageTeam(Student student, Team newTeam) {
        if (!newTeam.getMembers().contains(student)) {
            if (newTeam.getMembers().size() >= 10) {
                throw new IllegalStateException("Maximum team size of 10 reached for team: " + newTeam.getName());
            }
            newTeam.getMembers().add(student);
            teamRepository.save(newTeam);

            System.out.println("TEAM ASSIGNMENT: Member Added to " + newTeam.getName());
        }
    }

    public void removeStudentFromAllOldTeams(Student student) {
        Team oldTeam = student.getTeam();
        if (oldTeam != null) {
            removeStudentFromOldStage(student, oldTeam);
        }
        java.util.List<Team> otherTeams = teamRepository.findAllTeamsByStudentId(student.getId());
        for (Team ot : otherTeams) {
            if (oldTeam == null || !ot.getId().equals(oldTeam.getId())) {
                removeStudentFromOldStage(student, ot);
            }
        }
        if (entityManager != null) {
            try {
                entityManager.createNativeQuery("DELETE FROM team_members WHERE student_id = :sid")
                        .setParameter("sid", student.getId())
                        .executeUpdate();
            } catch (Exception ignored) {
            }
        }
    }

    public void removeStudentFromOldStage(Student student, Team oldTeam) {
        if (oldTeam != null) {
            oldTeam.getMembers().remove(student);

            if (oldTeam.getCaptain() != null && oldTeam.getCaptain().getId().equals(student.getId())) {
                oldTeam.setCaptain(null); // Clear previous captaincy
            }

            if (oldTeam.getViceCaptain() != null && oldTeam.getViceCaptain().getId().equals(student.getId())) {
                oldTeam.setViceCaptain(null); // Clear previous vice captaincy on Team
            }

            // Clear previous vice captaincy if applicable
            java.util.List<StageTeam> oldStageTeams = stageTeamRepository.findByTeamId(oldTeam.getId());
            for (StageTeam st : oldStageTeams) {
                if (st.getViceCaptain() != null && st.getViceCaptain().getId().equals(student.getId())) {
                    st.setViceCaptain(null);
                    stageTeamRepository.save(st);
                }
                if (st.getCaptain() != null && st.getCaptain().getId().equals(student.getId())) {
                    st.setCaptain(null);
                    stageTeamRepository.save(st);
                }
            }

            // Clean up team_members records for this student and old team
            if (entityManager != null) {
                try {
                    entityManager.createNativeQuery(
                            "DELETE FROM team_members WHERE student_id = :sid AND team_id = :tid")
                            .setParameter("sid", student.getId())
                            .setParameter("tid", oldTeam.getId())
                            .executeUpdate();
                } catch (Exception ignored) {
                }
            }

            teamRepository.save(oldTeam);

            // Re-evaluate captaincy for the old team ALWAYS
            captainSelectionService.evaluateCaptainForTeam(oldTeam);

            System.out.println("TEAM ASSIGNMENT: Removed from old team " + oldTeam.getName());

            teamCleanupService.autoDeleteEmptyTeam(oldTeam);
        }
    }

    private String extractBaseTeamName(String oldName) {
        if (oldName.contains("- Team")) {
            return oldName.substring(oldName.indexOf("- Team") + 2).trim();
        } else if (oldName.startsWith("Team")) {
            return oldName;
        }
        return oldName; // fallback
    }


    // --- INITIAL TEAM ASSIGNMENT LOGIC ---
    private void handleInitialTeamAssignment(Student student, ActivityStage nextStage) {
        Long deptId = student.getDepartment() != null ? student.getDepartment().getId() : null;
        Long secId = student.getSection() != null ? student.getSection().getId() : null;
        String yearStr = jjcet.PragatiX.entity.Team.resolveCanonicalYearOfStudy(student.getYear());

        if (deptId == null || yearStr == null)
            return;

        int teamCount = 6;

        boolean isStage2 = nextStage.getDisplayOrder() == 2
                || nextStage.getStageName().toLowerCase().contains("stage 2");

        if (isStage2) {
            int stage1Count = teamRepository.countStage1TeamsForClass(deptId, yearStr, secId);
            if (stage1Count == 0) {
                System.err.println("WARNING: Stage 1 teams have not been created for this class/group yet. Cannot create Stage 2 teams. Aborting promotion.");
                throw new IllegalStateException("Stage 1 teams have not been created for this class/group yet. Cannot create Stage 2 teams.");
            }
            teamCount = stage1Count;
        }

        java.util.List<Team> teams = new java.util.ArrayList<>();

        for (int i = 0; i < teamCount; i++) {
            String teamName = nextStage.getStageName() + " - Team " + (char) ('A' + i);
            Team t = findNextStageTeam(teamName, deptId, secId, yearStr);
            teams.add(t);
        }

        Team assignedTeam = null;
        boolean assignCaptain = false;
        boolean assignViceCaptain = false;
        StageTeam assignedStageTeam = null;

        // Phase 1: Captain Assignment (Teams A -> F)
        for (int i = 0; i < teamCount; i++) {
            Team t = teams.get(i);
            if (t == null) {
                // Team does not exist -> dynamically create it and assign Captain
                String teamName = nextStage.getStageName() + " - Team " + (char) ('A' + i);
                assignedTeam = createNextStageTeamIfRequired(teamName, student, nextStage);
                teams.set(i, assignedTeam);
                assignCaptain = true;
                break;
            } else if (t.getCaptain() == null) {
                // Team exists but has no captain
                assignedTeam = t;
                assignCaptain = true;
                break;
            }
        }

        // Phase 2: Vice Captain Assignment (Teams F -> A)
        if (assignedTeam == null) {
            for (int i = teamCount - 1; i >= 0; i--) {
                Team t = teams.get(i);
                if (t != null && t.getCaptain() != null) {
                    StageTeam st = stageTeamRepository.findByStageIdAndTeamId(nextStage.getId(), t.getId())
                            .orElse(null);
                    if (st != null && st.getViceCaptain() == null) {
                        assignedTeam = t;
                        assignedStageTeam = st;
                        assignViceCaptain = true;
                        System.out.println(
                                "SNAKE ALGORITHM: Reverse Assigning Vice Captain to " + assignedTeam.getName());
                        break;
                    }
                }
            }
        }

        // Phase 3: Member Assignment (Snake Pattern)
        if (assignedTeam == null) {
            int totalMembers = 0;
            for (Team t : teams) {
                if (t != null) {
                    totalMembers += t.getMembers() != null ? t.getMembers().size() : 0;
                }
            }

            // Phase 3 begins after all teams have 1 Captain + 1 Vice Captain
            int sequenceIndex = totalMembers - (teamCount * 2);
            if (sequenceIndex < 0)
                sequenceIndex = 0;

            int cycle = sequenceIndex / teamCount;
            int pos = sequenceIndex % teamCount;
            int teamIndex;

            if (cycle % 2 == 0) {
                teamIndex = pos;
                System.out.println(
                        "SNAKE ALGORITHM: Forward Phase Member Assignment -> Team " + (char) ('A' + teamIndex));
            } else {
                teamIndex = teamCount - 1 - pos;
                System.out.println(
                        "SNAKE ALGORITHM: Reverse Phase Member Assignment -> Team " + (char) ('A' + teamIndex));
            }

            assignedTeam = teams.get(teamIndex);
        }

        removeStudentFromAllOldTeams(student);

        if (assignedTeam != null) {
            student.setPromotionOrder(null); // Clear legacy counter
            moveStudent(student, assignedTeam, nextStage);

            if (assignCaptain) {
                leadershipSyncService.syncLeadership(assignedTeam, student, assignedTeam.getViceCaptain());
            } else if (assignViceCaptain) {
                leadershipSyncService.syncLeadership(assignedTeam, assignedTeam.getCaptain(), student);
            }

            System.out.println("\n================ TEAM PROMOTION ASSIGNMENT ================");
            System.out.println("Student:");
            System.out.println("Student ID: " + student.getId());
            System.out.println("Student Register Number: " + student.getRegNo());
            System.out.println("\nSource Stage: " + student.getStage());
            System.out.println("Target Stage: " + nextStage.getStageName());
            
            System.out.println("\nEligible Target Teams:");
            System.out.println("Team ID | Team Name | Current Members | Distribution Order");
            int[] teamMemberCounts = new int[teamCount];
            for (int k = 0; k < teamCount; k++) {
                Team t = teams.get(k);
                int cnt = (t != null && t.getMembers() != null) ? t.getMembers().size() : 0;
                teamMemberCounts[k] = cnt;
                if (t != null) {
                    System.out.println(t.getId() + " | " + t.getName() + " | " + cnt + " | " + k);
                } else {
                    System.out.println("N/A | (To be created) | 0 | " + k);
                }
            }
            
            System.out.println("\nSelected Team:");
            System.out.println("Selected Team ID: " + assignedTeam.getId());
            System.out.println("Selected Team Name: " + assignedTeam.getName());
            
            String reason = assignCaptain ? "First assignment (Captain Phase)" : (assignViceCaptain ? "Second assignment (Vice Captain Phase)" : "Snake Algorithm Member Distribution");
            System.out.println("Reason: " + reason);
            
            System.out.println("\nAfter Assignment:");
            for (int k = 0; k < teamCount; k++) {
                Team t = teams.get(k);
                if (t != null) {
                    String tName = t.getName();
                    int newCnt = teamMemberCounts[k] + (t.getId().equals(assignedTeam.getId()) ? 1 : 0);
                    System.out.println(tName + " = " + newCnt + " members");
                }
            }
            
            System.out.println("\nOld Team Removal:");
            System.out.println("Old Team: Checked and removed (duplicate removal prevented by Set logic)");
            
            System.out.println("\nCaptain:");
            Student cap = assignedTeam.getCaptain();
            System.out.println("Captain ID: " + (cap != null ? cap.getId() : "None"));
            System.out.println("Captain Register Number: " + (cap != null ? cap.getRegNo() : "None"));
            
            System.out.println("\nVice Captain:");
            Student vc = assignedTeam.getViceCaptain();
            System.out.println("Vice Captain ID: " + (vc != null ? vc.getId() : "None"));
            System.out.println("Vice Captain Register Number: " + (vc != null ? vc.getRegNo() : "None"));
            
            System.out.println("\nPersisted:\nYES");
            System.out.println("=================================================================\n");
        }
    }
}
