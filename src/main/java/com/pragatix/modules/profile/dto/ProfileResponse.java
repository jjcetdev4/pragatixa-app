package com.pragatix.modules.profile.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProfileResponse {
    // Common Info
    private Long id;
    private String fullName;
    private String username;
    private String email;
    private String phone;
    private String role;
    private String department;
    private String accountStatus;
    private LocalDateTime createdDate;
    private LocalDateTime lastUpdated;

    // Role Specific Stats / Info
    private SuperAdminDetails superAdminDetails;
    private AdminDetails adminDetails;
    private TeacherDetails teacherDetails;
    private StudentDetails studentDetails;
    private CcDetails ccDetails;
    private HodDetails hodDetails;

    public ProfileResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public SuperAdminDetails getSuperAdminDetails() {
        return superAdminDetails;
    }

    public void setSuperAdminDetails(SuperAdminDetails superAdminDetails) {
        this.superAdminDetails = superAdminDetails;
    }

    public AdminDetails getAdminDetails() {
        return adminDetails;
    }

    public void setAdminDetails(AdminDetails adminDetails) {
        this.adminDetails = adminDetails;
    }

    public TeacherDetails getTeacherDetails() {
        return teacherDetails;
    }

    public void setTeacherDetails(TeacherDetails teacherDetails) {
        this.teacherDetails = teacherDetails;
    }

    public StudentDetails getStudentDetails() {
        return studentDetails;
    }

    public void setStudentDetails(StudentDetails studentDetails) {
        this.studentDetails = studentDetails;
    }

    public CcDetails getCcDetails() {
        return ccDetails;
    }

    public void setCcDetails(CcDetails ccDetails) {
        this.ccDetails = ccDetails;
    }

    public HodDetails getHodDetails() {
        return hodDetails;
    }

    public void setHodDetails(HodDetails hodDetails) {
        this.hodDetails = hodDetails;
    }

    public static class SuperAdminDetails {
        private long totalDepartments;
        private long totalStudents;
        private long totalTeachers;
        private long totalStaff;
        private long totalAdmins;
        private long totalActivities;
        private long totalStages;
        private List<String> permissions;

        public long getTotalDepartments() {
            return totalDepartments;
        }

        public void setTotalDepartments(long totalDepartments) {
            this.totalDepartments = totalDepartments;
        }

        public long getTotalStudents() {
            return totalStudents;
        }

        public void setTotalStudents(long totalStudents) {
            this.totalStudents = totalStudents;
        }

        public long getTotalTeachers() {
            return totalTeachers;
        }

        public void setTotalTeachers(long totalTeachers) {
            this.totalTeachers = totalTeachers;
        }

        public long getTotalStaff() {
            return totalStaff;
        }

        public void setTotalStaff(long totalStaff) {
            this.totalStaff = totalStaff;
        }

        public long getTotalAdmins() {
            return totalAdmins;
        }

        public void setTotalAdmins(long totalAdmins) {
            this.totalAdmins = totalAdmins;
        }

        public long getTotalActivities() {
            return totalActivities;
        }

        public void setTotalActivities(long totalActivities) {
            this.totalActivities = totalActivities;
        }

        public long getTotalStages() {
            return totalStages;
        }

        public void setTotalStages(long totalStages) {
            this.totalStages = totalStages;
        }

        public List<String> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<String> permissions) {
            this.permissions = permissions;
        }
    }

    public static class AdminDetails {
        private String academicYear;
        private long totalStudentsInYear;
        private long totalGroups;
        private long totalActivities;
        private long totalStages;
        private List<String> permissions;

        public String getAcademicYear() {
            return academicYear;
        }

        public void setAcademicYear(String academicYear) {
            this.academicYear = academicYear;
        }

        public long getTotalStudentsInYear() {
            return totalStudentsInYear;
        }

        public void setTotalStudentsInYear(long totalStudentsInYear) {
            this.totalStudentsInYear = totalStudentsInYear;
        }

        public long getTotalGroups() {
            return totalGroups;
        }

        public void setTotalGroups(long totalGroups) {
            this.totalGroups = totalGroups;
        }

        public long getTotalActivities() {
            return totalActivities;
        }

        public void setTotalActivities(long totalActivities) {
            this.totalActivities = totalActivities;
        }

        public long getTotalStages() {
            return totalStages;
        }

        public void setTotalStages(long totalStages) {
            this.totalStages = totalStages;
        }

        public List<String> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<String> permissions) {
            this.permissions = permissions;
        }
    }

    public static class TeacherDetails {
        private String employeeId;
        private long totalStudents;
        private long totalActivities;
        private long totalSections;
        private long attendanceTakenCount;
        private List<String> subjectsHandling;
        private List<String> permissions;

        public String getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(String employeeId) {
            this.employeeId = employeeId;
        }

        public long getTotalStudents() {
            return totalStudents;
        }

        public void setTotalStudents(long totalStudents) {
            this.totalStudents = totalStudents;
        }

        public long getTotalActivities() {
            return totalActivities;
        }

        public void setTotalActivities(long totalActivities) {
            this.totalActivities = totalActivities;
        }

        public long getTotalSections() {
            return totalSections;
        }

        public void setTotalSections(long totalSections) {
            this.totalSections = totalSections;
        }

        public long getAttendanceTakenCount() {
            return attendanceTakenCount;
        }

        public void setAttendanceTakenCount(long attendanceTakenCount) {
            this.attendanceTakenCount = attendanceTakenCount;
        }

        public List<String> getSubjectsHandling() {
            return subjectsHandling;
        }

        public void setSubjectsHandling(List<String> subjectsHandling) {
            this.subjectsHandling = subjectsHandling;
        }

        public List<String> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<String> permissions) {
            this.permissions = permissions;
        }
    }

    public static class StudentDetails {
        private String registerNumber;
        private String rollNumber;
        private String academicYear;
        private String section;
        private String semester;
        private String batch;
        private long currentXp;
        private String currentStage;
        private String currentLevel;
        private long rank;
        private double attendancePercentage;
        private String teamName;
        private boolean isCaptain;
        private boolean isViceCaptain;
        private long teamMembersCount;
        private long teamXp;
        private long teamRank;
        private List<String> permissions;

        public String getRegisterNumber() {
            return registerNumber;
        }

        public void setRegisterNumber(String registerNumber) {
            this.registerNumber = registerNumber;
        }

        public String getRollNumber() {
            return rollNumber;
        }

        public void setRollNumber(String rollNumber) {
            this.rollNumber = rollNumber;
        }

        public String getAcademicYear() {
            return academicYear;
        }

        public void setAcademicYear(String academicYear) {
            this.academicYear = academicYear;
        }

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }

        public String getSemester() {
            return semester;
        }

        public void setSemester(String semester) {
            this.semester = semester;
        }

        public String getBatch() {
            return batch;
        }

        public void setBatch(String batch) {
            this.batch = batch;
        }

        public long getCurrentXp() {
            return currentXp;
        }

        public void setCurrentXp(long currentXp) {
            this.currentXp = currentXp;
        }

        public String getCurrentStage() {
            return currentStage;
        }

        public void setCurrentStage(String currentStage) {
            this.currentStage = currentStage;
        }

        public String getCurrentLevel() {
            return currentLevel;
        }

        public void setCurrentLevel(String currentLevel) {
            this.currentLevel = currentLevel;
        }

        public long getRank() {
            return rank;
        }

        public void setRank(long rank) {
            this.rank = rank;
        }

        public double getAttendancePercentage() {
            return attendancePercentage;
        }

        public void setAttendancePercentage(double attendancePercentage) {
            this.attendancePercentage = attendancePercentage;
        }

        public String getTeamName() {
            return teamName;
        }

        public void setTeamName(String teamName) {
            this.teamName = teamName;
        }

        @JsonProperty("isCaptain")
        public boolean isCaptain() {
            return isCaptain;
        }

        public void setCaptain(boolean captain) {
            isCaptain = captain;
        }

        @JsonProperty("isViceCaptain")
        public boolean isViceCaptain() {
            return isViceCaptain;
        }

        public void setViceCaptain(boolean viceCaptain) {
            isViceCaptain = viceCaptain;
        }

        public long getTeamMembersCount() {
            return teamMembersCount;
        }

        public void setTeamMembersCount(long teamMembersCount) {
            this.teamMembersCount = teamMembersCount;
        }

        public long getTeamXp() {
            return teamXp;
        }

        public void setTeamXp(long teamXp) {
            this.teamXp = teamXp;
        }

        public long getTeamRank() {
            return teamRank;
        }

        public void setTeamRank(long teamRank) {
            this.teamRank = teamRank;
        }

        public List<String> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<String> permissions) {
            this.permissions = permissions;
        }
    }

    public static class CcDetails {
        private String section;
        private String academicYear;
        private long totalStudents;
        private long totalActivities;
        private List<String> permissions;

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }

        public String getAcademicYear() {
            return academicYear;
        }

        public void setAcademicYear(String academicYear) {
            this.academicYear = academicYear;
        }

        public long getTotalStudents() {
            return totalStudents;
        }

        public void setTotalStudents(long totalStudents) {
            this.totalStudents = totalStudents;
        }

        public long getTotalActivities() {
            return totalActivities;
        }

        public void setTotalActivities(long totalActivities) {
            this.totalActivities = totalActivities;
        }

        public List<String> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<String> permissions) {
            this.permissions = permissions;
        }
    }

    public static class HodDetails {
        private long totalFaculty;
        private long totalStudents;
        private long totalSections;
        private long totalSubjects;
        private List<String> permissions;

        public long getTotalFaculty() {
            return totalFaculty;
        }

        public void setTotalFaculty(long totalFaculty) {
            this.totalFaculty = totalFaculty;
        }

        public long getTotalStudents() {
            return totalStudents;
        }

        public void setTotalStudents(long totalStudents) {
            this.totalStudents = totalStudents;
        }

        public long getTotalSections() {
            return totalSections;
        }

        public void setTotalSections(long totalSections) {
            this.totalSections = totalSections;
        }

        public long getTotalSubjects() {
            return totalSubjects;
        }

        public void setTotalSubjects(long totalSubjects) {
            this.totalSubjects = totalSubjects;
        }

        public List<String> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<String> permissions) {
            this.permissions = permissions;
        }
    }
}
