package com.spdms.student;

import com.spdms.dto.*;
import com.spdms.common.response.ApiResponse;
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
    private final StageValidationService stageValidationService;
    private final com.spdms.admin.ActivityStageService activityStageService;
    private final com.spdms.modules.authentication.repository.UserRepository userRepository;
    private final com.spdms.repository.StudentRepository studentRepository;
    private final com.spdms.repository.ActivityRepository activityRepository;
    private final com.spdms.repository.DisciplineLogRepository disciplineLogRepository;
    private final com.spdms.repository.ActivityAssignmentRepository activityAssignmentRepository;
    private final com.spdms.repository.StudentActivityXpRepository studentActivityXpRepository;
    private final com.spdms.repository.XpTransactionRepository xpTransactionRepository;
    private final com.spdms.modules.authentication.security.StudentAuthResolver studentAuthResolver;

    public StudentController(StudentService studentService,
            StageValidationService stageValidationService,
            com.spdms.admin.ActivityStageService activityStageService,
            com.spdms.modules.authentication.repository.UserRepository userRepository,
            com.spdms.repository.StudentRepository studentRepository,
            com.spdms.repository.ActivityRepository activityRepository,
            com.spdms.repository.DisciplineLogRepository disciplineLogRepository,
            com.spdms.repository.ActivityAssignmentRepository activityAssignmentRepository,
            com.spdms.repository.StudentActivityXpRepository studentActivityXpRepository,
            com.spdms.repository.XpTransactionRepository xpTransactionRepository,
            com.spdms.modules.authentication.security.StudentAuthResolver studentAuthResolver) {
        this.studentService = studentService;
        this.stageValidationService = stageValidationService;
        this.activityStageService = activityStageService;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.activityRepository = activityRepository;
        this.disciplineLogRepository = disciplineLogRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.xpTransactionRepository = xpTransactionRepository;
        this.studentAuthResolver = studentAuthResolver;
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
            @RequestParam(defaultValue = "100") int size) {
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

    /**
     * POST /api/v1/students/bulk-parse – Parse Excel file and return student
     * preview
     */
    @PostMapping(value = "/bulk-parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Bulk Parse Students Spreadsheet", description = "Parses Excel and returns JSON preview list of student records without saving. Requires ADMIN or TEACHER role.")
    public ResponseEntity<ApiResponse<List<CreateStudentRequest>>> bulkParseStudents(
            @RequestParam("file") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ApiResponse<List<CreateStudentRequest>> response = studentService.bulkParse(file, username);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * POST /api/v1/students/bulk-import – Confirm import of selected students list
     */
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

    /**
     * GET /api/v1/students/department-performance – Get overall/year performance of
     * HOD department
     */
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

    /** POST /api/v1/students/{id}/make-captain – Promote student to Team Captain */
    @PostMapping("/{id}/make-captain")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Promote Student to Team Captain", description = "Sets the student as the Captain of their assigned team.")
    public ResponseEntity<ApiResponse<Void>> promoteToTeamCaptain(@PathVariable Long id) {
        ApiResponse<Void> response = studentService.promoteToTeamCaptain(id);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * POST /api/v1/students/{id}/remove-captain – Remove student from Team Captain
     * status
     */
    @PostMapping("/{id}/remove-captain")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Remove Student from Team Captain status", description = "Removes the student as the Captain of their assigned team.")
    public ResponseEntity<ApiResponse<Void>> removeTeamCaptain(@PathVariable Long id) {
        ApiResponse<Void> response = studentService.removeTeamCaptain(id);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * GET /api/v1/students/stages – Get all stages with validation for logged-in
     * student
     */
    @GetMapping("/stages")
    @PreAuthorize("hasAnyRole('STUDENT')")
    @Operation(summary = "Get Stages Configured for Student", description = "Returns list of stages enriched with specific user validation (unlock rules).")
    public ResponseEntity<?> getStudentStages() {
        try {
            com.spdms.entity.Student student = studentAuthResolver.getLoggedInStudent();
            List<ActivityStageResponse> stages = activityStageService.getAllStages();


            if (stages != null && !stages.isEmpty()) {
                // 1. Gather all Subgroup IDs
                List<Long> subgroupIds = new java.util.ArrayList<>();
                for (ActivityStageResponse stage : stages) {
                    if (stage.getSubgroups() != null) {
                        for (com.spdms.dto.ActivitySubgroupResponse subgroup : stage.getSubgroups()) {
                            subgroupIds.add(subgroup.getId());
                        }
                    }
                }

                // 2. Batch fetch Activities
                java.util.Map<Long, List<com.spdms.entity.Activity>> activitiesBySubgroup = new java.util.HashMap<>();
                List<Long> allActivityIds = new java.util.ArrayList<>();
                if (!subgroupIds.isEmpty()) {
                    List<com.spdms.entity.Activity> allActivities = activityRepository.findBySubgroupIdIn(subgroupIds);
                    for (com.spdms.entity.Activity act : allActivities) {
                        if (act.getSubgroup() != null) {
                            activitiesBySubgroup.computeIfAbsent(act.getSubgroup().getId(), k -> new java.util.ArrayList<>()).add(act);
                        }
                        allActivityIds.add(act.getId());
                    }
                }

                // 3. Batch fetch Assignments
                java.util.Map<Long, List<com.spdms.entity.ActivityAssignment>> assignmentsByActivity = new java.util.HashMap<>();
                if (!allActivityIds.isEmpty()) {
                    List<com.spdms.entity.ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityIdIn(allActivityIds);
                    for (com.spdms.entity.ActivityAssignment assignment : allAssignments) {
                        if (assignment.getActivity() != null) {
                            assignmentsByActivity.computeIfAbsent(assignment.getActivity().getId(), k -> new java.util.ArrayList<>()).add(assignment);
                        }
                    }
                }

                // 4. Batch fetch XP Transactions for this student
                java.util.Map<Long, Integer> xpByActivityId = new java.util.HashMap<>();
                java.util.Map<String, Integer> xpByActivityName = new java.util.HashMap<>();
                List<com.spdms.entity.XpTransaction> allTxs = xpTransactionRepository.findByStudentIdAndStatus(student.getId(), "APPROVED");
                
                System.out.println("==================================================");
                System.out.println("Loaded transactions = " + allTxs.size());
                System.out.println("Student String ID: " + student.getStudentId() + " (Long ID: " + student.getId() + ")");
                System.out.println("==================================================");
                
                for (com.spdms.entity.XpTransaction tx : allTxs) {
                    System.out.println("XP Transaction");
                    System.out.println("id=" + tx.getId());
                    System.out.println("activityId=" + (tx.getActivity() != null ? tx.getActivity().getId() : "null"));
                    System.out.println("activityName=\"" + tx.getActivityName() + "\"");
                    System.out.println("xp=" + tx.getXpPoints());
                    System.out.println("status=" + tx.getStatus());
                    System.out.println("studentId=" + (tx.getStudent() != null ? tx.getStudent().getStudentId() : "null"));
                    System.out.println("--------------------------------------------------");

                    if (tx.getActivity() != null) {
                        xpByActivityId.merge(tx.getActivity().getId(), tx.getXpPoints(), Integer::sum);
                    }
                    if (tx.getActivityName() != null) {
                        String baseName = tx.getActivityName();
                        int idx = baseName.lastIndexOf(" (");
                        if (idx != -1 && baseName.contains("Awarded by")) {
                            baseName = baseName.substring(0, idx);
                        }
                        String normalizedTxName = baseName.trim().toLowerCase().replaceAll("\\s+", " ").replaceAll("^\\p{Punct}+|\\p{Punct}+$", "");
                        xpByActivityName.merge(normalizedTxName, tx.getXpPoints(), Integer::sum);
                    }
                }

                // 5. In-Memory Assembly
                for (ActivityStageResponse stage : stages) {
                    StageValidationResponse validation = stageValidationService.validateStage(student.getId(), stage.getId());
                    stage.setValidation(validation);
                    stage.setVisible(validation.isVisible());
                    stage.setLocked(validation.isLocked());
                    stage.setIsCompleted(validation.isCompleted());
                    stage.setIsActive(validation.isActive());
                    stage.setStageStatus(validation.getStageStatus());

                    if (stage.getSubgroups() != null) {
                        for (com.spdms.dto.ActivitySubgroupResponse subgroup : stage.getSubgroups()) {
                            Long subId = subgroup.getId();
                            List<com.spdms.entity.Activity> activities = activitiesBySubgroup.getOrDefault(subId, java.util.Collections.emptyList());
                            List<com.spdms.dto.ActivityResponse> enrichedActivities = new java.util.ArrayList<>();
                            
                            for (com.spdms.entity.Activity act : activities) {
                                  com.spdms.dto.ActivityResponse actMap = new com.spdms.dto.ActivityResponse();
                                  actMap.setActivityId(act.getId());
                                  
                                  String currentActivityName = act.getActivityName() != null ? act.getActivityName() : act.getName();
                                  String normalizedActName = currentActivityName != null ? currentActivityName.trim().toLowerCase().replaceAll("\\s+", " ").replaceAll("^\\p{Punct}+|\\p{Punct}+$", "") : null;

                                  actMap.setActivityName(currentActivityName);
                                  actMap.setDescription(act.getActivityDescription() != null ? act.getActivityDescription() : act.getDescription());
                                  int rewardXp = (act.getAwardXp() != null && act.getAwardXp() > 0) ? act.getAwardXp() : act.getMaxPoints();
                                  actMap.setRewardXp(rewardXp);
                                  
                                  int sumXp = 0;
                                  boolean matchedById = false;
                                  boolean matchedByName = false;
                                  
                                  if (xpByActivityId.containsKey(act.getId())) {
                                      sumXp = xpByActivityId.get(act.getId());
                                      matchedById = true;
                                  } else if (normalizedActName != null && xpByActivityName.containsKey(normalizedActName)) {
                                      sumXp = xpByActivityName.get(normalizedActName);
                                      matchedByName = true;
                                  }
                                  
                                  System.out.println("Activity:");
                                  System.out.println("Activity ID : " + act.getId());
                                  System.out.println("Activity Name : " + currentActivityName);
                                  System.out.println("Normalized Activity Name : " + normalizedActName);
                                  System.out.println("Reward XP : " + rewardXp);
                                  System.out.println("Matched ActivityId : " + matchedById);
                                  System.out.println("Matched ActivityName : " + matchedByName);
                                  System.out.println("Awarded XP : " + sumXp);
                                  System.out.println("--------------------------------------------------");
                                  
                                  Integer cap = act.getCap();
                                  int awardedXp = sumXp;
                                  int requiredXp = rewardXp;
                                  
                                  if (cap != null && cap > 1) {
                                      requiredXp = rewardXp * cap;
                                  }
                                  
                                  if (awardedXp > requiredXp) {
                                      awardedXp = requiredXp;
                                  }
                                  
                                  actMap.setAwardedXp(awardedXp);
                                  actMap.setRequiredXp(requiredXp);
                                  actMap.setRemainingXp(Math.max(0, requiredXp - awardedXp));

                                  actMap.setFrequency(act.getFrequency() != null ? act.getFrequency() : act.getAwardFrequency());
                                  actMap.setEvidence(act.getEvidence());

                                  String facultyName = null;
                                  Long facultyId = null;

                                  List<com.spdms.entity.ActivityAssignment> assignments = assignmentsByActivity.getOrDefault(act.getId(), java.util.Collections.emptyList());
                                  com.spdms.entity.ActivityAssignment bestAssignment = null;
                                  for (com.spdms.entity.ActivityAssignment assignment : assignments) {
                                      if (student.getSection() != null && assignment.getSection() != null && assignment.getSection().getId().equals(student.getSection().getId())) {
                                          bestAssignment = assignment;
                                          break;
                                      } else if (student.getSection() != null && student.getSection().getDepartment() != null && assignment.getDepartment() != null && assignment.getDepartment().getId().equals(student.getSection().getDepartment().getId())) {
                                          bestAssignment = assignment;
                                      } else if (bestAssignment == null) {
                                          bestAssignment = assignment; // fallback
                                      }
                                  }
                                  if (bestAssignment != null && bestAssignment.getTeacher() != null) {
                                      facultyName = bestAssignment.getTeacher().getFullName();
                                      facultyId = bestAssignment.getTeacher().getId();
                                  }
                                  
                                  if (facultyName == null && act.getSubgroup() != null && act.getSubgroup().getAssignedFaculty() != null) {
                                      facultyName = act.getSubgroup().getAssignedFaculty().getFullName();
                                      facultyId = act.getSubgroup().getAssignedFaculty().getId();
                                  }

                                  actMap.setFacultyName(facultyName);
                                  actMap.setFacultyId(facultyId);

                                  boolean completed = awardedXp >= requiredXp;
                                  String status = completed ? "COMPLETED" : (awardedXp > 0 ? "IN_PROGRESS" : "NOT_STARTED");
                                  
                                  actMap.setCompleted(completed);
                                  actMap.setStatus(status);

                                  enrichedActivities.add(actMap);
                            }
                            subgroup.setActivities(enrichedActivities);
                        }
                    }
                }
            }
            return ResponseEntity.ok(ApiResponse.ok(stages));
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            return ResponseEntity.status(500).body(sw.toString());
        }
    }

    /**
     * GET /api/v1/students/subgroups/{subgroupId}/activities – Shared endpoint for
     * read-only activity data
     */
    @GetMapping("/subgroups/{subgroupId}/activities")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'TEACHER')")
    @Operation(summary = "Get all activities of a subgroup")
    public ResponseEntity<ApiResponse<List<com.spdms.entity.Activity>>> getActivitiesBySubgroup(
            @PathVariable Long subgroupId) {
        List<com.spdms.entity.Activity> activities = activityRepository.findBySubgroupId(subgroupId);
        // We only want the basic fields mapped to avoid serialization issues, but
        // ApiResponse handles it
        return ResponseEntity.ok(ApiResponse.ok(activities));
    }
}
