package com.pragatix.modules.analytics.controller;

import com.pragatix.modules.analytics.dto.*;
import com.pragatix.modules.analytics.service.XpAnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics/xp")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_SUPERADMIN', 'ADMIN', 'SUPER_ADMIN', 'SUPERADMIN')")
public class XpAnalyticsController {

    private final XpAnalyticsService service;

    public XpAnalyticsController(XpAnalyticsService service) {
        this.service = service;
    }




    @GetMapping("/award-penalty")
    public ResponseEntity<List<XpAwardVsPenaltyDTO>> getAwardVsPenalty(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.getAwardVsPenalty(academicYear, departmentId, stageId, sectionId, startDate, endDate));
    }

    @GetMapping("/departments")
    public ResponseEntity<List<GroupedXpDTO>> getDepartmentRanking(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.getDepartmentRanking(academicYear, stageId, startDate, endDate));
    }

    @GetMapping("/sections")
    public ResponseEntity<List<GroupedXpDTO>> getSectionRanking(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.getSectionRanking(academicYear, departmentId, stageId, startDate, endDate));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<List<XpHeatmapDTO>> getMonthlyHeatmap(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.getMonthlyHeatmap(academicYear, departmentId, stageId, sectionId, startDate, endDate));
    }

    @GetMapping("/top-performers")
    public ResponseEntity<List<XpTopPerformerDTO>> getTopPerformers(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.getTopPerformers(academicYear, departmentId, stageId, sectionId, startDate, endDate));
    }

    @GetMapping("/low-xp")
    public ResponseEntity<List<LowXpStudentDTO>> getLowXpStudents(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "20") Long threshold) {
        return ResponseEntity.ok(service.getLowXpStudents(academicYear, departmentId, stageId, sectionId, startDate, endDate, threshold));
    }

    @GetMapping("/activities")
    public ResponseEntity<List<ActivityXpContributionDTO>> getActivityXpContribution(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(service.getActivityXpContribution(academicYear, departmentId, stageId, sectionId, startDate, endDate, category));
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getXpHistory(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String activityName,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<XpHistoryDTO> data = service.getXpHistory(academicYear, departmentId, stageId, sectionId, startDate, endDate, activityName, type, size, page * size);
        long total = service.getXpHistoryCount(academicYear, departmentId, stageId, sectionId, startDate, endDate, activityName, type);
        
        return ResponseEntity.ok(Map.of(
            "content", data,
            "totalElements", total,
            "totalPages", (int) Math.ceil((double) total / size),
            "currentPage", page
        ));
    }

    @GetMapping("/export-history")
    public ResponseEntity<byte[]> exportHistory(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String activityName,
            @RequestParam(required = false) String type) {
            
        byte[] data = service.exportXpHistory(academicYear, departmentId, stageId, sectionId, startDate, endDate, activityName, type);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"XP_History_Report.xlsx\"");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
