package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.Activity;
import com.pragatix.entity.ActivitySubgroup;
import com.pragatix.modules.activity.repository.ActivityRepository;
import com.pragatix.modules.activity.repository.ActivitySubgroupRepository;
import com.pragatix.repository.ActivityAssignmentRepository;
import com.pragatix.repository.DisciplineLogRepository;
import com.pragatix.modules.student.repository.StudentActivityXpRepository;
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
    private final DisciplineLogRepository disciplineLogRepository;
    private final StudentActivityXpRepository studentActivityXpRepository;
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
            DisciplineLogRepository disciplineLogRepository,
            StudentActivityXpRepository studentActivityXpRepository,
            ActivityValidationService validationService,
            ActivityRequestMapper requestMapper,
            AdminAssignmentService adminAssignmentService,
            com.pragatix.modules.authentication.repository.UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.activityStageRepository = activityStageRepository;
        this.activityStageMappingRepository = activityStageMappingRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.disciplineLogRepository = disciplineLogRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
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

        Object[] awardConfig = requestMapper.parseAwardConfiguration(body);
        boolean awardEnabled = (Boolean) awardConfig[0];
        Integer awardXp = (Integer) awardConfig[1];
        boolean penaltyEnabled = (Boolean) awardConfig[2];
        Integer penaltyXp = (Integer) awardConfig[3];

        ResponseEntity<ApiResponse<String>> confVal = validationService.validateXpConfiguration(awardEnabled,
                penaltyEnabled, awardXp, penaltyXp);
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

        log.debug("Entity before save [Create] - Award Enabled: {}, Award XP: {}, Penalty Enabled: {}, Penalty XP: {}",
                activity.getAwardEnabled(), activity.getAwardXp(), activity.getPenaltyEnabled(),
                activity.getPenaltyXp());
        Activity saved = activityRepository.save(activity);
        log.debug("Entity after save [Create] - Award Enabled: {}, Award XP: {}, Penalty Enabled: {}, Penalty XP: {}",
                saved.getAwardEnabled(), saved.getAwardXp(), saved.getPenaltyEnabled(), saved.getPenaltyXp());
        adminAssignmentService.populateActivityTransientFields(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Activity created successfully", saved));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Activity>> updateActivity(Long activityId, Map<String, Object> body) {
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Activity>error("Activity not found"));
        }

        try {
            validateAdminAcademicYearAccess(activity.getAcademicYear());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(e.getMessage()));
        }

        requestMapper.mapBasicFields(activity, body);

        String xpCategory = requestMapper.extractXpCategory(body);
        ResponseEntity<ApiResponse<String>> catVal = validationService.validateXpCategory(xpCategory);
        if (catVal != null)
            return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(catVal.getBody().getMessage()));
        String matchedCategory = validationService.matchXpCategory(xpCategory);

        Object[] awardConfig = requestMapper.parseAwardConfiguration(body);
        boolean awardEnabled = (Boolean) awardConfig[0];
        Integer awardXp = (Integer) awardConfig[1];
        boolean penaltyEnabled = (Boolean) awardConfig[2];
        Integer penaltyXp = (Integer) awardConfig[3];

        ResponseEntity<ApiResponse<String>> confVal = validationService.validateXpConfiguration(awardEnabled,
                penaltyEnabled, awardXp, penaltyXp);
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
        }

        log.debug("Entity before save [Update] - Award Enabled: {}, Award XP: {}, Penalty Enabled: {}, Penalty XP: {}",
                activity.getAwardEnabled(), activity.getAwardXp(), activity.getPenaltyEnabled(),
                activity.getPenaltyXp());
        Activity saved = activityRepository.save(activity);
        log.debug("Entity after save [Update] - Award Enabled: {}, Award XP: {}, Penalty Enabled: {}, Penalty XP: {}",
                saved.getAwardEnabled(), saved.getAwardXp(), saved.getPenaltyEnabled(), saved.getPenaltyXp());
        adminAssignmentService.populateActivityTransientFields(saved);
        return ResponseEntity.ok(ApiResponse.ok("Activity updated successfully", saved));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteActivity(Long activityId, boolean force) {
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Activity not found"));
        }

        long xpTransactions = studentActivityXpRepository.countByActivityId(activityId);
        long studentAssignments = activityAssignmentRepository.countByActivityId(activityId);
        long disciplineLogs = disciplineLogRepository.countByActivityId(activityId);
        boolean isMappedToStage = activity.getStage() != null;

        if (!force && (xpTransactions > 0 || studentAssignments > 0 || disciplineLogs > 0 || isMappedToStage)) {
            String msg = "Cannot delete Activity. Currently referenced by: " +
                    (xpTransactions > 0 ? "\n• " + xpTransactions + " XP Transactions" : "") +
                    (studentAssignments > 0 ? "\n• " + studentAssignments + " Student Assignments" : "") +
                    (disciplineLogs > 0 ? "\n• " + disciplineLogs + " Discipline Logs" : "") +
                    (isMappedToStage ? "\n• 1 Stage Mapping" : "");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.<Void>error(msg));
        }

        if (force) {
            studentActivityXpRepository.deleteByActivityId(activityId);
            activityAssignmentRepository.deleteByActivityId(activityId);
            disciplineLogRepository.nullifyActivityReferences(activityId);
        }

        activityRepository.deleteById(activityId);
        log.debug("Admin deleted activity with ID: {}", activityId);
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

        String mode = activity.getModeType() != null ? activity.getModeType().trim().toLowerCase() : "";
        String computedCategory;
        if (mode.contains("group")) {
            computedCategory = "group";
        } else {
            if (activity.isMandatory()) {
                computedCategory = "must";
            } else {
                computedCategory = "individual";
            }
        }

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
