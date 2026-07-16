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
import com.spdms.repository.StudentActivityXpRepository;
import com.spdms.entity.ActivityAssignment;
import com.spdms.entity.AssignmentScope;
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
import java.util.NoSuchElementException;
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
    private final StudentActivityXpRepository studentActivityXpRepository;
    private final ActivityStageService activityStageService;
    private final com.spdms.repository.CustomFrequencyRepository customFrequencyRepository;

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
            StudentActivityXpRepository studentActivityXpRepository,
            ActivityStageService activityStageService,
            com.spdms.repository.CustomFrequencyRepository customFrequencyRepository) {
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
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.activityStageService = activityStageService;
        this.customFrequencyRepository = customFrequencyRepository;
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
                .subRoles(resolveSubRoles(request.getSubRoles(), roles))
                .section(section)
                .year(request.getYear())
                .active(true)
                .build();

        User saved = userRepository.save(user);
        log.info("Admin created new user: {}", saved.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User created successfully", toResponse(saved)));
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
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllDepartments() {
        List<Department> depts = departmentRepository.findAll();
        List<Map<String, Object>> response = new ArrayList<>();
        for (Department d : depts) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId());
            map.put("code", d.getCode());
            map.put("deptCode", d.getDeptCode());
            map.put("name", d.getName());
            map.put("deptName", d.getDeptName());
            map.put("description", d.getDescription());
            
            // Query sections for this department
            List<Section> sections = sectionRepository.findByDepartment_Id(d.getId());
            List<Map<String, Object>> sectionMaps = new ArrayList<>();
            for (Section s : sections) {
                Map<String, Object> secMap = new HashMap<>();
                secMap.put("id", s.getId());
                secMap.put("sectionName", s.getSectionName());
                secMap.put("name", s.getSectionName()); // Both name and sectionName for compatibility
                secMap.put("departmentId", d.getId());
                sectionMaps.add(secMap);
            }
            map.put("sections", sectionMaps);
            response.add(map);
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Create Department")
    public ResponseEntity<ApiResponse<Department>> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request) {
        if (departmentRepository.findByCode(request.getCode()).isPresent()
                || departmentRepository.findByDeptCode(request.getCode()).isPresent()) {
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
        
        List<Section> savedSections = new ArrayList<>();
        if (request.getSections() != null) {
            for (String sec : request.getSections()) {
                Section section = new Section();
                section.setDepartment(saved);
                section.setSectionName(sec);
                savedSections.add(sectionRepository.save(section));
            }
        }
        saved.setSections(savedSections);
        
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

        if (departmentRepository.findByCode(request.getCode()).stream()
                .anyMatch(existing -> !existing.getId().equals(id)) ||
                departmentRepository.findByDeptCode(request.getCode()).stream()
                        .anyMatch(existing -> !existing.getId().equals(id))) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Department code already registered by another department"));
        }
        if (departmentRepository.findByName(request.getName()).stream()
                .anyMatch(existing -> !existing.getId().equals(id))) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Department name already registered by another department"));
        }

        dept.setName(request.getName());
        dept.setCode(request.getCode());
        dept.setDeptCode(request.getCode());
        dept.setDeptName(request.getName());
        dept.setDescription(request.getDescription());

        Department saved = departmentRepository.save(dept);
        
        if (request.getSections() != null) {
            sectionRepository.deleteByDepartment_Id(saved.getId());
            List<Section> savedSections = new ArrayList<>();
            for (String sec : request.getSections()) {
                Section section = new Section();
                section.setDepartment(saved);
                section.setSectionName(sec);
                savedSections.add(sectionRepository.save(section));
            }
            saved.setSections(savedSections);
        }

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

        long sections = sectionRepository.countByDepartment_Id(id);
        long students = studentRepository.countByDepartmentId(id);
        long faculty = facultyRepository.countByDepartmentId(id);
        long subjects = subjectRepository.countByDepartmentId(id);
        long subgroups = activitySubgroupRepository.countByAssignedDepartmentId(id);
        long users = userRepository.countByDepartmentId(id);
        long groups = studentGroupRepository.countByDepartmentId(id);

        java.util.List<String> deps = new java.util.ArrayList<>();
        if (sections > 0)
            deps.add(sections + " Section(s)");
        if (students > 0)
            deps.add(students + " Student(s)");
        if (faculty > 0)
            deps.add(faculty + " Faculty Member(s)");
        if (subjects > 0)
            deps.add(subjects + " Subject(s)");
        if (subgroups > 0)
            deps.add(subgroups + " Activity Subgroup(s)");
        if (users > 0)
            deps.add(users + " User(s)");
        if (groups > 0)
            deps.add(groups + " Student Group(s)");

        if (!deps.isEmpty()) {
            String msg = "Cannot delete Department because it contains: " + String.join(", ", deps)
                    + ". Remove or reassign them first.";
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(msg));
        }

        departmentRepository.deleteById(id);
        log.info("Admin deleted department with ID: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("Department deleted successfully", null));
    }

    @GetMapping("/departments/{id}/sections")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get Sections of Department")
    public ResponseEntity<ApiResponse<List<Section>>> getSectionsOfDept(@PathVariable Long id) {
        if (!departmentRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Department not found"));
        }
        List<Section> sections = sectionRepository.findByDepartment_IdOrderBySectionNameAsc(id);
        return ResponseEntity.ok(ApiResponse.ok("Sections retrieved successfully", sections));
    }

    @PostMapping("/departments/{id}/sections")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Create Section for Department")
    public ResponseEntity<ApiResponse<Section>> createSection(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Department dept = departmentRepository.findById(id).orElse(null);
        if (dept == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Department not found"));
        }
        String sectionName = (String) body.get("sectionName");
        if (sectionName == null || sectionName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Section name is required"));
        }
        sectionName = sectionName.trim().toUpperCase();
        if (sectionRepository.findByDepartmentAndSectionName(dept, sectionName).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Section already exists in this department"));
        }
        Section sec = Section.builder()
                .department(dept)
                .sectionName(sectionName)
                .build();
        Section saved = sectionRepository.save(sec);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Section created successfully", saved));
    }

    @DeleteMapping("/departments/{id}/sections/{sectionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Delete Section from Department")
    public ResponseEntity<ApiResponse<Void>> deleteSection(
            @PathVariable Long id,
            @PathVariable Long sectionId) {
        if (!departmentRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Department not found"));
        }
        if (!sectionRepository.existsById(sectionId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Section not found"));
        }
        sectionRepository.deleteById(sectionId);
        return ResponseEntity.ok(ApiResponse.ok("Section deleted successfully", null));
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
        return ResponseEntity
                .ok(ApiResponse.ok("Academic years fetched successfully", academicYearRepository.findAll()));
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
        // Only return clean gender values: Male, Female, Other
        List<String> validGenders = java.util.Arrays.asList("Male", "Female", "Other");
        List<Gender> filtered = genderRepository.findAll().stream()
                .filter(g -> validGenders.stream()
                        .anyMatch(valid -> valid.equalsIgnoreCase(g.getGenderName())))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Genders fetched successfully", filtered));
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
    public ResponseEntity<ApiResponse<ActivityStageResponse>> createStage(
            @Valid @RequestBody ActivityStageRequest request) {
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

    @GetMapping("/stages/{id}/report")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get stage completion report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStageReport(@PathVariable Long id) {
        try {
            Map<String, Object> report = activityStageService.getStageReport(id);
            return ResponseEntity.ok(ApiResponse.ok(report));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
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
                .section(user.getSection() != null ? user.getSection().getSectionName() : null)
                .sectionId(user.getSection() != null ? user.getSection().getId() : null)
                .sectionName(user.getSection() != null ? user.getSection().getSectionName() : null)
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
        if (body.get("userId") != null && !body.get("userId").toString().isEmpty()
                && !body.get("userId").toString().equals("null")) {
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
        log.info("Admin assigned faculty {} to subgroup {}", faculty != null ? faculty.getUsername() : "null",
                subgroup.getName());
        return ResponseEntity.ok(ApiResponse.ok("Faculty assigned successfully", saved));
    }

    private void populateActivityTransientFields(Activity activity) {
        List<ActivityAssignment> assignments = activityAssignmentRepository.findByActivityId(activity.getId());
        List<Map<String, Object>> summary = new ArrayList<>();
        
        for (ActivityAssignment aa : assignments) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", aa.getId());
            map.put("scope", aa.getAssignmentScope() != null ? aa.getAssignmentScope().name() : "");
            map.put("departmentId", aa.getDepartment() != null ? aa.getDepartment().getId() : null);
            map.put("departmentName", aa.getDepartment() != null ? aa.getDepartment().getName() : "Global");
            map.put("sectionId", aa.getSection() != null ? aa.getSection().getId() : null);
            map.put("section", aa.getSection() != null ? aa.getSection().getSectionName() : null);
            map.put("sectionName", aa.getSection() != null ? aa.getSection().getSectionName() : null);
            map.put("assignmentMode", activity.getAssignmentMode());
            
            if (aa.getTeacher() != null) {
                map.put("teacherId", aa.getTeacher().getId());
                map.put("teacherName", aa.getTeacher().getFullName());
                map.put("teacher", aa.getTeacher().getFullName());
                map.put("username", aa.getTeacher().getUsername());
            } else {
                map.put("teacherId", 0);
                map.put("teacherName", "Any Faculty");
                map.put("teacher", "Any Faculty");
                map.put("username", "any");
            }
            summary.add(map);
        }
        activity.setAssignmentSummary(summary);
        
        // Populate departmentId for backward compat if there's any department set
        if (!assignments.isEmpty() && assignments.get(0).getDepartment() != null) {
            activity.setDepartmentId(assignments.get(0).getDepartment().getId().toString());
        }
    }

    private ActivityAssignmentResponse toResponse(ActivityAssignment aa) {
        if (aa == null)
            return null;
        return ActivityAssignmentResponse.builder()
                .id(aa.getId())
                .activityId(aa.getActivity() != null ? aa.getActivity().getId() : null)
                .activityName(aa.getActivity() != null ? aa.getActivity().getName() : null)
                .departmentId(aa.getDepartment() != null ? aa.getDepartment().getId() : null)
                .departmentName(aa.getDepartment() != null ? aa.getDepartment().getName() : null)
                .sectionId(aa.getSection() != null ? aa.getSection().getId() : null)
                .sectionName(aa.getSection() != null ? aa.getSection().getSectionName() : null)
                .teacherId(aa.getTeacher() != null ? aa.getTeacher().getId() : 0L)
                .teacherName(aa.getTeacher() != null ? aa.getTeacher().getFullName() : "Any Faculty")
                .teacherUsername(aa.getTeacher() != null ? aa.getTeacher().getUsername() : "any")
                .assignedBy(aa.getAssignedBy() != null ? aa.getAssignedBy().getFullName() : null)
                .assignedAt(aa.getAssignedAt())
                .year(aa.getYear())
                .assignmentScope(aa.getAssignmentScope() != null ? aa.getAssignmentScope().name() : null)
                .build();
    }

    private static String normalizeYearToRoman(String yr) {
        if (yr == null)
            return null;
        String t = yr.trim().toUpperCase();
        if (t.equals("1") || t.equals("I"))
            return "I";
        if (t.equals("2") || t.equals("II"))
            return "II";
        if (t.equals("3") || t.equals("III"))
            return "III";
        if (t.equals("4") || t.equals("IV"))
            return "IV";
        return yr;
    }

    private boolean isAssignmentMatching(ActivityAssignment a, User u) {
        if (u.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"))) {
            return true;
        }
        
        // GLOBAL scope
        if (a.getAssignmentScope() == AssignmentScope.GLOBAL) {
            return true;
        }

        return a.getTeacher() != null && a.getTeacher().getId().equals(u.getId());
    }

    private ActivityAssignment getPriorityAssignment(List<ActivityAssignment> matches) {
        if (matches.isEmpty()) return null;
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.SPECIFIC_FACULTY) return a;
        }
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.SECTION) return a;
        }
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.DEPARTMENT) return a;
        }
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.GLOBAL) return a;
        }
        return matches.get(0);
    }

    @GetMapping("/my-activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get activities assigned to the currently logged in teacher")
    public ResponseEntity<ApiResponse<List<MyActivityResponse>>> getMyActivities() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("User not found"));
        }

        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findAll();

        List<ActivityAssignment> matchingAssignments = allAssignments.stream()
                .filter(a -> isAssignmentMatching(a, currentUser))
                .collect(Collectors.toList());

        // Group by Activity ID and get one assignment per activity
        Map<Long, List<ActivityAssignment>> assignmentsByActivity = matchingAssignments.stream()
                .collect(Collectors.groupingBy(a -> a.getActivity().getId()));

        List<MyActivityResponse> responses = new ArrayList<>();
        for (Map.Entry<Long, List<ActivityAssignment>> entry : assignmentsByActivity.entrySet()) {
            List<ActivityAssignment> activityAssignments = entry.getValue();
            ActivityAssignment aa = getPriorityAssignment(activityAssignments);
            Activity act = aa.getActivity();

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
                    .departmentName(aa.getDepartment() != null ? aa.getDepartment().getName() : "Global")
                    .sectionId(aa.getSection() != null ? aa.getSection().getId() : null)
                    .sectionName(aa.getSection() != null ? aa.getSection().getSectionName() : null)
                    .assignedBy(aa.getAssignedBy() != null ? aa.getAssignedBy().getFullName() : "")
                    .assignedAt(aa.getAssignedAt())
                    .xpCategory(act.getXpCategory())
                    .awardXp(act.getAwardXp())
                    .awardEnabled(act.getAwardEnabled())
                    .penaltyEnabled(act.getPenaltyEnabled())
                    .penaltyXp(act.getPenaltyXp())
                    .awardType(act.getAwardType())
                    .repeatAllowed(act.isRepeatAllowed())
                    .xpType(act.getXpType())
                    .cap(act.getCap())
                    .awardFrequency(act.getAwardFrequency())
                    .awardDays(act.getAwardDays())
                    .build());
        }

        responses.sort(java.util.Comparator.comparing(MyActivityResponse::getName));

        log.info("CC getMyActivities: Activities returned count: {}", responses.size());
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
        // Events are now global – all activities are visible to all users regardless of
        // department.

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

        String xpType = "Reward";
        if (body.containsKey("xpType") && body.get("xpType") != null) {
            xpType = body.get("xpType").toString().trim();
        }
        activity.setXpType(xpType);

        String xpCategory = (String) body.get("xpCategory");
        if (xpCategory == null || xpCategory.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("XP Category is required"));
        }
        List<String> allowedCategories = List.of(
                "Academic", "Skill", "Communication", "Leadership", "Discipline",
                "Placement", "Innovation", "Community", "Sports", "Cultural");
        boolean isAllowed = allowedCategories.stream().anyMatch(cat -> cat.equalsIgnoreCase(xpCategory.trim()));
        if (!isAllowed) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid XP Category: " + xpCategory));
        }
        String matchedCategory = allowedCategories.stream()
                .filter(cat -> cat.equalsIgnoreCase(xpCategory.trim()))
                .findFirst()
                .orElse(xpCategory);
        activity.setXpCategory(matchedCategory);

        // Parse & Validate Award XP & Penalty XP Configuration
        Boolean awardEnabled = false;
        if (body.containsKey("awardEnabled")) {
            Object val = body.get("awardEnabled");
            if (val instanceof Boolean) awardEnabled = (Boolean) val;
            else if (val instanceof String) awardEnabled = Boolean.parseBoolean((String) val);
        }
        Integer awardXp = 0;
        if (body.containsKey("awardXp")) {
            Object val = body.get("awardXp");
            if (val instanceof Number) awardXp = ((Number) val).intValue();
            else if (val instanceof String) {
                try { awardXp = Integer.parseInt((String) val); } catch (Exception ignored) {}
            }
        } else if (body.containsKey("xp")) {
            try { awardXp = Integer.parseInt(body.get("xp").toString()); } catch (Exception ignored) {}
        }

        Boolean penaltyEnabled = false;
        if (body.containsKey("penaltyEnabled")) {
            Object val = body.get("penaltyEnabled");
            if (val instanceof Boolean) penaltyEnabled = (Boolean) val;
            else if (val instanceof String) penaltyEnabled = Boolean.parseBoolean((String) val);
        }
        Integer penaltyXp = 0;
        if (body.containsKey("penaltyXp")) {
            Object val = body.get("penaltyXp");
            if (val instanceof Number) penaltyXp = ((Number) val).intValue();
            else if (val instanceof String) {
                try { penaltyXp = Integer.parseInt((String) val); } catch (Exception ignored) {}
            }
        }

        // Backward compatibility parsing from passXp / failXp / xpType / awardXp
        if (!body.containsKey("awardEnabled") && !body.containsKey("penaltyEnabled")) {
            Integer passXp = 0;
            if (body.containsKey("passXp")) {
                try { passXp = Integer.parseInt(body.get("passXp").toString()); } catch (Exception ignored) {}
            }
            Integer failXp = 0;
            if (body.containsKey("failXp")) {
                try { failXp = Integer.parseInt(body.get("failXp").toString()); } catch (Exception ignored) {}
            }
            if (passXp > 0 || failXp > 0) {
                awardEnabled = passXp > 0;
                awardXp = passXp;
                penaltyEnabled = failXp > 0;
                penaltyXp = failXp;
            } else {
                String reqXpType = body.containsKey("xpType") && body.get("xpType") != null ? body.get("xpType").toString() : "Reward";
                if ("Penalty".equalsIgnoreCase(reqXpType) || "Discipline".equalsIgnoreCase(reqXpType)) {
                    penaltyEnabled = true;
                    penaltyXp = awardXp;
                    awardEnabled = false;
                    awardXp = 0;
                } else if ("Mixed".equalsIgnoreCase(reqXpType)) {
                    awardEnabled = true;
                    penaltyEnabled = true;
                    penaltyXp = awardXp;
                } else {
                    awardEnabled = true;
                    penaltyEnabled = false;
                    penaltyXp = 0;
                }
            }
        }

        // Validation Rules:
        if (!awardEnabled && !penaltyEnabled) {
            return ResponseEntity.badRequest().body(ApiResponse.error("At least one XP Configuration (Award or Penalty) must be enabled"));
        }
        if (awardEnabled) {
            if (awardXp == null || awardXp < 0 || (!penaltyEnabled && awardXp == 0)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Award XP value must be greater than zero when enabled"));
            }
        } else {
            awardXp = 0;
        }
        if (penaltyEnabled) {
            if (penaltyXp == null || penaltyXp <= 0) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Penalty XP value must be greater than zero when enabled"));
            }
        } else {
            penaltyXp = 0;
        }

        activity.setAwardEnabled(awardEnabled);
        activity.setAwardXp(awardXp);
        activity.setPenaltyEnabled(penaltyEnabled);
        activity.setPenaltyXp(penaltyXp);

        // Parse Award Type
        String awardType = "Fixed XP";
        if (body.containsKey("awardType") && body.get("awardType") != null) {
            awardType = body.get("awardType").toString();
        }
        activity.setAwardType(awardType);

        // ── Award Frequency ───────────────────────────────────────────────────────
        List<String> validFrequencies = List.of("One Time", "Daily", "Weekly", "Monthly", "Every Period", "Per Assignment", "Manual");
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
            freqValid = customFrequencyRepository.findByNameIgnoreCase(awardFrequencyFinal).isPresent();
        }
        if (!freqValid) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Invalid Award Frequency. Must be one of: One Time, Daily, Weekly, Monthly, Every Period, Per Assignment, Manual, or a registered Custom Frequency"));
        }
        String matchedFrequency = validFrequencies.stream()
                .filter(f -> f.equalsIgnoreCase(awardFrequencyFinal)).findFirst().orElse(awardFrequencyFinal);
        activity.setAwardFrequency(matchedFrequency);
        // Keep resetPeriod in sync for backward compat
        activity.setResetPeriod(matchedFrequency);
        activity.setRepeatAllowed(!matchedFrequency.equalsIgnoreCase("One Time"));

        // ── Cap ───────────────────────────────────────────────────────────────────
        Integer cap = 1;
        if (body.containsKey("cap") && body.get("cap") != null) {
            Object capVal = body.get("cap");
            if (capVal instanceof Number)
                cap = ((Number) capVal).intValue();
            else {
                try {
                    cap = Integer.parseInt(capVal.toString());
                } catch (Exception ignored) {
                }
            }
        } else if (body.containsKey("maximumAwards") && body.get("maximumAwards") != null) {
            Object maxA = body.get("maximumAwards");
            if (maxA instanceof Number)
                cap = ((Number) maxA).intValue();
            else {
                try {
                    cap = Integer.parseInt(maxA.toString());
                } catch (Exception ignored) {
                }
            }
        }
        if (matchedFrequency.equalsIgnoreCase("One Time") || matchedFrequency.equalsIgnoreCase("Manual")) {
            cap = 1;
        }
        if (matchedFrequency.equalsIgnoreCase("Every Period")) {
            cap = 8;
        }
        if (matchedFrequency.equalsIgnoreCase("Per Assignment")) {
            cap = null;
        }
        if (cap != null && cap <= 0) {
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
                } catch (Exception ignored) {
                }
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

        log.info("Entity before save [Create] - Award Enabled: {}, Award XP: {}, Penalty Enabled: {}, Penalty XP: {}",
                 activity.getAwardEnabled(), activity.getAwardXp(), activity.getPenaltyEnabled(), activity.getPenaltyXp());
        Activity saved = activityRepository.save(activity);
        log.info("Entity after save [Create] - Award Enabled: {}, Award XP: {}, Penalty Enabled: {}, Penalty XP: {}",
                 saved.getAwardEnabled(), saved.getAwardXp(), saved.getPenaltyEnabled(), saved.getPenaltyXp());
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
        if (body.containsKey("xpType") && body.get("xpType") != null) {
            activity.setXpType(body.get("xpType").toString().trim());
        }
        if (body.containsKey("xpCategory")) {
            String xpCategory = (String) body.get("xpCategory");
            if (xpCategory == null || xpCategory.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("XP Category is required"));
            }
            List<String> allowedCategories = List.of(
                    "Academic", "Skill", "Communication", "Leadership", "Discipline",
                    "Placement", "Innovation", "Community", "Sports", "Cultural");
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

        if (body.containsKey("awardEnabled") || body.containsKey("penaltyEnabled") || body.containsKey("awardXp") || body.containsKey("penaltyXp") || body.containsKey("passXp") || body.containsKey("failXp") || body.containsKey("xpType")) {
            Boolean awardEnabled = activity.getAwardEnabled();
            if (body.containsKey("awardEnabled")) {
                Object val = body.get("awardEnabled");
                if (val instanceof Boolean) awardEnabled = (Boolean) val;
                else if (val instanceof String) awardEnabled = Boolean.parseBoolean((String) val);
            }
            Integer awardXp = activity.getAwardXp();
            if (body.containsKey("awardXp")) {
                Object val = body.get("awardXp");
                if (val instanceof Number) awardXp = ((Number) val).intValue();
                else if (val instanceof String) {
                    try { awardXp = Integer.parseInt((String) val); } catch (Exception ignored) {}
                }
            } else if (body.containsKey("xp")) {
                try { awardXp = Integer.parseInt(body.get("xp").toString()); } catch (Exception ignored) {}
            }

            Boolean penaltyEnabled = activity.getPenaltyEnabled();
            if (body.containsKey("penaltyEnabled")) {
                Object val = body.get("penaltyEnabled");
                if (val instanceof Boolean) penaltyEnabled = (Boolean) val;
                else if (val instanceof String) penaltyEnabled = Boolean.parseBoolean((String) val);
            }
            Integer penaltyXp = activity.getPenaltyXp();
            if (body.containsKey("penaltyXp")) {
                Object val = body.get("penaltyXp");
                if (val instanceof Number) penaltyXp = ((Number) val).intValue();
                else if (val instanceof String) {
                    try { penaltyXp = Integer.parseInt((String) val); } catch (Exception ignored) {}
                }
            }

            // Backward compatibility checks during updates
            if (!body.containsKey("awardEnabled") && !body.containsKey("penaltyEnabled")) {
                if (body.containsKey("passXp") || body.containsKey("failXp")) {
                    Integer passXp = body.containsKey("passXp") ? Integer.parseInt(body.get("passXp").toString()) : (activity.getAwardEnabled() ? activity.getAwardXp() : 0);
                    Integer failXp = body.containsKey("failXp") ? Integer.parseInt(body.get("failXp").toString()) : (activity.getPenaltyEnabled() ? activity.getPenaltyXp() : 0);
                    awardEnabled = passXp > 0;
                    awardXp = passXp;
                    penaltyEnabled = failXp > 0;
                    penaltyXp = failXp;
                } else if (body.containsKey("xpType")) {
                    String reqXpType = body.get("xpType").toString();
                    if ("Penalty".equalsIgnoreCase(reqXpType) || "Discipline".equalsIgnoreCase(reqXpType)) {
                        penaltyEnabled = true;
                        penaltyXp = awardXp;
                        awardEnabled = false;
                        awardXp = 0;
                    } else if ("Mixed".equalsIgnoreCase(reqXpType)) {
                        awardEnabled = true;
                        penaltyEnabled = true;
                        penaltyXp = awardXp;
                    } else {
                        awardEnabled = true;
                        penaltyEnabled = false;
                        penaltyXp = 0;
                    }
                }
            }

            if (!awardEnabled && !penaltyEnabled) {
                return ResponseEntity.badRequest().body(ApiResponse.error("At least one XP Configuration (Award or Penalty) must be enabled"));
            }
            if (awardEnabled) {
                if (awardXp == null || awardXp < 0 || (!penaltyEnabled && awardXp == 0)) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("Award XP value must be greater than zero when enabled"));
                }
            } else {
                awardXp = 0;
            }
            if (penaltyEnabled) {
                if (penaltyXp == null || penaltyXp <= 0) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("Penalty XP value must be greater than zero when enabled"));
                }
            } else {
                penaltyXp = 0;
            }

            activity.setAwardEnabled(awardEnabled);
            activity.setAwardXp(awardXp);
            activity.setPenaltyEnabled(penaltyEnabled);
            activity.setPenaltyXp(penaltyXp);
        }

        if (body.containsKey("awardType") && body.get("awardType") != null) {
            activity.setAwardType(body.get("awardType").toString());
        }

        // ── Award Frequency ───────────────────────────────────────────────────────
        List<String> validFrequencies2 = List.of("One Time", "Daily", "Weekly", "Monthly", "Every Period", "Per Assignment", "Manual");
        if (body.containsKey("awardFrequency") && body.get("awardFrequency") != null) {
            String af = body.get("awardFrequency").toString().trim();
            boolean afValid = validFrequencies2.stream().anyMatch(f -> f.equalsIgnoreCase(af));
            if (!afValid) {
                afValid = customFrequencyRepository.findByNameIgnoreCase(af).isPresent();
            }
            if (!afValid) {
                return ResponseEntity.badRequest().body(ApiResponse.error(
                        "Invalid Award Frequency. Must be one of: One Time, Daily, Weekly, Monthly, Every Period, Per Assignment, Manual, or a registered Custom Frequency"));
            }
            String matchedAf = validFrequencies2.stream()
                    .filter(f -> f.equalsIgnoreCase(af)).findFirst().orElse(af);
            activity.setAwardFrequency(matchedAf);
            activity.setResetPeriod(matchedAf);
            activity.setRepeatAllowed(!matchedAf.equalsIgnoreCase("One Time"));
        }

        // ── Cap ───────────────────────────────────────────────────────────────────
        String currentFreq = activity.getAwardFrequency();
        boolean isOneTimeOrManual = currentFreq.equalsIgnoreCase("One Time") || currentFreq.equalsIgnoreCase("Manual");
        boolean isEveryPeriod = currentFreq.equalsIgnoreCase("Every Period");
        if (body.containsKey("cap") && body.get("cap") != null && !isOneTimeOrManual && !isEveryPeriod) {
            Object capVal = body.get("cap");
            Integer newCap = null;
            if (capVal instanceof Number)
                newCap = ((Number) capVal).intValue();
            else {
                try {
                    newCap = Integer.parseInt(capVal.toString());
                } catch (Exception ignored) {
                }
            }
            if (newCap != null && newCap > 0)
                activity.setMaximumAwards(newCap);
        } else if (body.containsKey("maximumAwards") && body.get("maximumAwards") != null && !isOneTimeOrManual && !isEveryPeriod) {
            Object maxA = body.get("maximumAwards");
            Integer newMax = null;
            if (maxA instanceof Number)
                newMax = ((Number) maxA).intValue();
            else {
                try {
                    newMax = Integer.parseInt(maxA.toString());
                } catch (Exception ignored) {
                }
            }
            if (newMax != null && newMax > 0)
                activity.setMaximumAwards(newMax);
        }
        if (isOneTimeOrManual)
            activity.setMaximumAwards(1);
        if (isEveryPeriod)
            activity.setMaximumAwards(8);
        if (currentFreq.equalsIgnoreCase("Per Assignment"))
            activity.setMaximumAwards(null);

        // ── Award Days ────────────────────────────────────────────────────────────
        if (body.containsKey("awardDays")) {
            Object daysVal = body.get("awardDays");
            if (daysVal == null) {
                activity.setAwardDays(null);
            } else if (daysVal instanceof List) {
                activity.setAwardDays(
                        ((List<?>) daysVal).stream().map(Object::toString).collect(Collectors.joining(",")));
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
                } catch (Exception ignored) {
                }
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

        log.info("Entity before save [Update] - Award Enabled: {}, Award XP: {}, Penalty Enabled: {}, Penalty XP: {}",
                 activity.getAwardEnabled(), activity.getAwardXp(), activity.getPenaltyEnabled(), activity.getPenaltyXp());
        Activity saved = activityRepository.save(activity);
        log.info("Entity after save [Update] - Award Enabled: {}, Award XP: {}, Penalty Enabled: {}, Penalty XP: {}",
                 saved.getAwardEnabled(), saved.getAwardXp(), saved.getPenaltyEnabled(), saved.getPenaltyXp());
        populateActivityTransientFields(saved);
        return ResponseEntity.ok(ApiResponse.ok("Activity updated successfully", saved));
    }

    @PostMapping(value = { "/activities/{id}/assign", "/activity/{id}/assign" })
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Assign departments/sections/faculty to an activity")
    public ResponseEntity<ApiResponse<Void>> assignActivity(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        
        log.info("Admin Assignment Request received for Activity ID: {}", id);
        
        Activity activity = activityRepository.findById(id).orElse(null);
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Activity not found"));
        }
        
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);

        // First delete referencing student activity xp records to prevent constraint violation
        studentActivityXpRepository.deleteByActivityId(id);

        // Delete all existing assignments
        activityAssignmentRepository.deleteByActivityId(id);

        boolean ccEnabled = Boolean.TRUE.equals(body.get("ccEnabled"));
        boolean globalEnabled = Boolean.TRUE.equals(body.get("globalEnabled"));

        if (ccEnabled) {
            // ── CLASS COORDINATOR ASSIGNMENT MODE ──────────────────────────────
            activity.setAssignmentMode("CLASS_COORDINATOR");
            activityRepository.save(activity);

            List<Department> allDepts = departmentRepository.findAll();
            List<String> warnings = new ArrayList<>();

            for (Department dept : allDepts) {
                List<Section> sections = sectionRepository.findByDepartment_Id(dept.getId());
                for (Section sec : sections) {
                    log.info("Checking CC for Department: {} (ID: {}), Section: {} (ID: {})", dept.getName(), dept.getId(), sec.getSectionName(), sec.getId());
                    java.util.List<User> ccs = userRepository.findClassCoordinatorsByDepartmentAndSection(dept.getId(), sec.getId());
                    if (ccs.isEmpty()) {
                        // Debug WHY it failed
                        java.util.List<User> allUsersInSection = userRepository.findAll().stream()
                            .filter(u -> u.getSection() != null && u.getSection().getId().equals(sec.getId()))
                            .collect(Collectors.toList());
                        
                        if (allUsersInSection.isEmpty()) {
                            log.warn("No users found in Section ID: {}", sec.getId());
                        } else {
                            for (User u : allUsersInSection) {
                                boolean hasTeacherRole = u.getRoles().stream().anyMatch(r -> "ROLE_TEACHER".equals(r.getName()));
                                boolean hasCCSubRole = u.getSubRoles().stream().anyMatch(sr -> "CC".equalsIgnoreCase(sr.getName()));
                                log.warn("User in section - ID: {}, Name: {}, Dept ID: {}, Sec ID: {}, hasTeacherRole: {}, hasCCSubRole: {}, active: {}", 
                                    u.getId(), u.getFullName(), 
                                    u.getDepartment() != null ? u.getDepartment().getId() : "null",
                                    u.getSection() != null ? u.getSection().getId() : "null",
                                    hasTeacherRole, hasCCSubRole, u.isActive());
                            }
                        }
                        warnings.add("Section " + sec.getSectionName() + " (" + dept.getName() + "): No Class Coordinator assigned");
                        continue;
                    }
                    User cc = ccs.get(0);
                    log.info("✔ CC Found - Teacher: {}, Department: {}, Section: {}, Teacher ID: {}", cc.getFullName(), dept.getName(), sec.getSectionName(), cc.getId());
                    ActivityAssignment aa = new ActivityAssignment();
                    aa.setActivity(activity);
                    aa.setAssignmentScope(AssignmentScope.SECTION);
                    aa.setDepartment(dept);
                    aa.setSection(sec);
                    aa.setTeacher(cc);
                    aa.setAssignedBy(currentUser);
                    aa.setAssignedAt(LocalDateTime.now());
                    aa.setYear("1");
                    activityAssignmentRepository.save(aa);
                    log.info("CC Assignment: Activity {} → {} (CC of {} / {})", id, cc.getFullName(), dept.getName(), sec.getSectionName());
                }
            }

            if (!warnings.isEmpty()) {
                log.warn("CC Assignment completed with warnings: {}", warnings);
            }
            return ResponseEntity.ok(ApiResponse.ok("Class Coordinator assignments saved successfully", null));

        } else if (globalEnabled) {
            // ── GLOBAL ASSIGNMENT MODE ─────────────────────────────────────────
            activity.setAssignmentMode("GLOBAL");
            activityRepository.save(activity);

            List<Department> allDepts = departmentRepository.findAll();
            for (Department dept : allDepts) {
                ActivityAssignment aa = new ActivityAssignment();
                aa.setActivity(activity);
                aa.setAssignmentScope(AssignmentScope.GLOBAL);
                aa.setDepartment(dept);
                aa.setAssignedBy(currentUser);
                aa.setAssignedAt(LocalDateTime.now());
                aa.setYear("1");
                activityAssignmentRepository.save(aa);
            }
        } else {
            // ── MANUAL ASSIGNMENT MODE ─────────────────────────────────────────
            activity.setAssignmentMode("MANUAL");
            activityRepository.save(activity);

            List<Map<String, Object>> assignmentsList = (List<Map<String, Object>>) body.get("assignments");
            if (assignmentsList != null) {
                for (Map<String, Object> item : assignmentsList) {
                    ActivityAssignment aa = new ActivityAssignment();
                    aa.setActivity(activity);
                    
                    String scopeStr = (String) item.get("scope");
                    AssignmentScope scope = AssignmentScope.valueOf(scopeStr);
                    aa.setAssignmentScope(scope);

                    if (item.get("departmentId") != null) {
                        Long deptId = Long.valueOf(item.get("departmentId").toString());
                        Department dept = departmentRepository.findById(deptId).orElse(null);
                        aa.setDepartment(dept);
                    }
                    
                    if (item.get("sectionId") != null) {
                        Long secId = Long.valueOf(item.get("sectionId").toString());
                        Section sec = sectionRepository.findById(secId).orElse(null);
                        aa.setSection(sec);
                    }

                    if (item.get("facultyId") != null) {
                        Long facId = Long.valueOf(item.get("facultyId").toString());
                        User teacher = userRepository.findById(facId).orElse(null);
                        aa.setTeacher(teacher);
                    }

                    String year = (String) item.get("year");
                    if (year == null || year.trim().isEmpty()) {
                        year = "1";
                    }
                    aa.setYear(year);

                    aa.setAssignedBy(currentUser);
                    aa.setAssignedAt(LocalDateTime.now());
                    activityAssignmentRepository.save(aa);
                }
            }
        }

        return ResponseEntity.ok(ApiResponse.ok("Assignments updated successfully", null));
    }

    @DeleteMapping("/activities/{activityId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Delete an activity")
    public ResponseEntity<ApiResponse<Void>> deleteActivity(@PathVariable Long activityId) {
        if (!activityRepository.existsById(activityId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Activity not found"));
        }
        studentActivityXpRepository.deleteByActivityId(activityId);
        activityAssignmentRepository.deleteByActivityId(activityId);
        disciplineLogRepository.nullifyActivityReferences(activityId);
        activityRepository.deleteById(activityId);
        log.info("Admin deleted activity with ID: {}", activityId);
        return ResponseEntity.ok(ApiResponse.ok("Activity deleted successfully", null));
    }

    @GetMapping("/departments/class-coordinators")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get all class coordinators mapped by department and section")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getClassCoordinators() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<User> users = userRepository.findAll();
        for (User u : users) {
            boolean isTeacher = u.getRoles().stream().anyMatch(r -> "ROLE_TEACHER".equals(r.getName()));
            boolean isCC = u.getSubRoles().stream().anyMatch(sr -> "CC".equalsIgnoreCase(sr.getName()));
            if (isTeacher && isCC && u.isActive()) {
                Map<String, Object> map = new HashMap<>();
                map.put("department", u.getDepartment() != null ? u.getDepartment().getName() : null);
                map.put("departmentId", u.getDepartment() != null ? u.getDepartment().getId() : null);
                map.put("section", u.getSection() != null ? u.getSection().getSectionName() : null);
                map.put("sectionId", u.getSection() != null ? u.getSection().getId() : null);
                map.put("teacher", u.getFullName());
                map.put("teacherId", u.getId());
                result.add(map);
            }
        }
        return ResponseEntity.ok(ApiResponse.ok("Class Coordinators fetched successfully", result));
    }

    @GetMapping("/frequencies/custom")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all custom award frequencies")
    public ResponseEntity<ApiResponse<List<com.spdms.entity.CustomFrequency>>> getCustomFrequencies() {
        return ResponseEntity.ok(ApiResponse.ok("Fetched custom frequencies", customFrequencyRepository.findAll()));
    }

    @PostMapping("/frequencies/custom")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a custom award frequency")
    public ResponseEntity<ApiResponse<com.spdms.entity.CustomFrequency>> createCustomFrequency(
            @RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Name is required"));
        }
        name = name.trim();

        if (customFrequencyRepository.findByNameIgnoreCase(name).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Custom frequency with this name already exists"));
        }

        String capType = (String) payload.getOrDefault("capType", "UNLIMITED");
        Integer defaultCap = payload.containsKey("defaultCap") && payload.get("defaultCap") != null
                ? Integer.parseInt(payload.get("defaultCap").toString())
                : 0;

        com.spdms.entity.CustomFrequency freq = new com.spdms.entity.CustomFrequency(name, capType, defaultCap);
        freq = customFrequencyRepository.save(freq);

        return ResponseEntity.ok(ApiResponse.ok("Custom frequency created", freq));
    }

    @jakarta.annotation.PostConstruct
    @org.springframework.transaction.annotation.Transactional
    public void migrateLegacyNullYears() {
        log.info("Starting default year migration for ActivityAssignment records...");
        List<ActivityAssignment> assignments = activityAssignmentRepository.findAll();
        boolean changed = false;
        for (ActivityAssignment aa : assignments) {
            if (aa.getYear() == null || aa.getYear().trim().isEmpty()) {
                aa.setYear("1");
                activityAssignmentRepository.save(aa);
                changed = true;
            }
        }
        if (changed) {
            log.info("Default year migration completed successfully.");
        } else {
            log.info("No legacy ActivityAssignment records needed migration.");
        }
    }
}
