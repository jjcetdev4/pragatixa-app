package jjcet.PragatiX.modules.analytics.controller;

import jjcet.PragatiX.modules.analytics.dto.InterventionStatusDto;
import jjcet.PragatiX.modules.analytics.dto.InterventionRiskDto;
import jjcet.PragatiX.modules.analytics.domain.service.risk.RiskClassificationService;
import jjcet.PragatiX.modules.analytics.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// Legacy controller superseded by api.controller.AnalyticsController
// @RestController
@RequestMapping("/api/v1/analytics/intervention")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_SUPERADMIN', 'ADMIN', 'SUPER_ADMIN', 'SUPERADMIN', 'ROLE_HOD', 'HOD', 'ROLE_FACULTY', 'FACULTY', 'ROLE_STUDENT', 'STUDENT')")
public class InterventionAnalyticsController {

    private final AnalyticsService analyticsService;
    private final RiskClassificationService riskClassificationService;

    public InterventionAnalyticsController(AnalyticsService analyticsService, RiskClassificationService riskClassificationService) {
        this.analyticsService = analyticsService;
        this.riskClassificationService = riskClassificationService;
    }

    @GetMapping
    public ResponseEntity<?> getInterventionRisks(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null || endDate == null) {
            startDate = LocalDate.of(2000, 1, 1);
            endDate = LocalDate.of(2030, 12, 31);
        }

        // Get raw risk data from analytics service
        List<InterventionRiskDto> rawData = analyticsService.getInterventionRisks(yearNo, departmentId, stage, startDate, endDate);

        // Apply risk classification to determine status levels
        long totalStudents = 0;
        long highCount = 0;
        long mediumCount = 0;
        long lowCount = 0;
        List<InterventionRiskDto> riskDtos = new java.util.ArrayList<>();

        for (InterventionRiskDto risk : rawData) {
            totalStudents++;
            if ("HIGH".equals(risk.riskLevel())) {
                highCount++;
            } else if ("MEDIUM".equals(risk.riskLevel())) {
                mediumCount++;
            } else {
                lowCount++;
            }
            riskDtos.add(risk);
        }

        InterventionStatusDto status = new InterventionStatusDto(totalStudents, highCount, mediumCount, lowCount, riskDtos);

        return ResponseEntity.ok(status);
    }
}