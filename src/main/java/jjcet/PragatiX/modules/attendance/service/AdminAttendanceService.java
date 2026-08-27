package jjcet.PragatiX.modules.attendance.service;

import jjcet.PragatiX.entity.Attendance;
import jjcet.PragatiX.modules.attendance.dto.response.AdminAttendanceSummaryResponse;
import jjcet.PragatiX.modules.attendance.dto.response.StudentAttendanceListItemResponse;
import jjcet.PragatiX.modules.attendance.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import jjcet.PragatiX.modules.attendance.dto.response.StudentAttendanceMatrixItemResponse;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.repository.YearRepository;
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

    @Transactional(readOnly = true)
    public AdminAttendanceSummaryResponse getDashboardSummary(LocalDate date, Long yearId, Long deptId,
            Long sectionId) {
        User currentUser = authUtils.getCurrentUser();
        if (currentUser != null && authUtils.isAdmin(currentUser) && !authUtils.isSuperAdmin(currentUser)) {
            String adminYearStr = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
            if (adminYearStr != null) {
                Long adminYearId = yearRepository.findByYearNo(Byte.parseByte(adminYearStr))
                        .map(jjcet.PragatiX.entity.Year::getId)
                        .orElse(null);
                if (adminYearId != null) {
                    yearId = adminYearId;
                }
            }
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
            String name = student.getFullName();
            if (name == null || name.trim().isEmpty()) {
                name = student.getUser() != null ? student.getUser().getFullName() : null;
            }
            if (name == null || name.trim().isEmpty()) {
                name = student.getRegNo() != null ? student.getRegNo() : student.getSprNo();
            }
            item.setStudentName(name != null ? name.trim() : "Unknown");

            String reg = student.getRegNo();
            if (reg == null || reg.trim().isEmpty()) {
                reg = student.getSprNo();
            }
            item.setRegisterNumber(reg != null ? reg : "");

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
            boolean hasPresent = item.getPeriodStatuses().values().stream()
                    .anyMatch(s -> s.equals("P") || s.equals("OD"));
            boolean hasAbsent = item.getPeriodStatuses().values().stream()
                    .anyMatch(s -> s.equals("A") || s.equals("L"));
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
