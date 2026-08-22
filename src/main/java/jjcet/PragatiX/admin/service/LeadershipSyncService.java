package jjcet.PragatiX.admin.service;

import jjcet.PragatiX.entity.StageTeam;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.Team;
import jjcet.PragatiX.repository.StageTeamRepository;
import jjcet.PragatiX.repository.TeamRepository;
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
        if (team == null)
            return;

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
