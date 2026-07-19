package com.spdms.modules.admin.service;

import com.spdms.common.response.ApiResponse;
import com.spdms.modules.activity.dto.request.ActivityStageRequest;
import com.spdms.modules.activity.dto.response.ActivityStageResponse;
import com.spdms.modules.activity.service.ActivityStageService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import com.spdms.modules.admin.service.*;
import com.spdms.modules.admin.mapper.*;

@Service
public class AdminStageService {
    private static final Logger log = LoggerFactory.getLogger(AdminStageService.class);

    private final ActivityStageService activityStageService;

    public AdminStageService(ActivityStageService activityStageService) {
        this.activityStageService = activityStageService;
    }

    public ResponseEntity<ApiResponse<List<ActivityStageResponse>>> getAllStages() {
        List<ActivityStageResponse> stages = activityStageService.getAllStages();
        return ResponseEntity.ok(ApiResponse.ok(stages));
    }

    public ResponseEntity<ApiResponse<ActivityStageResponse>> createStage(
            @Valid @RequestBody ActivityStageRequest request) {
        ActivityStageResponse saved = activityStageService.createStage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Stage created successfully", saved));
    }

    public ResponseEntity<ApiResponse<ActivityStageResponse>> getStage(@PathVariable Long id) {
        return activityStageService.getStageById(id)
                .map(stage -> ResponseEntity.ok(ApiResponse.ok(stage)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Stage not found")));
    }

    public ResponseEntity<ApiResponse<ActivityStageResponse>> editStage(
            @PathVariable Long id,
            @Valid @RequestBody ActivityStageRequest request) {
        ActivityStageResponse updated = activityStageService.updateStage(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Stage updated successfully", updated));
    }

    public ResponseEntity<ApiResponse<Map<String, Object>>> getStageReport(@PathVariable Long id) {
        try {
            Map<String, Object> report = activityStageService.getStageReport(id);
            return ResponseEntity.ok(ApiResponse.ok(report));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    public ResponseEntity<ApiResponse<Void>> deleteStage(@PathVariable Long id) {
        activityStageService.deleteStage(id);
        return ResponseEntity.ok(ApiResponse.ok("Stage deleted successfully", null));
    }

}
