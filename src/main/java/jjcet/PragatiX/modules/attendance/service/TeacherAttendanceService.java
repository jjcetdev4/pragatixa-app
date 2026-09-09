package jjcet.PragatiX.modules.attendance.service;

import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.modules.attendance.dto.request.SaveAttendanceRequest;
import jjcet.PragatiX.modules.attendance.dto.response.StudentAttendanceListItemResponse;
import jjcet.PragatiX.modules.attendance.dto.response.MarkedPeriodInfoResponse;
import jjcet.PragatiX.modules.analytics.util.AnalyticsRoleUtils;
import jjcet.PragatiX.modules.attendance.repository.AttendanceRepository;
import jjcet.PragatiX.modules.attendance.repository.AttendanceRepository;
import jjcet.PragatiX.repository.*;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.modules.faculty.repository.FacultyRepository;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private jjcet.PragatiX.modules.attendance.repository.AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceStreakService streakService;

    @Autowired
    private jjcet.PragatiX.modules.notification.service.NotificationService notificationService;

    @Autowired
    private AuthUtils authUtils;

    @Autowired
    private jjcet.PragatiX.modules.academiccalendar.service.AcademicCalendarResolver academicCalendarResolver;

    private Long resolveYearIdIfAdmin(Long yearId) {
        User currentUser = authUtils.getCurrentUser();
        if (currentUser != null && authUtils.isAdmin(currentUser) && !authUtils.isSuperAdmin(currentUser)) {
            String adminYearStr = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
            if (adminYearStr != null) {
                Long adminYearId = yearRepository.findByYearNo(Byte.parseByte(adminYearStr))
                        .map(jjcet.PragatiX.entity.Year::getId)
                        .orElse(null);
                if (adminYearId != null) {
                    return adminYearId;
                }
            }
        }
        return yearId;
    }

    @Transactional(readOnly = true)
    public Integer getNextPeriod(LocalDate date, Long yearId, Long departmentId, Long sectionId) {
        yearId = resolveYearIdIfAdmin(yearId);
        Integer maxPeriod = attendanceRepository.findMaxPeriodForSession(date, yearId, departmentId, sectionId);
        if (maxPeriod == null) {
            return 1;
        }
        return maxPeriod < 8 ? maxPeriod + 1 : 8;
    }

    private boolean canUserViewAttendanceHistory(User user, LocalDate date, Integer period, Long yearId, Long deptId, Long sectionId) {
        if (user == null) return false;
        if (authUtils.isSuperAdmin(user) || authUtils.isAdmin(user)) {
            return true;
        }

        // Check if user is HOD of this department
        boolean isHod = AnalyticsRoleUtils.isHod(user);
        if (isHod && user.getDepartment() != null && deptId != null && user.getDepartment().getId().equals(deptId)) {
            return true;
        }

        // Check if user is CC of this section/department
        boolean isCc = (user.getSubRoles() != null && user.getSubRoles().stream().anyMatch(sr -> sr != null && ("CC".equalsIgnoreCase(sr.getName()) || "CLASS_COORDINATOR".equalsIgnoreCase(sr.getName()))))
                || (user.getRoles() != null && user.getRoles().stream().anyMatch(r -> r != null && "ROLE_CLASS_COORDINATOR".equalsIgnoreCase(r.getName())));
        if (isCc) {
            if (sectionId != null && user.getSection() != null && user.getSection().getId().equals(sectionId)) {
                return true;
            }
            if (sectionId == null && user.getDepartment() != null && deptId != null && user.getDepartment().getId().equals(deptId)) {
                return true;
            }
        }

        // Check if user is the faculty who marked this attendance
        List<Faculty> markingFaculties = attendanceRepository.findMarkingFacultyForSession(date, period, yearId, deptId, sectionId);
        markingFaculties = markingFaculties.stream().filter(java.util.Objects::nonNull).collect(Collectors.toList());
        for (Faculty f : markingFaculties) {
            if (f.getUser() != null && f.getUser().getId().equals(user.getId())) {
                return true;
            }
        }

        // Fallback: If no faculty was recorded on legacy attendance rows, allow HOD to view
        if (markingFaculties.isEmpty() && isHod) {
            return true;
        }

        return false;
    }

    public static class SessionFacultyInfo {
        private final Long userId;
        private final String name;
        private final String department;

        public SessionFacultyInfo(Long userId, String name, String department) {
            this.userId = userId;
            this.name = name;
            this.department = department;
        }

        public Long getUserId() { return userId; }
        public String getName() { return name; }
        public String getDepartment() { return department; }
    }

    public SessionFacultyInfo resolveFacultyForSession(LocalDate date, Integer period, Long yearId, Long deptId, Long sectionId) {
        // 1. Try finding from Attendance table
        List<Faculty> markingFaculties = attendanceRepository.findMarkingFacultyForSession(date, period, yearId, deptId, sectionId);
        if (markingFaculties != null) {
            markingFaculties = markingFaculties.stream().filter(Objects::nonNull).collect(Collectors.toList());
            if (!markingFaculties.isEmpty()) {
                Faculty f = markingFaculties.get(0);
                if (f != null) {
                    String name = f.getUser() != null ? f.getUser().getFullName() : null;
                    String dept = null;
                    if (f.getUser() != null && f.getUser().getDepartment() != null) {
                        dept = f.getUser().getDepartment().getDeptCode() != null ? f.getUser().getDepartment().getDeptCode() : f.getUser().getDepartment().getName();
                    }
                    if (dept == null && f.getDepartment() != null) {
                        dept = f.getDepartment().getDeptCode() != null ? f.getDepartment().getDeptCode() : f.getDepartment().getName();
                    }
                    if (name != null) {
                        return new SessionFacultyInfo(f.getUser() != null ? f.getUser().getId() : null, name, dept != null ? dept : "");
                    }
                }
            }
        }

        // 2. Try finding from AttendanceSession table
        try {
            Optional<AttendanceSession> sessionOpt = (sectionId != null && sectionId > 0)
                    ? attendanceSessionRepository.findByAttendanceDateAndPeriodNumberAndDepartmentIdAndSectionIdAndYearId(date, period, deptId, sectionId, yearId)
                    : attendanceSessionRepository.findByAttendanceDateAndPeriodNumberAndDepartmentIdAndSectionIsNullAndYearId(date, period, deptId, yearId);
            if (sessionOpt.isPresent() && sessionOpt.get().getTeacher() != null) {
                Faculty f = sessionOpt.get().getTeacher();
                String name = f.getUser() != null ? f.getUser().getFullName() : null;
                String dept = null;
                if (f.getUser() != null && f.getUser().getDepartment() != null) {
                    dept = f.getUser().getDepartment().getDeptCode() != null ? f.getUser().getDepartment().getDeptCode() : f.getUser().getDepartment().getName();
                }
                if (dept == null && f.getDepartment() != null) {
                    dept = f.getDepartment().getDeptCode() != null ? f.getDepartment().getDeptCode() : f.getDepartment().getName();
                }
                if (name != null) {
                    return new SessionFacultyInfo(f.getUser() != null ? f.getUser().getId() : null, name, dept != null ? dept : "");
                }
            }
        } catch (Exception ignored) {}

        // 3. Fallback to Class Coordinator if assigned
        if (sectionId != null && sectionId > 0 && deptId != null && deptId > 0) {
            List<User> ccs = userRepository.findClassCoordinatorsByDepartmentAndSection(deptId, sectionId);
            if (ccs != null && !ccs.isEmpty()) {
                User cc = ccs.get(0);
                String dept = cc.getDepartment() != null ? (cc.getDepartment().getDeptCode() != null ? cc.getDepartment().getDeptCode() : cc.getDepartment().getName()) : "";
                return new SessionFacultyInfo(cc.getId(), cc.getFullName(), dept);
            }
        }

        // 4. Fallback to Department HOD
        if (deptId != null && deptId > 0) {
            List<User> hods = userRepository.findHODByDepartment(deptId);
            if (hods != null && !hods.isEmpty()) {
                User hod = hods.get(0);
                String dept = hod.getDepartment() != null ? (hod.getDepartment().getDeptCode() != null ? hod.getDepartment().getDeptCode() : hod.getDepartment().getName()) : "";
                return new SessionFacultyInfo(hod.getId(), hod.getFullName(), dept);
            }
        }

        return new SessionFacultyInfo(null, null, null);
    }

    @Transactional(readOnly = true)
    public List<MarkedPeriodInfoResponse> getMarkedPeriodsInfo(LocalDate date, Long yearId, Long departmentId, Long sectionId) {
        yearId = resolveYearIdIfAdmin(yearId);
        List<Integer> periods = attendanceRepository.findMarkedPeriodsForSession(date, yearId, departmentId, sectionId);
        User currentUser = authUtils.getCurrentUser();

        List<MarkedPeriodInfoResponse> result = new ArrayList<>();
        for (Integer p : periods) {
            boolean canView = canUserViewAttendanceHistory(currentUser, date, p, yearId, departmentId, sectionId);
            SessionFacultyInfo facultyInfo = resolveFacultyForSession(date, p, yearId, departmentId, sectionId);
            
            boolean isMarkedByMe = false;
            String facultyName = facultyInfo.getName();
            String facultyDept = facultyInfo.getDepartment();

            if (currentUser != null && facultyInfo.getUserId() != null && facultyInfo.getUserId().equals(currentUser.getId())) {
                isMarkedByMe = true;
            } else if (canView && AnalyticsRoleUtils.isHod(currentUser)) {
                isMarkedByMe = true;
            }

            result.add(new MarkedPeriodInfoResponse(p, canView, isMarkedByMe, facultyName, facultyDept, null));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Integer> getMarkedPeriods(LocalDate date, Long yearId, Long departmentId, Long sectionId) {
        yearId = resolveYearIdIfAdmin(yearId);
        return attendanceRepository.findMarkedPeriodsForSession(date, yearId, departmentId, sectionId);
    }

    @Transactional(readOnly = true)
    public List<StudentAttendanceListItemResponse> getStudentListWithAttendance(LocalDate date, Integer period,
            Long yearId, Long deptId, Long sectionId) {

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

        // Check if this period is already marked; if so, verify view authorization
        List<Integer> markedPeriods = attendanceRepository.findMarkedPeriodsForSession(date, yearId, deptId, sectionId);
        boolean isPeriodMarked = markedPeriods != null && markedPeriods.contains(period);
        if (isPeriodMarked) {
            boolean authorized = canUserViewAttendanceHistory(currentUser, date, period, yearId, deptId, sectionId);
            if (!authorized) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You are not authorized to view the attendance history for this period. Attendance can only be viewed by the faculty who marked it, the Class Coordinator, or the Department HOD.");
            }
        }
        
        SessionFacultyInfo facultyInfo = resolveFacultyForSession(date, period, yearId, deptId, sectionId);
        String sessionFacultyName = facultyInfo.getName();
        String sessionFacultyDept = facultyInfo.getDepartment();

        jjcet.PragatiX.entity.Year yearEntity = yearRepository.findById(yearId).orElse(null);
        jjcet.PragatiX.enums.AcademicYear academicYear = null;
        if (yearEntity != null && yearEntity.getYearNo() != null) {
            int no = yearEntity.getYearNo();
            if (no == 1)
                academicYear = jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR;
            else if (no == 2)
                academicYear = jjcet.PragatiX.enums.AcademicYear.SECOND_YEAR;
            else if (no == 3)
                academicYear = jjcet.PragatiX.enums.AcademicYear.THIRD_YEAR;
            else if (no == 4)
                academicYear = jjcet.PragatiX.enums.AcademicYear.FOURTH_YEAR;
        }

        academicCalendarResolver.validateDateForAttendance(date, academicYear);

        List<Student> students = (sectionId != null)
                ? studentRepository.findByYearRefIdAndDepartmentIdAndSectionId(yearId, deptId, sectionId)
                : studentRepository.findByYearRefIdAndDepartmentId(yearId, deptId);

        List<StudentAttendanceListItemResponse> responseList = new ArrayList<>();

        for (Student s : students) {
            StudentAttendanceListItemResponse res = new StudentAttendanceListItemResponse();
            res.setStudentId(s.getId());
            String name = s.getFullName();
            if (name == null || name.trim().isEmpty()) {
                name = s.getUser() != null ? s.getUser().getFullName() : null;
            }
            if (name == null || name.trim().isEmpty()) {
                name = s.getRegNo() != null ? s.getRegNo() : s.getSprNo();
            }
            res.setStudentName(name != null ? name.trim() : "Unknown");

            String reg = s.getRegNo();
            if (reg == null || reg.trim().isEmpty()) {
                reg = s.getSprNo();
            }
            res.setRegisterNumber(reg != null ? reg : "");

            Optional<Attendance> recordOpt = attendanceRepository.findByStudentIdAndAttendanceDateAndPeriodNo(s.getId(),
                    date, period);
            if (recordOpt.isPresent()) {
                res.setStatus(jjcet.PragatiX.entity.AttendanceRecord.AttendanceStatus
                        .valueOf(recordOpt.get().getStatus().name()));
                res.setRemarks(recordOpt.get().getRemarks());
                if (recordOpt.get().getFaculty() != null) {
                    Faculty rf = recordOpt.get().getFaculty();
                    if (rf.getUser() != null) {
                        res.setMarkedByFacultyName(rf.getUser().getFullName());
                    }
                    if (rf.getDepartment() != null) {
                        res.setMarkedByFacultyDepartment(rf.getDepartment().getDeptCode() != null
                                ? rf.getDepartment().getDeptCode()
                                : rf.getDepartment().getName());
                    }
                } else if (sessionFacultyName != null) {
                    res.setMarkedByFacultyName(sessionFacultyName);
                    res.setMarkedByFacultyDepartment(sessionFacultyDept);
                }
                if (recordOpt.get().getCreatedAt() != null) {
                    res.setMarkedAt(recordOpt.get().getCreatedAt().toString());
                }
            } else {
                res.setStatus(jjcet.PragatiX.entity.AttendanceRecord.AttendanceStatus.PRESENT); // Default if not marked
                res.setMarkedByFacultyName(sessionFacultyName);
                res.setMarkedByFacultyDepartment(sessionFacultyDept);
            }
            responseList.add(res);
        }

        return responseList;
    }

    @Transactional
    public void saveAttendance(String username, SaveAttendanceRequest request) {
        log.info("Starting saveAttendance for user: {}, records count: {}", username,
                request.getRecords() != null ? request.getRecords().size() : 0);

        jjcet.PragatiX.entity.Year reqYear = yearRepository.findById(request.getYearId()).orElse(null);
        jjcet.PragatiX.enums.AcademicYear reqAcademicYear = null;
        if (reqYear != null && reqYear.getYearNo() != null) {
            int no = reqYear.getYearNo();
            if (no == 1)
                reqAcademicYear = jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR;
            else if (no == 2)
                reqAcademicYear = jjcet.PragatiX.enums.AcademicYear.SECOND_YEAR;
            else if (no == 3)
                reqAcademicYear = jjcet.PragatiX.enums.AcademicYear.THIRD_YEAR;
            else if (no == 4)
                reqAcademicYear = jjcet.PragatiX.enums.AcademicYear.FOURTH_YEAR;
        }

        academicCalendarResolver.validateDateForAttendance(request.getDate(), reqAcademicYear);

        if (request.getDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot mark attendance for future dates.");
        }

        String adminYearStr = null;
        User currentUser = authUtils.getCurrentUser();
        if (currentUser != null && authUtils.isAdmin(currentUser) && !authUtils.isSuperAdmin(currentUser)) {
            adminYearStr = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
        }

        Faculty teacher = null;
        User userObj = authUtils.getCurrentUser();
        if (userObj == null && username != null) {
            userObj = userRepository.findByUsername(username).orElse(null);
        }

        if (userObj != null) {
            final User resolvedUser = userObj;
            teacher = facultyRepository.findByUserId(resolvedUser.getId())
                    .or(() -> facultyRepository.findByUserUsername(resolvedUser.getUsername()))
                    .orElseGet(() -> {
                        Faculty newFaculty = new Faculty();
                        newFaculty.setUser(resolvedUser);
                        newFaculty.setDepartment(resolvedUser.getDepartment() != null ? resolvedUser.getDepartment() : departmentRepository.findAll().stream().findFirst().orElse(null));
                        newFaculty.setSection(resolvedUser.getSection());
                        newFaculty.setDesignation(resolvedUser.getFullName() != null ? resolvedUser.getFullName() : resolvedUser.getUsername());
                        newFaculty.setPhoneNo(resolvedUser.getPhone() != null ? resolvedUser.getPhone() : "0000000000");
                        return facultyRepository.save(newFaculty);
                    });
        }

        List<Long> studentIds = request.getRecords().stream()
                .map(SaveAttendanceRequest.StudentAttendanceRequest::getStudentId)
                .collect(Collectors.toList());

        if (!studentIds.isEmpty()) {
            boolean alreadyMarked = attendanceRepository.existsByStudentIdInAndAttendanceDateAndPeriodNo(
                    studentIds, request.getDate(), request.getPeriod());
            if (alreadyMarked) {
                throw new IllegalArgumentException(
                        "Attendance for this class in period " + request.getPeriod() + " has already been marked.");
            }
        }

        int count = 0;
        final Faculty finalTeacher = teacher;
        for (SaveAttendanceRequest.StudentAttendanceRequest recordReq : request.getRecords()) {
            Student student = studentRepository.findById(recordReq.getStudentId()).orElseThrow();

            if (adminYearStr != null && !adminYearStr.equals(student.getYear())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You are not authorized to mark attendance for students outside your academic year.");
            }

            Optional<Attendance> existingOpt = attendanceRepository.findByStudentIdAndAttendanceDateAndPeriodNo(
                    student.getId(), request.getDate(), request.getPeriod());

            Attendance.AttendanceStatus oldStatus = existingOpt.map(Attendance::getStatus).orElse(null);
            Attendance.AttendanceStatus newStatus = Attendance.AttendanceStatus.valueOf(recordReq.getStatus().name());

            Attendance attendance = existingOpt.orElseGet(() -> Attendance.builder()
                    .student(student)
                    .faculty(finalTeacher)
                    .regNo(student.getRegNo())
                    .attendanceDate(request.getDate())
                    .periodNo(request.getPeriod())
                    .build());

            attendance.setFaculty(finalTeacher);
            attendance.setStatus(newStatus);
            attendance.setRemarks(recordReq.getRemarks());

            attendanceRepository.save(attendance);
            count++;

            // Trigger absence notification ONLY when student is marked ABSENT (e.g. PRESENT -> ABSENT, or initial ABSENT)
            // If already ABSENT (ABSENT -> ABSENT), or PRESENT, no SMS is triggered.
            if (newStatus == Attendance.AttendanceStatus.ABSENT && oldStatus != Attendance.AttendanceStatus.ABSENT) {
                try {
                    notificationService.sendAbsenceNotification(student.getId(), request.getDate(),
                            request.getPeriod());
                } catch (Exception e) {
                    log.error("Failed to trigger SMS notification for student {}", student.getRegNo(), e);
                }
            }
        }

        // Also save AttendanceSession for session-level auditing
        try {
            Long reqYearId = request.getYearId();
            Long reqDeptId = request.getDepartmentId();
            Long reqSectionId = request.getSectionId();
            LocalDate reqDate = request.getDate();
            Integer reqPeriod = request.getPeriod();

            Optional<AttendanceSession> sessionOpt = (reqSectionId != null && reqSectionId > 0)
                    ? attendanceSessionRepository.findByAttendanceDateAndPeriodNumberAndDepartmentIdAndSectionIdAndYearId(reqDate, reqPeriod, reqDeptId, reqSectionId, reqYearId)
                    : attendanceSessionRepository.findByAttendanceDateAndPeriodNumberAndDepartmentIdAndSectionIsNullAndYearId(reqDate, reqPeriod, reqDeptId, reqYearId);

            AttendanceSession session = sessionOpt.orElseGet(AttendanceSession::new);
            session.setAttendanceDate(reqDate);
            session.setPeriodNumber(reqPeriod);
            if (reqYear != null) session.setYear(reqYear);
            try {
                AcademicYear ay = academicYearRepository.findAll().stream().findFirst().orElse(null);
                if (ay != null) session.setAcademicYear(ay);
            } catch (Exception ignored) {}
            session.setDepartment(departmentRepository.findById(reqDeptId).orElse(null));
            if (reqSectionId != null && reqSectionId > 0) {
                session.setSection(sectionRepository.findById(reqSectionId).orElse(null));
            }
            if (teacher != null) {
                session.setTeacher(teacher);
            }
            attendanceSessionRepository.save(session);
        } catch (Exception e) {
            log.warn("Could not save AttendanceSession: {}", e.getMessage());
        }

        log.info("AttendanceRecord Count = {}", count);
        log.info("Attendance committed successfully.");
    }
}
