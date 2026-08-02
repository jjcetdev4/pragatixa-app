package com.pragatix.modules.academiccalendar.repository;

import com.pragatix.entity.AcademicWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicWeekRepository extends JpaRepository<AcademicWeek, Long> {
    List<AcademicWeek> findByAcademicMonthId(Long academicMonthId);

    @org.springframework.data.jpa.repository.Query("SELECT aw FROM AcademicWeek aw WHERE aw.academicMonth.academicYearEnum = :academicYear AND :engineDate BETWEEN aw.startDate AND aw.endDate")
    java.util.Optional<AcademicWeek> findActiveWeekForDate(@org.springframework.data.repository.query.Param("academicYear") com.pragatix.enums.AcademicYear academicYear, @org.springframework.data.repository.query.Param("engineDate") java.time.LocalDate engineDate);
}
