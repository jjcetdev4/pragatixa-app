package com.pragatix.modules.attendancesettings.service;

import com.pragatix.entity.AttendanceSettings;
import com.pragatix.enums.AcademicYear;
import com.pragatix.modules.attendancesettings.repository.AttendanceSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * EngineClockService — Single source of truth for the "effective" engine timestamp.
 *
 * - Production Mode: uses the real system clock (LocalDate.now() / LocalDateTime.now()).
 * - Test Mode:       uses the administrator-configured test date and time stored in AttendanceSettings.
 *
 * IMPORTANT: Only attendance engine code should call this service.
 * JWT, login, audit logs, and all other modules must continue using LocalDate/LocalDateTime.now() directly.
 */
@Service
public class EngineClockService {

    @Autowired
    private AttendanceSettingsRepository settingsRepository;

    /**
     * Returns the effective date for the engine.
     * In Test Mode: the configured testDate. In Production: LocalDate.now().
     */
    public LocalDate getEffectiveDate(AcademicYear academicYear) {
        AttendanceSettings settings = getSettings(academicYear);
        if (settings != null && Boolean.TRUE.equals(settings.getTestModeEnabled()) && settings.getTestDate() != null) {
            return settings.getTestDate();
        }
        return LocalDate.now();
    }

    /**
     * Returns the effective time for the engine.
     * In Test Mode: the configured testTime (or midnight if not set). In Production: LocalTime.now().
     */
    public LocalTime getEffectiveTime(AcademicYear academicYear) {
        AttendanceSettings settings = getSettings(academicYear);
        if (settings != null && Boolean.TRUE.equals(settings.getTestModeEnabled())) {
            return settings.getTestTime() != null ? settings.getTestTime() : LocalTime.MIDNIGHT;
        }
        return LocalTime.now();
    }

    /**
     * Returns the effective full datetime for the engine.
     */
    public LocalDateTime getEffectiveDateTime(AcademicYear academicYear) {
        return LocalDateTime.of(getEffectiveDate(academicYear), getEffectiveTime(academicYear));
    }

    /**
     * Returns true if the engine is in Test Mode for this Academic Year.
     */
    public boolean isTestMode(AcademicYear academicYear) {
        AttendanceSettings settings = getSettings(academicYear);
        return settings != null && Boolean.TRUE.equals(settings.getTestModeEnabled());
    }

    private AttendanceSettings getSettings(AcademicYear academicYear) {
        if (academicYear == null) return null;
        return settingsRepository.findByAcademicYear(academicYear).orElse(null);
    }
}
