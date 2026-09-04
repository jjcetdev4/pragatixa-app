package jjcet.PragatiX.modules.academiccalendar.service;

import jjcet.PragatiX.entity.AcademicHoliday;
import jjcet.PragatiX.entity.AlternateWorkingDay;
import jjcet.PragatiX.modules.academiccalendar.repository.AcademicHolidayRepository;
import jjcet.PragatiX.modules.academiccalendar.repository.AcademicWeekRepository;
import jjcet.PragatiX.modules.academiccalendar.repository.AlternateWorkingDayRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class AcademicCalendarResolver {

    private final AcademicHolidayRepository holidayRepository;
    private final AlternateWorkingDayRepository awdRepository;
    private final AcademicWeekRepository academicWeekRepository;

    public AcademicCalendarResolver(AcademicHolidayRepository holidayRepository,
            AlternateWorkingDayRepository awdRepository,
            AcademicWeekRepository academicWeekRepository) {
        this.holidayRepository = holidayRepository;
        this.awdRepository = awdRepository;
        this.academicWeekRepository = academicWeekRepository;
    }

    /**
     * Returns true if the date is an explicitly configured Alternate Working Day
     * for the specific Academic Year.
     */
    public boolean isAlternateWorkingDay(LocalDate date, jjcet.PragatiX.enums.AcademicYear academicYear) {
        if (academicYear == null)
            return false;
        return awdRepository.findByEffectiveDateAndAcademicMonth_AcademicYearEnum(date, academicYear).isPresent();
    }

    /**
     * Returns true if the date falls inside an active configured Academic Week.
     */
    public boolean isDateInConfiguredWeek(LocalDate date, jjcet.PragatiX.enums.AcademicYear academicYear) {
        if (academicYear == null)
            return false;
        return academicWeekRepository.findActiveWeekForDate(academicYear, date).isPresent();
    }

    /**
     * Validates whether attendance can be marked for the given date.
     * Throws an IllegalArgumentException with a clear reason if not allowed.
     */
    public void validateDateForAttendance(LocalDate date, jjcet.PragatiX.enums.AcademicYear academicYear) {
        if (academicYear == null) {
            return;
        }

        // 1. Check if explicitly configured as a Holiday
        if (holidayRepository.findByHolidayDateAndAcademicMonth_AcademicYearEnum(date, academicYear).isPresent()) {
            throw new IllegalArgumentException("Attendance cannot be marked. Today is configured as a Holiday.");
        }

        // 2. Check if Alternate Working Day (AWD overrides Sunday & default schedule)
        if (isAlternateWorkingDay(date, academicYear)) {
            return;
        }

        // 3. Sunday is a holiday by default
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Attendance cannot be marked on Sunday.");
        }

        // 4. Date must fall within a configured Academic Week
        if (!isDateInConfiguredWeek(date, academicYear)) {
            throw new IllegalArgumentException("Attendance cannot be marked. This date is not configured in the Academic Calendar.");
        }
    }

    /**
     * Returns true if the date is a holiday (either a Sunday and NOT an AWD,
     * or explicitly configured in AcademicHolidays).
     */
    public boolean isHoliday(LocalDate date, jjcet.PragatiX.enums.AcademicYear academicYear) {
        if (academicYear == null)
            return false;

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
    public boolean isWorkingDay(LocalDate date, jjcet.PragatiX.enums.AcademicYear academicYear) {
        if (academicYear == null)
            return false;
        if (isAlternateWorkingDay(date, academicYear))
            return true;
        if (isHoliday(date, academicYear))
            return false;
        return isDateInConfiguredWeek(date, academicYear);
    }

    /**
     * Resolves the effective academic day of the week for the given date.
     */
    public DayOfWeek getEffectiveAcademicDay(LocalDate date, jjcet.PragatiX.enums.AcademicYear academicYear) {
        if (academicYear == null)
            return date.getDayOfWeek();

        Optional<AlternateWorkingDay> awdOpt = awdRepository.findByEffectiveDateAndAcademicMonth_AcademicYearEnum(date,
                academicYear);

        if (awdOpt.isPresent()) {
            return DayOfWeek.valueOf(awdOpt.get().getWorkingDay().toUpperCase());
        }

        if (isHoliday(date, academicYear)) {
            return null;
        }

        return date.getDayOfWeek();
    }

    public LocalDate getEffectiveWorkingDate(LocalDate date, jjcet.PragatiX.enums.AcademicYear academicYear) {
        if (isHoliday(date, academicYear)) {
            return null;
        }
        return date;
    }
}
