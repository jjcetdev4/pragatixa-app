package jjcet.PragatiX.modules.analytics.dto;

public class DepartmentXpAttendanceDto {
    private String department;
    private String month;
    private Double avgXp;
    private Double attendanceRate;

    public DepartmentXpAttendanceDto() {
    }

    public DepartmentXpAttendanceDto(String department, String month, Double avgXp, Double attendanceRate) {
        this.department = department;
        this.month = month;
        this.avgXp = avgXp;
        this.attendanceRate = attendanceRate;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Double getAvgXp() {
        return avgXp;
    }

    public void setAvgXp(Double avgXp) {
        this.avgXp = avgXp;
    }

    public Double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(Double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }
}