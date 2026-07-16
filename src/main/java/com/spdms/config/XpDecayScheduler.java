package com.spdms.config;

import com.spdms.entity.Student;
import com.spdms.entity.Streak;
import com.spdms.entity.XpTransaction;
import com.spdms.modules.student.repository.StudentRepository;
import com.spdms.repository.StreakRepository;
import com.spdms.repository.XpTransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class XpDecayScheduler {

    private final StreakRepository streakRepository;
    private final StudentRepository studentRepository;
    private final XpTransactionRepository xpTransactionRepository;

    public XpDecayScheduler(StreakRepository streakRepository,
                              StudentRepository studentRepository,
                              XpTransactionRepository xpTransactionRepository) {
        this.streakRepository = streakRepository;
        this.studentRepository = studentRepository;
        this.xpTransactionRepository = xpTransactionRepository;
    }

    /**
     * Runs daily at 2:00 AM to check for broken streaks and apply carry-forward penalties
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void checkAndApplyDecay() {
        List<Streak> allStreaks = streakRepository.findAll();
        LocalDateTime thresholdTime = LocalDateTime.now().minusHours(36);

        for (Streak streak : allStreaks) {
            // If the streak is active and hasn't been updated for 36 hours
            if (!streak.isBroken() && streak.getCurrentStreak() > 0 && 
                (streak.getLastUpdated() == null || streak.getLastUpdated().isBefore(thresholdTime))) {
                
                // Mark streak as broken
                streak.setBroken(true);
                int oldStreak = streak.getCurrentStreak();
                streak.setCurrentStreak(0);
                streakRepository.save(streak);

                // Apply negative XP transaction
                Student student = streak.getStudent();
                int penaltyPoints = streak.getPenaltyPerBreak();

                XpTransaction penaltyTx = XpTransaction.builder()
                        .student(student)
                        .category("DISCIPLINE")
                        .activityName("Streak broken: " + streak.getStreakType() + " (was " + oldStreak + " days)")
                        .xpPoints(-Math.abs(penaltyPoints))
                        .evidenceUrl("System Auto-Penalty")
                        .submittedAt(LocalDateTime.now())
                        .status("APPROVED")
                        .approvedBy("System Scheduler")
                        .isPenalty(true)
                        .capApplied(false)
                        .build();

                xpTransactionRepository.save(penaltyTx);

                // Deduct from student's total XP
                student.setTotalXp(Math.max(-9999, student.getTotalXp() - Math.abs(penaltyPoints)));
                studentRepository.save(student);
            }
        }
    }
}
