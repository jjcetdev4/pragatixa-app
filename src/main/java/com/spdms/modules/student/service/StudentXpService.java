package com.pragatix.modules.student.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.dto.AwardXpRequest;
import com.pragatix.entity.Activity;
import com.pragatix.entity.ActivityAssignment;
import com.pragatix.entity.AssignmentScope;
import com.pragatix.entity.Student;
import com.pragatix.entity.User;
import com.pragatix.repository.ActivityAssignmentRepository;
import com.pragatix.modules.activity.repository.ActivityRepository;
import com.pragatix.modules.activity.service.AssignmentSecurityService;
import com.pragatix.modules.authentication.repository.UserRepository;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.repository.PenaltyRequestRepository;
import com.pragatix.entity.PenaltyRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudentXpService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final StudentRepository studentRepository;
    private final AssignmentSecurityService assignmentSecurityService;
    private final StudentXpValidator validator;
    private final XpEngineService xpEngineService;
    private final PenaltyRequestRepository penaltyRequestRepository;

    public StudentXpService(UserRepository userRepository,
            ActivityRepository activityRepository,
            ActivityAssignmentRepository activityAssignmentRepository,
            StudentRepository studentRepository,
            AssignmentSecurityService assignmentSecurityService,
            StudentXpValidator validator,
            XpEngineService xpEngineService,
            PenaltyRequestRepository penaltyRequestRepository) {
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.studentRepository = studentRepository;
        this.assignmentSecurityService = assignmentSecurityService;
        this.validator = validator;
        this.xpEngineService = xpEngineService;
        this.penaltyRequestRepository = penaltyRequestRepository;
    }

    public com.pragatix.entity.ActivityAssignment findMatchingAssignmentForStudent(
            java.util.List<com.pragatix.entity.ActivityAssignment> matching, com.pragatix.entity.Student student) {
        if (student == null)
            return null;
        for (com.pragatix.entity.ActivityAssignment a : matching) {
            com.pragatix.entity.AssignmentScope scope = a.getAssignmentScope();
            if (scope == com.pragatix.entity.AssignmentScope.GLOBAL
                    || scope == com.pragatix.entity.AssignmentScope.SPECIFIC_FACULTY) {
                return a;
            }
            if (scope == com.pragatix.entity.AssignmentScope.DEPARTMENT &&
                    student.getDepartment() != null && a.getDepartment() != null &&
                    student.getDepartment().getId().equals(a.getDepartment().getId())) {
                return a;
            }
            if (scope == com.pragatix.entity.AssignmentScope.SECTION &&
                    student.getSection() != null && a.getSection() != null &&
                    student.getSection().getId().equals(a.getSection().getId())) {
                return a;
            }
        }
        return null;
    }

    @org.springframework.transaction.annotation.Transactional
    public org.springframework.http.ResponseEntity<com.pragatix.common.response.ApiResponse<Void>> awardStudentXp(
            com.pragatix.dto.AwardXpRequest request, String username) {
        com.pragatix.entity.User teacher = userRepository.findByUsername(username).orElse(null);
        if (teacher == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<Void>error("Teacher profile not found"));

        Student student = studentRepository.findById(request.getRegNo()).orElse(null);
        if (student == null)
            return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Student not found"));

        Activity activity = activityRepository.findById(request.getActivityId()).orElse(null);
        if (activity == null)
            return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Activity not found"));

        if (activity.getStage() != null && activity.getStage().getStatus() != com.pragatix.enums.StageStatus.ACTIVE) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>error("Cannot award XP for an activity in a non-active stage."));
        }

        if (activity.getSubgroup() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>error("Subgroup not found for Activity " + activity.getId()));
        }

        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityId(activity.getId());
        List<ActivityAssignment> matching = allAssignments.stream()
                .filter(a -> assignmentSecurityService.isUserAssignedFaculty(a, teacher))
                .collect(Collectors.toList());

        ActivityAssignment assignment = findMatchingAssignmentForStudent(matching, student);
        if (assignment == null)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.<Void>error(
                    "Access Denied: You are not authorized to award XP to this student for this activity."));

        int calculatedXp = calculateXpToAward(activity, request.getResult());
        int xpToAward = calculatedXp;
        if (calculatedXp > 0 && request.getXp() > 0) {
            xpToAward = request.getXp();
        }

        String limitError = validator.checkAwardLimit(student, activity);
        if (limitError != null)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.<Void>error(limitError));

        xpEngineService.awardXp(student, activity, teacher, assignment, xpToAward, request.getRemarks());
        return ResponseEntity.ok(ApiResponse.ok("XP points awarded successfully", null));
    }

    @Transactional
    public ResponseEntity<?> awardStudentXpBatch(AwardXpRequest request, String username) {
        Activity activity = null;
        int xpToAward = 0;
        try {
            User teacher = userRepository.findByUsername(username).orElse(null);
            if (teacher == null)
                return buildErrorResponse("Teacher profile not found", request, null, 0);

            activity = activityRepository.findById(request.getActivityId()).orElse(null);
            if (activity == null)
                return buildErrorResponse("Activity not found", request, null, 0);

            if (activity.getStage() != null
                    && activity.getStage().getStatus() != com.pragatix.enums.StageStatus.ACTIVE) {
                return buildErrorResponse("Cannot award XP for an activity in a non-active stage.", request, activity,
                        0);
            }

            if (activity.getSubgroup() == null) {
                return buildErrorResponse("Subgroup not found for Activity " + activity.getId(), request, activity, 0);
            }

            List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityId(activity.getId());
            List<ActivityAssignment> matching = allAssignments.stream()
                    .filter(a -> assignmentSecurityService.isUserAssignedFaculty(a, teacher))
                    .collect(Collectors.toList());

            int calculatedXp = calculateXpToAward(activity, request.getResult());
            xpToAward = calculatedXp;
            if (calculatedXp > 0 && request.getXp() > 0) {
                xpToAward = request.getXp();
            }

            List<Long> studentIds = request.getStudentIds();
            if (studentIds == null || studentIds.isEmpty()) {
                if (request.getRegNo() != null) {
                    studentIds = List.of(request.getRegNo());
                } else {
                    return buildErrorResponse("At least one student must be selected", request, activity, xpToAward);
                }
            }

            List<Student> students = studentRepository.findAllById(studentIds);
            Map<Long, Student> studentMap = students.stream().collect(Collectors.toMap(Student::getId, s -> s));

            List<String> errors = new ArrayList<>();
            int successCount = 0;

            for (Long regNo : studentIds) {
                System.out.println("SERVICE DEBUG: Processing studentId: " + regNo);
                Student student = studentMap.get(regNo);
                if (student == null) {
                    errors.add("Student " + regNo + " not found");
                    continue;
                }

                ActivityAssignment assignment = findMatchingAssignmentForStudent(matching, student);
                if (assignment == null) {
                    errors.add("Access Denied: You are not authorized to award XP to student " + student.getFullName());
                    continue;
                }

                String limitError = validator.checkAwardLimit(student, activity);
                if (limitError != null) {
                    errors.add(limitError);
                    continue;
                }

                boolean isPenalty = (activity.getPenaltyEnabled() != null && activity.getPenaltyEnabled())
                        || "Penalty".equalsIgnoreCase(activity.getXpType());

                if (isPenalty) {
                    boolean hasCcRole = teacher.getSubRoles().stream()
                            .anyMatch(sr -> "CC".equalsIgnoreCase(sr.getName()));
                    boolean sameSection = teacher.getSection() != null && student.getSection() != null
                            && teacher.getSection().getId().equals(student.getSection().getId());
                    boolean sameDepartment = teacher.getDepartment() != null && student.getDepartment() != null
                            && teacher.getDepartment().getId().equals(student.getDepartment().getId());
                    boolean isCc = hasCcRole && sameSection && sameDepartment;

                    int configuredPenalty = 0;
                    if (activity.getPenaltyXp() != null && activity.getPenaltyXp() != 0) {
                        configuredPenalty = Math.abs(activity.getPenaltyXp());
                    } else if (activity.getAwardXp() != null && activity.getAwardXp() != 0) {
                        configuredPenalty = Math.abs(activity.getAwardXp());
                    } else {
                        configuredPenalty = Math.abs(xpToAward);
                    }

                    PenaltyRequest penaltyRequest = new PenaltyRequest();
                    penaltyRequest.setStudent(student);
                    penaltyRequest.setTeacher(teacher);
                    penaltyRequest.setTeacherName(teacher.getFullName());
                    penaltyRequest.setActivity(activity);
                    penaltyRequest.setActivityName(activity.getActivityName());
                    penaltyRequest.setPenaltyXP(configuredPenalty);
                    penaltyRequest
                            .setReason(request.getRemarks() != null ? request.getRemarks() : "No reason provided");

                    if (isCc) {
                        penaltyRequest.setStatus("AUTO_APPROVED");
                        penaltyRequest.setApprovedAt(LocalDateTime.now());
                        penaltyRequest.setApprovedBy(teacher.getFullName());

                        System.out.println("Activity ID: " + activity.getId());
                        System.out.println("Activity XP: " + activity.getPenaltyXp());
                        System.out.println("PenaltyRequest.penaltyXp: " + penaltyRequest.getPenaltyXP());
                        penaltyRequestRepository.save(penaltyRequest);

                        System.out.println("Teacher ID : " + teacher.getId());
                        System.out.println("Student CC : " + teacher.getId());
                        System.out.println("Teacher Is CC : TRUE");
                        System.out.println("Penalty Request Saved");
                        System.out.println("Status : AUTO_APPROVED");
                        System.out.println("XP Engine : EXECUTED");

                        xpEngineService.awardXp(student, activity, teacher, assignment, xpToAward,
                                request.getRemarks());
                    } else {
                        penaltyRequest.setStatus("PENDING");
                        User cc = userRepository.findAll().stream()
                                .filter(u -> u.getSubRoles().stream()
                                        .anyMatch(sr -> "CC".equalsIgnoreCase(sr.getName())))
                                .filter(u -> u.getSection() != null && student.getSection() != null
                                        && u.getSection().getId().equals(student.getSection().getId()))
                                .filter(u -> u.getDepartment() != null && student.getDepartment() != null
                                        && u.getDepartment().getId().equals(student.getDepartment().getId()))
                                .filter(User::isActive)
                                .findFirst()
                                .orElse(null);

                        if (cc != null) {
                            penaltyRequest.setCc(cc);
                            penaltyRequest.setCcName(cc.getFullName());
                        }

                        System.out.println("Activity ID: " + activity.getId());
                        System.out.println("Activity XP: " + activity.getPenaltyXp());
                        System.out.println("PenaltyRequest.penaltyXp: " + penaltyRequest.getPenaltyXP());
                        penaltyRequestRepository.save(penaltyRequest);

                        System.out.println("Teacher ID : " + teacher.getId());
                        System.out.println("Student CC : " + (cc != null ? cc.getId() : "null"));
                        System.out.println("Teacher Is CC : FALSE");
                        System.out.println("Penalty Request Saved");
                        System.out.println("Status : PENDING");
                        System.out.println("XP Engine : SKIPPED");
                    }
                } else {
                    xpEngineService.awardXp(student, activity, teacher, assignment, xpToAward, request.getRemarks());
                }
                successCount++;
            }

            if (!errors.isEmpty()) {
                if (successCount == 0) {
                    return buildErrorResponse(String.join(" | ", errors), request, activity, xpToAward);
                } else {
                    return ResponseEntity.ok(ApiResponse.ok(
                            "XP awarded to " + successCount + " students. Errors: " + String.join(" | ", errors),
                            null));
                }
            }
            return ResponseEntity
                    .ok(ApiResponse.ok("XP points awarded successfully to " + successCount + " students", null));
        } catch (Exception e) {
            logError(e, "Unexpected Exception", request, activity, xpToAward);
            return buildErrorResponse("Internal server error: " + e.getMessage(), request, activity, xpToAward);
        }
    }

    private ResponseEntity<?> buildErrorResponse(String message, AwardXpRequest request, Activity activity,
            int xpToAward) {
        logError(null, message, request, activity, xpToAward);
        java.util.Map<String, Object> errorRes = new java.util.LinkedHashMap<>();
        errorRes.put("status", 400);
        errorRes.put("error", "Bad Request");
        errorRes.put("message", message);
        errorRes.put("path", "/api/v1/student-xp/award/batch");
        errorRes.put("timestamp", java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
    }

    private void logError(Exception e, String message, AwardXpRequest request, Activity activity, int xpToAward) {
        System.err.println("=== XP AWARD BATCH ERROR ===");
        if (e != null) {
            System.err.println("Exception type: " + e.getClass().getName());
            System.err.println("Exception message: " + e.getMessage());
            System.err.println("Full stack trace:");
            e.printStackTrace();
        } else {
            System.err.println("Exception type: Validation Error");
            System.err.println("Exception message: " + message);
            System.err.println("Full stack trace: N/A");
        }
        System.err.println("Student ID: " + (request != null ? request.getStudentIds() : "null"));
        System.err.println("Activity ID: " + (request != null ? request.getActivityId() : "null"));
        System.err.println(
                "Activity XP: " + (activity != null ? activity.getAwardXp() + "/" + activity.getPenaltyXp() : "N/A"));
        System.err.println("Penalty flag: "
                + (activity != null ? (activity.getPenaltyEnabled() != null && activity.getPenaltyEnabled()) : "N/A"));
        try {
            System.err.println("Request payload: "
                    + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request));
        } catch (Exception ex) {
            System.err.println("Request payload: " + request);
        }
        System.err.println("Calculated applied XP: " + xpToAward);
        System.err.println("Current request status: PENDING");
        System.err.println("============================");
    }

    private int calculateXpToAward(Activity activity, String resultStr) {
        int xpToAward = 0;
        Boolean isAward = activity.getAwardEnabled();
        Boolean isPenalty = activity.getPenaltyEnabled();

        if (isAward == null && isPenalty == null) {
            if ("Penalty".equalsIgnoreCase(activity.getXpType())) {
                isAward = false;
                isPenalty = true;
            } else {
                isAward = true;
                isPenalty = false;
            }
        }

        boolean awardable = Boolean.TRUE.equals(isAward);
        boolean penalizable = Boolean.TRUE.equals(isPenalty);

        if (penalizable && !awardable) {
            int px = activity.getPenaltyXp() != null ? activity.getPenaltyXp()
                    : (activity.getAwardXp() != null ? activity.getAwardXp() : 0);
            xpToAward = -Math.abs(px);
        } else if (awardable && !penalizable) {
            xpToAward = activity.getAwardXp() != null ? activity.getAwardXp() : 0;
        } else {
            if ("FAIL".equalsIgnoreCase(resultStr)) {
                int px = activity.getPenaltyXp() != null ? activity.getPenaltyXp()
                        : (activity.getAwardXp() != null ? activity.getAwardXp() : 0);
                xpToAward = -Math.abs(px);
            } else {
                xpToAward = activity.getAwardXp() != null ? activity.getAwardXp() : 0;
            }
        }

        return xpToAward;
    }
}
