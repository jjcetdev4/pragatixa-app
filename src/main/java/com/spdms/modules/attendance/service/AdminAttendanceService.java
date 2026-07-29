package com.pragatix.modules.attendance.service;

import com.pragatix.entity.Attendance;
import com.pragatix.modules.attendance.dto.response.AdminAttendanceSummaryResponse;
import com.pragatix.modules.attendance.dto.response.StudentAttendanceListItemResponse;
import com.pragatix.modules.attendance.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import com.pragatix.modules.authentication.security.AuthUtils;
import com.pragatix.entity.User;
import com.pragatix.repository.YearRepository;
import org.springframework.security.access.AccessDeniedException;

@Service
public class AdminAttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private AuthUtils authUtils;

    @Autowired
    private YearRepository yearRepository;

    @Transactional(readOnly = true)
    public AdminAttendanceSummaryResponse getDashboardSummary(LocalDate date, Integer period, Long yearId, Long deptId,
            Long sectionId) {
        User currentUser = authUtils.getCurrentUser();
        if (currentUser != null && authUtils.isAdmin(currentUser) && !authUtils.isSuperAdmin(currentUser)) {
            String adminYearStr = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
            if (adminYearStr != null) {
                Long adminYearId = yearRepository.findByYearNo(Byte.parseByte(adminYearStr))
                        .map(com.pragatix.entity.Year::getId)
                        .orElse(null);
                if (adminYearId != null) {
                    yearId = adminYearId;
                }
            }
        }

        if (yearId == null) {
            throw new IllegalArgumentException("yearId is required");
        }

        long presentCount = attendanceRepository.countBySessionDetailsAndStatus(date, period, yearId, deptId, sectionId,
                Attendance.AttendanceStatus.PRESENT);
        long absentCount = attendanceRepository.countBySessionDetailsAndStatus(date, period, yearId, deptId, sectionId,
                Attendance.AttendanceStatus.ABSENT);
        long totalStudents = presentCount + absentCount;

        double percentage = totalStudents == 0 ? 0 : ((double) presentCount / totalStudents) * 100.0;

        List<Attendance> presentRecords = attendanceRepository.findBySessionDetailsAndStatus(date, period, yearId,
                deptId, sectionId, Attendance.AttendanceStatus.PRESENT);
        List<Attendance> absentRecords = attendanceRepository.findBySessionDetailsAndStatus(date, period, yearId,
                deptId, sectionId, Attendance.AttendanceStatus.ABSENT);

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
        res.setStatus(com.pragatix.entity.AttendanceRecord.AttendanceStatus.valueOf(record.getStatus().name())); // Frontend
                                                                                                                 // expects
                                                                                                                 // AttendanceRecord
                                                                                                                 // status
        res.setRemarks(record.getRemarks());
        return res;
    }
}
