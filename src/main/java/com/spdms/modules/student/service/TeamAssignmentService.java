package com.spdms.modules.student.service;

import com.spdms.entity.*;
import com.spdms.repository.*;
import com.spdms.modules.student.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TeamAssignmentService {

    private final StudentRepository studentRepository;
    private final TeamRepository teamRepository;
    private final StageTeamRepository stageTeamRepository;
    private final com.spdms.admin.service.CaptainSelectionService captainSelectionService;

    public TeamAssignmentService(StudentRepository studentRepository,
                                 TeamRepository teamRepository,
                                 StageTeamRepository stageTeamRepository,
                                 com.spdms.admin.service.CaptainSelectionService captainSelectionService) {
        this.studentRepository = studentRepository;
        this.teamRepository = teamRepository;
        this.stageTeamRepository = stageTeamRepository;
        this.captainSelectionService = captainSelectionService;
    }

    @Transactional
    public void assignTeamOnPromotion(Student student, ActivityStage nextStage) {
        System.out.println("TEAM ASSIGNMENT: Promotion Started for " + student.getRegNo());
        if (student.getTeam() == null) {
            handleInitialTeamAssignment(student, nextStage);
        } else {
            promoteStudentToNextStage(student, nextStage);
        }
    }

    @Transactional
    public void promoteStudentToNextStage(Student student, ActivityStage nextStage) {
        Team oldTeam = student.getTeam();
        if (oldTeam == null) return;
        
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
        
        removeStudentFromOldStage(student, oldTeam);
        
        moveStudent(student, newTeam, nextStage);
        
        System.out.println("Student Moved: YES");
        System.out.println("TEAM ASSIGNMENT: Promotion Success for " + student.getRegNo());
        System.out.println("=====================================================");
    }

    public Team createNextStageTeamIfRequired(String newTeamName, Student student, ActivityStage nextStage) {
        Long deptId = student.getDepartment() != null ? student.getDepartment().getId() : null;
        Long secId = student.getSection() != null ? student.getSection().getId() : null;
        String yearStr = student.getYear();

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
        // Prevent cross-team joining by checking if student already has a DIFFERENT team in this stage
        // We know student.team is now newTeam or will be updated to newTeam.
        student.setTeam(newTeam);
        addStudentToStageTeam(student, newTeam);
        
        // Dynamically evaluate captaincy for the new team
        captainSelectionService.evaluateCaptainForTeam(newTeam);
        
        student.setPromotionTimestamp(LocalDateTime.now());
        studentRepository.save(student);
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

    // Removed assignCaptainIfFirstMember and createCaptain as captain selection is now dynamic

    public void removeStudentFromOldStage(Student student, Team oldTeam) {
        if (oldTeam != null) {
            oldTeam.getMembers().remove(student);
            if (oldTeam.getCaptain() != null && oldTeam.getCaptain().getId().equals(student.getId())) {
                student.setCaptain(false);
                oldTeam.setCaptain(null); // Clear previous captaincy
            }
            teamRepository.save(oldTeam);
            // Re-evaluate captaincy for the old team
            captainSelectionService.evaluateCaptainForTeam(oldTeam);

            System.out.println("TEAM ASSIGNMENT: Removed from old team " + oldTeam.getName());
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
        String yearStr = student.getYear();

        if (deptId == null || yearStr == null) return;

        // Note: For initial assignment, we still need to calculate how many students are in the class.
        // I am leaving this part relatively intact but using findNextStageTeam to avoid stream().
        long classStrength = studentRepository.countByDepartmentIdAndYearAndSectionId(deptId, yearStr, secId);
        int teamCount = classStrength <= 40 ? 3 : 6;

        ensureTeamsExist(deptId, secId, yearStr, teamCount, student.getDepartment(), student.getSection(), nextStage);

        Integer promotedCount = studentRepository.countPromotedStudents(nextStage.getDisplayOrder(), deptId, yearStr, secId);
        int myOrder = (promotedCount != null ? promotedCount : 0) + 1;
        student.setPromotionOrder(myOrder);

        Team assignedTeam;
        boolean becomesCaptain = false;

        if (myOrder <= teamCount) {
            String teamName = nextStage.getStageName() + " - Team " + (char) ('A' + myOrder - 1);
            assignedTeam = findNextStageTeam(teamName, deptId, secId, yearStr);
        } else {
            int i = myOrder - teamCount - 1;
            int cycle = i / teamCount;
            int pos = i % teamCount;
            int teamIndex;
            if (cycle % 2 == 0) {
                teamIndex = teamCount - pos;
            } else {
                teamIndex = pos + 1;
            }
            String teamName = nextStage.getStageName() + " - Team " + (char) ('A' + teamIndex - 1);
            assignedTeam = findNextStageTeam(teamName, deptId, secId, yearStr);
        }

        removeStudentFromOldStage(student, student.getTeam()); // in case they somehow had one

        if (assignedTeam != null) {
            moveStudent(student, assignedTeam, nextStage);
        }
    }

    private void ensureTeamsExist(Long deptId, Long secId, String yearStr, int teamCount, Department dept, Section sec, ActivityStage stage) {
        for (int i = 1; i <= teamCount; i++) {
            String teamName = stage.getStageName() + " - Team " + (char) ('A' + i - 1);
            if (findNextStageTeam(teamName, deptId, secId, yearStr) == null) {
                Team team = new Team();
                team.setName(teamName);
                team.setSize(10);
                team.setDepartment(dept);
                team.setSection(sec);
                team.setYear(yearStr);
                team.setCreatedBy(null);
                teamRepository.save(team);
                
                StageTeam st = new StageTeam();
                st.setStage(stage);
                st.setTeam(team);
                stageTeamRepository.save(st);
            }
        }
    }
}
