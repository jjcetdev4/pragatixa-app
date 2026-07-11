package com.spdms.mapper;

import com.spdms.dto.ActivityStageRequest;
import com.spdms.dto.ActivityStageResponse;
import com.spdms.entity.ActivityStage;
import org.springframework.stereotype.Component;

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
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .displayOrder(request.getDisplayOrder())
                .isActive(request.isActive())
                .build();
    }

    public void updateEntity(ActivityStageRequest request, ActivityStage entity) {
        if (request == null || entity == null) {
            return;
        }
        entity.setName(request.getName());
        entity.setStageName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setDisplayOrder(request.getDisplayOrder());
        entity.setIsActive(request.isActive());
    }

    public ActivityStageResponse toResponse(ActivityStage entity) {
        if (entity == null) {
            return null;
        }
        ActivityStageResponse response = new ActivityStageResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setStartDate(entity.getStartDate());
        response.setEndDate(entity.getEndDate());
        response.setDisplayOrder(entity.getDisplayOrder());
        response.setIsActive(entity.isActive());
        return response;
    }
}
