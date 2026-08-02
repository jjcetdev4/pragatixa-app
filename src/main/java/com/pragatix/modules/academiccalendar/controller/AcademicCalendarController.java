package com.pragatix.modules.academiccalendar.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.academiccalendar.dto.AcademicHolidayDto;
import com.pragatix.modules.academiccalendar.dto.AcademicMonthDto;
import com.pragatix.modules.academiccalendar.dto.AcademicWeekDto;
import com.pragatix.modules.academiccalendar.dto.AlternateWorkingDayDto;
import com.pragatix.modules.academiccalendar.service.AcademicCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academic-calendar")
@PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_SUPERADMIN', 'ROLE_ADMIN')")
public class AcademicCalendarController {

    @Autowired
    private AcademicCalendarService calendarService;

    @Autowired
    private com.pragatix.modules.authentication.security.AuthUtils authUtils;

    @GetMapping("/month")
    public ResponseEntity<ApiResponse<AcademicMonthDto>> getOrCreateMonth(
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam(required = false) com.pragatix.enums.AcademicYear academicYear) {
        
        com.pragatix.entity.User user = authUtils.getCurrentUser();
        com.pragatix.enums.AcademicYear targetYear;
        
        if (authUtils.isSuperAdmin(user)) {
            if (academicYear == null) {
                throw new IllegalArgumentException("Academic Year is required for Super Admin");
            }
            targetYear = academicYear;
        } else if (authUtils.isAdmin(user)) {
            targetYear = user.getAcademicYear();
            if (targetYear == null) {
                throw new IllegalArgumentException("Admin does not have an assigned Academic Year");
            }
        } else {
            throw new SecurityException("Unauthorized access to calendar configuration");
        }

        AcademicMonthDto result = calendarService.getOrCreateMonth(month, year, targetYear);
        return ResponseEntity.ok(ApiResponse.ok("Month retrieved successfully", result));
    }

    // --- Weeks ---
    @GetMapping("/month/{monthId}/weeks")
    public ResponseEntity<ApiResponse<List<AcademicWeekDto>>> getWeeks(@PathVariable Long monthId) {
        List<AcademicWeekDto> result = calendarService.getWeeksForMonth(monthId);
        return ResponseEntity.ok(ApiResponse.ok("Weeks retrieved successfully", result));
    }

    @PostMapping("/weeks")
    public ResponseEntity<ApiResponse<AcademicWeekDto>> addWeek(@RequestBody AcademicWeekDto dto) {
        try {
            AcademicWeekDto result = calendarService.addWeek(dto);
            return ResponseEntity.ok(ApiResponse.ok("Week added successfully", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/weeks/{id}")
    public ResponseEntity<ApiResponse<AcademicWeekDto>> updateWeek(@PathVariable Long id, @RequestBody AcademicWeekDto dto) {
        try {
            AcademicWeekDto result = calendarService.updateWeek(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Week updated successfully", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/weeks/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWeek(@PathVariable Long id) {
        calendarService.deleteWeek(id);
        return ResponseEntity.ok(ApiResponse.ok("Week deleted successfully", null));
    }

    // --- Holidays ---
    @GetMapping("/month/{monthId}/holidays")
    public ResponseEntity<ApiResponse<List<AcademicHolidayDto>>> getHolidays(@PathVariable Long monthId) {
        List<AcademicHolidayDto> result = calendarService.getHolidaysForMonth(monthId);
        return ResponseEntity.ok(ApiResponse.ok("Holidays retrieved successfully", result));
    }

    @PostMapping("/holidays")
    public ResponseEntity<ApiResponse<AcademicHolidayDto>> addHoliday(@RequestBody AcademicHolidayDto dto) {
        try {
            AcademicHolidayDto result = calendarService.addHoliday(dto);
            return ResponseEntity.ok(ApiResponse.ok("Holiday added successfully", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/holidays/{id}")
    public ResponseEntity<ApiResponse<AcademicHolidayDto>> updateHoliday(@PathVariable Long id, @RequestBody AcademicHolidayDto dto) {
        try {
            AcademicHolidayDto result = calendarService.updateHoliday(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Holiday updated successfully", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/holidays/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHoliday(@PathVariable Long id) {
        calendarService.deleteHoliday(id);
        return ResponseEntity.ok(ApiResponse.ok("Holiday deleted successfully", null));
    }

    // --- Alternate Working Days ---
    @GetMapping("/month/{monthId}/alternate-working-days")
    public ResponseEntity<ApiResponse<List<AlternateWorkingDayDto>>> getAlternateWorkingDays(@PathVariable Long monthId) {
        List<AlternateWorkingDayDto> result = calendarService.getAlternateWorkingDaysForMonth(monthId);
        return ResponseEntity.ok(ApiResponse.ok("Alternate working days retrieved successfully", result));
    }

    @PostMapping("/alternate-working-days")
    public ResponseEntity<ApiResponse<AlternateWorkingDayDto>> addAlternateWorkingDay(@RequestBody AlternateWorkingDayDto dto) {
        try {
            AlternateWorkingDayDto result = calendarService.addAlternateWorkingDay(dto);
            return ResponseEntity.ok(ApiResponse.ok("Alternate working day added successfully", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/alternate-working-days/{id}")
    public ResponseEntity<ApiResponse<AlternateWorkingDayDto>> updateAlternateWorkingDay(@PathVariable Long id, @RequestBody AlternateWorkingDayDto dto) {
        try {
            AlternateWorkingDayDto result = calendarService.updateAlternateWorkingDay(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Alternate working day updated successfully", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/alternate-working-days/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAlternateWorkingDay(@PathVariable Long id) {
        System.out.println("Removing Alternate Working Day ID: " + id);
        try {
            calendarService.deleteAlternateWorkingDay(id);
            System.out.println("Rows deleted: 1");
            return ResponseEntity.ok(ApiResponse.ok("Alternate working day deleted successfully", null));
        } catch (Exception e) {
            System.err.println("Failed to remove Alternate Working Day: " + e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Unable to remove Alternate Working Day. Please try again."));
        }
    }
}
