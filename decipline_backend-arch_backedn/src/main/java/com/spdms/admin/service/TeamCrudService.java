package com.spdms.admin.service;

import com.spdms.common.response.ApiResponse;
import com.spdms.dto.CreateTeamRequest;
import com.spdms.dto.TeamResponse;
import com.spdms.entity.*;
import com.spdms.repository.ActivityAssignmentRepository;
import com.spdms.modules.authentication.repository.UserRepository;
import com.spdms.modules.student.dto.response.StudentResponse;
import com.spdms.modules.student.repository.StudentActivityXpRepository;
import com.spdms.modules.student.repository.StudentRepository;
import com.spdms.repository.GroupDeletionAuditLogRepository;
import com.spdms.repository.TeamRemovalRequestRepository;
import com.spdms.repository.TeamRepository;
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

    public TeamCrudService(TeamRepository teamRepository,
                           UserRepository userRepository,
                           StudentRepository studentRepository,
                           ActivityAssignmentRepository activityAssignmentRepository,
                           StudentActivityXpRepository studentActivityXpRepository,
                           GroupDeletionAuditLogRepository auditLogRepository,
                           TeamRemovalRequestRepository teamRemovalRequestRepository,
                           TeamValidationService validationService,
                           TeamMapper mapper) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.auditLogRepository = auditLogRepository;
        this.teamRemovalRequestRepository = teamRemovalRequestRepository;
        this.validationService = validationService;
        this.mapper = mapper;
    }

    @Transactional
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(CreateTeamRequest request, String username) {
        log.debug("Creating Team with name: {}", request.getName());
        
        Student studentAttempt = studentRepository.findByStudentId(username).orElse(null);
        if (studentAttempt != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access Denied: Students are not allowed to create groups."));
        }

        User creator = userRepository.findByUsername(username).orElse(null);
        if (creator == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        ActivityAssignment assignment = null;
        if (request.getAssignmentId() != null) {
            assignment = activityAssignmentRepository.findById(request.getAssignmentId()).orElse(null);
        }

        if (!validationService.canCreateTeam(creator, assignment)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access Denied: Only Assigned Faculty, Class Coordinators (CC), or Admins can create teams."));
        }

        if (request.getCaptainStudentId() == null || request.getCaptainStudentId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Captain Student ID is required."));
        }

        Student captain = studentRepository.findByStudentId(request.getCaptainStudentId()).orElse(null);
        if (captain == null) return ResponseEntity.badRequest().body(ApiResponse.error("Captain student not found with ID: " + request.getCaptainStudentId()));
        if (captain.getTeam() != null) return ResponseEntity.badRequest().body(ApiResponse.error("Proposed Captain " + captain.getFullName() + " is already assigned to team: " + captain.getTeam().getName()));

        if (teamRepository.existsByName(request.getName())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Team name '" + request.getName() + "' already exists."));
        }

        List<Student> members = new ArrayList<>();
        if (request.getMemberStudentIds() != null && !request.getMemberStudentIds().isEmpty()) {
            List<String> validIds = new java.util.ArrayList<>();
            for (String sid : request.getMemberStudentIds()) {
                if (!sid.trim().equalsIgnoreCase(captain.getStudentId().trim())) {
                    validIds.add(sid);
                }
            }
            if (!validIds.isEmpty()) {
                List<Student> fetchedMembers = studentRepository.findByStudentIdIn(validIds);
                if (fetchedMembers.size() < validIds.size()) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("One or more member students not found."));
                }
                for (Student m : fetchedMembers) {
                    if (m.getTeam() != null) return ResponseEntity.badRequest().body(ApiResponse.error("Student " + m.getFullName() + " is already assigned to team: " + m.getTeam().getName()));
                    members.add(m);
                }
            }
        }

        int totalSize = 1 + members.size();
        if (totalSize > request.getSize()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot add " + totalSize + " members (including captain) because the team size limit is " + request.getSize() + "."));
        }

        Team team = Team.builder()
                .name(request.getName())
                .size(request.getSize())
                .captain(captain)
                .assignment(assignment)
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
        for (Student m : members) studentResponses.add(mapper.toStudentResponse(m));

        TeamResponse response = new TeamResponse(savedTeam.getId(), savedTeam.getName(), savedTeam.getSize(), captain.getStudentId(), captain.getFullName(), studentResponses);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Team created successfully", response));
    }

    @Transactional
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(Long id, CreateTeamRequest request) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        if (!team.getName().equalsIgnoreCase(request.getName()) && teamRepository.existsByName(request.getName())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Team name '" + request.getName() + "' already exists."));
        }

        team.setName(request.getName());

        long currentMembersCount = team.getMembers().size();
        boolean captainInMembers = team.getMembers().stream().anyMatch(m -> team.getCaptain() != null && m.getId().equals(team.getCaptain().getId()));
        long totalSize = currentMembersCount + (captainInMembers ? 0 : 1);
        if (request.getSize() < totalSize) {
            return ResponseEntity.badRequest().body(ApiResponse.error("New limit cannot be less than the current number of members (" + totalSize + ")"));
        }

        team.setSize(request.getSize());
        teamRepository.save(team);
        return ResponseEntity.ok(ApiResponse.ok("Team updated successfully", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> updateTeamLimit(Long id, int size) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        long currentMembersCount = team.getMembers().size();
        boolean captainInMembers = team.getCaptain() != null && team.getMembers().stream().anyMatch(m -> m.getId().equals(team.getCaptain().getId()));
        long totalSize = currentMembersCount + (captainInMembers ? 0 : 1);
        if (size < totalSize) {
            return ResponseEntity.badRequest().body(ApiResponse.error("New limit cannot be less than the current number of members (" + totalSize + ")"));
        }

        team.setSize(size);
        teamRepository.save(team);
        return ResponseEntity.ok(ApiResponse.ok("Team limit updated successfully", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteTeam(Long teamId, String username) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));

        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        ActivityAssignment assignment = team.getAssignment();
        if (assignment == null) return ResponseEntity.badRequest().body(ApiResponse.error("Team is missing assignment context"));

        if (!validationService.canDeleteTeam(currentUser, assignment)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access Denied: Only Admin, Assigned Faculty, or Class Coordinators of the section can delete this team."));
        }

        List<Student> allMembers = new ArrayList<>(team.getMembers());
        if (team.getCaptain() != null && !allMembers.contains(team.getCaptain())) allMembers.add(team.getCaptain());

        if (!allMembers.isEmpty() && studentActivityXpRepository.existsByAssignmentAndStudentIn(assignment, allMembers)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("This group already contains awarded activity records. Please archive or complete administrative cleanup before deletion."));
        }

        List<Student> studentsToUpdate = new ArrayList<>();
        if (team.getCaptain() != null) {
            Student captain = team.getCaptain();
            captain.setTeam(null);
            studentsToUpdate.add(captain);
        }

        for (Student member : team.getMembers()) {
            member.setTeam(null);
            studentsToUpdate.add(member);
        }
        
        if (!studentsToUpdate.isEmpty()) {
            studentRepository.saveAll(studentsToUpdate);
        }
        
        team.getMembers().clear();
        team.setCaptain(null);

        teamRemovalRequestRepository.deleteAll(teamRemovalRequestRepository.findByTeamId(teamId));

        String teamName = team.getName();
        teamRepository.delete(team);

        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
        boolean isAssignedFaculty = assignment.getTeacher() != null && assignment.getTeacher().getUsername().equals(username);
        String roleStr = isAdmin ? "ADMIN" : (isAssignedFaculty ? "ASSIGNED_FACULTY" : "CC");
        
        GroupDeletionAuditLog auditLog = new GroupDeletionAuditLog(
                teamId, teamName, username, roleStr, "User initiated deletion", java.time.LocalDateTime.now()
        );
        auditLogRepository.save(auditLog);
        return ResponseEntity.ok(ApiResponse.ok("Group deleted successfully", null));
    }
}
