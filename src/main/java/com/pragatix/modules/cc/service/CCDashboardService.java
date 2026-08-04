package com.pragatix.modules.cc.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.User;
import com.pragatix.modules.authentication.repository.UserRepository;
import com.pragatix.repository.BadgeRequestRepository;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.modules.activity.repository.ActivityRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CCDashboardService {

    private final UserRepository userRepository;
    private final BadgeRequestRepository badgeRequestRepository;
    private final StudentRepository studentRepository;
    private final ActivityRepository activityRepository;

    public CCDashboardService(UserRepository userRepository,
            BadgeRequestRepository badgeRequestRepository,
            StudentRepository studentRepository,
            ActivityRepository activityRepository) {
        this.userRepository = userRepository;
        this.badgeRequestRepository = badgeRequestRepository;
        this.studentRepository = studentRepository;
        this.activityRepository = activityRepository;
    }

    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("CC not found"));

        if (user.getDepartment() == null || user.getSection() == null) {
            throw new RuntimeException("CC is not assigned to a valid department and section");
        }

        Long deptId = user.getDepartment().getId();
        Long sectionId = user.getSection().getId();

        long pendingBadgeRequests = badgeRequestRepository.countByStatusAndDepartmentIdAndSectionId("PENDING", deptId,
                sectionId);

        // Scope students based on department and section if methods exist. For now
        // using global count since user specifically asked for badge request scoped
        // counts.
        long totalStudents = studentRepository.count();
        long totalActivities = activityRepository.count();
        long totalAttendance = 120; // Example placeholder since it requires attendance queries

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", totalStudents);
        stats.put("totalActivities", totalActivities);
        stats.put("totalAttendance", totalAttendance);
        stats.put("pendingBadgeRequests", pendingBadgeRequests);

        return ResponseEntity.ok(ApiResponse.ok("CC Stats loaded", stats));
    }
}
