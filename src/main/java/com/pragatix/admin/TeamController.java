package com.pragatix.admin;

import com.pragatix.admin.service.TeamCrudService;
import com.pragatix.admin.service.TeamMemberService;
import com.pragatix.admin.service.TeamQueryService;
import com.pragatix.admin.service.TeamRequestService;
import com.pragatix.common.response.ApiResponse;
import com.pragatix.dto.CreateTeamRequest;
import com.pragatix.dto.TeamRemovalRequestDto;
import com.pragatix.dto.TeamResponse;
import com.pragatix.entity.Student;
import com.pragatix.modules.authentication.security.StudentAuthResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Teams", description = "Student team management for Class Coordinators (CC) and Students")
@SecurityRequirement(name = "bearerAuth")
public class TeamController {

    private final TeamCrudService teamCrudService;
    private final TeamMemberService teamMemberService;
    private final TeamRequestService teamRequestService;
    private final TeamQueryService teamQueryService;
    private final StudentAuthResolver studentAuthResolver;

    public TeamController(TeamCrudService teamCrudService,
            TeamMemberService teamMemberService,
            TeamRequestService teamRequestService,
            TeamQueryService teamQueryService,
            StudentAuthResolver studentAuthResolver) {
        this.teamCrudService = teamCrudService;
        this.teamMemberService = teamMemberService;
        this.teamRequestService = teamRequestService;
        this.teamQueryService = teamQueryService;
        this.studentAuthResolver = studentAuthResolver;
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Create Team", description = "Creates a student team. Capable of being called by an Assigned Faculty, CC, or Admin.")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return teamCrudService.createTeam(request, username);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "List Teams")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getAllTeams(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long sectionId) {
        return teamQueryService.getAllTeams(academicYear, departmentId, sectionId);
    }

    @GetMapping("/my-team")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get My Team", description = "Returns the team details for the logged-in student (captain/member).")
    public ResponseEntity<ApiResponse<TeamResponse>> getMyTeam() {
        Student student = studentAuthResolver.getLoggedInStudent();
        return teamQueryService.getMyTeam(student);
    }

