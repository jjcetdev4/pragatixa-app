package jjcet.PragatiX.modules.attendance.service;

import jjcet.PragatiX.entity.Attendance;
import jjcet.PragatiX.modules.attendance.dto.response.StudentAttendanceHistoryResponse;
import jjcet.PragatiX.modules.attendance.dto.response.StudentAttendanceSummaryResponse;
import jjcet.PragatiX.modules.attendance.repository.AttendanceRepository;
import jjcet.PragatiX.repository.StreakRepository;
import jjcet.PragatiX.entity.Streak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudentAttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StreakRepository streakRepository;

    @Transactional(readOnly = true)
    public StudentAttendanceSummaryResponse getSummary(Long studentId) {
        List<Attendance> allRecords = attendanceRepository.findByStudentIdOrderByAttendanceDateDescPeriodNoDesc(studentId);

        // Group attendance records by attendance date to calculate DAY-WISE counts
        Map<LocalDate, List<Attendance>> recordsByDate = allRecords.stream()
                .filter(r -> r.getAttendanceDate() != null)
                .collect(Collectors.groupingBy(Attendance::getAttendanceDate));

        long totalPresentDays = 0;
        long totalAbsentDays = 0;

        LocalDate now = LocalDate.now();
        long monthPresentDays = 0;
        long monthAbsentDays = 0;

        for (Map.Entry<LocalDate, List<Attendance>> entry : recordsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<Attendance> dayRecords = entry.getValue();

            if (dayRecords.isEmpty()) {
                continue;
            }

            long presentCount = dayRecords.stream()
                    .filter(r -> r.getStatus() == Attendance.AttendanceStatus.PRESENT || r.getStatus() == Attendance.AttendanceStatus.OD)
                    .count();
            long absentCount = dayRecords.stream()
                    .filter(r -> r.getStatus() == Attendance.AttendanceStatus.ABSENT)
                    .count();

            // A student is considered Present for the Day if they have no absences on that marked day
            boolean isPresentDay = (absentCount == 0 && presentCount > 0);
            boolean isAbsentDay = (absentCount > 0);

            if (isPresentDay) {
                totalPresentDays++;
                if (date.getMonthValue() == now.getMonthValue() && date.getYear() == now.getYear()) {
                    monthPresentDays++;
                }
            } else if (isAbsentDay) {
                totalAbsentDays++;
                if (date.getMonthValue() == now.getMonthValue() && date.getYear() == now.getYear()) {
                    monthAbsentDays++;
                }
            }
        }

        long totalDays = totalPresentDays + totalAbsentDays;
        double overallPercentage = totalDays == 0 ? 0.0 : ((double) totalPresentDays / totalDays) * 100.0;

        long monthTotalDays = monthPresentDays + monthAbsentDays;
        double monthlyPercentage = monthTotalDays == 0 ? 0.0 : ((double) monthPresentDays / monthTotalDays) * 100.0;

        Streak existingStreak = streakRepository.findByStudentIdAndStreakType(studentId, "ATTENDANCE").orElse(null);
        int streak = existingStreak != null ? existingStreak.getCurrentStreak() : 0;

        StudentAttendanceSummaryResponse res = new StudentAttendanceSummaryResponse();
        res.setAttendancePercentage(Math.round(overallPercentage * 100.0) / 100.0);
        res.setMonthlyAttendancePercentage(Math.round(monthlyPercentage * 100.0) / 100.0);
        res.setCurrentStreak(streak);
        res.setTotalPresentDays(totalPresentDays);
        res.setTotalAbsentDays(totalAbsentDays);

        return res;
    }

    @Transactional(readOnly = true)
    public List<StudentAttendanceHistoryResponse> getHistory(Long studentId, LocalDate date) {
        List<Attendance> records;
        if (date != null) {
            records = attendanceRepository.findByStudentIdAndAttendanceDateOrderByPeriodNoDesc(studentId, date);
        } else {
            records = attendanceRepository.findByStudentIdOrderByAttendanceDateDescPeriodNoDesc(studentId);
        }
        return records.stream().map(r -> {
            StudentAttendanceHistoryResponse res = new StudentAttendanceHistoryResponse();
            res.setDate(r.getAttendanceDate());
            res.setPeriod(r.getPeriodNo());
            res.setStatus(jjcet.PragatiX.entity.AttendanceRecord.AttendanceStatus.valueOf(r.getStatus().name()));
            res.setRemarks(r.getRemarks());
            return res;
        }).collect(Collectors.toList());
    }
}
