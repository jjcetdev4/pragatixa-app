package com.pragatix.modules.academiccalendar.repository;

import com.pragatix.entity.AcademicMonth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcademicMonthRepository extends JpaRepository<AcademicMonth, Long> {
    Optional<AcademicMonth> findByMonthAndYearAndAcademicYearEnum(Integer month, Integer year, com.pragatix.enums.AcademicYear academicYearEnum);
}
