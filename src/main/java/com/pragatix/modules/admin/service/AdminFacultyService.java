package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.User;
import com.pragatix.entity.ActivitySubgroup;
import com.pragatix.modules.authentication.repository.UserRepository;
import com.pragatix.modules.activity.repository.ActivitySubgroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import org.springframework.stereotype.Service;
import com.pragatix.modules.admin.service.*;
import com.pragatix.modules.admin.mapper.*;

@Service
public class AdminFacultyService {
    private static final Logger log = LoggerFactory.getLogger(AdminFacultyService.class);

    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final UserRepository userRepository;

    public AdminFacultyService(ActivitySubgroupRepository activitySubgroupRepository, UserRepository userRepository) {
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.userRepository = userRepository;
    }

    @Transactional
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
        log.debug("Admin assigned faculty {} to subgroup {}", faculty != null ? faculty.getUsername() : "null",
                subgroup.getName());
        return ResponseEntity.ok(ApiResponse.ok("Faculty assigned successfully", saved));
    }

}
