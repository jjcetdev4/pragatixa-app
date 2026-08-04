package com.pragatix.modules.attendance.repository;

import com.pragatix.entity.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

        Optional<AttendanceSession> findByAttendanceDateAndPeriodNumberAndDepartmentIdAndSectionIdAndYearId(
                        LocalDate attendanceDate, Integer periodNumber, Long departmentId, Long sectionId, Long yearId);

        Optional<AttendanceSession> findByAttendanceDateAndPeriodNumberAndDepartmentIdAndSectionIsNullAndYearId(
                        LocalDate attendanceDate, Integer periodNumber, Long departmentId, Long yearId);
}
