package com.pragatix.modules.attendance.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.attendance.dto.response.StudentAttendanceHistoryResponse;
import com.pragatix.modules.attendance.dto.response.StudentAttendanceSummaryResponse;
import com.pragatix.modules.attendance.service.StudentAttendanceService;
import com.pragatix.modules.authentication.security.StudentAuthResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/attendance")
public class StudentAttendanceController {

    @Autowired
    private StudentAttendanceService attendanceService;

    @Autowired
    private StudentAuthResolver studentAuthResolver;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<StudentAttendanceSummaryResponse>> getSummary() {
        Long studentId = studentAuthResolver.getLoggedInStudent().getId();
        StudentAttendanceSummaryResponse summary = attendanceService.getSummary(studentId);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<StudentAttendanceHistoryResponse>>> getHistory() {
        Long studentId = studentAuthResolver.getLoggedInStudent().getId();
        List<StudentAttendanceHistoryResponse> history = attendanceService.getHistory(studentId);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}
