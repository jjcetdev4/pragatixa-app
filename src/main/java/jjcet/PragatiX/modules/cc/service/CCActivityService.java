package jjcet.PragatiX.modules.cc.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.modules.activity.dto.response.ActivityStageResponse;
import jjcet.PragatiX.modules.activity.repository.ActivityRepository;
import jjcet.PragatiX.modules.admin.service.ActivityQueryService;
import jjcet.PragatiX.modules.admin.service.AdminAssignmentService;
import jjcet.PragatiX.modules.admin.service.AdminStageService;
import jjcet.PragatiX.modules.activity.repository.ActivityStageRepository;
import jjcet.PragatiX.modules.cc.dto.CCActivityAssignRequest;
import jjcet.PragatiX.modules.cc.dto.CCTeacherAssignRequest;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.repository.ActivityAssignmentRepository;
import jjcet.PragatiX.repository.ActivityTemporaryAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CCActivityService {

    private static final Logger log = LoggerFactory.getLogger(CCActivityService.class);

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final ActivityTemporaryAssignmentRepository activityTemporaryAssignmentRepository;
    private final ActivityStageRepository activityStageRepository;
    private final StudentRepository studentRepository;
    private final AdminAssignmentService adminAssignmentService;
    private final AdminStageService adminStageService;
    private final ActivityQueryService activityQueryService;

    public CCActivityService(UserRepository userRepository,
            ActivityRepository activityRepository,
            ActivityAssignmentRepository activityAssignmentRepository,
            ActivityTemporaryAssignmentRepository activityTemporaryAssignmentRepository,
            ActivityStageRepository activityStageRepository,
            StudentRepository studentRepository,
            AdminAssignmentService adminAssignmentService,
            AdminStageService adminStageService,
            ActivityQueryService activityQueryService) {
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.activityTemporaryAssignmentRepository = activityTemporaryAssignmentRepository;
        this.activityStageRepository = activityStageRepository;
        this.studentRepository = studentRepository;
        this.adminAssignmentService = adminAssignmentService;
        this.adminStageService = adminStageService;
        this.activityQueryService = activityQueryService;
    }

    private User validateAndGetCCUser(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return null;
        }
        boolean isCcOrAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN") || r.getName().equalsIgnoreCase("ROLE_SUPER_ADMIN"))
                || user.getSubRoles().stream().map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));
        if (!isCcOrAdmin) {
            return null;
        }
        return user;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<ActivityStageResponse>>> getStages(String username) {
        return getStages(username, null);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<ActivityStageResponse>>> getStages(String username, String academicYear) {
        User ccUser = validateAndGetCCUser(username);
        if (ccUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access Denied: Only Class Coordinators can access this module."));
        }

        jjcet.PragatiX.enums.AcademicYear targetYear = null;
        if (academicYear != null && !academicYear.trim().isEmpty()) {
            targetYear = jjcet.PragatiX.enums.AcademicYear.fromString(academicYear);
        }
        if (targetYear == null) {
            targetYear = jjcet.PragatiX.enums.AcademicYear.fromUser(ccUser);
        }

        return adminStageService.getAllStages(targetYear);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Activity>>> getActiveActivities(String username, Long stageId,
            String subgroup) {
        try {
            java.nio.file.Files.writeString(java.nio.file.Paths.get("c:/Updating_SPDMS/updating_decipline_backend/cc_debug.log"), 
                "getActiveActivities called for user: " + username + " stageId: " + stageId + " subgroup: " + subgroup + "\n",
                java.nio.file.StandardOpenOption.APPEND);
        } catch(Exception e) {}

        User ccUser = validateAndGetCCUser(username);
        if (ccUser == null) {
            try {
                java.nio.file.Files.writeString(java.nio.file.Paths.get("c:/Updating_SPDMS/updating_decipline_backend/cc_debug.log"), 
                    "User not found or invalid CC\n",
                    java.nio.file.StandardOpenOption.APPEND);
            } catch(Exception e) {}
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "User is not a valid Class Coordinator", null));
        }

        jjcet.PragatiX.enums.AcademicYear ccAcademicYear = jjcet.PragatiX.enums.AcademicYear.fromUser(ccUser);
        try {
            java.nio.file.Files.writeString(java.nio.file.Paths.get("c:/Updating_SPDMS/updating_decipline_backend/cc_debug.log"), 
                "ccAcademicYear: " + ccAcademicYear + "\n",
                java.nio.file.StandardOpenOption.APPEND);
        } catch(Exception e) {}

        List<Activity> rawActivities;
        if (stageId != null) {
            rawActivities = activityQueryService.getActivitiesByStageUnfiltered(stageId, subgroup, ccAcademicYear);
        } else {
            rawActivities = activityRepository.findAll();
        }
        
        try {
            java.nio.file.Files.writeString(java.nio.file.Paths.get("c:/Updating_SPDMS/updating_decipline_backend/cc_debug.log"), 
                "rawActivities size: " + rawActivities.size() + "\n",
                java.nio.file.StandardOpenOption.APPEND);
        } catch(Exception e) {}

        Long deptId = ccUser.getDepartment() != null ? ccUser.getDepartment().getId() : null;
        Long secId = ccUser.getSection() != null ? ccUser.getSection().getId() : null;
        LocalDate today = LocalDate.now();

        // 1. Gather all activity IDs that have an active teacher assignment for this CC
        // / section - for "assign staff" page we return all activities, not just assigned
        Set<Long> assignedActivityIds = new HashSet<>();
        try {
            assignedActivityIds.addAll(activityAssignmentRepository.findActivityIdsWithAssignedTeacher(stageId, deptId, secId));
            assignedActivityIds.addAll(activityTemporaryAssignmentRepository.findActivityIdsWithActiveTemporaryTeacher(stageId, deptId, secId, today));
        } catch (Exception e) {
            // Ignore if Hibernate fails with null parameters, we add all raw activities anyway
        }
        
        // Include all activity ids from the raw list so CC can see and assign activities
        // that haven't been assigned yet
        rawActivities.forEach(a -> assignedActivityIds.add(a.getId()));

        // 2. Filter raw activities:
        // - Active status
        // - Not Attendance Engine
        // - Match CC's Academic Year (if applicable)
        // - Assigned to this class (or implicitly assigned because we added all raw
        // ones)
        List<Activity> activeActivities = rawActivities.stream()
                .filter(a -> a.getStatus() == null || "ACTIVE".equalsIgnoreCase(a.getStatus()))
                .filter(a -> !Boolean.TRUE.equals(a.getAttendanceEngineEnabled()))
                .filter(a -> ccAcademicYear == null || a.getAcademicYear() == null
                        || a.getAcademicYear() == ccAcademicYear)
                .collect(Collectors.toList());

        for (Activity activity : activeActivities) {
            adminAssignmentService.populateActivityTransientFields(activity, stageId);
        }

        return ResponseEntity.ok(ApiResponse.ok("Active activities fetched successfully", activeActivities));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCCClassDetails(String username) {
        User ccUser = validateAndGetCCUser(username);
        if (ccUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access Denied: Only Class Coordinators can access class details."));
        }

        if (ccUser.getDepartment() == null || ccUser.getSection() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Class Coordinator is not assigned to a Department and Section."));
        }

        String yearStr = ccUser.getYear() != null && !ccUser.getYear().trim().isEmpty()
                ? ccUser.getYear().trim()
                : "1";

        String yearDisplay = formatYearDisplay(yearStr);

        Map<String, Object> details = new HashMap<>();
        details.put("ccId", ccUser.getId());
        details.put("ccName", ccUser.getFullName());
        details.put("departmentId", ccUser.getDepartment().getId());
        details.put("departmentName", ccUser.getDepartment().getName());
        details.put("departmentCode", ccUser.getDepartment().getDeptCode());
        details.put("sectionId", ccUser.getSection().getId());
        details.put("sectionName", ccUser.getSection().getSectionName());
        details.put("year", yearStr);
        details.put("yearName", yearDisplay);

        return ResponseEntity.ok(ApiResponse.ok("CC Class Details loaded", details));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getClassStudents(String username, Long activityId) {
        return getClassStudents(username, activityId, null);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getClassStudents(String username, Long activityId,
            Long stageId) {
        User ccUser = validateAndGetCCUser(username);
        if (ccUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access Denied: Only Class Coordinators can view class students."));
        }

        if (ccUser.getDepartment() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Class Coordinator is not assigned to a Department."));
        }

        Long deptId = ccUser.getDepartment().getId();
        Long sectionId = ccUser.getSection() != null ? ccUser.getSection().getId() : null;
        String year = ccUser.getYear() != null && !ccUser.getYear().trim().isEmpty()
                ? ccUser.getYear().trim()
                : "1";

        int stageOrder = 0;
        if (stageId != null) {
            ActivityStage targetStage = activityStageRepository.findById(stageId).orElse(null);
            if (targetStage != null) {
                stageOrder = targetStage.getDisplayOrder();
            }
        }
        if (stageOrder == 0 && activityId != null) {
            Activity activity = activityRepository.findById(activityId).orElse(null);
            if (activity != null) {
                if (activity.getStage() != null) {
                    stageOrder = activity.getStage().getDisplayOrder();
                } else if (activity.getSubgroup() != null && activity.getSubgroup().getStage() != null) {
                    stageOrder = activity.getSubgroup().getStage().getDisplayOrder();
                }
            }
        }
        final int activityStageOrder = stageOrder;

        List<Student> students;
        if (sectionId != null) {
            if (activityStageOrder > 0) {
                students = studentRepository.findByDepartmentIdAndSectionIdAndStage(deptId, sectionId, activityStageOrder);
            } else {
                students = studentRepository.findByDepartmentIdAndSectionId(deptId, sectionId);
            }
        } else {
            if (activityStageOrder > 0) {
                students = studentRepository.findByDepartmentIdAndStage(deptId, activityStageOrder);
            } else {
                students = studentRepository.findByDepartmentId(deptId);
            }
        }

        List<Map<String, Object>> result = students.stream()
                .filter(Student::isActive)
                .filter(s -> isYearMatching(s.getYear(), year))
                .filter(s -> activityStageOrder <= 0 || s.getStage() == activityStageOrder
                        || s.getCurrentStage() == activityStageOrder)
                .sorted(Comparator
                        .comparing((Student s) -> s.getFullName() != null ? s.getFullName().trim() : "",
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing((Student s) -> s.getRegNo() != null ? s.getRegNo().trim() : "",
                                String.CASE_INSENSITIVE_ORDER))
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", s.getId());
                    map.put("regNo", s.getRegNo());
                    map.put("fullName", s.getFullName());
                    map.put("rollNo", s.getRegNo());
                    map.put("sprNo", s.getSprNo());
                    map.put("email", s.getEmail());
                    map.put("department", s.getDepartment() != null ? s.getDepartment().getName() : "");
                    map.put("section", s.getSection() != null ? s.getSection().getSectionName() : "");
                    map.put("year", s.getYear());
                    map.put("assigned", true);
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Class students fetched successfully", result));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> assignActivity(String username, Long activityId,
            CCActivityAssignRequest request) {
        User ccUser = validateAndGetCCUser(username);
        if (ccUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access Denied: Only Class Coordinators can assign activities."));
        }

        Department dept = ccUser.getDepartment();
        Section sec = ccUser.getSection();
        if (dept == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Class Coordinator is not assigned to a valid Department."));
        }

        String year = ccUser.getYear() != null && !ccUser.getYear().trim().isEmpty()
                ? ccUser.getYear().trim()
                : "1";

        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Activity not found."));
        }

        if (Boolean.TRUE.equals(activity.getAttendanceEngineEnabled())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Attendance Engine activities cannot be assigned manually."));
        }

        // ── Fetch any existing assignment first so we can use the execution stage ──
        // For activities reused across stages, the assignment's stage is the execution
        // stage.
        // activity.getStage() is the original creation stage and must not be used for
        // eligibility.
        List<ActivityAssignment> existingAssignments = activityAssignmentRepository.findByActivityId(activity.getId());
        ActivityAssignment existingAssignment = existingAssignments.stream()
                .filter(a -> a.getDepartment() != null && a.getDepartment().getId().equals(dept.getId())
                        && (sec == null || (a.getSection() != null && a.getSection().getId().equals(sec.getId())))
                        && (a.getTeacher() == null || a.getTeacher().getId().equals(ccUser.getId())))
                .findFirst()
                .orElse(null);

        // Resolve the stage to validate against: prefer existing assignment stage, fall
        // back to activity stage
        int stageOrder = 0;
        if (existingAssignment != null && existingAssignment.getStage() != null) {
            stageOrder = existingAssignment.getStage().getDisplayOrder();
        } else if (activity.getStage() != null) {
            stageOrder = activity.getStage().getDisplayOrder();
        } else if (activity.getSubgroup() != null && activity.getSubgroup().getStage() != null) {
            stageOrder = activity.getSubgroup().getStage().getDisplayOrder();
        }
        final int activityStageOrder = stageOrder;

        List<Student> assignedStudents;
        if (request != null && request.getStudentIds() != null && !request.getStudentIds().isEmpty()) {
            assignedStudents = studentRepository.findAllById(request.getStudentIds());
            for (Student s : assignedStudents) {
                boolean deptMatch = s.getDepartment() != null && s.getDepartment().getId().equals(dept.getId());
                boolean secMatch = sec == null || (s.getSection() != null && s.getSection().getId().equals(sec.getId()));
                if (!deptMatch || !secMatch) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(
                            "Security Violation: Student " + s.getRegNo() + " (" + s.getFullName()
                                    + ") does not belong to your assigned Class (" + dept.getName()
                                    + (sec != null ? " - Section " + sec.getSectionName() : "") + ")."));
                }
                int sStage = s.getCurrentStage() > 0 ? s.getCurrentStage() : (s.getStage() > 0 ? s.getStage() : 1);
                if (activityStageOrder > 0 && sStage != activityStageOrder) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(
                            "Student " + s.getFullName() + " (" + s.getRegNo() + ") is in Stage " + sStage
                                    + " and is not eligible for Stage " + activityStageOrder + " activities."));
                }
            }
        } else {
            List<Student> allClassStudents;
            if (sec != null) {
                allClassStudents = studentRepository.findByDepartmentIdAndSectionId(dept.getId(), sec.getId());
            } else {
                allClassStudents = studentRepository.findByDepartmentId(dept.getId());
            }
            assignedStudents = allClassStudents.stream()
                    .filter(Student::isActive)
                    .filter(s -> isYearMatching(s.getYear(), year))
                    .filter(s -> {
                        int sStage = s.getCurrentStage() > 0 ? s.getCurrentStage() : (s.getStage() > 0 ? s.getStage() : 1);
                        return activityStageOrder <= 0 || sStage == activityStageOrder;
                    })
                    .collect(Collectors.toList());
        }

        ActivityAssignment assignment = existingAssignment;
        if (assignment == null) {
            assignment = new ActivityAssignment();
            assignment.setActivity(activity);
            assignment.setDepartment(dept);
            assignment.setSection(sec);
            assignment.setTeacher(ccUser);
            assignment.setAssignmentScope(AssignmentScope.SECTION);
            assignment.setYear(year);
            assignment.setStage(activity.getStage());
            assignment.setAssignedBy(ccUser);
            assignment.setAssignedAt(LocalDateTime.now());
            assignment = activityAssignmentRepository.save(assignment);
        } else {
            assignment.setTeacher(ccUser);
            assignment.setAssignedBy(ccUser);
            assignment.setAssignedAt(LocalDateTime.now());
            assignment = activityAssignmentRepository.save(assignment);
        }

        log.info("========================================");
        log.info("CC Activity Assignment");
        log.info("CC ID            : {}", ccUser.getId());
        log.info("Department       : {}", dept.getName());
        log.info("Year             : {}", year);
        log.info("Section          : {}", sec != null ? sec.getSectionName() : "N/A");
        log.info("Activity ID      : {}", activity.getId());
        log.info("Students Assigned: {}", assignedStudents.size());
        log.info("Success          : true");
        log.info("========================================");

        Map<String, Object> result = new HashMap<>();
        result.put("assignmentId", assignment.getId());
        result.put("activityId", activity.getId());
        result.put("activityName", activity.getName() != null ? activity.getName() : activity.getActivityName());
        result.put("studentsAssigned", assignedStudents.size());
        result.put("department", dept.getName());
        result.put("year", year);
        result.put("section", sec != null ? sec.getSectionName() : "N/A");

        return ResponseEntity.ok(
                ApiResponse.ok("Activity successfully assigned to " + assignedStudents.size() + " students.", result));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getClassTeachers(String username) {
        User ccUser = validateAndGetCCUser(username);
        if (ccUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access Denied: Only Class Coordinators can access this endpoint."));
        }

        List<User> allTeachers = userRepository.findByRoleName("ROLE_TEACHER");
        List<Map<String, Object>> teachersList = allTeachers.stream()
                .filter(u -> u != null && u.isActive())
                .filter(u -> u.getRoles().stream().anyMatch(r -> "ROLE_TEACHER".equalsIgnoreCase(r.getName())))
                .filter(u -> u.getRoles().stream().noneMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getName()) ||
                        "ROLE_SUPER_ADMIN".equalsIgnoreCase(r.getName()) ||
                        "ROLE_STUDENT".equalsIgnoreCase(r.getName())))
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("username", u.getUsername());
                    map.put("fullName", u.getFullName());
                    map.put("email", u.getEmail());
                    map.put("phone", u.getPhone());
                    map.put("departmentId", u.getDepartment() != null ? u.getDepartment().getId() : null);
                    map.put("department", u.getDepartment() != null ? u.getDepartment().getName() : "");
                    map.put("departmentName", u.getDepartment() != null ? u.getDepartment().getName() : "");
                    map.put("sectionId", u.getSection() != null ? u.getSection().getId() : null);
                    map.put("sectionName", u.getSection() != null ? u.getSection().getSectionName() : "");
                    map.put("section", u.getSection() != null ? u.getSection().getSectionName() : "");
                    map.put("year", u.getYear() != null && !u.getYear().trim().isEmpty() ? u.getYear() : "");
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Class teachers fetched successfully", teachersList));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> assignTeacherToActivity(String username, Long activityId,
            CCTeacherAssignRequest request) {
        User ccUser = validateAndGetCCUser(username);
        if (ccUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access Denied: Only Class Coordinators can assign activities."));
        }

        Department dept = ccUser.getDepartment();
        Section sec = ccUser.getSection();
        if (dept == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Class Coordinator is not assigned to a valid Department."));
        }

        String year = ccUser.getYear() != null && !ccUser.getYear().trim().isEmpty()
                ? ccUser.getYear().trim()
                : "1";

        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Activity not found."));
        }

        if (Boolean.TRUE.equals(activity.getAttendanceEngineEnabled())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Attendance Engine activities cannot be assigned manually."));
        }

        if (request == null || request.getTeacherId() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Please select a teacher to assign."));
        }

        User teacher = userRepository.findById(request.getTeacherId()).orElse(null);
        if (teacher == null || !teacher.isActive()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Selected teacher not found or inactive."));
        }

        boolean isTeacher = teacher.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_TEACHER"));
        if (!isTeacher) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Selected user is not a teacher."));
        }

        Long stageId = request.getStageId();
        ActivityStage targetStage = null;
        if (stageId != null) {
            targetStage = activityStageRepository.findById(stageId).orElse(null);
        }
        if (targetStage == null && activity.getStage() != null) {
            targetStage = activity.getStage();
        }

        String duration = (request.getAssignmentDuration() != null && !request.getAssignmentDuration().trim().isEmpty())
                ? request.getAssignmentDuration().trim().toUpperCase()
                : "PERMANENT";

        LocalDate today = LocalDate.now();
        List<ActivityAssignment> existingAssignments = (stageId != null)
                ? activityAssignmentRepository.findByActivityIdAndStageId(activity.getId(), stageId)
                : activityAssignmentRepository.findByActivityId(activity.getId());

        ActivityAssignment existingPermanentAssignment = existingAssignments.stream()
                .filter(a -> a.getDepartment() != null && a.getDepartment().getId().equals(dept.getId())
                        && (sec == null || (a.getSection() != null && a.getSection().getId().equals(sec.getId()))))
                .findFirst()
                .orElse(null);

        Map<String, Object> result = new HashMap<>();

        if ("ONLY_TODAY".equals(duration) || "TEMPORARY".equals(duration)) {
            // Cancel any prior active temporary assignments for this activity/dept/sec
            // today
            List<ActivityTemporaryAssignment> priorTemp = activityTemporaryAssignmentRepository.findActiveAssignments(
                    activity.getId(), dept.getId(), sec != null ? sec.getId() : null, today);
            for (ActivityTemporaryAssignment p : priorTemp) {
                p.setStatus("CANCELLED");
                p.setUpdatedAt(LocalDateTime.now());
                activityTemporaryAssignmentRepository.save(p);
            }

            User origTeacher = existingPermanentAssignment != null ? existingPermanentAssignment.getTeacher() : null;

            ActivityTemporaryAssignment tempAssignment = new ActivityTemporaryAssignment();
            tempAssignment.setActivity(activity);
            tempAssignment.setStage(targetStage);
            tempAssignment.setDepartment(dept);
            tempAssignment.setSection(sec);
            tempAssignment.setYear(year);
            tempAssignment.setOriginalTeacher(origTeacher);
            tempAssignment.setTemporaryTeacher(teacher);
            tempAssignment.setAssignedBy(ccUser);
            tempAssignment.setAssignmentDate(today);
            tempAssignment.setExpiryDate(today);
            tempAssignment.setStatus("ACTIVE");
            tempAssignment.setRemarks(request.getRemarks());
            tempAssignment = activityTemporaryAssignmentRepository.save(tempAssignment);

            log.info("========================================");
            log.info("CC Temporary Teacher Assignment (Only Today)");
            log.info("CC ID               : {}", ccUser.getId());
            log.info("Department          : {}", dept.getName());
            log.info("Year                : {}", year);
            log.info("Section             : {}", (sec != null ? sec.getSectionName() : "None"));
            log.info("Activity ID         : {}", activity.getId());
            log.info("Temporary Teacher   : {} ({})", teacher.getFullName(), teacher.getId());
            log.info("Original Teacher    : {}", (origTeacher != null ? origTeacher.getFullName() : "None"));
            log.info("Expiry Date         : {}", today);
            log.info("Success             : true");
            log.info("========================================");

            result.put("temporaryAssignmentId", tempAssignment.getId());
            result.put("assignmentDuration", "ONLY_TODAY");
            result.put("activityId", activity.getId());
            result.put("activityName", activity.getName() != null ? activity.getName() : activity.getActivityName());
            result.put("teacherId", teacher.getId());
            result.put("teacherName", teacher.getFullName());
            result.put("originalTeacherId", origTeacher != null ? origTeacher.getId() : null);
            result.put("originalTeacherName", origTeacher != null ? origTeacher.getFullName() : null);
            result.put("assignmentDate", today.toString());
            result.put("expiryDate", today.toString());
            result.put("department", dept.getName());
            result.put("year", year);
            result.put("section", sec != null ? sec.getSectionName() : null);

            return ResponseEntity.ok(
                    ApiResponse.ok("Temporary assignment active for today: " + teacher.getFullName() + ".", result));
        } else {
            // Permanent Assignment
            // Cancel any active temporary assignments so permanent takes effect immediately
            List<ActivityTemporaryAssignment> priorTemp = activityTemporaryAssignmentRepository.findActiveAssignments(
                    activity.getId(), dept.getId(), sec != null ? sec.getId() : null, today);
            for (ActivityTemporaryAssignment p : priorTemp) {
                p.setStatus("CANCELLED");
                p.setUpdatedAt(LocalDateTime.now());
                activityTemporaryAssignmentRepository.save(p);
            }

            ActivityAssignment assignment = existingPermanentAssignment;
            if (assignment == null) {
                assignment = new ActivityAssignment();
                assignment.setActivity(activity);
                assignment.setDepartment(dept);
                assignment.setSection(sec);
                assignment.setTeacher(teacher);
                assignment.setAssignmentScope(AssignmentScope.SECTION);
                assignment.setYear(year);
                assignment.setStage(targetStage);
                assignment.setAssignedBy(ccUser);
                assignment.setAssignedAt(LocalDateTime.now());
            } else {
                assignment.setTeacher(teacher);
                assignment.setYear(year);
                if (targetStage != null) {
                    assignment.setStage(targetStage);
                }
                assignment.setAssignedBy(ccUser);
                assignment.setAssignedAt(LocalDateTime.now());
            }
            assignment = activityAssignmentRepository.save(assignment);

            log.info("========================================");
            log.info("CC Permanent Teacher Activity Assignment");
            log.info("CC ID            : {}", ccUser.getId());
            log.info("Department       : {}", dept.getName());
            log.info("Year             : {}", year);
            log.info("Section          : {}", (sec != null ? sec.getSectionName() : "None"));
            log.info("Activity ID      : {}", activity.getId());
            log.info("Teacher Assigned : {} ({})", teacher.getFullName(), teacher.getId());
            log.info("Success          : true");
            log.info("========================================");

            result.put("assignmentId", assignment.getId());
            result.put("assignmentDuration", "PERMANENT");
            result.put("activityId", activity.getId());
            result.put("activityName", activity.getName() != null ? activity.getName() : activity.getActivityName());
            result.put("teacherId", teacher.getId());
            result.put("teacherName", teacher.getFullName());
            result.put("department", dept.getName());
            result.put("year", year);
            result.put("section", sec != null ? sec.getSectionName() : null);

            return ResponseEntity.ok(ApiResponse.ok("Permanently assigned to " + teacher.getFullName() + ".", result));
        }
    }

    private String formatYearDisplay(String year) {
        if (year == null || year.trim().isEmpty())
            return "1st Year";
        String y = year.trim().toUpperCase();
        if (y.equals("1") || y.equals("I") || y.contains("1ST"))
            return "1st Year";
        if (y.equals("2") || y.equals("II") || y.contains("2ND"))
            return "2nd Year";
        if (y.equals("3") || y.equals("III") || y.contains("3RD"))
            return "3rd Year";
        if (y.equals("4") || y.equals("IV") || y.contains("4TH"))
            return "4th Year";
        return year;
    }

    private boolean isYearMatching(String studentYear, String ccYear) {
        if (studentYear == null || ccYear == null)
            return true;
        String s = studentYear.trim().toUpperCase();
        String c = ccYear.trim().toUpperCase();
        if (s.equals(c))
            return true;
        if ((s.equals("1") || s.equals("I") || s.contains("1ST"))
                && (c.equals("1") || c.equals("I") || c.contains("1ST")))
            return true;
        if ((s.equals("2") || s.equals("II") || s.contains("2ND"))
                && (c.equals("2") || c.equals("II") || c.contains("2ND")))
            return true;
        if ((s.equals("3") || s.equals("III") || s.contains("3RD"))
                && (c.equals("3") || c.equals("III") || c.contains("3RD")))
            return true;
        if ((s.equals("4") || s.equals("IV") || s.contains("4TH"))
                && (c.equals("4") || c.equals("IV") || c.contains("4TH")))
            return true;
        return false;
    }
}
