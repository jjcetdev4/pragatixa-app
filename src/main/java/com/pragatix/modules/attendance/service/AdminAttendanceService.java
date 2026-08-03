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
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import com.pragatix.modules.academiccalendar.service.AcademicCalendarResolver;
import com.pragatix.modules.attendance.dto.response.StudentAttendanceMatrixItemResponse;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.entity.Student;
import com.pragatix.modules.authentication.security.AuthUtils;
import com.pragatix.entity.User;
import com.pragatix.repository.YearRepository;
import org.springframework.security.access.AccessDeniedException;

@Service
public class AdminAttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AuthUtils authUtils;

    @Autowired
    private YearRepository yearRepository;

    @Autowired
    private AcademicCalendarResolver academicCalendarResolver;

    @Transactional(readOnly = true)
    public AdminAttendanceSummaryResponse getDashboardSummary(LocalDate date, Long yearId, Long deptId,
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

        com.pragatix.entity.Year yearEntity = yearRepository.findById(yearId).orElse(null);
        com.pragatix.enums.AcademicYear academicYear = null;
        if (yearEntity != null && yearEntity.getYearNo() != null) {
            int no = yearEntity.getYearNo();
            if (no == 1) academicYear = com.pragatix.enums.AcademicYear.FIRST_YEAR;
            else if (no == 2) academicYear = com.pragatix.enums.AcademicYear.SECOND_YEAR;
            else if (no == 3) academicYear = com.pragatix.enums.AcademicYear.THIRD_YEAR;
            else if (no == 4) academicYear = com.pragatix.enums.AcademicYear.FOURTH_YEAR;
        }

        if (academicCalendarResolver.isHoliday(date, academicYear)) {
            throw new IllegalArgumentException("Attendance cannot be marked. Today is configured as a Holiday.");
        }

        if (yearId == null) {
            throw new IllegalArgumentException("yearId is required");
        }

        List<Student> allStudents;
        if (deptId == null) {
            allStudents = studentRepository.findByYearRefId(yearId);
        } else if (sectionId != null) {
            allStudents = studentRepository.findByYearRefIdAndDepartmentIdAndSectionId(yearId, deptId, sectionId);
        } else {
            allStudents = studentRepository.findByYearRefIdAndDepartmentId(yearId, deptId);
        }

        List<Attendance> dayRecords = attendanceRepository.findBySessionDetails(date, yearId, deptId, sectionId);

        Map<Long, List<Attendance>> recordsByStudent = dayRecords.stream()
                .collect(Collectors.groupingBy(a -> a.getStudent().getId()));

        long totalStudents = allStudents.size();
        long presentCount = 0;
        long absentCount = 0;

        List<StudentAttendanceMatrixItemResponse> matrixItems = allStudents.stream().map(student -> {
            StudentAttendanceMatrixItemResponse item = new StudentAttendanceMatrixItemResponse();
            item.setStudentId(student.getId());
            item.setStudentName(student.getUser().getFullName());
            item.setRegisterNumber(student.getRegNo());

            Map<Integer, String> periodStatuses = new HashMap<>();
            for (int i = 1; i <= 8; i++) {
                periodStatuses.put(i, "—");
            }

            List<Attendance> studentRecords = recordsByStudent.getOrDefault(student.getId(), List.of());
            boolean hasPresent = false;
            boolean hasAbsent = false;

            for (Attendance record : studentRecords) {
                if (record.getPeriodNo() >= 1 && record.getPeriodNo() <= 8) {
                    String statusStr = "—";
                    if (record.getStatus() == Attendance.AttendanceStatus.PRESENT) {
                        statusStr = "P";
                        hasPresent = true;
                    } else if (record.getStatus() == Attendance.AttendanceStatus.ABSENT) {
                        statusStr = "A";
                        hasAbsent = true;
                    } else if (record.getStatus() == Attendance.AttendanceStatus.OD) {
                        statusStr = "OD";
                        hasPresent = true;
                    } else if (record.getStatus() == Attendance.AttendanceStatus.LEAVE) {
                        statusStr = "L";
                        hasAbsent = true;
                    }
                    periodStatuses.put(record.getPeriodNo(), statusStr);
                }
            }

            item.setPeriodStatuses(periodStatuses);
            return item;
        }).collect(Collectors.toList());

        for (StudentAttendanceMatrixItemResponse item : matrixItems) {
            boolean hasPresent = item.getPeriodStatuses().values().stream().anyMatch(s -> s.equals("P") || s.equals("OD"));
            boolean hasAbsent = item.getPeriodStatuses().values().stream().anyMatch(s -> s.equals("A") || s.equals("L"));
            if (hasPresent) {
                presentCount++;
            } else if (hasAbsent) {
                absentCount++;
            }
        }

        double percentage = totalStudents == 0 ? 0 : ((double) presentCount / totalStudents) * 100.0;

        AdminAttendanceSummaryResponse response = new AdminAttendanceSummaryResponse();
        response.setTotalStudents(totalStudents);
        response.setTotalPresent(presentCount);
        response.setTotalAbsent(absentCount);
        response.setAttendancePercentage(Math.round(percentage * 100.0) / 100.0);
        response.setStudents(matrixItems);

        return response;
    }
}
