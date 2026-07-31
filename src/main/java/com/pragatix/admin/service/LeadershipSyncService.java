package com.pragatix.admin.service;

import com.pragatix.entity.StageTeam;
import com.pragatix.entity.Student;
import com.pragatix.entity.Team;
import com.pragatix.repository.StageTeamRepository;
import com.pragatix.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeadershipSyncService {

    private final TeamRepository teamRepository;
    private final StageTeamRepository stageTeamRepository;

    public LeadershipSyncService(TeamRepository teamRepository, StageTeamRepository stageTeamRepository) {
        this.teamRepository = teamRepository;
        this.stageTeamRepository = stageTeamRepository;
    }

    @Transactional
    public void syncLeadership(Team team, Student captain, Student viceCaptain) {
        if (team == null) return;

        // 1. Update the main teams table
        team.setCaptain(captain);
        team.setViceCaptain(viceCaptain);
        teamRepository.save(team);

        // 2. Synchronize all associated StageTeam records
        List<StageTeam> stageTeams = stageTeamRepository.findByTeamId(team.getId());
        for (StageTeam st : stageTeams) {
            st.setCaptain(captain);
            st.setViceCaptain(viceCaptain);
            stageTeamRepository.save(st);
        }
    }
}
