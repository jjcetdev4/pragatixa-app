package com.spdms.modules.student.service;

import com.spdms.entity.*;
import com.spdms.modules.activity.repository.ActivityStageRepository;
import com.spdms.modules.activity.service.StageValidationService;
import com.spdms.modules.student.repository.StudentActivityXpRepository;
import com.spdms.modules.student.repository.StudentRepository;
import com.spdms.repository.XpTransactionRepository;
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
    private final com.spdms.admin.service.CaptainSelectionService captainSelectionService;
    private final com.spdms.repository.StreakRepository streakRepository;

    public XpEngineService(StudentRepository studentRepository,
                           StudentActivityXpRepository studentActivityXpRepository,
                           XpTransactionRepository xpTransactionRepository,
                           ActivityStageRepository activityStageRepository,
                           StageValidationService stageValidationService,
                           TeamAssignmentService teamAssignmentService,
                           com.spdms.admin.service.CaptainSelectionService captainSelectionService,
                           com.spdms.repository.StreakRepository streakRepository) {
        this.studentRepository = studentRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.xpTransactionRepository = xpTransactionRepository;
        this.activityStageRepository = activityStageRepository;
        this.stageValidationService = stageValidationService;
        this.teamAssignmentService = teamAssignmentService;
        this.captainSelectionService = captainSelectionService;
        this.streakRepository = streakRepository;
    }

    @Transactional
    public Student awardXp(Student student, Activity activity, User authorizedUser, ActivityAssignment assignment, int requestXp, String remarks) {
        
        System.out.println("=====================================================");
        System.out.println("XP ENGINE: Processing Award for Student: " + student.getId());
        
        // 1. Determine Activity Category Dynamically
        String resolvedCategory = "SKILL";
        String activityName = "General XP";
        
        if (activity != null) {
            resolvedCategory = "";
            activityName = activity.getName();
            com.spdms.entity.ActivitySubgroup subgroup = activity.getSubgroup();
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

        if (activity != null) {
            penaltyFlag = (activity.getPenaltyEnabled() != null && activity.getPenaltyEnabled()) || "Penalty".equalsIgnoreCase(activity.getXpType());
            if (penaltyFlag) {
                configuredXp = activity.getPenaltyXp() != null ? activity.getPenaltyXp() : 0;
                if (configuredXp == 0) configuredXp = activity.getAwardXp() != null ? activity.getAwardXp() : 0;
            } else {
                configuredXp = activity.getAwardXp() != null ? activity.getAwardXp() : 0;
            }
        } else {
            configuredXp = Math.abs(requestXp);
            penaltyFlag = requestXp < 0;
        }

        int appliedXp = 0;
        if (penaltyFlag) {
            appliedXp = -Math.abs(configuredXp);
        } else {
            // For regular rewards, allow partial points if valid, otherwise use configured. But if strict rule:
            appliedXp = Math.abs(configuredXp);
            if (requestXp > 0 && requestXp < configuredXp) {
                appliedXp = requestXp; // Allow partial grading for awards, but never for penalties.
            }
        }

        System.out.println("Configured XP: " + configuredXp);
        System.out.println("Penalty: " + penaltyFlag);
        System.out.println("Calculated Applied XP: " + appliedXp);
        
        int oldTotalXp = student.getTotalXp();

        // 2. Update Category XP & Total XP
        if (resolvedCategory.contains("MUST") || resolvedCategory.contains("MANDATORY") || resolvedCategory.contains(" M ")) {
            student.setMustXp(student.getMustXp() + appliedXp);
        } else if (resolvedCategory.contains("INDIVIDUAL") || resolvedCategory.contains(" I ")) {
            student.setIndividualXp(student.getIndividualXp() + appliedXp);
        } else if (resolvedCategory.contains("GROUP") || resolvedCategory.contains("TEAM") || resolvedCategory.contains(" G ")) {
            student.setGroupXp(student.getGroupXp() + appliedXp);
        }
        
        // Update total
        student.setTotalXp(oldTotalXp + appliedXp);
        student.setScore(student.getScore() + appliedXp);
        
        int newTotalXp = student.getTotalXp();
        System.out.println("Old Total XP: " + oldTotalXp);
        System.out.println("New Total XP: " + newTotalXp);

        // 3. Save XP History
        if (activity != null && authorizedUser != null) {
            StudentActivityXp record = new StudentActivityXp(
                    student, activity, authorizedUser, assignment, appliedXp, remarks != null ? remarks : "", LocalDateTime.now());
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
                .build();
        xpTransactionRepository.save(tx);

        // Update streak
        updateStreakOnSubmission(student, activityName);

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
        if (currentStage == null || currentStage.getStatus() != com.spdms.enums.StageStatus.ACTIVE) {
            System.out.println("Stage Engine: Current stage not found or inactive.");
            return;
        }

        boolean thresholdsMet = stageValidationService.isStageThresholdsMet(student, currentStage);

        if (thresholdsMet) {
            System.out.println("Promotion Result: SUCCESS (Thresholds Met)");
            
            ActivityStage nextStage = activityStageRepository.findFirstByDisplayOrderGreaterThanOrderByDisplayOrderAsc(student.getStage()).orElse(null);
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
            java.util.Optional<Streak> streakOpt = streakRepository.findByStudentRegNoAndStreakType(student.getRegNo(), type);
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
                if (streak.getLastUpdated() != null && streak.getLastUpdated().isAfter(LocalDateTime.now().minusHours(36))) {
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
