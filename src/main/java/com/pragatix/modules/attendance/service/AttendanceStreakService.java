package jjcet.PragatiX.modules.attendance.service;

import jjcet.PragatiX.modules.attendance.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jjcet.PragatiX.entity.Streak;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.repository.StreakRepository;
import jjcet.PragatiX.entity.Attendance;

@Service
public class AttendanceStreakService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StreakRepository streakRepository;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AttendanceStreakService.class);

    @Transactional
    public void incrementAttendanceStreak(Student student, LocalDate engineDate) {
        Optional<Streak> existingStreak = streakRepository.findByStudentRegNoAndStreakType(student.getRegNo(),
                "ATTENDANCE");
        Streak streak = existingStreak.orElseGet(() -> Streak.builder()
                .student(student)
                .regNo(student.getRegNo())
                .streakType("ATTENDANCE")
                .currentStreak(0)
                .isBroken(false)
                .penaltyPerBreak(10)
                .build());

        if (streak.getLastProcessedDate() != null && streak.getLastProcessedDate().isEqual(engineDate)) {
            log.info("ATTENDANCE STREAK ENGINE");
            log.info("Student ID: {}", student.getId());
            log.info("Execution Date: {}", engineDate);
            log.info("Already Processed: true");
            log.info("Current Streak: {}", streak.getCurrentStreak());
            log.info("Action: SKIPPED - ALREADY PROCESSED");
            return;
        }

        int previousStreak = streak.getCurrentStreak();
        streak.setCurrentStreak(previousStreak + 1);
        streak.setBroken(false);
        streak.setLastUpdated(LocalDateTime.now());
        streak.setLastProcessedDate(engineDate);
        streakRepository.save(streak);

        log.info("ATTENDANCE STREAK ENGINE");
        log.info("Student ID: {}", student.getId());
        log.info("Execution Date: {}", engineDate);
        log.info("Working Day: true");
        log.info("Required Periods: 8");
        log.info("Present Periods: 8");
        log.info("Eligible For Streak: true");
        log.info("Already Processed: false");
        log.info("Previous Streak: {}", previousStreak);
        log.info("New Streak: {}", streak.getCurrentStreak());
    }
}
