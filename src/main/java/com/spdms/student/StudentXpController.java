package com.spdms.student;

import com.spdms.dto.ApiResponse;
import com.spdms.dto.AwardXpRequest;
import com.spdms.dto.MyActivityStudentsResponse;
import com.spdms.entity.*;
import com.spdms.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Student XP", description = "Endpoints for managing student XP awards")
public class StudentXpController {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final StudentRepository studentRepository;
    private final StudentActivityXpRepository studentActivityXpRepository;
    private final XpTransactionRepository xpTransactionRepository;

    public StudentXpController(UserRepository userRepository,
                               ActivityRepository activityRepository,
                               ActivityAssignmentRepository activityAssignmentRepository,
                               StudentRepository studentRepository,
                               StudentActivityXpRepository studentActivityXpRepository,
                               XpTransactionRepository xpTransactionRepository) {
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.studentRepository = studentRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.xpTransactionRepository = xpTransactionRepository;
    }

    @GetMapping("/my-activities/{activityId}/students")
    @PreAuthorize("hasRole('TEACHER')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get list of students eligible for the given assigned activity")
    public ResponseEntity<ApiResponse<MyActivityStudentsResponse>> getStudentsForActivity(@PathVariable Long activityId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User teacher = userRepository.findByUsername(username).orElse(null);
        if (teacher == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Teacher profile not found"));
        }

        List<ActivityAssignment> assignments = activityAssignmentRepository.findByActivityIdAndTeacherId(activityId, teacher.getId());
        if (assignments.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access Denied: You are not assigned to this activity."));
        }

        Activity activity = assignments.get(0).getActivity();
        
        // Resolve eligible students
        Map<Long, Student> uniqueStudents = new LinkedHashMap<>();
        for (ActivityAssignment assignment : assignments) {
            List<Student> list;
            if (assignment.getSection() != null) {
                list = studentRepository.findByDepartmentIdAndSectionId(
                    assignment.getDepartment().getId(), 
                    assignment.getSection().getId()
                );
            } else {
                list = studentRepository.findByDepartmentId(assignment.getDepartment().getId());
            }
            if (list != null) {
                for (Student s : list) {
                    if (s.isActive()) {
                        uniqueStudents.put(s.getId(), s);
                    }
                }
            }
        }

        // Map Students
        List<MyActivityStudentsResponse.StudentDetail> studentDetails = uniqueStudents.values().stream()
            .map(s -> new MyActivityStudentsResponse.StudentDetail(
                s.getId(),
                s.getFullName(),
                s.getStudentId(),
                s.getRegNo(),
                s.getDepartment() != null ? s.getDepartment().getName() : "",
                s.getSectionRef() != null ? s.getSectionRef().getSectionName() : (s.getSection() != null ? s.getSection() : ""),
                s.getTotalXp(),
                s.getScore()
            ))
            .collect(Collectors.toList());

        // Parse evidence list
        List<String> evidenceList = new ArrayList<>();
        if (activity.getEvidence() != null && !activity.getEvidence().trim().isEmpty()) {
            for (String ev : activity.getEvidence().split(",")) {
                evidenceList.add(ev.trim());
            }
        }

        MyActivityStudentsResponse.ActivityDetail actDetail = new MyActivityStudentsResponse.ActivityDetail(
            activity.getId(),
            activity.getName(),
            activity.getDescription(),
            activity.getOwnerDepartment(),
            evidenceList,
            activity.getFrequency(),
            activity.getType()
        );

        ActivityAssignment firstAssign = assignments.get(0);
        MyActivityStudentsResponse.AssignmentDetail assignDetail = new MyActivityStudentsResponse.AssignmentDetail(
            firstAssign.getId(),
            firstAssign.getAssignedBy() != null ? firstAssign.getAssignedBy().getFullName() : "",
            firstAssign.getAssignedAt() != null ? firstAssign.getAssignedAt().toString() : ""
        );

        int xpLimit = 0;
        try {
            xpLimit = Integer.parseInt(activity.getXp());
        } catch (Exception ignored) {}

        MyActivityStudentsResponse response = new MyActivityStudentsResponse(actDetail, studentDetails, xpLimit, assignDetail);
        return ResponseEntity.ok(ApiResponse.ok("Students retrieved successfully", response));
    }

