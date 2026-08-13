package jjcet.PragatiX.modules.academiccalendar.repository;

import jjcet.PragatiX.entity.AcademicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicHolidayRepository extends JpaRepository<AcademicHoliday, Long> {
    List<AcademicHoliday> findByAcademicMonthId(Long academicMonthId);

    java.util.Optional<AcademicHoliday> findByHolidayDateAndAcademicMonth_AcademicYearEnum(
            java.time.LocalDate holidayDate, jjcet.PragatiX.enums.AcademicYear academicYearEnum);
}
