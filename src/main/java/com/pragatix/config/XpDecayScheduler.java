package com.pragatix.config;

import com.pragatix.entity.Student;
import com.pragatix.entity.Streak;
import com.pragatix.modules.student.service.XpEngineService;
import com.pragatix.repository.StreakRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class XpDecayScheduler {

    private final StreakRepository streakRepository;
    private final XpEngineService xpEngineService;
    private final com.pragatix.modules.academiccalendar.service.AcademicCalendarResolver academicCalendarResolver;

    public XpDecayScheduler(StreakRepository streakRepository,
            XpEngineService xpEngineService,
            com.pragatix.modules.academiccalendar.service.AcademicCalendarResolver academicCalendarResolver) {
        this.streakRepository = streakRepository;
        this.xpEngineService = xpEngineService;
        this.academicCalendarResolver = academicCalendarResolver;
    }

    /**
     * Runs daily at 2:00 AM to check for broken streaks and apply carry-forward
     * penalties
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void checkAndApplyDecay() {
        List<Streak> allStreaks = streakRepository.findAll();
        LocalDateTime thresholdTime = LocalDateTime.now().minusHours(36);

        List<Streak> streaksToSave = new java.util.ArrayList<>();

        for (Streak streak : allStreaks) {
            com.pragatix.enums.AcademicYear academicYear = com.pragatix.enums.AcademicYear.fromStudent(streak.getStudent());
            boolean yesterdayWasHoliday = academicCalendarResolver.isHoliday(java.time.LocalDate.now().minusDays(1), academicYear);

            if ("ATTENDANCE".equalsIgnoreCase(streak.getStreakType()) && yesterdayWasHoliday) {
                continue; // Do not break attendance streaks if yesterday was a holiday
            }

            // If the streak is active and hasn't been updated for 36 hours
            if (!streak.isBroken() && streak.getCurrentStreak() > 0 &&
                    (streak.getLastUpdated() == null || streak.getLastUpdated().isBefore(thresholdTime))) {

                // Mark streak as broken
                streak.setBroken(true);
                int oldStreak = streak.getCurrentStreak();
                streak.setCurrentStreak(0);
                streaksToSave.add(streak);

                // Apply negative XP transaction via XpEngineService
                Student student = streak.getStudent();
                int penaltyPoints = streak.getPenaltyPerBreak();

                xpEngineService.awardXp(student, null, null, null, -Math.abs(penaltyPoints),
                        "Streak broken: " + streak.getStreakType() + " (was " + oldStreak + " days)");
            }
        }

        if (!streaksToSave.isEmpty())
            streakRepository.saveAll(streaksToSave);
    }
}
