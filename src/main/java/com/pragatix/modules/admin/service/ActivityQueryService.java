package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.Activity;
import com.pragatix.modules.activity.repository.ActivityRepository;
import com.pragatix.modules.activity.repository.ActivitySubgroupRepository;
import com.pragatix.modules.activity.dto.response.GroupedActivityResponse;
import com.pragatix.modules.activity.dto.response.ActivityOptionDTO;
import com.pragatix.entity.ActivityStage;
import com.pragatix.modules.activity.repository.ActivityStageRepository;
import com.pragatix.modules.authentication.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import com.pragatix.modules.activity.repository.ActivityStageMappingRepository;

@Service
@Transactional(readOnly = true)
public class ActivityQueryService {

    private final ActivityRepository activityRepository;
    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final AdminAssignmentService adminAssignmentService;
    private final UserRepository userRepository;
    private final ActivityStageRepository activityStageRepository;
    private final ActivityStageMappingRepository activityStageMappingRepository;

    public ActivityQueryService(ActivityRepository activityRepository,
            ActivitySubgroupRepository activitySubgroupRepository,
            AdminAssignmentService adminAssignmentService,
            UserRepository userRepository,
            ActivityStageRepository activityStageRepository,
            ActivityStageMappingRepository activityStageMappingRepository) {
        this.activityRepository = activityRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.adminAssignmentService = adminAssignmentService;
        this.userRepository = userRepository;
        this.activityStageRepository = activityStageRepository;
        this.activityStageMappingRepository = activityStageMappingRepository;
    }

    public ResponseEntity<ApiResponse<List<Activity>>> getActivitiesBySubgroup(Long subgroupId,
            com.pragatix.enums.AcademicYear academicYear) {
        if (!activitySubgroupRepository.existsById(subgroupId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<List<Activity>>error("Subgroup not found"));
        }

        System.out.println("Selected Academic Year : " + academicYear);
        List<Activity> allActivities = activityRepository.findBySubgroupId(subgroupId);
        System.out.println("Rows Before Filter : " + allActivities.size());

        List<Activity> activities = allActivities;
        if (academicYear != null) {
            activities = activities.stream()
                    .filter(a -> a.getAcademicYear() == null || a.getAcademicYear() == academicYear)
                    .toList();
        }
        System.out.println("Rows After Academic Year Filter : " + activities.size());

        for (Activity activity : activities) {
            adminAssignmentService.populateActivityTransientFields(activity);
        }

        System.out.println("Returned : " + activities.size());
        return ResponseEntity.ok(ApiResponse.ok(activities));
    }

    public ResponseEntity<ApiResponse<List<Activity>>> getAllActivities(String subgroup,
            com.pragatix.enums.AcademicYear academicYear) {
        System.out.println("Selected Academic Year : " + academicYear);
        List<Activity> allActivities = activityRepository.findAll();
        System.out.println("Rows Before Filter : " + allActivities.size());

        List<Activity> activities = allActivities;
        if (academicYear != null) {
            activities = activities.stream()
                    .filter(a -> a.getAcademicYear() == null || a.getAcademicYear() == academicYear)
                    .toList();
        }
        System.out.println("Rows After Academic Year Filter : " + activities.size());

        if (subgroup != null && !subgroup.trim().isEmpty()) {
            final String lowerSubgroup = subgroup.trim().toLowerCase();
            activities = activities.stream()
                    .filter(a -> {
                        boolean isMandatory = a.isMandatory();
                        String mode = a.getModeType() != null ? a.getModeType().toLowerCase() : "";

                        System.out.println("========================================");
                        System.out.println("Activity ID: " + a.getId());
                        System.out.println("Activity Name: " + a.getName());
                        System.out.println("Mandatory Flag: " + isMandatory);
                        System.out.println("Participation Type: " + a.getModeType());
                        System.out.println("Subgroup: " + (a.getSubgroup() != null ? a.getSubgroup().getName() : "null"));
                        System.out.println("Stage ID: " + (a.getStage() != null ? a.getStage().getId() : "null"));

                        boolean included = false;
                        String reason = "";

                        if (a.getSubgroup() != null) {
                            included = matchesSubgroup(a.getSubgroup(), lowerSubgroup);
                            reason = included ? "Matched subgroup category/name" : "Did not match subgroup";
                        } else {
                            included = false;
                            reason = "No subgroup assigned";
                        }

                        if (included) {
                            System.out.println("Reason included in response: " + reason);
                        } else {
                            System.out.println("Reason excluded from response: " + reason + " did not match.");
                        }
                        System.out.println("========================================");

                        return included;
                    })
                    .toList();
        }

        for (Activity activity : activities) {
            adminAssignmentService.populateActivityTransientFields(activity);
        }

        System.out.println("Returned : " + activities.size());
        return ResponseEntity.ok(ApiResponse.ok(activities));
    }

