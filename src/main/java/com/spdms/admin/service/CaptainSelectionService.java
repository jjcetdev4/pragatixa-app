package com.pragatix.admin.service;

import com.pragatix.entity.ActivityStage;
import com.pragatix.entity.Student;
import com.pragatix.entity.Team;
import com.pragatix.repository.TeamRepository;
import com.pragatix.modules.activity.repository.ActivityStageRepository;
import com.pragatix.modules.student.repository.StudentRepository;
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
    private final com.pragatix.repository.StageTeamRepository stageTeamRepository;

    public CaptainSelectionService(TeamRepository teamRepository,
            StudentRepository studentRepository,
            ActivityStageRepository activityStageRepository,
            com.pragatix.repository.StageTeamRepository stageTeamRepository) {
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
        // Disabled legacy XP-based leadership reassignment.
        // Leadership is determined strictly by the order in which team members satisfy the promotion conditions.
        // Highest XP no longer influences leadership.
        System.out.println("CAPTAIN SELECTION: Legacy XP-based reassignment disabled for team: " + (team != null ? team.getName() : "null"));
    }

    private void updateStageTeamCaptain(Team team, Student captain) {
        // Also update the StageTeam record if it exists
        List<com.pragatix.entity.StageTeam> stageTeams = stageTeamRepository.findByTeamId(team.getId());
        for (com.pragatix.entity.StageTeam st : stageTeams) {
            st.setCaptain(captain);
            stageTeamRepository.save(st);
        }
    }
}
