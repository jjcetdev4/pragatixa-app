package com.pragatix.modules.attendance.dto.response;

public class StudentAttendanceSummaryResponse {
    private double attendancePercentage;
    private double monthlyAttendancePercentage;
    private int currentStreak;
    private long totalPresentDays;
    private long totalAbsentDays;

    public StudentAttendanceSummaryResponse() {
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public double getMonthlyAttendancePercentage() {
        return monthlyAttendancePercentage;
    }

    public void setMonthlyAttendancePercentage(double monthlyAttendancePercentage) {
        this.monthlyAttendancePercentage = monthlyAttendancePercentage;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public long getTotalPresentDays() {
        return totalPresentDays;
    }

    public void setTotalPresentDays(long totalPresentDays) {
        this.totalPresentDays = totalPresentDays;
    }

    public long getTotalAbsentDays() {
        return totalAbsentDays;
    }

    public void setTotalAbsentDays(long totalAbsentDays) {
        this.totalAbsentDays = totalAbsentDays;
    }
}
