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
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Create Group", description = "Creates a student group. Capable of being called by a student (who becomes captain) or an Admin/CC.")
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        // 1. Get logged-in username and determine if student or CC
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        Student captain = studentRepository.findByStudentId(username).orElse(null);
        if (captain != null) {
            // Logged in as student - they are the captain of the group they create
            if (captain.getGroup() != null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("You are already assigned to group: " + captain.getGroup().getName()));
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
                        .error("Access Denied: Only Class Coordinators (CC) or students can create groups."));
            }

            if (request.getCaptainStudentId() == null || request.getCaptainStudentId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Captain Student ID is required."));
            }

            captain = studentRepository.findByStudentId(request.getCaptainStudentId()).orElse(null);
            if (captain == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Captain student not found with ID: " + request.getCaptainStudentId()));
            }
            if (captain.getGroup() != null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Proposed Captain " + captain.getFullName()
                        + " is already assigned to group: " + captain.getGroup().getName()));
            }
        }

        // 2. Validate group name duplication
        if (groupRepository.existsByName(request.getName())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Group name '" + request.getName() + "' already exists."));
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
                if (m.getGroup() != null) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(
                            "Student " + m.getFullName() + " is already assigned to group: " + m.getGroup().getName()));
                }
                members.add(m);
            }
        }

        // 5. Size Validation (Captain is included in size)
        int totalSize = 1 + members.size();
        if (totalSize > request.getSize()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot add " + totalSize
                    + " members (including captain) because the group size limit is " + request.getSize() + "."));
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
                studentResponses);

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
                    .map(m -> toStudentResponse(m))
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

            return new GroupResponse(
                    g.getId(),
                    g.getName(),
                    g.getSize(),
                    captainId,
                    captainName,
                    studentResponses);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @GetMapping("/my-group")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get My Group", description = "Returns the group details for the logged-in student (captain/member).")
    public ResponseEntity<ApiResponse<GroupResponse>> getMyGroup() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Student student = studentRepository.findByStudentId(username).orElse(null);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Student not found"));
        }
        Group group = student.getGroup();
        if (group == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("You do not belong to any group"));
        }

        List<StudentResponse> studentResponses = group.getMembers().stream()
                .map(m -> toStudentResponse(m))
                .collect(Collectors.toList());

        String captainId = group.getCaptain() != null ? group.getCaptain().getStudentId() : null;
        String captainName = group.getCaptain() != null ? group.getCaptain().getFullName() : null;

        if (captainId != null) {
            boolean captainInMembers = studentResponses.stream()
                    .anyMatch(s -> s.getStudentId().equals(captainId));
            if (!captainInMembers) {
                studentResponses.add(0, toStudentResponse(group.getCaptain()));
            }
        }

        GroupResponse response = new GroupResponse(
                group.getId(),
                group.getName(),
                group.getSize(),
                captainId,
                captainName,
                studentResponses);

        return ResponseEntity.ok(ApiResponse.ok("Group details retrieved successfully", response));
    }

    @PostMapping("/my-group/add-member")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Add Group Member", description = "Adds a student to the captain's group.")
    public ResponseEntity<ApiResponse<Void>> addMember(@RequestParam String studentId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Student captain = studentRepository.findByStudentId(username).orElse(null);
        if (captain == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Captain student not found"));
        }
        Group group = captain.getGroup();
        if (group == null || group.getCaptain() == null || !group.getCaptain().getId().equals(captain.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("You are not the captain of any group"));
        }

        Student member = studentRepository.findByStudentId(studentId).orElse(null);
        if (member == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + studentId));
        }

        if (member.getGroup() != null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student " + member.getFullName() + " is already in group: " + member.getGroup().getName()));
        }

        long currentMembersCount = group.getMembers().size();
        boolean captainInMembers = group.getMembers().stream().anyMatch(m -> m.getId().equals(group.getCaptain().getId()));
        long totalSize = currentMembersCount + (captainInMembers ? 0 : 1) + 1;
        if (totalSize > group.getSize()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot add member. Group size limit of " + group.getSize() + " exceeded."));
        }

        member.setGroup(group);
        studentRepository.save(member);

        return ResponseEntity.ok(ApiResponse.ok("Member added successfully", null));
    }

    @PostMapping("/my-group/remove-member")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Remove Group Member", description = "Removes a student from the captain's group.")
    public ResponseEntity<ApiResponse<Void>> removeMember(@RequestParam String studentId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Student captain = studentRepository.findByStudentId(username).orElse(null);
        if (captain == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Captain student not found"));
        }
        Group group = captain.getGroup();
        if (group == null || group.getCaptain() == null || !group.getCaptain().getId().equals(captain.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("You are not the captain of any group"));
        }

        Student member = studentRepository.findByStudentId(studentId).orElse(null);
        if (member == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student not found with ID: " + studentId));
        }

        if (member.getGroup() == null || !member.getGroup().getId().equals(group.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student is not a member of your group"));
        }

        if (member.getId().equals(captain.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("You cannot remove yourself from the group"));
        }

        member.setGroup(null);
        studentRepository.save(member);

        return ResponseEntity.ok(ApiResponse.ok("Member removed successfully", null));
    }

    @PutMapping("/my-group/limit")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Update Group Limit", description = "Updates the maximum size limit of the group.")
    public ResponseEntity<ApiResponse<Void>> updateGroupLimit(@RequestParam int size) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Student captain = studentRepository.findByStudentId(username).orElse(null);
        if (captain == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Captain student not found"));
        }
        Group group = captain.getGroup();
        if (group == null || group.getCaptain() == null || !group.getCaptain().getId().equals(captain.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("You are not the captain of any group"));
        }

        long currentMembersCount = group.getMembers().size();
        boolean captainInMembers = group.getMembers().stream().anyMatch(m -> m.getId().equals(group.getCaptain().getId()));
        long totalSize = currentMembersCount + (captainInMembers ? 0 : 1);
        if (size < totalSize) {
            return ResponseEntity.badRequest().body(ApiResponse.error("New limit cannot be less than the current number of members (" + totalSize + ")"));
        }

        group.setSize(size);
        groupRepository.save(group);

        return ResponseEntity.ok(ApiResponse.ok("Group limit updated successfully", null));
    }

    private StudentResponse toStudentResponse(Student student) {
        Long groupId = student.getGroup() != null ? student.getGroup().getId() : null;
        String groupName = student.getGroup() != null ? student.getGroup().getName() : null;
        boolean isCap = student.getGroup() != null && student.getGroup().getCaptain() != null
                && student.getGroup().getCaptain().getId().equals(student.getId());

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
                .isCaptain(isCap)
                .build();
    }
}
