package com.spdms.admin.service;

import com.spdms.common.response.ApiResponse;
import com.spdms.dto.TeamRemovalRequestDto;
import com.spdms.entity.Student;
import com.spdms.entity.Team;
import com.spdms.entity.TeamRemovalRequest;
import com.spdms.entity.User;
import com.spdms.modules.activity.service.AssignmentSecurityService;
import com.spdms.modules.authentication.repository.UserRepository;
import com.spdms.modules.student.repository.StudentRepository;
import com.spdms.repository.TeamRemovalRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamRequestService {

    private final TeamRemovalRequestRepository teamRemovalRequestRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AssignmentSecurityService assignmentSecurityService;
    private final TeamMapper mapper;

    public TeamRequestService(TeamRemovalRequestRepository teamRemovalRequestRepository,
                              StudentRepository studentRepository,
                              UserRepository userRepository,
                              AssignmentSecurityService assignmentSecurityService,
                              TeamMapper mapper) {
        this.teamRemovalRequestRepository = teamRemovalRequestRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.assignmentSecurityService = assignmentSecurityService;
        this.mapper = mapper;
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> requestRemoveMember(Student captain, String studentId, String reason) {
        Team team = captain.getTeam();
        if (team == null || team.getCaptain() == null || !team.getCaptain().getId().equals(captain.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("You are not the captain of any team"));
        }

        Student member = studentRepository.findByStudentId(studentId).orElse(null);
        if (member == null) return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + studentId));
        if (member.getTeam() == null || !member.getTeam().getId().equals(team.getId())) return ResponseEntity.badRequest().body(ApiResponse.error("Student is not a member of your team"));
        if (member.getId().equals(captain.getId())) return ResponseEntity.badRequest().body(ApiResponse.error("You cannot remove yourself from the team"));
        if (teamRemovalRequestRepository.existsByTeamIdAndStudentStudentIdAndStatus(team.getId(), studentId, "PENDING")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("A pending removal request already exists for this student"));
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
            requests = requests.stream()
                    .filter(req -> assignmentSecurityService.isUserAssignedFaculty(req.getTeam().getAssignment(), currentUser))
                    .collect(Collectors.toList());
        }

        List<TeamRemovalRequestDto> dtos = requests.stream().map(mapper::toTeamRemovalRequestDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Pending requests retrieved", dtos));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> approveRemovalRequest(Long id, String username) {
        TeamRemovalRequest request = teamRemovalRequestRepository.findById(id).orElse(null);
        if (request == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Request not found"));
        if (!"PENDING".equals(request.getStatus())) return ResponseEntity.badRequest().body(ApiResponse.error("Request is not pending"));

        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser != null && !assignmentSecurityService.isUserAssignedFaculty(request.getTeam().getAssignment(), currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access Denied: You are not authorized to approve this request."));
        }

        Student member = request.getStudent();
        Team team = request.getTeam();

        if (member.getTeam() != null && member.getTeam().getId().equals(team.getId())) {
            member.setTeam(null);
            studentRepository.save(member);
        }

        request.setStatus("APPROVED");
        teamRemovalRequestRepository.save(request);
        return ResponseEntity.ok(ApiResponse.ok("Request approved and student removed", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> rejectRemovalRequest(Long id, String username) {
        TeamRemovalRequest request = teamRemovalRequestRepository.findById(id).orElse(null);
        if (request == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Request not found"));
        if (!"PENDING".equals(request.getStatus())) return ResponseEntity.badRequest().body(ApiResponse.error("Request is not pending"));

        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser != null && !assignmentSecurityService.isUserAssignedFaculty(request.getTeam().getAssignment(), currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access Denied: You are not authorized to reject this request."));
        }

        request.setStatus("REJECTED");
        teamRemovalRequestRepository.save(request);
        return ResponseEntity.ok(ApiResponse.ok("Request rejected", null));
    }
}
