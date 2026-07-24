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
    private final com.spdms.repository.StageTeamRepository stageTeamRepository;

    public CaptainSelectionService(TeamRepository teamRepository, 
                                   StudentRepository studentRepository,
                                   ActivityStageRepository activityStageRepository,
                                   com.spdms.repository.StageTeamRepository stageTeamRepository) {
        this.teamRepository = teamRepository;
        this.studentRepository = studentRepository;
        this.activityStageRepository = activityStageRepository;
        this.stageTeamRepository = stageTeamRepository;
    }

    @Transactional
    public void evaluateCaptainPromotion(Student student) {
        // Obsolete, captain selection is now evaluated per team upon promotion/XP award
        if (student.getTeam() != null) {
            evaluateCaptainForTeam(student.getTeam());
        }
    }

    @Transactional
    public void evaluateCaptainForTeam(Team team) {
        if (team == null || team.getMembers() == null || team.getMembers().isEmpty()) {
            return;
        }
        
        List<Student> eligibleMembers = team.getMembers().stream()
                .filter(Student::isActive)
                .collect(Collectors.toList());

        if (eligibleMembers.isEmpty()) {
            if (team.getCaptain() != null) {
                Student oldCaptain = team.getCaptain();
                oldCaptain.setCaptain(false);
                studentRepository.save(oldCaptain);
                team.setCaptain(null);
                teamRepository.save(team);
                updateStageTeamCaptain(team, null);
            }
            return;
        }

        eligibleMembers.sort(Comparator.comparingInt(Student::getTotalXp).reversed()
                .thenComparing(Student::getId));
        
        Student newCaptain = eligibleMembers.get(0);
        Student oldCaptain = team.getCaptain();
        
        if (oldCaptain == null || !oldCaptain.getId().equals(newCaptain.getId())) {
            if (oldCaptain != null) {
                oldCaptain.setCaptain(false);
                studentRepository.save(oldCaptain);
            }
            newCaptain.setCaptain(true);
            team.setCaptain(newCaptain);
            studentRepository.save(newCaptain);
            teamRepository.save(team);
            
            updateStageTeamCaptain(team, newCaptain);
            
            System.out.println("CAPTAIN SELECTION: New Captain for " + team.getName() + " -> " + newCaptain.getRegNo());
        }
    }
    
    private void updateStageTeamCaptain(Team team, Student captain) {
        // Also update the StageTeam record if it exists
        List<com.spdms.entity.StageTeam> stageTeams = stageTeamRepository.findByTeamId(team.getId());
        for (com.spdms.entity.StageTeam st : stageTeams) {
            st.setCaptain(captain);
            stageTeamRepository.save(st);
        }
    }
}
