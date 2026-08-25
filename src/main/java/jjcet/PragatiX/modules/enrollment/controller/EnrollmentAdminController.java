package jjcet.PragatiX.modules.enrollment.controller;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.enrollment.dto.EnrollmentDto;
import jjcet.PragatiX.modules.enrollment.dto.EnrollmentImportResultDto;
import jjcet.PragatiX.modules.enrollment.dto.EnrollmentStatusDto;
import jjcet.PragatiX.modules.enrollment.service.EnrollmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/admin/enrollment")
public class EnrollmentAdminController {

    private final EnrollmentService enrollmentService;

    public EnrollmentAdminController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<EnrollmentStatusDto>> getStatus() {
        boolean enabled = enrollmentService.isEnrollmentEnabled();
        return ResponseEntity.ok(ApiResponse.ok(new EnrollmentStatusDto(enabled)));
    }

    @PutMapping("/status")
    public ResponseEntity<ApiResponse<EnrollmentStatusDto>> updateStatus(
            @RequestBody EnrollmentStatusDto dto,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "ADMIN";
        enrollmentService.setEnrollmentEnabled(dto.isEnabled(), username);
        return ResponseEntity.ok(ApiResponse.ok("Enrollment status updated to " + (dto.isEnabled() ? "ON" : "OFF"), dto));
    }

    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<java.util.List<java.util.Map<String, Object>>>> getDepartments() {
        java.util.List<java.util.Map<String, Object>> depts = enrollmentService.getMainStudentDepartments();
        return ResponseEntity.ok(ApiResponse.ok(depts));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] excelContent = enrollmentService.generateExcelTemplate();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Student_Enrollment_Template.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelContent);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EnrollmentImportResultDto>> importExcel(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : "ADMIN";
            EnrollmentImportResultDto result = enrollmentService.importEnrollmentExcel(file, username);
            String message = "Import complete: " + result.getImportedCount() + " imported, " + result.getSkippedCount() + " skipped.";
            return ResponseEntity.ok(ApiResponse.ok(message, result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to import Excel: " + e.getMessage()));
        }
    }

    @PostMapping("/single")
    public ResponseEntity<ApiResponse<EnrollmentDto>> createSingleEnrollment(
            @jakarta.validation.Valid @RequestBody jjcet.PragatiX.modules.enrollment.dto.SingleEnrollmentRequestDto dto,
            Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : "ADMIN";
            EnrollmentDto result = enrollmentService.createSingleEnrollment(dto, username);
            return ResponseEntity.ok(ApiResponse.ok("Student added to pending enrollment list successfully", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to add student: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EnrollmentDto>> updateEnrollment(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody jjcet.PragatiX.modules.enrollment.dto.SingleEnrollmentRequestDto dto,
            Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : "ADMIN";
            EnrollmentDto result = enrollmentService.updateEnrollment(id, dto, username);
            return ResponseEntity.ok(ApiResponse.ok("Student enrollment details updated successfully", result));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to update student: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEnrollment(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : "ADMIN";
            enrollmentService.deleteEnrollment(id, username);
            return ResponseEntity.ok(ApiResponse.ok("Student enrollment record deleted successfully", null));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to delete student: " + e.getMessage()));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<EnrollmentDto>>> getPendingList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<EnrollmentDto> pendingPage = enrollmentService.getPendingList(pageable, search, departmentId);
        return ResponseEntity.ok(ApiResponse.ok(pendingPage));
    }

    @GetMapping("/enrolled")
    public ResponseEntity<ApiResponse<Page<EnrollmentDto>>> getEnrolledList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("enrolledAt").descending());
        Page<EnrollmentDto> enrolledPage = enrollmentService.getEnrolledList(pageable, search, departmentId);
        return ResponseEntity.ok(ApiResponse.ok(enrolledPage));
    }
}
