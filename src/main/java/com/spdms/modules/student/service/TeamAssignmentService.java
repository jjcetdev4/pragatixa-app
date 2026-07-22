package com.spdms.modules.student.service;

import com.spdms.entity.*;
import com.spdms.repository.*;
import com.spdms.modules.student.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.spdms.modules.authentication.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TeamAssignmentService {

    private final StudentRepository studentRepository;
    private final TeamRepository teamRepository;
    private final StageTeamRepository stageTeamRepository;
    private final UserRepository userRepository;
    private final com.spdms.admin.service.CaptainSelectionService captainSelectionService;

    public TeamAssignmentService(StudentRepository studentRepository,
                                 TeamRepository teamRepository,
                                 StageTeamRepository stageTeamRepository,
                                 UserRepository userRepository,
                                 com.spdms.admin.service.CaptainSelectionService captainSelectionService) {
        this.studentRepository = studentRepository;
        this.teamRepository = teamRepository;
        this.stageTeamRepository = stageTeamRepository;
        this.userRepository = userRepository;
        this.captainSelectionService = captainSelectionService;
    }

    @Transactional
    public void assignTeamOnPromotion(Student student, ActivityStage nextStage) {
        if (student.getTeam() == null) {
            handleInitialTeamAssignment(student, nextStage);
        } else {
            handleSubsequentStagePromotion(student, nextStage);
        }
    }

    private void handleInitialTeamAssignment(Student student, ActivityStage nextStage) {
        Long deptId = student.getDepartment() != null ? student.getDepartment().getId() : null;
        Long secId = student.getSection() != null ? student.getSection().getId() : null;
        String yearStr = student.getYear();

        if (deptId == null || yearStr == null) return;

        List<Student> classStudents = studentRepository.findAll().stream()
                .filter(s -> s.isActive() && 
                             s.getDepartment() != null && s.getDepartment().getId().equals(deptId) &&
                             s.getYear() != null && s.getYear().equals(yearStr) &&
                             ((secId == null && s.getSection() == null) || (s.getSection() != null && s.getSection().getId().equals(secId))))
                .toList();

        int classStrength = classStudents.size();
        int teamCount = classStrength <= 40 ? 3 : 6;

        ensureTeamsExist(deptId, secId, yearStr, teamCount, student.getDepartment(), student.getSection(), nextStage);

        long promotedCount = classStudents.stream()
                .filter(s -> s.getStage() >= nextStage.getDisplayOrder() && s.getPromotionOrder() != null)
                .count();

        int myOrder = (int) promotedCount + 1;
        student.setPromotionOrder(myOrder);

        Team assignedTeam;
        boolean becomesCaptain = false;

        if (myOrder <= teamCount) {
            String teamName = nextStage.getStageName() + " - Team " + (char) ('A' + myOrder - 1);
            assignedTeam = getTeamByName(teamName, deptId, secId, yearStr);
            becomesCaptain = true;
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
            assignedTeam = getTeamByName(teamName, deptId, secId, yearStr);
        }

        cleanupOldTeam(student);

        student.setTeam(assignedTeam);
        student.setCaptain(becomesCaptain);
        if (assignedTeam != null) {
            assignedTeam.getMembers().add(student);
        }

        if (becomesCaptain && assignedTeam != null) {
            assignedTeam.setCaptain(student);
            teamRepository.save(assignedTeam);
            
            StageTeam st = new StageTeam();
            st.setStage(nextStage);
            st.setTeam(assignedTeam);
            st.setCaptain(student);
            stageTeamRepository.save(st);
        } else if (assignedTeam != null) {
            teamRepository.save(assignedTeam);
        }

        student.setPromotionTimestamp(LocalDateTime.now());
        studentRepository.save(student);
    }

    private void handleSubsequentStagePromotion(Student student, ActivityStage nextStage) {
        Team oldTeam = student.getTeam();
        if (oldTeam == null) return;

        // Extract base team name
        String oldName = oldTeam.getName();
        String baseName = oldName;
        if (oldName.contains("- Team")) {
            baseName = oldName.substring(oldName.indexOf("- Team") + 2).trim();
        } else if (oldName.startsWith("Team")) {
            baseName = oldName;
        }
        
        String newTeamName = nextStage.getStageName() + " - " + baseName;
        
        Long deptId = student.getDepartment() != null ? student.getDepartment().getId() : null;
        Long secId = student.getSection() != null ? student.getSection().getId() : null;
        String yearStr = student.getYear();
        
        // Find destination Stage team. If destination doesn't exist, create it.
        Team newTeam = getTeamByName(newTeamName, deptId, secId, yearStr);
        if (newTeam == null) {
            newTeam = new Team();
            newTeam.setName(newTeamName);
            newTeam.setSize(10);
            newTeam.setDepartment(student.getDepartment());
            newTeam.setSection(student.getSection());
            newTeam.setYear(yearStr);
            newTeam.setCreatedBy(null);
            newTeam = teamRepository.save(newTeam);
        }
        
        cleanupOldTeam(student);
        
        // Assign promoted student to new team
        student.setTeam(newTeam);
        newTeam.getMembers().add(student);
        
        // Check if newTeam already has a captain
        if (newTeam.getCaptain() == null) {
            student.setCaptain(true);
            newTeam.setCaptain(student);
            
            StageTeam st = new StageTeam();
            st.setStage(nextStage);
            st.setTeam(newTeam);
            st.setCaptain(student);
            stageTeamRepository.save(st);
        } else {
            student.setCaptain(false);
        }
        
        teamRepository.save(newTeam);
        student.setPromotionTimestamp(LocalDateTime.now());
        studentRepository.save(student);
    }

    private void cleanupOldTeam(Student student) {
        Team oldTeam = student.getTeam();
        if (oldTeam != null) {
            oldTeam.getMembers().remove(student);
            if (oldTeam.getCaptain() != null && oldTeam.getCaptain().getId().equals(student.getId())) {
                captainSelectionService.reassignCaptain(oldTeam, student);
            }
            teamRepository.save(oldTeam);
        }
    }

    private void ensureTeamsExist(Long deptId, Long secId, String yearStr, int teamCount, Department dept, Section sec, ActivityStage stage) {
        for (int i = 1; i <= teamCount; i++) {
            String teamName = stage.getStageName() + " - Team " + (char) ('A' + i - 1);
            if (getTeamByName(teamName, deptId, secId, yearStr) == null) {
                Team team = new Team();
                team.setName(teamName);
                team.setSize(10); // arbitrary default
                team.setDepartment(dept);
                team.setSection(sec);
                team.setYear(yearStr);
                team.setCreatedBy(null);
                teamRepository.save(team);
            }
        }
    }

    private Team getTeamByName(String name, Long deptId, Long secId, String yearStr) {
        return teamRepository.findAll().stream()
                .filter(t -> t.getName().equalsIgnoreCase(name) &&
                             t.getDepartment() != null && t.getDepartment().getId().equals(deptId) &&
                             t.getYear() != null && t.getYear().equals(yearStr) &&
                             ((secId == null && t.getSection() == null) || (t.getSection() != null && t.getSection().getId().equals(secId))))
                .findFirst().orElse(null);
    }
}
