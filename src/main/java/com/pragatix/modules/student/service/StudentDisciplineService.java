package com.pragatix.modules.student.service;

import com.pragatix.dto.*;
import com.pragatix.modules.activity.dto.request.*;
import com.pragatix.modules.activity.dto.response.*;
import com.pragatix.modules.student.dto.request.*;
import com.pragatix.modules.student.dto.response.*;
import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.*;
import com.pragatix.repository.*;
import com.pragatix.modules.activity.repository.*;
import com.pragatix.modules.faculty.repository.*;
import com.pragatix.modules.student.repository.*;
import com.pragatix.modules.authentication.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.ss.usermodel.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class StudentDisciplineService {
    private static final Logger log = LoggerFactory.getLogger(StudentDisciplineService.class);

    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final DisciplineLogRepository disciplineLogRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final StudentMapper studentMapper;
    private final XpEngineService xpEngineService;

    public StudentDisciplineService(ActivitySubgroupRepository activitySubgroupRepository,
            DisciplineLogRepository disciplineLogRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            StudentMapper studentMapper,
            XpEngineService xpEngineService) {
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.disciplineLogRepository = disciplineLogRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.studentMapper = studentMapper;
        this.xpEngineService = xpEngineService;
    }

    @Transactional
    public ApiResponse<StudentResponse> adjustPoints(Long regNo, PointAdjustmentRequest request, String username) {
        User creator = userRepository.findByUsername(username).orElse(null);
        if (creator == null) {
            return ApiResponse.error("Unauthorized");
        }

        Student student = studentRepository.findById(regNo).orElse(null);
        if (student == null) {
            return ApiResponse.error("Student not found");
        }

        ActivitySubgroup subgroup = null;
        if (request.getSubgroupId() != null) {
            subgroup = activitySubgroupRepository.findById(request.getSubgroupId()).orElse(null);
            if (subgroup == null) {
                return ApiResponse.error("Activity subgroup not found");
            }

            // Verify assignment:
            if (subgroup.getAssignedFaculty() != null) {
                // If it is assigned to a specific faculty, verify that the logged-in user
                // matches the assignee
                if (!subgroup.getAssignedFaculty().getId().equals(creator.getId())) {
                    boolean isAdmin = creator.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
                    if (!isAdmin) {
                        return ApiResponse.error("Access Denied: Only the assigned faculty ("
                                + subgroup.getAssignedFaculty().getFullName()
                                + ") can award points for this activity.");
                    }
                }
            }
        }

        // Adjust point score using centralized XP Engine
        Student saved = xpEngineService.awardXp(student, null, creator, null, request.getPoints(), request.getReason());

        // Record log
        DisciplineLog logEntry = DisciplineLog.builder()
                .student(saved)
                .points(request.getPoints())
                .reason(request.getReason())
                .subgroup(subgroup)
                .recordedBy(creator)
                .incidentDate(LocalDateTime.now())
                .build();
        disciplineLogRepository.save(logEntry);

        log.debug("Teacher {} adjusted student {} points by {}. Reason: {}", creator.getUsername(), saved.getRegNo(),
                request.getPoints(), request.getReason());
        return ApiResponse.ok("Points updated successfully", studentMapper.toResponse(saved));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<DisciplineLog>> getDisciplineLogs(Long regNo) {
        if (!studentRepository.existsById(regNo)) {
            return ApiResponse.error("Student not found");
        }
        List<DisciplineLog> logs = disciplineLogRepository.findByStudentIdOrderByCreatedAtDesc(regNo);
        return ApiResponse.ok("Discipline logs loaded", logs);
    }

    @Transactional(readOnly = true)
    public ApiResponse<DepartmentPerformanceResponse> getDepartmentPerformance(String username) {
        User creator = userRepository.findByUsername(username).orElse(null);
        if (creator == null) {
            return ApiResponse.error("Unauthorized");
        }

        boolean isHodOrAdmin = creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"))
                || creator.getSubRoles().stream().map(SubRole::getName)
                        .anyMatch(sr -> sr.trim().equalsIgnoreCase("HOD"));

        if (!isHodOrAdmin) {
            return ApiResponse.error("Access Denied: Only Head of Department (HOD) can see department performance.");
        }

        Department department = creator.getDepartment();
        if (department == null) {
            return ApiResponse.error("No department assigned to this user.");
        }

        List<Student> students = studentRepository.findByDepartmentId(department.getId());
        long totalStudents = students.size();
        double overallAverage = students.stream()
                .mapToDouble(Student::getScore)
                .average()
                .orElse(100.0);

        Map<String, Double> yearWiseAverage = students.stream()
                .filter(s -> s.getAcademicYear() != null && !s.getAcademicYear().trim().isEmpty())
                .collect(Collectors.groupingBy(
                        Student::getAcademicYear,
                        TreeMap::new,
                        Collectors.averagingDouble(Student::getScore)));

        DepartmentPerformanceResponse response = new DepartmentPerformanceResponse(
                department.getName(),
                overallAverage,
                totalStudents,
                yearWiseAverage);

        return ApiResponse.ok("Department performance metrics loaded", response);
    }

}
