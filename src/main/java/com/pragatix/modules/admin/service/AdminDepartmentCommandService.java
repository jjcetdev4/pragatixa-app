package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.admin.dto.request.CreateDepartmentRequest;
import com.pragatix.entity.Department;
import com.pragatix.entity.Section;
import com.pragatix.repository.DepartmentRepository;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.modules.authentication.repository.UserRepository;
import com.pragatix.modules.activity.repository.ActivitySubgroupRepository;
import com.pragatix.repository.SubjectRepository;
import com.pragatix.repository.SectionRepository;
import com.pragatix.modules.faculty.repository.FacultyRepository;
import com.pragatix.modules.student.repository.StudentGroupRepository;
import com.pragatix.modules.authentication.security.AuthUtils;
import com.pragatix.repository.YearRepository;
import com.pragatix.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDepartmentCommandService {

    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final DepartmentRepository departmentRepository;
    private final FacultyRepository facultyRepository;
    private final SectionRepository sectionRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final AuthUtils authUtils;
    private final YearRepository yearRepository;

    public AdminDepartmentCommandService(ActivitySubgroupRepository activitySubgroupRepository,
            DepartmentRepository departmentRepository, FacultyRepository facultyRepository,
            SectionRepository sectionRepository, StudentGroupRepository studentGroupRepository,
            StudentRepository studentRepository, SubjectRepository subjectRepository, UserRepository userRepository,
            AuthUtils authUtils, YearRepository yearRepository) {
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.departmentRepository = departmentRepository;
        this.facultyRepository = facultyRepository;
        this.sectionRepository = sectionRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.authUtils = authUtils;
        this.yearRepository = yearRepository;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllDepartments() {
        List<Department> depts = departmentRepository.findAll();
        
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
            response.add(map);
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Department>> createDepartment(CreateDepartmentRequest request) {
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

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Department created successfully", saved));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Department>> updateDepartment(Long id, CreateDepartmentRequest request) {
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
            List<Section> sectionsToSave = new ArrayList<>();
            for (String sec : request.getSections()) {
                Section section = new Section();
                section.setDepartment(saved);
                section.setSectionName(sec);
                sectionsToSave.add(section);
            }
            List<Section> savedSections = sectionRepository.saveAll(sectionsToSave);
            saved.setSections(savedSections);
        }

        return ResponseEntity.ok(ApiResponse.ok("Department updated successfully", saved));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(Long id) {
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
        return ResponseEntity.ok(ApiResponse.ok("Department deleted successfully", null));
    }
}
