package com.spdms.admin;

import com.spdms.dto.ApiResponse;
import com.spdms.dto.CreateUserRequest;
import com.spdms.dto.UpdateUserRequest;
import com.spdms.dto.CreateDepartmentRequest;
import com.spdms.dto.UserResponse;
import com.spdms.dto.ActivityStageRequest;
import com.spdms.dto.ActivityStageResponse;
import com.spdms.admin.ActivityStageService;
import com.spdms.entity.Department;
import com.spdms.entity.Role;
import com.spdms.entity.User;
import com.spdms.entity.ActivityStage;
import com.spdms.entity.ActivitySubgroup;
import com.spdms.entity.Activity;
import com.spdms.entity.Subject;
import com.spdms.repository.DepartmentRepository;
import com.spdms.repository.RoleRepository;
import com.spdms.repository.StudentRepository;
import com.spdms.repository.UserRepository;
import com.spdms.repository.ActivityStageRepository;
import com.spdms.repository.ActivitySubgroupRepository;
import com.spdms.repository.SubjectRepository;
import com.spdms.repository.ActivityRepository;
import com.spdms.repository.DisciplineLogRepository;
import com.spdms.repository.ActivityAssignmentRepository;
import com.spdms.entity.ActivityAssignment;
import com.spdms.dto.ActivityAssignmentResponse;
import com.spdms.dto.MyActivityResponse;
import java.util.ArrayList;
import com.spdms.entity.SubRole;
import com.spdms.repository.SubRoleRepository;
import com.spdms.repository.SectionRepository;
import com.spdms.repository.FacultyRepository;
import com.spdms.repository.StudentGroupRepository;
import com.spdms.repository.AcademicYearRepository;
import com.spdms.repository.YearRepository;
import com.spdms.repository.SemesterRepository;
import com.spdms.repository.GenderRepository;
import com.spdms.entity.AcademicYear;
import com.spdms.entity.Year;
import com.spdms.entity.Semester;
import com.spdms.entity.Gender;
import com.spdms.entity.Section;
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

