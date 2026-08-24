package jjcet.PragatiX.admin.service;

import jjcet.PragatiX.entity.StageTeam;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.Team;
import jjcet.PragatiX.repository.StageTeamRepository;
import jjcet.PragatiX.repository.TeamRemovalRequestRepository;
import jjcet.PragatiX.repository.TeamRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class TeamCleanupService {

    private final TeamRepository teamRepository;
    private final StageTeamRepository stageTeamRepository;
    private final TeamRemovalRequestRepository teamRemovalRequestRepository;
    private final StudentRepository studentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public TeamCleanupService(TeamRepository teamRepository,
            StageTeamRepository stageTeamRepository,
            TeamRemovalRequestRepository teamRemovalRequestRepository,
            StudentRepository studentRepository) {
        this.teamRepository = teamRepository;
        this.stageTeamRepository = stageTeamRepository;
        this.teamRemovalRequestRepository = teamRemovalRequestRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * Deletes the given team if it is completely empty.
     * A team is EMPTY when: captain is NULL AND there are no members.
     * 
     * @param team the team to check and delete if empty
     * @return true if the team was deleted, false otherwise
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean autoDeleteEmptyTeam(Team team) {
        if (team == null || team.getId() == null)
            return false;

        boolean noCaptain = team.getCaptain() == null;
        List<Student> members = studentRepository.findByTeamId(team.getId());
        boolean noMembers = members == null || members.isEmpty();

        if (noCaptain && noMembers) {
            team.setViceCaptain(null);

            List<StageTeam> stageTeams = stageTeamRepository.findByTeamId(team.getId());
            if (!stageTeams.isEmpty()) {
                stageTeamRepository.deleteAll(stageTeams);
            }

            teamRemovalRequestRepository.deleteAll(teamRemovalRequestRepository.findByTeamId(team.getId()));

            try {
                teamRepository.delete(team);
            } catch (Exception ex) {
                team.setDeleted(true);
                teamRepository.save(team);
            }
            System.out.println("TEAM CLEANUP: Automatically deleted empty team: " + team.getName() + " (ID: "
                    + team.getId() + ")");
            return true;
        }
        return false;
    }
}
