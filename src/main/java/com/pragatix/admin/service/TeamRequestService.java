package com.pragatix.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.dto.TeamRemovalRequestDto;
import com.pragatix.entity.Student;
import com.pragatix.entity.Team;
import com.pragatix.entity.TeamRemovalRequest;
import com.pragatix.entity.User;
import com.pragatix.modules.activity.service.AssignmentSecurityService;
import com.pragatix.modules.authentication.repository.UserRepository;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.repository.TeamRemovalRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pragatix.modules.authentication.security.AuthUtils;

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

    public TeamRequestService(TeamRemovalRequestRepository teamRemovalRequestRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            AssignmentSecurityService assignmentSecurityService,
            TeamMapper mapper,
            AuthUtils authUtils) {
        this.teamRemovalRequestRepository = teamRemovalRequestRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.assignmentSecurityService = assignmentSecurityService;
        this.mapper = mapper;
        this.authUtils = authUtils;
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
