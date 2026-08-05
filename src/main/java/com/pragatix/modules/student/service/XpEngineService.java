package com.pragatix.modules.student.service;

import com.pragatix.entity.*;
import com.pragatix.modules.activity.repository.ActivityStageRepository;
import com.pragatix.modules.activity.service.StageValidationService;
import com.pragatix.modules.student.repository.StudentActivityXpRepository;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.repository.XpTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class XpEngineService {

    private final StudentRepository studentRepository;
    private final StudentActivityXpRepository studentActivityXpRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final ActivityStageRepository activityStageRepository;
    private final StageValidationService stageValidationService;
    private final TeamAssignmentService teamAssignmentService;
    private final com.pragatix.admin.service.CaptainSelectionService captainSelectionService;
    private final com.pragatix.repository.StreakRepository streakRepository;
    private final com.pragatix.modules.activity.service.ActivityStreakService activityStreakService;

    public XpEngineService(StudentRepository studentRepository,
            StudentActivityXpRepository studentActivityXpRepository,
            XpTransactionRepository xpTransactionRepository,
            ActivityStageRepository activityStageRepository,
            StageValidationService stageValidationService,
            TeamAssignmentService teamAssignmentService,
            com.pragatix.admin.service.CaptainSelectionService captainSelectionService,
            com.pragatix.repository.StreakRepository streakRepository,
            com.pragatix.modules.activity.service.ActivityStreakService activityStreakService) {
        this.studentRepository = studentRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.xpTransactionRepository = xpTransactionRepository;
        this.activityStageRepository = activityStageRepository;
        this.stageValidationService = stageValidationService;
        this.teamAssignmentService = teamAssignmentService;
        this.captainSelectionService = captainSelectionService;
        this.streakRepository = streakRepository;
        this.activityStreakService = activityStreakService;
    }

    @Transactional
    public Student awardXp(Student student, Activity activity, User authorizedUser, ActivityAssignment assignment,
            int requestXp, String remarks) {
        return awardXp(student, activity, authorizedUser, assignment, requestXp, remarks, null);
    }

    @Transactional
    public Student awardXp(Student student, Activity activity, User authorizedUser, ActivityAssignment assignment,
            int requestXp, String remarks, com.pragatix.modules.attendance.dto.AttendanceXpExecutionRequest attendanceReq) {

        System.out.println("=====================================================");
        System.out.println("XP ENGINE: Processing Award for Student: " + student.getId());

        // 1. Determine Activity Category Dynamically
        String resolvedCategory = "SKILL";
        String activityName = "General XP";

        if (activity != null) {
            resolvedCategory = "";
            activityName = activity.getName();
            com.pragatix.entity.ActivitySubgroup subgroup = activity.getSubgroup();
            if (subgroup != null) {
                resolvedCategory += (subgroup.getCategory() != null ? subgroup.getCategory() : "") + " " +
                        (subgroup.getName() != null ? subgroup.getName() : "");
            }
            if (activity.getXpCategory() != null) {
                resolvedCategory += " " + activity.getXpCategory();
            }
            resolvedCategory = resolvedCategory.toUpperCase();
        }

        // ====================================================
        // DEBUG: Before processing
        // ====================================================
        int configuredXp = 0;
        boolean penaltyFlag = false;
        int appliedXp = 0;

        if (attendanceReq != null) {
            System.out.println("Attendance Override = TRUE");
            System.out.println("Activity ID\n" + (activity != null ? activity.getId() : "null"));
            System.out.println("Attendance Rule\n" + attendanceReq.getAttendanceRule());
            System.out.println("Calculated XP\n" + attendanceReq.getCalculatedXp());
            System.out.println("Activity Award XP (Ignored)\n" + (activity != null ? activity.getAwardXp() : "null"));
            System.out.println("Activity Penalty XP (Ignored)\n" + (activity != null ? activity.getPenaltyXp() : "null"));
            System.out.println("Applied XP\n" + attendanceReq.getCalculatedXp());
            System.out.println("Reason\n" + attendanceReq.getReason());

            configuredXp = Math.abs(attendanceReq.getCalculatedXp());
            penaltyFlag = attendanceReq.getIsPenalty();
            appliedXp = attendanceReq.getCalculatedXp(); // Note: we assume Attendance engine passes signed XP correctly (-40 for penalties)
        } else {
            System.out.println("Attendance Override = FALSE");
            
            if (activity != null) {
                if (Boolean.TRUE.equals(activity.getAttendanceEngineEnabled()) ||
                   (Boolean.TRUE.equals(activity.getAwardEnabled()) && Boolean.TRUE.equals(activity.getPenaltyEnabled()))) {
                    penaltyFlag = requestXp < 0;
                } else {
                    penaltyFlag = (activity.getPenaltyEnabled() != null && activity.getPenaltyEnabled())
                            || "Penalty".equalsIgnoreCase(activity.getXpType());
                }
                if (penaltyFlag) {
                    configuredXp = activity.getPenaltyXp() != null ? activity.getPenaltyXp() : 0;
                    if (configuredXp == 0)
                        configuredXp = activity.getAwardXp() != null ? activity.getAwardXp() : 0;
                } else {
                    configuredXp = activity.getAwardXp() != null ? activity.getAwardXp() : 0;
                }
            } else {
                configuredXp = Math.abs(requestXp);
                penaltyFlag = requestXp < 0;
            }

            if (penaltyFlag) {
                if (activity != null && Boolean.TRUE.equals(activity.getAttendanceEngineEnabled())) {
                    appliedXp = -Math.abs(requestXp);
                } else {
                    appliedXp = -Math.abs(configuredXp);
                }
            } else {
                // For regular rewards, allow partial points if valid, otherwise use configured.
                appliedXp = Math.abs(configuredXp);
                if (requestXp > 0 && requestXp < configuredXp) {
                    appliedXp = requestXp; // Allow partial grading for awards, but never for penalties.
                }
            }

            System.out.println("Activity Award XP Used: " + (activity != null ? activity.getAwardXp() : "null"));
            System.out.println("Activity Penalty XP Used: " + (activity != null ? activity.getPenaltyXp() : "null"));
            System.out.println("Applied XP: " + appliedXp);
        }

        int oldTotalXp = student.getTotalXp();

        // 2. Update Category XP & Total XP
        boolean addedToMust = false;
        boolean addedToInd = false;
        boolean addedToGrp = false;

        if (activity != null) {
            // Use domain helper methods for classification to avoid string parsing
            if (activity.isMustXpEligible()) {
                student.setMustXp(student.getMustXp() + appliedXp);
                addedToMust = true;
            }
            
            if (activity.isIndividualXpEligible()) {
                student.setIndividualXp(student.getIndividualXp() + appliedXp);
                addedToInd = true;
            }
            
            if (activity.isGroupXpEligible()) {
                student.setGroupXp(student.getGroupXp() + appliedXp);
                addedToGrp = true;
            }
        }
        
        System.out.println("Activity ID\n" + (activity != null ? activity.getId() : "null"));
        System.out.println("XP Type\n" + (activity != null ? activity.getXpType() : "null"));
        System.out.println("Subgroup\n" + (activity != null && activity.getSubgroup() != null ? activity.getSubgroup().getName() : "null"));
        System.out.println("Mode Type\n" + (activity != null ? activity.getModeType() : "null"));
        System.out.println("Awarded XP\n" + appliedXp);
        System.out.println("Added to Must XP?\n" + addedToMust);
        System.out.println("Added to Individual XP?\n" + addedToInd);
        System.out.println("Added to Group XP?\n" + addedToGrp);

        // Update total
        student.setTotalXp(oldTotalXp + appliedXp);
        student.setScore(student.getScore() + appliedXp);

        int newTotalXp = student.getTotalXp();
        System.out.println("Old Total XP: " + oldTotalXp);
        System.out.println("New Total XP: " + newTotalXp);

        // 3. Save XP History
        if (activity != null && authorizedUser != null) {
            StudentActivityXp record = new StudentActivityXp(
                    student, activity, authorizedUser, assignment, appliedXp, remarks != null ? remarks : "",
                    LocalDateTime.now(), student.getStage());
            studentActivityXpRepository.save(record);
        }

        // 4. Save Transaction
        String approverName = authorizedUser != null ? authorizedUser.getFullName() : "SYSTEM";
        XpTransaction tx = XpTransaction.builder()
                .student(student)
                .activity(activity)
                .category(resolvedCategory)
                .activityName(activityName + (remarks != null && !remarks.isEmpty() ? " - " + remarks : ""))
                .xpPoints(appliedXp)
                .submittedAt(LocalDateTime.now())
                .status("APPROVED")
                .approvedBy(approverName)
                .isPenalty(appliedXp < 0)
                .capApplied(false)
                .stage(student.getStage())
                .build();
        tx = xpTransactionRepository.saveAndFlush(tx);

        System.out.println("Award XP");
        System.out.println("Student\n" + student.getId());
        if (activity != null) {
            System.out.println("Activity\n" + activity.getId());
        }
        System.out.println("Completion Record Saved\nYES");
        System.out.println("Record ID\n" + tx.getId());
        System.out.println("Table Name\nxp_transactions");
        System.out.println("--------------------------------");

        // Update streak
        updateStreakOnSubmission(student, activityName);
        
        // Activity-wise Streak hook
        activityStreakService.incrementStreak(student, activity);

        // 5. Evaluate Captain
        captainSelectionService.evaluateCaptainPromotion(student);

        // 6. Recalculate Stage & Promote if eligible
        evaluateStagePromotion(student);

        // 7. Save and Return
        student = studentRepository.save(student);
        System.out.println("XP ENGINE: Transaction Completed and Saved.");
        System.out.println("=====================================================");

        return student;
    }

    @Transactional
    public void evaluateStagePromotion(Student student) {
        System.out.println("PROMOTION DEBUG: Promoting studentId: " + student.getId());
        System.out.println("STAGE ENGINE: Evaluating Stage for Student: " + student.getId());
        System.out.println("Current Stage: " + student.getStage());

        ActivityStage currentStage = activityStageRepository.findByDisplayOrder(student.getStage()).orElse(null);
        if (currentStage == null || currentStage.getStatus() != com.pragatix.enums.StageStatus.ACTIVE) {
            System.out.println("Stage Engine: Current stage not found or inactive.");
            return;
        }

        boolean thresholdsMet = stageValidationService.isStageThresholdsMet(student, currentStage);

        if (thresholdsMet) {
            System.out.println("Promotion Result: SUCCESS (Thresholds Met)");

            ActivityStage nextStage = activityStageRepository
                    .findFirstByDisplayOrderGreaterThanOrderByDisplayOrderAsc(student.getStage()).orElse(null);
            if (nextStage != null) {
                // Complete & Lock Current, Unlock & Activate Next
                student.setStage(nextStage.getDisplayOrder());
                student.setCurrentStage(nextStage.getDisplayOrder()); // Backward compatibility
                student.setCurrentStageId(nextStage.getId());
                student.setPromotionTimestamp(LocalDateTime.now());
                System.out.println("Student Promoted to Stage: " + nextStage.getDisplayOrder());

                // Assign Team if needed (Only Stage 2 and above)
                if (nextStage.getDisplayOrder() >= 2) {
                    teamAssignmentService.assignTeamOnPromotion(student, nextStage);
                    System.out.println("Team Assignment Result: EXECUTED");
                } else {
                    System.out.println("Team Assignment Result: SKIPPED (Stage < 2)");
                }
            } else {
                System.out.println("Promotion Result: BLOCKED (No Next Stage)");
            }
        } else {
            System.out.println("Promotion Result: PENDING (Thresholds not met)");
        }
    }

    public void updateStreakOnSubmission(Student student, String activity) {
        String type = null;
        int penalty = 0;

        if (activity.toLowerCase().contains("journal")) {
            type = "MONDAY_JOURNAL";
            penalty = student.getStage() == 1 ? 10 : (student.getStage() == 2 ? 30 : 50);
        } else if (activity.toLowerCase().contains("diary")) {
            type = "ENGLISH_DIARY";
            penalty = 20;
        } else if (activity.toLowerCase().contains("c programming") || activity.toLowerCase().contains("c coding")) {
            type = "C_CODING";
            penalty = 30;
        } else if (activity.toLowerCase().contains("python")) {
            type = "PYTHON_CODING";
            penalty = 30;
        } else if (activity.toLowerCase().contains("library")) {
            type = "LIBRARY";
            penalty = 20;
        } else if (activity.toLowerCase().contains("coe lab") || activity.toLowerCase().contains("d2p lab")) {
            type = "COE_LAB";
            penalty = 20;
        }

        if (type != null) {
            java.util.Optional<Streak> streakOpt = streakRepository.findByStudentRegNoAndStreakType(student.getRegNo(),
                    type);
            Streak streak;
            if (streakOpt.isEmpty()) {
                streak = Streak.builder()
                        .student(student)
                        .regNo(student.getRegNo())
                        .streakType(type)
                        .currentStreak(1)
                        .lastUpdated(LocalDateTime.now())
                        .isBroken(false)
                        .penaltyPerBreak(penalty)
                        .build();
            } else {
                streak = streakOpt.get();
                if (streak.getLastUpdated() != null
                        && streak.getLastUpdated().isAfter(LocalDateTime.now().minusHours(36))) {
                    streak.setCurrentStreak(streak.getCurrentStreak() + 1);
                } else {
                    streak.setCurrentStreak(1);
                }
                streak.setBroken(false);
                streak.setLastUpdated(LocalDateTime.now());
            }
            streakRepository.save(streak);
        }
    }

}
