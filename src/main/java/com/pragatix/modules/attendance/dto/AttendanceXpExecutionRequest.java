package com.pragatix.modules.attendance.dto;

import java.time.LocalDate;

public class AttendanceXpExecutionRequest {
    private Long studentId;
    private Long activityId;
    private String attendanceRule;
    private int calculatedXp;
    private boolean isPenalty;
    private LocalDate attendanceDate;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private String reason;
    private String remarks;
    
    public AttendanceXpExecutionRequest() {}
    
    // Getters and Setters
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    
    public String getAttendanceRule() { return attendanceRule; }
    public void setAttendanceRule(String attendanceRule) { this.attendanceRule = attendanceRule; }
    
    public int getCalculatedXp() { return calculatedXp; }
    public void setCalculatedXp(int calculatedXp) { this.calculatedXp = calculatedXp; }
    
    public boolean getIsPenalty() { return isPenalty; }
    public void setIsPenalty(boolean isPenalty) { this.isPenalty = isPenalty; }
    
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    
    public LocalDate getWeekStartDate() { return weekStartDate; }
    public void setWeekStartDate(LocalDate weekStartDate) { this.weekStartDate = weekStartDate; }
    
    public LocalDate getWeekEndDate() { return weekEndDate; }
    public void setWeekEndDate(LocalDate weekEndDate) { this.weekEndDate = weekEndDate; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
