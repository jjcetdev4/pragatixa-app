package com.spdms.modules.student.service;

import com.spdms.common.response.ApiResponse;
import com.spdms.dto.AwardXpRequest;
import com.spdms.entity.Activity;
import com.spdms.entity.ActivityAssignment;
import com.spdms.entity.AssignmentScope;
import com.spdms.entity.Student;
import com.spdms.entity.User;
import com.spdms.repository.ActivityAssignmentRepository;
import com.spdms.modules.activity.repository.ActivityRepository;
import com.spdms.modules.activity.service.AssignmentSecurityService;
import com.spdms.modules.authentication.repository.UserRepository;
import com.spdms.modules.student.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public StudentXpService(UserRepository userRepository,
                            ActivityRepository activityRepository,
                            ActivityAssignmentRepository activityAssignmentRepository,
                            StudentRepository studentRepository,
                            AssignmentSecurityService assignmentSecurityService,
                            StudentXpValidator validator,
                            XpEngineService xpEngineService) {
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.studentRepository = studentRepository;
        this.assignmentSecurityService = assignmentSecurityService;
        this.validator = validator;
        this.xpEngineService = xpEngineService;
    }

    public com.spdms.entity.ActivityAssignment findMatchingAssignmentForStudent(java.util.List<com.spdms.entity.ActivityAssignment> matching, com.spdms.entity.Student student) {
        if (student == null) return null;
        for (com.spdms.entity.ActivityAssignment a : matching) {
            com.spdms.entity.AssignmentScope scope = a.getAssignmentScope();
            if (scope == com.spdms.entity.AssignmentScope.GLOBAL || scope == com.spdms.entity.AssignmentScope.SPECIFIC_FACULTY) {
                return a;
            }
            if (scope == com.spdms.entity.AssignmentScope.DEPARTMENT && 
                student.getDepartment() != null && a.getDepartment() != null && 
                student.getDepartment().getId().equals(a.getDepartment().getId())) {
                return a;
            }
            if (scope == com.spdms.entity.AssignmentScope.SECTION && 
                student.getSection() != null && a.getSection() != null && 
                student.getSection().getId().equals(a.getSection().getId())) {
                return a;
            }
        }
        return null;
    }

    @org.springframework.transaction.annotation.Transactional
    public org.springframework.http.ResponseEntity<com.spdms.common.response.ApiResponse<Void>> awardStudentXp(com.spdms.dto.AwardXpRequest request, String username) {
        com.spdms.entity.User teacher = userRepository.findByUsername(username).orElse(null);
        if (teacher == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.<Void>error("Teacher profile not found"));

        Student student = studentRepository.findById(request.getRegNo()).orElse(null);
        if (student == null) return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Student not found"));

        Activity activity = activityRepository.findById(request.getActivityId()).orElse(null);
        if (activity == null) return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Activity not found"));
        
        if (activity.getStage() != null && activity.getStage().getStatus() != com.spdms.enums.StageStatus.ACTIVE) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Cannot award XP for an activity in a non-active stage."));
        }

        if (activity.getSubgroup() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Subgroup not found for Activity " + activity.getId()));
        }

        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityId(activity.getId());
        List<ActivityAssignment> matching = allAssignments.stream()
            .filter(a -> assignmentSecurityService.isUserAssignedFaculty(a, teacher))
            .collect(Collectors.toList());

        ActivityAssignment assignment = findMatchingAssignmentForStudent(matching, student);
        if (assignment == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.<Void>error("Access Denied: You are not authorized to award XP to this student for this activity."));

        int calculatedXp = calculateXpToAward(activity, request.getResult());
        int xpToAward = request.getXp() != 0 ? request.getXp() : calculatedXp;

        String limitError = validator.checkAwardLimit(student, activity);
        if (limitError != null) return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.<Void>error(limitError));

        xpEngineService.awardXp(student, activity, teacher, assignment, xpToAward, request.getRemarks());
        return ResponseEntity.ok(ApiResponse.ok("XP points awarded successfully", null));
    }

    @Transactional
    public ResponseEntity<?> awardStudentXpBatch(AwardXpRequest request, String username) {
        User teacher = userRepository.findByUsername(username).orElse(null);
        if (teacher == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.<Void>error("Teacher profile not found"));

        Activity activity = activityRepository.findById(request.getActivityId()).orElse(null);
        if (activity == null) return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Activity not found"));

        if (activity.getStage() != null && activity.getStage().getStatus() != com.spdms.enums.StageStatus.ACTIVE) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Cannot award XP for an activity in a non-active stage."));
        }

        if (activity.getSubgroup() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Subgroup not found for Activity " + activity.getId()));
        }

        List<ActivityAssignment> allAssignments = activityAssignmentRepository.findByActivityId(activity.getId());
        List<ActivityAssignment> matching = allAssignments.stream()
            .filter(a -> assignmentSecurityService.isUserAssignedFaculty(a, teacher))
            .collect(Collectors.toList());

        int calculatedXp = calculateXpToAward(activity, request.getResult());
        int xpToAward = request.getXp() != 0 ? request.getXp() : calculatedXp;

        List<Long> studentIds = request.getStudentIds();
        if (studentIds == null || studentIds.isEmpty()) {
            if (request.getRegNo() != null) {
                studentIds = List.of(request.getRegNo());
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.<Void>error("At least one student must be selected"));
            }
        }

        List<Student> students = studentRepository.findAllById(studentIds);
        Map<Long, Student> studentMap = students.stream().collect(Collectors.toMap(Student::getId, s -> s));

        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (Long regNo : studentIds) {
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

            xpEngineService.awardXp(student, activity, teacher, assignment, xpToAward, request.getRemarks());
            successCount++;
        }

        if (!errors.isEmpty()) {
            if (successCount == 0) {
                java.util.Map<String, Object> errorRes = new java.util.HashMap<>();
                errorRes.put("success", false);
                errorRes.put("errors", errors);
                return ResponseEntity.badRequest().body(errorRes);
            } else {
                return ResponseEntity.ok(ApiResponse.ok("XP awarded to " + successCount + " students. Errors: " + String.join(" | ", errors), null));
            }
        }
        return ResponseEntity.ok(ApiResponse.ok("XP points awarded successfully to " + successCount + " students", null));
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
            int px = activity.getPenaltyXp() != null ? activity.getPenaltyXp() : (activity.getAwardXp() != null ? activity.getAwardXp() : 0);
            xpToAward = -Math.abs(px);
        } else if (awardable && !penalizable) {
            xpToAward = activity.getAwardXp() != null ? activity.getAwardXp() : 0;
        } else {
            if ("FAIL".equalsIgnoreCase(resultStr)) {
                int px = activity.getPenaltyXp() != null ? activity.getPenaltyXp() : (activity.getAwardXp() != null ? activity.getAwardXp() : 0);
                xpToAward = -Math.abs(px);
            } else {
                xpToAward = activity.getAwardXp() != null ? activity.getAwardXp() : 0;
            }
        }
        
        return xpToAward;
    }
}
