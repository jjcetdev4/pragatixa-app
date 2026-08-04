package com.pragatix.modules.academiccalendar.repository;

import com.pragatix.entity.AlternateWorkingDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlternateWorkingDayRepository extends JpaRepository<AlternateWorkingDay, Long> {
    List<AlternateWorkingDay> findByAcademicMonthId(Long academicMonthId);
    java.util.Optional<AlternateWorkingDay> findByEffectiveDateAndAcademicMonth_AcademicYearEnum(java.time.LocalDate effectiveDate, com.pragatix.enums.AcademicYear academicYearEnum);
}
