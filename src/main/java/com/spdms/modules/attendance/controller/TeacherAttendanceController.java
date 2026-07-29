package com.spdms.modules.attendance.controller;

import com.spdms.common.response.ApiResponse;
import com.spdms.modules.attendance.dto.request.SaveAttendanceRequest;
import com.spdms.modules.attendance.dto.response.StudentAttendanceListItemResponse;
import com.spdms.modules.attendance.service.TeacherAttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/teacher/attendance")
public class TeacherAttendanceController {

    @Autowired
    private TeacherAttendanceService attendanceService;

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<StudentAttendanceListItemResponse>>> getStudentsWithAttendance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer period,
            @RequestParam Long yearId,
            @RequestParam Long departmentId,
            @RequestParam(required = false) Long sectionId) {
            
        List<StudentAttendanceListItemResponse> students = attendanceService.getStudentListWithAttendance(date, period, yearId, departmentId, sectionId);
        return ResponseEntity.ok(ApiResponse.ok(students));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<String>> saveAttendance(
            Principal principal,
            @RequestBody SaveAttendanceRequest request) {
            
        attendanceService.saveAttendance(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok("Attendance saved successfully"));
    }
}
