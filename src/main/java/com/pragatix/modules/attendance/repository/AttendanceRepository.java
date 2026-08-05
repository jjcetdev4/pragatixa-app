package com.pragatix.modules.attendance.repository;

import com.pragatix.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByStudentIdAndAttendanceDateAndPeriodNo(Long studentId, LocalDate date, Integer periodNo);

    boolean existsByStudentIdInAndAttendanceDateAndPeriodNo(List<Long> studentIds, LocalDate date, Integer periodNo);

    long countByStudentIdAndAttendanceDate(Long studentId, LocalDate date);

    long countByStudentIdAndAttendanceDateAndStatus(Long studentId, LocalDate date, Attendance.AttendanceStatus status);

    long countByStudentIdAndStatus(Long studentId, Attendance.AttendanceStatus status);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.attendanceDate = :date AND a.periodNo = :period AND a.student.yearRef.id = :yearId AND a.student.department.id = :deptId AND (:sectionId IS NULL OR a.student.section.id = :sectionId) AND a.status = :status")
    long countBySessionDetailsAndStatus(@Param("date") LocalDate date, @Param("period") Integer period,
            @Param("yearId") Long yearId, @Param("deptId") Long deptId, @Param("sectionId") Long sectionId,
            @Param("status") Attendance.AttendanceStatus status);

    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate = :date AND a.periodNo = :period AND a.student.yearRef.id = :yearId AND a.student.department.id = :deptId AND (:sectionId IS NULL OR a.student.section.id = :sectionId) AND a.status = :status")
    List<Attendance> findBySessionDetailsAndStatus(@Param("date") LocalDate date, @Param("period") Integer period,
            @Param("yearId") Long yearId, @Param("deptId") Long deptId, @Param("sectionId") Long sectionId,
            @Param("status") Attendance.AttendanceStatus status);

    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate = :date AND a.student.yearRef.id = :yearId AND (:deptId IS NULL OR a.student.department.id = :deptId) AND (:sectionId IS NULL OR a.student.section.id = :sectionId)")
    List<Attendance> findBySessionDetails(@Param("date") LocalDate date, @Param("yearId") Long yearId, @Param("deptId") Long deptId, @Param("sectionId") Long sectionId);

    @Query("SELECT a FROM Attendance a WHERE a.student.id = :studentId ORDER BY a.attendanceDate DESC, a.periodNo DESC")
    List<Attendance> findByStudentIdOrderByAttendanceDateDescPeriodNoDesc(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND FUNCTION('MONTH', a.attendanceDate) = :month AND FUNCTION('YEAR', a.attendanceDate) = :year AND a.status = :status")
    long countByStudentIdAndMonthAndYearAndStatus(@Param("studentId") Long studentId, @Param("month") int month,
            @Param("year") int year, @Param("status") Attendance.AttendanceStatus status);

    @Query("SELECT a.attendanceDate FROM Attendance a WHERE a.student.id = :studentId GROUP BY a.attendanceDate ORDER BY a.attendanceDate DESC")
    List<LocalDate> findDistinctAttendanceDatesByStudentId(@Param("studentId") Long studentId);
}
