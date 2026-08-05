package com.pragatix.modules.student.controller;

import com.pragatix.modules.student.service.StudentService;
import com.pragatix.modules.student.service.StudentStageFacade;

import com.pragatix.dto.*;
import com.pragatix.modules.activity.dto.request.*;
import com.pragatix.modules.activity.dto.response.*;
import com.pragatix.modules.student.dto.request.*;
import com.pragatix.modules.student.dto.response.*;
import com.pragatix.common.response.ApiResponse;
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
import com.pragatix.entity.DisciplineLog;
import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Students", description = "Student management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class StudentController {

    private final StudentService studentService;
    private final StudentStageFacade studentStageFacade;
    private final com.pragatix.modules.authentication.security.StudentAuthResolver studentAuthResolver;
    private final com.pragatix.modules.activity.repository.ActivityRepository activityRepository;
    private final com.pragatix.modules.activity.service.ActivityStreakService activityStreakService;

    public StudentController(StudentService studentService,
            StudentStageFacade studentStageFacade,
            com.pragatix.modules.authentication.security.StudentAuthResolver studentAuthResolver,
            com.pragatix.modules.activity.repository.ActivityRepository activityRepository,
            com.pragatix.modules.activity.service.ActivityStreakService activityStreakService) {
        this.studentService = studentService;
        this.studentStageFacade = studentStageFacade;
        this.studentAuthResolver = studentAuthResolver;
        this.activityRepository = activityRepository;
        this.activityStreakService = activityStreakService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Add Student", description = "Creates a new student record. Requires ADMIN or TEACHER role.")
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(
            @Valid @RequestBody CreateStudentRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ApiResponse<StudentResponse> response = studentService.createStudent(request, username);
        return response.isSuccess() ? ResponseEntity.status(HttpStatus.CREATED).body(response)
                : ResponseEntity.badRequest().body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Get All Students", description = "Returns paginated list of all students.")
    public ResponseEntity<ApiResponse<Page<StudentResponse>>> getAllStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String section) {
        return ResponseEntity.ok(studentService.getAllStudents(page, size, sortBy));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get Student by ID")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudentById(@PathVariable Long id) {
        ApiResponse<StudentResponse> response = studentService.getStudentById(id);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.status(404).body(response);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Search Students", description = "Search by name, student ID, or email.")
    public ResponseEntity<ApiResponse<Page<StudentResponse>>> searchStudents(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(studentService.searchStudents(keyword, page, size));
    }

    @GetMapping("/team-member-search")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Smart search for team members", description = "Search active students by name, reg_no, or spr_no for team selection.")
    public ResponseEntity<ApiResponse<java.util.List<com.pragatix.modules.student.dto.response.StudentSearchDTO>>> searchActiveStudentsForTeam(
            @RequestParam String keyword) {
        return ResponseEntity.ok(studentService.searchActiveStudentsForTeam(keyword));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Delete Student", description = "Deletes a student record. Requires ADMIN or TEACHER role.")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable Long id) {
        ApiResponse<Void> response = studentService.deleteStudent(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage() != null && response.getMessage().contains("authorized")) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(response);
        } else {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Update Student", description = "Updates student profile details. Requires ADMIN or TEACHER role.")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStudentRequest request) {
        ApiResponse<StudentResponse> response = studentService.updateStudent(id, request);
        return response.isSuccess() ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @PostMapping(value = "/bulk-parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Bulk Parse Students Spreadsheet", description = "Parses Excel and returns JSON preview list of student records without saving. Requires ADMIN or TEACHER role.")
    public ResponseEntity<ApiResponse<List<CreateStudentRequest>>> bulkParseStudents(
            @RequestParam("file") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ApiResponse<List<CreateStudentRequest>> response = studentService.bulkParse(file, username);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/bulk-import")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Bulk Import Selected Students", description = "Saves selected list of parsed student records into the database. Requires ADMIN or TEACHER role.")
    public ResponseEntity<ApiResponse<String>> bulkImportStudents(@RequestBody List<CreateStudentRequest> requests) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ApiResponse<String> response = studentService.bulkImport(requests, username);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/{id}/adjust-points")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Adjust Student Points", description = "Adds or deducts points for a student. Checks activity-faculty assignments.")
    public ResponseEntity<ApiResponse<StudentResponse>> adjustPoints(
            @PathVariable Long id,
            @Valid @RequestBody PointAdjustmentRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ApiResponse<StudentResponse> response = studentService.adjustPoints(id, request, username);
        return response.isSuccess() ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @GetMapping("/{id}/discipline-logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Get Discipline Logs", description = "Fetch history logs of points adjustments for a student.")
    public ResponseEntity<ApiResponse<List<DisciplineLog>>> getDisciplineLogs(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getDisciplineLogs(id));
    }

    @GetMapping("/department-performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get Department Performance Report", description = "Returns overall and year-wise average discipline scores. Requires sub-role HOD.")
    public ResponseEntity<ApiResponse<DepartmentPerformanceResponse>> getDepartmentPerformance() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ApiResponse<DepartmentPerformanceResponse> response = studentService.getDepartmentPerformance(username);
        return response.isSuccess() ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @PostMapping("/{id}/make-captain")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Promote Student to Team Captain", description = "Sets the student as the Captain of their assigned team.")
    public ResponseEntity<ApiResponse<Void>> promoteToTeamCaptain(@PathVariable Long id) {
        ApiResponse<Void> response = studentService.promoteToTeamCaptain(id);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/{id}/remove-captain")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Remove Student from Team Captain status", description = "Removes the student as the Captain of their assigned team.")
    public ResponseEntity<ApiResponse<Void>> removeTeamCaptain(@PathVariable Long id) {
        ApiResponse<Void> response = studentService.removeTeamCaptain(id);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/stages")
    @PreAuthorize("hasAnyRole('STUDENT')")
    @Operation(summary = "Get Stages Configured for Student", description = "Returns list of stages enriched with specific user validation (unlock rules).")
    public ResponseEntity<?> getStudentStages() {
        com.pragatix.entity.Student student = studentAuthResolver.getLoggedInStudent();
        return studentStageFacade.getStudentStages(student);
    }

    @GetMapping("/subgroups/{subgroupId}/activities")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'TEACHER')")
    @Operation(summary = "Get all activities of a subgroup")
    public ResponseEntity<ApiResponse<List<com.pragatix.entity.Activity>>> getActivitiesBySubgroup(
            @PathVariable Long subgroupId) {
        List<com.pragatix.entity.Activity> activities = activityRepository.findBySubgroupId(subgroupId);
        return ResponseEntity.ok(ApiResponse.ok("Activities fetched successfully", activities));
    }

    @GetMapping("/me/activity-streaks")
    @PreAuthorize("hasAnyRole('STUDENT')")
    @Operation(summary = "Get Student Activity Streaks", description = "Returns all activity streaks for the logged-in student.")
    public ResponseEntity<ApiResponse<List<StudentActivityStreakDTO>>> getMyActivityStreaks() {
        com.pragatix.entity.Student student = studentAuthResolver.getLoggedInStudent();
        List<com.pragatix.entity.StudentActivityStreak> streaks = activityStreakService.getStudentActivityStreaks(student.getId());
        List<StudentActivityStreakDTO> dtos = streaks.stream()
                .map(s -> new StudentActivityStreakDTO(
                        s.getActivity().getId(),
                        s.getActivity().getName(),
                        s.getCurrentStreak(),
                        s.getLongestStreak(),
                        s.getLastCompletedDate()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Activity streaks fetched successfully", dtos));
    }
}
