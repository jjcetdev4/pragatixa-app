package com.spdms.modules.admin.service;

import com.spdms.common.response.ApiResponse;
import com.spdms.repository.DepartmentRepository;
import com.spdms.repository.BadgeRequestRepository;
import com.spdms.modules.student.repository.StudentRepository;
import com.spdms.modules.authentication.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import com.spdms.modules.admin.service.*;
import com.spdms.modules.admin.mapper.*;

@Service
public class AdminDashboardService {
    private static final Logger log = LoggerFactory.getLogger(AdminDashboardService.class);

    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final com.spdms.repository.DisciplineLogRepository disciplineLogRepository;
    private final BadgeRequestRepository badgeRequestRepository;

    public AdminDashboardService(DepartmentRepository departmentRepository, StudentRepository studentRepository, UserRepository userRepository, com.spdms.repository.DisciplineLogRepository disciplineLogRepository, BadgeRequestRepository badgeRequestRepository) {
        this.departmentRepository = departmentRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.disciplineLogRepository = disciplineLogRepository;
        this.badgeRequestRepository = badgeRequestRepository;
    }

    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        long totalStudents = studentRepository.count();
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
