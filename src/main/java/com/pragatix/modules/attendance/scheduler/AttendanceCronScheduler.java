package com.pragatix.modules.attendance.scheduler;

import com.pragatix.entity.AttendanceSettings;
import com.pragatix.enums.AcademicYear;
import com.pragatix.modules.attendance.service.AttendanceDailyEngineService;
import com.pragatix.modules.attendance.service.AttendanceWeeklyEngineService;
import com.pragatix.modules.attendancesettings.repository.AttendanceSettingsRepository;
import com.pragatix.modules.attendancesettings.service.EngineClockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import com.pragatix.modules.academiccalendar.repository.AcademicWeekRepository;
import com.pragatix.entity.AcademicWeek;

@Component
public class AttendanceCronScheduler {

    private static final Logger log = LoggerFactory.getLogger(AttendanceCronScheduler.class);

    private final AttendanceDailyEngineService dailyEngineService;
    private final AttendanceWeeklyEngineService weeklyEngineService;
    private final AttendanceSettingsRepository settingsRepository;
    private final EngineClockService clockService;
    private final AcademicWeekRepository academicWeekRepository;

    public AttendanceCronScheduler(AttendanceDailyEngineService dailyEngineService,
                                   AttendanceWeeklyEngineService weeklyEngineService,
                                   AttendanceSettingsRepository settingsRepository,
                                   EngineClockService clockService,
                                   AcademicWeekRepository academicWeekRepository) {
        this.dailyEngineService = dailyEngineService;
        this.weeklyEngineService = weeklyEngineService;
        this.settingsRepository = settingsRepository;
        this.clockService = clockService;
        this.academicWeekRepository = academicWeekRepository;
    }

    /**
     * Runs every 5 minutes to check if the Daily or Weekly engine needs to run.
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void executeAttendanceEngines() {
        log.info("AttendanceCronScheduler checking configured times...");
        List<AttendanceSettings> allSettings = settingsRepository.findAll();

        for (AttendanceSettings settings : allSettings) {
            AcademicYear year = settings.getAcademicYear();
            if (year == null) {
                log.warn("Skipping attendance settings with null AcademicYear (ID: {})", settings.getId());
                continue;
            }
            LocalTime effectiveTime = clockService.getEffectiveTime(year);
            LocalDate effectiveDate = clockService.getEffectiveDate(year);

            // Fetch Active AcademicWeek
            AcademicWeek activeWeek = academicWeekRepository.findActiveWeekForDate(year, effectiveDate).orElse(null);
            if (activeWeek == null) {
                log.info("No active Academic Week configured for Academic Year: {}. Skipping engine runs.", year);
                continue;
            }
            
            LocalDate startDate = activeWeek.getStartDate();
            LocalDate endDate = activeWeek.getEndDate();

            // Check Daily Engine
            if (Boolean.TRUE.equals(settings.getDailyEngineEnabled()) && settings.getDailyProcessingTime() != null) {
                if (startDate != null && endDate != null && 
                    !effectiveDate.isBefore(startDate) && !effectiveDate.isAfter(endDate)) {
                    
                    if (!effectiveTime.isBefore(settings.getDailyProcessingTime())) {
                        boolean runToday = settings.getLastDailyRun() == null || !settings.getLastDailyRun().toLocalDate().isEqual(effectiveDate);
                        if (runToday) {
                            log.info("Triggering Daily Engine automatically for Academic Year: {}", year);
                            dailyEngineService.execute(year);
                        }
                    }
                }
            }

            // Check Weekly Engine
            if (Boolean.TRUE.equals(settings.getWeeklyEngineEnabled()) && settings.getWeeklyProcessingTime() != null) {
                if (endDate == null) {
                    continue; // Cannot run weekly engine without a configured end date
                }
                // Weekly engine only runs on the configured end date
                if (effectiveDate.isEqual(endDate)) {
                    if (!effectiveTime.isBefore(settings.getWeeklyProcessingTime())) {
                        boolean runToday = settings.getLastWeeklyRun() == null || !settings.getLastWeeklyRun().toLocalDate().isEqual(effectiveDate);
                        if (runToday) {
                            log.info("Triggering Weekly Engine automatically for Academic Year: {}", year);
                            weeklyEngineService.execute(year);
                        }
                    }
                }
            }
        }
    }
}
