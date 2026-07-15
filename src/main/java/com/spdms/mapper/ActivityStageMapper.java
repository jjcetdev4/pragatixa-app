package com.spdms.mapper;

import com.spdms.dto.ActivityStageRequest;
import com.spdms.dto.ActivityStageResponse;
import com.spdms.entity.ActivityStage;
import com.spdms.enums.StageStatus;
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
                .status(calculateStatus(request.getStartDateTime(), request.getEndDateTime()))
                .build();
    }

    private StageStatus calculateStatus(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return StageStatus.UPCOMING;
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(start)) {
            return StageStatus.UPCOMING;
        } else if (!now.isBefore(start) && now.isBefore(end)) {
            return StageStatus.ACTIVE;
        } else {
            return StageStatus.COMPLETED;
        }
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
        entity.setStatus(calculateStatus(request.getStartDateTime(), request.getEndDateTime()));
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
        
        // Dynamically calculate status against current server time
        StageStatus calculatedStatus = calculateStatus(entity.getStartDateTime(), entity.getEndDateTime());
        response.setStatus(calculatedStatus);
        response.setIsActive(calculatedStatus == StageStatus.ACTIVE);
        response.setIsUpcoming(calculatedStatus == StageStatus.UPCOMING);
        response.setIsCompleted(calculatedStatus == StageStatus.COMPLETED);
        
        LocalDateTime now = LocalDateTime.now();
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
