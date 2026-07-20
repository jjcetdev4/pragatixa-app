package com.spdms.modules.admin.service;

import com.spdms.common.response.ApiResponse;
import com.spdms.entity.ActivityStage;
import com.spdms.entity.ActivitySubgroup;
import com.spdms.entity.Activity;
import com.spdms.modules.activity.repository.ActivityStageRepository;
import com.spdms.modules.activity.repository.ActivitySubgroupRepository;
import com.spdms.modules.activity.repository.ActivityRepository;
import com.spdms.repository.DisciplineLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import com.spdms.modules.admin.service.*;
import com.spdms.modules.admin.mapper.*;

@Service
public class AdminSubgroupService {
    private static final Logger log = LoggerFactory.getLogger(AdminSubgroupService.class);

    private final ActivityRepository activityRepository;
    private final ActivityStageRepository activityStageRepository;
    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final DisciplineLogRepository disciplineLogRepository;

    public AdminSubgroupService(ActivityRepository activityRepository, ActivityStageRepository activityStageRepository, ActivitySubgroupRepository activitySubgroupRepository, DisciplineLogRepository disciplineLogRepository) {
        this.activityRepository = activityRepository;
        this.activityStageRepository = activityStageRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.disciplineLogRepository = disciplineLogRepository;
    }

    @Transactional
    public ResponseEntity<ApiResponse<ActivitySubgroup>> createSubgroup(
            @PathVariable Long stageId,
            @RequestBody Map<String, Object> body) {

        ActivityStage stage = activityStageRepository.findById(stageId).orElse(null);
        if (stage == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Stage not found"));
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
