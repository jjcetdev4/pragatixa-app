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
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import jjcet.PragatiX.repository.YearRepository;
import jjcet.PragatiX.modules.attendance.dto.response.AdminAttendanceHistoryItemResponse;
import jjcet.PragatiX.entity.AttendanceSession;
import jjcet.PragatiX.modules.attendance.repository.AttendanceSessionRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;

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
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private jjcet.PragatiX.modules.authentication.repository.UserRepository userRepository;

    @Transactional(readOnly = true)
    public AdminAttendanceSummaryResponse getDashboardSummary(LocalDate date, Long yearId, Long deptId,
            Long sectionId, Integer period) {
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
        if (currentUser != null && authUtils.isHOD(currentUser) && !authUtils.isAdmin(currentUser)
                && !authUtils.isSuperAdmin(currentUser)) {
            if (deptId == null && currentUser.getDepartment() != null) {
                deptId = currentUser.getDepartment().getId();
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
            if (period != null) {
                String st = item.getPeriodStatuses().get(period);
                if ("P".equals(st) || "OD".equals(st)) {
                    presentCount++;
                } else if ("A".equals(st) || "L".equals(st)) {
                    absentCount++;
                }
            } else {
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

    @Transactional(readOnly = true)
    public List<AdminAttendanceHistoryItemResponse> getAttendanceHistory(LocalDate date, Long yearId, Long deptId,
            Long sectionId, Integer period) {
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
        if (currentUser != null && authUtils.isHOD(currentUser) && !authUtils.isAdmin(currentUser)
                && !authUtils.isSuperAdmin(currentUser)) {
            if (deptId == null && currentUser.getDepartment() != null) {
                deptId = currentUser.getDepartment().getId();
            }
        }

        List<Attendance> records = attendanceRepository.findMarkedHistoryRecords(date, yearId, deptId, sectionId,
                period);

        // Group by session: date + "_" + period + "_" + yearId + "_" + deptId + "_" +
        // sectionId
        Map<String, List<Attendance>> grouped = new LinkedHashMap<>();
        for (Attendance a : records) {
            Long yId = (a.getStudent() != null && a.getStudent().getYearRef() != null)
                    ? a.getStudent().getYearRef().getId()
                    : 0L;
            Long dId = (a.getStudent() != null && a.getStudent().getDepartment() != null)
                    ? a.getStudent().getDepartment().getId()
                    : 0L;
            Long sId = (a.getStudent() != null && a.getStudent().getSection() != null)
                    ? a.getStudent().getSection().getId()
                    : 0L;
            String key = a.getAttendanceDate() + "_" + a.getPeriodNo() + "_" + yId + "_" + dId + "_" + sId;
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(a);
        }

        List<AdminAttendanceHistoryItemResponse> result = new ArrayList<>();
        for (List<Attendance> list : grouped.values()) {
            if (list.isEmpty())
                continue;
            Attendance first = list.get(0);
            AdminAttendanceHistoryItemResponse item = new AdminAttendanceHistoryItemResponse();
            item.setDate(first.getAttendanceDate());
            item.setPeriod(first.getPeriodNo());

            if (first.getStudent() != null) {
                if (first.getStudent().getYearRef() != null) {
                    item.setYearId(first.getStudent().getYearRef().getId());
                    item.setYearName(first.getStudent().getYearRef().getYearName() != null
                            ? first.getStudent().getYearRef().getYearName()
                            : "Year " + first.getStudent().getYearRef().getYearNo());
                }
                if (first.getStudent().getDepartment() != null) {
                    item.setDepartmentId(first.getStudent().getDepartment().getId());
                    item.setDepartmentName(first.getStudent().getDepartment().getName() != null
                            ? first.getStudent().getDepartment().getName()
                            : first.getStudent().getDepartment().getCode());
                }
                if (first.getStudent().getSection() != null) {
                    item.setSectionId(first.getStudent().getSection().getId());
                    item.setSectionName(first.getStudent().getSection().getSectionName());
                } else {
                    item.setSectionName("All Sections / None");
                }
            }

            // Faculty info
            jjcet.PragatiX.entity.Faculty faculty = list.stream()
                    .map(Attendance::getFaculty)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            Long yId = item.getYearId();
            Long dId = item.getDepartmentId();
            Long sId = item.getSectionId();

            if (faculty == null) {
                try {
                    Optional<AttendanceSession> sessionOpt = (sId != null && sId > 0)
                            ? attendanceSessionRepository
                                    .findByAttendanceDateAndPeriodNumberAndDepartmentIdAndSectionIdAndYearId(
                                            first.getAttendanceDate(), first.getPeriodNo(), dId, sId, yId)
                            : attendanceSessionRepository
                                    .findByAttendanceDateAndPeriodNumberAndDepartmentIdAndSectionIsNullAndYearId(
                                            first.getAttendanceDate(), first.getPeriodNo(), dId, yId);
                    if (sessionOpt.isPresent() && sessionOpt.get().getTeacher() != null) {
                        faculty = sessionOpt.get().getTeacher();
                    }
                } catch (Exception ignored) {
                }
            }

            if (faculty != null) {
                item.setFacultyId(faculty.getId());
                String facName = null;
                String facDept = null;
                if (faculty.getUser() != null) {
                    facName = faculty.getUser().getFullName();
                    item.setFacultyEmail(faculty.getUser().getEmail());
                    if (faculty.getUser().getDepartment() != null) {
                        facDept = faculty.getUser().getDepartment().getName();
                    }
                }
                if (facDept == null && faculty.getDepartment() != null) {
                    facDept = faculty.getDepartment().getName();
                }
                item.setFacultyName(
                        (facName != null && !facName.trim().isEmpty()) ? facName : "Staff #" + faculty.getId());
                item.setFacultyDepartmentName(facDept != null ? facDept : "");
                item.setFacultyDesignation(faculty.getDesignation() != null ? faculty.getDesignation() : "Faculty");
            } else {
                // Fallback for legacy attendance records without faculty_id: check CC or HOD
                String facName = null;
                String facDept = null;
                String facRole = null;
                String facEmail = null;

                if (sId != null && sId > 0 && dId != null && dId > 0) {
                    List<User> ccs = userRepository.findClassCoordinatorsByDepartmentAndSection(dId, sId);
                    if (ccs != null && !ccs.isEmpty()) {
                        User cc = ccs.get(0);
                        facName = cc.getFullName();
                        facEmail = cc.getEmail();
                        facDept = cc.getDepartment() != null ? cc.getDepartment().getName() : null;
                        facRole = "Class Coordinator";
                    }
                }
                if (facName == null && dId != null && dId > 0) {
                    List<User> hods = userRepository.findAll().stream()
                            .filter(u -> !u.isDeleted() && u.isActive() && u.getDepartment() != null
                                    && u.getDepartment().getId().equals(dId))
                            .filter(u -> u.getRoles().stream()
                                    .anyMatch(r -> "ROLE_HOD".equalsIgnoreCase(r.getName())
                                            || "HOD".equalsIgnoreCase(r.getName())))
                            .collect(Collectors.toList());
                    if (!hods.isEmpty()) {
                        User hod = hods.get(0);
                        facName = hod.getFullName();
                        facEmail = hod.getEmail();
                        facDept = hod.getDepartment() != null ? hod.getDepartment().getName() : null;
                        facRole = "HOD";
                    }
                }
                if (facName == null) {
                    // Fallback to any active HOD or Teacher in the system
                    List<User> allStaff = userRepository.findAll().stream()
                            .filter(u -> !u.isDeleted() && u.isActive())
                            .filter(u -> u.getRoles().stream()
                                    .anyMatch(r -> "ROLE_HOD".equalsIgnoreCase(r.getName())
                                            || "HOD".equalsIgnoreCase(r.getName())
                                            || "ROLE_TEACHER".equalsIgnoreCase(r.getName())
                                            || "TEACHER".equalsIgnoreCase(r.getName())))
                            .collect(Collectors.toList());
                    if (!allStaff.isEmpty()) {
                        User staffUser = allStaff.stream()
                                .filter(u -> u.getRoles().stream()
                                        .anyMatch(r -> "ROLE_HOD".equalsIgnoreCase(r.getName())
                                                || "HOD".equalsIgnoreCase(r.getName())))
                                .findFirst()
                                .orElse(allStaff.get(0));
                        facName = staffUser.getFullName();
                        facEmail = staffUser.getEmail();
                        facDept = staffUser.getDepartment() != null ? staffUser.getDepartment().getName() : "";
                        facRole = staffUser.getRoles().stream().anyMatch(
                                r -> "ROLE_HOD".equalsIgnoreCase(r.getName()) || "HOD".equalsIgnoreCase(r.getName()))
                                        ? "HOD"
                                        : "Faculty";
                    }
                }
                if (facName != null) {
                    item.setFacultyName(facName);
                    item.setFacultyEmail(facEmail);
                    item.setFacultyDepartmentName(facDept != null ? facDept : "");
                    item.setFacultyDesignation(facRole);
                } else {
                    item.setFacultyName("Marking Faculty");
                    item.setFacultyDepartmentName("");
                    item.setFacultyDesignation("Faculty");
                }
            }

            long total = list.size();
            long present = list.stream().filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT
                    || a.getStatus() == Attendance.AttendanceStatus.OD).count();
            long absent = list.stream().filter(a -> a.getStatus() == Attendance.AttendanceStatus.ABSENT
                    || a.getStatus() == Attendance.AttendanceStatus.LEAVE).count();

            item.setTotalStudents(total);
            item.setPresentCount(present);
            item.setAbsentCount(absent);

            java.time.LocalDateTime markedAt = list.stream()
                    .map(Attendance::getCreatedAt)
                    .filter(Objects::nonNull)
                    .max(java.time.LocalDateTime::compareTo)
                    .orElse(null);
            if (markedAt == null) {
                markedAt = list.stream()
                        .map(Attendance::getUpdatedAt)
                        .filter(Objects::nonNull)
                        .max(java.time.LocalDateTime::compareTo)
                        .orElse(null);
            }
            item.setMarkedAt(markedAt);

            result.add(item);
        }

        return result;
    }
}
