package com.pragatix.modules.academiccalendar.service;

import com.pragatix.entity.AcademicHoliday;
import com.pragatix.entity.AlternateWorkingDay;
import com.pragatix.modules.academiccalendar.repository.AcademicHolidayRepository;
import com.pragatix.modules.academiccalendar.repository.AlternateWorkingDayRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class AcademicCalendarResolver {

    private final AcademicHolidayRepository holidayRepository;
    private final AlternateWorkingDayRepository awdRepository;

    public AcademicCalendarResolver(AcademicHolidayRepository holidayRepository,
                                    AlternateWorkingDayRepository awdRepository) {
        this.holidayRepository = holidayRepository;
        this.awdRepository = awdRepository;
    }

    /**
     * Returns true if the date is an explicitly configured Alternate Working Day for the specific Academic Year.
     */
    public boolean isAlternateWorkingDay(LocalDate date, com.pragatix.enums.AcademicYear academicYear) {
        if (academicYear == null) return false;
        return awdRepository.findByEffectiveDateAndAcademicMonth_AcademicYearEnum(date, academicYear).isPresent();
    }

    /**
     * Returns true if the date is a holiday (either a Sunday and NOT an AWD,
     * or explicitly configured in AcademicHolidays).
     */
    public boolean isHoliday(LocalDate date, com.pragatix.enums.AcademicYear academicYear) {
        if (academicYear == null) return false;
        
        if (isAlternateWorkingDay(date, academicYear)) {
            return false;
        }

        // Sunday by default is a holiday unless overridden by AWD
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return true;
        }

        return holidayRepository.findByHolidayDateAndAcademicMonth_AcademicYearEnum(date, academicYear).isPresent();
    }

    /**
     * Returns true if the date is considered a working day.
     */
    public boolean isWorkingDay(LocalDate date, com.pragatix.enums.AcademicYear academicYear) {
        return !isHoliday(date, academicYear);
    }

    /**
     * Resolves the effective academic day of the week for the given date.
     */
    public DayOfWeek getEffectiveAcademicDay(LocalDate date, com.pragatix.enums.AcademicYear academicYear) {
        if (academicYear == null) return date.getDayOfWeek();

        Optional<AlternateWorkingDay> awdOpt = awdRepository.findByEffectiveDateAndAcademicMonth_AcademicYearEnum(date, academicYear);

        if (awdOpt.isPresent()) {
            return DayOfWeek.valueOf(awdOpt.get().getWorkingDay().toUpperCase());
        }

        if (isHoliday(date, academicYear)) {
            return null; 
        }

        return date.getDayOfWeek();
    }

    public LocalDate getEffectiveWorkingDate(LocalDate date, com.pragatix.enums.AcademicYear academicYear) {
        if (isHoliday(date, academicYear)) {
            return null;
        }
        return date;
    }
}
