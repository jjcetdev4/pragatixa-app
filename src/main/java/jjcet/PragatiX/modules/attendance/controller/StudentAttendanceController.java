package jjcet.PragatiX.modules.attendance.controller;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.attendance.dto.response.StudentAttendanceHistoryResponse;
import jjcet.PragatiX.modules.attendance.dto.response.StudentAttendanceSummaryResponse;
import jjcet.PragatiX.modules.attendance.service.StudentAttendanceService;
import jjcet.PragatiX.modules.authentication.security.StudentAuthResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

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
    public ResponseEntity<ApiResponse<List<StudentAttendanceHistoryResponse>>> getHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        Long studentId = studentAuthResolver.getLoggedInStudent().getId();
        List<StudentAttendanceHistoryResponse> history = attendanceService.getHistory(studentId, date);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}