    @PostMapping("/student-xp/award")
    @PreAuthorize("hasRole('TEACHER')")
    @Transactional
    @Operation(summary = "Award XP points to a student for a specific activity")
    public ResponseEntity<ApiResponse<Void>> awardStudentXp(@RequestBody AwardXpRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User teacher = userRepository.findByUsername(username).orElse(null);
        if (teacher == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Teacher profile not found"));
        }

        Student student = studentRepository.findById(request.getStudentId()).orElse(null);
        if (student == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Student not found"));
        }

        Activity activity = activityRepository.findById(request.getActivityId()).orElse(null);
        if (activity == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Activity not found"));
        }

        ActivityAssignment assignment = activityAssignmentRepository.findById(request.getAssignmentId()).orElse(null);
        if (assignment == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Activity assignment not found"));
        }

        // Security check: teacher can only award for activities assigned to them
        if (!assignment.getTeacher().getId().equals(teacher.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access Denied: You are not authorized to award XP for this assignment."));
        }

        // Validate XP points limits (strict Admin-configured XP value used)
        int xpToAward = activity.getAwardXp() != null ? activity.getAwardXp() : 0;
        if (xpToAward <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Activity XP value is not configured correctly"));
        }

        // Validate Award Rules and check limits
        String limitError = checkAwardLimit(student, activity);
        if (limitError != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(limitError));
        }

        // Record history log
        StudentActivityXp record = new StudentActivityXp(
            student,
            activity,
            teacher,
            assignment,
            xpToAward,
            request.getRemarks() != null ? request.getRemarks() : "",
            LocalDateTime.now()
        );
        studentActivityXpRepository.save(record);

        // Update student scores using modifying query to prevent saving other columns (like password)
        studentRepository.updateStudentXpAndScore(
            student.getId(), 
            student.getTotalXp() + xpToAward, 
            student.getScore() + xpToAward
        );

        // Create matching XpTransaction for student profile history view
        XpTransaction tx = XpTransaction.builder()
            .student(student)
            .category(activity.getXpCategory() != null ? activity.getXpCategory().toUpperCase() : "SKILL")
            .activityName(activity.getName() + " (Awarded by " + teacher.getFullName() + ")")
            .xpPoints(xpToAward)
            .submittedAt(LocalDateTime.now())
            .status("APPROVED")
            .approvedBy(teacher.getFullName())
            .isPenalty(false)
            .capApplied(false)
            .build();
        xpTransactionRepository.save(tx);

        return ResponseEntity.ok(ApiResponse.ok("XP points awarded successfully", null));
    }

    @PostMapping("/student-xp/award/batch")
    @PreAuthorize("hasRole('TEACHER')")
    @Transactional
    @Operation(summary = "Award XP points to multiple students for a specific activity")
    public ResponseEntity<ApiResponse<Void>> awardStudentXpBatch(@RequestBody AwardXpRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User teacher = userRepository.findByUsername(username).orElse(null);
        if (teacher == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Teacher profile not found"));
        }

        Activity activity = activityRepository.findById(request.getActivityId()).orElse(null);
        if (activity == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Activity not found"));
        }

        ActivityAssignment assignment = activityAssignmentRepository.findById(request.getAssignmentId()).orElse(null);
        if (assignment == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Activity assignment not found"));
        }

        // Security check: teacher can only award for activities assigned to them
        if (!assignment.getTeacher().getId().equals(teacher.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access Denied: You are not authorized to award XP for this assignment."));
        }

        // Configured XP points limits
        int xpToAward = activity.getAwardXp() != null ? activity.getAwardXp() : 0;
        if (xpToAward <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Activity XP value is not configured correctly"));
        }

        List<Long> studentIds = request.getStudentIds();
        if (studentIds == null || studentIds.isEmpty()) {
            if (request.getStudentId() != null) {
                studentIds = List.of(request.getStudentId());
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("At least one student must be selected"));
            }
        }

        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (Long studentId : studentIds) {
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) {
                errors.add("Student ID " + studentId + " not found");
                continue;
            }

            // Validate Award Rules and check limits
            String limitError = checkAwardLimit(student, activity);
            if (limitError != null) {
                errors.add(limitError);
                continue;
            }

            // Record history log
            StudentActivityXp record = new StudentActivityXp(
                student,
                activity,
                teacher,
                assignment,
                xpToAward,
                request.getRemarks() != null ? request.getRemarks() : "",
                LocalDateTime.now()
            );
            studentActivityXpRepository.save(record);

            // Update student scores using modifying query
            studentRepository.updateStudentXpAndScore(
                student.getId(),
                student.getTotalXp() + xpToAward,
                student.getScore() + xpToAward
            );

            // Create matching XpTransaction
            XpTransaction tx = XpTransaction.builder()
                .student(student)
                .category(activity.getXpCategory() != null ? activity.getXpCategory().toUpperCase() : "SKILL")
                .activityName(activity.getName() + " (Awarded by " + teacher.getFullName() + ")")
                .xpPoints(xpToAward)
                .submittedAt(LocalDateTime.now())
                .status("APPROVED")
                .approvedBy(teacher.getFullName())
                .isPenalty(false)
                .capApplied(false)
                .build();
            xpTransactionRepository.save(tx);
            successCount++;
        }

