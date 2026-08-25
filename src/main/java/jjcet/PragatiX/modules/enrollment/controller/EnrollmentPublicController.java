package jjcet.PragatiX.modules.enrollment.controller;

import jakarta.validation.Valid;
import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.enrollment.dto.CompleteEnrollmentRequestDto;
import jjcet.PragatiX.modules.enrollment.dto.CompleteEnrollmentResponseDto;
import jjcet.PragatiX.modules.enrollment.dto.EnrollmentStatusDto;
import jjcet.PragatiX.modules.enrollment.dto.PendingStudentDto;
import jjcet.PragatiX.modules.enrollment.service.EnrollmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/enrollment")
public class EnrollmentPublicController {

    private final EnrollmentService enrollmentService;

    public EnrollmentPublicController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<EnrollmentStatusDto>> getStatus() {
        boolean enabled = enrollmentService.isEnrollmentEnabled();
        return ResponseEntity.ok(ApiResponse.ok(new EnrollmentStatusDto(enabled)));
    }

    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingDepartments() {
        List<Map<String, Object>> list = enrollmentService.getPendingDepartments();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/alphabets")
    public ResponseEntity<ApiResponse<List<String>>> getPendingAlphabets(
            @RequestParam("departmentId") Long departmentId) {
        List<String> alphabets = enrollmentService.getPendingAlphabets(departmentId);
        return ResponseEntity.ok(ApiResponse.ok(alphabets));
    }

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<PendingStudentDto>>> getPendingStudents(
            @RequestParam("departmentId") Long departmentId,
            @RequestParam("letter") String letter) {
        List<PendingStudentDto> students = enrollmentService.getPendingStudents(departmentId, letter);
        return ResponseEntity.ok(ApiResponse.ok(students));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<CompleteEnrollmentResponseDto>> completeEnrollment(
            @Valid @RequestBody CompleteEnrollmentRequestDto request) {
        try {
            CompleteEnrollmentResponseDto response = enrollmentService.completeEnrollment(request.getEnrollmentId());
            return ResponseEntity.ok(ApiResponse.ok(response.getMessage(), response));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Enrollment failed: " + e.getMessage()));
        }
    }
}
