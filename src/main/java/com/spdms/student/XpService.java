package com.spdms.student;

import com.spdms.common.response.ApiResponse;
import com.spdms.entity.Student;
import com.spdms.entity.XpTransaction;
import com.spdms.entity.Streak;
import com.spdms.entity.Activity;
import com.spdms.repository.ActivityRepository;
import com.spdms.modules.student.repository.StudentRepository;
import com.spdms.repository.XpTransactionRepository;
import com.spdms.repository.StreakRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class XpService {

    private final XpTransactionRepository xpTransactionRepository;
    private final StreakRepository streakRepository;
    private final StudentRepository studentRepository;
    private final ActivityRepository activityRepository;

    public XpService(XpTransactionRepository xpTransactionRepository,
                     StreakRepository streakRepository,
                     StudentRepository studentRepository,
                     ActivityRepository activityRepository) {
        this.xpTransactionRepository = xpTransactionRepository;
        this.streakRepository = streakRepository;
        this.studentRepository = studentRepository;
        this.activityRepository = activityRepository;
    }

    /**
     * Get XP Summary by category for a student
     */
    public Map<String, Integer> getXpSummary(String studentId) {
        List<XpTransaction> txs = xpTransactionRepository.findByStudentStudentId(studentId);
        Map<String, Integer> summary = new HashMap<>();
        summary.put("ACADEMIC", 0);
        summary.put("SKILL", 0);
        summary.put("COMMUNICATION", 0);
        summary.put("LEADERSHIP", 0);
        summary.put("INNOVATION", 0);
        summary.put("PLACEMENT", 0);
        summary.put("DISCIPLINE", 0);
        summary.put("COMMUNITY", 0);
        summary.put("SPORTS", 0);
        summary.put("CULTURAL", 0);

        for (XpTransaction tx : txs) {
            if ("APPROVED".equalsIgnoreCase(tx.getStatus())) {
                String cat = tx.getCategory().toUpperCase();
                summary.put(cat, summary.getOrDefault(cat, 0) + tx.getXpPoints());
            }
        }
        return summary;
    }

    /**
     * Get paginated XP history list
     */
    public Page<XpTransaction> getXpHistory(String studentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        return xpTransactionRepository.findByStudentStudentId(studentId, pageable);
    }

    /**
     * Get all streaks for a student
     */
    public List<Streak> getStudentStreaks(String studentId) {
        return streakRepository.findByStudentStudentId(studentId);
    }

    /**
     * Student submits activity evidence for XP
     */
    @Transactional
    public ApiResponse<XpTransaction> submitXpClaim(String studentId, String category, String activityName, int xpPoints, String evidenceUrl) {
        Optional<Student> studentOpt = studentRepository.findByStudentId(studentId);
        if (studentOpt.isEmpty()) {
            return ApiResponse.error("Student not found");
        }
        Student student = studentOpt.get();

        // Enforce XP caps
        int allowedPoints = applyCapsAndLimits(student, activityName, xpPoints);
        boolean capApplied = allowedPoints < xpPoints;

        // Resolve XP Category from the Activity if possible
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

        // Update streaks if it is a streak-related activity
        updateStreakOnSubmission(student, activityName);

        return ApiResponse.ok("XP Claim submitted successfully", saved);
    }

    /**
     * Faculty/Admin approves a pending XP claim
     */
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

        // Add to student's total XP
        Student student = tx.getStudent();
        student.setTotalXp(student.getTotalXp() + tx.getXpPoints());
        studentRepository.save(student);

        return ApiResponse.ok("XP transaction approved successfully", saved);
    }

    /**
     * Faculty/Admin rejects a pending XP claim
     */
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

    /**
     * Faculty logs a discipline violation (Negative XP)
     */
    @Transactional
    public ApiResponse<XpTransaction> logViolation(String studentId, String violationType, int xpPenalty, String appliedBy, String description) {
        Optional<Student> studentOpt = studentRepository.findByStudentId(studentId);
        if (studentOpt.isEmpty()) {
            return ApiResponse.error("Student not found");
        }
        Student student = studentOpt.get();

        // Save violation transaction as APPROVED immediately with negative XP
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

        // Deduct from student's total XP
        student.setTotalXp(Math.max(-9999, student.getTotalXp() - Math.abs(xpPenalty)));
        studentRepository.save(student);

        return ApiResponse.ok("Violation logged successfully. Points deducted.", saved);
    }

    /**
     * Internal helper to validate and apply activity caps
     */
    private int applyCapsAndLimits(Student student, String activity, int points) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).withHour(0).withMinute(0);
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0);

        List<XpTransaction> studentTxs = xpTransactionRepository.findByStudentStudentId(student.getStudentId());

        if (activity.toLowerCase().contains("attendance")) {
            // Cap: 120/month
            int currentMonthEarned = sumPointsForActivityInPeriod(studentTxs, "attendance", startOfMonth);
            return Math.max(0, Math.min(points, 120 - currentMonthEarned));
        } else if (activity.toLowerCase().contains("oral presentation")) {
            // Cap: 120/month
            int currentMonthEarned = sumPointsForActivityInPeriod(studentTxs, "oral presentation", startOfMonth);
            return Math.max(0, Math.min(points, 120 - currentMonthEarned));
        } else if (activity.toLowerCase().contains("english diary")) {
            // Cap: 25/week
            int currentWeekEarned = sumPointsForActivityInPeriod(studentTxs, "english diary", startOfWeek);
            return Math.max(0, Math.min(points, 25 - currentWeekEarned));
        } else if (activity.toLowerCase().contains("c programming") || activity.toLowerCase().contains("c coding")) {
            // Cap: 50/week
            int currentWeekEarned = sumPointsForActivityInPeriod(studentTxs, "c programming", startOfWeek)
                    + sumPointsForActivityInPeriod(studentTxs, "c coding", startOfWeek);
            return Math.max(0, Math.min(points, 50 - currentWeekEarned));
        } else if (activity.toLowerCase().contains("library")) {
            // Cap: 60/week
            int currentWeekEarned = sumPointsForActivityInPeriod(studentTxs, "library", startOfWeek);
            return Math.max(0, Math.min(points, 60 - currentWeekEarned));
        } else if (activity.toLowerCase().contains("coe lab") || activity.toLowerCase().contains("d2p lab")) {
            // Cap: 60/week
            int currentWeekEarned = sumPointsForActivityInPeriod(studentTxs, "coe lab", startOfWeek)
                    + sumPointsForActivityInPeriod(studentTxs, "d2p lab", startOfWeek);
            return Math.max(0, Math.min(points, 60 - currentWeekEarned));
        } else if (activity.toLowerCase().contains("domain report")) {
            // Cap: 150/month
            int currentMonthEarned = sumPointsForActivityInPeriod(studentTxs, "domain report", startOfMonth);
            return Math.max(0, Math.min(points, 150 - currentMonthEarned));
        } else if (activity.toLowerCase().contains("certificate course")) {
            // Cap: 200/semester (using total database count for current semester context)
            int semesterTotal = sumPointsForActivityInPeriod(studentTxs, "certificate course", now.minusMonths(5));
            return Math.max(0, Math.min(points, 200 - semesterTotal));
        } else if (activity.toLowerCase().contains("resume first draft")) {
            // Cap: 50 once
            boolean alreadyClaimed = anyActivityClaimed(studentTxs, "resume first draft");
            return alreadyClaimed ? 0 : points;
        } else if (activity.toLowerCase().contains("ms word")) {
            // Cap: 50 once
            boolean alreadyClaimed = anyActivityClaimed(studentTxs, "ms word");
            return alreadyClaimed ? 0 : points;
        } else if (activity.toLowerCase().contains("ms excel")) {
            // Cap: 50 once
            boolean alreadyClaimed = anyActivityClaimed(studentTxs, "ms excel");
            return alreadyClaimed ? 0 : points;
        } else if (activity.toLowerCase().contains("ms powerpoint")) {
            // Cap: 50 once
            boolean alreadyClaimed = anyActivityClaimed(studentTxs, "ms powerpoint");
            return alreadyClaimed ? 0 : points;
        } else if (activity.toLowerCase().contains("typing 20 wpm")) {
            // Cap: 20 once
            boolean alreadyClaimed = anyActivityClaimed(studentTxs, "typing 20 wpm");
            return alreadyClaimed ? 0 : points;
        } else if (activity.toLowerCase().contains("duolingo")) {
            // Cap: 45/month
            int currentMonthEarned = sumPointsForActivityInPeriod(studentTxs, "duolingo", startOfMonth);
            return Math.max(0, Math.min(points, 45 - currentMonthEarned));
        }

        return points; // No cap matches, return original points
    }

    private int sumPointsForActivityInPeriod(List<XpTransaction> txs, String activityKeyword, LocalDateTime since) {
        int sum = 0;
        for (XpTransaction tx : txs) {
            if (tx.getActivityName().toLowerCase().contains(activityKeyword)
                    && tx.getSubmittedAt().isAfter(since)
                    && !"REJECTED".equalsIgnoreCase(tx.getStatus())) {
                sum += tx.getXpPoints();
            }
        }
        return sum;
    }

    private boolean anyActivityClaimed(List<XpTransaction> txs, String activityKeyword) {
        for (XpTransaction tx : txs) {
            if (tx.getActivityName().toLowerCase().contains(activityKeyword)
                    && !"REJECTED".equalsIgnoreCase(tx.getStatus())) {
                return true;
            }
        }
        return false;
    }

    private void updateStreakOnSubmission(Student student, String activity) {
        String type = null;
        int penalty = 0;

        if (activity.toLowerCase().contains("journal")) {
            type = "MONDAY_JOURNAL";
            penalty = student.getStage() == 1 ? 10 : (student.getStage() == 2 ? 30 : 50);
        } else if (activity.toLowerCase().contains("diary")) {
            type = "ENGLISH_DIARY";
            penalty = 20; // Default penalty
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
            Optional<Streak> streakOpt = streakRepository.findByStudentStudentIdAndStreakType(student.getStudentId(), type);
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
                // If last update was within past 36 hours, increment streak
                if (streak.getLastUpdated() != null && streak.getLastUpdated().isAfter(LocalDateTime.now().minusHours(36))) {
                    streak.setCurrentStreak(streak.getCurrentStreak() + 1);
                } else {
                    streak.setCurrentStreak(1); // reset or start fresh
                }
                streak.setBroken(false);
                streak.setLastUpdated(LocalDateTime.now());
            }
            streakRepository.save(streak);
        }
    }
}
