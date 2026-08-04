package com.pragatix.modules.analytics.dto;

public class LowAttendanceStudentDTO {
    private String rollNo;
    private String name;
    private Double attendancePercentage;

    public LowAttendanceStudentDTO() {}

    public LowAttendanceStudentDTO(String rollNo, String name, Double attendancePercentage) {
        this.rollNo = rollNo;
        this.name = name;
        this.attendancePercentage = attendancePercentage;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(Double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }
}
