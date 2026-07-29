package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.repository.DepartmentRepository;
import com.pragatix.repository.BadgeRequestRepository;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.modules.authentication.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.pragatix.entity.User;
import com.pragatix.modules.authentication.security.AuthUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import com.pragatix.modules.admin.service.*;
import com.pragatix.modules.admin.mapper.*;

@Service
public class AdminDashboardService {
    private static final Logger log = LoggerFactory.getLogger(AdminDashboardService.class);

    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final com.pragatix.repository.DisciplineLogRepository disciplineLogRepository;
    private final BadgeRequestRepository badgeRequestRepository;
    private final AuthUtils authUtils;

    public AdminDashboardService(DepartmentRepository departmentRepository, StudentRepository studentRepository,
            UserRepository userRepository, com.pragatix.repository.DisciplineLogRepository disciplineLogRepository,
            BadgeRequestRepository badgeRequestRepository, AuthUtils authUtils) {
        this.departmentRepository = departmentRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.disciplineLogRepository = disciplineLogRepository;
        this.badgeRequestRepository = badgeRequestRepository;
        this.authUtils = authUtils;
    }

    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        User currentUser = authUtils.getCurrentUser();

        long totalStudents;
        if (currentUser != null && !authUtils.isSuperAdmin(currentUser) && authUtils.isAdmin(currentUser)) {
            String adminYear = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
            log.info("\nCurrent User Role:\nADMIN\n\nAcademic Year:\n{}\n", currentUser.getAcademicYear());
            if (adminYear != null) {
                totalStudents = studentRepository.countByYear(adminYear);
                log.info("\nStudent Count:\n{}\n", totalStudents);
            } else {
                totalStudents = 0; // Admin with no year assigned sees 0 students
                log.info("\nStudent Count:\n0 (No Year Assigned)\n");
            }
        } else {
            totalStudents = studentRepository.count();
            log.info("\nCurrent User Role:\nSUPER_ADMIN\n\nStudent Count Query:\nALL STUDENTS\n\nDatabase Result:\n{}\n\nReturned to Flutter:\n{}\n", totalStudents, totalStudents);
        }

        long teachersCount = userRepository.countActiveGenuineTeachers();
        long totalDepartments = departmentRepository.count();
        long totalAlerts = disciplineLogRepository.count();
        long pendingBadgeRequests = badgeRequestRepository.countByStatus("PENDING");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", totalStudents);
        stats.put("teachersCount", teachersCount);
        stats.put("totalDepartments", totalDepartments);
        stats.put("totalAlerts", totalAlerts);
        stats.put("pendingBadgeRequests", pendingBadgeRequests);

        return ResponseEntity.ok(ApiResponse.ok("Stats loaded", stats));
    }

}
