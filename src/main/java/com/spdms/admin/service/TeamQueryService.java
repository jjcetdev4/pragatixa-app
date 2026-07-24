package com.spdms.admin.service;

import com.spdms.common.response.ApiResponse;
import com.spdms.dto.TeamResponse;
import com.spdms.entity.Student;
import com.spdms.entity.Team;
import com.spdms.modules.student.repository.StudentRepository;
import com.spdms.repository.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TeamQueryService {

    private final TeamRepository teamRepository;
    private final StudentRepository studentRepository;
    private final TeamMapper mapper;

    private final com.spdms.modules.authentication.repository.UserRepository userRepository;
    private final TeamValidationService validationService;
    private final TeamCleanupService teamCleanupService;

    public TeamQueryService(TeamRepository teamRepository, StudentRepository studentRepository, TeamMapper mapper,
                            com.spdms.modules.authentication.repository.UserRepository userRepository,
                            TeamValidationService validationService,
                            TeamCleanupService teamCleanupService) {
        this.teamRepository = teamRepository;
        this.studentRepository = studentRepository;
        this.mapper = mapper;
        this.userRepository = userRepository;
        this.validationService = validationService;
        this.teamCleanupService = teamCleanupService;
    }

    public ResponseEntity<ApiResponse<List<TeamResponse>>> getAllTeams() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        com.spdms.entity.User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        List<Team> teams = teamRepository.findAll();
        List<TeamResponse> responses = teams.stream()
                .filter(team -> {
                    if (team.getCaptain() == null && (team.getMembers() == null || team.getMembers().isEmpty())) {
                        teamCleanupService.autoDeleteEmptyTeam(team);
                        return false;
                    }
                    try {
                        validationService.validateTeamAccess(currentUser, team);
                        return true;
                    } catch (org.springframework.security.access.AccessDeniedException e) {
                        return false;
                    }
                })
                .map(mapper::toTeamResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    public ResponseEntity<ApiResponse<TeamResponse>> getMyTeam(Student student) {
        if (student.getTeam() == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("You do not belong to any team"));
        Team team = teamRepository.findByIdWithMembers(student.getTeam().getId()).orElse(null);
        if (team == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team details could not be loaded"));
        return ResponseEntity.ok(ApiResponse.ok("Team details retrieved successfully", mapper.toTeamResponse(team)));
    }

    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(Long id) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));
        
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        com.spdms.entity.User currentUser = userRepository.findByUsername(username).orElse(null);
        
        if (currentUser != null) {
            try {
                validationService.validateTeamAccess(currentUser, team);
            } catch (org.springframework.security.access.AccessDeniedException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
            }
        }
        
        return ResponseEntity.ok(ApiResponse.ok("Team details retrieved successfully", mapper.toTeamResponse(team)));
    }

    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyClassmates(Student currentStudent) {
        if (currentStudent.getDepartment() == null || currentStudent.getSection() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student is not assigned to a department and section."));
        }

        List<Student> classmates = studentRepository.findByDepartmentIdAndSectionId(
                currentStudent.getDepartment().getId(),
                currentStudent.getSection().getId()
        );

        List<Map<String, Object>> response = classmates.stream()
                .filter(s -> !s.getId().equals(currentStudent.getId()))
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("regNo", s.getRegNo());
                    map.put("fullName", s.getFullName());
                    map.put("regNo", s.getRegNo());
                    map.put("sprNo", s.getSprNo());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Classmates retrieved successfully", response));
    }
}