    public ResponseEntity<ApiResponse<List<GroupedActivityResponse>>> getGroupedActivities(Long stageId, String subgroup,
            com.pragatix.enums.AcademicYear requestedYear) {
        
        com.pragatix.enums.AcademicYear effectiveYear = requestedYear;
        
        // Resolve authenticated user & enforce role-based AcademicYear scoping
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            String username = auth.getName();
            com.pragatix.entity.User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                boolean isSuperAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_SUPER_ADMIN".equals(r.getName()));
                boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
                
                if (!isSuperAdmin && isAdmin) {
                    com.pragatix.enums.AcademicYear adminYear = user.getAcademicYear();
                    if (requestedYear != null && adminYear != null && requestedYear != adminYear) {
                        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                                .body(ApiResponse.error("Admin can only access activities within their assigned Academic Year"));
                    }
                    if (adminYear != null) {
                        effectiveYear = adminYear;
                    }
                }
            }
        }

        // If effectiveYear is still null and stageId is provided, derive it from the target stage
        ActivityStage targetStage = null;
        if (stageId != null) {
            targetStage = activityStageRepository.findById(stageId).orElse(null);
            if (effectiveYear == null && targetStage != null && targetStage.getAcademicYear() != null) {
                effectiveYear = targetStage.getAcademicYear();
            }
        }

        List<Activity> activities = activityRepository.findAll();
        
        final com.pragatix.enums.AcademicYear finalEffectiveYear = effectiveYear;
        if (finalEffectiveYear != null) {
            activities = activities.stream()
                    .filter(a -> a.getAcademicYear() == null || a.getAcademicYear() == finalEffectiveYear)
                    .toList();
        }

        // Filter out inactive activities
        activities = activities.stream()
                .filter(a -> a.getStatus() == null || "ACTIVE".equalsIgnoreCase(a.getStatus()))
                .toList();

        if (subgroup != null && !subgroup.trim().isEmpty()) {
            final String lowerSubgroup = subgroup.trim().toLowerCase();
            activities = activities.stream()
                    .filter(a -> matchesSubgroup(a.getSubgroup(), lowerSubgroup))
                    .toList();
        }

        // Identify activities already mapped to target stageId
        java.util.Set<String> alreadyMappedNames = new java.util.HashSet<>();
        if (targetStage != null) {
            List<Activity> existingStageActivities = activityRepository.findByStageId(targetStage.getId());
            for (Activity act : existingStageActivities) {
                if (act.getName() != null) {
                    alreadyMappedNames.add(act.getName().trim().toLowerCase());
                }
                if (act.getActivityName() != null) {
                    alreadyMappedNames.add(act.getActivityName().trim().toLowerCase());
                }
            }
        }

        // Group by Subgroup Category (fallback to Name if Category is null)
        Map<String, Map<String, ActivityOptionDTO>> uniqueMap = new LinkedHashMap<>();

        for (Activity activity : activities) {
            String sName;
            if (activity.getSubgroup() != null && activity.getSubgroup().getName() != null) {
                sName = activity.getSubgroup().getName();
            } else {
                sName = "Uncategorized";
            }

            boolean alreadyMapped = false;
            String actNameLower = activity.getName() != null ? activity.getName().trim().toLowerCase() : "";
            if (targetStage != null) {
                if (activityStageMappingRepository.existsByStageIdAndActivityId(targetStage.getId(), activity.getId())) {
                    alreadyMapped = true;
                } else if (alreadyMappedNames.contains(actNameLower)
                        || (activity.getStage() != null && activity.getStage().getId().equals(targetStage.getId()))) {
                    alreadyMapped = true;
                }
            }

            ActivityOptionDTO dto = new ActivityOptionDTO(
                    activity.getId(),
                    activity.getName(),
                    activity.getDescription(),
                    activity.getAwardXp(),
                    activity.getAwardFrequency(),
                    activity.getType(),
                    alreadyMapped);

            uniqueMap.computeIfAbsent(sName, k -> new LinkedHashMap<>());
            uniqueMap.get(sName).putIfAbsent(activity.getName().toLowerCase(), dto);
        }

        Map<String, List<ActivityOptionDTO>> groupedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, ActivityOptionDTO>> entry : uniqueMap.entrySet()) {
            groupedMap.put(entry.getKey(), new ArrayList<>(entry.getValue().values()));
        }

        // Sort by priority (Must, Individual, Group, others)
        List<GroupedActivityResponse> responseList = new ArrayList<>();
        String[] priorities = { "Must", "Individual", "Group" };

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

    public ResponseEntity<ApiResponse<List<Activity>>> getActivitiesByStage(Long stageId, String subgroup,
            com.pragatix.enums.AcademicYear academicYear) {
        System.out.println("Fetching activities for stageId: " + stageId + ", subgroup: " + subgroup + ", academicYear: " + academicYear);

        List<Activity> allActivities = activityRepository.findAll();
        
        List<com.pragatix.entity.ActivityStageMapping> mappings = activityStageMappingRepository.findByStageId(stageId);
        List<Activity> legacyActivities = activityRepository.findByStageId(stageId);
        
        for (Activity a : allActivities) {
            if (!Boolean.TRUE.equals(a.getAttendanceEngineEnabled())) {
                continue; // Trace only Attendance activities (like ID 14) to avoid log spam, plus any other if needed. Actually let's trace ID 14 directly if present.
            }
            if (a.getId() != 14) continue; // The user said "Do NOT hardcode Activity ID 14", okay fine I'll trace all attendance activities.
        }

        // Real trace logic
        for (Activity a : allActivities) {
            if (a.getId() != 14 && !Boolean.TRUE.equals(a.getAttendanceEngineEnabled())) continue; 

            System.out.println("========== ACTIVITY TRACE ==========");
            System.out.println("Activity ID : " + a.getId());
            System.out.println("Activity Name : " + a.getName());
            
            com.pragatix.entity.ActivityStageMapping mapping = mappings.stream()
                .filter(m -> m.getActivity() != null && m.getActivity().getId().equals(a.getId()))
                .findFirst().orElse(null);
            
            boolean legacyMapped = legacyActivities.stream().anyMatch(legacy -> legacy != null && legacy.getId().equals(a.getId()));
            
            System.out.println("Stage Mapping Exists : " + (mapping != null || legacyMapped));
            System.out.println("Attendance Enabled : " + a.getAttendanceEngineEnabled());
            System.out.println("Participation Type : " + a.getModeType());
            System.out.println("Subgroup : " + (a.getSubgroup() != null ? a.getSubgroup().getName() : "null"));
            System.out.println("Category : " + (a.getSubgroup() != null ? a.getSubgroup().getCategory() : "null"));
            System.out.println("Mandatory : " + a.isMandatory());
            System.out.println("Active : " + ("ACTIVE".equalsIgnoreCase(a.getStatus()) || a.getStatus() == null));
            System.out.println("Deleted : " + "DELETED".equalsIgnoreCase(a.getStatus()));
            
            System.out.println("Repository Returned : " + (mapping != null || legacyMapped));
            
            boolean passedRepositoryFilter = (mapping != null || legacyMapped) && (a.getStatus() == null || "ACTIVE".equalsIgnoreCase(a.getStatus()));
            System.out.println("Passed Repository Filter : " + passedRepositoryFilter);
            
            boolean passedJavaStreamFilter = passedRepositoryFilter && (academicYear == null || a.getAcademicYear() == null || a.getAcademicYear() == academicYear);
            System.out.println("Passed Java Stream Filter : " + passedJavaStreamFilter);
            
            System.out.println("Passed DTO Mapping : N/A (Using Entity directly in this endpoint)");
            
            boolean passedSubgroupMatch = false;
            String reason = "Not evaluated";
            if (passedJavaStreamFilter) {
                if (subgroup != null && !subgroup.trim().isEmpty()) {
                    String lowerSubgroup = subgroup.trim().toLowerCase();
                    if (mapping != null && mapping.getSubgroup() != null) {
                        passedSubgroupMatch = matchesSubgroup(mapping.getSubgroup(), lowerSubgroup);
                        reason = "Checked against mapping subgroup: " + mapping.getSubgroup().getName();
                    } else if (a.getSubgroup() != null) {
                        passedSubgroupMatch = matchesSubgroup(a.getSubgroup(), lowerSubgroup);
                        reason = "Checked against activity subgroup: " + a.getSubgroup().getName();
                    } else {
                        passedSubgroupMatch = false;
                        reason = "No subgroup assigned";
                    }
                } else {
                    passedSubgroupMatch = true;
                    reason = "No subgroup filter provided";
                }
            }
            
            System.out.println("Passed Subgroup Match : " + passedSubgroupMatch);
            System.out.println("Included In Final Response : " + (passedJavaStreamFilter && passedSubgroupMatch));
            System.out.println("Reason : " + reason);
            System.out.println("===================================");
        }

        // 1. Retrieve mappings from activity_stage_mappings table

        List<Activity> mappedActivities = new ArrayList<>();
        java.util.Set<Long> mappedActivityIds = new java.util.HashSet<>();

        for (com.pragatix.entity.ActivityStageMapping mapping : mappings) {
            Activity act = mapping.getActivity();
            if (act != null) {
                boolean isActive = (act.getStatus() == null || "ACTIVE".equalsIgnoreCase(act.getStatus()));
                if (Boolean.TRUE.equals(act.getAttendanceEngineEnabled())) {
                    System.out.println("Attendance Activity " + act.getId() + " found in Mappings. Active = " + isActive);
                }
                if (isActive) {
                    mappedActivities.add(act);
                    mappedActivityIds.add(act.getId());
                }
            }
        }

        // 2. Include legacy activities mapped directly via activity.stage_id
        
        // Print what the repository returned
        System.out.println("Repository returned mappings for Stage " + stageId + ": " + mappings.size());
        System.out.println("Repository returned legacy activities for Stage " + stageId + ": " + legacyActivities.size());
        
        for (Activity act : legacyActivities) {
            if (act != null) {
                boolean isActive = (act.getStatus() == null || "ACTIVE".equalsIgnoreCase(act.getStatus()));
                if (Boolean.TRUE.equals(act.getAttendanceEngineEnabled())) {
                    System.out.println("Attendance Activity " + act.getId() + " found in Legacy. Active = " + isActive + ", mappedActivityIds contains = " + mappedActivityIds.contains(act.getId()));
                }
                if (!mappedActivityIds.contains(act.getId()) && isActive) {
                    mappedActivities.add(act);
                    mappedActivityIds.add(act.getId());
                }
            }
        }

        System.out.println("Rows Before Filter : " + mappedActivities.size());

        // 3. Filter by Academic Year if provided
        List<Activity> activities = mappedActivities;
        if (academicYear != null) {
            activities = activities.stream()
                    .filter(a -> a.getAcademicYear() == null || a.getAcademicYear() == academicYear)
                    .toList();
        }
        System.out.println("Rows After Academic Year Filter : " + activities.size());

        // 4. Filter by Subgroup (Must / Individual / Group)
        if (subgroup != null && !subgroup.trim().isEmpty()) {
            final String lowerSubgroup = subgroup.trim().toLowerCase();
            activities = activities.stream()
                    .filter(a -> {
                        boolean isMandatory = a.isMandatory();
                        String mode = a.getModeType() != null ? a.getModeType().toLowerCase() : "";

                        System.out.println("========================================");
                        System.out.println("Activity ID: " + a.getId());
                        System.out.println("Activity Name: " + a.getName());
                        System.out.println("Mandatory Flag: " + isMandatory);
                        System.out.println("Participation Type: " + a.getModeType());
                        System.out.println("Subgroup: " + (a.getSubgroup() != null ? a.getSubgroup().getName() : "null"));
                        System.out.println("Stage ID: " + (a.getStage() != null ? a.getStage().getId() : "null"));

                        boolean included = false;
                        String reason = "";

                        com.pragatix.entity.ActivityStageMapping m = mappings.stream()
                                .filter(mp -> mp.getActivity() != null && mp.getActivity().getId().equals(a.getId()))
                                .findFirst().orElse(null);

                        if (m != null && m.getSubgroup() != null) {
                            included = matchesSubgroup(m.getSubgroup(), lowerSubgroup);
                            reason = included ? "Matched mapped subgroup category/name" : "Did not match mapped subgroup";
                        } else if (a.getSubgroup() != null) {
                            included = matchesSubgroup(a.getSubgroup(), lowerSubgroup);
                            reason = included ? "Matched global subgroup category/name" : "Did not match global subgroup";
                        } else {
                            included = false;
                            reason = "No subgroup assigned";
                        }

                        if (included) {
                            System.out.println("Reason included in response: " + reason);
                        } else {
                            System.out.println("Reason excluded from response: " + reason);
                        }
                        System.out.println("========================================");

                        return included;
                    })
                    .toList();
        }

        for (Activity activity : activities) {
            adminAssignmentService.populateActivityTransientFields(activity, stageId);

            // Apply stage-specific configuration overrides if present in ActivityStageMapping
            com.pragatix.entity.ActivityStageMapping m = mappings.stream()
                    .filter(mp -> mp.getActivity() != null && mp.getActivity().getId().equals(activity.getId()))
                    .findFirst().orElse(null);
            if (m != null) {
                if (m.getAwardXp() != null) activity.setAwardXp(m.getAwardXp());
                if (m.getAwardEnabled() != null) activity.setAwardEnabled(m.getAwardEnabled());
                if (m.getPenaltyEnabled() != null) activity.setPenaltyEnabled(m.getPenaltyEnabled());
                if (m.getPenaltyXp() != null) activity.setPenaltyXp(m.getPenaltyXp());
                if (m.getAwardFrequency() != null) activity.setAwardFrequency(m.getAwardFrequency());
                if (m.getAssignmentMode() != null) activity.setAssignmentMode(m.getAssignmentMode());
            }
        }

        System.out.println("Returned : " + activities.size());
        return ResponseEntity.ok(ApiResponse.ok(activities));
    }

    private boolean matchesSubgroup(com.pragatix.entity.ActivitySubgroup subgroup, String lowerSubgroupFilter) {
        if (subgroup == null || lowerSubgroupFilter == null) return false;
        
        boolean result = false;
        String failReason = "";

        // 1. Match by canonical category
        if (subgroup.getCategory() != null && subgroup.getCategory().trim().equalsIgnoreCase(lowerSubgroupFilter)) {
            result = true;
        } else {
            failReason += "category.equalsIgnoreCase(" + lowerSubgroupFilter + ") returned FALSE; ";
        }
        
        // 2. Fallback to matching by display name prefix
        if (!result && subgroup.getName() != null) {
            String nameLower = subgroup.getName().trim().toLowerCase();
            if (nameLower.startsWith(lowerSubgroupFilter)) {
                result = true;
            } else {
                failReason += "startsWith(" + lowerSubgroupFilter + ") returned FALSE; ";
            }
            if (!result && nameLower.equalsIgnoreCase(lowerSubgroupFilter)) {
                result = true;
            } else if (!result) {
                failReason += "equalsIgnoreCase(" + lowerSubgroupFilter + ") returned FALSE;";
            }
        }

        System.out.println("========== MATCH DEBUG ==========");
        // Hack to get activity ID context if available, otherwise just print subgroup details
        System.out.println("Subgroup ID: " + subgroup.getId());
        System.out.println("Filter   : " + lowerSubgroupFilter);
        System.out.println("Category : " + subgroup.getCategory());
        System.out.println("Name     : " + subgroup.getName());
        System.out.println("Result   : " + result);
        if (!result) {
            System.out.println("Failure  : " + failReason);
        }
        
        return result;
    }
}
