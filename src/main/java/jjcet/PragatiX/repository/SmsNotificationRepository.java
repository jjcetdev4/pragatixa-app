package jjcet.PragatiX.repository;

import jjcet.PragatiX.entity.SmsNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
public interface SmsNotificationRepository extends JpaRepository<SmsNotification, Long> {
    boolean existsByStudentIdAndCreatedAtBetween(Long studentId, LocalDateTime start, LocalDateTime end);

    boolean existsByStudentIdAndAttendanceDate(Long studentId, LocalDate attendanceDate);

    boolean existsByStudentIdAndAttendanceDateAndStatus(Long studentId, LocalDate attendanceDate, String status);
}
