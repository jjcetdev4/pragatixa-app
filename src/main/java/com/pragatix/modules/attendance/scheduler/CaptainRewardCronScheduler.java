package com.pragatix.modules.attendance.scheduler;

import com.pragatix.entity.CaptainRewardSettings;
import com.pragatix.enums.AcademicYear;
import com.pragatix.modules.attendance.repository.CaptainRewardSettingsRepository;
import com.pragatix.modules.attendance.service.CaptainWeeklyEngineService;
import com.pragatix.modules.attendancesettings.service.EngineClockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class CaptainRewardCronScheduler {

    private static final Logger log = LoggerFactory.getLogger(CaptainRewardCronScheduler.class);

    @Autowired
    private CaptainRewardSettingsRepository settingsRepository;

    @Autowired
    private CaptainWeeklyEngineService weeklyEngineService;

    @Autowired
    private EngineClockService clockService;

    @Scheduled(cron = "0 * * * * *")
    public void runScheduler() {
        List<CaptainRewardSettings> allSettings = settingsRepository.findAll();

        for (CaptainRewardSettings settings : allSettings) {
            AcademicYear year = settings.getAcademicYear();
            if (year == null || !Boolean.TRUE.equals(settings.getEngineEnabled())) {
                continue;
            }

            try {
                weeklyEngineService.execute(year);
            } catch (Exception e) {
                log.error("Error executing Captain Reward Engine for year {}: {}", year, e.getMessage(), e);
            }
        }
    }
}
