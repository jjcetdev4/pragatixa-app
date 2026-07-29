package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.User;
import com.pragatix.modules.authentication.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AdminClassCoordinatorQueryService {

    private final UserRepository userRepository;

    public AdminClassCoordinatorQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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
}
