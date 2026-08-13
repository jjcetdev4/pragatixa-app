package jjcet.PragatiX.admin.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.dto.TeamRemovalRequestDto;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.Team;
import jjcet.PragatiX.entity.StageTeam;
import jjcet.PragatiX.entity.TeamRemovalRequest;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.activity.service.AssignmentSecurityService;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.repository.TeamRemovalRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamRequestService {

    private final TeamRemovalRequestRepository teamRemovalRequestRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AssignmentSecurityService assignmentSecurityService;
    private final TeamMapper mapper;
    private final AuthUtils authUtils;
    private final jjcet.PragatiX.repository.TeamRepository teamRepository;
    private final jjcet.PragatiX.repository.StageTeamRepository stageTeamRepository;
    private final TeamCleanupService teamCleanupService;
    private final CaptainSelectionService captainSelectionService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public TeamRequestService(TeamRemovalRequestRepository teamRemovalRequestRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            AssignmentSecurityService assignmentSecurityService,
            TeamMapper mapper,
            AuthUtils authUtils,
            jjcet.PragatiX.repository.TeamRepository teamRepository,
            jjcet.PragatiX.repository.StageTeamRepository stageTeamRepository,
            TeamCleanupService teamCleanupService,
            CaptainSelectionService captainSelectionService) {
        this.teamRemovalRequestRepository = teamRemovalRequestRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.assignmentSecurityService = assignmentSecurityService;
        this.mapper = mapper;
        this.authUtils = authUtils;
        this.teamRepository = teamRepository;
        this.stageTeamRepository = stageTeamRepository;
        this.teamCleanupService = teamCleanupService;
        this.captainSelectionService = captainSelectionService;
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> requestRemoveMember(Student captain, String regNo, String reason) {
        Team team = captain.getTeam();
        if (team == null || team.getCaptain() == null || !team.getCaptain().getId().equals(captain.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not the captain of any team"));
        }

        Student member = studentRepository.findByRegNo(regNo).orElse(null);
        if (member == null)
            return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + regNo));
        if (member.getTeam() == null || !member.getTeam().getId().equals(team.getId()))
            return ResponseEntity.badRequest().body(ApiResponse.error("Student is not a member of your team"));
        if (member.getId().equals(captain.getId()))
            return ResponseEntity.badRequest().body(ApiResponse.error("You cannot remove yourself from the team"));
        if (teamRemovalRequestRepository.existsByTeamIdAndStudentRegNoAndStatus(team.getId(), regNo, "PENDING")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("A pending removal request already exists for this student"));
        }

        TeamRemovalRequest request = new TeamRemovalRequest(team, member, captain, reason, "PENDING");
        teamRemovalRequestRepository.save(request);
        return ResponseEntity.ok(ApiResponse.ok("Removal request sent to Assigned Faculty successfully", null));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<TeamRemovalRequestDto>>> getPendingRemovalRequests(String username) {
        User currentUser = userRepository.findByUsername(username).orElse(null);
        List<TeamRemovalRequest> requests = teamRemovalRequestRepository.findByStatus("PENDING");

        if (currentUser != null) {
            if (authUtils.isAdmin(currentUser) && !authUtils.isSuperAdmin(currentUser)) {
                String adminYear = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
                if (adminYear != null) {
                    requests = requests.stream()
                            .filter(req -> adminYear.equals(req.getTeam().getYear()))
                            .collect(Collectors.toList());
                } else {
                    requests = java.util.Collections.emptyList();
                }
            } else if (!authUtils.isSuperAdmin(currentUser) && !authUtils.isAdmin(currentUser)) {
                requests = requests.stream()
                        .filter(req -> {
                            boolean isCC = currentUser.getSubRoles().stream()
                                    .anyMatch(sr -> sr.getName().equalsIgnoreCase("CC"));
                            return isCC && req.getTeam().getDepartment() != null
                                    && req.getTeam().getDepartment().getId().equals(currentUser.getDepartment().getId())
                                    && req.getTeam().getYear().equals(currentUser.getYear())
                                    && req.getTeam().getSection() != null
                                    && req.getTeam().getSection().getId().equals(currentUser.getSection().getId());
                        })
                        .collect(Collectors.toList());
            }
        }

        List<TeamRemovalRequestDto> dtos = requests.stream().map(mapper::toTeamRemovalRequestDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Pending requests retrieved", dtos));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> approveRemovalRequest(Long id, String username) {
        TeamRemovalRequest request = teamRemovalRequestRepository.findById(id).orElse(null);
        if (request == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Request not found"));
        if (!"PENDING".equals(request.getStatus()))
            return ResponseEntity.badRequest().body(ApiResponse.error("Request is not pending"));

        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser != null) {
            if (authUtils.isAdmin(currentUser) && !authUtils.isSuperAdmin(currentUser)) {
                String adminYear = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
                if (adminYear == null || !adminYear.equals(request.getTeam().getYear())) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(ApiResponse.error(
                            "Access Denied: You are not authorized to approve this request for this academic year."));
                }
            } else if (!authUtils.isSuperAdmin(currentUser) && !authUtils.isAdmin(currentUser)) {
                boolean isCC = currentUser.getSubRoles().stream().anyMatch(sr -> sr.getName().equalsIgnoreCase("CC"));
                boolean isAuthorized = isCC && request.getTeam().getDepartment() != null
                        && request.getTeam().getDepartment().getId().equals(currentUser.getDepartment().getId())
                        && request.getTeam().getYear().equals(currentUser.getYear())
                        && request.getTeam().getSection() != null
                        && request.getTeam().getSection().getId().equals(currentUser.getSection().getId());
                if (!isAuthorized) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                            .body(ApiResponse.error("Access Denied: You are not authorized to approve this request."));
                }
            }
        }

        Student member = request.getStudent();
        Team team = request.getTeam();

        if (member != null && team != null) {
            boolean wasCaptain = team.getCaptain() != null && team.getCaptain().getId().equals(member.getId());
            boolean wasViceCaptain = team.getViceCaptain() != null
                    && team.getViceCaptain().getId().equals(member.getId());

            if (wasCaptain) {
                team.setCaptain(null);
            }
            if (wasViceCaptain) {
                team.setViceCaptain(null);
            }

            // Clear from all associated StageTeam records
            List<StageTeam> stageTeams = stageTeamRepository.findByTeamId(team.getId());
            for (StageTeam st : stageTeams) {
                if (st.getCaptain() != null && st.getCaptain().getId().equals(member.getId())) {
                    st.setCaptain(null);
                    stageTeamRepository.save(st);
                }
                if (st.getViceCaptain() != null && st.getViceCaptain().getId().equals(member.getId())) {
                    st.setViceCaptain(null);
                    stageTeamRepository.save(st);
                }
            }

            if (team.getMembers() != null) {
                team.getMembers().remove(member);
            }

            if (member.getTeam() != null && member.getTeam().getId().equals(team.getId())) {
                member.setTeam(null);
                studentRepository.save(member);
            }

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

            if (!teamCleanupService.autoDeleteEmptyTeam(team)) {
                if (wasCaptain) {
                    captainSelectionService.evaluateCaptainForTeam(team);
                }
                teamRepository.save(team);
            }
        }

        request.setStatus("APPROVED");
        teamRemovalRequestRepository.save(request);
        return ResponseEntity.ok(ApiResponse.ok("Request approved and student removed", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> rejectRemovalRequest(Long id, String username) {
        TeamRemovalRequest request = teamRemovalRequestRepository.findById(id).orElse(null);
        if (request == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Request not found"));
        if (!"PENDING".equals(request.getStatus()))
            return ResponseEntity.badRequest().body(ApiResponse.error("Request is not pending"));

        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser != null) {
            if (authUtils.isAdmin(currentUser) && !authUtils.isSuperAdmin(currentUser)) {
                String adminYear = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
                if (adminYear == null || !adminYear.equals(request.getTeam().getYear())) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(ApiResponse.error(
                            "Access Denied: You are not authorized to reject this request for this academic year."));
                }
            } else if (!authUtils.isSuperAdmin(currentUser) && !authUtils.isAdmin(currentUser)) {
                boolean isCC = currentUser.getSubRoles().stream().anyMatch(sr -> sr.getName().equalsIgnoreCase("CC"));
                boolean isAuthorized = isCC && request.getTeam().getDepartment() != null
                        && request.getTeam().getDepartment().getId().equals(currentUser.getDepartment().getId())
                        && request.getTeam().getYear().equals(currentUser.getYear())
                        && request.getTeam().getSection() != null
                        && request.getTeam().getSection().getId().equals(currentUser.getSection().getId());
                if (!isAuthorized) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                            .body(ApiResponse.error("Access Denied: You are not authorized to reject this request."));
                }
            }
        }

        request.setStatus("REJECTED");
        teamRemovalRequestRepository.save(request);
        return ResponseEntity.ok(ApiResponse.ok("Request rejected", null));
    }
}