        if (successCount == 0 && !errors.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(String.join(", ", errors)));
        }

        return ResponseEntity.ok(ApiResponse.ok("XP points awarded successfully to " + successCount + " students", null));
    }

    private String checkAwardLimit(Student student, Activity activity) {
        String awardFrequency = activity.getAwardFrequency();
        if (awardFrequency == null || awardFrequency.trim().isEmpty()) {
            awardFrequency = "One Time";
        }

        // ── Step 1: Award Day validation (Weekly only) ────────────────────────
        if ("Weekly".equalsIgnoreCase(awardFrequency)) {
            String awardDays = activity.getAwardDays();
            if (awardDays != null && !awardDays.trim().isEmpty()) {
                java.time.DayOfWeek today = LocalDate.now().getDayOfWeek();
                String todayName = today.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
                boolean dayAllowed = java.util.Arrays.stream(awardDays.split(","))
                    .map(String::trim)
                    .anyMatch(d -> d.equalsIgnoreCase(todayName));
                if (!dayAllowed) {
                    String daysFormatted = java.util.Arrays.stream(awardDays.split(","))
                        .map(String::trim).collect(Collectors.joining(", "));
                    return "XP can only be awarded on the configured Award Days: " + daysFormatted + ". Today is " + todayName + ".";
                }
            }
        }

        // ── Step 2: Cap validation ────────────────────────────────────────────
        List<StudentActivityXp> history = studentActivityXpRepository.findByStudentIdAndActivityId(
            student.getId(), activity.getId());

        // One Time — only once ever
        if ("One Time".equalsIgnoreCase(awardFrequency)) {
            if (!history.isEmpty()) {
                return "Student " + student.getFullName() + " has already been awarded XP for this one-time activity.";
            }
            return null;
        }

        // Manual — no time window, but still check cap (cap=1 always)
        if ("Manual".equalsIgnoreCase(awardFrequency)) {
            if (!history.isEmpty()) {
                return "Student " + student.getFullName() + " has already been awarded XP for this manual activity. Contact the administrator to reset.";
            }
            return null;
        }

        // For repeating frequencies — determine window start
        Integer cap = activity.getMaximumAwards();
        if (cap == null || cap <= 0) cap = 1;

        LocalDate now = LocalDate.now();
        LocalDateTime windowStart;
        String windowLabel;

        if ("Daily".equalsIgnoreCase(awardFrequency)) {
            windowStart = now.atStartOfDay();
            windowLabel = "today";
        } else if ("Weekly".equalsIgnoreCase(awardFrequency)) {
            windowStart = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay();
            windowLabel = "this week";
        } else if ("Monthly".equalsIgnoreCase(awardFrequency)) {
            windowStart = now.withDayOfMonth(1).atStartOfDay();
            windowLabel = "this month";
        } else {
            // Unknown frequency — treat as one-time
            if (!history.isEmpty()) {
                return "Student " + student.getFullName() + " has already been awarded XP for this activity.";
            }
            return null;
        }

        final LocalDateTime limitStart = windowStart;
        long awardsInWindow = history.stream()
            .filter(h -> !h.getAwardedAt().isBefore(limitStart))
            .count();

        if (awardsInWindow >= cap) {
            return "Student " + student.getFullName() + " has already reached the maximum allowed XP awards ("
                + cap + ") for " + windowLabel + ".";
        }

        return null;
    }
}
