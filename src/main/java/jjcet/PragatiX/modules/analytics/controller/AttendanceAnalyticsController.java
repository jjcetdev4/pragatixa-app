package jjcet.PragatiX.modules.analytics.controller;

import jjcet.PragatiX.modules.analytics.dto.*;
import jjcet.PragatiX.modules.analytics.service.attendance.AttendanceAnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

// Legacy controller superseded by api.controller.AnalyticsController
// @RestController
@RequestMapping("/api/v1/analytics/attendance")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_SUPERADMIN', 'ADMIN', 'SUPER_ADMIN', 'SUPERADMIN', 'ROLE_HOD', 'HOD', 'ROLE_FACULTY', 'FACULTY', 'ROLE_STUDENT', 'STUDENT')")
public class AttendanceAnalyticsController {

    private final AttendanceAnalyticsService service;

    public AttendanceAnalyticsController(AttendanceAnalyticsService service) {
        this.service = service;
    }

    @RequestMapping(value = "/overview", method = RequestMethod.GET)
    public ResponseEntity<AnalyticsOverviewDTO> getOverview(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period) {
        return ResponseEntity.ok(service.getOverview(academicYear, departmentId, stageId, sectionId, startDate, endDate, period));
    }

    @RequestMapping(value = "/trend", method = RequestMethod.GET)
    public ResponseEntity<List<AttendanceTrendDTO>> getTrend(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period) {
        return ResponseEntity.ok(service.getTrend(academicYear, departmentId, stageId, sectionId, startDate, endDate, period));
    }

    @RequestMapping(value = "/distribution", method = RequestMethod.GET)
    public ResponseEntity<AttendanceDistributionDTO> getDistribution(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period) {
        return ResponseEntity.ok(service.getDistribution(academicYear, departmentId, stageId, sectionId, startDate, endDate, period));
    }

    @RequestMapping(value = "/departments", method = RequestMethod.GET)
    public ResponseEntity<List<GroupedAttendanceDTO>> getDepartmentWise(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period) {
        return ResponseEntity.ok(service.getDepartmentWiseAttendance(academicYear, startDate, endDate, period));
    }

    @RequestMapping(value = "/low-attendance", method = RequestMethod.GET)
    public ResponseEntity<List<LowAttendanceStudentDTO>> getLowAttendanceStudents(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period,
            @RequestParam(required = false, defaultValue = "75.0") Double threshold) {
        return ResponseEntity.ok(service.getLowAttendanceStudents(academicYear, departmentId, stageId, sectionId, startDate, endDate, period, threshold));
    }

    @RequestMapping(value = "/sections", method = RequestMethod.GET)
    public ResponseEntity<List<GroupedAttendanceDTO>> getSectionWise(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period) {
        return ResponseEntity.ok(service.getSectionWiseAttendance(academicYear, departmentId, stageId, startDate, endDate, period));
    }

    @RequestMapping(value = "/summary-table", method = RequestMethod.GET)
    public ResponseEntity<List<AttendanceSummaryRowDTO>> getSummaryTable(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period) {
        return ResponseEntity.ok(service.getSummaryTable(academicYear, departmentId, stageId, sectionId, startDate, endDate, period));
    }

    @RequestMapping(value = "/export", method = RequestMethod.GET)
    public ResponseEntity<byte[]> exportAttendanceReport(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period) {
        
        byte[] excelData = service.exportAttendanceReport(academicYear, departmentId, stageId, sectionId, startDate, endDate, period);
        
        String dateStr = (startDate != null) ? startDate.toString() : LocalDate.now().toString();
        String prefix = (departmentId != null) ? (sectionId != null ? "SectionReport_" : "DepartmentReport_") : "AttendanceReport_";
        String filename = prefix + dateStr + ".xlsx";

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        headers.add("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        
        return new ResponseEntity<>(excelData, headers, org.springframework.http.HttpStatus.OK);
    }
}
