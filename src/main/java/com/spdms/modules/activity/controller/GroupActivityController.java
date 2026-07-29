package com.pragatix.modules.activity.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.student.dto.response.StudentResponse;
import com.pragatix.dto.TeamResponse;
import com.pragatix.entity.*;
import com.pragatix.repository.*;
import com.pragatix.modules.activity.repository.*;
import com.pragatix.modules.student.repository.*;
import com.pragatix.modules.authentication.repository.UserRepository;
import com.pragatix.modules.student.service.XpEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/group-activities")
@Tag(name = "Group Activities", description = "Management of group activities by Faculty")
@SecurityRequirement(name = "bearerAuth")
public class GroupActivityController {

    private static final Logger log = LoggerFactory.getLogger(GroupActivityController.class);

    private final TeamRepository teamRepository;
    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final XpEngineService xpEngineService;

    public GroupActivityController(TeamRepository teamRepository,
            ActivityAssignmentRepository activityAssignmentRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            XpEngineService xpEngineService) {
        this.teamRepository = teamRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.xpEngineService = xpEngineService;
    }

    @GetMapping("/assignments/{assignmentId}/teams")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get Teams for Assignment", description = "Returns all teams created for a specific activity assignment.")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeamsForAssignment(@PathVariable Long assignmentId) {
        ActivityAssignment assignment = activityAssignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Assignment not found"));
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);

        boolean canDelete = false;
        if (currentUser != null) {
            boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
            boolean isCc = currentUser.getSubRoles().stream().map(com.pragatix.entity.SubRole::getName)
                    .anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));
            boolean isAssignedFaculty = assignment.getTeacher() != null
                    && assignment.getTeacher().getUsername().equals(username);

            boolean matchesDeptAndSection = false;
            if (isCc && assignment.getDepartment() != null && currentUser.getDepartment() != null) {
                if (assignment.getDepartment().getId().equals(currentUser.getDepartment().getId())) {
                    if (assignment.getSection() == null || (currentUser.getSection() != null
                            && assignment.getSection().getId().equals(currentUser.getSection().getId()))) {
                        matchesDeptAndSection = true;
                    }
                }
            }
            canDelete = isAdmin || isAssignedFaculty || matchesDeptAndSection;
        }
        final boolean finalCanDelete = canDelete;

        List<Team> teams = teamRepository.findAll().stream().filter(t -> {
            if (assignment.getAssignmentScope() == AssignmentScope.GLOBAL)
                return true;
            if (t.getDepartment() == null || assignment.getDepartment() == null)
                return false;
            if (!t.getDepartment().getId().equals(assignment.getDepartment().getId()))
                return false;
            if (assignment.getAssignmentScope() == AssignmentScope.DEPARTMENT)
                return true;
            if (t.getYear() == null || assignment.getYear() == null || !t.getYear().equals(assignment.getYear()))
                return false;
            if (t.getSection() == null || assignment.getSection() == null
                    || !t.getSection().getId().equals(assignment.getSection().getId()))
                return false;
            return true;
        }).collect(Collectors.toList());
        List<TeamResponse> responses = teams.stream().map(g -> {
            List<StudentResponse> studentResponses = g.getMembers().stream()
                    .map(this::toStudentResponse)
                    .collect(Collectors.toList());

            String captainId = g.getCaptain() != null ? g.getCaptain().getRegNo() : null;
            String captainName = g.getCaptain() != null ? g.getCaptain().getFullName() : null;

            if (captainId != null) {
                boolean captainInMembers = studentResponses.stream()
                        .anyMatch(s -> s.getRegNo().equals(captainId));
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
                    studentResponses,
                    assignment.getId(),
                    assignment.getActivity().getActivityName(),
                    finalCanDelete);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @PostMapping("/teams/{teamId}/award-xp")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional
    @Operation(summary = "Award XP to Team", description = "Awards XP to all or selected members of a team with remarks.")
    public ResponseEntity<ApiResponse<String>> awardXpToTeam(@PathVariable Long teamId,
            @RequestBody Map<String, Object> body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User teacher = userRepository.findByUsername(username).orElse(null);
        if (teacher == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Teacher not found"));
        }

        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));
        }

        if (!body.containsKey("assignmentId")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("assignmentId must be provided in the request body"));
        }
        Long assignmentId = Long.valueOf(body.get("assignmentId").toString());
        ActivityAssignment assignment = activityAssignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Assignment not found"));
        }

        Activity activity = assignment.getActivity();
        if (activity.getStage() != null && activity.getStage().getStatus() != com.pragatix.enums.StageStatus.ACTIVE) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Cannot award XP for an activity in a non-active stage."));
        }

        boolean equalDistribution = body.containsKey("equalDistribution")
                && Boolean.parseBoolean(body.get("equalDistribution").toString());

        if (equalDistribution) {
            int xp = Integer.parseInt(body.get("xp").toString());
            String remarks = body.containsKey("remarks") ? body.get("remarks").toString() : null;

            List<Student> studentsToAward = new ArrayList<>(team.getMembers());
            if (team.getCaptain() != null && !studentsToAward.contains(team.getCaptain())) {
                studentsToAward.add(team.getCaptain());
            }

            for (Student member : studentsToAward) {
                xpEngineService.awardXp(member, activity, teacher, assignment, xp, remarks);
            }
        } else {
            List<Map<String, Object>> studentsData = (List<Map<String, Object>>) body.get("students");
            if (studentsData == null || studentsData.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("No student data provided"));
            }

            List<String> studentIds = studentsData.stream().map(s -> s.get("regNo").toString())
                    .collect(Collectors.toList());
            List<Student> fetchedStudents = studentRepository.findByRegNoIn(studentIds);
            Map<String, Student> studentMap = fetchedStudents.stream()
                    .collect(Collectors.toMap(Student::getRegNo, s -> s));

            for (Map<String, Object> sData : studentsData) {
                String regNo = sData.get("regNo").toString();
                int xp = Integer.parseInt(sData.get("xp").toString());
                String remarks = sData.containsKey("remarks") ? sData.get("remarks").toString() : null;

                Student student = studentMap.get(regNo);
                if (student != null) {
                    xpEngineService.awardXp(student, activity, teacher, assignment, xp, remarks);
                }
            }
        }

        return ResponseEntity.ok(ApiResponse.ok("XP awarded successfully", null));
    }

    private StudentResponse toStudentResponse(Student student) {
        StudentResponse s = new StudentResponse();
        s.setRegNo(student.getRegNo());
        s.setFullName(student.getFullName());
        s.setDepartmentName(student.getDepartment() != null ? student.getDepartment().getName() : null);
        s.setSection(student.getSection() != null ? student.getSection().getSectionName() : null);
        return s;
    }
}
