package com.pragatix.modules.attendance.service;

import com.pragatix.entity.*;
import com.pragatix.modules.attendance.dto.request.SaveAttendanceRequest;
import com.pragatix.modules.attendance.dto.response.StudentAttendanceListItemResponse;
import com.pragatix.modules.attendance.repository.AttendanceRepository;
import com.pragatix.modules.attendance.repository.AttendanceRepository;
import com.pragatix.repository.*;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.modules.faculty.repository.FacultyRepository;
import com.pragatix.modules.authentication.security.AuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TeacherAttendanceService {

    private static final Logger log = LoggerFactory.getLogger(TeacherAttendanceService.class);

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private YearRepository yearRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private AttendanceStreakService streakService;

    @Autowired
    private com.pragatix.modules.notification.service.NotificationService notificationService;

    @Autowired
    private AuthUtils authUtils;

    @Autowired
    private com.pragatix.modules.academiccalendar.service.AcademicCalendarResolver academicCalendarResolver;

    @Transactional(readOnly = true)
    public List<StudentAttendanceListItemResponse> getStudentListWithAttendance(LocalDate date, Integer period,
            Long yearId, Long deptId, Long sectionId) {
        
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

        List<Student> students = (sectionId != null)
                ? studentRepository.findByYearRefIdAndDepartmentIdAndSectionId(yearId, deptId, sectionId)
                : studentRepository.findByYearRefIdAndDepartmentId(yearId, deptId);

        List<StudentAttendanceListItemResponse> responseList = new ArrayList<>();

        for (Student s : students) {
            StudentAttendanceListItemResponse res = new StudentAttendanceListItemResponse();
            res.setStudentId(s.getId());
            res.setStudentName(s.getFullName());
            res.setRegisterNumber(s.getRegNo());

            Optional<Attendance> recordOpt = attendanceRepository.findByStudentIdAndAttendanceDateAndPeriodNo(s.getId(),
                    date, period);
            if (recordOpt.isPresent()) {
                res.setStatus(com.pragatix.entity.AttendanceRecord.AttendanceStatus
                        .valueOf(recordOpt.get().getStatus().name()));
                res.setRemarks(recordOpt.get().getRemarks());
            } else {
                res.setStatus(com.pragatix.entity.AttendanceRecord.AttendanceStatus.PRESENT); // Default if not marked
            }
            responseList.add(res);
        }

        return responseList;
    }

    @Transactional
    public void saveAttendance(String username, SaveAttendanceRequest request) {
        log.info("Starting saveAttendance for user: {}, records count: {}", username,
                request.getRecords() != null ? request.getRecords().size() : 0);

        com.pragatix.entity.Year reqYear = yearRepository.findById(request.getYearId()).orElse(null);
        com.pragatix.enums.AcademicYear reqAcademicYear = null;
        if (reqYear != null && reqYear.getYearNo() != null) {
            int no = reqYear.getYearNo();
            if (no == 1) reqAcademicYear = com.pragatix.enums.AcademicYear.FIRST_YEAR;
            else if (no == 2) reqAcademicYear = com.pragatix.enums.AcademicYear.SECOND_YEAR;
            else if (no == 3) reqAcademicYear = com.pragatix.enums.AcademicYear.THIRD_YEAR;
            else if (no == 4) reqAcademicYear = com.pragatix.enums.AcademicYear.FOURTH_YEAR;
        }

        if (academicCalendarResolver.isHoliday(request.getDate(), reqAcademicYear)) {
            throw new IllegalArgumentException("Attendance cannot be marked. Today is configured as a Holiday.");
        }

        if (request.getDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot mark attendance for future dates.");
        }

        String adminYearStr = null;
        User currentUser = authUtils.getCurrentUser();
        if (currentUser != null && authUtils.isAdmin(currentUser) && !authUtils.isSuperAdmin(currentUser)) {
            adminYearStr = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
        }

        Faculty teacher = facultyRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Faculty not found for username: " + username));

        List<Long> studentIds = request.getRecords().stream()
                .map(SaveAttendanceRequest.StudentAttendanceRequest::getStudentId)
                .collect(Collectors.toList());

        if (!studentIds.isEmpty()) {
            boolean alreadyMarked = attendanceRepository.existsByStudentIdInAndAttendanceDateAndPeriodNo(
                    studentIds, request.getDate(), request.getPeriod());
            if (alreadyMarked) {
                throw new IllegalArgumentException("Attendance for this class in period " + request.getPeriod() + " has already been marked.");
            }
        }

        int count = 0;
        for (SaveAttendanceRequest.StudentAttendanceRequest recordReq : request.getRecords()) {
            Student student = studentRepository.findById(recordReq.getStudentId()).orElseThrow();

            if (adminYearStr != null && !adminYearStr.equals(student.getYear())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You are not authorized to mark attendance for students outside your academic year.");
            }

            Attendance attendance = attendanceRepository.findByStudentIdAndAttendanceDateAndPeriodNo(
                    student.getId(), request.getDate(), request.getPeriod())
                    .orElseGet(() -> Attendance.builder()
                            .student(student)
                            .faculty(teacher)
                            .regNo(student.getRegNo())
                            .attendanceDate(request.getDate())
                            .periodNo(request.getPeriod())
                            .build());

            attendance.setStatus(Attendance.AttendanceStatus.valueOf(recordReq.getStatus().name()));
            attendance.setRemarks(recordReq.getRemarks());

            attendanceRepository.save(attendance);
            count++;

            try {
                streakService.updateAttendanceStreak(student, request.getDate());
            } catch (Exception e) {
                log.error("Failed to update streak", e);
                throw new RuntimeException(
                        "Failed to update streak for student " + student.getRegNo() + ": " + e.getMessage(), e);
            }

            if (attendance.getStatus() == Attendance.AttendanceStatus.ABSENT) {
                try {
                    notificationService.sendAbsenceNotification(student.getId(), request.getDate());
                } catch (Exception e) {
                    log.error("Failed to queue SMS notification for student {}", student.getRegNo(), e);
                }
            }
        }

        log.info("AttendanceRecord Count = {}", count);
        log.info("Attendance committed successfully.");
    }
}
