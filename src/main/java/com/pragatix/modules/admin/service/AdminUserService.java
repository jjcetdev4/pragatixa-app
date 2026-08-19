package jjcet.PragatiX.modules.admin.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.authentication.dto.request.CreateUserRequest;
import jjcet.PragatiX.modules.authentication.dto.request.UpdateUserRequest;
import jjcet.PragatiX.modules.authentication.dto.response.UserResponse;
import jjcet.PragatiX.entity.Department;
import jjcet.PragatiX.entity.Role;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.repository.DepartmentRepository;
import jjcet.PragatiX.modules.authentication.repository.RoleRepository;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.entity.SubRole;
import jjcet.PragatiX.modules.authentication.repository.SubRoleRepository;
import jjcet.PragatiX.repository.SectionRepository;
import jjcet.PragatiX.entity.Section;
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
import jjcet.PragatiX.modules.admin.service.*;
import jjcet.PragatiX.modules.admin.mapper.*;

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
    private final jjcet.PragatiX.modules.audit.service.AuditService auditService;

    public AdminUserService(DepartmentRepository departmentRepository, PasswordEncoder passwordEncoder,
            RoleRepository roleRepository, SectionRepository sectionRepository, SubRoleRepository subRoleRepository,
            UserRepository userRepository, AdminMapper adminMapper, jjcet.PragatiX.modules.audit.service.AuditService auditService) {
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.sectionRepository = sectionRepository;
        this.subRoleRepository = subRoleRepository;
        this.userRepository = userRepository;
        this.adminMapper = adminMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(Long departmentId) {
        List<User> users = userRepository.findAll();
        if (departmentId != null) {
            users = users.stream()
                    .filter(u -> u.getDepartment() != null && u.getDepartment().getId().equals(departmentId))
                    .collect(Collectors.toList());
        }
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
        
        auditService.log(
            jjcet.PragatiX.enums.AuditAction.CREATE,
            jjcet.PragatiX.enums.AuditModule.USER,
            "USER",
            saved.getId(),
            "Created user " + saved.getUsername() + (saved.getFullName() != null ? " (" + saved.getFullName() + ")" : "")
        );
        
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
        
        java.util.Map<String, Object> oldValues = new java.util.HashMap<>();
        java.util.Map<String, Object> newValues = new java.util.HashMap<>();
        newValues.put("username", saved.getUsername());
        newValues.put("fullName", saved.getFullName());
        newValues.put("email", saved.getEmail());
        if (saved.getDepartment() != null) newValues.put("departmentId", saved.getDepartment().getId());
        
        auditService.log(
            jjcet.PragatiX.enums.AuditAction.UPDATE,
            jjcet.PragatiX.enums.AuditModule.USER,
            "USER",
            saved.getId(),
            "Updated user " + saved.getUsername(),
            oldValues,
            newValues
        );
        
        log.debug("Admin updated user: {}", saved.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("User updated successfully", adminMapper.toUserResponse(saved)));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("User not found"));
        }
        
        user.setDeleted(true);
        user.setDeletedAt(java.time.LocalDateTime.now());
        user.setPermanentDeleteAt(java.time.LocalDateTime.now().plusDays(30));
        
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            user.setDeletedBy(auth.getName());
        }

        userRepository.save(user);
        
        auditService.log(
            jjcet.PragatiX.enums.AuditAction.DELETE,
            jjcet.PragatiX.enums.AuditModule.USER,
            "USER",
            user.getId(),
            "Moved user " + user.getUsername() + " to Recycle Bin"
        );
        
        log.debug("Admin soft deleted user with ID: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("User moved to Recycle Bin", null));
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
