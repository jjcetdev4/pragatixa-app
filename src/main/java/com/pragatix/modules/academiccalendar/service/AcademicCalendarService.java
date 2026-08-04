package com.pragatix.modules.academiccalendar.service;

import com.pragatix.entity.AcademicHoliday;
import com.pragatix.entity.AcademicMonth;
import com.pragatix.entity.AcademicWeek;
import com.pragatix.entity.AlternateWorkingDay;
import com.pragatix.modules.academiccalendar.dto.AcademicHolidayDto;
import com.pragatix.modules.academiccalendar.dto.AcademicMonthDto;
import com.pragatix.modules.academiccalendar.dto.AcademicWeekDto;
import com.pragatix.modules.academiccalendar.dto.AlternateWorkingDayDto;
import com.pragatix.modules.academiccalendar.repository.AcademicHolidayRepository;
import com.pragatix.modules.academiccalendar.repository.AcademicMonthRepository;
import com.pragatix.modules.academiccalendar.repository.AcademicWeekRepository;
import com.pragatix.modules.academiccalendar.repository.AlternateWorkingDayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AcademicCalendarService {

    @Autowired
    private AcademicMonthRepository monthRepository;

    @Autowired
    private AcademicWeekRepository weekRepository;

    @Autowired
    private AcademicHolidayRepository holidayRepository;

    @Autowired
    private AlternateWorkingDayRepository alternateWorkingDayRepository;

    // --- Academic Month ---

    public AcademicMonthDto getOrCreateMonth(Integer month, Integer year, com.pragatix.enums.AcademicYear academicYear) {
        AcademicMonth academicMonth = monthRepository.findByMonthAndYearAndAcademicYearEnum(month, year, academicYear).orElseGet(() -> {
            AcademicMonth newMonth = new AcademicMonth();
            newMonth.setMonth(month);
            newMonth.setYear(year);
            newMonth.setAcademicYearEnum(academicYear);
            return monthRepository.save(newMonth);
        });
        return convertToDto(academicMonth);
    }

    private AcademicMonthDto convertToDto(AcademicMonth entity) {
        AcademicMonthDto dto = new AcademicMonthDto();
        dto.setId(entity.getId());
        dto.setMonth(entity.getMonth());
        dto.setYear(entity.getYear());
        dto.setAcademicYearEnum(entity.getAcademicYearEnum());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    // --- Academic Week ---

    public List<AcademicWeekDto> getWeeksForMonth(Long monthId) {
        return weekRepository.findByAcademicMonthId(monthId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public AcademicWeekDto addWeek(AcademicWeekDto dto) {
        AcademicMonth month = monthRepository.findById(dto.getAcademicMonthId())
                .orElseThrow(() -> new RuntimeException("Academic Month not found"));

        validateWeekDates(dto, month, null);

        AcademicWeek week = new AcademicWeek();
        week.setAcademicMonth(month);
        week.setWeekNumber(dto.getWeekNumber());
        week.setStartDate(dto.getStartDate());
        week.setEndDate(dto.getEndDate());
        return convertToDto(weekRepository.save(week));
    }

    public AcademicWeekDto updateWeek(Long id, AcademicWeekDto dto) {
        AcademicWeek week = weekRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Academic Week not found"));

        validateWeekDates(dto, week.getAcademicMonth(), id);

        week.setWeekNumber(dto.getWeekNumber());
        week.setStartDate(dto.getStartDate());
        week.setEndDate(dto.getEndDate());
        return convertToDto(weekRepository.save(week));
    }

    public void deleteWeek(Long id) {
        weekRepository.deleteById(id);
    }

    private void validateWeekDates(AcademicWeekDto dto, AcademicMonth month, Long excludeWeekId) {
        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        List<AcademicWeek> existingWeeks = weekRepository.findByAcademicMonthId(month.getId());
        for (AcademicWeek ew : existingWeeks) {
            if (excludeWeekId != null && ew.getId().equals(excludeWeekId)) {
                continue;
            }
            if (!dto.getEndDate().isBefore(ew.getStartDate()) && !dto.getStartDate().isAfter(ew.getEndDate())) {
                throw new IllegalArgumentException("Week dates cannot overlap with existing configured weeks");
            }
        }
    }

    private AcademicWeekDto convertToDto(AcademicWeek entity) {
        AcademicWeekDto dto = new AcademicWeekDto();
        dto.setId(entity.getId());
        dto.setAcademicMonthId(entity.getAcademicMonth().getId());
        dto.setWeekNumber(entity.getWeekNumber());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        return dto;
    }

    // --- Academic Holiday ---

    public List<AcademicHolidayDto> getHolidaysForMonth(Long monthId) {
        return holidayRepository.findByAcademicMonthId(monthId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public AcademicHolidayDto addHoliday(AcademicHolidayDto dto) {
        AcademicMonth month = monthRepository.findById(dto.getAcademicMonthId())
                .orElseThrow(() -> new RuntimeException("Academic Month not found"));

        if (dto.getHolidayDate().getMonthValue() != month.getMonth()) {
            throw new IllegalArgumentException("Holiday date must fall within the selected month");
        }

        boolean isAwd = alternateWorkingDayRepository.findAll().stream()
                .anyMatch(a -> a.getEffectiveDate().equals(dto.getHolidayDate()));
        if (isAwd) {
            throw new IllegalArgumentException("This date is already configured as an Alternate Working Day. Remove it first before marking this date as a Holiday.");
        }

        AcademicHoliday holiday = new AcademicHoliday();
        holiday.setAcademicMonth(month);
        holiday.setHolidayName(dto.getHolidayName());
        holiday.setHolidayDate(dto.getHolidayDate());
        return convertToDto(holidayRepository.save(holiday));
    }

    public AcademicHolidayDto updateHoliday(Long id, AcademicHolidayDto dto) {
        AcademicHoliday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Academic Holiday not found"));

        if (dto.getHolidayDate().getMonthValue() != holiday.getAcademicMonth().getMonth()) {
            throw new IllegalArgumentException("Holiday date must fall within the selected month");
        }

        boolean isAwd = alternateWorkingDayRepository.findAll().stream()
                .filter(a -> !a.getId().equals(id)) // not needed strictly since they are different entities, but good for safety
                .anyMatch(a -> a.getEffectiveDate().equals(dto.getHolidayDate()));
        if (isAwd) {
            throw new IllegalArgumentException("This date is already configured as an Alternate Working Day. Remove it first before marking this date as a Holiday.");
        }

        holiday.setHolidayName(dto.getHolidayName());
        holiday.setHolidayDate(dto.getHolidayDate());
        return convertToDto(holidayRepository.save(holiday));
    }

    public void deleteHoliday(Long id) {
        holidayRepository.deleteById(id);
    }

    private AcademicHolidayDto convertToDto(AcademicHoliday entity) {
        AcademicHolidayDto dto = new AcademicHolidayDto();
        dto.setId(entity.getId());
        dto.setAcademicMonthId(entity.getAcademicMonth().getId());
        dto.setHolidayName(entity.getHolidayName());
        dto.setHolidayDate(entity.getHolidayDate());
        return dto;
    }

    // --- Alternate Working Day ---

    public List<AlternateWorkingDayDto> getAlternateWorkingDaysForMonth(Long monthId) {
        return alternateWorkingDayRepository.findByAcademicMonthId(monthId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public AlternateWorkingDayDto addAlternateWorkingDay(AlternateWorkingDayDto dto) {
        AcademicMonth month = monthRepository.findById(dto.getAcademicMonthId())
                .orElseThrow(() -> new RuntimeException("Academic Month not found"));

        boolean isHoliday = holidayRepository.findAll().stream()
                .anyMatch(h -> h.getHolidayDate().equals(dto.getEffectiveDate()));
        if (isHoliday) {
            throw new IllegalArgumentException("This date is already configured as a Holiday. Remove the Holiday first before creating an Alternate Working Day.");
        }

        AlternateWorkingDay awd = new AlternateWorkingDay();
        awd.setAcademicMonth(month);
        awd.setEffectiveDate(dto.getEffectiveDate());
        awd.setOriginalHolidayDay(dto.getOriginalHolidayDay());
        awd.setWorkingDay(dto.getWorkingDay());
        awd.setReason(dto.getReason());
        
        return convertToDto(alternateWorkingDayRepository.save(awd));
    }

    public AlternateWorkingDayDto updateAlternateWorkingDay(Long id, AlternateWorkingDayDto dto) {
        AlternateWorkingDay awd = alternateWorkingDayRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alternate Working Day not found"));

        boolean isHoliday = holidayRepository.findAll().stream()
                .anyMatch(h -> h.getHolidayDate().equals(dto.getEffectiveDate()));
        if (isHoliday) {
            throw new IllegalArgumentException("This date is already configured as a Holiday. Remove the Holiday first before creating an Alternate Working Day.");
        }

        awd.setEffectiveDate(dto.getEffectiveDate());
        awd.setOriginalHolidayDay(dto.getOriginalHolidayDay());
        awd.setWorkingDay(dto.getWorkingDay());
        awd.setReason(dto.getReason());
        
        return convertToDto(alternateWorkingDayRepository.save(awd));
    }

    public void deleteAlternateWorkingDay(Long id) {
        alternateWorkingDayRepository.deleteById(id);
    }

    private AlternateWorkingDayDto convertToDto(AlternateWorkingDay entity) {
        AlternateWorkingDayDto dto = new AlternateWorkingDayDto();
        dto.setId(entity.getId());
        dto.setAcademicMonthId(entity.getAcademicMonth().getId());
        dto.setEffectiveDate(entity.getEffectiveDate());
        dto.setOriginalHolidayDay(entity.getOriginalHolidayDay());
        dto.setWorkingDay(entity.getWorkingDay());
        dto.setReason(entity.getReason());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
