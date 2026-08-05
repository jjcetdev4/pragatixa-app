package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.Activity;
import com.pragatix.entity.ActivitySubgroup;
import com.pragatix.modules.activity.repository.ActivityRepository;
import com.pragatix.modules.activity.repository.ActivitySubgroupRepository;
import com.pragatix.repository.ActivityAssignmentRepository;
import com.pragatix.repository.DisciplineLogRepository;
import com.pragatix.modules.student.repository.StudentActivityXpRepository;
import com.pragatix.repository.StudentActivityStreakRepository;
import com.pragatix.repository.XpTransactionRepository;
import com.pragatix.repository.PenaltyRequestRepository;
import com.pragatix.repository.ActivityCompletionRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pragatix.entity.ActivityStageMapping;
import com.pragatix.modules.activity.repository.ActivityStageMappingRepository;
import com.pragatix.modules.activity.repository.ActivityStageRepository;
import com.pragatix.entity.ActivityStage;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class ActivityCrudService {

    private static final Logger log = LoggerFactory.getLogger(ActivityCrudService.class);

    private final ActivityRepository activityRepository;
    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final ActivityStageRepository activityStageRepository;
    private final ActivityStageMappingRepository activityStageMappingRepository;
    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final PenaltyRequestRepository penaltyRequestRepository;
    private final ActivityCompletionRequestRepository activityCompletionRequestRepository;
    private final DisciplineLogRepository disciplineLogRepository;
    private final StudentActivityXpRepository studentActivityXpRepository;
    private final StudentActivityStreakRepository studentActivityStreakRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final ActivityValidationService validationService;
    private final ActivityRequestMapper requestMapper;
    private final AdminAssignmentService adminAssignmentService;

    private final com.pragatix.modules.authentication.repository.UserRepository userRepository;

    public ActivityCrudService(
            ActivityRepository activityRepository,
            ActivitySubgroupRepository activitySubgroupRepository,
            ActivityStageRepository activityStageRepository,
            ActivityStageMappingRepository activityStageMappingRepository,
            ActivityAssignmentRepository activityAssignmentRepository,
            PenaltyRequestRepository penaltyRequestRepository,
            ActivityCompletionRequestRepository activityCompletionRequestRepository,
            DisciplineLogRepository disciplineLogRepository,
            StudentActivityXpRepository studentActivityXpRepository,
            StudentActivityStreakRepository studentActivityStreakRepository,
            XpTransactionRepository xpTransactionRepository,
            ActivityValidationService validationService,
            ActivityRequestMapper requestMapper,
            AdminAssignmentService adminAssignmentService,
            com.pragatix.modules.authentication.repository.UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.activityStageRepository = activityStageRepository;
        this.activityStageMappingRepository = activityStageMappingRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.penaltyRequestRepository = penaltyRequestRepository;
        this.activityCompletionRequestRepository = activityCompletionRequestRepository;
        this.disciplineLogRepository = disciplineLogRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.studentActivityStreakRepository = studentActivityStreakRepository;
        this.xpTransactionRepository = xpTransactionRepository;
        this.validationService = validationService;
        this.requestMapper = requestMapper;
        this.adminAssignmentService = adminAssignmentService;
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
    public ResponseEntity<ApiResponse<Activity>> createActivity(Long subgroupId, Map<String, Object> body) {
        ActivitySubgroup subgroup = activitySubgroupRepository.findById(subgroupId).orElse(null);
        if (subgroup == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Activity>error("Subgroup not found"));
        }

        Activity activity = new Activity();
        activity.setSubgroup(subgroup);
        activity.setStage(subgroup.getStage());
        if (subgroup.getStage() != null) {
            activity.setAcademicYear(subgroup.getStage().getAcademicYear());
            try {
                validateAdminAcademicYearAccess(activity.getAcademicYear());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(e.getMessage()));
            }
        }

        requestMapper.mapBasicFields(activity, body);

        String xpCategory = requestMapper.extractXpCategory(body);
        ResponseEntity<ApiResponse<String>> catVal = validationService.validateXpCategory(xpCategory);
        if (catVal != null)
            return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(catVal.getBody().getMessage()));
        String matchedCategory = validationService.matchXpCategory(xpCategory);

        Object[] awardConfig = requestMapper.parseAwardConfiguration(body, null);
        boolean awardEnabled = (Boolean) awardConfig[0];
        Integer awardXp = (Integer) awardConfig[1];
        boolean penaltyEnabled = (Boolean) awardConfig[2];
        Integer penaltyXp = (Integer) awardConfig[3];

        ResponseEntity<ApiResponse<String>> confVal = null;
        if (!Boolean.TRUE.equals(activity.getAttendanceEngineEnabled())) {
            confVal = validationService.validateXpConfiguration(awardEnabled, penaltyEnabled, awardXp, penaltyXp);
        }
        if (confVal != null)
            return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(confVal.getBody().getMessage()));

        String awardType = requestMapper.parseAwardType(body);
        String awardFrequencyFinal = requestMapper.parseAwardFrequency(body);

        ResponseEntity<ApiResponse<String>> freqVal = validationService.validateAwardFrequency(awardFrequencyFinal);
        if (freqVal != null)
            return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(freqVal.getBody().getMessage()));
        String matchedFrequency = validationService.matchAwardFrequency(awardFrequencyFinal);

        Integer cap = requestMapper.parseCap(body, matchedFrequency);
        ResponseEntity<ApiResponse<String>> capVal = validationService.validateCap(matchedFrequency, cap);
        if (capVal != null)
            return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(capVal.getBody().getMessage()));

        List<String> awardDays = requestMapper.parseAwardDays(body);
        ResponseEntity<ApiResponse<String>> daysVal = validationService.validateAwardDays(awardDays);
        if (daysVal != null)
            return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(daysVal.getBody().getMessage()));

        requestMapper.mapRemainingConfiguration(activity, body, matchedCategory, awardEnabled, awardXp, penaltyEnabled,
                penaltyXp, awardType, matchedFrequency, cap, awardDays);

        if (Boolean.TRUE.equals(activity.getAttendanceEngineEnabled())) {
            if (activity.getEvidence() == null || activity.getEvidence().trim().isEmpty()) {
                activity.setEvidence("Manual");
            }
            if (activity.getStage() != null) {
                long existingCount = activityRepository.countByStageIdAndAttendanceEngineEnabledTrue(activity.getStage().getId());
                if (existingCount > 0) {
                    String errorMsg = "An Attendance Engine activity is already configured for this stage.";
                    return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(errorMsg));
                }
            }
        }

        log.debug("Entity before save [Create] - Award Enabled: {}, Award XP: {}, Penalty Enabled: {}, Penalty XP: {}",
                activity.getAwardEnabled(), activity.getAwardXp(), activity.getPenaltyEnabled(),
                activity.getPenaltyXp());
        log.info("FORENSIC: ActivityCrudService before save - attendanceEngineEnabled: {}", activity.getAttendanceEngineEnabled());
        Activity saved = activityRepository.save(activity);
        log.info("FORENSIC: ActivityCrudService after save - attendanceEngineEnabled: {}", saved.getAttendanceEngineEnabled());

        log.debug("Entity after save [Create] - Award Enabled: {}, Award XP: {}, Penalty Enabled: {}, Penalty XP: {}",
                saved.getAwardEnabled(), saved.getAwardXp(), saved.getPenaltyEnabled(), saved.getPenaltyXp());
        adminAssignmentService.populateActivityTransientFields(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Activity created successfully", saved));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Activity>> updateActivity(Long activityId, Map<String, Object> body) {
        System.out.println("========== FORENSIC TRACE: UPDATE ACTIVITY ==========");
        System.out.println("Activity ID : " + activityId);
        System.out.println("Raw Request Body : " + body);
        
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Activity>error("Activity not found"));
        }

        try {
            validateAdminAcademicYearAccess(activity.getAcademicYear());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(e.getMessage()));
        }

        System.out.println("Status before mapBasicFields : " + activity.getStatus());
        requestMapper.mapBasicFields(activity, body);
        System.out.println("Status after mapBasicFields : " + activity.getStatus());

        String xpCategory = requestMapper.extractXpCategory(body);
        ResponseEntity<ApiResponse<String>> catVal = validationService.validateXpCategory(xpCategory);
        if (catVal != null)
            return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(catVal.getBody().getMessage()));
        String matchedCategory = validationService.matchXpCategory(xpCategory);

        Object[] awardConfig = requestMapper.parseAwardConfiguration(body, activity);
        boolean awardEnabled = (Boolean) awardConfig[0];
        Integer awardXp = (Integer) awardConfig[1];
        boolean penaltyEnabled = (Boolean) awardConfig[2];
        Integer penaltyXp = (Integer) awardConfig[3];

        ResponseEntity<ApiResponse<String>> confVal = null;
        if (!Boolean.TRUE.equals(activity.getAttendanceEngineEnabled())) {
            confVal = validationService.validateXpConfiguration(awardEnabled, penaltyEnabled, awardXp, penaltyXp);
        }
        if (confVal != null)
            return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(confVal.getBody().getMessage()));

        String awardType = requestMapper.parseAwardType(body);
        String awardFrequencyFinal = requestMapper.parseAwardFrequency(body);

        ResponseEntity<ApiResponse<String>> freqVal = validationService.validateAwardFrequency(awardFrequencyFinal);
        if (freqVal != null)
            return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(freqVal.getBody().getMessage()));
        String matchedFrequency = validationService.matchAwardFrequency(awardFrequencyFinal);

        Integer cap = requestMapper.parseCap(body, matchedFrequency);
        ResponseEntity<ApiResponse<String>> capVal = validationService.validateCap(matchedFrequency, cap);
        if (capVal != null)
            return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(capVal.getBody().getMessage()));

        List<String> awardDays = requestMapper.parseAwardDays(body);
        ResponseEntity<ApiResponse<String>> daysVal = validationService.validateAwardDays(awardDays);
        if (daysVal != null)
            return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(daysVal.getBody().getMessage()));

        requestMapper.mapRemainingConfiguration(activity, body, matchedCategory, awardEnabled, awardXp, penaltyEnabled,
                penaltyXp, awardType, matchedFrequency, cap, awardDays);

        if (Boolean.TRUE.equals(activity.getAttendanceEngineEnabled())) {
            if (activity.getEvidence() == null || activity.getEvidence().trim().isEmpty()) {
                activity.setEvidence("Manual");
            }
            if (activity.getStage() != null) {
                java.util.Optional<Activity> existing = activityRepository.findByStageIdAndAttendanceEngineEnabledTrue(activity.getStage().getId());
                if (existing.isPresent() && !existing.get().getId().equals(activity.getId())) {
                    String errorMsg = "An Attendance Engine activity is already configured for this stage.";
                    return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(errorMsg));
                }
            }
        }

        if (body.containsKey("stageId") && body.get("stageId") != null) {
            try {
                Long stageId = Long.valueOf(body.get("stageId").toString());
                ActivityStageMapping mapping = activityStageMappingRepository.findByStageIdAndActivityId(stageId, activityId).orElse(null);
                if (mapping != null) {
                    mapping.setAwardXp(awardXp);
                    mapping.setAwardEnabled(awardEnabled);
                    mapping.setPenaltyEnabled(penaltyEnabled);
                    mapping.setPenaltyXp(penaltyXp);
                    mapping.setAwardFrequency(matchedFrequency);
                    activityStageMappingRepository.save(mapping);
                }
            } catch (Exception ignored) {}
        } else {
            // Synchronize across all mappings for this activity if specific stageId is not provided
            try {
                List<com.pragatix.entity.ActivityStageMapping> mappings = activityStageMappingRepository.findByActivityId(activityId);
                for (com.pragatix.entity.ActivityStageMapping m : mappings) {
                    m.setAwardXp(awardXp);
                    m.setAwardEnabled(awardEnabled);
                    m.setPenaltyEnabled(penaltyEnabled);
                    m.setPenaltyXp(penaltyXp);
                    m.setAwardFrequency(matchedFrequency);
                }
                if (!mappings.isEmpty()) {
                    activityStageMappingRepository.saveAll(mappings);
                }
            } catch (Exception ignored) {}
        }

        if (body.containsKey("subgroup") && body.get("subgroup") != null) {
            String newSubgroupName = body.get("subgroup").toString().trim();
            if (activity.getSubgroup() != null && activity.getSubgroup().getStage() != null) {
                Long primaryStageId = activity.getSubgroup().getStage().getId();
                ActivitySubgroup newSubgroup = activitySubgroupRepository.findByStageIdAndNameIgnoreCase(primaryStageId, newSubgroupName).orElse(null);
                if (newSubgroup != null) {
                    activity.setSubgroup(newSubgroup);
                    
                    // Synchronize the subgroup update across all mappings for this activity
                    List<com.pragatix.entity.ActivityStageMapping> mappings = activityStageMappingRepository.findByActivityId(activityId);
                    for (com.pragatix.entity.ActivityStageMapping m : mappings) {
                        Long mappingStageId = m.getStage().getId();
                        ActivitySubgroup targetSubgroup = activitySubgroupRepository.findByStageIdAndNameIgnoreCase(mappingStageId, newSubgroupName).orElse(null);
                        
                        // Fallback matching logic similar to matchesSubgroup
                        if (targetSubgroup == null) {
                            List<ActivitySubgroup> stageSubgroups = activitySubgroupRepository.findByStageId(mappingStageId);
                            for (ActivitySubgroup sg : stageSubgroups) {
                                if (sg.getCategory() != null && sg.getCategory().trim().equalsIgnoreCase(newSubgroupName)) {
                                    targetSubgroup = sg;
                                    break;
                                } else if (sg.getName() != null && sg.getName().toLowerCase().startsWith(newSubgroupName.toLowerCase())) {
                                    targetSubgroup = sg;
                                    break;
                                }
                            }
                        }
                        
                        if (targetSubgroup != null) {
                            m.setSubgroup(targetSubgroup);
                        }
                    }
                    if (!mappings.isEmpty()) {
                        activityStageMappingRepository.saveAll(mappings);
                    }
                    
                } else {
                    return ResponseEntity.badRequest().body(ApiResponse.<Activity>error("Invalid subgroup name for this stage"));
                }
            }
        }

        log.debug("Entity before save [Update] - Award Enabled: {}, Award XP: {}, Penalty Enabled: {}, Penalty XP: {}",
                activity.getAwardEnabled(), activity.getAwardXp(), activity.getPenaltyEnabled(),
                activity.getPenaltyXp());

        System.out.println("Status before save : " + activity.getStatus());
        log.info("FORENSIC: ActivityCrudService before save - attendanceEngineEnabled: {}", activity.getAttendanceEngineEnabled());
        Activity saved = activityRepository.save(activity);
        log.info("FORENSIC: ActivityCrudService after save - attendanceEngineEnabled: {}", saved.getAttendanceEngineEnabled());

        activityRepository.flush(); // Force write to DB
        
        // Re-read from database
        Activity reRead = activityRepository.findById(saved.getId()).orElse(null);
        System.out.println("Status after save (re-read from DB) : " + (reRead != null ? reRead.getStatus() : "null"));
        System.out.println("=====================================================");

        adminAssignmentService.populateActivityTransientFields(saved);
        return ResponseEntity.ok(ApiResponse.ok("Activity updated successfully", saved));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteActivity(Long activityId, boolean force) {
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Activity not found"));
        }

        System.out.println("=================================");
        System.out.println("ACTIVITY DELETE");
        System.out.println("=================================");
        System.out.println("Activity ID : " + activityId);

        // 1. Delete penalty_requests WHERE activity_id = ?
        System.out.println("Deleting Penalty Requests...");
        int deletedPenaltyRequests = penaltyRequestRepository.deleteByActivityId(activityId);
        System.out.println("Deleted : " + deletedPenaltyRequests);

        // 2. Delete activity completion requests
        activityCompletionRequestRepository.deleteByActivityId(activityId);

        // 3. Remove all Stage Activity mappings for the activity
        activityStageMappingRepository.deleteByActivityId(activityId);

        // 4. Remove any Activity Assignment mappings
        activityAssignmentRepository.deleteByActivityId(activityId);

        // 5. Remove XP configuration/mappings related to the activity
        studentActivityXpRepository.deleteByActivityId(activityId);
        xpTransactionRepository.deleteByActivityId(activityId);
        studentActivityStreakRepository.deleteByActivityId(activityId);

        // 6. Remove discipline log references pointing to this activity
        disciplineLogRepository.nullifyActivityReferences(activityId);

        // Flush changes
        activityRepository.flush();

        // 7. Delete Activity
        System.out.println("Deleting Activity...");
        activityRepository.deleteById(activityId);
        activityRepository.flush();

        System.out.println("Success");
        System.out.println("Activity Deleted Successfully");
        System.out.println("=================================");

        log.info("Admin deleted activity with ID and all its references: {}", activityId);
        return ResponseEntity.ok(ApiResponse.ok("Activity deleted successfully", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> mapActivityToStage(Long stageId, Long activityId, String subgroupName) {
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Activity not found"));
        }

        ActivityStage stage = activityStageRepository.findById(stageId).orElse(null);
        if (stage == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Stage not found"));
        }

        if (activity.getStatus() != null && "INACTIVE".equalsIgnoreCase(activity.getStatus())) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Cannot map inactive or deleted activity"));
        }

        if (activity.getAcademicYear() != null && stage.getAcademicYear() != null && activity.getAcademicYear() != stage.getAcademicYear()) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Cross-year mapping is not allowed"));
        }

        try {
            validateAdminAcademicYearAccess(stage.getAcademicYear());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>error(e.getMessage()));
        }

        // Check if activity with same name is already mapped to target stage
        List<Activity> existingStageActivities = activityRepository.findByStageId(stage.getId());
        String sourceNameLower = activity.getName() != null ? activity.getName().trim().toLowerCase() : "";
        for (Activity existing : existingStageActivities) {
            String existingNameLower = existing.getName() != null ? existing.getName().trim().toLowerCase() : "";
            if (!sourceNameLower.isEmpty() && existingNameLower.equals(sourceNameLower)) {
                return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Activity '" + activity.getName() + "' is already mapped to this stage"));
            }
        }

        String computedCategory = subgroupName.trim().toLowerCase();

        ActivitySubgroup subgroup = activitySubgroupRepository.findByStageIdAndCategoryIgnoreCase(stageId, computedCategory)
                .orElseGet(() -> activitySubgroupRepository.findByStageIdAndNameIgnoreCase(stageId, computedCategory)
                        .orElse(null));

        if (subgroup == null) {
            subgroup = new ActivitySubgroup();
            subgroup.setStage(stage);
            subgroup.setCategory(computedCategory);
            String displayName = computedCategory.substring(0, 1).toUpperCase() + computedCategory.substring(1).toLowerCase();
            if (computedCategory.equalsIgnoreCase("must")) {
                subgroup.setThreshold(
                        subgroup.getStage().getMustThreshold() != null ? subgroup.getStage().getMustThreshold() : 0);
            } else if (computedCategory.equalsIgnoreCase("individual")) {
                subgroup.setThreshold(subgroup.getStage().getIndividualThreshold() != null
                        ? subgroup.getStage().getIndividualThreshold()
                        : 0);
            } else if (computedCategory.equalsIgnoreCase("group")) {
                subgroup.setThreshold(
                        subgroup.getStage().getGroupThreshold() != null ? subgroup.getStage().getGroupThreshold() : 0);
            } else {
                subgroup.setThreshold(0);
            }
            subgroup.setName(displayName);
            subgroup = activitySubgroupRepository.save(subgroup);
        }

        if (activityStageMappingRepository.existsByStageIdAndActivityId(stageId, activityId)) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Activity is already mapped to this stage"));
        }

        try {
            System.out.println("========================================");
            System.out.println("Stage ID: " + stageId);
            System.out.println("Activity ID: " + activityId);
            System.out.println("Activity Name: " + activity.getName());
            System.out.println("Activity Type: " + activity.getModeType());
            System.out.println("isMandatory: " + activity.isMandatory());
            System.out.println("Selected Subgroup: " + computedCategory);
            System.out.println("Subgroup ID: " + (subgroup != null ? subgroup.getId() : "null"));
            System.out.println("SQL INSERT: ActivityStageMapping (activity_id=" + activityId + ", stage_id=" + stageId + ", subgroup_id=" + (subgroup != null ? subgroup.getId() : "null") + ")");
            System.out.println("========================================");

            ActivityStageMapping mapping = new ActivityStageMapping(activity, stage, subgroup);
            mapping.setAwardEnabled(activity.getAwardEnabled());
            mapping.setPenaltyEnabled(activity.getPenaltyEnabled());
            mapping.setAwardXp(activity.getAwardXp());
            mapping.setPenaltyXp(activity.getPenaltyXp());
            mapping.setAwardFrequency(activity.getAwardFrequency());
            activityStageMappingRepository.save(mapping);
            log.debug("Successfully mapped activity ID {} to stage ID {} subgroup {}", activityId, stageId, subgroupName);
            return ResponseEntity.ok(ApiResponse.ok("Activity mapped successfully", null));
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity error mapping activity to stage: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Void>error("Activity is already mapped to this stage."));
        } catch (Exception e) {
            log.error("Failed to map activity to stage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>error("Failed to map activity to stage: " + e.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> unmapActivityFromStage(Long stageId, Long activityId) {
        ActivityStageMapping mapping = activityStageMappingRepository.findByStageIdAndActivityId(stageId, activityId).orElse(null);
        if (mapping != null) {
            activityStageMappingRepository.delete(mapping);
            return ResponseEntity.ok(ApiResponse.ok("Activity removed from stage successfully", null));
        }

        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity != null && activity.getStage() != null && activity.getStage().getId().equals(stageId)) {
            activity.setStage(null);
            activityRepository.save(activity);
            return ResponseEntity.ok(ApiResponse.ok("Activity removed from stage successfully", null));
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Activity mapping not found"));
    }
}
