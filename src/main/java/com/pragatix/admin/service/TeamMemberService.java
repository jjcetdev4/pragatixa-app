package jjcet.PragatiX.admin.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.Team;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.repository.TeamRepository;
import jjcet.PragatiX.enums.TeamRole;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.entity.StageTeam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TeamMemberService {

    private final TeamRepository teamRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    private final TeamValidationService validationService;
    private final jjcet.PragatiX.admin.service.CaptainSelectionService captainSelectionService;
    private final jjcet.PragatiX.admin.service.TeamMapper teamMapper;
    private final jjcet.PragatiX.repository.StageTeamRepository stageTeamRepository;
    private final jjcet.PragatiX.admin.service.TeamCleanupService teamCleanupService;
    private final jjcet.PragatiX.admin.service.LeadershipSyncService leadershipSyncService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public TeamMemberService(TeamRepository teamRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            TeamValidationService validationService,
            jjcet.PragatiX.admin.service.CaptainSelectionService captainSelectionService,
            jjcet.PragatiX.admin.service.TeamMapper teamMapper,
            jjcet.PragatiX.repository.StageTeamRepository stageTeamRepository,
            jjcet.PragatiX.admin.service.TeamCleanupService teamCleanupService,
            jjcet.PragatiX.admin.service.LeadershipSyncService leadershipSyncService) {
        this.teamRepository = teamRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.validationService = validationService;
        this.captainSelectionService = captainSelectionService;
        this.teamMapper = teamMapper;
        this.stageTeamRepository = stageTeamRepository;
        this.teamCleanupService = teamCleanupService;
        this.leadershipSyncService = leadershipSyncService;
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> addMemberToTeam(Long id, String regNo) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        Team team = teamRepository.findById(id).orElse(null);
        if (team == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        try {
            validationService.validateTeamAccess(currentUser, team);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        }

        Student member = studentRepository.findByRegNo(regNo).orElse(null);
        if (member == null)
            return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + regNo));
        if (member.getTeam() != null || !teamRepository.findAllTeamsByStudentId(member.getId()).isEmpty())
            return ResponseEntity.badRequest().body(ApiResponse
                    .error("Student " + member.getFullName() + " already belongs to an existing team."));

        long currentMembersCount = team.getMembers().size();
        boolean captainInMembers = team.getMembers().stream()
                .anyMatch(m -> team.getCaptain() != null && m.getId().equals(team.getCaptain().getId()));
        long totalSize = currentMembersCount + (captainInMembers ? 0 : 1) + 1;
        if (totalSize > team.getSize()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Cannot add member. Team size limit of " + team.getSize() + " exceeded."));
        }

        member.setTeam(team);
        studentRepository.save(member);
        team.getMembers().add(member);
        teamRepository.save(team);

        try {
            if (entityManager != null) {
                entityManager.createNativeQuery(
                        "INSERT INTO team_members (team_id, student_id) VALUES (:tid, :sid) " +
                                "ON DUPLICATE KEY UPDATE team_id = :tid")
                        .setParameter("tid", team.getId())
                        .setParameter("sid", member.getId())
                        .executeUpdate();
            }
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok(ApiResponse.ok("Member added successfully", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<jjcet.PragatiX.dto.TeamResponse>> removeMemberFromTeam(Long id, String regNo) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        Team team = teamRepository.findById(id).orElse(null);
        if (team == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        try {
            validationService.validateTeamAccess(currentUser, team);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        }

        Student member = studentRepository.findByRegNo(regNo).orElse(null);
        if (member == null)
            return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + regNo));
        if (member.getTeam() == null || !member.getTeam().getId().equals(team.getId()))
            return ResponseEntity.badRequest().body(ApiResponse.error("Student is not a member of this team"));

        boolean wasCaptain = team.getCaptain() != null && member.getId().equals(team.getCaptain().getId());
        boolean wasViceCaptain = team.getViceCaptain() != null && member.getId().equals(team.getViceCaptain().getId());

        if (wasCaptain) {
            team.setCaptain(null);
        }
        if (wasViceCaptain) {
            team.setViceCaptain(null);
        }

        // Clean up StageTeam leadership if held by this student
        List<jjcet.PragatiX.entity.StageTeam> stageTeams = stageTeamRepository.findByTeamId(team.getId());
        for (jjcet.PragatiX.entity.StageTeam st : stageTeams) {
            if (st.getCaptain() != null && st.getCaptain().getId().equals(member.getId())) {
                st.setCaptain(null);
                stageTeamRepository.save(st);
            }
            if (st.getViceCaptain() != null && st.getViceCaptain().getId().equals(member.getId())) {
                st.setViceCaptain(null);
                stageTeamRepository.save(st);
            }
        }

        team.getMembers().remove(member);
        member.setTeam(null);
        studentRepository.save(member);

        try {
            if (entityManager != null) {
                entityManager.createNativeQuery(
                        "DELETE FROM team_members WHERE student_id = :sid AND team_id = :tid")
                        .setParameter("sid", member.getId())
                        .setParameter("tid", team.getId())
                        .executeUpdate();
            }
        } catch (Exception ignored) {
        }

        if (teamCleanupService.autoDeleteEmptyTeam(team)) {
            return ResponseEntity
                    .ok(ApiResponse.ok("Member removed successfully and empty team auto-deleted", null));
        }

        if (wasCaptain) {
            captainSelectionService.evaluateCaptainForTeam(team);
        }

        teamRepository.save(team);

        return ResponseEntity
                .ok(ApiResponse.ok("Member removed successfully", teamMapper.toTeamResponse(team)));
    }

    @Transactional
    public ResponseEntity<ApiResponse<jjcet.PragatiX.dto.TeamResponse>> assignTeamCaptain(Long id, String regNo) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        Team team = teamRepository.findById(id).orElse(null);
        if (team == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        try {
            validationService.validateTeamAccess(currentUser, team);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        }

        Student captain = studentRepository.findByRegNo(regNo).orElse(null);
        if (captain == null)
            return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + regNo));
        if (captain.getTeam() != null && !captain.getTeam().getId().equals(team.getId()))
            return ResponseEntity.badRequest().body(ApiResponse
                    .error("Student is already assigned to a different team: " + captain.getTeam().getName()));

        if (team.getViceCaptain() != null && team.getViceCaptain().getId().equals(captain.getId())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("A student cannot hold both Captain and Vice Captain roles in the same team."));
        }

        captain.setTeam(team);
        studentRepository.save(captain);
        if (!team.getMembers().contains(captain)) {
            team.getMembers().add(captain);
        }
        leadershipSyncService.syncLeadership(team, captain, team.getViceCaptain());
        return ResponseEntity
                .ok(ApiResponse.ok("Student assigned as Team Captain successfully", teamMapper.toTeamResponse(team)));
    }

    @Transactional
    public ResponseEntity<ApiResponse<jjcet.PragatiX.dto.TeamResponse>> removeTeamCaptain(Long id) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        Team team = teamRepository.findById(id).orElse(null);
        if (team == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        try {
            validationService.validateTeamAccess(currentUser, team);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        }

        leadershipSyncService.syncLeadership(team, null, team.getViceCaptain());
        return ResponseEntity.ok(ApiResponse.ok("Team Captain removed successfully", teamMapper.toTeamResponse(team)));
    }

    @Transactional
    public ResponseEntity<ApiResponse<jjcet.PragatiX.dto.TeamResponse>> assignTeamViceCaptain(Long id, String regNo) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        Team team = teamRepository.findById(id).orElse(null);
        if (team == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        try {
            validationService.validateTeamAccess(currentUser, team);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        }

        Student viceCaptain = studentRepository.findByRegNo(regNo).orElse(null);
        if (viceCaptain == null)
            return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + regNo));
        if (viceCaptain.getTeam() != null && !viceCaptain.getTeam().getId().equals(team.getId()))
            return ResponseEntity.badRequest().body(ApiResponse
                    .error("Student is already assigned to a different team: " + viceCaptain.getTeam().getName()));

        if (team.getCaptain() != null && team.getCaptain().getId().equals(viceCaptain.getId())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("A student cannot hold both Captain and Vice Captain roles in the same team."));
        }

        viceCaptain.setTeam(team);
        studentRepository.save(viceCaptain);
        if (!team.getMembers().contains(viceCaptain)) {
            team.getMembers().add(viceCaptain);
        }
        leadershipSyncService.syncLeadership(team, team.getCaptain(), viceCaptain);
        return ResponseEntity.ok(
                ApiResponse.ok("Student assigned as Team Vice Captain successfully", teamMapper.toTeamResponse(team)));
    }

    @Transactional
    public ResponseEntity<ApiResponse<jjcet.PragatiX.dto.TeamResponse>> removeTeamViceCaptain(Long id) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        Team team = teamRepository.findById(id).orElse(null);
        if (team == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        try {
            validationService.validateTeamAccess(currentUser, team);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        }

        leadershipSyncService.syncLeadership(team, team.getCaptain(), null);
        return ResponseEntity
                .ok(ApiResponse.ok("Team Vice Captain removed successfully", teamMapper.toTeamResponse(team)));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> addMemberByStudent(Student captain, String regNo) {
        Team team = captain.getTeam();
        if (team == null || team.getCaptain() == null || !team.getCaptain().getId().equals(captain.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not the captain of any team"));
        }

        Student member = studentRepository.findByRegNo(regNo).orElse(null);
        if (member == null)
            return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + regNo));
        if (member.getTeam() != null || !teamRepository.findAllTeamsByStudentId(member.getId()).isEmpty())
            return ResponseEntity.badRequest().body(ApiResponse
                    .error("Student " + member.getFullName() + " already belongs to an existing team."));

        long currentMembersCount = team.getMembers().size();
        boolean captainInMembers = team.getMembers().stream()
                .anyMatch(m -> m.getId().equals(team.getCaptain().getId()));
        long totalSize = currentMembersCount + (captainInMembers ? 0 : 1) + 1;
        if (totalSize > team.getSize()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Cannot add member. Team size limit of " + team.getSize() + " exceeded."));
        }

        member.setTeam(team);
        studentRepository.save(member);
        team.getMembers().add(member);
        teamRepository.save(team);

        try {
            if (entityManager != null) {
                entityManager.createNativeQuery(
                        "INSERT INTO team_members (team_id, student_id) VALUES (:tid, :sid) " +
                                "ON DUPLICATE KEY UPDATE team_id = :tid")
                        .setParameter("tid", team.getId())
                        .setParameter("sid", member.getId())
                        .executeUpdate();
            }
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok(ApiResponse.ok("Member added successfully", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> addMemberByCC(Long id, String regNo) {
        return addMemberToTeam(id, regNo);
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> addMembersByCC(Long id, java.util.List<String> regNos) {
        return addMembersToTeam(id, regNos);
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> addMembersToTeam(Long id, java.util.List<String> regNos) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        Team team = teamRepository.findById(id).orElse(null);
        if (team == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        try {
            validationService.validateTeamAccess(currentUser, team);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        }

        long currentMembersCount = team.getMembers().size();
        boolean captainInMembers = team.getMembers().stream()
                .anyMatch(m -> team.getCaptain() != null && m.getId().equals(team.getCaptain().getId()));
        long totalSize = currentMembersCount + (captainInMembers ? 0 : 1);

        if (totalSize + regNos.size() > team.getSize()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Cannot add members. Team size limit of " + team.getSize() + " exceeded."));
        }

        for (String regNo : regNos) {
            Student member = studentRepository.findByRegNo(regNo).orElse(null);
            if (member == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + regNo));
            }
            if (member.getTeam() != null || !teamRepository.findAllTeamsByStudentId(member.getId()).isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse
                        .error("Student " + member.getFullName() + " already belongs to an existing team."));
            }

            // Validate configuration match
            if (team.getYear() != null && member.getYear() != null && !team.getYear().equals(member.getYear())) {
                return ResponseEntity.badRequest().body(ApiResponse
                        .error("Student " + member.getFullName() + " is in a different academic year than the team."));
            }
            if (team.getDepartment() != null && member.getDepartment() != null
                    && !team.getDepartment().getId().equals(member.getDepartment().getId())) {
                return ResponseEntity.badRequest().body(ApiResponse
                        .error("Student " + member.getFullName() + " is in a different department than the team."));
            }
            if (team.getSection() != null && member.getSection() != null
                    && !team.getSection().getId().equals(member.getSection().getId())) {
                return ResponseEntity.badRequest().body(ApiResponse
                        .error("Student " + member.getFullName() + " is in a different section than the team."));
            }

            member.setTeam(team);
            studentRepository.save(member);
            team.getMembers().add(member);

            try {
                if (entityManager != null) {
                    entityManager.createNativeQuery(
                            "INSERT INTO team_members (team_id, student_id) VALUES (:tid, :sid) " +
                                    "ON DUPLICATE KEY UPDATE team_id = :tid")
                            .setParameter("tid", team.getId())
                            .setParameter("sid", member.getId())
                            .executeUpdate();
                }
            } catch (Exception ignored) {
            }
        }

        teamRepository.save(team);
        return ResponseEntity.ok(ApiResponse.ok("Members added successfully", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<jjcet.PragatiX.dto.TeamResponse>> removeMemberByCC(Long id, String regNo) {
        return removeMemberFromTeam(id, regNo);
    }

}
