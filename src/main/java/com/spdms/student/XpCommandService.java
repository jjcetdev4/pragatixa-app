package com.spdms.student;

import com.spdms.common.response.ApiResponse;
import com.spdms.entity.Activity;
import com.spdms.entity.Streak;
import com.spdms.entity.Student;
import com.spdms.entity.XpTransaction;
import com.spdms.modules.activity.repository.ActivityRepository;
import com.spdms.modules.student.repository.StudentRepository;
import com.spdms.repository.StreakRepository;
import com.spdms.repository.XpTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class XpCommandService {

    private final XpTransactionRepository xpTransactionRepository;
    private final StreakRepository streakRepository;
    private final StudentRepository studentRepository;
    private final ActivityRepository activityRepository;
    private final XpCalculationService xpCalculationService;

    public XpCommandService(XpTransactionRepository xpTransactionRepository,
                            StreakRepository streakRepository,
                            StudentRepository studentRepository,
                            ActivityRepository activityRepository,
                            XpCalculationService xpCalculationService) {
        this.xpTransactionRepository = xpTransactionRepository;
        this.streakRepository = streakRepository;
        this.studentRepository = studentRepository;
        this.activityRepository = activityRepository;
        this.xpCalculationService = xpCalculationService;
    }

    @Transactional
    public ApiResponse<XpTransaction> submitXpClaim(String regNo, String category, String activityName, int xpPoints, String evidenceUrl) {
        Optional<Student> studentOpt = studentRepository.findByRegNo(regNo);
        if (studentOpt.isEmpty()) {
            return ApiResponse.error("Student not found");
        }
        Student student = studentOpt.get();

        int allowedPoints = xpCalculationService.applyCapsAndLimits(student, activityName, xpPoints);
        boolean capApplied = allowedPoints < xpPoints;

        String resolvedCategory = category;
        Activity resolvedActivity = null;
        if (activityName != null) {
            List<Activity> activityList = activityRepository.findByActivityName(activityName);
            if (!activityList.isEmpty()) {
                resolvedActivity = activityList.get(0);
                if (resolvedActivity.getXpCategory() != null) {
                    resolvedCategory = resolvedActivity.getXpCategory();
                }
            }
        }

        XpTransaction claim = XpTransaction.builder()
                .student(student)
                .activity(resolvedActivity)
                .category(resolvedCategory.toUpperCase())
                .activityName(activityName)
                .xpPoints(allowedPoints)
                .evidenceUrl(evidenceUrl)
                .submittedAt(LocalDateTime.now())
                .status("PENDING")
                .isPenalty(false)
                .capApplied(capApplied)
                .build();

        XpTransaction saved = xpTransactionRepository.save(claim);
        updateStreakOnSubmission(student, activityName);

        return ApiResponse.ok("XP Claim submitted successfully", saved);
    }

    @Transactional
    public ApiResponse<XpTransaction> approveXpClaim(Long txId, String approvedBy) {
        Optional<XpTransaction> txOpt = xpTransactionRepository.findById(txId);
        if (txOpt.isEmpty()) {
            return ApiResponse.error("XP transaction not found");
        }
        XpTransaction tx = txOpt.get();
        if (!"PENDING".equalsIgnoreCase(tx.getStatus())) {
            return ApiResponse.error("Transaction is already processed");
        }

        tx.setStatus("APPROVED");
        tx.setApprovedBy(approvedBy);
        XpTransaction saved = xpTransactionRepository.save(tx);

        Student student = tx.getStudent();
        student.setTotalXp(student.getTotalXp() + tx.getXpPoints());
        studentRepository.save(student);

        return ApiResponse.ok("XP transaction approved successfully", saved);
    }

    @Transactional
    public ApiResponse<XpTransaction> rejectXpClaim(Long txId, String approvedBy) {
        Optional<XpTransaction> txOpt = xpTransactionRepository.findById(txId);
        if (txOpt.isEmpty()) {
            return ApiResponse.error("XP transaction not found");
        }
        XpTransaction tx = txOpt.get();
        if (!"PENDING".equalsIgnoreCase(tx.getStatus())) {
            return ApiResponse.error("Transaction is already processed");
        }

        tx.setStatus("REJECTED");
        tx.setApprovedBy(approvedBy);
        XpTransaction saved = xpTransactionRepository.save(tx);

        return ApiResponse.ok("XP transaction rejected", saved);
    }

    @Transactional
    public ApiResponse<XpTransaction> logViolation(String regNo, String violationType, int xpPenalty, String appliedBy, String description) {
        Optional<Student> studentOpt = studentRepository.findByRegNo(regNo);
        if (studentOpt.isEmpty()) {
            return ApiResponse.error("Student not found");
        }
        Student student = studentOpt.get();

        XpTransaction violationTx = XpTransaction.builder()
                .student(student)
                .category("DISCIPLINE")
                .activityName("Violation: " + violationType)
                .xpPoints(-Math.abs(xpPenalty))
                .evidenceUrl(description)
                .submittedAt(LocalDateTime.now())
                .status("APPROVED")
                .approvedBy(appliedBy)
                .isPenalty(true)
                .capApplied(false)
                .build();

        XpTransaction saved = xpTransactionRepository.save(violationTx);

        student.setTotalXp(Math.max(-9999, student.getTotalXp() - Math.abs(xpPenalty)));
        studentRepository.save(student);

        return ApiResponse.ok("Violation logged successfully. Points deducted.", saved);
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
            Optional<Streak> streakOpt = streakRepository.findByStudentRegNoAndStreakType(student.getRegNo(), type);
            Streak streak;
            if (streakOpt.isEmpty()) {
                streak = Streak.builder()
                        .student(student)
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
