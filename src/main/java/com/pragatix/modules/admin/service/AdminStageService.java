package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.activity.dto.request.ActivityStageRequest;
import com.pragatix.modules.activity.dto.response.ActivityStageResponse;
import com.pragatix.modules.activity.service.ActivityStageService;
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
import com.pragatix.modules.admin.service.*;
import com.pragatix.modules.admin.mapper.*;

@Service
public class AdminStageService {
    private static final Logger log = LoggerFactory.getLogger(AdminStageService.class);

    private final ActivityStageService activityStageService;
    private final com.pragatix.modules.student.repository.StudentRepository studentRepository;
    private final com.pragatix.modules.student.service.XpEngineService xpEngineService;

    public AdminStageService(ActivityStageService activityStageService,
            com.pragatix.modules.student.repository.StudentRepository studentRepository,
            com.pragatix.modules.student.service.XpEngineService xpEngineService) {
        this.activityStageService = activityStageService;
        this.studentRepository = studentRepository;
        this.xpEngineService = xpEngineService;
    }

    public ResponseEntity<ApiResponse<Void>> evaluatePromotions() {
        List<com.pragatix.entity.Student> activeStudents = studentRepository.findByActiveTrue();
        int evaluated = 0;
        for (com.pragatix.entity.Student student : activeStudents) {
            xpEngineService.evaluateStagePromotion(student);
            evaluated++;
        }
        return ResponseEntity
                .ok(ApiResponse.ok("Evaluated stage promotions for " + evaluated + " active students.", null));
    }

    public ResponseEntity<ApiResponse<List<ActivityStageResponse>>> getAllStages(
            com.pragatix.enums.AcademicYear academicYear) {
        List<ActivityStageResponse> stages = activityStageService.getAllStages(academicYear);
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
