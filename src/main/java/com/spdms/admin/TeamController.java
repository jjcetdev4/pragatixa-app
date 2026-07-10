package com.spdms.admin;

import com.spdms.dto.*;
import com.spdms.entity.*;
import com.spdms.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Teams", description = "Student team management for Class Coordinators (CC) and Students")
@SecurityRequirement(name = "bearerAuth")
public class TeamController {

    private static final Logger log = LoggerFactory.getLogger(TeamController.class);

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRemovalRequestRepository teamRemovalRequestRepository;

    public TeamController(TeamRepository teamRepository,
                          UserRepository userRepository,
                          StudentRepository studentRepository,
                          TeamMemberRepository teamMemberRepository,
                          TeamRemovalRequestRepository teamRemovalRequestRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamRemovalRequestRepository = teamRemovalRequestRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Create Team", description = "Creates a student team. Capable of being called by a student (who becomes captain) or an Admin/CC.")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        log.info("Creating Team with name: {}", request.getName());
        // 1. Get logged-in username and determine if student or CC
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        Student captain = studentRepository.findByStudentId(username).orElse(null);
        if (captain != null) {
            // Logged in as student - they are the captain of the team they create
            if (captain.getTeam() != null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("You are already assigned to team: " + captain.getTeam().getName()));
            }
        } else {
            // Logged in as Staff/Admin - verify CC or Admin role
            User creator = userRepository.findByUsername(username).orElse(null);
            if (creator == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
            }
            boolean isCcOrAdmin = creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"))
                    || creator.getSubRoles().stream().map(SubRole::getName)
                            .anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));

            if (!isCcOrAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse
                        .error("Access Denied: Only Class Coordinators (CC) or students can create teams."));
            }

            if (request.getCaptainStudentId() == null || request.getCaptainStudentId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Captain Student ID is required."));
            }

            captain = studentRepository.findByStudentId(request.getCaptainStudentId()).orElse(null);
            if (captain == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Captain student not found with ID: " + request.getCaptainStudentId()));
            }
            if (captain.getTeam() != null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Proposed Captain " + captain.getFullName()
                        + " is already assigned to team: " + captain.getTeam().getName()));
            }
        }

        // 2. Validate team name duplication
        if (teamRepository.existsByName(request.getName())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Team name '" + request.getName() + "' already exists."));
        }

        // 3. Find and validate Members
        List<Student> members = new ArrayList<>();
        if (request.getMemberStudentIds() != null && !request.getMemberStudentIds().isEmpty()) {
            for (String sid : request.getMemberStudentIds()) {
                // Prevent captain from being added as a member again
                if (sid.trim().equalsIgnoreCase(captain.getStudentId().trim())) {
                    continue;
                }
                Student m = studentRepository.findByStudentId(sid).orElse(null);
                if (m == null) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("Member student not found with ID: " + sid));
                }
                if (m.getTeam() != null) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(
                            "Student " + m.getFullName() + " is already assigned to team: " + m.getTeam().getName()));
                }
                members.add(m);
            }
        }

        // 5. Size Validation (Captain is included in size)
        int totalSize = 1 + members.size();
        if (totalSize > request.getSize()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot add " + totalSize
                    + " members (including captain) because the team size limit is " + request.getSize() + "."));
        }

        // 6. Create Team
        Team team = Team.builder()
                .name(request.getName())
                .size(request.getSize())
                .captain(captain)
                .build();

        Team savedTeam = teamRepository.save(team);
        log.info("Saved Team: {}", savedTeam.getName());

        // 7. Allot team reference to captain and members
        captain.setTeam(savedTeam);
        studentRepository.save(captain);
        log.info("Assigned student {} to Team {}", captain.getFullName(), savedTeam.getName());

        for (Student m : members) {
            m.setTeam(savedTeam);
            studentRepository.save(m);
            log.info("Assigned student {} to Team {}", m.getFullName(), savedTeam.getName());
        }

        // Add captain and members to response list
        List<StudentResponse> studentResponses = new ArrayList<>();
        studentResponses.add(toStudentResponse(captain));
        for (Student m : members) {
            studentResponses.add(toStudentResponse(m));
        }

        TeamResponse response = new TeamResponse(
                savedTeam.getId(),
                savedTeam.getName(),
                savedTeam.getSize(),
                captain.getStudentId(),
                captain.getFullName(),
                studentResponses);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Team created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional(readOnly = true)
    @Operation(summary = "List Teams")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getAllTeams() {
        List<Team> teams = teamRepository.findAll();
        List<TeamResponse> responses = teams.stream().map(g -> {
            List<StudentResponse> studentResponses = g.getMembers().stream()
                    .map(this::toStudentResponse)
                    .collect(Collectors.toList());

            String captainId = g.getCaptain() != null ? g.getCaptain().getStudentId() : null;
            String captainName = g.getCaptain() != null ? g.getCaptain().getFullName() : null;

            if (captainId != null) {
                boolean captainInMembers = studentResponses.stream()
                        .anyMatch(s -> s.getStudentId().equals(captainId));
                if (!captainInMembers) {
                    studentResponses.add(0, toStudentResponse(g.getCaptain()));
                }
            }

            return new TeamResponse(
                    g.getId(),
                    g.getName(),
                    g.getSize(),
                    captainId,
                    captainName,
                    studentResponses);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @GetMapping("/my-team")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get My Team", description = "Returns the team details for the logged-in student (captain/member).")
    public ResponseEntity<ApiResponse<TeamResponse>> getMyTeam() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Student student = studentRepository.findByStudentId(username).orElse(null);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Student not found"));
        }
        Team team = student.getTeam();
        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("You do not belong to any team"));
        }

        List<StudentResponse> studentResponses = team.getMembers().stream()
                .map(this::toStudentResponse)
                .collect(Collectors.toList());

        String captainId = team.getCaptain() != null ? team.getCaptain().getStudentId() : null;
        String captainName = team.getCaptain() != null ? team.getCaptain().getFullName() : null;

        if (captainId != null) {
            boolean captainInMembers = studentResponses.stream()
                    .anyMatch(s -> s.getStudentId().equals(captainId));
            if (!captainInMembers) {
                studentResponses.add(0, toStudentResponse(team.getCaptain()));
            }
        }

        TeamResponse response = new TeamResponse(
                team.getId(),
                team.getName(),
                team.getSize(),
                captainId,
                captainName,
                studentResponses);

        return ResponseEntity.ok(ApiResponse.ok("Team details retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER') or hasRole('STUDENT')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get Team by ID")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(@PathVariable Long id) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));
        }

        List<StudentResponse> studentResponses = team.getMembers().stream()
                .map(this::toStudentResponse)
                .collect(Collectors.toList());

        String captainId = team.getCaptain() != null ? team.getCaptain().getStudentId() : null;
        String captainName = team.getCaptain() != null ? team.getCaptain().getFullName() : null;

        if (captainId != null) {
            boolean captainInMembers = studentResponses.stream()
                    .anyMatch(s -> s.getStudentId().equals(captainId));
            if (!captainInMembers) {
                studentResponses.add(0, toStudentResponse(team.getCaptain()));
            }
        }

        TeamResponse response = new TeamResponse(
                team.getId(),
                team.getName(),
                team.getSize(),
                captainId,
                captainName,
                studentResponses);

        return ResponseEntity.ok(ApiResponse.ok("Team details retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional
    @Operation(summary = "Update Team")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(@PathVariable Long id, @Valid @RequestBody CreateTeamRequest request) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));
        }

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
        Team saved = teamRepository.save(team);
        log.info("Updated Team: {}", saved.getName());

        return ResponseEntity.ok(ApiResponse.ok("Team updated successfully", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional
    @Operation(summary = "Delete Team")
    public ResponseEntity<ApiResponse<Void>> deleteTeam(@PathVariable Long id) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));
        }

        log.info("Deleting Team ID = {}", id);
        long memberCount = teamMemberRepository.countByTeamId(id);
        log.info("Member Count = {}", memberCount);
        log.info("Captain = {}", team.getCaptain() != null ? team.getCaptain().getStudentId() : "null");
        log.info("Repository Result = {}", memberCount);

        if (memberCount > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Cannot delete team because it still contains members."));
        }

        teamRepository.delete(team);
        log.info("Deleted Team: {}", team.getName());

        return ResponseEntity.ok(ApiResponse.ok("Team deleted successfully", null));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional
    @Operation(summary = "Add Team Member by Team ID", description = "Adds a student to a team by team ID.")
    public ResponseEntity<ApiResponse<Void>> addMemberToTeam(@PathVariable Long id, @RequestParam String studentId) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));
        }

        Student member = studentRepository.findByStudentId(studentId).orElse(null);
        if (member == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + studentId));
        }

        if (member.getTeam() != null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student " + member.getFullName() + " is already in team: " + member.getTeam().getName()));
        }

        long currentMembersCount = team.getMembers().size();
        boolean captainInMembers = team.getMembers().stream().anyMatch(m -> team.getCaptain() != null && m.getId().equals(team.getCaptain().getId()));
        long totalSize = currentMembersCount + (captainInMembers ? 0 : 1) + 1;
        if (totalSize > team.getSize()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot add member. Team size limit of " + team.getSize() + " exceeded."));
        }

        member.setTeam(team);
        studentRepository.save(member);
        log.info("Assigned student {} to Team {}", member.getFullName(), team.getName());

        return ResponseEntity.ok(ApiResponse.ok("Member added successfully", null));
    }

    @DeleteMapping("/{id}/members/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional
    @Operation(summary = "Remove Team Member by Team ID", description = "Removes a student from a team by team ID.")
    public ResponseEntity<ApiResponse<Void>> removeMemberFromTeam(@PathVariable Long id, @PathVariable String studentId) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));
        }

        Student member = studentRepository.findByStudentId(studentId).orElse(null);
        if (member == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + studentId));
        }

        if (member.getTeam() == null || !member.getTeam().getId().equals(team.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student is not a member of this team"));
        }

        if (team.getCaptain() != null && member.getId().equals(team.getCaptain().getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("You cannot remove the captain from the team this way"));
        }

        member.setTeam(null);
        studentRepository.save(member);
        log.info("Removed student {} from Team {}", member.getFullName(), team.getName());

        return ResponseEntity.ok(ApiResponse.ok("Member removed successfully", null));
    }

    @PostMapping("/{id}/captain")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional
    @Operation(summary = "Assign Team Captain", description = "Assigns/promotes a student to captain of a team.")
    public ResponseEntity<ApiResponse<Void>> assignTeamCaptain(@PathVariable Long id, @RequestParam String studentId) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));
        }

        Student captain = studentRepository.findByStudentId(studentId).orElse(null);
        if (captain == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + studentId));
        }

        if (captain.getTeam() != null && !captain.getTeam().getId().equals(team.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student is already assigned to a different team: " + captain.getTeam().getName()));
        }

        captain.setTeam(team);
        studentRepository.save(captain);

        team.setCaptain(captain);
        teamRepository.save(team);
        log.info("Assigned student {} as Captain of Team {}", captain.getFullName(), team.getName());

        return ResponseEntity.ok(ApiResponse.ok("Student assigned as Team Captain successfully", null));
    }

    @PostMapping("/my-team/add-member")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Add Team Member", description = "Adds a student to the captain's team.")
    public ResponseEntity<ApiResponse<Void>> addMember(@RequestParam String studentId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Student captain = studentRepository.findByStudentId(username).orElse(null);
        if (captain == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Captain student not found"));
        }
        Team team = captain.getTeam();
        if (team == null || team.getCaptain() == null || !team.getCaptain().getId().equals(captain.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("You are not the captain of any team"));
        }

        Student member = studentRepository.findByStudentId(studentId).orElse(null);
        if (member == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + studentId));
        }

        if (member.getTeam() != null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student " + member.getFullName() + " is already in team: " + member.getTeam().getName()));
        }

        long currentMembersCount = team.getMembers().size();
        boolean captainInMembers = team.getMembers().stream().anyMatch(m -> m.getId().equals(team.getCaptain().getId()));
        long totalSize = currentMembersCount + (captainInMembers ? 0 : 1) + 1;
        if (totalSize > team.getSize()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot add member. Team size limit of " + team.getSize() + " exceeded."));
        }

        member.setTeam(team);
        studentRepository.save(member);
        log.info("Assigned student {} to Team {}", member.getFullName(), team.getName());

        return ResponseEntity.ok(ApiResponse.ok("Member added successfully", null));
    }

    @PostMapping("/{id}/add-member")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Add Team Member (CC)", description = "Adds a student to a specific team (CC/Admin only).")
    public ResponseEntity<ApiResponse<Void>> addMemberByCC(@PathVariable Long id, @RequestParam String studentId) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));
        }

        Student member = studentRepository.findByStudentId(studentId).orElse(null);
        if (member == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + studentId));
        }

        if (member.getTeam() != null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student " + member.getFullName() + " is already in team: " + member.getTeam().getName()));
        }

        long currentMembersCount = team.getMembers().size();
        boolean captainInMembers = team.getCaptain() != null && team.getMembers().stream().anyMatch(m -> m.getId().equals(team.getCaptain().getId()));
        long totalSize = currentMembersCount + (captainInMembers ? 0 : 1) + 1;
        if (totalSize > team.getSize()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot add member. Team size limit of " + team.getSize() + " exceeded."));
        }

        member.setTeam(team);
        studentRepository.save(member);
        log.info("CC added student {} to Team {}", member.getFullName(), team.getName());

        return ResponseEntity.ok(ApiResponse.ok("Member added successfully", null));
    }

    @PostMapping("/{id}/remove-member")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Remove Team Member (CC)", description = "Removes a student from a specific team (CC/Admin only).")
    public ResponseEntity<ApiResponse<Void>> removeMemberByCC(@PathVariable Long id, @RequestParam String studentId) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));
        }

        Student member = studentRepository.findByStudentId(studentId).orElse(null);
        if (member == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + studentId));
        }

        if (member.getTeam() == null || !member.getTeam().getId().equals(team.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student is not a member of this team"));
        }

        if (team.getCaptain() != null && member.getId().equals(team.getCaptain().getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot directly remove the captain. Reassign captaincy first."));
        }

        member.setTeam(null);
        studentRepository.save(member);
        log.info("CC removed student {} from Team {}", member.getFullName(), team.getName());

        return ResponseEntity.ok(ApiResponse.ok("Member removed successfully", null));
    }

    @PostMapping("/my-team/remove-request")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Request Team Member Removal", description = "Creates a request to remove a student from the captain's team.")
    public ResponseEntity<ApiResponse<Void>> requestRemoveMember(@RequestParam String studentId, @RequestParam(required = false, defaultValue = "Requested by Captain") String reason) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Student captain = studentRepository.findByStudentId(username).orElse(null);
        if (captain == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Captain student not found"));
        }
        Team team = captain.getTeam();
        if (team == null || team.getCaptain() == null || !team.getCaptain().getId().equals(captain.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("You are not the captain of any team"));
        }

        Student member = studentRepository.findByStudentId(studentId).orElse(null);
        if (member == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + studentId));
        }

        if (member.getTeam() == null || !member.getTeam().getId().equals(team.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student is not a member of your team"));
        }

        if (member.getId().equals(captain.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("You cannot remove yourself from the team"));
        }

        if (teamRemovalRequestRepository.existsByTeamIdAndStudentStudentIdAndStatus(team.getId(), studentId, "PENDING")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("A pending removal request already exists for this student"));
        }

        TeamRemovalRequest request = new TeamRemovalRequest(team, member, captain, reason, "PENDING");
        teamRemovalRequestRepository.save(request);
        log.info("Created removal request for student {} from Team {} by {}", member.getFullName(), team.getName(), captain.getFullName());

        return ResponseEntity.ok(ApiResponse.ok("Removal request sent to CC successfully", null));
    }

    @GetMapping("/removal-requests/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get Pending Removal Requests", description = "Gets all pending team member removal requests.")
    public ResponseEntity<ApiResponse<List<TeamRemovalRequestDto>>> getPendingRemovalRequests() {
        List<TeamRemovalRequest> requests = teamRemovalRequestRepository.findByStatus("PENDING");
        List<TeamRemovalRequestDto> dtos = requests.stream().map(req -> new TeamRemovalRequestDto(
                req.getId(),
                req.getTeam().getId(),
                req.getTeam().getName(),
                req.getStudent().getStudentId(),
                req.getStudent().getFullName(),
                req.getCaptain().getStudentId(),
                req.getCaptain().getFullName(),
                req.getReason(),
                req.getStatus(),
                req.getCreatedAt()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Pending requests retrieved", dtos));
    }

    @PutMapping("/removal-requests/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional
    @Operation(summary = "Approve Removal Request", description = "Approves a removal request and removes the student from the team.")
    public ResponseEntity<ApiResponse<Void>> approveRemovalRequest(@PathVariable Long id) {
        TeamRemovalRequest request = teamRemovalRequestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Request not found"));
        }
        if (!"PENDING".equals(request.getStatus())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Request is not pending"));
        }

        Student member = request.getStudent();
        Team team = request.getTeam();

        if (member.getTeam() != null && member.getTeam().getId().equals(team.getId())) {
            member.setTeam(null);
            studentRepository.save(member);
        }

        request.setStatus("APPROVED");
        teamRemovalRequestRepository.save(request);

        log.info("Approved removal request {}. Removed student {} from Team {}", id, member.getFullName(), team.getName());
        return ResponseEntity.ok(ApiResponse.ok("Request approved and student removed", null));
    }

    @PutMapping("/removal-requests/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional
    @Operation(summary = "Reject Removal Request", description = "Rejects a removal request.")
    public ResponseEntity<ApiResponse<Void>> rejectRemovalRequest(@PathVariable Long id) {
        TeamRemovalRequest request = teamRemovalRequestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Request not found"));
        }
        if (!"PENDING".equals(request.getStatus())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Request is not pending"));
        }

        request.setStatus("REJECTED");
        teamRemovalRequestRepository.save(request);

        log.info("Rejected removal request {}", id);
        return ResponseEntity.ok(ApiResponse.ok("Request rejected", null));
    }

    @PutMapping("/{id}/limit")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Update Team Limit", description = "Updates the maximum size limit of the team (CC/Admin only).")
    public ResponseEntity<ApiResponse<Void>> updateTeamLimit(@PathVariable Long id, @RequestParam int size) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));
        }

        long currentMembersCount = team.getMembers().size();
        boolean captainInMembers = team.getCaptain() != null && team.getMembers().stream().anyMatch(m -> m.getId().equals(team.getCaptain().getId()));
        long totalSize = currentMembersCount + (captainInMembers ? 0 : 1);
        if (size < totalSize) {
            return ResponseEntity.badRequest().body(ApiResponse.error("New limit cannot be less than the current number of members (" + totalSize + ")"));
        }

        team.setSize(size);
        teamRepository.save(team);
        log.info("Updated Team {} limit to: {}", team.getName(), size);

        return ResponseEntity.ok(ApiResponse.ok("Team limit updated successfully", null));
    }

    private StudentResponse toStudentResponse(Student student) {
        Long teamId = student.getTeam() != null ? student.getTeam().getId() : null;
        String teamName = student.getTeam() != null ? student.getTeam().getName() : null;
        boolean isCap = student.getTeam() != null && student.getTeam().getCaptain() != null
                && student.getTeam().getCaptain().getId().equals(student.getId());

        return StudentResponse.builder()
                .id(student.getId())
                .studentId(student.getStudentId())
                .fullName(student.getFullName())
                .email(student.getEmail())
                .phone(student.getPhone())
                .gender(student.getGender())
                .dateOfBirth(student.getDateOfBirth())
                .address(student.getAddress())
                .departmentName(student.getDepartment() != null ? student.getDepartment().getName() : null)
                .semester(student.getSemester())
                .academicYear(student.getAcademicYear())
                .active(student.isActive())
                .createdAt(student.getCreatedAt())
                .sprNo(student.getSprNo())
                .score(student.getScore())
                .teamId(teamId)
                .teamName(teamName)
                .isCaptain(isCap)
                .build();
    }
}
