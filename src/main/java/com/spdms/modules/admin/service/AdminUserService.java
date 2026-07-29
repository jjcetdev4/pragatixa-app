package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.authentication.dto.request.CreateUserRequest;
import com.pragatix.modules.authentication.dto.request.UpdateUserRequest;
import com.pragatix.modules.authentication.dto.response.UserResponse;
import com.pragatix.entity.Department;
import com.pragatix.entity.Role;
import com.pragatix.entity.User;
import com.pragatix.repository.DepartmentRepository;
import com.pragatix.modules.authentication.repository.RoleRepository;
import com.pragatix.modules.authentication.repository.UserRepository;
import com.pragatix.entity.SubRole;
import com.pragatix.modules.authentication.repository.SubRoleRepository;
import com.pragatix.repository.SectionRepository;
import com.pragatix.entity.Section;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import com.pragatix.modules.admin.service.*;
import com.pragatix.modules.admin.mapper.*;

@Service
public class AdminUserService {
    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final SectionRepository sectionRepository;
    private final SubRoleRepository subRoleRepository;
    private final UserRepository userRepository;
    private final AdminMapper adminMapper;

    public AdminUserService(DepartmentRepository departmentRepository, PasswordEncoder passwordEncoder,
            RoleRepository roleRepository, SectionRepository sectionRepository, SubRoleRepository subRoleRepository,
            UserRepository userRepository, AdminMapper adminMapper) {
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.sectionRepository = sectionRepository;
        this.subRoleRepository = subRoleRepository;
        this.userRepository = userRepository;
        this.adminMapper = adminMapper;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserResponse> responses = users.stream()
                .map(adminMapper::toUserResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @Transactional
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Username already exists"));
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email already registered"));
        }

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId()).orElse(null);
            if (department == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Department not found"));
            }
        }

        Set<Role> roles = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String rName : request.getRoles()) {
                Role role = roleRepository.findByName(rName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + rName));
                roles.add(role);
            }
        } else {
            Role teacherRole = roleRepository.findByName("ROLE_TEACHER")
                    .orElseThrow(() -> new RuntimeException("Role ROLE_TEACHER not found"));
            roles.add(teacherRole);
        }

        Section section = null;
        if (request.getSectionId() != null) {
            section = sectionRepository.findById(request.getSectionId()).orElse(null);
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .department(department)
                .roles(roles)
                .subRoles(this.resolveSubRoles(request.getSubRoles(), roles))
                .section(section)
                .year(request.getYear())
                .active(true)
                .build();

        User saved = userRepository.save(user);
        log.debug("Admin created new user: {}", saved.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User created successfully", adminMapper.toUserResponse(saved)));
    }

    @Transactional
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("User not found with ID: " + id));
        }

        userRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new RuntimeException("Email already registered by another user");
            }
        });

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId()).orElse(null);
            if (department == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Department not found"));
            }
        }

        Set<Role> roles = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String rName : request.getRoles()) {
                Role role = roleRepository.findByName(rName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + rName));
                roles.add(role);
            }
        }

        Section section = null;
        if (request.getSectionId() != null) {
            section = sectionRepository.findById(request.getSectionId()).orElse(null);
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setDepartment(department);
        user.setSection(section);
        user.setYear(request.getYear());

        user.getRoles().clear();
        if (roles != null) {
            user.getRoles().addAll(roles);
        }

        user.getSubRoles().clear();
        if (request.getSubRoles() != null) {
            user.getSubRoles().addAll(this.resolveSubRoles(request.getSubRoles(), roles));
        }

        user.setActive(request.isActive());

        User saved = userRepository.save(user);
        log.debug("Admin updated user: {}", saved.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("User updated successfully", adminMapper.toUserResponse(saved)));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("User not found"));
        }
        userRepository.deleteById(id);
        log.debug("Admin deleted user with ID: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("User deleted successfully", null));
    }

    public Set<SubRole> resolveSubRoles(Set<String> subRoleNames, Set<Role> roles) {
        if (subRoleNames == null)
            return new HashSet<>();
        Role defaultRole = roles != null && !roles.isEmpty() ? roles.iterator().next() : null;
        Set<SubRole> subRoles = new HashSet<>();
        for (String name : subRoleNames) {
            SubRole sr = subRoleRepository.findByName(name)
                    .orElseGet(() -> subRoleRepository.save(
                            SubRole.builder().name(name).role(defaultRole).build()));
            subRoles.add(sr);
        }
        return subRoles;
    }

}