    @GetMapping("/my-team/details")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get My Team Details (Leaderboard)", description = "Returns the team details for the logged-in student with XP sorting.")
    public ResponseEntity<ApiResponse<com.pragatix.dto.StudentTeamDetailsResponse>> getMyTeamDetails() {
        Student student = studentAuthResolver.getLoggedInStudent();
        return teamQueryService.getMyTeamDetails(student);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER') or hasRole('STUDENT')")
    @Operation(summary = "Get Team by ID")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(@PathVariable Long id) {
        return teamQueryService.getTeamById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Update Team")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(@PathVariable Long id,
            @Valid @RequestBody CreateTeamRequest request) {
        return teamCrudService.updateTeam(id, request);
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Add Team Member by Team ID", description = "Adds a student to a team by team ID.")
    public ResponseEntity<ApiResponse<Void>> addMemberToTeam(@PathVariable Long id, @RequestParam String regNo) {
        return teamMemberService.addMemberToTeam(id, regNo);
    }

    @DeleteMapping("/{id}/members/{regNo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Remove Team Member by Team ID", description = "Removes a student from a team by team ID.")
    public ResponseEntity<ApiResponse<TeamResponse>> removeMemberFromTeam(@PathVariable Long id,
            @PathVariable String regNo) {
        return teamMemberService.removeMemberFromTeam(id, regNo);
    }

    @PostMapping("/{id}/captain")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Assign Team Captain", description = "Assigns/promotes a student to captain of a team.")
    public ResponseEntity<ApiResponse<TeamResponse>> assignTeamCaptain(@PathVariable Long id,
            @RequestParam String regNo) {
        return teamMemberService.assignTeamCaptain(id, regNo);
    }

    @PutMapping("/{id}/captain")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Assign Team Captain", description = "Assigns/promotes a student to captain of a team.")
    public ResponseEntity<ApiResponse<TeamResponse>> assignTeamCaptainPut(@PathVariable Long id,
            @RequestParam String regNo) {
        return teamMemberService.assignTeamCaptain(id, regNo);
    }

    @DeleteMapping("/{id}/captain")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Remove Team Captain", description = "Removes the captain role from a team.")
    public ResponseEntity<ApiResponse<TeamResponse>> removeTeamCaptain(@PathVariable Long id) {
        return teamMemberService.removeTeamCaptain(id);
    }

    @PutMapping("/{id}/vice-captain")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Assign Team Vice Captain", description = "Assigns/promotes a student to vice captain of a team.")
    public ResponseEntity<ApiResponse<TeamResponse>> assignTeamViceCaptain(@PathVariable Long id,
            @RequestParam String regNo) {
        return teamMemberService.assignTeamViceCaptain(id, regNo);
    }

    @DeleteMapping("/{id}/vice-captain")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Remove Team Vice Captain", description = "Removes the vice captain role from a team.")
    public ResponseEntity<ApiResponse<TeamResponse>> removeTeamViceCaptain(@PathVariable Long id) {
        return teamMemberService.removeTeamViceCaptain(id);
    }

    @GetMapping("/my-classmates")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get My Classmates", description = "Returns a list of students in the same department and section as the logged-in student.")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyClassmates() {
        Student currentStudent = studentAuthResolver.getLoggedInStudent();
        return teamQueryService.getMyClassmates(currentStudent);
    }

    @PostMapping("/my-team/add-member")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Add Team Member", description = "Adds a student to the captain's team.")
    public ResponseEntity<ApiResponse<Void>> addMember(@RequestParam String regNo) {
        Student captain = studentAuthResolver.getLoggedInStudent();
        return teamMemberService.addMemberByStudent(captain, regNo);
    }

    @PostMapping("/{id}/add-member")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Add Team Member (CC)", description = "Adds a student to a specific team (CC/Admin only).")
    public ResponseEntity<ApiResponse<Void>> addMemberByCC(@PathVariable Long id, @RequestParam String regNo) {
        return teamMemberService.addMemberByCC(id, regNo);
    }

    @PostMapping("/{id}/remove-member")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Remove Team Member (CC)", description = "Removes a student from a specific team (CC/Admin only).")
    public ResponseEntity<ApiResponse<TeamResponse>> removeMemberByCC(@PathVariable Long id,
            @RequestParam String regNo) {
        return teamMemberService.removeMemberByCC(id, regNo);
    }

    @PostMapping("/my-team/remove-request")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Request Team Member Removal", description = "Creates a request to remove a student from the captain's team.")
    public ResponseEntity<ApiResponse<Void>> requestRemoveMember(@RequestParam String regNo,
            @RequestParam(required = false, defaultValue = "Requested by Captain") String reason) {
        Student captain = studentAuthResolver.getLoggedInStudent();
        return teamRequestService.requestRemoveMember(captain, regNo, reason);
    }

    @GetMapping("/removal-requests/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get Pending Removal Requests", description = "Gets all pending team member removal requests.")
    public ResponseEntity<ApiResponse<List<TeamRemovalRequestDto>>> getPendingRemovalRequests() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return teamRequestService.getPendingRemovalRequests(username);
    }

    @PutMapping("/removal-requests/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Approve Removal Request", description = "Approves a removal request and removes the student from the team.")
    public ResponseEntity<ApiResponse<Void>> approveRemovalRequest(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return teamRequestService.approveRemovalRequest(id, username);
    }

    @PutMapping("/removal-requests/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Reject Removal Request", description = "Rejects a removal request.")
    public ResponseEntity<ApiResponse<Void>> rejectRemovalRequest(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return teamRequestService.rejectRemovalRequest(id, username);
    }

    @PutMapping("/{id}/limit")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Update Team Limit", description = "Updates the maximum size limit of the team (CC/Admin only).")
    public ResponseEntity<ApiResponse<Void>> updateTeamLimit(@PathVariable Long id, @RequestParam int size) {
        return teamCrudService.updateTeamLimit(id, size);
    }

    @DeleteMapping("/{teamId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Delete Team", description = "Deletes a team if no XP has been awarded. Authorized for Admin, Assigned Faculty, or matching CC.")
    public ResponseEntity<ApiResponse<Void>> deleteTeam(@PathVariable Long teamId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return teamCrudService.deleteTeam(teamId, username);
    }
}
