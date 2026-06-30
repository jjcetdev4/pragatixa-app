package com.spdms.admin;

import com.spdms.dto.ApiResponse;
import com.spdms.dto.CreateUserRequest;
import com.spdms.dto.UpdateUserRequest;
import com.spdms.dto.CreateDepartmentRequest;
import com.spdms.dto.UserResponse;
import com.spdms.entity.Department;
import com.spdms.entity.Role;
import com.spdms.entity.User;
import com.spdms.entity.ActivityStage;
import com.spdms.entity.ActivitySubgroup;
import com.spdms.entity.Subject;
import com.spdms.repository.DepartmentRepository;
import com.spdms.repository.RoleRepository;
import com.spdms.repository.StudentRepository;
import com.spdms.repository.UserRepository;
import com.spdms.repository.ActivityStageRepository;
import com.spdms.repository.ActivitySubgroupRepository;
import com.spdms.repository.SubjectRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Admin dashboard and user management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityStageRepository activityStageRepository;
    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final SubjectRepository subjectRepository;

    public AdminController(StudentRepository studentRepository,
                           UserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           ActivityStageRepository activityStageRepository,
                           ActivitySubgroupRepository activitySubgroupRepository,
                           SubjectRepository subjectRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.activityStageRepository = activityStageRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.subjectRepository = subjectRepository;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Dashboard Stats", description = "Get overview metrics for admin dashboard.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        long totalStudents = studentRepository.count();
        long totalUsers = userRepository.count();
        long totalDepartments = departmentRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", totalStudents);
        stats.put("totalUsers", totalUsers);
        stats.put("totalDepartments", totalDepartments);

        return ResponseEntity.ok(ApiResponse.ok("Stats loaded", stats));
    }

    // ==========================================
    // USER / TEACHER ENDPOINTS
    // ==========================================

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List All Users", description = "Returns all staff/users (teachers and admins).")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserResponse> responses = users.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Create User", description = "Creates a new teacher or admin account.")
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

        User user = User.builder()
            .username(request.getUsername())
            .password(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getFullName())
            .email(request.getEmail())
            .department(department)
            .roles(roles)
            .subRoles(request.getSubRoles() != null ? request.getSubRoles() : new HashSet<>())
            .section(request.getSection())
            .year(request.getYear())
            .active(true)
            .build();

        User saved = userRepository.save(user);
        log.info("Admin created new user: {}", saved.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("User created successfully", toResponse(saved)));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Update User", description = "Updates teacher or admin profile information and role selections.")
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

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setDepartment(department);
        user.setSection(request.getSection());
        user.setYear(request.getYear());
        
        user.getRoles().clear();
        if (roles != null) {
            user.getRoles().addAll(roles);
        }
        
        user.getSubRoles().clear();
        if (request.getSubRoles() != null) {
            user.getSubRoles().addAll(request.getSubRoles());
        }
        
        user.setActive(request.isActive());

        User saved = userRepository.save(user);
        log.info("Admin updated user: {}", saved.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("User updated successfully", toResponse(saved)));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Delete User", description = "Deletes a teacher or admin staff account. Requires ADMIN role.")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("User not found"));
        }
        userRepository.deleteById(id);
        log.info("Admin deleted user with ID: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("User deleted successfully", null));
    }

    // ==========================================
    // DEPARTMENT ENDPOINTS
    // ==========================================

    @GetMapping("/departments")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "List Departments")
    public ResponseEntity<ApiResponse<List<Department>>> getAllDepartments() {
        return ResponseEntity.ok(ApiResponse.ok(departmentRepository.findAll()));
    }

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Create Department")
    public ResponseEntity<ApiResponse<Department>> createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        if (departmentRepository.findByCode(request.getCode()).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Department code already exists"));
        }
        Department dept = Department.builder()
            .name(request.getName())
            .code(request.getCode())
            .description(request.getDescription())
            .build();
        Department saved = departmentRepository.save(dept);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Department created successfully", saved));
    }

    @PutMapping("/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Update Department")
    public ResponseEntity<ApiResponse<Department>> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody CreateDepartmentRequest request) {
        Department dept = departmentRepository.findById(id).orElse(null);
        if (dept == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Department not found"));
        }

        departmentRepository.findByCode(request.getCode()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new RuntimeException("Department code already registered by another department");
            }
        });

        dept.setName(request.getName());
        dept.setCode(request.getCode());
        dept.setDescription(request.getDescription());

        Department saved = departmentRepository.save(dept);
        log.info("Admin updated department: {}", saved.getCode());
        return ResponseEntity.ok(ApiResponse.ok("Department updated successfully", saved));
    }

    @DeleteMapping("/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Delete Department")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable Long id) {
        if (!departmentRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Department not found"));
        }
        departmentRepository.deleteById(id);
        log.info("Admin deleted department with ID: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("Department deleted successfully", null));
    }

    // ==========================================
    // ROLE ENDPOINTS
    // ==========================================

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List Roles")
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        List<Role> roles = roleRepository.findAll().stream()
            .filter(r -> r.getName().equals("ROLE_TEACHER") || r.getName().equals("ROLE_TRANSPORT"))
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(roles));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Create Role")
    public ResponseEntity<ApiResponse<Role>> createRole(@RequestBody Map<String, String> body) {
        String roleName = body.get("name");
        if (roleName == null || roleName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Role name is required"));
        }
        String formattedName = roleName.trim().toUpperCase();
        if (!formattedName.startsWith("ROLE_")) {
            formattedName = "ROLE_" + formattedName;
        }
        if (roleRepository.existsByName(formattedName)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Role already exists"));
        }
        Role role = Role.builder().name(formattedName).build();
        Role saved = roleRepository.save(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Role created successfully", saved));
    }

    // ==========================================
    // ACTIVITY STAGES & SUBGROUPS CONFIG ENDPOINTS
    // ==========================================

    @GetMapping("/stages")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get all activity stages with subgroups")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllStages() {
        List<ActivityStage> stages = activityStageRepository.findAll();
        List<Map<String, Object>> responseList = stages.stream().map(stage -> {
            Map<String, Object> stageMap = new HashMap<>();
            stageMap.put("id", stage.getId());
            stageMap.put("name", stage.getName());
            stageMap.put("description", stage.getDescription());
            
            List<ActivitySubgroup> subgroups = activitySubgroupRepository.findByStageId(stage.getId());
            List<Map<String, Object>> subMaps = subgroups.stream().map(sub -> {
                Map<String, Object> subMap = new HashMap<>();
                subMap.put("id", sub.getId());
                subMap.put("name", sub.getName());
                subMap.put("threshold", sub.getThreshold());
                subMap.put("assignedFacultyId", sub.getAssignedFaculty() != null ? sub.getAssignedFaculty().getId() : null);
                subMap.put("assignedFacultyName", sub.getAssignedFaculty() != null ? sub.getAssignedFaculty().getFullName() : null);
                return subMap;
            }).collect(Collectors.toList());
            
            stageMap.put("subgroups", subMaps);
            return stageMap;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(responseList));
    }

    @PostMapping("/stages")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Create a new stage")
    public ResponseEntity<ApiResponse<ActivityStage>> createStage(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Stage name is required"));
        }
        if (activityStageRepository.existsByName(name)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Stage already exists"));
        }
        ActivityStage stage = ActivityStage.builder()
            .name(name.trim())
            .description(body.get("description"))
            .build();
        ActivityStage saved = activityStageRepository.save(stage);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Stage created successfully", saved));
    }

    @PostMapping("/stages/{stageId}/subgroups")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Create a subgroup under a stage")
    public ResponseEntity<ApiResponse<ActivitySubgroup>> createSubgroup(
            @PathVariable Long stageId,
            @RequestBody Map<String, Object> body) {
        
        ActivityStage stage = activityStageRepository.findById(stageId).orElse(null);
        if (stage == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Stage not found"));
        }

        String name = (String) body.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Subgroup name is required"));
        }

        int threshold = 0;
        if (body.get("threshold") != null) {
            threshold = Integer.parseInt(body.get("threshold").toString());
        }

        ActivitySubgroup subgroup = ActivitySubgroup.builder()
            .name(name.trim())
            .threshold(threshold)
            .stage(stage)
            .build();
        
        ActivitySubgroup saved = activitySubgroupRepository.save(subgroup);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Subgroup created successfully", saved));
    }

    @PutMapping("/subgroups/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Update a subgroup's name or threshold")
    public ResponseEntity<ApiResponse<ActivitySubgroup>> updateSubgroup(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        
        ActivitySubgroup subgroup = activitySubgroupRepository.findById(id).orElse(null);
        if (subgroup == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Subgroup not found"));
        }

        if (body.get("name") != null) {
            subgroup.setName(body.get("name").toString().trim());
        }
        if (body.get("threshold") != null) {
            subgroup.setThreshold(Integer.parseInt(body.get("threshold").toString()));
        }

        ActivitySubgroup saved = activitySubgroupRepository.save(subgroup);
        return ResponseEntity.ok(ApiResponse.ok("Subgroup updated successfully", saved));
    }

    @DeleteMapping("/stages/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Delete a stage and its subgroups")
    public ResponseEntity<ApiResponse<Void>> deleteStage(@PathVariable Long id) {
        if (!activityStageRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Stage not found"));
        }
        // Cascade delete subgroups manually
        List<ActivitySubgroup> subgroups = activitySubgroupRepository.findByStageId(id);
        activitySubgroupRepository.deleteAll(subgroups);
        
        activityStageRepository.deleteById(id);
        log.info("Admin deleted stage and its subgroups: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("Stage deleted successfully", null));
    }

    @DeleteMapping("/subgroups/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Delete a subgroup")
    public ResponseEntity<ApiResponse<Void>> deleteSubgroup(@PathVariable Long id) {
        if (!activitySubgroupRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Subgroup not found"));
        }
        activitySubgroupRepository.deleteById(id);
        log.info("Admin deleted subgroup with ID: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("Subgroup deleted successfully", null));
    }

    private UserResponse toResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());

        Long deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
        String deptName = user.getDepartment() != null ? user.getDepartment().getName() : null;

        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .active(user.isActive())
            .roles(roleNames)
            .subRoles(user.getSubRoles())
            .departmentId(deptId)
            .departmentName(deptName)
            .section(user.getSection())
            .year(user.getYear())
            .build();
    }

    // ==========================================
    // DYNAMIC SUBJECTS ENDPOINTS
    // ==========================================

    @GetMapping("/subjects")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all dynamic subjects")
    public ResponseEntity<ApiResponse<List<Subject>>> getAllSubjects() {
        return ResponseEntity.ok(ApiResponse.ok(subjectRepository.findAll()));
    }

    @PostMapping("/subjects")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Create a dynamic subject")
    public ResponseEntity<ApiResponse<Subject>> createSubject(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Subject name is required"));
        }
        String cleanName = name.trim();
        if (subjectRepository.existsByName(cleanName)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Subject already exists"));
        }
        Subject subject = new Subject(cleanName);
        Subject saved = subjectRepository.save(subject);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Subject created successfully", saved));
    }

    @DeleteMapping("/subjects/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Delete a dynamic subject")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(@PathVariable Long id) {
        if (!subjectRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Subject not found"));
        }
        subjectRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("Subject deleted successfully", null));
    }

    // ==========================================
    // FACULTY ACTIVITY ASSIGNMENT
    // ==========================================

    @PutMapping("/subgroups/{id}/assign-faculty")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Assign a faculty member to an activity subgroup")
    public ResponseEntity<ApiResponse<ActivitySubgroup>> assignFacultyToSubgroup(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        ActivitySubgroup subgroup = activitySubgroupRepository.findById(id).orElse(null);
        if (subgroup == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Subgroup not found"));
        }

        User faculty = null;
        if (body.get("userId") != null && !body.get("userId").toString().isEmpty() && !body.get("userId").toString().equals("null")) {
            Long userId = Long.valueOf(body.get("userId").toString());
            faculty = userRepository.findById(userId).orElse(null);
            if (faculty == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Faculty user not found"));
            }
            boolean isTeacher = faculty.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_TEACHER"));
            if (!isTeacher) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Assigned user must be a Teacher"));
            }
        }

        subgroup.setAssignedFaculty(faculty);
        ActivitySubgroup saved = activitySubgroupRepository.save(subgroup);
        log.info("Admin assigned faculty {} to subgroup {}", faculty != null ? faculty.getUsername() : "null", subgroup.getName());
        return ResponseEntity.ok(ApiResponse.ok("Faculty assigned successfully", saved));
    }
}
