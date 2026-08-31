package jjcet.PragatiX.modules.analytics.dto;

import java.util.List;

public class AllInOneDashboardDto {
    private Long totalStudents;
    private Double avgXpAll;
    private Double todayAttendance;
    private String topPerformer;
    private String topTeam;

    // Frontend dashboard data
    private Double totalXp;
    private Double avgAttendance;
    private Double avgEngagement;
    private List<InstitutionGrowthDto> institutionGrowth;
    private List<DepartmentGrowthDto> departmentGrowth;
    private List<XpCurveDto> xpCurve;
    private List<StageDistributionDto> stageDistribution;
    private List<AttendanceDto> attendance;
    private AttendanceSummaryDto attendanceSummary;
    private ActivityFunnelWrapperDto activityFunnel;
    private InterventionStatusDto interventionRisk;
    private List<PerformanceLeaderDto> topPerformers;
    private List<MoverDto> mostImproved;

    public AllInOneDashboardDto() {
    }

    public AllInOneDashboardDto(Long totalStudents, Double avgXpAll,
                               Double todayAttendance, String topPerformer, String topTeam) {
        this.totalStudents = totalStudents;
        this.avgXpAll = avgXpAll;
        this.todayAttendance = todayAttendance;
        this.topPerformer = topPerformer;
        this.topTeam = topTeam;
    }

    public AllInOneDashboardDto(Long totalStudents, Double avgXpAll,
                               Double todayAttendance, String topPerformer, String topTeam,
                               Double totalXp, Double avgAttendance, Double avgEngagement,
                               List<InstitutionGrowthDto> institutionGrowth,
                               List<DepartmentGrowthDto> departmentGrowth,
                               List<XpCurveDto> xpCurve,
                               List<StageDistributionDto> stageDistribution,
                               List<AttendanceDto> attendance,
                               ActivityFunnelWrapperDto activityFunnel,
                               InterventionStatusDto interventionRisk,
                               List<PerformanceLeaderDto> topPerformers,
                               List<MoverDto> mostImproved) {
        this.totalStudents = totalStudents;
        this.avgXpAll = avgXpAll;
        this.todayAttendance = todayAttendance;
        this.topPerformer = topPerformer;
        this.topTeam = topTeam;
        this.totalXp = totalXp;
        this.avgAttendance = avgAttendance;
        this.avgEngagement = avgEngagement;
        this.institutionGrowth = institutionGrowth;
        this.departmentGrowth = departmentGrowth;
        this.xpCurve = xpCurve;
        this.stageDistribution = stageDistribution;
        this.attendance = attendance;
        this.activityFunnel = activityFunnel;
        this.interventionRisk = interventionRisk;
        this.topPerformers = topPerformers;
        this.mostImproved = mostImproved;
    }

    public Long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(Long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public Double getAvgXpAll() {
        return avgXpAll;
    }

    public void setAvgXpAll(Double avgXpAll) {
        this.avgXpAll = avgXpAll;
    }

    public Double getTodayAttendance() {
        return todayAttendance;
    }

    public void setTodayAttendance(Double todayAttendance) {
        this.todayAttendance = todayAttendance;
    }

    public String getTopPerformer() {
        return topPerformer;
    }

    public void setTopPerformer(String topPerformer) {
        this.topPerformer = topPerformer;
    }

    public String getTopTeam() {
        return topTeam;
    }

    public void setTopTeam(String topTeam) {
        this.topTeam = topTeam;
    }

    public Double getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(Double totalXp) {
        this.totalXp = totalXp;
    }

    public Double getAvgAttendance() {
        return avgAttendance;
    }

    public void setAvgAttendance(Double avgAttendance) {
        this.avgAttendance = avgAttendance;
    }

    public Double getAvgEngagement() {
        return avgEngagement;
    }

    public void setAvgEngagement(Double avgEngagement) {
        this.avgEngagement = avgEngagement;
    }

    public List<InstitutionGrowthDto> getInstitutionGrowth() {
        return institutionGrowth;
    }

    public void setInstitutionGrowth(List<InstitutionGrowthDto> institutionGrowth) {
        this.institutionGrowth = institutionGrowth;
    }

    public List<DepartmentGrowthDto> getDepartmentGrowth() {
        return departmentGrowth;
    }

    public void setDepartmentGrowth(List<DepartmentGrowthDto> departmentGrowth) {
        this.departmentGrowth = departmentGrowth;
    }

    public List<XpCurveDto> getXpCurve() {
        return xpCurve;
    }

    public void setXpCurve(List<XpCurveDto> xpCurve) {
        this.xpCurve = xpCurve;
    }

    public List<StageDistributionDto> getStageDistribution() {
        return stageDistribution;
    }

    public void setStageDistribution(List<StageDistributionDto> stageDistribution) {
        this.stageDistribution = stageDistribution;
    }

    public List<AttendanceDto> getAttendance() {
        return attendance;
    }

    public void setAttendance(List<AttendanceDto> attendance) {
        this.attendance = attendance;
    }

    public AttendanceSummaryDto getAttendanceSummary() {
        return attendanceSummary;
    }

    public void setAttendanceSummary(AttendanceSummaryDto attendanceSummary) {
        this.attendanceSummary = attendanceSummary;
    }

    public ActivityFunnelWrapperDto getActivityFunnel() {
        return activityFunnel;
    }

    public void setActivityFunnel(ActivityFunnelWrapperDto activityFunnel) {
        this.activityFunnel = activityFunnel;
    }

    public InterventionStatusDto getInterventionRisk() {
        return interventionRisk;
    }

    public void setInterventionRisk(InterventionStatusDto interventionRisk) {
        this.interventionRisk = interventionRisk;
    }

    public List<PerformanceLeaderDto> getTopPerformers() {
        return topPerformers;
    }

    public void setTopPerformers(List<PerformanceLeaderDto> topPerformers) {
        this.topPerformers = topPerformers;
    }

    public List<MoverDto> getMostImproved() {
        return mostImproved;
    }

    public void setMostImproved(List<MoverDto> mostImproved) {
        this.mostImproved = mostImproved;
    }
}