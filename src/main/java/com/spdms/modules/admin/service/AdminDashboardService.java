package com.spdms.modules.admin.service;

import com.spdms.common.response.ApiResponse;
import com.spdms.repository.DepartmentRepository;
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

    public AdminDashboardService(DepartmentRepository departmentRepository, StudentRepository studentRepository, UserRepository userRepository, com.spdms.repository.DisciplineLogRepository disciplineLogRepository) {
        this.departmentRepository = departmentRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.disciplineLogRepository = disciplineLogRepository;
    }

    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        long totalStudents = studentRepository.count();
        long totalUsers = userRepository.count();
        long totalDepartments = departmentRepository.count();
        long totalAlerts = disciplineLogRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", totalStudents);
        stats.put("totalUsers", totalUsers);
        stats.put("totalDepartments", totalDepartments);
        stats.put("totalAlerts", totalAlerts);

        return ResponseEntity.ok(ApiResponse.ok("Stats loaded", stats));
    }

}
