package jjcet.PragatiX.modules.analytics.scoped.controller;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.analytics.dto.AllInOneDashboardDto;
import jjcet.PragatiX.modules.analytics.scoped.service.CaptainScopedAnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/captain/analytics/scoped")
@PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'STUDENT', 'ROLE_CAPTAIN', 'CAPTAIN', 'ROLE_ADMIN', 'ADMIN', 'ROLE_SUPER_ADMIN', 'SUPER_ADMIN', 'ROLE_SUPERADMIN', 'SUPERADMIN')")
public class CaptainScopedAnalyticsController {

    private final CaptainScopedAnalyticsService captainScopedAnalyticsService;

    public CaptainScopedAnalyticsController(CaptainScopedAnalyticsService captainScopedAnalyticsService) {
        this.captainScopedAnalyticsService = captainScopedAnalyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AllInOneDashboardDto>> getScopedDashboard(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        AllInOneDashboardDto data = captainScopedAnalyticsService.getScopedDashboard(yearNo, stage, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
