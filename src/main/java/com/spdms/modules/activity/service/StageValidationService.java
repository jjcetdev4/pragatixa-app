package com.spdms.modules.activity.service;

import com.spdms.modules.activity.dto.response.StageValidationResponse;
import com.spdms.entity.ActivityStage;
import com.spdms.entity.ActivitySubgroup;
import com.spdms.entity.Student;
import com.spdms.modules.activity.repository.ActivityStageRepository;
import com.spdms.modules.activity.repository.ActivitySubgroupRepository;
import com.spdms.modules.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StageValidationService {
    private static final Logger log = LoggerFactory.getLogger(StageValidationService.class);

    private final ActivityStageRepository activityStageRepository;
    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final com.spdms.modules.student.repository.StudentActivityXpRepository studentActivityXpRepository;
    private final StudentRepository studentRepository;

    public StageValidationService(ActivityStageRepository activityStageRepository,
                                  ActivitySubgroupRepository activitySubgroupRepository,
                                  com.spdms.modules.student.repository.StudentActivityXpRepository studentActivityXpRepository,
                                  StudentRepository studentRepository) {
        this.activityStageRepository = activityStageRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.studentRepository = studentRepository;
    }

    public StageValidationResponse validateStage(Long studentId, Long stageId) {
        try {
            ActivityStage stage = activityStageRepository.findById(stageId)
                    .orElseThrow(() -> new IllegalArgumentException("Stage not found"));

            StageValidationResponse response = new StageValidationResponse();
            response.setUseDateValidation(stage.isUseDateValidation());
            response.setUseThresholdValidation(stage.isUseThresholdValidation());
            response.setUseCombinedValidation(stage.isUseCombinedValidation());

            LocalDateTime now = LocalDateTime.now();
            String stageStatus;

            String dateStatus = "ACTIVE";
            if (stage.getStartDateTime() != null && now.isBefore(stage.getStartDateTime())) {
                dateStatus = "LOCKED_BEFORE_START";
            } else if (stage.getEndDateTime() != null && now.isAfter(stage.getEndDateTime())) {
                dateStatus = "ENDED";
            }

            boolean thresholdMet = isStageThresholdsMet(studentId, stageId);

            if (!stage.isUseDateValidation() && !stage.isUseThresholdValidation() && !stage.isUseCombinedValidation()) {
                stageStatus = "UNLOCKED";
            } else if (stage.isUseCombinedValidation()) {
                if (dateStatus.equals("ACTIVE")) {
                    stageStatus = thresholdMet ? "UNLOCKED" : "LOCKED_THRESHOLD";
                } else {
                    stageStatus = dateStatus; 
                }
            } else if (stage.isUseDateValidation()) {
                stageStatus = dateStatus; 
            } else if (stage.isUseThresholdValidation()) {
                stageStatus = thresholdMet ? "UNLOCKED" : "LOCKED_THRESHOLD";
            } else {
                stageStatus = "UNLOCKED";
            }

            response.setStageStatus(stageStatus);
            response.setVisible(true);
            response.setLocked(stageStatus.startsWith("LOCKED"));
            response.setCompleted(stageStatus.equals("ENDED") || stageStatus.equals("UNLOCKED"));
            response.setActive(stageStatus.equals("ACTIVE"));

            log.debug("Stage: {} | Status: {}", stage.getName(), stageStatus);
            return response;
        } catch (Exception e) {
            throw e;
        }
    }

    public boolean isStageThresholdsMet(Long studentId, Long stageId) {
        ActivityStage stage = activityStageRepository.findById(stageId).orElse(null);
        if (stage == null) return false;

        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null) return false;

        if (stage.getExpectedXp() != null && stage.getExpectedXp() > 0 && student.getTotalXp() < stage.getExpectedXp()) return false;
        if (stage.getMustThreshold() != null && stage.getMustThreshold() > 0 && student.getMustXp() < stage.getMustThreshold()) return false;
        if (stage.getIndividualThreshold() != null && stage.getIndividualThreshold() > 0 && student.getIndividualXp() < stage.getIndividualThreshold()) return false;
        if (stage.getGroupThreshold() != null && stage.getGroupThreshold() > 0 && student.getGroupXp() < stage.getGroupThreshold()) return false;

        List<ActivitySubgroup> subgroups = activitySubgroupRepository.findByStageId(stageId);
        List<com.spdms.entity.StudentActivityXp> history = studentActivityXpRepository.findByStudentId(studentId);
        
        for (ActivitySubgroup subgroup : subgroups) {
            int earnedXp = history.stream()
                    .filter(xp -> xp.getActivity() != null && xp.getActivity().getSubgroup() != null && xp.getActivity().getSubgroup().getId().equals(subgroup.getId()))
                    .mapToInt(com.spdms.entity.StudentActivityXp::getXpAwarded)
                    .sum();
            
            if (earnedXp < subgroup.getThreshold()) {
                return false;
            }
        }
        return true;
    }
}
