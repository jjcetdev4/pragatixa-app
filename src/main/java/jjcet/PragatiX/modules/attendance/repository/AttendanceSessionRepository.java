package jjcet.PragatiX.modules.attendance.repository;

import jjcet.PragatiX.entity.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

        Optional<AttendanceSession> findFirstByAttendanceDateAndPeriodNumberAndDepartmentIdAndSectionIdAndYearId(
                        LocalDate attendanceDate, Integer periodNumber, Long departmentId, Long sectionId, Long yearId);

        Optional<AttendanceSession> findFirstByAttendanceDateAndPeriodNumberAndDepartmentIdAndSectionIsNullAndYearId(
                        LocalDate attendanceDate, Integer periodNumber, Long departmentId, Long yearId);

        Optional<AttendanceSession> findByAttendanceDateAndPeriodNumberAndDepartmentIdAndSectionIdAndYearId(
                        LocalDate attendanceDate, Integer periodNumber, Long departmentId, Long sectionId, Long yearId);

        Optional<AttendanceSession> findByAttendanceDateAndPeriodNumberAndDepartmentIdAndSectionIsNullAndYearId(
                        LocalDate attendanceDate, Integer periodNumber, Long departmentId, Long yearId);
}
