package com.pragatix.modules.student.dto.response;

import java.util.List;

public class MyActivityStudentsResponse {
    private ActivityDetail activity;
    private List<StudentDetail> students;
    private int xpLimit;
    private AssignmentDetail assignment;

    public MyActivityStudentsResponse() {
    }

    public MyActivityStudentsResponse(ActivityDetail activity, List<StudentDetail> students, int xpLimit,
            AssignmentDetail assignment) {
        this.activity = activity;
        this.students = students;
        this.xpLimit = xpLimit;
        this.assignment = assignment;
    }

    public ActivityDetail getActivity() {
        return activity;
    }

    public void setActivity(ActivityDetail activity) {
        this.activity = activity;
    }

    public List<StudentDetail> getStudents() {
        return students;
    }

    public void setStudents(List<StudentDetail> students) {
        this.students = students;
    }

    public int getXpLimit() {
        return xpLimit;
    }

    public void setXpLimit(int xpLimit) {
        this.xpLimit = xpLimit;
    }

    public AssignmentDetail getAssignment() {
        return assignment;
    }

    public void setAssignment(AssignmentDetail assignment) {
        this.assignment = assignment;
    }

    public static class ActivityDetail {
        private Long id;
        private String name;
        private String description;
        private String department;
        private List<String> evidence;
        private String frequency;
        private String type;
        private String xpCategory;
        private Boolean awardEnabled;
        private Integer awardXp;
        private Boolean penaltyEnabled;
        private Integer penaltyXp;
        private Integer cap;

        public ActivityDetail() {
        }

        public ActivityDetail(Long id, String name, String description, String department, List<String> evidence,
                String frequency, String type,
                String xpCategory, Boolean awardEnabled, Integer awardXp, Boolean penaltyEnabled, Integer penaltyXp,
                Integer cap) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.department = department;
            this.evidence = evidence;
            this.frequency = frequency;
            this.type = type;
            this.xpCategory = xpCategory;
            this.awardEnabled = awardEnabled;
            this.awardXp = awardXp;
            this.penaltyEnabled = penaltyEnabled;
            this.penaltyXp = penaltyXp;
            this.cap = cap;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public List<String> getEvidence() {
            return evidence;
        }

        public void setEvidence(List<String> evidence) {
            this.evidence = evidence;
        }

        public String getFrequency() {
            return frequency;
        }

        public void setFrequency(String frequency) {
            this.frequency = frequency;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getXpCategory() {
            return xpCategory;
        }

        public void setXpCategory(String xpCategory) {
            this.xpCategory = xpCategory;
        }

        public Boolean getAwardEnabled() {
            return awardEnabled;
        }

        public void setAwardEnabled(Boolean awardEnabled) {
            this.awardEnabled = awardEnabled;
        }

        public Integer getAwardXp() {
            return awardXp;
        }

        public void setAwardXp(Integer awardXp) {
            this.awardXp = awardXp;
        }

        public Boolean getPenaltyEnabled() {
            return penaltyEnabled;
        }

        public void setPenaltyEnabled(Boolean penaltyEnabled) {
            this.penaltyEnabled = penaltyEnabled;
        }

        public Integer getPenaltyXp() {
            return penaltyXp;
        }

        public void setPenaltyXp(Integer penaltyXp) {
            this.penaltyXp = penaltyXp;
        }

        public Integer getCap() {
            return cap;
        }

        public void setCap(Integer cap) {
            this.cap = cap;
        }
    }

    public static class StudentDetail {
        private Long id;
        private String fullName;
        private String regNo;
        private String departmentName;
        private String sectionName;
        private String year;
        private int totalXp;
        private int score;

        public StudentDetail() {
        }

        public StudentDetail(Long id, String fullName, String regNo, String departmentName, String sectionName,
                String year, int totalXp, int score) {
            this.id = id;
            this.fullName = fullName;
            this.regNo = regNo;
            this.departmentName = departmentName;
            this.sectionName = sectionName;
            this.year = year;
            this.totalXp = totalXp;
            this.score = score;
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

        public String getRegNo() {
            return regNo;
        }

        public void setRegNo(String regNo) {
            this.regNo = regNo;
        }

        public String getDepartmentName() {
            return departmentName;
        }

        public void setDepartmentName(String departmentName) {
            this.departmentName = departmentName;
        }

        public String getSectionName() {
            return sectionName;
        }

        public void setSectionName(String sectionName) {
            this.sectionName = sectionName;
        }

        public String getYear() {
            return year;
        }

        public void setYear(String year) {
            this.year = year;
        }

        public int getTotalXp() {
            return totalXp;
        }

        public void setTotalXp(int totalXp) {
            this.totalXp = totalXp;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }
    }

    public static class AssignmentDetail {
        private Long id;
        private String assignedBy;
        private String assignedAt;
        private String assignedFacultyName;
        private String assignmentMode;

        public AssignmentDetail() {
        }

        public AssignmentDetail(Long id, String assignedBy, String assignedAt, String assignedFacultyName,
                String assignmentMode) {
            this.id = id;
            this.assignedBy = assignedBy;
            this.assignedAt = assignedAt;
            this.assignedFacultyName = assignedFacultyName;
            this.assignmentMode = assignmentMode;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getAssignedBy() {
            return assignedBy;
        }

        public void setAssignedBy(String assignedBy) {
            this.assignedBy = assignedBy;
        }

        public String getAssignedAt() {
            return assignedAt;
        }

        public void setAssignedAt(String assignedAt) {
            this.assignedAt = assignedAt;
        }

        public String getAssignedFacultyName() {
            return assignedFacultyName;
        }

        public void setAssignedFacultyName(String assignedFacultyName) {
            this.assignedFacultyName = assignedFacultyName;
        }

        public String getAssignmentMode() {
            return assignmentMode;
        }

        public void setAssignmentMode(String assignmentMode) {
            this.assignmentMode = assignmentMode;
        }
    }
}
