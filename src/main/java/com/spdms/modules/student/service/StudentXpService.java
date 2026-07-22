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
    private final com.spdms.modules.activity.repository.ActivityStageRepository activityStageRepository;
    private final com.spdms.modules.activity.service.StageValidationService stageValidationService;
    private final TeamAssignmentService teamAssignmentService;
    private final com.spdms.student.XpCommandService xpCommandService;

    public StudentXpService(UserRepository userRepository,
                            ActivityRepository activityRepository,
                            ActivityAssignmentRepository activityAssignmentRepository,
                            StudentRepository studentRepository,
                            StudentActivityXpRepository studentActivityXpRepository,
                            XpTransactionRepository xpTransactionRepository,
                            AssignmentSecurityService assignmentSecurityService,
                            StudentXpValidator validator,
                            com.spdms.admin.service.CaptainSelectionService captainSelectionService,
                            com.spdms.modules.activity.repository.ActivityStageRepository activityStageRepository,
                            com.spdms.modules.activity.service.StageValidationService stageValidationService,
                            TeamAssignmentService teamAssignmentService,
                            com.spdms.student.XpCommandService xpCommandService) {
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.studentRepository = studentRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.xpTransactionRepository = xpTransactionRepository;
        this.assignmentSecurityService = assignmentSecurityService;
        this.validator = validator;
        this.captainSelectionService = captainSelectionService;
        this.activityStageRepository = activityStageRepository;
        this.stageValidationService = stageValidationService;
        this.teamAssignmentService = teamAssignmentService;
        this.xpCommandService = xpCommandService;
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

        saveXpRecords(student, activity, teacher, assignment, request.getResult(), xpToAward, request.getRemarks());
        return ResponseEntity.ok(ApiResponse.ok("XP points awarded successfully", null));
    }

    @Transactional
        public ResponseEntity<?> awardStudentXpBatch(AwardXpRequest request, String username) {
        User teacher = userRepository.findByUsername(username).orElse(null);
        if (teacher == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.<Void>error("Teacher profile not found"));

        Activity activity = activityRepository.findById(request.getActivityId()).orElse(null);
        if (activity == null) return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Activity not found"));
        
        System.out.println("Teacher username: " + username);
        System.out.println("Teacher ID: " + teacher.getId());
        System.out.println("Activity ID: " + activity.getId());
        System.out.println("Activity Name: " + activity.getName());
        System.out.println("Activity Stage: " + (activity.getStage() != null ? activity.getStage().getName() : "null"));
        System.out.println("Activity Subgroup: " + (activity.getSubgroup() != null ? activity.getSubgroup().getName() : "null"));
        System.out.println("Activity Category: " + (activity.getSubgroup() != null ? activity.getSubgroup().getCategory() : "null"));
        System.out.println("StudentIds: " + request.getStudentIds());
        System.out.println("XP: " + request.getXp());
        System.out.println("Result: " + request.getResult());
        System.out.println("Remarks: " + request.getRemarks());

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

        List<StudentActivityXp> activityXpsToSave = new ArrayList<>();
        List<XpTransaction> txsToSave = new ArrayList<>();
        List<Student> studentsToUpdate = new ArrayList<>();

        for (Long regNo : studentIds) {
            Student student = studentMap.get(regNo);
            if (student == null) {
                String e = "Student " + regNo + " not found";
                System.out.println("ERROR ADDED: " + e);
                errors.add(e);
                continue;
            }

            System.out.println("Student ID: " + student.getId());
            System.out.println("Student Name: " + student.getFullName());
            System.out.println("Department: " + (student.getDepartment() != null ? student.getDepartment().getName() : "null"));
            System.out.println("Section: " + (student.getSection() != null ? student.getSection().getSectionName() : "null"));
            System.out.println("Current Stage: " + student.getCurrentStage());
            System.out.println("Current Score: " + student.getScore());
            System.out.println("Must XP: " + student.getMustXp());
            System.out.println("Individual XP: " + student.getIndividualXp());
            System.out.println("Group XP: " + student.getGroupXp());

            System.out.println("BEFORE findMatchingAssignmentForStudent()");
            System.out.println("matching assignments count: " + matching.size());
            for (ActivityAssignment a : matching) {
                System.out.println("Assignment ID: " + a.getId());
                System.out.println("Assignment Scope: " + a.getAssignmentScope());
                System.out.println("Department: " + (a.getDepartment() != null ? a.getDepartment().getName() : "null"));
                System.out.println("Section: " + (a.getSection() != null ? a.getSection().getSectionName() : "null"));
                System.out.println("Assigned Teacher: " + (a.getTeacher() != null ? a.getTeacher().getFullName() : "null"));
            }

            ActivityAssignment assignment = findMatchingAssignmentForStudent(matching, student);
            System.out.println("Assignment Found = " + (assignment != null ? "TRUE" : "FALSE"));

            if (assignment == null) {
                String e = "Access Denied: You are not authorized to award XP to student " + student.getFullName();
                System.out.println("ERROR ADDED: " + e);
                errors.add(e);
                continue;
            }

            System.out.println("BEFORE validator.checkAwardLimit()");
            System.out.println("Student: " + student.getFullName());
            System.out.println("Activity: " + activity.getName());
            System.out.println("Award XP: " + xpToAward);

            String limitError = validator.checkAwardLimit(student, activity);
            System.out.println("Validator returned: " + (limitError != null ? limitError : "NULL"));
            
            if (limitError != null) {
                System.out.println("ERROR ADDED: " + limitError);
                errors.add(limitError);
                continue;
            }

            student.setTotalXp(student.getTotalXp() + xpToAward);
            student.setScore(student.getScore() + xpToAward);
            
            com.spdms.entity.ActivitySubgroup subgroup = activity.getSubgroup();
            if (subgroup != null && subgroup.getCategory() != null) {
                String cat = subgroup.getCategory().toUpperCase();
                if ("M".equals(cat) || "MUST".equals(cat)) {
                    student.setMustXp(student.getMustXp() + xpToAward);
                } else if ("I".equals(cat) || "INDIVIDUAL".equals(cat)) {
                    student.setIndividualXp(student.getIndividualXp() + xpToAward);
                } else if ("G".equals(cat) || "GROUP".equals(cat)) {
                    student.setGroupXp(student.getGroupXp() + xpToAward);
                }
            }
            
            captainSelectionService.evaluateCaptainPromotion(student);
            evaluateStagePromotion(student);

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

            xpCommandService.updateStreakOnSubmission(student, activity.getName());

            successCount++;
        }

        if (!activityXpsToSave.isEmpty()) {
            studentActivityXpRepository.saveAll(activityXpsToSave);
            xpTransactionRepository.saveAll(txsToSave);
            studentRepository.saveAll(studentsToUpdate);
        }

        if (!errors.isEmpty()) {
            System.out.println("ALL collected errors: " + String.join(", ", errors));
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

    private void saveXpRecords(Student student, Activity activity, User teacher, ActivityAssignment assignment, String resultStr, int xpToAward, String remarks) {
        StudentActivityXp record = new StudentActivityXp(
            student, activity, teacher, assignment, xpToAward, remarks != null ? remarks : "", resultStr, LocalDateTime.now()
        );
        studentActivityXpRepository.save(record);

        student.setTotalXp(student.getTotalXp() + xpToAward);
        student.setScore(student.getScore() + xpToAward);
        
        com.spdms.entity.ActivitySubgroup subgroup = activity.getSubgroup();
        if (subgroup != null && subgroup.getCategory() != null) {
            String cat = subgroup.getCategory().toUpperCase();
            if ("M".equals(cat) || "MUST".equals(cat)) {
                student.setMustXp(student.getMustXp() + xpToAward);
            } else if ("I".equals(cat) || "INDIVIDUAL".equals(cat)) {
                student.setIndividualXp(student.getIndividualXp() + xpToAward);
            } else if ("G".equals(cat) || "GROUP".equals(cat)) {
                student.setGroupXp(student.getGroupXp() + xpToAward);
            }
        }
        
        captainSelectionService.evaluateCaptainPromotion(student);
        evaluateStagePromotion(student);

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

    private void evaluateStagePromotion(Student student) {
        com.spdms.entity.ActivityStage currentStage = activityStageRepository.findByDisplayOrder(student.getCurrentStage()).orElse(null);
        if (currentStage == null || currentStage.getStatus() != com.spdms.enums.StageStatus.ACTIVE) {
            return;
        }

        com.spdms.modules.activity.dto.response.StageValidationResponse validation = stageValidationService.validateStage(student.getId(), currentStage.getId());
        boolean thresholdsMet = stageValidationService.isStageThresholdsMet(student.getId(), currentStage.getId());
        
        if ("UNLOCKED".equals(validation.getStageStatus()) || thresholdsMet) {
            com.spdms.entity.ActivityStage nextStage = activityStageRepository.findFirstByDisplayOrderGreaterThanOrderByDisplayOrderAsc(student.getCurrentStage()).orElse(null);
            if (nextStage != null) {
                student.setCurrentStage(nextStage.getDisplayOrder());
                student.setStage(nextStage.getDisplayOrder());
                student.setScore(0);
                
                teamAssignmentService.assignTeamOnPromotion(student, nextStage);
                
                studentRepository.save(student);
            }
        }
    }
}







