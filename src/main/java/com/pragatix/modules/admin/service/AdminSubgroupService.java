package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.ActivityStage;
import com.pragatix.entity.ActivitySubgroup;
import com.pragatix.entity.Activity;
import com.pragatix.modules.activity.repository.ActivityStageRepository;
import com.pragatix.modules.activity.repository.ActivitySubgroupRepository;
import com.pragatix.modules.activity.repository.ActivityRepository;
import com.pragatix.repository.DisciplineLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import com.pragatix.modules.admin.service.*;
import com.pragatix.modules.admin.mapper.*;

@Service
public class AdminSubgroupService {
    private static final Logger log = LoggerFactory.getLogger(AdminSubgroupService.class);

    private final ActivityRepository activityRepository;
    private final ActivityStageRepository activityStageRepository;
    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final DisciplineLogRepository disciplineLogRepository;

    private final com.pragatix.modules.authentication.repository.UserRepository userRepository;

    public AdminSubgroupService(ActivityRepository activityRepository, ActivityStageRepository activityStageRepository,
            ActivitySubgroupRepository activitySubgroupRepository, DisciplineLogRepository disciplineLogRepository,
            com.pragatix.modules.authentication.repository.UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.activityStageRepository = activityStageRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.disciplineLogRepository = disciplineLogRepository;
        this.userRepository = userRepository;
    }

    private void validateAdminAcademicYearAccess(com.pragatix.enums.AcademicYear targetYear) {
        if (targetYear == null)
            return;
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        com.pragatix.entity.User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            boolean isSuperAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_SUPER_ADMIN".equals(r.getName()));
            boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
            if (isAdmin && !isSuperAdmin) {
                if (user.getAcademicYear() == null || !targetYear.equals(user.getAcademicYear())) {
                    throw new IllegalArgumentException(
                            "Admin account is not authorized for Academic Year: " + targetYear.name());
                }
            }
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<ActivitySubgroup>> createSubgroup(
            @PathVariable Long stageId,
            @RequestBody Map<String, Object> body) {

        ActivityStage stage = activityStageRepository.findById(stageId).orElse(null);
        if (stage == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Stage not found"));
        }

        try {
            validateAdminAcademicYearAccess(stage.getAcademicYear());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }

        String name = (String) body.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Subgroup name is required"));
        }

        int threshold = 0;
        if (body.get("threshold") != null) {
            threshold = Integer.parseInt(body.get("threshold").toString());
        }

        ActivitySubgroup subgroup = ActivitySubgroup.builder()
                .name(name.trim())
                .threshold(threshold)
                .stage(stage)
                .build();

        ActivitySubgroup saved = activitySubgroupRepository.save(subgroup);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Subgroup created successfully", saved));
    }

    @Transactional
    public ResponseEntity<ApiResponse<ActivitySubgroup>> updateSubgroup(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        ActivitySubgroup subgroup = activitySubgroupRepository.findById(id).orElse(null);
        if (subgroup == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Subgroup not found"));
        }

        if (subgroup.getStage() != null) {
            try {
                validateAdminAcademicYearAccess(subgroup.getStage().getAcademicYear());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
            }
        }

        if (body.get("name") != null) {
            subgroup.setName(body.get("name").toString().trim());
        }
        if (body.get("threshold") != null) {
            subgroup.setThreshold(Integer.parseInt(body.get("threshold").toString()));
        }

        ActivitySubgroup saved = activitySubgroupRepository.save(subgroup);
        return ResponseEntity.ok(ApiResponse.ok("Subgroup updated successfully", saved));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteSubgroup(@PathVariable Long id) {
        if (!activitySubgroupRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Subgroup not found"));
        }

        // 1. Nullify references in DisciplineLog
        disciplineLogRepository.nullifySubgroupReferences(id);

        List<Activity> activities = activityRepository.findBySubgroupId(id);
        for (Activity act : activities) {
            disciplineLogRepository.nullifyActivityReferences(act.getId());
        }

        // 2. Delete activities
        activityRepository.deleteAll(activities);

        // 3. Delete subgroup
        activitySubgroupRepository.deleteById(id);

        log.debug("Admin deleted subgroup with ID: {}", id);
        return ResponseEntity.ok(ApiResponse.ok("Subgroup deleted successfully", null));
    }

}
