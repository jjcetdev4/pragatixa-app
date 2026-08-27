package jjcet.PragatiX.modules.attendance.repository;

import jjcet.PragatiX.entity.AttendanceEngineExecution;
import jjcet.PragatiX.enums.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceEngineExecutionRepository extends JpaRepository<AttendanceEngineExecution, Long> {

    Optional<AttendanceEngineExecution> findFirstByAcademicYearAndEngineTypeAndPeriodStartAndPeriodEndAndStatus(
            AcademicYear academicYear, String engineType, LocalDate periodStart, LocalDate periodEnd, String status);

    Optional<AttendanceEngineExecution> findFirstByAcademicYearAndEngineTypeAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(
            AcademicYear academicYear, String engineType, LocalDate periodStart, LocalDate periodEnd);

    List<AttendanceEngineExecution> findByAcademicYearOrderByStartedAtDesc(AcademicYear academicYear);

    List<AttendanceEngineExecution> findTop20ByAcademicYearOrderByStartedAtDesc(AcademicYear academicYear);
}
