package jjcet.PragatiX.modules.admin.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.admin.dto.request.CreateDepartmentRequest;
import jjcet.PragatiX.entity.Department;
import jjcet.PragatiX.entity.Section;
import jjcet.PragatiX.repository.DepartmentRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.activity.repository.ActivitySubgroupRepository;
import jjcet.PragatiX.repository.SubjectRepository;
import jjcet.PragatiX.repository.SectionRepository;
import jjcet.PragatiX.modules.faculty.repository.FacultyRepository;
import jjcet.PragatiX.modules.student.repository.StudentGroupRepository;
import jjcet.PragatiX.repository.ActivityAssignmentRepository;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import jjcet.PragatiX.repository.YearRepository;
import jjcet.PragatiX.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDepartmentCommandService {

    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final DepartmentRepository departmentRepository;
    private final FacultyRepository facultyRepository;
    private final SectionRepository sectionRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final AuthUtils authUtils;
    private final YearRepository yearRepository;
    private final jjcet.PragatiX.modules.audit.service.AuditService auditService;
    
    @PersistenceContext
    private EntityManager entityManager;

    public AdminDepartmentCommandService(ActivitySubgroupRepository activitySubgroupRepository,
            ActivityAssignmentRepository activityAssignmentRepository,
            DepartmentRepository departmentRepository, FacultyRepository facultyRepository,
            SectionRepository sectionRepository, StudentGroupRepository studentGroupRepository,
            StudentRepository studentRepository, SubjectRepository subjectRepository, UserRepository userRepository,
            AuthUtils authUtils, YearRepository yearRepository, jjcet.PragatiX.modules.audit.service.AuditService auditService) {
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.departmentRepository = departmentRepository;
        this.facultyRepository = facultyRepository;
        this.sectionRepository = sectionRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.authUtils = authUtils;
        this.yearRepository = yearRepository;
        this.auditService = auditService;
    }

    private static final List<String> DEFAULT_SECTION_DEPTS = java.util.Arrays.asList(
            "Aeronautical Engineering",
            "Artificial Intelligence and Data Science",
            "Civil Engineering",
            "Computer Science and Engineering",
            "Computer Science and Engineering (Cyber Security)",
            "Electrical and Electronics Engineering",
            "Electronics and Communication Engineering",
            "Information Technology",
            "Mechanical Engineering"
    );

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllDepartments(boolean all) {
        List<Department> depts = departmentRepository.findAll().stream()
                .filter(d -> !d.isDeleted())
                .filter(d -> all || Boolean.TRUE.equals(d.getSupportsSections()))
                .collect(java.util.stream.Collectors.toList());

        List<Map<String, Object>> response = new ArrayList<>();

        List<Section> allSections = sectionRepository.findAll();
        Map<Long, List<Section>> sectionsByDept = allSections.stream()
                .filter(s -> s.getDepartment() != null)
                .collect(java.util.stream.Collectors.groupingBy(s -> s.getDepartment().getId()));

        for (Department d : depts) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId());
            map.put("code", d.getCode());
            map.put("deptCode", d.getDeptCode());
            map.put("departmentId", d.getId());
            map.put("name", d.getName());
            map.put("departmentName", d.getName());
            map.put("deptName", d.getDeptName());
            map.put("description", d.getDescription());

            List<Section> sections = sectionsByDept.getOrDefault(d.getId(), new ArrayList<>());
            List<Map<String, Object>> sectionMaps = new ArrayList<>();
            for (Section s : sections) {
                Map<String, Object> secMap = new HashMap<>();
                secMap.put("id", s.getId());
                secMap.put("sectionName", s.getSectionName());
                secMap.put("name", s.getSectionName());
                secMap.put("departmentId", d.getId());
                sectionMaps.add(secMap);
            }
            map.put("sections", sectionMaps);
            map.put("hasSections", !sections.isEmpty());
            
            boolean supportsSec = d.getSupportsSections() != null ? d.getSupportsSections() : 
                                  DEFAULT_SECTION_DEPTS.contains(d.getName()) || DEFAULT_SECTION_DEPTS.contains(d.getDeptName());
            map.put("supportsSections", supportsSec);
            
            response.add(map);
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Department>> createDepartment(CreateDepartmentRequest request) {
        if (departmentRepository.findByCodeIgnoreCase(request.getCode()).isPresent()
                || departmentRepository.findByDeptCodeIgnoreCase(request.getCode()).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Department code already exists"));
        }
        if (departmentRepository.findByNameIgnoreCase(request.getName()).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Department name already exists"));
        }
        Department dept = Department.builder()
                .deptCode(request.getCode())
                .deptName(request.getName())
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .supportsSections(request.getSupportsSections() != null ? request.getSupportsSections() : false)
                .build();
        Department saved = departmentRepository.save(dept);

        List<Section> savedSections = new ArrayList<>();
        if (request.getSections() != null) {
            List<Section> sectionsToSave = new ArrayList<>();
            for (String sec : request.getSections()) {
                Section section = new Section();
                section.setDepartment(saved);
                section.setSectionName(sec);
                sectionsToSave.add(section);
            }
            savedSections = sectionRepository.saveAll(sectionsToSave);
        }
        saved.setSections(savedSections);

        auditService.log(
            jjcet.PragatiX.enums.AuditAction.CREATE,
            jjcet.PragatiX.enums.AuditModule.DEPARTMENT,
            "DEPARTMENT",
            saved.getId(),
            "Created department " + saved.getName()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Department created successfully", saved));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Department>> updateDepartment(Long id, CreateDepartmentRequest request) {
        Department dept = departmentRepository.findById(id).orElse(null);
        if (dept == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Department not found"));
        }

        if (departmentRepository.findByCodeIgnoreCase(request.getCode()).stream()
                .anyMatch(existing -> !existing.getId().equals(id) && !existing.isDeleted()) ||
                departmentRepository.findByDeptCodeIgnoreCase(request.getCode()).stream()
                        .anyMatch(existing -> !existing.getId().equals(id) && !existing.isDeleted())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Department code already registered by another department"));
        }
        if (departmentRepository.findByNameIgnoreCase(request.getName()).stream()
                .anyMatch(existing -> !existing.getId().equals(id) && !existing.isDeleted())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Department name already registered by another department"));
        }

        dept.setName(request.getName());
        dept.setCode(request.getCode());
        dept.setDeptCode(request.getCode());
        dept.setDeptName(request.getName());
        dept.setDescription(request.getDescription());
        if (request.getSupportsSections() != null) {
            dept.setSupportsSections(request.getSupportsSections());
        }

        if (request.getSections() != null) {
            if (dept.getSections() == null) {
                dept.setSections(new ArrayList<>());
            }
            java.util.Set<String> existingNames = dept.getSections().stream()
                    .map(Section::getSectionName)
                    .filter(java.util.Objects::nonNull)
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .collect(java.util.stream.Collectors.toSet());

            for (String secName : request.getSections()) {
                if (secName != null && !secName.trim().isEmpty()) {
                    String trimmedUpper = secName.trim();
                    if (!existingNames.contains(trimmedUpper.toUpperCase())) {
                        Section section = new Section();
                        section.setDepartment(dept);
                        section.setSectionName(trimmedUpper);
                        dept.getSections().add(section);
                        existingNames.add(trimmedUpper.toUpperCase());
                    }
                }
            }
        }

        Department saved = departmentRepository.save(dept);

        java.util.Map<String, Object> oldValues = new java.util.HashMap<>();
        java.util.Map<String, Object> newValues = new java.util.HashMap<>();
        newValues.put("code", saved.getCode());
        newValues.put("name", saved.getName());
        newValues.put("description", saved.getDescription());

        auditService.log(
            jjcet.PragatiX.enums.AuditAction.UPDATE,
            jjcet.PragatiX.enums.AuditModule.DEPARTMENT,
            "DEPARTMENT",
            saved.getId(),
            "Updated department " + saved.getName(),
            oldValues,
            newValues
        );

        return ResponseEntity.ok(ApiResponse.ok("Department updated successfully", saved));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id).orElse(null);
        if (department == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Department not found"));
        }

        department.setDeleted(true);
        department.setDeletedAt(java.time.LocalDateTime.now());
        department.setPermanentDeleteAt(java.time.LocalDateTime.now().plusDays(30));
        
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            department.setDeletedBy(auth.getName());
        }
        
        departmentRepository.save(department);

        auditService.log(
                jjcet.PragatiX.enums.AuditAction.DELETE,
                jjcet.PragatiX.enums.AuditModule.DEPARTMENT,
                "DEPARTMENT",
                department.getId(),
                "Soft deleted department: " + department.getName()
        );

        return ResponseEntity.ok(ApiResponse.ok("Department deleted successfully and moved to Recycle Bin", null));
    }

    @Transactional
    public void permanentlyDeleteDepartment(Long id) {
        Department department = departmentRepository.findById(id).orElse(null);
        if (department == null) {
            // Already deleted or not found
            return;
        }

        String deptName = department.getName();
        System.out.println("================ DEPARTMENT PERMANENT DELETE ================");
        System.out.println("Department ID: " + id);
        System.out.println("Department Name: " + deptName);
        System.out.println("Schema dependency discovery and cleanup started.");

        // Step 2: Nullable references (SET NULL)
        String[] nullableTables = {
            "activities:department_id",
            "activity_subgroups:assigned_department_id",
            "activity_temporary_assignments:department_id",
            "badge_requests:department_id",
            "subjects:dept_id",
            "teams:department_id",
            "users:department_id"
        };
        for (String ref : nullableTables) {
            String[] parts = ref.split(":");
            String table = parts[0];
            String column = parts[1];
            System.out.println("Setting NULL for: " + table + "." + column);
            entityManager.createNativeQuery("UPDATE " + table + " SET " + column + " = NULL WHERE " + column + " = :id")
                .setParameter("id", id)
                .executeUpdate();
        }

        // Step 3: Non-Nullable references (DELETE)
        // Order matters if they have foreign keys pointing to each other.
        // e.g. student_group depends on student, etc.
        // But normally if we delete these, we rely on the caller acknowledging it's a deep clean.
        String[] nonNullableTables = {
            "activity_assignments:department_id",
            "attendance_sessions:department_id",
            "timetable:department_id",
            "students_group:dept_id",
            "students:department_id",
            "faculty:dept_id",
            "section:dept_id"
        };
        for (String ref : nonNullableTables) {
            String[] parts = ref.split(":");
            String table = parts[0];
            String column = parts[1];
            System.out.println("Deleting from: " + table + " where " + column + " matches");
            entityManager.createNativeQuery("DELETE FROM " + table + " WHERE " + column + " = :id")
                .setParameter("id", id)
                .executeUpdate();
        }

        // Step 4: Physically delete the department
        System.out.println("Physical delete for Department");
        entityManager.createNativeQuery("DELETE FROM departments WHERE id = :id")
            .setParameter("id", id)
            .executeUpdate();
            
        System.out.println("Department delete: SUCCESS");

        // Step 5: Create PERMANENT_DELETE audit log
        auditService.log(
                jjcet.PragatiX.enums.AuditAction.PERMANENT_DELETE,
                jjcet.PragatiX.enums.AuditModule.DEPARTMENT,
                "DEPARTMENT",
                id,
                "Permanently deleted department " + deptName + " from Recycle Bin"
        );
        System.out.println("Audit: SUCCESS");
        System.out.println("Transaction: COMMITTED");
    }
}
