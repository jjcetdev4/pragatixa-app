package com.spdms.dto;

import java.util.List;

public class MyActivityStudentsResponse {
    private ActivityDetail activity;
    private List<StudentDetail> students;
    private int xpLimit;
    private AssignmentDetail assignment;

    public MyActivityStudentsResponse() {}

    public MyActivityStudentsResponse(ActivityDetail activity, List<StudentDetail> students, int xpLimit, AssignmentDetail assignment) {
        this.activity = activity;
        this.students = students;
        this.xpLimit = xpLimit;
        this.assignment = assignment;
    }

    public ActivityDetail getActivity() { return activity; }
    public void setActivity(ActivityDetail activity) { this.activity = activity; }

    public List<StudentDetail> getStudents() { return students; }
    public void setStudents(List<StudentDetail> students) { this.students = students; }

    public int getXpLimit() { return xpLimit; }
    public void setXpLimit(int xpLimit) { this.xpLimit = xpLimit; }

    public AssignmentDetail getAssignment() { return assignment; }
    public void setAssignment(AssignmentDetail assignment) { this.assignment = assignment; }

    public static class ActivityDetail {
        private Long id;
        private String name;
        private String description;
        private String department;
        private List<String> evidence;
        private String frequency;
        private String type;

        public ActivityDetail() {}

        public ActivityDetail(Long id, String name, String description, String department, List<String> evidence, String frequency, String type) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.department = department;
            this.evidence = evidence;
            this.frequency = frequency;
            this.type = type;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public List<String> getEvidence() { return evidence; }
        public void setEvidence(List<String> evidence) { this.evidence = evidence; }

        public String getFrequency() { return frequency; }
        public void setFrequency(String frequency) { this.frequency = frequency; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class StudentDetail {
        private Long id;
        private String fullName;
        private String studentId;
        private Long regNo;
        private String departmentName;
        private String sectionName;
        private int totalXp;
        private int score;

        public StudentDetail() {}

        public StudentDetail(Long id, String fullName, String studentId, Long regNo, String departmentName, String sectionName, int totalXp, int score) {
            this.id = id;
            this.fullName = fullName;
            this.studentId = studentId;
            this.regNo = regNo;
            this.departmentName = departmentName;
            this.sectionName = sectionName;
            this.totalXp = totalXp;
            this.score = score;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }

        public Long getRegNo() { return regNo; }
        public void setRegNo(Long regNo) { this.regNo = regNo; }

        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

        public String getSectionName() { return sectionName; }
        public void setSectionName(String sectionName) { this.sectionName = sectionName; }

        public int getTotalXp() { return totalXp; }
        public void setTotalXp(int totalXp) { this.totalXp = totalXp; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
    }

    public static class AssignmentDetail {
        private Long id;
        private String assignedBy;
        private String assignedAt;

        public AssignmentDetail() {}

        public AssignmentDetail(Long id, String assignedBy, String assignedAt) {
            this.id = id;
            this.assignedBy = assignedBy;
            this.assignedAt = assignedAt;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getAssignedBy() { return assignedBy; }
        public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

        public String getAssignedAt() { return assignedAt; }
        public void setAssignedAt(String assignedAt) { this.assignedAt = assignedAt; }
    }
}
