package com.spdms.admin.service;

import com.spdms.entity.ActivityStage;
import com.spdms.entity.Student;
import com.spdms.entity.Team;
import com.spdms.repository.TeamRepository;
import com.spdms.modules.activity.repository.ActivityStageRepository;
import com.spdms.modules.student.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CaptainSelectionService {

    private final TeamRepository teamRepository;
    private final StudentRepository studentRepository;
    private final ActivityStageRepository activityStageRepository;

    public CaptainSelectionService(TeamRepository teamRepository, 
                                   StudentRepository studentRepository,
                                   ActivityStageRepository activityStageRepository) {
        this.teamRepository = teamRepository;
        this.studentRepository = studentRepository;
        this.activityStageRepository = activityStageRepository;
    }

    @Transactional
    public void evaluateCaptainPromotion(Student student) {
        // Find the current stage threshold
        Optional<ActivityStage> currentStageOpt = activityStageRepository.findFirstByDisplayOrderGreaterThanOrderByDisplayOrderAsc(student.getStage() - 1);
        
        if (currentStageOpt.isEmpty()) {
            return; // No stage to evaluate against
        }
        
        ActivityStage currentStage = currentStageOpt.get();
        if (student.getTotalXp() < currentStage.getExpectedXp()) {
            return; // Has not crossed the threshold
        }

        // Student crosses the threshold -> Handled entirely by StudentXpService now
        // to avoid duplicate/conflicting stage promotion and bypassing MUST/IND/GRP thresholds
        // student.setStage(student.getStage() + 1);
        // studentRepository.save(student);

        // Check if student is captain - this is also now handled by TeamAssignmentService
        // during the actual Team Transition workflow
        // Team team = student.getTeam();
        // if (team != null && team.getCaptain() != null && team.getCaptain().getId().equals(student.getId())) {
        //     reassignCaptain(team, student);
        // }
    }

    public void reassignCaptain(Team team, Student promotedStudent) {
        int teamBaseStage = promotedStudent.getStage() - 1; 
        
        List<Student> eligibleMembers = team.getMembers().stream()
                .filter(Student::isActive)
                .filter(m -> !m.getId().equals(promotedStudent.getId()))
                .filter(m -> m.getStage() <= teamBaseStage) 
                .collect(Collectors.toList());

        if (eligibleMembers.isEmpty()) {
            eligibleMembers = team.getMembers().stream()
                    .filter(Student::isActive)
                    .filter(m -> !m.getId().equals(promotedStudent.getId()))
                    .collect(Collectors.toList());
        }

        if (eligibleMembers.isEmpty()) {
            team.setCaptain(null);
        } else {
            eligibleMembers.sort(Comparator.comparingInt(Student::getTotalXp).reversed()
                    .thenComparing(Student::getId));
            
            Student newCaptain = eligibleMembers.get(0);
            team.setCaptain(newCaptain);
            newCaptain.setCaptain(true);
            studentRepository.save(newCaptain);
        }
        
        teamRepository.save(team);
    }
}
