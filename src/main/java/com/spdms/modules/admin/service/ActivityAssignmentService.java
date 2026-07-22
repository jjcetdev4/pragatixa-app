package com.spdms.modules.admin.service;

import com.spdms.common.response.ApiResponse;
import com.spdms.entity.Activity;
import com.spdms.entity.ActivityAssignment;
import com.spdms.entity.AssignmentScope;
import com.spdms.entity.Department;
import com.spdms.entity.Section;
import com.spdms.entity.User;
import com.spdms.modules.activity.repository.ActivityRepository;
import com.spdms.repository.ActivityAssignmentRepository;
import com.spdms.repository.DepartmentRepository;
import com.spdms.repository.SectionRepository;
import com.spdms.modules.authentication.repository.UserRepository;
import com.spdms.modules.activity.dto.request.AssignmentRequest;
import com.spdms.modules.activity.dto.response.ActivityAssignmentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spdms.modules.student.repository.StudentActivityXpRepository;

@Service
public class ActivityAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(ActivityAssignmentService.class);

    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final ActivityRepository activityRepository;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final StudentActivityXpRepository studentActivityXpRepository;

    public ActivityAssignmentService(ActivityAssignmentRepository activityAssignmentRepository, 
                                     ActivityRepository activityRepository, 
                                     DepartmentRepository departmentRepository, 
                                     SectionRepository sectionRepository, 
                                     UserRepository userRepository,
                                     StudentActivityXpRepository studentActivityXpRepository) {
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.activityRepository = activityRepository;
        this.departmentRepository = departmentRepository;
        this.sectionRepository = sectionRepository;
        this.userRepository = userRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> assignActivity(Long id, Map<String, Object> body) {
        Activity activity = activityRepository.findById(id).orElse(null);
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Activity not found"));
        }

        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);

        // Delete all existing assignments and their dependent XP records
        studentActivityXpRepository.deleteByActivityId(id);
        activityAssignmentRepository.deleteByActivityId(id);

        boolean ccEnabled = Boolean.TRUE.equals(body.get("ccEnabled"));
        boolean globalEnabled = Boolean.TRUE.equals(body.get("globalEnabled"));

        if (ccEnabled) {
            activity.setAssignmentMode("CLASS_COORDINATOR");
            activityRepository.save(activity);

            List<Department> allDepts = departmentRepository.findAll();
            List<String> warnings = new ArrayList<>();
            List<ActivityAssignment> assignmentsToSave = new ArrayList<>();

            java.util.List<User> allCCs = userRepository.findAllClassCoordinators();
            Map<String, User> ccMap = new java.util.HashMap<>();
            for (User u : allCCs) {
                if (u.getDepartment() != null && u.getSection() != null) {
                    ccMap.put(u.getDepartment().getId() + "_" + u.getSection().getId(), u);
                }
            }

            List<Section> allSections = sectionRepository.findAll();
            Map<Long, List<Section>> sectionsByDept = allSections.stream()
                .filter(s -> s.getDepartment() != null)
                .collect(java.util.stream.Collectors.groupingBy(s -> s.getDepartment().getId()));

            for (Department dept : allDepts) {
                List<Section> sections = sectionsByDept.getOrDefault(dept.getId(), new ArrayList<>());
                for (Section sec : sections) {
                    User cc = ccMap.get(dept.getId() + "_" + sec.getId());
                    if (cc == null) {
                        warnings.add("Section " + sec.getSectionName() + " (" + dept.getName() + "): No Class Coordinator assigned");
                        continue;
                    }
                    ActivityAssignment aa = new ActivityAssignment();
                    aa.setActivity(activity);
                    aa.setAssignmentScope(AssignmentScope.SECTION);
                    aa.setDepartment(dept);
                    aa.setSection(sec);
                    aa.setTeacher(cc);
                    aa.setAssignedBy(currentUser);
                    aa.setAssignedAt(LocalDateTime.now());
                    aa.setYear("1");
                    assignmentsToSave.add(aa);
                }
            }
            if (!assignmentsToSave.isEmpty()) {
                activityAssignmentRepository.saveAll(assignmentsToSave);
            }
            if (!warnings.isEmpty()) {
                log.warn("CC Assignment completed with warnings: {}", warnings);
                return ResponseEntity.ok(ApiResponse.ok("Assigned to sections with class coordinators. Warnings: " + String.join(", ", warnings), null));
            }
            return ResponseEntity.ok(ApiResponse.ok("Class Coordinator assignments saved successfully", null));

        } else if (globalEnabled) {
            activity.setAssignmentMode("GLOBAL");
            activityRepository.save(activity);

            List<Department> allDepts = departmentRepository.findAll();
            List<ActivityAssignment> assignmentsToSave = new ArrayList<>();
            for (Department dept : allDepts) {
                ActivityAssignment aa = new ActivityAssignment();
                aa.setActivity(activity);
                aa.setAssignmentScope(AssignmentScope.GLOBAL);
                aa.setDepartment(dept);
                aa.setAssignedBy(currentUser);
                aa.setAssignedAt(LocalDateTime.now());
                aa.setYear("1");
                assignmentsToSave.add(aa);
            }
            if (!assignmentsToSave.isEmpty()) {
                activityAssignmentRepository.saveAll(assignmentsToSave);
            }
            return ResponseEntity.ok(ApiResponse.ok("Activity successfully assigned globally", null));

        } else {
            // MANUAL ASSIGNMENT MODE
            activity.setAssignmentMode("MANUAL");
            activityRepository.save(activity);
            
            List<Map<String, Object>> assignmentsList = (List<Map<String, Object>>) body.get("assignments");
            List<ActivityAssignment> assignmentsToSave = new ArrayList<>();
            if (assignmentsList != null) {
                for (Map<String, Object> item : assignmentsList) {
                    ActivityAssignment aa = new ActivityAssignment();
                    aa.setActivity(activity);
                    
                    String scopeStr = (String) item.get("scope");
                    AssignmentScope scope = AssignmentScope.valueOf(scopeStr);
                    aa.setAssignmentScope(scope);
                    aa.setAssignedBy(currentUser);
                    aa.setAssignedAt(LocalDateTime.now());

                    if (scope == AssignmentScope.DEPARTMENT) {
                        Long deptId = ((Number) item.get("departmentId")).longValue();
                        Department dept = departmentRepository.findById(deptId).orElse(null);
                        aa.setDepartment(dept);
                    } else if (scope == AssignmentScope.SECTION) {
                        Long secId = ((Number) item.get("sectionId")).longValue();
                        Section sec = sectionRepository.findById(secId).orElse(null);
                        aa.setSection(sec);
                        if (sec != null) aa.setDepartment(sec.getDepartment());
                    }

                    assignmentsToSave.add(aa);
                }
            }
            if (!assignmentsToSave.isEmpty()) {
                activityAssignmentRepository.saveAll(assignmentsToSave);
            }
            return ResponseEntity.ok(ApiResponse.ok("Activity assignments updated successfully", null));
        }
    }
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<ActivityAssignmentResponse>>> getAssignments(Long activityId) {
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<List<ActivityAssignmentResponse>>error("Activity not found"));
        }
        
        List<ActivityAssignment> assignments = activityAssignmentRepository.findByActivityId(activityId);
        List<ActivityAssignmentResponse> response = assignments.stream().map(a -> new ActivityAssignmentResponse(
            a.getId(),
            a.getActivity().getId(),
            a.getActivity().getName(),
            a.getDepartment() != null ? a.getDepartment().getId() : null,
            a.getDepartment() != null ? a.getDepartment().getName() : null,
            a.getSection() != null ? a.getSection().getId() : null,
            a.getSection() != null ? a.getSection().getSectionName() : null,
            a.getTeacher() != null ? a.getTeacher().getId() : null,
            a.getTeacher() != null ? a.getTeacher().getFullName() : null,
            a.getTeacher() != null ? a.getTeacher().getUsername() : null,
            a.getAssignedBy() != null ? a.getAssignedBy().getFullName() : "System",
            a.getAssignedAt(),
            a.getYear(),
            a.getAssignmentScope() != null ? a.getAssignmentScope().name() : null
        )).collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.ok("Assignments fetched successfully", response));
    }

    @Transactional
    public ResponseEntity<ApiResponse<ActivityAssignmentResponse>> addAssignment(Long activityId, AssignmentRequest request) {
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<ActivityAssignmentResponse>error("Activity not found"));
        }
        
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);

        // Check for existing assignment for the same department and section
        List<ActivityAssignment> existing = activityAssignmentRepository.findByActivityId(activityId);
        ActivityAssignment aa = existing.stream().filter(a -> {
            boolean deptMatch = (a.getDepartment() == null && request.getDepartmentId() == null) || 
                                (a.getDepartment() != null && a.getDepartment().getId().equals(request.getDepartmentId()));
            boolean secMatch = (a.getSection() == null && request.getSectionId() == null) || 
                               (a.getSection() != null && a.getSection().getId().equals(request.getSectionId()));
            return deptMatch && secMatch;
        }).findFirst().orElse(new ActivityAssignment());

        aa.setActivity(activity);
        aa.setAssignmentScope(request.getScope());
        aa.setYear(request.getYear() != null ? request.getYear() : "1");
        aa.setAssignedBy(currentUser);
        aa.setAssignedAt(LocalDateTime.now());

        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getDepartmentId()).orElse(null);
            aa.setDepartment(dept);
        } else {
            aa.setDepartment(null);
        }
        
        if (request.getSectionId() != null) {
            Section sec = sectionRepository.findById(request.getSectionId()).orElse(null);
            aa.setSection(sec);
        } else {
            aa.setSection(null);
        }
        
        if (request.getTeacherId() != null) {
            User teacher = userRepository.findById(request.getTeacherId()).orElse(null);
            aa.setTeacher(teacher);
        } else {
            aa.setTeacher(null);
        }

        activityAssignmentRepository.save(aa);

        ActivityAssignmentResponse resp = new ActivityAssignmentResponse(
            aa.getId(),
            aa.getActivity().getId(),
            aa.getActivity().getName(),
            aa.getDepartment() != null ? aa.getDepartment().getId() : null,
            aa.getDepartment() != null ? aa.getDepartment().getName() : null,
            aa.getSection() != null ? aa.getSection().getId() : null,
            aa.getSection() != null ? aa.getSection().getSectionName() : null,
            aa.getTeacher() != null ? aa.getTeacher().getId() : null,
            aa.getTeacher() != null ? aa.getTeacher().getFullName() : null,
            aa.getTeacher() != null ? aa.getTeacher().getUsername() : null,
            aa.getAssignedBy() != null ? aa.getAssignedBy().getFullName() : "System",
            aa.getAssignedAt(),
            aa.getYear(),
            aa.getAssignmentScope() != null ? aa.getAssignmentScope().name() : null
        );

        return ResponseEntity.ok(ApiResponse.ok("Assignment added successfully", resp));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> removeAssignment(Long assignmentId) {
        if (!activityAssignmentRepository.existsById(assignmentId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Assignment not found"));
        }
        studentActivityXpRepository.deleteByAssignmentId(assignmentId);
        activityAssignmentRepository.deleteById(assignmentId);
        return ResponseEntity.ok(ApiResponse.ok("Assignment removed successfully", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> clearAssignments(Long activityId) {
        if (!activityRepository.existsById(activityId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Activity not found"));
        }
        studentActivityXpRepository.deleteByActivityId(activityId);
        activityAssignmentRepository.deleteByActivityId(activityId);
        
        // Reset assignment mode since all assignments are cleared
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity != null) {
            activity.setAssignmentMode(null);
            activityRepository.save(activity);
        }
        
        return ResponseEntity.ok(ApiResponse.ok("All faculty assignments removed successfully", null));
    }
}
