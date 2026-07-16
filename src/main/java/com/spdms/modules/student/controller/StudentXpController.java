package com.spdms.modules.student.controller;

import com.spdms.modules.student.service.StudentService;
import com.spdms.student.XpService;

import com.spdms.common.response.ApiResponse;
import com.spdms.dto.AwardXpRequest;
import com.spdms.modules.student.dto.response.MyActivityStudentsResponse;
import com.spdms.entity.*;
import com.spdms.repository.*;
import com.spdms.modules.activity.repository.*;
import com.spdms.modules.faculty.repository.*;
import com.spdms.modules.student.repository.*;
import com.spdms.modules.authentication.repository.UserRepository;
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
    private final SectionRepository sectionRepository;
    private final com.spdms.service.AssignmentSecurityService assignmentSecurityService;

    public StudentXpController(UserRepository userRepository,
                               ActivityRepository activityRepository,
                               ActivityAssignmentRepository activityAssignmentRepository,
                               StudentRepository studentRepository,
                               StudentActivityXpRepository studentActivityXpRepository,
                               XpTransactionRepository xpTransactionRepository,
                               SectionRepository sectionRepository,
                               com.spdms.service.AssignmentSecurityService assignmentSecurityService) {
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.studentRepository = studentRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.xpTransactionRepository = xpTransactionRepository;
        this.sectionRepository = sectionRepository;
        this.assignmentSecurityService = assignmentSecurityService;
    }

    private String normalizeYearToRoman(String yr) {
        if (yr == null)
            return null;
        String t = yr.trim().toUpperCase();
        if (t.equals("1") || t.equals("I"))
            return "I";
        if (t.equals("2") || t.equals("II"))
            return "II";
        if (t.equals("3") || t.equals("III"))
            return "III";
        if (t.equals("4") || t.equals("IV"))
            return "IV";
        return yr;
    }

    private ActivityAssignment getPriorityAssignment(List<ActivityAssignment> matches) {
        if (matches.isEmpty()) return null;
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.SPECIFIC_FACULTY) return a;
        }
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.SECTION) return a;
        }
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.DEPARTMENT) return a;
        }
        for (ActivityAssignment a : matches) {
            if (a.getAssignmentScope() == AssignmentScope.GLOBAL) return a;
        }
        return matches.get(0);
    }

    private ActivityAssignment findMatchingAssignmentForStudent(List<ActivityAssignment> matching, Student student) {
        if (student == null) return null;
        for (ActivityAssignment a : matching) {
            if (a.getAssignmentScope() == AssignmentScope.GLOBAL) {
                return a;
            }
            if (student.getDepartment() != null && a.getDepartment() != null 
                    && student.getDepartment().getId().equals(a.getDepartment().getId())) {
                if (a.getAssignmentScope() == AssignmentScope.DEPARTMENT) {
                    return a;
                }
                if (student.getSection() != null && a.getSection() != null 
                        && student.getSection().getId().equals(a.getSection().getId())) {
                    return a;
                }
            }
        }
        return null;
    }

    @GetMapping("/my-activities/{activityId}/years")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get distinct years assigned to the activity that the teacher has permission to view")
    public ResponseEntity<ApiResponse<List<String>>> getYearsForActivity(@PathVariable Long activityId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("User profile not found"));
        }

        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityId(activityId);
        List<ActivityAssignment> matching = allAssignments.stream()
            .filter(a -> assignmentSecurityService.isUserAssignedFaculty(a, currentUser))
            .collect(Collectors.toList());

        List<String> years = matching.stream()
            .map(ActivityAssignment::getYear)
            .filter(Objects::nonNull)
            .filter(y -> !y.trim().isEmpty())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        if (years.isEmpty()) {
            years.add("1");
        }

        return ResponseEntity.ok(ApiResponse.ok("Years retrieved successfully", years));
    }

    @GetMapping("/my-activities/{activityId}/departments")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get distinct departments assigned to the activity/year that the teacher has permission to view")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDepartmentsForActivity(
            @PathVariable Long activityId,
            @RequestParam(required = false) String year) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("User profile not found"));
        }

        String targetYear = (year == null || year.trim().isEmpty()) ? "1" : year;

        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityId(activityId);
        List<ActivityAssignment> matching = allAssignments.stream()
            .filter(a -> assignmentSecurityService.isUserAssignedFaculty(a, currentUser))
            .filter(a -> isYearMatching(targetYear, a.getYear()))
            .collect(Collectors.toList());

        List<Map<String, Object>> depts = matching.stream()
            .map(ActivityAssignment::getDepartment)
            .filter(Objects::nonNull)
            .distinct()
            .map(d -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", d.getId());
                map.put("name", d.getName());
                return map;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Departments retrieved successfully", depts));
    }

    @GetMapping("/my-activities/{activityId}/sections")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get distinct sections assigned to the activity/year/department that the teacher has permission to view")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSectionsForActivity(
            @PathVariable Long activityId,
            @RequestParam(required = false) String year,
            @RequestParam Long departmentId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("User profile not found"));
        }

        String targetYear = (year == null || year.trim().isEmpty()) ? "1" : year;

        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityId(activityId);
        List<ActivityAssignment> matching = allAssignments.stream()
            .filter(a -> assignmentSecurityService.isUserAssignedFaculty(a, currentUser))
            .filter(a -> isYearMatching(targetYear, a.getYear()))
            .filter(a -> a.getDepartment() == null || a.getDepartment().getId().equals(departmentId))
            .collect(Collectors.toList());

        if (matching.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access Denied: You are not assigned to this activity."));
        }

        // Load all sections belonging to the selected department directly from Section repository
        boolean hasDepartmentLevelOrGlobalAssignment = matching.stream()
            .anyMatch(a -> a.getSection() == null);

        List<com.spdms.entity.Section> allSections = sectionRepository.findByDepartment_Id(departmentId);
        List<Map<String, Object>> sections = allSections.stream()
            .filter(s -> hasDepartmentLevelOrGlobalAssignment || 
                         matching.stream().anyMatch(a -> a.getSection() != null && a.getSection().getId().equals(s.getId())))
            .map(s -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", s.getId());
                map.put("sectionName", s.getSectionName());
                return map;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Sections retrieved successfully", sections));
    }

    @GetMapping("/my-activities/{activityId}/students")
    @PreAuthorize("hasRole('TEACHER')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get list of students eligible for the given assigned activity")
    public ResponseEntity<ApiResponse<MyActivityStudentsResponse>> getStudentsForActivity(
            @PathVariable Long activityId,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long sectionId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User teacher = userRepository.findByUsername(username).orElse(null);
        if (teacher == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Teacher profile not found"));
        }

        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityId(activityId);
        List<ActivityAssignment> matching = allAssignments.stream()
            .filter(a -> assignmentSecurityService.isUserAssignedFaculty(a, teacher))
            .filter(a -> year == null || isYearMatching(year, a.getYear()))
            .filter(a -> departmentId == null || a.getDepartment() == null || a.getDepartment().getId().equals(departmentId))
            .filter(a -> sectionId == null || a.getSection() == null || a.getSection().getId().equals(sectionId))
            .collect(Collectors.toList());

        if (matching.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access Denied: You are not assigned to this activity."));
        }

        ActivityAssignment priorityAssignment = getPriorityAssignment(matching);
        Activity activity = priorityAssignment.getActivity();
        
        String targetYear = (year == null || year.trim().isEmpty()) ? "1" : year;
        
        List<Student> rawStudents;
        if (departmentId != null) {
            if (sectionId != null) {
                rawStudents = studentRepository.findByDepartmentIdAndSectionId(departmentId, sectionId);
            } else {
                rawStudents = studentRepository.findByDepartmentId(departmentId);
            }
        } else {
            rawStudents = studentRepository.findAll();
        }

        Set<Student> uniqueStudents = new java.util.HashSet<>();
        if (rawStudents != null) {
            for (Student s : rawStudents) {
                if (s.isActive()) {
                    if (departmentId != null && (s.getDepartment() == null || !s.getDepartment().getId().equals(departmentId))) {
                        continue;
                    }
                    if (!isYearMatching(targetYear, s.getYear())) {
                        continue;
                    }
                    if (sectionId != null) {
                        if (s.getSection() == null || !s.getSection().getId().equals(sectionId)) {
                            continue;
                        }
                    }
                    uniqueStudents.add(s);
                }
            }
        }

        List<Student> studentList = new ArrayList<>(uniqueStudents);
        studentList.sort((s1, s2) -> {
            Long r1 = s1.getRegNo() != null ? s1.getRegNo() : 0L;
            Long r2 = s2.getRegNo() != null ? s2.getRegNo() : 0L;
            return r1.compareTo(r2);
        });

        List<MyActivityStudentsResponse.StudentDetail> studentDetails = new ArrayList<>();
        for (Student s : studentList) {
            String secName = s.getSection() != null ? s.getSection().getSectionName() : "";
            studentDetails.add(new MyActivityStudentsResponse.StudentDetail(
                s.getId(),
                s.getFullName(),
                s.getStudentId(),
                s.getRegNo(),
                s.getDepartment() != null ? s.getDepartment().getName() : "",
                secName,
                s.getYear() != null ? s.getYear() : "",
                s.getTotalXp(),
                s.getScore()
            ));
        }

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
            activity.getType(),
            activity.getXpCategory(),
            activity.getAwardEnabled(),
            activity.getAwardXp(),
            activity.getPenaltyEnabled(),
            activity.getPenaltyXp(),
            activity.getCap()
        );

        String assignedFacultyName = "Any Faculty";
        String assignmentMode = "Global";
        if (priorityAssignment.getAssignmentScope() == AssignmentScope.SPECIFIC_FACULTY) {
            assignedFacultyName = priorityAssignment.getTeacher() != null ? priorityAssignment.getTeacher().getFullName() : "Any Faculty";
            assignmentMode = "Specific Faculty";
        } else if (priorityAssignment.getAssignmentScope() == AssignmentScope.SECTION || priorityAssignment.getAssignmentScope() == AssignmentScope.DEPARTMENT) {
            assignmentMode = "Class Coordinator (Auto Assigned)";
            assignedFacultyName = "Class Coordinator (Auto Assigned)";
            if (priorityAssignment.getDepartment() != null && priorityAssignment.getSection() != null) {
                List<User> ccs = userRepository.findClassCoordinatorsByDepartmentAndSection(
                    priorityAssignment.getDepartment().getId(),
                    priorityAssignment.getSection().getId()
                );
                if (!ccs.isEmpty()) {
                    assignedFacultyName = ccs.get(0).getFullName();
                }
            }
        }

        MyActivityStudentsResponse.AssignmentDetail assignDetail = new MyActivityStudentsResponse.AssignmentDetail(
            priorityAssignment.getId(),
            priorityAssignment.getAssignedBy() != null ? priorityAssignment.getAssignedBy().getFullName() : "",
            priorityAssignment.getAssignedAt() != null ? priorityAssignment.getAssignedAt().toString() : "",
            assignedFacultyName,
            assignmentMode
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
        if (activity.getStage() != null && activity.getStage().getStatus() != com.spdms.enums.StageStatus.ACTIVE) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot award XP for an activity in a non-active stage."));
        }

        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityId(activity.getId());
        List<ActivityAssignment> matching = allAssignments.stream()
            .filter(a -> assignmentSecurityService.isUserAssignedFaculty(a, teacher))
            .collect(Collectors.toList());

        ActivityAssignment assignment = findMatchingAssignmentForStudent(matching, student);
        if (assignment == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access Denied: You are not authorized to award XP to this student for this activity."));
        }

        // Dynamic calculation of XP based on PASS/FAIL result
        String resultStr = request.getResult();
        int xpToAward = 0;
        Boolean isAward = activity.getAwardEnabled();
        Boolean isPenalty = activity.getPenaltyEnabled();
        if (isAward == null && isPenalty == null) {
            int legacyXp = activity.getAwardXp() != null ? activity.getAwardXp() : 0;
            if ("Penalty".equalsIgnoreCase(activity.getXpType())) {
                isAward = false;
                isPenalty = true;
            } else {
                isAward = true;
                isPenalty = false;
            }
        }
        if ("FAIL".equalsIgnoreCase(resultStr)) {
            if (Boolean.TRUE.equals(isPenalty)) {
                int px = activity.getPenaltyXp() != null ? activity.getPenaltyXp() : activity.getAwardXp();
                xpToAward = -Math.abs(px);
            }
        } else {
            if (Boolean.TRUE.equals(isAward)) {
                xpToAward = activity.getAwardXp() != null ? activity.getAwardXp() : 0;
            }
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
            resultStr,
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
            .activity(activity)
            .category(activity.getXpCategory() != null ? activity.getXpCategory().toUpperCase() : "SKILL")
            .activityName(activity.getName() + " (" + resultStr + " - Awarded by " + teacher.getFullName() + ")")
            .xpPoints(xpToAward)
            .submittedAt(LocalDateTime.now())
            .status("APPROVED")
            .approvedBy(teacher.getFullName())
            .isPenalty(xpToAward < 0)
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
        if (activity.getStage() != null && activity.getStage().getStatus() != com.spdms.enums.StageStatus.ACTIVE) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot award XP for an activity in a non-active stage."));
        }

        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityId(activity.getId());
        List<ActivityAssignment> matching = allAssignments.stream()
            .filter(a -> assignmentSecurityService.isUserAssignedFaculty(a, teacher))
            .collect(Collectors.toList());

        // Dynamic calculation of XP based on PASS/FAIL result
        String resultStr = request.getResult();
        int xpToAward = 0;
        Boolean isAward = activity.getAwardEnabled();
        Boolean isPenalty = activity.getPenaltyEnabled();
        if (isAward == null && isPenalty == null) {
            int legacyXp = activity.getAwardXp() != null ? activity.getAwardXp() : 0;
            if ("Penalty".equalsIgnoreCase(activity.getXpType())) {
                isAward = false;
                isPenalty = true;
            } else {
                isAward = true;
                isPenalty = false;
            }
        }
        if ("FAIL".equalsIgnoreCase(resultStr)) {
            if (Boolean.TRUE.equals(isPenalty)) {
                int px = activity.getPenaltyXp() != null ? activity.getPenaltyXp() : activity.getAwardXp();
                xpToAward = -Math.abs(px);
            }
        } else {
            if (Boolean.TRUE.equals(isAward)) {
                xpToAward = activity.getAwardXp() != null ? activity.getAwardXp() : 0;
            }
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

            ActivityAssignment assignment = findMatchingAssignmentForStudent(matching, student);
            if (assignment == null) {
                errors.add("Access Denied: You are not authorized to award XP to student " + student.getFullName());
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
                resultStr,
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
                .activity(activity)
                .category(activity.getXpCategory() != null ? activity.getXpCategory().toUpperCase() : "SKILL")
                .activityName(activity.getName() + " (" + resultStr + " - Awarded by " + teacher.getFullName() + ")")
                .xpPoints(xpToAward)
                .submittedAt(LocalDateTime.now())
                .status("APPROVED")
                .approvedBy(teacher.getFullName())
                .isPenalty(xpToAward < 0)
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
        // Per Assignment - unlimited awards
        if ("Per Assignment".equalsIgnoreCase(awardFrequency)) {
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
        } else if ("Every Period".equalsIgnoreCase(awardFrequency)) {
            windowStart = now.atStartOfDay();
            windowLabel = "today";
            cap = 8;
        } else if ("Weekly".equalsIgnoreCase(awardFrequency)) {
            windowStart = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay();
            windowLabel = "this week";
        } else if ("Monthly".equalsIgnoreCase(awardFrequency)) {
            windowStart = now.withDayOfMonth(1).atStartOfDay();
            windowLabel = "this month";
        } else {
            // Custom frequency — treat as lifetime cap
            if (cap == null || cap <= 0) {
                // Unlimited cap
                return null;
            } else {
                // Manual cap
                if (history.size() >= cap) {
                    return "Student " + student.getFullName() + " has reached the maximum cap (" + cap + ") for this activity.";
                }
                return null;
            }
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

    private boolean isYearMatching(String yr1, String yr2) {
        String y1 = (yr1 == null || yr1.trim().isEmpty()) ? "1" : yr1.trim().toLowerCase();
        String y2 = (yr2 == null || yr2.trim().isEmpty()) ? "1" : yr2.trim().toLowerCase();
        if (y1.equals(y2)) return true;

        int n1 = getYearNumber(y1);
        int n2 = getYearNumber(y2);
        if (n1 != -1 && n2 != -1) {
            return n1 == n2;
        }
        return false;
    }

    private int getYearNumber(String y) {
        if (y.contains("1") || y.equals("i") || y.contains("first")) return 1;
        if (y.contains("2") || y.equals("ii") || y.contains("second")) return 2;
        if (y.contains("3") || y.equals("iii") || y.contains("third")) return 3;
        if (y.contains("4") || y.equals("iv") || y.contains("fourth")) return 4;
        return -1;
    }
}
