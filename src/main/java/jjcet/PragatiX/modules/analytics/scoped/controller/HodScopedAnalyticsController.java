package jjcet.PragatiX.modules.analytics.scoped.controller;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.analytics.dto.AllInOneDashboardDto;
import jjcet.PragatiX.modules.analytics.scoped.service.HodScopedAnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/hod/analytics/scoped")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'ROLE_TEACHER', 'TEACHER', 'ROLE_HOD', 'HOD')")
public class HodScopedAnalyticsController {

    private final HodScopedAnalyticsService hodScopedAnalyticsService;

    public HodScopedAnalyticsController(HodScopedAnalyticsService hodScopedAnalyticsService) {
        this.hodScopedAnalyticsService = hodScopedAnalyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AllInOneDashboardDto>> getScopedDashboard(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        AllInOneDashboardDto data = hodScopedAnalyticsService.getScopedDashboard(yearNo, stage, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
