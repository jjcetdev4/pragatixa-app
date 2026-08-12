package com.pragatix.modules.profile.service;

import com.pragatix.entity.*;
import com.pragatix.modules.profile.dto.ProfileResponse;
import com.pragatix.modules.authentication.security.AuthUtils;
import com.pragatix.repository.*;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.modules.faculty.repository.FacultyRepository;
import com.pragatix.modules.authentication.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProfileService {

    private final AuthUtils authUtils;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final StageTeamRepository stageTeamRepository;

    public ProfileService(AuthUtils authUtils,
            StudentRepository studentRepository,
            FacultyRepository facultyRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            TeamRepository teamRepository,
            StageTeamRepository stageTeamRepository) {
        this.authUtils = authUtils;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.stageTeamRepository = stageTeamRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile() {
        String authName = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = authUtils.getCurrentUser();

        Student student = studentRepository.findByRegNo(authName).orElse(null);
        if (student == null) {
            student = studentRepository.findByEmail(authName).orElse(null);
        }
        if (student == null && user != null) {
            student = studentRepository.findByUserId(user.getId()).orElse(null);
            if (student == null && user.getEmail() != null) {
                student = studentRepository.findByEmail(user.getEmail()).orElse(null);
            }
        }

        if (student != null) {
            boolean isCap = student.getTeam() != null && student.getTeam().getCaptain() != null
                    && student.getTeam().getCaptain().getId().equals(student.getId());
            boolean isViceCap = false;
            if (student.getTeam() != null) {
                if (student.getTeam().getViceCaptain() != null && student.getTeam().getViceCaptain().getId().equals(student.getId())) {
                    isViceCap = true;
                } else {
                    List<com.pragatix.entity.StageTeam> stageTeams = stageTeamRepository.findByTeamId(student.getTeam().getId());
                    for (com.pragatix.entity.StageTeam st : stageTeams) {
                        if (st.getViceCaptain() != null && st.getViceCaptain().getId().equals(student.getId())) {
                            isViceCap = true;
                            break;
                        }
                    }
                }
            }

            String primaryRole = isCap ? "CAPTAIN" : (isViceCap ? "VICE_CAPTAIN" : "STUDENT");

            ProfileResponse response = new ProfileResponse();
            response.setId(user != null ? user.getId() : student.getId());
            response.setFullName(student.getFullName());
            response.setUsername(student.getRegNo());
            response.setEmail(student.getEmail());
            response.setPhone(student.getPhoneNo() != null ? student.getPhoneNo() : (user != null ? user.getPhone() : ""));
            response.setDepartment(student.getDepartment() != null
                    ? (student.getDepartment().getName() != null ? student.getDepartment().getName() : student.getDepartment().getDeptName())
                    : (user != null && user.getDepartment() != null ? user.getDepartment().getName() : "N/A"));
            response.setAccountStatus("Active");
            response.setCreatedDate(student.getCreatedAt());
            response.setLastUpdated(student.getUpdatedAt());
            response.setRole(primaryRole);
            response.setStudentDetails(buildStudentDetails(student));
            return response;
        }

        if (user == null) {
            throw new RuntimeException("User not authenticated");
        }

        ProfileResponse response = new ProfileResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setDepartment(user.getDepartment() != null ? user.getDepartment().getName() : "N/A");
        response.setAccountStatus(user.isActive() ? "Active" : "Inactive");
        response.setCreatedDate(user.getCreatedAt());
        response.setLastUpdated(user.getUpdatedAt());

        // Determine highest precedence role
        String primaryRole = determinePrimaryRole(user);
        response.setRole(primaryRole);

        // Populate Role-Specific Details
        if (authUtils.isSuperAdmin(user)) {
            response.setSuperAdminDetails(buildSuperAdminDetails(user));
        } else if (authUtils.isAdmin(user)) {
            response.setAdminDetails(buildAdminDetails(user));
        }

        // For teachers, populate all applicable roles (Teacher, CC, HOD)
        boolean isTeacher = user.getRoles().stream().anyMatch(r -> "ROLE_TEACHER".equals(r.getName()));
        if (isTeacher) {
            response.setTeacherDetails(buildTeacherDetails(user));
        }

        for (com.pragatix.entity.SubRole sr : user.getSubRoles()) {
            if ("HOD".equalsIgnoreCase(sr.getName())) {
                response.setHodDetails(buildHodDetails(user));
            }
            if ("CC".equalsIgnoreCase(sr.getName()) || "CLASS_COORDINATOR".equalsIgnoreCase(sr.getName())) {
                response.setCcDetails(buildCcDetails(user));
            }
        }

        return response;
    }

    private String determinePrimaryRole(User user) {
        if (authUtils.isSuperAdmin(user))
            return "SUPER_ADMIN";
        if (authUtils.isAdmin(user))
            return "ADMIN";
        boolean isTeacher = false;
        boolean isStudent = false;
        for (Role r : user.getRoles()) {
            if ("ROLE_TEACHER".equals(r.getName()))
                isTeacher = true;
            if ("ROLE_STUDENT".equals(r.getName()))
                isStudent = true;
        }
        for (com.pragatix.entity.SubRole sr : user.getSubRoles()) {
            if ("HOD".equalsIgnoreCase(sr.getName()))
                return "HOD";
            if ("CC".equalsIgnoreCase(sr.getName()) || "CLASS_COORDINATOR".equalsIgnoreCase(sr.getName()))
                return "CC";
        }
        if (isTeacher)
            return "TEACHER";
        if (isStudent) {
            return "STUDENT";
        }
        return "STAFF";
    }

    private ProfileResponse.SuperAdminDetails buildSuperAdminDetails(User user) {
        ProfileResponse.SuperAdminDetails d = new ProfileResponse.SuperAdminDetails();
        d.setTotalDepartments(departmentRepository.count());
        d.setTotalStudents(studentRepository.count());
        d.setTotalTeachers(facultyRepository.count());
        d.setTotalAdmins(0);
        d.setTotalActivities(0);
        d.setTotalStages(0);
        d.setPermissions(List.of("Full System Access", "Manage Users", "System Configuration"));
        return d;
    }

    private ProfileResponse.AdminDetails buildAdminDetails(User user) {
        ProfileResponse.AdminDetails d = new ProfileResponse.AdminDetails();
        d.setAcademicYear(AuthUtils.getAssignedYearString(user.getAcademicYear()));
        d.setTotalStudentsInYear(0);
        d.setTotalGroups(0);
        d.setTotalActivities(0);
        d.setTotalStages(0);
        d.setPermissions(List.of("Manage Students (Assigned Year)", "Manage Attendance", "View Reports"));
        return d;
    }

    private ProfileResponse.HodDetails buildHodDetails(User user) {
        ProfileResponse.HodDetails d = new ProfileResponse.HodDetails();
        
        long totalFaculty = 0;
        long totalStudents = 0;
        
        if (user.getDepartment() != null) {
            totalFaculty = facultyRepository.countByDepartmentId(user.getDepartment().getId());
            totalStudents = studentRepository.countByDepartmentId(user.getDepartment().getId());
        }
        
        d.setTotalFaculty((int) totalFaculty);
        d.setTotalStudents((int) totalStudents);
        d.setTotalSections(0);
        d.setTotalSubjects(0);
        d.setPermissions(List.of("Manage Faculty", "View Students", "Assign Faculty"));
        return d;
    }

    private String normalizeYear(String year) {
        if (year == null) return null;
        switch (year.toUpperCase().trim()) {
            case "I": return "1";
            case "II": return "2";
            case "III": return "3";
            case "IV": return "4";
            case "V": return "5";
            default: return year;
        }
    }

    private ProfileResponse.CcDetails buildCcDetails(User user) {
        ProfileResponse.CcDetails d = new ProfileResponse.CcDetails();
        d.setSection(user.getSection() != null ? user.getSection().getSectionName() : "N/A");
        d.setAcademicYear(user.getYear());
        
        long totalStudents = 0;
        if (user.getDepartment() != null && user.getYear() != null && user.getSection() != null) {
            String studentYear = normalizeYear(user.getYear());
            totalStudents = studentRepository.countByDepartmentIdAndYearAndSectionId(
                user.getDepartment().getId(), studentYear, user.getSection().getId());
        }
        
        d.setTotalStudents((int) totalStudents);
        d.setTotalActivities(0);
        d.setPermissions(List.of("Manage Class", "View Attendance", "View Student Progress"));
        return d;
    }

    private ProfileResponse.TeacherDetails buildTeacherDetails(User user) {
        ProfileResponse.TeacherDetails d = new ProfileResponse.TeacherDetails();
        d.setEmployeeId(user.getUsername());
        d.setTotalStudents(0);
        d.setTotalActivities(0);
        d.setTotalSections(0);
        d.setAttendanceTakenCount(0);
        d.setSubjectsHandling(new ArrayList<>());
        d.setPermissions(List.of("Take Attendance", "Award XP", "View Student Profiles"));
        return d;
    }

    private ProfileResponse.StudentDetails buildStudentDetails(Student student) {
        ProfileResponse.StudentDetails d = new ProfileResponse.StudentDetails();
        d.setRegisterNumber(student.getRegNo());
        d.setRollNumber(student.getSprNo() != null ? student.getSprNo() : "N/A");
        d.setAcademicYear(student.getYearRef() != null ? student.getYearRef().getYearName() : student.getYear());
        d.setSection(student.getSection() != null ? student.getSection().getSectionName() : "N/A");
        d.setSemester(student.getSemesterRef() != null ? student.getSemesterRef().getSemesterName() : (student.getSemester() != null ? student.getSemester() : "N/A"));
        d.setBatch("N/A");
        d.setPermissions(List.of("View Profile", "View Attendance", "View Leaderboard"));
        
        boolean isCap = student.getTeam() != null && student.getTeam().getCaptain() != null
                && student.getTeam().getCaptain().getId().equals(student.getId());
        boolean isViceCap = false;
        if (student.getTeam() != null) {
            if (student.getTeam().getViceCaptain() != null && student.getTeam().getViceCaptain().getId().equals(student.getId())) {
                isViceCap = true;
            } else {
                List<com.pragatix.entity.StageTeam> stageTeams = stageTeamRepository.findByTeamId(student.getTeam().getId());
                for (com.pragatix.entity.StageTeam st : stageTeams) {
                    if (st.getViceCaptain() != null && st.getViceCaptain().getId().equals(student.getId())) {
                        isViceCap = true;
                        break;
                    }
                }
            }
        }

        d.setCurrentXp(student.getTotalXp());
        d.setCurrentStage("Stage " + student.getStage());
        d.setCurrentLevel(String.valueOf(student.getStage()));
        d.setRank(studentRepository.getStudentRankByTotalXp(student.getTotalXp()));
        d.setAttendancePercentage(100.0);
        d.setTeamName(student.getTeam() != null ? student.getTeam().getName() : "N/A");
        d.setCaptain(isCap);
        d.setViceCaptain(isViceCap);
        if (student.getTeam() != null) {
            d.setTeamMembersCount(student.getTeam().getMembers() != null ? student.getTeam().getMembers().size() : 0);
            d.setTeamXp(0);
        }

        return d;
    }
}
