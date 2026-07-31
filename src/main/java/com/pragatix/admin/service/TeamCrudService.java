package com.pragatix.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.dto.CreateTeamRequest;
import com.pragatix.dto.TeamResponse;
import com.pragatix.entity.*;
import com.pragatix.repository.ActivityAssignmentRepository;
import com.pragatix.modules.authentication.repository.UserRepository;
import com.pragatix.modules.student.dto.response.StudentResponse;
import com.pragatix.modules.student.repository.StudentActivityXpRepository;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.repository.GroupDeletionAuditLogRepository;
import com.pragatix.repository.TeamRemovalRequestRepository;
import com.pragatix.repository.TeamRepository;
import com.pragatix.repository.StageTeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TeamCrudService {

    private static final Logger log = LoggerFactory.getLogger(TeamCrudService.class);

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final StudentActivityXpRepository studentActivityXpRepository;
    private final GroupDeletionAuditLogRepository auditLogRepository;
    private final TeamRemovalRequestRepository teamRemovalRequestRepository;
    private final TeamValidationService validationService;
    private final TeamMapper mapper;
    private final StageTeamRepository stageTeamRepository;

    public TeamCrudService(TeamRepository teamRepository,
            UserRepository userRepository,
            StudentRepository studentRepository,
            ActivityAssignmentRepository activityAssignmentRepository,
            StudentActivityXpRepository studentActivityXpRepository,
            GroupDeletionAuditLogRepository auditLogRepository,
            TeamRemovalRequestRepository teamRemovalRequestRepository,
            TeamValidationService validationService,
            TeamMapper mapper,
            StageTeamRepository stageTeamRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.auditLogRepository = auditLogRepository;
        this.teamRemovalRequestRepository = teamRemovalRequestRepository;
        this.validationService = validationService;
        this.mapper = mapper;
        this.stageTeamRepository = stageTeamRepository;
    }

    @Transactional
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(CreateTeamRequest request, String username) {
        log.debug("Creating Team with name: {}", request.getName());

        Student studentAttempt = studentRepository.findByRegNo(username).orElse(null);
        if (studentAttempt != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access Denied: Students are not allowed to create groups."));
        }

        User creator = userRepository.findByUsername(username).orElse(null);
        if (creator == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        ActivityAssignment assignment = null;
        if (request.getAssignmentId() != null) {
            assignment = activityAssignmentRepository.findById(request.getAssignmentId()).orElse(null);
        }

        if (!validationService.canCreateTeam(creator, assignment)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(
                    "Access Denied: Only Assigned Faculty, Class Coordinators (CC), or Admins can create teams."));
        }

        if (request.getCaptainStudentId() == null || request.getCaptainStudentId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Captain Student ID is required."));
        }

        Student captain = studentRepository.findByRegNo(request.getCaptainStudentId()).orElse(null);
        if (captain == null)
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Captain student not found with ID: " + request.getCaptainStudentId()));
        if (captain.getTeam() != null)
            return ResponseEntity.badRequest().body(ApiResponse.error("Proposed Captain " + captain.getFullName()
                    + " is already assigned to team: " + captain.getTeam().getName()));

        if (teamRepository.existsByName(request.getName())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Team name '" + request.getName() + "' already exists."));
        }

        List<Student> members = new ArrayList<>();
        if (request.getMemberStudentIds() != null && !request.getMemberStudentIds().isEmpty()) {
            List<String> validIds = new java.util.ArrayList<>();
            for (String sid : request.getMemberStudentIds()) {
                if (!sid.trim().equalsIgnoreCase(captain.getRegNo().trim())) {
                    validIds.add(sid);
                }
            }
            if (!validIds.isEmpty()) {
                List<Student> fetchedMembers = studentRepository.findByRegNoIn(validIds);
                if (fetchedMembers.size() < validIds.size()) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("One or more member students not found."));
                }
                for (Student m : fetchedMembers) {
                    if (m.getTeam() != null)
                        return ResponseEntity.badRequest().body(ApiResponse.error("Student " + m.getFullName()
                                + " is already assigned to team: " + m.getTeam().getName()));
                    members.add(m);
                }
            }
        }

        int totalSize = 1 + members.size();
        if (totalSize > request.getSize()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot add " + totalSize
                    + " members (including captain) because the team size limit is " + request.getSize() + "."));
        }

        Team team = Team.builder()
                .name(request.getName())
                .size(request.getSize())
                .captain(captain)
                .department(captain.getDepartment())
                .year(captain.getYear())
                .section(captain.getSection())
                .createdBy(creator)
                .build();
        Team savedTeam = teamRepository.save(team);
        log.debug("Saved Team: {}", savedTeam.getName());

        captain.setTeam(savedTeam);
        for (Student m : members) {
            m.setTeam(savedTeam);
        }
        List<Student> toSave = new ArrayList<>(members);
        toSave.add(captain);
        studentRepository.saveAll(toSave);

        List<StudentResponse> studentResponses = new ArrayList<>();
        studentResponses.add(mapper.toStudentResponse(captain));
        for (Student m : members)
            studentResponses.add(mapper.toStudentResponse(m));

        TeamResponse response = mapper.toTeamResponse(savedTeam);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Team created successfully", response));
    }

    @Transactional
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(Long id, CreateTeamRequest request) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser != null) {
            try {
                validationService.validateTeamAccess(currentUser, team);
            } catch (org.springframework.security.access.AccessDeniedException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
            }
        }

        if (!team.getName().equalsIgnoreCase(request.getName()) && teamRepository.existsByName(request.getName())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Team name '" + request.getName() + "' already exists."));
        }

        team.setName(request.getName());

        long currentMembersCount = team.getMembers().size();
        boolean captainInMembers = team.getMembers().stream()
                .anyMatch(m -> team.getCaptain() != null && m.getId().equals(team.getCaptain().getId()));
        long totalSize = currentMembersCount + (captainInMembers ? 0 : 1);
        if (request.getSize() < totalSize) {
            return ResponseEntity.badRequest().body(ApiResponse
                    .error("New limit cannot be less than the current number of members (" + totalSize + ")"));
        }

        team.setSize(request.getSize());
        teamRepository.save(team);
        return ResponseEntity.ok(ApiResponse.ok("Team updated successfully", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> updateTeamLimit(Long id, int size) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser != null) {
            try {
                validationService.validateTeamAccess(currentUser, team);
            } catch (org.springframework.security.access.AccessDeniedException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
            }
        }

        long currentMembersCount = team.getMembers().size();
        boolean captainInMembers = team.getCaptain() != null
                && team.getMembers().stream().anyMatch(m -> m.getId().equals(team.getCaptain().getId()));
        long totalSize = currentMembersCount + (captainInMembers ? 0 : 1);
        if (size < totalSize) {
            return ResponseEntity.badRequest().body(ApiResponse
                    .error("New limit cannot be less than the current number of members (" + totalSize + ")"));
        }

        team.setSize(size);
        teamRepository.save(team);
        return ResponseEntity.ok(ApiResponse.ok("Team limit updated successfully", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteTeam(Long teamId, String username) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        try {
            validationService.validateTeamAccess(currentUser, team);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        }

        boolean isOnlyCaptain = team.getMembers().isEmpty() && team.getCaptain() != null;
        boolean isEmpty = team.getMembers().isEmpty() && team.getCaptain() == null;

        if (!isEmpty && !isOnlyCaptain) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Cannot delete team because it still contains students."));
        }

        if (isOnlyCaptain) {
            Student captain = team.getCaptain();
            team.getMembers().remove(captain);
            captain.setTeam(null);
            studentRepository.save(captain);
            team.setCaptain(null);
        }

        // Remove StageTeam mappings
        List<StageTeam> stageTeams = stageTeamRepository.findByTeamId(teamId);
        stageTeamRepository.deleteAll(stageTeams);

        teamRemovalRequestRepository.deleteAll(teamRemovalRequestRepository.findByTeamId(teamId));

        String teamName = team.getName();
        teamRepository.delete(team);

        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
        boolean isAssignedFaculty = false; // We can't determine this globally without an assignment context
        String roleStr = isAdmin ? "ADMIN" : (isAssignedFaculty ? "ASSIGNED_FACULTY" : "CC");

        GroupDeletionAuditLog auditLog = new GroupDeletionAuditLog(
                teamId, teamName, username, roleStr, "User initiated deletion", java.time.LocalDateTime.now());
        auditLogRepository.save(auditLog);
        return ResponseEntity.ok(ApiResponse.ok("Group deleted successfully", null));
    }
}
