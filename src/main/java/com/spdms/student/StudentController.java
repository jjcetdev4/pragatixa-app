package com.spdms.student;

import com.spdms.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.spdms.entity.DisciplineLog;
import java.util.List;

/**
 * Student management REST endpoints.
 */
@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Students", description = "Student management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /** POST /api/v1/students – Add new student (Admin or Teacher only) */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Add Student", description = "Creates a new student record. Requires ADMIN or TEACHER role.")
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(
            @Valid @RequestBody CreateStudentRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ApiResponse<StudentResponse> response = studentService.createStudent(request, username);
        return response.isSuccess()
            ? ResponseEntity.status(HttpStatus.CREATED).body(response)
            : ResponseEntity.badRequest().body(response);
    }

    /** GET /api/v1/students – List all students */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get All Students", description = "Returns paginated list of all students.")
    public ResponseEntity<ApiResponse<Page<StudentResponse>>> getAllStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fullName") String sortBy) {
        return ResponseEntity.ok(studentService.getAllStudents(page, size, sortBy));
    }

    /** GET /api/v1/students/{id} – Get student by ID */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get Student by ID")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudentById(@PathVariable Long id) {
        ApiResponse<StudentResponse> response = studentService.getStudentById(id);
        return response.isSuccess()
            ? ResponseEntity.ok(response)
            : ResponseEntity.status(404).body(response);
    }

    /** GET /api/v1/students/search?keyword= – Search students */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Search Students", description = "Search by name, student ID, or email.")
    public ResponseEntity<ApiResponse<Page<StudentResponse>>> searchStudents(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(studentService.searchStudents(keyword, page, size));
    }

    /** DELETE /api/v1/students/{id} – Delete student (Admin only) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete Student", description = "Deletes a student record. Requires ADMIN role.")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable Long id) {
        ApiResponse<Void> response = studentService.deleteStudent(id);
        return response.isSuccess()
            ? ResponseEntity.ok(response)
            : ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /** PUT /api/v1/students/{id} – Update student */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Update Student", description = "Updates student profile details. Requires ADMIN or TEACHER role.")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStudentRequest request) {
        ApiResponse<StudentResponse> response = studentService.updateStudent(id, request);
        return response.isSuccess()
            ? ResponseEntity.ok(response)
            : ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /** POST /api/v1/students/bulk-parse – Parse Excel file and return student preview */
    @PostMapping(value = "/bulk-parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Bulk Parse Students Spreadsheet", description = "Parses Excel and returns JSON preview list of student records without saving. Requires ADMIN or TEACHER role.")
    public ResponseEntity<ApiResponse<List<CreateStudentRequest>>> bulkParseStudents(@RequestParam("file") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ApiResponse<List<CreateStudentRequest>> response = studentService.bulkParse(file, username);
        return response.isSuccess()
            ? ResponseEntity.ok(response)
            : ResponseEntity.badRequest().body(response);
    }

    /** POST /api/v1/students/bulk-import – Confirm import of selected students list */
    @PostMapping("/bulk-import")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Bulk Import Selected Students", description = "Saves selected list of parsed student records into the database. Requires ADMIN or TEACHER role.")
    public ResponseEntity<ApiResponse<String>> bulkImportStudents(@RequestBody List<CreateStudentRequest> requests) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ApiResponse<String> response = studentService.bulkImport(requests, username);
        return response.isSuccess()
            ? ResponseEntity.ok(response)
            : ResponseEntity.badRequest().body(response);
    }

    /** POST /api/v1/students/{id}/adjust-points – Add or deduct points */
    @PostMapping("/{id}/adjust-points")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Adjust Student Points", description = "Adds or deducts points for a student. Checks activity-faculty assignments.")
    public ResponseEntity<ApiResponse<StudentResponse>> adjustPoints(
            @PathVariable Long id,
            @Valid @RequestBody PointAdjustmentRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ApiResponse<StudentResponse> response = studentService.adjustPoints(id, request, username);
        return response.isSuccess()
            ? ResponseEntity.ok(response)
            : ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /** GET /api/v1/students/{id}/discipline-logs – Get discipline logs/history */
    @GetMapping("/{id}/discipline-logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Get Discipline Logs", description = "Fetch history logs of points adjustments for a student.")
    public ResponseEntity<ApiResponse<List<DisciplineLog>>> getDisciplineLogs(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getDisciplineLogs(id));
    }

    /** GET /api/v1/students/department-performance – Get overall/year performance of HOD department */
    @GetMapping("/department-performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get Department Performance Report", description = "Returns overall and year-wise average discipline scores. Requires sub-role HOD.")
    public ResponseEntity<ApiResponse<DepartmentPerformanceResponse>> getDepartmentPerformance() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ApiResponse<DepartmentPerformanceResponse> response = studentService.getDepartmentPerformance(username);
        return response.isSuccess()
            ? ResponseEntity.ok(response)
            : ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
}
