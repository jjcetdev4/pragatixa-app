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
    private final com.spdms.modules.authentication.repository.UserRepository userRepository;
    private final com.spdms.modules.activity.repository.StageActivityMappingRepository stageActivityMappingRepository;

    public ActivityQueryService(ActivityRepository activityRepository, ActivitySubgroupRepository activitySubgroupRepository, AdminAssignmentService adminAssignmentService, com.spdms.modules.authentication.repository.UserRepository userRepository, com.spdms.modules.activity.repository.StageActivityMappingRepository stageActivityMappingRepository) {
        this.activityRepository = activityRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.adminAssignmentService = adminAssignmentService;
        this.userRepository = userRepository;
        this.stageActivityMappingRepository = stageActivityMappingRepository;
    }

    private com.spdms.entity.User getCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    private com.spdms.entity.AssignedAcademicYear resolveAcademicYear(com.spdms.entity.AssignedAcademicYear requestedYear, com.spdms.entity.User user) {
        if (user != null) {
            boolean isSuperAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_SUPER_ADMIN"));
            boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
            
            if (isSuperAdmin) {
                return requestedYear;
            } else if (isAdmin) {
                if (user.getAssignedAcademicYear() == null) {
                    throw new IllegalStateException("Year Admin does not have an assigned academic year.");
                }
                return user.getAssignedAcademicYear();
            }
        }
        
        if (requestedYear != null) return requestedYear;
        throw new SecurityException("User role requires an academic year to fetch activities.");
    }

    public ResponseEntity<ApiResponse<List<Activity>>> getActivitiesBySubgroup(Long subgroupId, com.spdms.entity.AssignedAcademicYear academicYear) {
        if (!activitySubgroupRepository.existsById(subgroupId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<List<Activity>>error("Subgroup not found"));
        }

        com.spdms.entity.User u = getCurrentUser();
        com.spdms.entity.AssignedAcademicYear effectiveYear = resolveAcademicYear(academicYear, u);

        List<Activity> activities;
        if (effectiveYear == null) {
            activities = activityRepository.findBySubgroupId(subgroupId);
        } else {
            activities = activityRepository.findBySubgroupIdAndStageAssignedAcademicYear(subgroupId, effectiveYear);
        }

        for (Activity activity : activities) {
            adminAssignmentService.populateActivityTransientFields(activity);
        }
        return ResponseEntity.ok(ApiResponse.ok(activities));
    }

    public ResponseEntity<ApiResponse<List<Activity>>> getAllActivities(String subgroup, com.spdms.entity.AssignedAcademicYear academicYear) {
        com.spdms.entity.User u = getCurrentUser();
        com.spdms.entity.AssignedAcademicYear effectiveYear = resolveAcademicYear(academicYear, u);

        List<Activity> activities = activityRepository.findAll();
        // Since Global Activities are templates, they do not inherently belong to an academic year until mapped.
        // Thus, we return all ACTIVE activities.
        activities = activities.stream().filter(a -> a.getStatus() != null && a.getStatus().equalsIgnoreCase("ACTIVE")).toList();

        if (u != null) {
            activities = adminAssignmentService.filterActivitiesForUser(activities, u);
        }
        
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

    public ResponseEntity<ApiResponse<List<GroupedActivityResponse>>> getGroupedActivities(Long stageId, String subgroup, com.spdms.entity.AssignedAcademicYear academicYear) {
        com.spdms.entity.User u = getCurrentUser();
        com.spdms.entity.AssignedAcademicYear effectiveYear = resolveAcademicYear(academicYear, u);

        System.out.println("Incoming Filters -> Academic Year: " + effectiveYear + ", Stage Id: " + stageId + ", Subgroup: " + subgroup + ", Role: " + (u != null ? u.getRoles() : "none") + ", Current User: " + (u != null ? u.getUsername() : "none"));

        List<Activity> allDbActivities = activityRepository.findAll();
        System.out.println("Total Activities in DB -> " + allDbActivities.size());

        List<Activity> activities = allDbActivities;

        // Apply Status Filter
        activities = activities.stream().filter(a -> a.getStatus() != null && a.getStatus().equalsIgnoreCase("ACTIVE")).toList();
        System.out.println("Activities after Status Filter -> " + activities.size());
        
        System.out.println("Activities after Academic Year Filter -> " + activities.size());

        if (subgroup != null && !subgroup.trim().isEmpty()) {
            final String lowerSubgroup = subgroup.toLowerCase();
            activities = activities.stream()
                .filter(a -> a.getSubgroup() != null && a.getSubgroup().getName() != null && a.getSubgroup().getName().equalsIgnoreCase(lowerSubgroup))
                .toList();
        }
        System.out.println("Activities after Stage Filter / Subgroup Filter -> " + activities.size());

        // WE DO NOT CALL filterActivitiesForUser for the Existing Activities popup
        System.out.println("Activities after Assignment Filter -> " + activities.size());

        // Group by Subgroup Category (fallback to Name if Category is null)
        Map<String, Map<String, ActivityOptionDTO>> uniqueMap = new LinkedHashMap<>();
        
        // Find existing names in the current stage using the mapping table
        java.util.Set<String> existingNamesInStage = new java.util.HashSet<>();
        if (stageId != null) {
             List<com.spdms.entity.StageActivityMapping> stageMappings = stageActivityMappingRepository.findByStageId(stageId);
             for(com.spdms.entity.StageActivityMapping mapping : stageMappings) {
                  if (mapping.getActivity() != null && mapping.getActivity().getName() != null) {
                      existingNamesInStage.add(mapping.getActivity().getName().toLowerCase());
                  }
             }
        }

        for (Activity activity : activities) {
            String sName = (activity.getSubgroup() != null && activity.getSubgroup().getName() != null)
                ? activity.getSubgroup().getName() : "Uncategorized";
            
            boolean mapped = existingNamesInStage.contains(activity.getName().toLowerCase());

            ActivityOptionDTO dto = new ActivityOptionDTO(
                activity.getId(),
                activity.getName(),
                activity.getDescription(),
                activity.getAwardXp(),
                activity.getAwardFrequency(),
                activity.getType(),
                activity.getStage() != null && activity.getStage().getAssignedAcademicYear() != null ? activity.getStage().getAssignedAcademicYear().name() : null,
                activity.getStatus(),
                activity.getSubgroup() != null ? activity.getSubgroup().getCategory() : null,
                sName,
                activity.getType(),
                mapped,
                activity.getStage() != null ? activity.getStage().getId() : null
            );

            uniqueMap.computeIfAbsent(sName, k -> new LinkedHashMap<>());
            uniqueMap.get(sName).putIfAbsent(activity.getName().toLowerCase(), dto);
        }

        Map<String, List<ActivityOptionDTO>> groupedMap = new LinkedHashMap<>();
        int finalCount = 0;
        for (Map.Entry<String, Map<String, ActivityOptionDTO>> entry : uniqueMap.entrySet()) {
            groupedMap.put(entry.getKey(), new ArrayList<>(entry.getValue().values()));
            finalCount += entry.getValue().size();
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
        
        System.out.println("Final Returned Activities -> " + finalCount);

        return ResponseEntity.ok(ApiResponse.ok(responseList));
    }

    public ResponseEntity<ApiResponse<List<Activity>>> getActivitiesByStage(Long stageId, String subgroup, com.spdms.entity.AssignedAcademicYear academicYear) {
        com.spdms.entity.User u = getCurrentUser();
        com.spdms.entity.AssignedAcademicYear effectiveYear = resolveAcademicYear(academicYear, u);

        System.out.println("--- getActivitiesByStage ---");
        System.out.println("Stage ID: " + stageId);
        System.out.println("Academic Year: " + effectiveYear);
        System.out.println("Subgroup: " + subgroup);

        List<com.spdms.entity.StageActivityMapping> mappings;
        if (effectiveYear == null) {
            mappings = stageActivityMappingRepository.findByStageId(stageId);
        } else {
            // Because stage mapping doesn't join by academic year automatically in a repository method without a custom query,
            // we will fetch by stageId and filter by AcademicYear in Java, since we already have the stage.
            mappings = stageActivityMappingRepository.findByStageId(stageId);
            mappings = mappings.stream().filter(m -> 
                m.getStage() != null && 
                m.getStage().getAssignedAcademicYear() == effectiveYear
            ).toList();
        }
        
        System.out.println("Rows returned from DB: " + mappings.size());

        if (u != null) {
            // we will map to activities temporarily for filterActivitiesForUser,
            // but we must preserve the mapping subgroup.
            // Since filterActivitiesForUser only checks ownership and scope, we can filter the mappings list.
            List<Activity> temp = mappings.stream().map(m -> m.getActivity()).collect(Collectors.toList());
            temp = adminAssignmentService.filterActivitiesForUser(temp, u);
            java.util.Set<Long> allowedActivityIds = temp.stream().map(Activity::getId).collect(Collectors.toSet());
            mappings = mappings.stream().filter(m -> allowedActivityIds.contains(m.getActivity().getId())).collect(Collectors.toList());
        }
        
        System.out.println("Rows after user filtering: " + mappings.size());
        
        if (subgroup != null && !subgroup.trim().isEmpty()) {
            final String upperSubgroup = subgroup.trim().toUpperCase();
            mappings = mappings.stream()
                .filter(m -> m.getSubgroupType() != null && m.getSubgroupType().trim().toUpperCase().equals(upperSubgroup))
                .toList();
        }
        
        List<Activity> activities = mappings.stream().map(m -> m.getActivity()).collect(Collectors.toList());
        System.out.println("Rows after subgroup filtering: " + activities.size());
        
        for (Activity activity : activities) {
            adminAssignmentService.populateActivityTransientFields(activity);
        }
        
        System.out.println("Rows returned to Flutter: " + activities.size());
        
        return ResponseEntity.ok(ApiResponse.ok(activities));
    }
}
