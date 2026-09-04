package jjcet.PragatiX.modules.admin.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.activity.dto.request.ActivityStageRequest;
import jjcet.PragatiX.modules.activity.dto.response.ActivityStageResponse;
import jjcet.PragatiX.modules.activity.service.ActivityStageService;
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
import jjcet.PragatiX.modules.admin.service.*;
import jjcet.PragatiX.modules.admin.mapper.*;

import jjcet.PragatiX.modules.activity.dto.request.EvaluatePromotionsRequest;
import jjcet.PragatiX.modules.activity.repository.ActivityStageRepository;
import jjcet.PragatiX.repository.AcademicYearRepository;

@Service
public class AdminStageService {
    private static final Logger log = LoggerFactory.getLogger(AdminStageService.class);

    private final ActivityStageService activityStageService;
    private final jjcet.PragatiX.modules.student.repository.StudentRepository studentRepository;
    private final jjcet.PragatiX.modules.student.service.XpEngineService xpEngineService;
    private final ActivityStageRepository activityStageRepository;
    private final AcademicYearRepository academicYearRepository;

    public AdminStageService(ActivityStageService activityStageService,
            jjcet.PragatiX.modules.student.repository.StudentRepository studentRepository,
            jjcet.PragatiX.modules.student.service.XpEngineService xpEngineService,
            ActivityStageRepository activityStageRepository,
            AcademicYearRepository academicYearRepository) {
        this.activityStageService = activityStageService;
        this.studentRepository = studentRepository;
        this.xpEngineService = xpEngineService;
        this.activityStageRepository = activityStageRepository;
        this.academicYearRepository = academicYearRepository;
    }

    public ResponseEntity<ApiResponse<Void>> evaluatePromotions() {
        return evaluatePromotions(null);
    }

    public ResponseEntity<ApiResponse<Void>> evaluatePromotions(EvaluatePromotionsRequest request) {
        if (request == null || request.getStageId() == null || request.getStageId() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid stage ID"));
        }
        if (!activityStageRepository.existsById(request.getStageId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Stage not found"));
        }

        if (request.getAcademicYearId() == null || request.getAcademicYearId() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid academic year ID"));
        }
        if (!academicYearRepository.existsById(request.getAcademicYearId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Academic year not found"));
        }

        List<jjcet.PragatiX.entity.Student> activeStudents = studentRepository.findByActiveTrue();
        int evaluated = 0;
        for (jjcet.PragatiX.entity.Student student : activeStudents) {
            try {
                xpEngineService.evaluateStagePromotion(student);
                evaluated++;
            } catch (Exception e) {
                // Log and continue to next student to avoid halting batch promotion
                System.err.println("Error evaluating promotion for student " + student.getId() + ": " + e.getMessage());
            }
        }
        return ResponseEntity
                .ok(ApiResponse.ok("Evaluated stage promotions for " + evaluated + " active students.", null));
    }

    public ResponseEntity<ApiResponse<List<ActivityStageResponse>>> getAllStages(
            jjcet.PragatiX.enums.AcademicYear academicYear) {
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
        return ResponseEntity.ok(ApiResponse.ok("Stage moved to Recycle Bin successfully", null));
    }
}
