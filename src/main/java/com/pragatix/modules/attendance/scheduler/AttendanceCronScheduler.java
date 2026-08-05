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
     * Runs every 1 minute to check if the Daily or Weekly engine needs to run.
     */
    @Scheduled(cron = "0 * * * * *")
    public void executeAttendanceEngines() {
        List<AttendanceSettings> allSettings = settingsRepository.findAll();

        for (AttendanceSettings settings : allSettings) {
            AcademicYear year = settings.getAcademicYear();
            if (year == null) {
                continue;
            }
            
            LocalTime effectiveTime = clockService.getEffectiveTime(year);
            LocalDate effectiveDate = clockService.getEffectiveDate(year);
            boolean isTestMode = clockService.isTestMode(year);

            // Check Daily Engine
            boolean shouldExecuteDaily = true;

            if (!Boolean.TRUE.equals(settings.getDailyEngineEnabled()) ||
                settings.getDailyProcessingTime() == null ||
                effectiveTime.isBefore(settings.getDailyProcessingTime()) ||
                (settings.getLastDailyRun() != null && settings.getLastDailyRun().toLocalDate().isEqual(effectiveDate))) {
                shouldExecuteDaily = false;
            }

            if (shouldExecuteDaily) {
                try {
                    dailyEngineService.execute(year);
                } catch (Exception e) {
                    log.error("Error executing Daily Engine for year {}: {}", year, e.getMessage(), e);
                }
            }

            // Fetch Active AcademicWeek for Weekly Engine
            AcademicWeek activeWeek = academicWeekRepository.findActiveWeekForDate(year, effectiveDate).orElse(null);

            // Check Weekly Engine
            boolean shouldExecuteWeekly = true;

            if (!Boolean.TRUE.equals(settings.getWeeklyEngineEnabled()) ||
                settings.getWeeklyProcessingTime() == null ||
                activeWeek == null || activeWeek.getEndDate() == null ||
                !effectiveDate.isEqual(activeWeek.getEndDate()) ||
                effectiveTime.isBefore(settings.getWeeklyProcessingTime()) ||
                (settings.getLastWeeklyRun() != null && settings.getLastWeeklyRun().toLocalDate().isEqual(effectiveDate))) {
                shouldExecuteWeekly = false;
            }

            if (shouldExecuteWeekly) {
                try {
                    weeklyEngineService.execute(year);
                } catch (Exception e) {
                    log.error("Error executing Weekly Engine for year {}: {}", year, e.getMessage(), e);
                }
            }
        }
    }
}
