package com.pragatix.modules.activity.mapper;

import com.pragatix.modules.activity.dto.request.ActivityStageRequest;
import com.pragatix.modules.activity.dto.response.ActivityStageResponse;
import com.pragatix.entity.ActivityStage;
import com.pragatix.enums.StageStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class ActivityStageMapper {

    public ActivityStage toEntity(ActivityStageRequest request) {
        if (request == null) {
            return null;
        }
        return ActivityStage.builder()
                .name(request.getName())
                .stageName(request.getName())
                .description(request.getDescription())
                .expectedXp(request.getExpectedXp() != null ? request.getExpectedXp() : 0)
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .displayOrder(request.getDisplayOrder())
                .useDateValidation(request.isUseDateValidation())
                .useThresholdValidation(request.isUseThresholdValidation())
                .useCombinedValidation(request.isUseCombinedValidation())
                .mustThreshold(request.getMustThreshold() != null ? request.getMustThreshold() : 0)
                .individualThreshold(request.getIndividualThreshold() != null ? request.getIndividualThreshold() : 0)
                .groupThreshold(request.getGroupThreshold() != null ? request.getGroupThreshold() : 0)
                .academicYear(request.getAcademicYear())
                .status(StageStatus.UPCOMING) // Default to UPCOMING for new stages
                .build();
    }

    public void updateEntity(ActivityStageRequest request, ActivityStage entity) {
        if (request == null || entity == null) {
            return;
        }
        entity.setName(request.getName());
        entity.setStageName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setExpectedXp(request.getExpectedXp() != null ? request.getExpectedXp() : 0);
        entity.setStartDateTime(request.getStartDateTime());
        entity.setEndDateTime(request.getEndDateTime());
        entity.setDisplayOrder(request.getDisplayOrder());
        entity.setUseDateValidation(request.isUseDateValidation());
        entity.setUseThresholdValidation(request.isUseThresholdValidation());
        entity.setUseCombinedValidation(request.isUseCombinedValidation());
        entity.setMustThreshold(request.getMustThreshold() != null ? request.getMustThreshold() : 0);
        entity.setIndividualThreshold(request.getIndividualThreshold() != null ? request.getIndividualThreshold() : 0);
        entity.setGroupThreshold(request.getGroupThreshold() != null ? request.getGroupThreshold() : 0);
        entity.setAcademicYear(request.getAcademicYear());
    }

    public ActivityStageResponse toResponse(ActivityStage entity) {
        if (entity == null) {
            return null;
        }
        ActivityStageResponse response = new ActivityStageResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setExpectedXp(entity.getExpectedXp());
        response.setStartDateTime(entity.getStartDateTime());
        response.setEndDateTime(entity.getEndDateTime());
        response.setDisplayOrder(entity.getDisplayOrder());
        response.setUseDateValidation(entity.isUseDateValidation());
        response.setUseThresholdValidation(entity.isUseThresholdValidation());
        response.setUseCombinedValidation(entity.isUseCombinedValidation());
        response.setMustThreshold(entity.getMustThreshold());
        response.setIndividualThreshold(entity.getIndividualThreshold());
        response.setGroupThreshold(entity.getGroupThreshold());
        response.setAcademicYear(entity.getAcademicYear());

        LocalDateTime now = LocalDateTime.now();
        StageStatus calculatedStatus;

        if (entity.getStartDateTime() == null && entity.getEndDateTime() == null) {
            calculatedStatus = StageStatus.ACTIVE;
        } else if (entity.getStartDateTime() != null && now.isBefore(entity.getStartDateTime())) {
            calculatedStatus = StageStatus.UPCOMING;
        } else if (entity.getEndDateTime() != null && now.isAfter(entity.getEndDateTime())) {
            calculatedStatus = StageStatus.COMPLETED;
        } else {
            calculatedStatus = StageStatus.ACTIVE;
        }

        System.out.println("======================================");
        System.out.println("DEBUG LOG: Stage ID = " + entity.getId());
        System.out.println("DEBUG LOG: Stage Name = " + entity.getName());
        System.out.println("DEBUG LOG: Start Date = " + (entity.getStartDateTime() != null ? entity.getStartDateTime().toLocalDate() : "null"));
        System.out.println("DEBUG LOG: End Date = " + (entity.getEndDateTime() != null ? entity.getEndDateTime().toLocalDate() : "null"));
        System.out.println("DEBUG LOG: Start Time = " + (entity.getStartDateTime() != null ? entity.getStartDateTime().toLocalTime() : "null"));
        System.out.println("DEBUG LOG: End Time = " + (entity.getEndDateTime() != null ? entity.getEndDateTime().toLocalTime() : "null"));
        System.out.println("DEBUG LOG: Current Time = " + now);
        System.out.println("DEBUG LOG: Calculated Status = " + calculatedStatus);
        
        if (entity.getStartDateTime() == null && entity.getEndDateTime() == null) {
            System.out.println("DEBUG LOG: Reason = No valid scheduling information exists. Defaulting to ACTIVE.");
        } else if (calculatedStatus == StageStatus.UPCOMING) {
            System.out.println("DEBUG LOG: Reason = Current time is before the configured Start Date.");
        } else if (calculatedStatus == StageStatus.COMPLETED) {
            System.out.println("DEBUG LOG: Reason = Current time is after the configured End Date.");
        } else {
            System.out.println("DEBUG LOG: Reason = Current time satisfies the ACTIVE condition (after start, before end).");
        }
        System.out.println("======================================");

        response.setStatus(calculatedStatus);
        response.setIsActive(calculatedStatus == StageStatus.ACTIVE);
        response.setIsUpcoming(calculatedStatus == StageStatus.UPCOMING);
        response.setIsCompleted(calculatedStatus == StageStatus.COMPLETED);
        if (calculatedStatus == StageStatus.UPCOMING && entity.getStartDateTime() != null) {
            long days = ChronoUnit.DAYS.between(now, entity.getStartDateTime());
            long hours = ChronoUnit.HOURS.between(now, entity.getStartDateTime()) % 24;
            String cd = "Starts in " + days + "d " + hours + "h";
            response.setCountdown(cd);
            response.setRemainingTime(cd);
        } else if (calculatedStatus == StageStatus.ACTIVE && entity.getEndDateTime() != null) {
            long days = ChronoUnit.DAYS.between(now, entity.getEndDateTime());
            long hours = ChronoUnit.HOURS.between(now, entity.getEndDateTime()) % 24;
            String cd = "Ends in " + days + "d " + hours + "h";
            response.setCountdown(cd);
            response.setRemainingTime(cd);
        } else {
            response.setCountdown("Ended");
            response.setRemainingTime("Ended");
        }

        return response;
    }
}
