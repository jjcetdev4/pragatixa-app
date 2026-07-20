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

        // Student crosses the threshold -> Promote to next stage
        student.setStage(student.getStage() + 1);
        studentRepository.save(student);

        // Check if student is captain
        Team team = student.getTeam();
        if (team != null && team.getCaptain() != null && team.getCaptain().getId().equals(student.getId())) {
            // Find new captain
            reassignCaptain(team, student);
        }
    }

    private void reassignCaptain(Team team, Student promotedStudent) {
        // Find all active members in the same team who have NOT been promoted out of the team's base stage
        // A simple heuristic: find the highest XP student in the team who is not the promoted student.
        // The prompt says: "Not already promoted out of the team"
        // Let's assume anyone with stage <= promotedStudent.getStage() - 1 is eligible.
        // Actually, just anyone currently in the team who is active.
        
        int teamBaseStage = promotedStudent.getStage() - 1; // Since we just incremented it
        
        List<Student> eligibleMembers = team.getMembers().stream()
                .filter(Student::isActive)
                .filter(m -> !m.getId().equals(promotedStudent.getId()))
                .filter(m -> m.getStage() <= teamBaseStage) // Not promoted out
                .collect(Collectors.toList());

        if (eligibleMembers.isEmpty()) {
            // If no eligible members, the team might just have no captain or we fall back to any member
            eligibleMembers = team.getMembers().stream()
                    .filter(Student::isActive)
                    .filter(m -> !m.getId().equals(promotedStudent.getId()))
                    .collect(Collectors.toList());
        }

        if (eligibleMembers.isEmpty()) {
            team.setCaptain(null);
        } else {
            // Sort by 1. Highest XP 2. Earliest timestamp (simulated by lowest ID if timestamp not available)
            eligibleMembers.sort(Comparator.comparingInt(Student::getTotalXp).reversed()
                    .thenComparing(Student::getId));
            
            Student newCaptain = eligibleMembers.get(0);
            team.setCaptain(newCaptain);
        }
        
        teamRepository.save(team);
    }
}
