package com.pragatix.modules.attendance.service;

import com.pragatix.entity.Attendance;
import com.pragatix.modules.attendance.dto.response.StudentAttendanceHistoryResponse;
import com.pragatix.modules.attendance.dto.response.StudentAttendanceSummaryResponse;
import com.pragatix.modules.attendance.repository.AttendanceRepository;
import com.pragatix.repository.StreakRepository;
import com.pragatix.entity.Streak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentAttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StreakRepository streakRepository;

    @Transactional(readOnly = true)
    public StudentAttendanceSummaryResponse getSummary(Long studentId) {
        long totalPresent = attendanceRepository.countByStudentIdAndStatus(studentId,
                Attendance.AttendanceStatus.PRESENT);
        long totalAbsent = attendanceRepository.countByStudentIdAndStatus(studentId,
                Attendance.AttendanceStatus.ABSENT);
        long totalDays = totalPresent + totalAbsent;
        double overallPercentage = totalDays == 0 ? 0 : ((double) totalPresent / totalDays) * 100.0;

        LocalDate now = LocalDate.now();
        long monthPresent = attendanceRepository.countByStudentIdAndMonthAndYearAndStatus(studentId,
                now.getMonthValue(), now.getYear(), Attendance.AttendanceStatus.PRESENT);
        long monthAbsent = attendanceRepository.countByStudentIdAndMonthAndYearAndStatus(studentId, now.getMonthValue(),
                now.getYear(), Attendance.AttendanceStatus.ABSENT);
        long monthTotal = monthPresent + monthAbsent;
        double monthlyPercentage = monthTotal == 0 ? 0 : ((double) monthPresent / monthTotal) * 100.0;

        Streak existingStreak = streakRepository.findByStudentIdAndStreakType(studentId, "ATTENDANCE").orElse(null);
        int streak = existingStreak != null ? existingStreak.getCurrentStreak() : 0;

        StudentAttendanceSummaryResponse res = new StudentAttendanceSummaryResponse();
        res.setAttendancePercentage(Math.round(overallPercentage * 100.0) / 100.0);
        res.setMonthlyAttendancePercentage(Math.round(monthlyPercentage * 100.0) / 100.0);
        res.setCurrentStreak(streak);
        res.setTotalPresentDays(totalPresent);
        res.setTotalAbsentDays(totalAbsent);

        return res;
    }

    @Transactional(readOnly = true)
    public List<StudentAttendanceHistoryResponse> getHistory(Long studentId) {
        List<Attendance> records = attendanceRepository.findByStudentIdOrderByAttendanceDateDescPeriodNoDesc(studentId);
        return records.stream().map(r -> {
            StudentAttendanceHistoryResponse res = new StudentAttendanceHistoryResponse();
            res.setDate(r.getAttendanceDate());
            res.setPeriod(r.getPeriodNo());
            res.setStatus(com.pragatix.entity.AttendanceRecord.AttendanceStatus.valueOf(r.getStatus().name()));
            res.setRemarks(r.getRemarks());
            return res;
        }).collect(Collectors.toList());
    }
}
