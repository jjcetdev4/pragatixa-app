package com.spdms.modules.admin.service;

import com.spdms.common.response.ApiResponse;
import com.spdms.entity.Activity;
import com.spdms.modules.activity.repository.ActivityRepository;
import com.spdms.modules.activity.repository.ActivitySubgroupRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ActivityQueryService {

    private final ActivityRepository activityRepository;
    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final AdminAssignmentService adminAssignmentService;

    public ActivityQueryService(ActivityRepository activityRepository, ActivitySubgroupRepository activitySubgroupRepository, AdminAssignmentService adminAssignmentService) {
        this.activityRepository = activityRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.adminAssignmentService = adminAssignmentService;
    }

    public ResponseEntity<ApiResponse<List<Activity>>> getActivitiesBySubgroup(Long subgroupId) {
        if (!activitySubgroupRepository.existsById(subgroupId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<List<Activity>>error("Subgroup not found"));
        }

        List<Activity> activities = activityRepository.findBySubgroupId(subgroupId);

        for (Activity activity : activities) {
            adminAssignmentService.populateActivityTransientFields(activity);
        }
        return ResponseEntity.ok(ApiResponse.ok(activities));
    }

    public ResponseEntity<ApiResponse<List<Activity>>> getAllActivities() {
        List<Activity> activities = activityRepository.findAll();
        for (Activity activity : activities) {
            adminAssignmentService.populateActivityTransientFields(activity);
        }
        return ResponseEntity.ok(ApiResponse.ok(activities));
    }

    public ResponseEntity<ApiResponse<List<Activity>>> getActivitiesByStage(Long stageId) {
        List<Activity> activities = activityRepository.findByStageId(stageId);
        
        for (Activity activity : activities) {
            adminAssignmentService.populateActivityTransientFields(activity);
        }
        
        return ResponseEntity.ok(ApiResponse.ok(activities));
    }
}
