package com.spdms.modules.admin.service;

import com.spdms.common.response.ApiResponse;
import com.spdms.entity.Activity;
import com.spdms.modules.activity.repository.ActivityRepository;
import com.spdms.modules.activity.repository.ActivitySubgroupRepository;
import com.spdms.modules.activity.dto.response.GroupedActivityResponse;
import com.spdms.modules.activity.dto.response.ActivityOptionDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

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

    public ResponseEntity<ApiResponse<List<Activity>>> getAllActivities(String subgroup) {
        List<Activity> activities = activityRepository.findAll();
        
        if (subgroup != null && !subgroup.trim().isEmpty()) {
            final String lowerSubgroup = subgroup.toLowerCase();
            activities = activities.stream()
                .filter(a -> a.getSubgroup() != null && a.getSubgroup().getName() != null && a.getSubgroup().getName().equalsIgnoreCase(lowerSubgroup))
                .toList();
        }
        
        for (Activity activity : activities) {
            adminAssignmentService.populateActivityTransientFields(activity);
        }
        return ResponseEntity.ok(ApiResponse.ok(activities));
    }

    public ResponseEntity<ApiResponse<List<GroupedActivityResponse>>> getGroupedActivities(String subgroup) {
        List<Activity> activities = activityRepository.findAll();
        
        if (subgroup != null && !subgroup.trim().isEmpty()) {
            final String lowerSubgroup = subgroup.toLowerCase();
            activities = activities.stream()
                .filter(a -> a.getSubgroup() != null && a.getSubgroup().getName() != null && a.getSubgroup().getName().equalsIgnoreCase(lowerSubgroup))
                .toList();
        }

        // Group by Subgroup Category (fallback to Name if Category is null)
        Map<String, Map<String, ActivityOptionDTO>> uniqueMap = new LinkedHashMap<>();
        
        for (Activity activity : activities) {
            String sName = (activity.getSubgroup() != null && activity.getSubgroup().getName() != null)
                ? activity.getSubgroup().getName() : "Uncategorized";
            

            ActivityOptionDTO dto = new ActivityOptionDTO(
                activity.getId(),
                activity.getName(),
                activity.getDescription(),
                activity.getAwardXp(),
                activity.getAwardFrequency(),
                activity.getType()
            );

            uniqueMap.computeIfAbsent(sName, k -> new LinkedHashMap<>());
            uniqueMap.get(sName).putIfAbsent(activity.getName().toLowerCase(), dto);
        }

        Map<String, List<ActivityOptionDTO>> groupedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, ActivityOptionDTO>> entry : uniqueMap.entrySet()) {
            groupedMap.put(entry.getKey(), new ArrayList<>(entry.getValue().values()));
        }

        // Sort by priority (Must, Individual, Group, others)
        List<GroupedActivityResponse> responseList = new ArrayList<>();
        String[] priorities = {"Must", "Individual", "Group"};

        for (String p : priorities) {
            String matchingKey = null;
            for (String k : groupedMap.keySet()) {
                if (k.toLowerCase().contains(p.toLowerCase())) {
                    matchingKey = k;
                    break;
                }
            }
            if (matchingKey != null) {
                responseList.add(new GroupedActivityResponse(matchingKey, groupedMap.remove(matchingKey)));
            }
        }

        // Add remaining
        for (Map.Entry<String, List<ActivityOptionDTO>> entry : groupedMap.entrySet()) {
            responseList.add(new GroupedActivityResponse(entry.getKey(), entry.getValue()));
        }

        return ResponseEntity.ok(ApiResponse.ok(responseList));
    }

    public ResponseEntity<ApiResponse<List<Activity>>> getActivitiesByStage(Long stageId, String subgroup) {
        List<Activity> activities = activityRepository.findByStageId(stageId);
        
        if (subgroup != null && !subgroup.trim().isEmpty()) {
            final String lowerSubgroup = subgroup.toLowerCase();
            activities = activities.stream()
                .filter(a -> a.getSubgroup() != null && a.getSubgroup().getName().toLowerCase().equals(lowerSubgroup))
                .toList();
        }
        
        for (Activity activity : activities) {
            adminAssignmentService.populateActivityTransientFields(activity);
        }
        
        return ResponseEntity.ok(ApiResponse.ok(activities));
    }
}
