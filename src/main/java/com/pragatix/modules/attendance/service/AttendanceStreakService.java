package com.pragatix.modules.attendance.service;

import com.pragatix.modules.attendance.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.pragatix.entity.Streak;
import com.pragatix.entity.Student;
import com.pragatix.repository.StreakRepository;
import com.pragatix.entity.Attendance;

@Service
public class AttendanceStreakService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StreakRepository streakRepository;

    @Transactional
    public void updateAttendanceStreak(Student student, LocalDate date) {
        long totalRecordsForDay = attendanceRepository.countByStudentIdAndAttendanceDate(student.getId(), date);

        if (totalRecordsForDay != 8) {
            // Day is incomplete, do not calculate streak yet.
            return;
        }

        long presentCount = attendanceRepository.countByStudentIdAndAttendanceDateAndStatus(student.getId(), date,
                Attendance.AttendanceStatus.PRESENT);

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

        if (presentCount == 8) {
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            streak.setBroken(false);
        } else {
            streak.setCurrentStreak(0);
            streak.setBroken(true);
        }

        streak.setLastUpdated(LocalDateTime.now());
        streakRepository.save(streak);
    }
}
