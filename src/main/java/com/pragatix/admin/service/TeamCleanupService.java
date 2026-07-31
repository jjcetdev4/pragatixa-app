package com.pragatix.admin.service;

import com.pragatix.entity.StageTeam;
import com.pragatix.entity.Team;
import com.pragatix.repository.StageTeamRepository;
import com.pragatix.repository.TeamRemovalRequestRepository;
import com.pragatix.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class TeamCleanupService {

    private final TeamRepository teamRepository;
    private final StageTeamRepository stageTeamRepository;
    private final TeamRemovalRequestRepository teamRemovalRequestRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public TeamCleanupService(TeamRepository teamRepository,
            StageTeamRepository stageTeamRepository,
            TeamRemovalRequestRepository teamRemovalRequestRepository) {
        this.teamRepository = teamRepository;
        this.stageTeamRepository = stageTeamRepository;
        this.teamRemovalRequestRepository = teamRemovalRequestRepository;
    }

    /**
     * Deletes the given team if it is completely empty.
     * A team is EMPTY when: captain is NULL AND there are no members.
     * 
     * @param team the team to check and delete if empty
     * @return true if the team was deleted, false otherwise
     */
    @Transactional
    public boolean autoDeleteEmptyTeam(Team team) {
        if (team == null)
            return false;

        if (team.getCaptain() == null && (team.getMembers() == null || team.getMembers().isEmpty())) {

            // In the new business rules: A team must exist ONLY when it has at least one
            // student.
            // A Vice Captain is a member of the team, so if members is empty, there is no
            // vice captain anyway!
            List<StageTeam> stageTeams = stageTeamRepository.findByTeamId(team.getId());
            if (!stageTeams.isEmpty()) {
                stageTeamRepository.deleteAll(stageTeams);
            }

            teamRemovalRequestRepository.deleteAll(teamRemovalRequestRepository.findByTeamId(team.getId()));

            if (entityManager != null) {
                entityManager.createNativeQuery("DELETE FROM team_members WHERE team_id = :tid")
                        .setParameter("tid", team.getId())
                        .executeUpdate();
            }

            teamRepository.delete(team);
            System.out.println("TEAM CLEANUP: Automatically deleted empty team: " + team.getName() + " (ID: "
                    + team.getId() + ")");
            return true;
        }
        return false;
    }
}
