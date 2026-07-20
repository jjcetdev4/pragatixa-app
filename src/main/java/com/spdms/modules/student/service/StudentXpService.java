package com.spdms.modules.student.service;

import com.spdms.common.response.ApiResponse;
import com.spdms.dto.AwardXpRequest;
import com.spdms.entity.Activity;
import com.spdms.entity.ActivityAssignment;
import com.spdms.entity.AssignmentScope;
import com.spdms.entity.Student;
import com.spdms.entity.StudentActivityXp;
import com.spdms.entity.User;
import com.spdms.entity.XpTransaction;
import com.spdms.repository.ActivityAssignmentRepository;
import com.spdms.modules.activity.repository.ActivityRepository;
import com.spdms.modules.activity.service.AssignmentSecurityService;
import com.spdms.modules.authentication.repository.UserRepository;
import com.spdms.modules.student.repository.StudentActivityXpRepository;
import com.spdms.modules.student.repository.StudentRepository;
import com.spdms.repository.XpTransactionRepository;
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
    private final StudentActivityXpRepository studentActivityXpRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final AssignmentSecurityService assignmentSecurityService;
    private final StudentXpValidator validator;
    private final com.spdms.admin.service.CaptainSelectionService captainSelectionService;

    public StudentXpService(UserRepository userRepository,
                            ActivityRepository activityRepository,
                            ActivityAssignmentRepository activityAssignmentRepository,
                            StudentRepository studentRepository,
                            StudentActivityXpRepository studentActivityXpRepository,
                            XpTransactionRepository xpTransactionRepository,
                            AssignmentSecurityService assignmentSecurityService,
                            StudentXpValidator validator,
                            com.spdms.admin.service.CaptainSelectionService captainSelectionService) {
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.studentRepository = studentRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.xpTransactionRepository = xpTransactionRepository;
        this.assignmentSecurityService = assignmentSecurityService;
        this.validator = validator;
        this.captainSelectionService = captainSelectionService;
    }

    public ActivityAssignment findMatchingAssignmentForStudent(List<ActivityAssignment> matching, Student student) {
        if (student == null) return null;
        for (ActivityAssignment a : matching) {
            if (a.getAssignmentScope() == AssignmentScope.GLOBAL) return a;
            if (student.getDepartment() != null && a.getDepartment() != null 
                    && student.getDepartment().getId().equals(a.getDepartment().getId())) {
                if (a.getAssignmentScope() == AssignmentScope.DEPARTMENT) return a;
                if (student.getSection() != null && a.getSection() != null 
                        && student.getSection().getId().equals(a.getSection().getId())) {
                    return a;
                }
            }
        }
        return null;
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> awardStudentXp(AwardXpRequest request, String username) {
        User teacher = userRepository.findByUsername(username).orElse(null);
        if (teacher == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.<Void>error("Teacher profile not found"));

        Student student = studentRepository.findById(request.getRegNo()).orElse(null);
        if (student == null) return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Student not found"));

        Activity activity = activityRepository.findById(request.getActivityId()).orElse(null);
        if (activity == null) return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Activity not found"));
        
        if (activity.getStage() != null && activity.getStage().getStatus() != com.spdms.enums.StageStatus.ACTIVE) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Cannot award XP for an activity in a non-active stage."));
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

        saveXpRecords(student, activity, teacher, assignment, request.getResult(), xpToAward, request.getRemarks());
        return ResponseEntity.ok(ApiResponse.ok("XP points awarded successfully", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> awardStudentXpBatch(AwardXpRequest request, String username) {
        User teacher = userRepository.findByUsername(username).orElse(null);
        if (teacher == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.<Void>error("Teacher profile not found"));

        Activity activity = activityRepository.findById(request.getActivityId()).orElse(null);
        if (activity == null) return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Activity not found"));
        
        if (activity.getStage() != null && activity.getStage().getStatus() != com.spdms.enums.StageStatus.ACTIVE) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Cannot award XP for an activity in a non-active stage."));
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

        List<String> errors = new ArrayList<>();
        int successCount = 0;

        List<StudentActivityXp> activityXpsToSave = new ArrayList<>(studentIds.size());
        List<XpTransaction> txsToSave = new ArrayList<>(studentIds.size());
        List<Student> studentsToUpdate = new ArrayList<>(studentIds.size());

        Map<Long, Student> studentMap = studentRepository.findAllById(studentIds)
            .stream().collect(Collectors.toMap(Student::getId, s -> s));

        for (Long regNo : studentIds) {
            Student student = studentMap.get(regNo);
            if (student == null) {
                errors.add("Student ID " + regNo + " not found");
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

            student.setTotalXp(student.getTotalXp() + xpToAward);
            student.setScore(student.getScore() + xpToAward);
            
            captainSelectionService.evaluateCaptainPromotion(student);

            studentsToUpdate.add(student);

            StudentActivityXp record = new StudentActivityXp(
                student, activity, teacher, assignment, xpToAward, request.getRemarks() != null ? request.getRemarks() : "", request.getResult(), LocalDateTime.now()
            );
            activityXpsToSave.add(record);

            XpTransaction tx = XpTransaction.builder()
                .student(student)
                .activity(activity)
                .category(activity.getXpCategory() != null ? activity.getXpCategory().toUpperCase() : "SKILL")
                .activityName(activity.getName() + " (" + request.getResult() + " - Awarded by " + teacher.getFullName() + ")")
                .xpPoints(xpToAward)
                .submittedAt(LocalDateTime.now())
                .status("APPROVED")
                .approvedBy(teacher.getFullName())
                .isPenalty(xpToAward < 0)
                .capApplied(false)
                .build();
            txsToSave.add(tx);

            successCount++;
        }

        if (!activityXpsToSave.isEmpty()) {
            studentActivityXpRepository.saveAll(activityXpsToSave);
            xpTransactionRepository.saveAll(txsToSave);
            studentRepository.saveAll(studentsToUpdate);
        }

        if (successCount == 0 && !errors.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>error(String.join(", ", errors)));
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

    private void saveXpRecords(Student student, Activity activity, User teacher, ActivityAssignment assignment, String resultStr, int xpToAward, String remarks) {
        StudentActivityXp record = new StudentActivityXp(
            student, activity, teacher, assignment, xpToAward, remarks != null ? remarks : "", resultStr, LocalDateTime.now()
        );
        studentActivityXpRepository.save(record);

        student.setTotalXp(student.getTotalXp() + xpToAward);
        student.setScore(student.getScore() + xpToAward);
        
        captainSelectionService.evaluateCaptainPromotion(student);

        studentRepository.save(student);

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
    }
}
