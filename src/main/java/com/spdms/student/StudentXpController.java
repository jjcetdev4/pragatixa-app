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

        // Validate XP points limits
        int xpLimit = 0;
        try {
            xpLimit = Integer.parseInt(activity.getXp());
        } catch (Exception ignored) {}

        if (request.getXp() <= 0 || request.getXp() > xpLimit) {
            return ResponseEntity.badRequest().body(ApiResponse.error("XP exceeds maximum allowed for this activity. Allowed: 1-" + xpLimit));
        }

        // Frequency duplication checks
        List<StudentActivityXp> history = studentActivityXpRepository.findByStudentIdAndActivityId(student.getId(), activity.getId());
        String freq = activity.getFrequency() != null ? activity.getFrequency().trim().toLowerCase() : "";

        if ("daily".equals(freq)) {
            LocalDate today = LocalDate.now();
            boolean existsToday = history.stream().anyMatch(h -> h.getAwardedAt().toLocalDate().isEqual(today));
            if (existsToday) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("Student already rewarded today"));
            }
        } else if ("weekly".equals(freq)) {
            LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
            boolean existsThisWeek = history.stream().anyMatch(h -> h.getAwardedAt().isAfter(weekAgo));
            if (existsThisWeek) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("Student already rewarded this week"));
            }
        } else if ("monthly".equals(freq)) {
            LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);
            boolean existsThisMonth = history.stream().anyMatch(h -> h.getAwardedAt().isAfter(monthAgo));
            if (existsThisMonth) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("Student already rewarded this month"));
            }
        } else if ("once".equals(freq)) {
            if (!history.isEmpty()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("Student already rewarded for this activity"));
            }
        }

        // Record history log
        StudentActivityXp record = new StudentActivityXp(
            student,
            activity,
            teacher,
            assignment,
            request.getXp(),
            request.getRemarks() != null ? request.getRemarks() : "",
            LocalDateTime.now()
        );
        studentActivityXpRepository.save(record);

        // Update student scores using modifying query to prevent saving other columns (like password)
        studentRepository.updateStudentXpAndScore(
            student.getId(), 
            student.getTotalXp() + request.getXp(), 
            student.getScore() + request.getXp()
        );

        // Create matching XpTransaction for student profile history view
        XpTransaction tx = XpTransaction.builder()
            .student(student)
            .category(activity.getType() != null ? activity.getType().toUpperCase() : "SKILL")
            .activityName(activity.getName() + " (Awarded by " + teacher.getFullName() + ")")
            .xpPoints(request.getXp())
            .submittedAt(LocalDateTime.now())
            .status("APPROVED")
            .approvedBy(teacher.getFullName())
            .isPenalty(false)
            .capApplied(false)
            .build();
        xpTransactionRepository.save(tx);

        return ResponseEntity.ok(ApiResponse.ok("XP points awarded successfully", null));
    }
}
