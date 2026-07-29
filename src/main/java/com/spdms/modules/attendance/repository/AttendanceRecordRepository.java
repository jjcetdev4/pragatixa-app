package com.pragatix.modules.attendance.repository;

import com.pragatix.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findByAttendanceSessionId(Long attendanceSessionId);

    Optional<AttendanceRecord> findByAttendanceSessionIdAndStudentId(Long attendanceSessionId, Long studentId);

    // For admin dashboard statistics
    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar JOIN ar.attendanceSession as session WHERE session.attendanceDate = :date AND session.periodNumber = :period AND session.year.id = :yearId AND session.department.id = :deptId AND (:sectionId IS NULL OR session.section.id = :sectionId) AND ar.status = 'PRESENT'")
    long countPresentBySessionDetails(@Param("date") LocalDate date, @Param("period") Integer period,
            @Param("yearId") Long yearId, @Param("deptId") Long deptId, @Param("sectionId") Long sectionId);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar JOIN ar.attendanceSession as session WHERE session.attendanceDate = :date AND session.periodNumber = :period AND session.year.id = :yearId AND session.department.id = :deptId AND (:sectionId IS NULL OR session.section.id = :sectionId) AND ar.status = 'ABSENT'")
    long countAbsentBySessionDetails(@Param("date") LocalDate date, @Param("period") Integer period,
            @Param("yearId") Long yearId, @Param("deptId") Long deptId, @Param("sectionId") Long sectionId);

    @Query("SELECT ar FROM AttendanceRecord ar JOIN ar.attendanceSession as session WHERE session.attendanceDate = :date AND session.periodNumber = :period AND session.year.id = :yearId AND session.department.id = :deptId AND (:sectionId IS NULL OR session.section.id = :sectionId) AND ar.status = :status")
    List<AttendanceRecord> findBySessionDetailsAndStatus(@Param("date") LocalDate date, @Param("period") Integer period,
            @Param("yearId") Long yearId, @Param("deptId") Long deptId, @Param("sectionId") Long sectionId,
            @Param("status") AttendanceRecord.AttendanceStatus status);

    // For student history
    @Query("SELECT ar FROM AttendanceRecord ar JOIN FETCH ar.attendanceSession session WHERE ar.student.id = :studentId ORDER BY session.attendanceDate DESC, session.periodNumber DESC")
    List<AttendanceRecord> findAllByStudentIdOrderByDateAndPeriod(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.student.id = :studentId AND ar.status = 'PRESENT'")
    long countTotalPresentByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.student.id = :studentId AND ar.status = 'ABSENT'")
    long countTotalAbsentByStudentId(@Param("studentId") Long studentId);

    // Monthly stats
    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar JOIN ar.attendanceSession session WHERE ar.student.id = :studentId AND FUNCTION('MONTH', session.attendanceDate) = :month AND FUNCTION('YEAR', session.attendanceDate) = :year AND ar.status = 'PRESENT'")
    long countPresentByStudentIdAndMonth(@Param("studentId") Long studentId, @Param("month") int month,
            @Param("year") int year);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar JOIN ar.attendanceSession session WHERE ar.student.id = :studentId AND FUNCTION('MONTH', session.attendanceDate) = :month AND FUNCTION('YEAR', session.attendanceDate) = :year AND ar.status = 'ABSENT'")
    long countAbsentByStudentIdAndMonth(@Param("studentId") Long studentId, @Param("month") int month,
            @Param("year") int year);

    // For streak calculations: need to check if a student was fully present on
    // specific dates
    @Query("SELECT session.attendanceDate FROM AttendanceRecord ar JOIN ar.attendanceSession session WHERE ar.student.id = :studentId GROUP BY session.attendanceDate ORDER BY session.attendanceDate DESC")
    List<LocalDate> findDistinctAttendanceDatesByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar JOIN ar.attendanceSession session WHERE ar.student.id = :studentId AND session.attendanceDate = :date AND ar.status = 'ABSENT'")
    long countAbsentPeriodsForStudentAndDate(@Param("studentId") Long studentId, @Param("date") LocalDate date);
}
