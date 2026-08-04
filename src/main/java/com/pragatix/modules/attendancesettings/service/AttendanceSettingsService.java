package com.pragatix.modules.attendancesettings.service;

import com.pragatix.entity.AttendanceSettings;
import com.pragatix.modules.attendancesettings.dto.AttendanceSettingsDto;
import com.pragatix.modules.attendancesettings.repository.AttendanceSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import com.pragatix.enums.AcademicYear;

@Service
public class AttendanceSettingsService {

    @Autowired
    private AttendanceSettingsRepository settingsRepository;

    // --- Settings ---

    public AttendanceSettingsDto getSettings(AcademicYear academicYear) {
        if (academicYear == null) academicYear = AcademicYear.FIRST_YEAR; // Fallback
        final AcademicYear finalYear = academicYear;

        AttendanceSettings settings = settingsRepository.findByAcademicYear(finalYear).orElseGet(() -> {
            AttendanceSettings defaultSettings = new AttendanceSettings();
            defaultSettings.setAcademicYear(finalYear);
            defaultSettings.setDailyEngineEnabled(false);
            defaultSettings.setDailyProcessingTime(LocalTime.of(17, 30));
            defaultSettings.setWeeklyEngineEnabled(false);
            defaultSettings.setWeeklyProcessingTime(LocalTime.of(18, 0));
            defaultSettings.setPartialDayPenalty(-5);
            defaultSettings.setFullDayPenalty(-10);
            defaultSettings.setPerfectWeekReward(30);
            return settingsRepository.save(defaultSettings);
        });
        return convertToDto(settings);
    }

    public AttendanceSettingsDto updateSettings(AttendanceSettingsDto dto, AcademicYear academicYear) {
        if (academicYear == null) academicYear = AcademicYear.FIRST_YEAR;
        final AcademicYear finalYear = academicYear;

        AttendanceSettings settings = settingsRepository.findByAcademicYear(finalYear).orElseGet(() -> {
            AttendanceSettings newSettings = new AttendanceSettings();
            newSettings.setAcademicYear(finalYear);
            return newSettings;
        });
        
        if (dto.getDailyEngineEnabled() != null) settings.setDailyEngineEnabled(dto.getDailyEngineEnabled());
        if (dto.getDailyProcessingTime() != null) settings.setDailyProcessingTime(dto.getDailyProcessingTime());
        if (dto.getWeeklyEngineEnabled() != null) settings.setWeeklyEngineEnabled(dto.getWeeklyEngineEnabled());
        if (dto.getWeeklyProcessingTime() != null) settings.setWeeklyProcessingTime(dto.getWeeklyProcessingTime());
        if (dto.getPartialDayPenalty() != null) settings.setPartialDayPenalty(dto.getPartialDayPenalty());
        if (dto.getFullDayPenalty() != null) settings.setFullDayPenalty(dto.getFullDayPenalty());
        if (dto.getPerfectWeekReward() != null) settings.setPerfectWeekReward(dto.getPerfectWeekReward());
        if (dto.getWeekStartFullPenalty() != null) settings.setWeekStartFullPenalty(dto.getWeekStartFullPenalty());
        if (dto.getWeekStartPartialPenalty() != null) settings.setWeekStartPartialPenalty(dto.getWeekStartPartialPenalty());
        if (dto.getWeekEndFullPenalty() != null) settings.setWeekEndFullPenalty(dto.getWeekEndFullPenalty());
        if (dto.getWeekEndPartialPenalty() != null) settings.setWeekEndPartialPenalty(dto.getWeekEndPartialPenalty());
        if (dto.getTestModeEnabled() != null) settings.setTestModeEnabled(dto.getTestModeEnabled());
        if (dto.getTestDate() != null && !dto.getTestDate().isEmpty()) {
            settings.setTestDate(java.time.LocalDate.parse(dto.getTestDate()));
        }
        if (dto.getTestTime() != null && !dto.getTestTime().isEmpty()) {
            settings.setTestTime(java.time.LocalTime.parse(dto.getTestTime()));
        }

        settings = settingsRepository.save(settings);
        return convertToDto(settings);
    }

    private AttendanceSettingsDto convertToDto(AttendanceSettings entity) {
        AttendanceSettingsDto dto = new AttendanceSettingsDto();
        dto.setId(entity.getId());
        dto.setAcademicYear(entity.getAcademicYear());
        dto.setDailyEngineEnabled(entity.getDailyEngineEnabled());
        dto.setDailyProcessingTime(entity.getDailyProcessingTime());
        dto.setWeeklyEngineEnabled(entity.getWeeklyEngineEnabled());
        dto.setWeeklyProcessingTime(entity.getWeeklyProcessingTime());
        dto.setPartialDayPenalty(entity.getPartialDayPenalty());
        dto.setFullDayPenalty(entity.getFullDayPenalty());
        dto.setPerfectWeekReward(entity.getPerfectWeekReward());
        dto.setWeekStartFullPenalty(entity.getWeekStartFullPenalty());
        dto.setWeekStartPartialPenalty(entity.getWeekStartPartialPenalty());
        dto.setWeekEndFullPenalty(entity.getWeekEndFullPenalty());
        dto.setWeekEndPartialPenalty(entity.getWeekEndPartialPenalty());
        dto.setTestModeEnabled(entity.getTestModeEnabled());
        dto.setTestDate(entity.getTestDate() != null ? entity.getTestDate().toString() : null);
        dto.setTestTime(entity.getTestTime() != null ? entity.getTestTime().toString() : null);
        dto.setLastDailyRun(entity.getLastDailyRun() != null ? entity.getLastDailyRun().toString() : null);
        dto.setLastWeeklyRun(entity.getLastWeeklyRun() != null ? entity.getLastWeeklyRun().toString() : null);
        dto.setDailyEngineStatus(entity.getDailyEngineStatus());
        dto.setWeeklyEngineStatus(entity.getWeeklyEngineStatus());
        return dto;
    }

}
