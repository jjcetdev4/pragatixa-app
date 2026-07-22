package com.spdms.modules.attendance.service;

import com.spdms.entity.Attendance;
import com.spdms.modules.attendance.dto.response.AdminAttendanceSummaryResponse;
import com.spdms.modules.attendance.dto.response.StudentAttendanceListItemResponse;
import com.spdms.modules.attendance.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminAttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Transactional(readOnly = true)
    public AdminAttendanceSummaryResponse getDashboardSummary(LocalDate date, Integer period, Long yearId, Long deptId, Long sectionId) {
        long presentCount = attendanceRepository.countBySessionDetailsAndStatus(date, period, yearId, deptId, sectionId, Attendance.AttendanceStatus.PRESENT);
        long absentCount = attendanceRepository.countBySessionDetailsAndStatus(date, period, yearId, deptId, sectionId, Attendance.AttendanceStatus.ABSENT);
        long totalStudents = presentCount + absentCount;
        
        double percentage = totalStudents == 0 ? 0 : ((double) presentCount / totalStudents) * 100.0;

        List<Attendance> presentRecords = attendanceRepository.findBySessionDetailsAndStatus(date, period, yearId, deptId, sectionId, Attendance.AttendanceStatus.PRESENT);
        List<Attendance> absentRecords = attendanceRepository.findBySessionDetailsAndStatus(date, period, yearId, deptId, sectionId, Attendance.AttendanceStatus.ABSENT);

        AdminAttendanceSummaryResponse response = new AdminAttendanceSummaryResponse();
        response.setTotalStudents(totalStudents);
        response.setTotalPresent(presentCount);
        response.setTotalAbsent(absentCount);
        response.setAttendancePercentage(Math.round(percentage * 100.0) / 100.0);

        response.setPresentStudents(presentRecords.stream().map(this::mapToItemResponse).collect(Collectors.toList()));
        response.setAbsentStudents(absentRecords.stream().map(this::mapToItemResponse).collect(Collectors.toList()));

        return response;
    }
    
    private StudentAttendanceListItemResponse mapToItemResponse(Attendance record) {
        StudentAttendanceListItemResponse res = new StudentAttendanceListItemResponse();
        res.setStudentId(record.getStudent().getId());
        res.setStudentName(record.getStudent().getUser().getFullName());
        res.setRegisterNumber(record.getStudent().getRegNo());
        res.setStatus(com.spdms.entity.AttendanceRecord.AttendanceStatus.valueOf(record.getStatus().name())); // Frontend expects AttendanceRecord status
        res.setRemarks(record.getRemarks());
        return res;
    }
}
