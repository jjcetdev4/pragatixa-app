package com.spdms.modules.admin.service;

import com.spdms.common.response.ApiResponse;
import com.spdms.entity.Activity;
import com.spdms.entity.ActivitySubgroup;
import com.spdms.modules.activity.repository.ActivityRepository;
import com.spdms.modules.activity.repository.ActivitySubgroupRepository;
import com.spdms.repository.ActivityAssignmentRepository;
import com.spdms.repository.DisciplineLogRepository;
import com.spdms.modules.student.repository.StudentActivityXpRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ActivityCrudService {

    private static final Logger log = LoggerFactory.getLogger(ActivityCrudService.class);

    private final ActivityRepository activityRepository;
    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final DisciplineLogRepository disciplineLogRepository;
    private final StudentActivityXpRepository studentActivityXpRepository;
    private final ActivityValidationService validationService;
    private final ActivityRequestMapper requestMapper;
    private final AdminAssignmentService adminAssignmentService;

    public ActivityCrudService(
            ActivityRepository activityRepository, 
            ActivitySubgroupRepository activitySubgroupRepository,
            ActivityAssignmentRepository activityAssignmentRepository,
            DisciplineLogRepository disciplineLogRepository,
            StudentActivityXpRepository studentActivityXpRepository,
            ActivityValidationService validationService, 
            ActivityRequestMapper requestMapper,
            AdminAssignmentService adminAssignmentService) {
        this.activityRepository = activityRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.disciplineLogRepository = disciplineLogRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.validationService = validationService;
        this.requestMapper = requestMapper;
        this.adminAssignmentService = adminAssignmentService;
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

        requestMapper.mapBasicFields(activity, body);

        String xpCategory = requestMapper.extractXpCategory(body);
        ResponseEntity<ApiResponse<String>> catVal = validationService.validateXpCategory(xpCategory);
        if (catVal != null) return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(catVal.getBody().getMessage()));
        String matchedCategory = validationService.matchXpCategory(xpCategory);

        Object[] awardConfig = requestMapper.parseAwardConfiguration(body);
        boolean awardEnabled = (Boolean) awardConfig[0];
        Integer awardXp = (Integer) awardConfig[1];
        boolean penaltyEnabled = (Boolean) awardConfig[2];
        Integer penaltyXp = (Integer) awardConfig[3];
        
        ResponseEntity<ApiResponse<String>> confVal = validationService.validateXpConfiguration(awardEnabled, penaltyEnabled, awardXp, penaltyXp);
        if (confVal != null) return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(confVal.getBody().getMessage()));

        String awardType = requestMapper.parseAwardType(body);
        String awardFrequencyFinal = requestMapper.parseAwardFrequency(body);
        
        ResponseEntity<ApiResponse<String>> freqVal = validationService.validateAwardFrequency(awardFrequencyFinal);
        if (freqVal != null) return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(freqVal.getBody().getMessage()));
        String matchedFrequency = validationService.matchAwardFrequency(awardFrequencyFinal);

        Integer cap = requestMapper.parseCap(body, matchedFrequency);
        ResponseEntity<ApiResponse<String>> capVal = validationService.validateCap(matchedFrequency, cap);
        if (capVal != null) return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(capVal.getBody().getMessage()));

        List<String> awardDays = requestMapper.parseAwardDays(body);
        ResponseEntity<ApiResponse<String>> daysVal = validationService.validateAwardDays(awardDays);
        if (daysVal != null) return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(daysVal.getBody().getMessage()));

        requestMapper.mapRemainingConfiguration(activity, body, matchedCategory, awardEnabled, awardXp, penaltyEnabled, penaltyXp, awardType, matchedFrequency, cap, awardDays);

        log.debug("Entity before save [Create] - Award Enabled: {}, Award XP: {}, Penalty Enabled: {}, Penalty XP: {}",
                 activity.getAwardEnabled(), activity.getAwardXp(), activity.getPenaltyEnabled(), activity.getPenaltyXp());
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

        requestMapper.mapBasicFields(activity, body);

        String xpCategory = requestMapper.extractXpCategory(body);
        ResponseEntity<ApiResponse<String>> catVal = validationService.validateXpCategory(xpCategory);
        if (catVal != null) return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(catVal.getBody().getMessage()));
        String matchedCategory = validationService.matchXpCategory(xpCategory);

        Object[] awardConfig = requestMapper.parseAwardConfiguration(body);
        boolean awardEnabled = (Boolean) awardConfig[0];
        Integer awardXp = (Integer) awardConfig[1];
        boolean penaltyEnabled = (Boolean) awardConfig[2];
        Integer penaltyXp = (Integer) awardConfig[3];
        
        ResponseEntity<ApiResponse<String>> confVal = validationService.validateXpConfiguration(awardEnabled, penaltyEnabled, awardXp, penaltyXp);
        if (confVal != null) return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(confVal.getBody().getMessage()));

        String awardType = requestMapper.parseAwardType(body);
        String awardFrequencyFinal = requestMapper.parseAwardFrequency(body);
        
        ResponseEntity<ApiResponse<String>> freqVal = validationService.validateAwardFrequency(awardFrequencyFinal);
        if (freqVal != null) return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(freqVal.getBody().getMessage()));
        String matchedFrequency = validationService.matchAwardFrequency(awardFrequencyFinal);

        Integer cap = requestMapper.parseCap(body, matchedFrequency);
        ResponseEntity<ApiResponse<String>> capVal = validationService.validateCap(matchedFrequency, cap);
        if (capVal != null) return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(capVal.getBody().getMessage()));

        List<String> awardDays = requestMapper.parseAwardDays(body);
        ResponseEntity<ApiResponse<String>> daysVal = validationService.validateAwardDays(awardDays);
        if (daysVal != null) return ResponseEntity.badRequest().body(ApiResponse.<Activity>error(daysVal.getBody().getMessage()));

        requestMapper.mapRemainingConfiguration(activity, body, matchedCategory, awardEnabled, awardXp, penaltyEnabled, penaltyXp, awardType, matchedFrequency, cap, awardDays);

        log.debug("Entity before save [Update] - Award Enabled: {}, Award XP: {}, Penalty Enabled: {}, Penalty XP: {}",
                 activity.getAwardEnabled(), activity.getAwardXp(), activity.getPenaltyEnabled(), activity.getPenaltyXp());
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
}