import java.time.LocalDateTime;
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
    private final SubRoleRepository subRoleRepository;
    private final SectionRepository sectionRepository;
    private final FacultyRepository facultyRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final AcademicYearRepository academicYearRepository;
    private final YearRepository yearRepository;
    private final SemesterRepository semesterRepository;
    private final GenderRepository genderRepository;
    private final ActivityRepository activityRepository;
    private final DisciplineLogRepository disciplineLogRepository;
    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final ActivityStageService activityStageService;
 
    public AdminController(StudentRepository studentRepository,
                           UserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           ActivityStageRepository activityStageRepository,
                           ActivitySubgroupRepository activitySubgroupRepository,
                           SubjectRepository subjectRepository,
                           SubRoleRepository subRoleRepository,
                           SectionRepository sectionRepository,
                           FacultyRepository facultyRepository,
                           StudentGroupRepository studentGroupRepository,
                           AcademicYearRepository academicYearRepository,
                           YearRepository yearRepository,
                           SemesterRepository semesterRepository,
                           GenderRepository genderRepository,
                           ActivityRepository activityRepository,
                           DisciplineLogRepository disciplineLogRepository,
                           ActivityAssignmentRepository activityAssignmentRepository,
                           ActivityStageService activityStageService) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.activityStageRepository = activityStageRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.subjectRepository = subjectRepository;
        this.subRoleRepository = subRoleRepository;
        this.sectionRepository = sectionRepository;
        this.facultyRepository = facultyRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.academicYearRepository = academicYearRepository;
        this.yearRepository = yearRepository;
        this.semesterRepository = semesterRepository;
        this.genderRepository = genderRepository;
        this.activityRepository = activityRepository;
        this.disciplineLogRepository = disciplineLogRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.activityStageService = activityStageService;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional(readOnly = true)
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
            .subRoles(resolveSubRoles(request.getSubRoles(), roles))
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
            user.getSubRoles().addAll(resolveSubRoles(request.getSubRoles(), roles));
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
        if (departmentRepository.findByCode(request.getCode()).isPresent() || departmentRepository.findByDeptCode(request.getCode()).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Department code already exists"));
        }
        if (departmentRepository.findByName(request.getName()).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Department name already exists"));
        }
        Department dept = Department.builder()
            .deptCode(request.getCode())
            .deptName(request.getName())
            .code(request.getCode())
            .name(request.getName())
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

        if (departmentRepository.findByCode(request.getCode()).stream().anyMatch(existing -> !existing.getId().equals(id)) ||
            departmentRepository.findByDeptCode(request.getCode()).stream().anyMatch(existing -> !existing.getId().equals(id))) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Department code already registered by another department"));
        }
        if (departmentRepository.findByName(request.getName()).stream().anyMatch(existing -> !existing.getId().equals(id))) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Department name already registered by another department"));
        }

        dept.setName(request.getName());
        dept.setCode(request.getCode());
        dept.setDeptCode(request.getCode());
        dept.setDeptName(request.getName());
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

        long sections = sectionRepository.countByDepartmentId(id);
        long students = studentRepository.countByDepartmentId(id);
        long faculty = facultyRepository.countByDepartmentId(id);
        long subjects = subjectRepository.countByDepartmentId(id);
        long subgroups = activitySubgroupRepository.countByAssignedDepartmentId(id);
        long users = userRepository.countByDepartmentId(id);
        long groups = studentGroupRepository.countByDepartmentId(id);

        java.util.List<String> deps = new java.util.ArrayList<>();
        if (sections > 0) deps.add(sections + " Section(s)");
        if (students > 0) deps.add(students + " Student(s)");
        if (faculty > 0) deps.add(faculty + " Faculty Member(s)");
        if (subjects > 0) deps.add(subjects + " Subject(s)");
        if (subgroups > 0) deps.add(subgroups + " Activity Subgroup(s)");
        if (users > 0) deps.add(users + " User(s)");
        if (groups > 0) deps.add(groups + " Student Group(s)");

        if (!deps.isEmpty()) {
            String msg = "Cannot delete Department because it contains: " + String.join(", ", deps) + ". Remove or reassign them first.";
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(msg));
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

    @GetMapping("/academic-years")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "List Academic Years")
    public ResponseEntity<ApiResponse<List<AcademicYear>>> getAllAcademicYears() {
        return ResponseEntity.ok(ApiResponse.ok("Academic years fetched successfully", academicYearRepository.findAll()));
    }

    @GetMapping("/years")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "List Years")
    public ResponseEntity<ApiResponse<List<Year>>> getAllYears() {
        return ResponseEntity.ok(ApiResponse.ok("Years fetched successfully", yearRepository.findAll()));
    }

    @GetMapping("/semesters")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "List Semesters")
    public ResponseEntity<ApiResponse<List<Semester>>> getAllSemesters() {
        return ResponseEntity.ok(ApiResponse.ok("Semesters fetched successfully", semesterRepository.findAll()));
    }

    @GetMapping("/genders")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "List Genders")
    public ResponseEntity<ApiResponse<List<Gender>>> getAllGenders() {
        return ResponseEntity.ok(ApiResponse.ok("Genders fetched successfully", genderRepository.findAll()));
    }

    @GetMapping("/sections")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "List Sections")
    public ResponseEntity<ApiResponse<List<Section>>> getAllSections() {
        return ResponseEntity.ok(ApiResponse.ok("Sections fetched successfully", sectionRepository.findAll()));
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
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Get all activity stages with subgroups")
    public ResponseEntity<ApiResponse<List<ActivityStageResponse>>> getAllStages() {
        List<ActivityStageResponse> stages = activityStageService.getAllStages();
        return ResponseEntity.ok(ApiResponse.ok(stages));
    }

    @PostMapping("/stages")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new stage")
    public ResponseEntity<ApiResponse<ActivityStageResponse>> createStage(@Valid @RequestBody ActivityStageRequest request) {
        ActivityStageResponse saved = activityStageService.createStage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Stage created successfully", saved));
    }

    @GetMapping("/stages/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Get activity stage by ID")
    public ResponseEntity<ApiResponse<ActivityStageResponse>> getStage(@PathVariable Long id) {
        return activityStageService.getStageById(id)
                .map(stage -> ResponseEntity.ok(ApiResponse.ok(stage)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Stage not found")));
    }

    @PutMapping("/stages/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing stage")
    public ResponseEntity<ApiResponse<ActivityStageResponse>> editStage(
            @PathVariable Long id,
            @Valid @RequestBody ActivityStageRequest request) {
        ActivityStageResponse updated = activityStageService.updateStage(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Stage updated successfully", updated));
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
    @Operation(summary = "Delete a stage and its subgroups")
    public ResponseEntity<ApiResponse<Void>> deleteStage(@PathVariable Long id) {
        activityStageService.deleteStage(id);
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

        // 1. Nullify references in DisciplineLog
        disciplineLogRepository.nullifySubgroupReferences(id);
        
        List<Activity> activities = activityRepository.findBySubgroupId(id);
        for (Activity act : activities) {
            disciplineLogRepository.nullifyActivityReferences(act.getId());
        }

        // 2. Delete activities
        activityRepository.deleteAll(activities);

        // 3. Delete subgroup
        activitySubgroupRepository.deleteById(id);
        
        log.info("Admin deleted subgroup with ID: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("Subgroup deleted successfully", null));
    }

    private Set<SubRole> resolveSubRoles(Set<String> subRoleNames, Set<Role> roles) {
        if (subRoleNames == null) return new HashSet<>();
        Role defaultRole = roles != null && !roles.isEmpty() ? roles.iterator().next() : null;
        Set<SubRole> subRoles = new HashSet<>();
        for (String name : subRoleNames) {
            SubRole sr = subRoleRepository.findByName(name)
                .orElseGet(() -> subRoleRepository.save(
                    SubRole.builder().name(name).role(defaultRole).build()
                ));
            subRoles.add(sr);
        }
        return subRoles;
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
            .subRoles(user.getSubRoles().stream().map(SubRole::getName).collect(Collectors.toSet()))
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

    private void populateActivityTransientFields(Activity activity) {
        String ownerDeptName = activity.getOwnerDepartment();
        Department department = null;
        if (ownerDeptName != null && !ownerDeptName.trim().isEmpty()) {
            department = departmentRepository.findByName(ownerDeptName)
                .orElseGet(() -> departmentRepository.findByCode(ownerDeptName).orElse(null));
        }

        if (department != null) {
            activity.setDepartmentId(department.getId().toString());
            
            List<Section> sections = sectionRepository.findByDepartmentId(department.getId());
            List<Map<String, Object>> summary = new ArrayList<>();
            
            if (sections != null && !sections.isEmpty()) {
                for (Section sec : sections) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("section", sec.getSectionName());
                    map.put("sectionId", sec.getId());
                    
                    ActivityAssignment aa = activityAssignmentRepository
                        .findByActivityIdAndSectionId(activity.getId(), sec.getId()).orElse(null);
                    if (aa != null) {
                        map.put("teacherId", aa.getTeacher().getId());
                        map.put("teacherName", aa.getTeacher().getFullName());
                        map.put("teacher", aa.getTeacher().getFullName());
                        map.put("username", aa.getTeacher().getUsername());
                    } else {
                        map.put("teacherId", null);
                        map.put("teacherName", "Not Assigned");
                        map.put("teacher", "Not Assigned");
                        map.put("username", "");
                    }
                    summary.add(map);
                }
            } else {
                Map<String, Object> map = new HashMap<>();
                map.put("type", "DEPARTMENT");
                
                ActivityAssignment aa = activityAssignmentRepository
                    .findByActivityIdAndSectionIsNull(activity.getId()).orElse(null);
                if (aa != null) {
                    map.put("teacherId", aa.getTeacher().getId());
                    map.put("teacherName", aa.getTeacher().getFullName());
                    map.put("teacher", aa.getTeacher().getFullName());
                    map.put("username", aa.getTeacher().getUsername());
                } else {
                    map.put("teacherId", null);
                    map.put("teacherName", "Not Assigned");
                    map.put("teacher", "Not Assigned");
                    map.put("username", "");
                }
                summary.add(map);
            }
            activity.setAssignmentSummary(summary);
        } else {
            activity.setAssignmentSummary(new ArrayList<>());
        }

        if (activity.getOwnerSubrole() != null && !activity.getOwnerSubrole().trim().isEmpty()) {
            userRepository.findByUsername(activity.getOwnerSubrole())
                .ifPresent(user -> activity.setTeacherId(user.getId().toString()));
        }
    }
    private ActivityAssignmentResponse toResponse(ActivityAssignment aa) {
        if (aa == null) return null;
        return ActivityAssignmentResponse.builder()
            .id(aa.getId())
            .activityId(aa.getActivity() != null ? aa.getActivity().getId() : null)
            .activityName(aa.getActivity() != null ? aa.getActivity().getName() : null)
            .departmentId(aa.getDepartment() != null ? aa.getDepartment().getId() : null)
            .departmentName(aa.getDepartment() != null ? aa.getDepartment().getName() : null)
            .sectionId(aa.getSection() != null ? aa.getSection().getId() : null)
            .sectionName(aa.getSection() != null ? aa.getSection().getSectionName() : null)
            .teacherId(aa.getTeacher() != null ? aa.getTeacher().getId() : null)
            .teacherName(aa.getTeacher() != null ? aa.getTeacher().getFullName() : null)
            .teacherUsername(aa.getTeacher() != null ? aa.getTeacher().getUsername() : null)
            .assignedBy(aa.getAssignedBy() != null ? aa.getAssignedBy().getFullName() : null)
            .assignedAt(aa.getAssignedAt())
            .build();
    }

    @GetMapping("/my-activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get activities assigned to the currently logged in teacher")
    public ResponseEntity<ApiResponse<List<MyActivityResponse>>> getMyActivities() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("User not found"));
        }

        List<ActivityAssignment> assignments = activityAssignmentRepository.findByTeacherId(currentUser.getId());
        List<MyActivityResponse> responses = new ArrayList<>();
        for (ActivityAssignment aa : assignments) {
            Activity act = aa.getActivity();
            if (act == null) continue;

            List<String> evidenceList = new java.util.ArrayList<>();
            if (act.getEvidence() != null && !act.getEvidence().trim().isEmpty()) {
                for (String ev : act.getEvidence().split(",")) {
                    evidenceList.add(ev.trim());
                }
            }

            responses.add(MyActivityResponse.builder()
                .activityId(act.getId())
                .name(act.getName())
                .description(act.getDescription())
                .frequency(act.getAwardFrequency())
                .evidence(evidenceList)
                .xp(act.getXp())
                .type(act.getType())
                .justification(act.getJustification())
                .departmentId(aa.getDepartment() != null ? aa.getDepartment().getId() : null)
                .departmentName(aa.getDepartment() != null ? aa.getDepartment().getName() : null)
                .sectionId(aa.getSection() != null ? aa.getSection().getId() : null)
                .sectionName(aa.getSection() != null ? aa.getSection().getSectionName() : null)
                .assignedBy(aa.getAssignedBy() != null ? aa.getAssignedBy().getFullName() : "")
                .assignedAt(aa.getAssignedAt())
                .xpCategory(act.getXpCategory())
                .awardXp(act.getAwardXp())
                .awardType(act.getAwardType())
                .repeatAllowed(act.isRepeatAllowed())
                .cap(act.getCap())
                .awardFrequency(act.getAwardFrequency())
                .awardDays(act.getAwardDays())
                .build());
        }

        return ResponseEntity.ok(ApiResponse.ok("My activities loaded successfully", responses));
    }

    // ==========================================
    // ACTIVITY ENDPOINTS
    // ==========================================

    @GetMapping("/subgroups/{subgroupId}/activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get all activities of a subgroup")
    public ResponseEntity<ApiResponse<List<Activity>>> getActivitiesBySubgroup(@PathVariable Long subgroupId) {
        if (!activitySubgroupRepository.existsById(subgroupId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Subgroup not found"));
        }
        
        List<Activity> activities = activityRepository.findBySubgroupId(subgroupId);
        // Events are now global – all activities are visible to all users regardless of department.
        
        for (Activity activity : activities) {
            populateActivityTransientFields(activity);
        }
        return ResponseEntity.ok(ApiResponse.ok(activities));
    }

    @PostMapping("/subgroups/{subgroupId}/activities")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Create a new activity under a subgroup")
    public ResponseEntity<ApiResponse<Activity>> createActivity(
            @PathVariable Long subgroupId,
            @RequestBody Map<String, Object> body) {
        
        ActivitySubgroup subgroup = activitySubgroupRepository.findById(subgroupId)
                .orElse(null);
        if (subgroup == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Subgroup not found"));
        }

        Activity activity = new Activity();
        activity.setSubgroup(subgroup);
        activity.setStage(subgroup.getStage());
        
        String name = (String) body.get("name");
        activity.setName(name);
        activity.setActivityName(name);
        
        String desc = (String) body.get("description");
        activity.setDescription(desc);
        activity.setActivityDescription(desc);
        
        activity.setFrequency((String) body.get("frequency"));
        activity.setOwnerDepartment(""); // Department removed: events are global for all departments
        activity.setOwnerSubrole(""); // teacher assignment removed from Admin creation

        Object evidenceObj = body.get("evidence");
        if (evidenceObj instanceof List) {
            List<?> evList = (List<?>) evidenceObj;
            activity.setEvidence(evList.stream().map(Object::toString).collect(Collectors.joining(", ")));
        } else if (evidenceObj instanceof String) {
            activity.setEvidence((String) evidenceObj);
        }
        
        activity.setXp((String) body.get("xp"));
        activity.setCap(body.get("cap"));
        activity.setType((String) body.get("type"));
        activity.setModeType(body.get("type") != null ? (String) body.get("type") : "Individual");
        activity.setJustification((String) body.get("justification"));
        
        String xpCategory = (String) body.get("xpCategory");
        if (xpCategory == null || xpCategory.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("XP Category is required"));
        }
        List<String> allowedCategories = List.of(
            "Academic", "Skill", "Communication", "Leadership", "Discipline",
            "Placement", "Innovation", "Community", "Sports", "Cultural"
        );
        boolean isAllowed = allowedCategories.stream().anyMatch(cat -> cat.equalsIgnoreCase(xpCategory.trim()));
        if (!isAllowed) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid XP Category: " + xpCategory));
        }
        String matchedCategory = allowedCategories.stream()
            .filter(cat -> cat.equalsIgnoreCase(xpCategory.trim()))
            .findFirst()
            .orElse(xpCategory);
        activity.setXpCategory(matchedCategory);
        
        // Parse & Validate Award XP
        Integer awardXp = 0;
        if (body.containsKey("awardXp")) {
            Object xpVal = body.get("awardXp");
            if (xpVal instanceof Number) {
                awardXp = ((Number) xpVal).intValue();
            } else if (xpVal instanceof String) {
                try {
                    awardXp = Integer.parseInt((String) xpVal);
                } catch (Exception ignored) {}
            }
        } else if (body.containsKey("xp")) {
            try {
                awardXp = Integer.parseInt(body.get("xp").toString());
            } catch (Exception ignored) {}
        }
        if (awardXp <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Award XP must be greater than zero"));
        }
        activity.setAwardXp(awardXp);

        // Parse Award Type
        String awardType = "Fixed XP";
        if (body.containsKey("awardType") && body.get("awardType") != null) {
            awardType = body.get("awardType").toString();
        }
        activity.setAwardType(awardType);

        // ── Award Frequency ───────────────────────────────────────────────────────
        List<String> validFrequencies = List.of("One Time", "Daily", "Weekly", "Monthly", "Manual");
        String awardFrequency = "One Time";
        if (body.containsKey("awardFrequency") && body.get("awardFrequency") != null) {
            awardFrequency = body.get("awardFrequency").toString().trim();
        } else if (body.containsKey("resetPeriod") && body.get("resetPeriod") != null) {
            // backward compat
            awardFrequency = body.get("resetPeriod").toString().trim();
        }
        final String awardFrequencyFinal = awardFrequency;
        boolean freqValid = validFrequencies.stream().anyMatch(f -> f.equalsIgnoreCase(awardFrequencyFinal));
        if (!freqValid) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                "Invalid Award Frequency. Must be one of: One Time, Daily, Weekly, Monthly, Manual"));
        }
        String matchedFrequency = validFrequencies.stream()
            .filter(f -> f.equalsIgnoreCase(awardFrequencyFinal)).findFirst().orElse("One Time");
        activity.setAwardFrequency(matchedFrequency);
        // Keep resetPeriod in sync for backward compat
        activity.setResetPeriod(matchedFrequency);
        activity.setRepeatAllowed(!matchedFrequency.equalsIgnoreCase("One Time"));

        // ── Cap ───────────────────────────────────────────────────────────────────
        Integer cap = 1;
        if (body.containsKey("cap") && body.get("cap") != null) {
            Object capVal = body.get("cap");
            if (capVal instanceof Number) cap = ((Number) capVal).intValue();
            else { try { cap = Integer.parseInt(capVal.toString()); } catch (Exception ignored) {} }
        } else if (body.containsKey("maximumAwards") && body.get("maximumAwards") != null) {
            Object maxA = body.get("maximumAwards");
            if (maxA instanceof Number) cap = ((Number) maxA).intValue();
            else { try { cap = Integer.parseInt(maxA.toString()); } catch (Exception ignored) {} }
        }
        if (matchedFrequency.equalsIgnoreCase("One Time") || matchedFrequency.equalsIgnoreCase("Manual")) {
            cap = 1;
        }
        if (cap <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cap must be greater than zero"));
        }
        activity.setMaximumAwards(cap);

        // ── Award Days (Weekly only) ───────────────────────────────────────────
        String awardDays = null;
        if (matchedFrequency.equalsIgnoreCase("Weekly")) {
            if (body.containsKey("awardDays") && body.get("awardDays") != null) {
                Object daysVal = body.get("awardDays");
                if (daysVal instanceof List) {
                    awardDays = ((List<?>) daysVal).stream().map(Object::toString).collect(Collectors.joining(","));
                } else {
                    awardDays = daysVal.toString().trim();
                }
            }
            if (awardDays == null || awardDays.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Award Days are required when Award Frequency is Weekly"));
            }
        }
        activity.setAwardDays(awardDays);

        int displayOrder = 0;
        if (body.containsKey("displayOrder")) {
            Object dispO = body.get("displayOrder");
            if (dispO instanceof Number) {
                displayOrder = ((Number) dispO).intValue();
            } else if (dispO instanceof String) {
                try {
                    displayOrder = Integer.parseInt((String) dispO);
                } catch (Exception ignored) {}
            }
        }
        activity.setDisplayOrder(displayOrder);

        String status = "ACTIVE";
        if (body.containsKey("status") && body.get("status") != null) {
            status = (String) body.get("status");
        }
        activity.setStatus(status);

        boolean evidenceRequired = true;
        if (body.containsKey("evidenceRequired") && body.get("evidenceRequired") != null) {
            evidenceRequired = Boolean.parseBoolean(body.get("evidenceRequired").toString());
        }
        activity.setEvidenceRequired(evidenceRequired);

        boolean isMandatory = true;
        if (body.containsKey("mandatory") && body.get("mandatory") != null) {
            isMandatory = Boolean.parseBoolean(body.get("mandatory").toString());
        } else if (body.containsKey("isMandatory") && body.get("isMandatory") != null) {
            isMandatory = Boolean.parseBoolean(body.get("isMandatory").toString());
        }
        activity.setMandatory(isMandatory);

        activity.setMaxPoints(100);

        Activity saved = activityRepository.save(activity);
        populateActivityTransientFields(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Activity created successfully", saved));
    }

    @PutMapping("/activities/{activityId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Update an activity")
    public ResponseEntity<ApiResponse<Activity>> updateActivity(
            @PathVariable Long activityId,
            @RequestBody Map<String, Object> body) {
        
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Activity not found"));
        }
        
        if (body.containsKey("name")) {
            String name = (String) body.get("name");
            activity.setName(name);
            activity.setActivityName(name);
        }
        if (body.containsKey("description")) {
            String desc = (String) body.get("description");
            activity.setDescription(desc);
            activity.setActivityDescription(desc);
        }
        if (body.containsKey("frequency")) {
            activity.setFrequency((String) body.get("frequency"));
        }
        // ownerDepartment field removed: events are global for all departments
        if (body.containsKey("evidence")) {
            Object evidenceObj = body.get("evidence");
            if (evidenceObj instanceof List) {
                List<?> evList = (List<?>) evidenceObj;
                activity.setEvidence(evList.stream().map(Object::toString).collect(Collectors.joining(", ")));
            } else if (evidenceObj instanceof String) {
                activity.setEvidence((String) evidenceObj);
            }
        }
        if (body.containsKey("xp")) {
            activity.setXp((String) body.get("xp"));
        }
        if (body.containsKey("cap")) {
            activity.setCap(body.get("cap"));
        }
        if (body.containsKey("type")) {
            String type = (String) body.get("type");
            activity.setType(type);
            activity.setModeType(type);
        }
        if (body.containsKey("justification")) {
            activity.setJustification((String) body.get("justification"));
        }
        if (body.containsKey("xpCategory")) {
            String xpCategory = (String) body.get("xpCategory");
            if (xpCategory == null || xpCategory.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("XP Category is required"));
            }
            List<String> allowedCategories = List.of(
                "Academic", "Skill", "Communication", "Leadership", "Discipline",
                "Placement", "Innovation", "Community", "Sports", "Cultural"
            );
            boolean isAllowed = allowedCategories.stream().anyMatch(cat -> cat.equalsIgnoreCase(xpCategory.trim()));
            if (!isAllowed) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid XP Category: " + xpCategory));
            }
            String matchedCategory = allowedCategories.stream()
                .filter(cat -> cat.equalsIgnoreCase(xpCategory.trim()))
                .findFirst()
                .orElse(xpCategory);
            activity.setXpCategory(matchedCategory);
        }

        if (body.containsKey("awardXp")) {
            Integer awardXp = 0;
            Object xpVal = body.get("awardXp");
            if (xpVal instanceof Number) {
                awardXp = ((Number) xpVal).intValue();
            } else if (xpVal instanceof String) {
                try {
                    awardXp = Integer.parseInt((String) xpVal);
                } catch (Exception ignored) {}
            }
            if (awardXp <= 0) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Award XP must be greater than zero"));
            }
            activity.setAwardXp(awardXp);
        }

        if (body.containsKey("awardType") && body.get("awardType") != null) {
            activity.setAwardType(body.get("awardType").toString());
        }

        // ── Award Frequency ───────────────────────────────────────────────────────
        List<String> validFrequencies2 = List.of("One Time", "Daily", "Weekly", "Monthly", "Manual");
        if (body.containsKey("awardFrequency") && body.get("awardFrequency") != null) {
            String af = body.get("awardFrequency").toString().trim();
            boolean afValid = validFrequencies2.stream().anyMatch(f -> f.equalsIgnoreCase(af));
            if (!afValid) {
                return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Invalid Award Frequency. Must be one of: One Time, Daily, Weekly, Monthly, Manual"));
            }
            String matchedAf = validFrequencies2.stream()
                .filter(f -> f.equalsIgnoreCase(af)).findFirst().orElse("One Time");
            activity.setAwardFrequency(matchedAf);
            activity.setResetPeriod(matchedAf);
            activity.setRepeatAllowed(!matchedAf.equalsIgnoreCase("One Time"));
        }

        // ── Cap ───────────────────────────────────────────────────────────────────
        String currentFreq = activity.getAwardFrequency();
        boolean isOneTimeOrManual = currentFreq.equalsIgnoreCase("One Time") || currentFreq.equalsIgnoreCase("Manual");
        if (body.containsKey("cap") && body.get("cap") != null && !isOneTimeOrManual) {
            Object capVal = body.get("cap");
            Integer newCap = null;
            if (capVal instanceof Number) newCap = ((Number) capVal).intValue();
            else { try { newCap = Integer.parseInt(capVal.toString()); } catch (Exception ignored) {} }
            if (newCap != null && newCap > 0) activity.setMaximumAwards(newCap);
        } else if (body.containsKey("maximumAwards") && body.get("maximumAwards") != null && !isOneTimeOrManual) {
            Object maxA = body.get("maximumAwards");
            Integer newMax = null;
            if (maxA instanceof Number) newMax = ((Number) maxA).intValue();
            else { try { newMax = Integer.parseInt(maxA.toString()); } catch (Exception ignored) {} }
            if (newMax != null && newMax > 0) activity.setMaximumAwards(newMax);
        }
        if (isOneTimeOrManual) activity.setMaximumAwards(1);

        // ── Award Days ────────────────────────────────────────────────────────────
        if (body.containsKey("awardDays")) {
            Object daysVal = body.get("awardDays");
            if (daysVal == null) {
                activity.setAwardDays(null);
            } else if (daysVal instanceof List) {
                activity.setAwardDays(((List<?>) daysVal).stream().map(Object::toString).collect(Collectors.joining(",")));
            } else {
                activity.setAwardDays(daysVal.toString().trim().isEmpty() ? null : daysVal.toString().trim());
            }
        }
        if (activity.getAwardFrequency().equalsIgnoreCase("Weekly") &&
            (activity.getAwardDays() == null || activity.getAwardDays().trim().isEmpty())) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                "Award Days are required when Award Frequency is Weekly"));
        }
        if (body.containsKey("displayOrder")) {
            Object dispO = body.get("displayOrder");
            if (dispO instanceof Number) {
                activity.setDisplayOrder(((Number) dispO).intValue());
            } else if (dispO instanceof String) {
                try {
                    activity.setDisplayOrder(Integer.parseInt((String) dispO));
                } catch (Exception ignored) {}
            }
        }
        if (body.containsKey("status") && body.get("status") != null) {
            activity.setStatus((String) body.get("status"));
        }
        if (body.containsKey("evidenceRequired") && body.get("evidenceRequired") != null) {
            activity.setEvidenceRequired(Boolean.parseBoolean(body.get("evidenceRequired").toString()));
        }
        if (body.containsKey("mandatory") && body.get("mandatory") != null) {
            activity.setMandatory(Boolean.parseBoolean(body.get("mandatory").toString()));
        } else if (body.containsKey("isMandatory") && body.get("isMandatory") != null) {
            activity.setMandatory(Boolean.parseBoolean(body.get("isMandatory").toString()));
        }

        Activity saved = activityRepository.save(activity);
        populateActivityTransientFields(saved);
        return ResponseEntity.ok(ApiResponse.ok("Activity updated successfully", saved));
    }

    @PostMapping(value = {"/activities/{id}/assign", "/activity/{id}/assign"})
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional
    @Operation(summary = "Assign a teacher to an activity")
    public ResponseEntity<ApiResponse<ActivityAssignmentResponse>> assignActivity(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        
        log.info("CC Assignment Request received for Activity ID: {}", id);
        
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        boolean isAdmin = currentUser != null && currentUser.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
        boolean isCc = currentUser != null && currentUser.getSubRoles().stream().map(com.spdms.entity.SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));
        
        if (!isAdmin && !isCc) {
            log.warn("Access Denied: User {} is neither admin nor CC", username);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access Denied: Only Admins or Class Coordinators can assign activities."));
        }

        Activity activity = activityRepository.findById(id).orElse(null);
        if (activity == null) {
            log.warn("Activity not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Activity not found"));
        }

        // Events are now global – ownerDepartment may be empty.
        // Resolve department from ownerDepartment if set, otherwise fall back to the requesting user's department.
        String ownerDeptName = activity.getOwnerDepartment();
        Department department = null;
        if (ownerDeptName != null && !ownerDeptName.trim().isEmpty()) {
            department = departmentRepository.findByName(ownerDeptName)
                .orElseGet(() -> departmentRepository.findByCode(ownerDeptName).orElse(null));
        }
        if (department == null && currentUser != null && currentUser.getDepartment() != null) {
            // Fall back to the requesting user's department (applicable for CC users on global events)
            department = currentUser.getDepartment();
            log.info("Activity {} has no owner department; using requesting user's department: {}", id, department.getName());
        }
        if (department == null) {
            log.warn("Could not resolve a department for activity {} assignment", id);
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot resolve department for assignment. Ensure the user has a department assigned."));
        }

        if (!isAdmin && currentUser.getDepartment() != null) {
            if (!currentUser.getDepartment().getId().equals(department.getId())) {
                log.warn("Access Denied: Coordinator department {} does not match activity department {}", currentUser.getDepartment().getId(), department.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access Denied: You can only assign activities for your own department."));
            }
        }

        if (body.get("teacherId") == null) {
            log.warn("teacherId is missing in request body");
            return ResponseEntity.badRequest().body(ApiResponse.error("teacherId is required"));
        }
        Long teacherId = Long.valueOf(body.get("teacherId").toString());
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new RuntimeException("Teacher not found"));

        Long sectionId = null;
        Section section = null;
        if (body.get("sectionId") != null && !body.get("sectionId").toString().isEmpty() && !body.get("sectionId").toString().equals("null")) {
            sectionId = Long.valueOf(body.get("sectionId").toString());
            section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found"));
        }

        log.info("Processing assignment: Teacher: {} ({}), Section: {}", teacher.getFullName(), teacher.getId(), section != null ? section.getSectionName() : "None");

        ActivityAssignment assignment;
        if (sectionId != null) {
            assignment = activityAssignmentRepository.findByActivityIdAndSectionId(id, sectionId)
                .orElse(new ActivityAssignment());
            assignment.setSection(section);
        } else {
            assignment = activityAssignmentRepository.findByActivityIdAndSectionIsNull(id)
                .orElse(new ActivityAssignment());
            assignment.setSection(null);
        }

        assignment.setActivity(activity);
        assignment.setDepartment(department);
        assignment.setTeacher(teacher);
        assignment.setAssignedBy(currentUser);
        assignment.setAssignedAt(LocalDateTime.now());

        log.info("Saving assignment to database. Current ID (null means new): {}", assignment.getId());
        ActivityAssignment saved = activityAssignmentRepository.saveAndFlush(assignment);
        log.info("Assignment successfully saved and flushed to database. Generated ID: {}", saved.getId());
        
        return ResponseEntity.ok(ApiResponse.ok("Teacher assigned successfully", toResponse(saved)));
    }

    @DeleteMapping("/activities/{activityId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Delete an activity")
    public ResponseEntity<ApiResponse<Void>> deleteActivity(@PathVariable Long activityId) {
        if (!activityRepository.existsById(activityId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Activity not found"));
        }
        disciplineLogRepository.nullifyActivityReferences(activityId);
        activityRepository.deleteById(activityId);
        log.info("Admin deleted activity with ID: {}", activityId);
        return ResponseEntity.ok(ApiResponse.ok("Activity deleted successfully", null));
    }
}
