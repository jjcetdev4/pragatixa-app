package jjcet.PragatiX.modules.academiccalendar.repository;

import jjcet.PragatiX.entity.AcademicMonth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcademicMonthRepository extends JpaRepository<AcademicMonth, Long> {
    Optional<AcademicMonth> findByMonthAndYearAndAcademicYearEnum(Integer month, Integer year,
            jjcet.PragatiX.enums.AcademicYear academicYearEnum);
}
