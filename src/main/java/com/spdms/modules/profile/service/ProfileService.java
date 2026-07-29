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

    public ProfileService(AuthUtils authUtils,
            StudentRepository studentRepository,
            FacultyRepository facultyRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            TeamRepository teamRepository) {
        this.authUtils = authUtils;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile() {
        User user = authUtils.getCurrentUser();
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
        switch (primaryRole) {
            case "SUPER_ADMIN":
                response.setSuperAdminDetails(buildSuperAdminDetails(user));
                break;
            case "ADMIN":
                response.setAdminDetails(buildAdminDetails(user));
                break;
            case "HOD":
                response.setHodDetails(buildHodDetails(user));
                break;
            case "CC":
                response.setCcDetails(buildCcDetails(user));
                break;
            case "TEACHER":
                response.setTeacherDetails(buildTeacherDetails(user));
                break;
            case "STUDENT":
            case "CAPTAIN":
            case "VICE_CAPTAIN":
                response.setStudentDetails(buildStudentDetails(user, primaryRole));
                break;
            default:
                break;
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
        for (SubRole sr : user.getSubRoles()) {
            if ("HOD".equals(sr.getName()))
                return "HOD";
            if ("CLASS_COORDINATOR".equals(sr.getName()))
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
        d.setAssignedAcademicYear(AuthUtils.getAssignedYearString(user.getAcademicYear()));
        d.setTotalStudentsInYear(0);
        d.setTotalGroups(0);
        d.setTotalActivities(0);
        d.setTotalStages(0);
        d.setPermissions(List.of("Manage Students (Assigned Year)", "Manage Attendance", "View Reports"));
        return d;
    }

    private ProfileResponse.HodDetails buildHodDetails(User user) {
        ProfileResponse.HodDetails d = new ProfileResponse.HodDetails();
        d.setTotalFaculty(0);
        d.setTotalStudents(0);
        d.setTotalSections(0);
        d.setTotalSubjects(0);
        d.setPermissions(List.of("Manage Faculty", "View Students", "Assign Faculty"));
        return d;
    }

    private ProfileResponse.CcDetails buildCcDetails(User user) {
        ProfileResponse.CcDetails d = new ProfileResponse.CcDetails();
        d.setSection(user.getSection() != null ? user.getSection().getSectionName() : "N/A");
        d.setAssignedAcademicYear(user.getYear());
        d.setTotalStudents(0);
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

    private ProfileResponse.StudentDetails buildStudentDetails(User user, String role) {
        ProfileResponse.StudentDetails d = new ProfileResponse.StudentDetails();
        d.setRegisterNumber(user.getUsername());
        d.setRollNumber("N/A");
        d.setAcademicYear(user.getYear());
        d.setSection(user.getSection() != null ? user.getSection().getSectionName() : "N/A");
        d.setSemester("N/A");
        d.setBatch("N/A");
        d.setCurrentXp(0);
        d.setCurrentStage("N/A");
        d.setCurrentLevel("N/A");
        d.setRank(0);
        d.setAttendancePercentage(100.0);
        d.setTeamName("N/A");
        d.setCaptain("CAPTAIN".equals(role));
        d.setViceCaptain("VICE_CAPTAIN".equals(role));
        d.setPermissions(List.of("View Profile", "View Attendance", "View Leaderboard"));
        return d;
    }
}
