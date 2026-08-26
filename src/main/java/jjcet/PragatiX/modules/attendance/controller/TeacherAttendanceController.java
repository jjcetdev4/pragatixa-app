package jjcet.PragatiX.modules.attendance.controller;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.attendance.dto.request.SaveAttendanceRequest;
import jjcet.PragatiX.modules.attendance.dto.response.StudentAttendanceListItemResponse;
import jjcet.PragatiX.modules.attendance.service.TeacherAttendanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/teacher/attendance")
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPERADMIN', 'SUPER_ADMIN', 'CLASS_COORDINATOR', 'HOD')")
public class TeacherAttendanceController {

    @Autowired
    private TeacherAttendanceService attendanceService;

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<StudentAttendanceListItemResponse>>> getStudentsWithAttendance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer period,
            @RequestParam(required = false) Long yearId,
            @RequestParam Long departmentId,
            @RequestParam(required = false) Long sectionId) {

        List<StudentAttendanceListItemResponse> students = attendanceService.getStudentListWithAttendance(date, period,
                yearId, departmentId, sectionId);
        return ResponseEntity.ok(ApiResponse.ok(students));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<String>> saveAttendance(
            Principal principal,
            @Valid @RequestBody SaveAttendanceRequest request) {

        attendanceService.saveAttendance(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok("Attendance saved successfully"));
    }

    @GetMapping("/next-period")
    public ResponseEntity<ApiResponse<Integer>> getNextPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long yearId,
            @RequestParam Long departmentId,
            @RequestParam(required = false) Long sectionId) {

        Integer nextPeriod = attendanceService.getNextPeriod(date, yearId, departmentId, sectionId);
        return ResponseEntity.ok(ApiResponse.ok(nextPeriod));
    }
}
