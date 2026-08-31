package jjcet.PragatiX.modules.analytics.dto;

public class AttendanceSummaryDto {
    private Long totalStudents;
    private Long presentCount;
    private Long absentCount;
    private Long odCount;
    private Long leaveCount;
    private Double presentPercentage;
    private Double absentPercentage;

    public AttendanceSummaryDto() {
    }

    public AttendanceSummaryDto(Long totalStudents, Long presentCount, Double presentPercentage) {
        this.totalStudents = totalStudents;
        this.presentCount = presentCount;
        this.presentPercentage = presentPercentage;
    }

    public AttendanceSummaryDto(Long totalStudents, Long presentCount, Long absentCount, Long odCount, Long leaveCount, Double presentPercentage, Double absentPercentage) {
        this.totalStudents = totalStudents;
        this.presentCount = presentCount;
        this.absentCount = absentCount;
        this.odCount = odCount;
        this.leaveCount = leaveCount;
        this.presentPercentage = presentPercentage;
        this.absentPercentage = absentPercentage;
    }

    public Long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(Long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public Long getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(Long presentCount) {
        this.presentCount = presentCount;
    }

    public Long getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(Long absentCount) {
        this.absentCount = absentCount;
    }

    public Long getOdCount() {
        return odCount;
    }

    public void setOdCount(Long odCount) {
        this.odCount = odCount;
    }

    public Long getLeaveCount() {
        return leaveCount;
    }

    public void setLeaveCount(Long leaveCount) {
        this.leaveCount = leaveCount;
    }

    public Double getPresentPercentage() {
        return presentPercentage;
    }

    public void setPresentPercentage(Double presentPercentage) {
        this.presentPercentage = presentPercentage;
    }

    public Double getAbsentPercentage() {
        return absentPercentage;
    }

    public void setAbsentPercentage(Double absentPercentage) {
        this.absentPercentage = absentPercentage;
    }
}
