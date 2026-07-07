package com.spdms.admin;

import com.spdms.dto.*;
import com.spdms.entity.*;
import com.spdms.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/groups")
@Tag(name = "Groups", description = "Student group management for Class Coordinators (CC)")
@SecurityRequirement(name = "bearerAuth")
public class GroupController {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public GroupController(GroupRepository groupRepository,
                           UserRepository userRepository,
                           StudentRepository studentRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Create Group", description = "Creates a student group. Requires teacher with sub-role CC.")
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        // 1. Get logged-in user and verify CC permissions
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User creator = userRepository.findByUsername(username).orElse(null);
        if (creator == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
        }

        boolean isCcOrAdmin = creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"))
                || creator.getSubRoles().stream().map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));

        if (!isCcOrAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access Denied: Only Class Coordinators (CC) can create groups."));
        }

        // 2. Validate group name duplication
        if (groupRepository.existsByName(request.getName())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Group name '" + request.getName() + "' already exists."));
        }

        // 3. Find Captain
        Student captain = studentRepository.findByStudentId(request.getCaptainStudentId()).orElse(null);
        if (captain == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Captain student not found with ID: " + request.getCaptainStudentId()));
        }

        // 4. Find Members
        List<Student> members = new ArrayList<>();
        if (request.getMemberStudentIds() != null && !request.getMemberStudentIds().isEmpty()) {
            for (String sid : request.getMemberStudentIds()) {
                Student m = studentRepository.findByStudentId(sid).orElse(null);
                if (m == null) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("Member student not found with ID: " + sid));
                }
                members.add(m);
            }
        }

        // 5. Size Validation (Captain is included in size)
        int totalSize = 1 + members.size();
        if (totalSize > request.getSize()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot add " + totalSize + " members (including captain) because the group size limit is " + request.getSize() + "."));
        }

        // 6. Create Group
        Group group = Group.builder()
                .name(request.getName())
                .size(request.getSize())
                .captain(captain)
                .build();

        Group savedGroup = groupRepository.save(group);

        // 7. Allot group reference to captain and members
        captain.setGroup(savedGroup);
        studentRepository.save(captain);

        for (Student m : members) {
            m.setGroup(savedGroup);
            studentRepository.save(m);
        }

        // Add captain and members to response list
        List<StudentResponse> studentResponses = new ArrayList<>();
        studentResponses.add(toStudentResponse(captain));
        for (Student m : members) {
            studentResponses.add(toStudentResponse(m));
        }

        GroupResponse response = new GroupResponse(
                savedGroup.getId(),
                savedGroup.getName(),
                savedGroup.getSize(),
                captain.getStudentId(),
                captain.getFullName(),
                studentResponses
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Group created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional(readOnly = true)
    @Operation(summary = "List Groups")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getAllGroups() {
        List<Group> groups = groupRepository.findAll();
        List<GroupResponse> responses = groups.stream().map(g -> {
            List<StudentResponse> studentResponses = g.getMembers().stream()
                    .map(this::toStudentResponse)
                    .collect(Collectors.toList());
            
            // Note: If captain is not in members (which is possible if mappedBy is one-to-many list), ensure it's displayed
            boolean captainInMembers = studentResponses.stream()
                    .anyMatch(s -> s.getStudentId().equals(g.getCaptain().getStudentId()));
            if (!captainInMembers) {
                studentResponses.add(0, toStudentResponse(g.getCaptain()));
            }

            return new GroupResponse(
                    g.getId(),
                    g.getName(),
                    g.getSize(),
                    g.getCaptain().getStudentId(),
                    g.getCaptain().getFullName(),
                    studentResponses
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    private StudentResponse toStudentResponse(Student student) {
        Long groupId = student.getGroup() != null ? student.getGroup().getId() : null;
        String groupName = student.getGroup() != null ? student.getGroup().getName() : null;

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
                .groupId(groupId)
                .groupName(groupName)
                .build();
    }
}
