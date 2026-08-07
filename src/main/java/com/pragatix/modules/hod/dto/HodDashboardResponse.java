package com.pragatix.modules.hod.dto;

import java.util.List;

public class HodDashboardResponse {
    private DepartmentInfoDTO departmentInfo;
    private List<String> availableYears;
    private String selectedYear;
    private DepartmentOverviewDTO overview;
    private HodAttendanceAnalyticsDTO attendance;
    private HodXpAnalyticsDTO xp;
    private HodDisciplineAnalyticsDTO discipline;
    private List<LeaderboardStudentDTO> leaderboard;
    private List<SectionComparisonDTO> sectionComparison;
    private List<RecentPenaltyDTO> recentPenalties;
    private List<YearComparisonDTO> yearComparison;

    public HodDashboardResponse() {
    }

    public HodDashboardResponse(DepartmentInfoDTO departmentInfo, List<String> availableYears, String selectedYear,
                                DepartmentOverviewDTO overview, HodAttendanceAnalyticsDTO attendance,
                                HodXpAnalyticsDTO xp, HodDisciplineAnalyticsDTO discipline,
                                List<LeaderboardStudentDTO> leaderboard, List<SectionComparisonDTO> sectionComparison,
                                List<RecentPenaltyDTO> recentPenalties, List<YearComparisonDTO> yearComparison) {
        this.departmentInfo = departmentInfo;
        this.availableYears = availableYears;
        this.selectedYear = selectedYear;
        this.overview = overview;
        this.attendance = attendance;
        this.xp = xp;
        this.discipline = discipline;
        this.leaderboard = leaderboard;
        this.sectionComparison = sectionComparison;
        this.recentPenalties = recentPenalties;
        this.yearComparison = yearComparison;
    }

    public DepartmentInfoDTO getDepartmentInfo() {
        return departmentInfo;
    }

    public void setDepartmentInfo(DepartmentInfoDTO departmentInfo) {
        this.departmentInfo = departmentInfo;
    }

    public List<String> getAvailableYears() {
        return availableYears;
    }

    public void setAvailableYears(List<String> availableYears) {
        this.availableYears = availableYears;
    }

    public String getSelectedYear() {
        return selectedYear;
    }

    public void setSelectedYear(String selectedYear) {
        this.selectedYear = selectedYear;
    }

    public DepartmentOverviewDTO getOverview() {
        return overview;
    }

    public void setOverview(DepartmentOverviewDTO overview) {
        this.overview = overview;
    }

    public HodAttendanceAnalyticsDTO getAttendance() {
        return attendance;
    }

    public void setAttendance(HodAttendanceAnalyticsDTO attendance) {
        this.attendance = attendance;
    }

    public HodXpAnalyticsDTO getXp() {
        return xp;
    }

    public void setXp(HodXpAnalyticsDTO xp) {
        this.xp = xp;
    }

    public HodDisciplineAnalyticsDTO getDiscipline() {
        return discipline;
    }

    public void setDiscipline(HodDisciplineAnalyticsDTO discipline) {
        this.discipline = discipline;
    }

    public List<LeaderboardStudentDTO> getLeaderboard() {
        return leaderboard;
    }

    public void setLeaderboard(List<LeaderboardStudentDTO> leaderboard) {
        this.leaderboard = leaderboard;
    }

    public List<SectionComparisonDTO> getSectionComparison() {
        return sectionComparison;
    }

    public void setSectionComparison(List<SectionComparisonDTO> sectionComparison) {
        this.sectionComparison = sectionComparison;
    }

    public List<RecentPenaltyDTO> getRecentPenalties() {
        return recentPenalties;
    }

    public void setRecentPenalties(List<RecentPenaltyDTO> recentPenalties) {
        this.recentPenalties = recentPenalties;
    }

    public List<YearComparisonDTO> getYearComparison() {
        return yearComparison;
    }

    public void setYearComparison(List<YearComparisonDTO> yearComparison) {
        this.yearComparison = yearComparison;
    }

    // ================== NESTED DTOS ==================

    public static class DepartmentInfoDTO {
        private Long id;
        private String name;
        private String code;

        public DepartmentInfoDTO() {}

        public DepartmentInfoDTO(Long id, String name, String code) {
            this.id = id;
            this.name = name;
            this.code = code;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class DepartmentOverviewDTO {
        private long totalStudents;
        private long totalTeachers;
        private long totalSections;
        private double averageXp;
        private double averageDisciplineScore;

        public DepartmentOverviewDTO() {}

        public DepartmentOverviewDTO(long totalStudents, long totalTeachers, long totalSections, double averageXp, double averageDisciplineScore) {
            this.totalStudents = totalStudents;
            this.totalTeachers = totalTeachers;
            this.totalSections = totalSections;
            this.averageXp = averageXp;
            this.averageDisciplineScore = averageDisciplineScore;
        }

        public long getTotalStudents() { return totalStudents; }
        public void setTotalStudents(long totalStudents) { this.totalStudents = totalStudents; }
        public long getTotalTeachers() { return totalTeachers; }
        public void setTotalTeachers(long totalTeachers) { this.totalTeachers = totalTeachers; }
        public long getTotalSections() { return totalSections; }
        public void setTotalSections(long totalSections) { this.totalSections = totalSections; }
        public double getAverageXp() { return averageXp; }
        public void setAverageXp(double averageXp) { this.averageXp = averageXp; }
        public double getAverageDisciplineScore() { return averageDisciplineScore; }
        public void setAverageDisciplineScore(double averageDisciplineScore) { this.averageDisciplineScore = averageDisciplineScore; }
    }

    public static class RecentPenaltyDTO {
        private Long id;
        private Long studentId;
        private String studentName;
        private String regNo;
        private String sectionName;
        private String reason;
        private int penaltyXp;
        private String penaltyDate;
        private String addedBy;

        public RecentPenaltyDTO() {}

        public RecentPenaltyDTO(Long id, Long studentId, String studentName, String regNo, String sectionName,
                                String reason, int penaltyXp, String penaltyDate, String addedBy) {
            this.id = id;
            this.studentId = studentId;
            this.studentName = studentName;
            this.regNo = regNo;
            this.sectionName = sectionName;
            this.reason = reason;
            this.penaltyXp = penaltyXp;
            this.penaltyDate = penaltyDate;
            this.addedBy = addedBy;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        public String getRegNo() { return regNo; }
        public void setRegNo(String regNo) { this.regNo = regNo; }
        public String getSectionName() { return sectionName; }
        public void setSectionName(String sectionName) { this.sectionName = sectionName; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public int getPenaltyXp() { return penaltyXp; }
        public void setPenaltyXp(int penaltyXp) { this.penaltyXp = penaltyXp; }
        public String getPenaltyDate() { return penaltyDate; }
        public void setPenaltyDate(String penaltyDate) { this.penaltyDate = penaltyDate; }
        public String getAddedBy() { return addedBy; }
        public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
    }

    public static class HodAttendanceAnalyticsDTO {
        private double overallAttendancePct;
        private int presentCount;
        private int partialAbsentCount;
        private int fullAbsentCount;
        private int totalRecords;
        private AttendanceDistributionDTO distribution;
        private List<AttendanceTrendItemDTO> trend;
        private List<AttendanceHeatmapItemDTO> heatmap;
        private List<WeeklyAttendanceSummaryDTO> weeklySummary;
        private List<SectionAttendanceDTO> sectionAttendance;

        public HodAttendanceAnalyticsDTO() {}

        public HodAttendanceAnalyticsDTO(double overallAttendancePct, int presentCount, int partialAbsentCount, int fullAbsentCount,
                                        int totalRecords, AttendanceDistributionDTO distribution, List<AttendanceTrendItemDTO> trend,
                                        List<AttendanceHeatmapItemDTO> heatmap, List<WeeklyAttendanceSummaryDTO> weeklySummary,
                                        List<SectionAttendanceDTO> sectionAttendance) {
            this.overallAttendancePct = overallAttendancePct;
            this.presentCount = presentCount;
            this.partialAbsentCount = partialAbsentCount;
            this.fullAbsentCount = fullAbsentCount;
            this.totalRecords = totalRecords;
            this.distribution = distribution;
            this.trend = trend;
            this.heatmap = heatmap;
            this.weeklySummary = weeklySummary;
            this.sectionAttendance = sectionAttendance;
        }

        public double getOverallAttendancePct() { return overallAttendancePct; }
        public void setOverallAttendancePct(double overallAttendancePct) { this.overallAttendancePct = overallAttendancePct; }
        public int getPresentCount() { return presentCount; }
        public void setPresentCount(int presentCount) { this.presentCount = presentCount; }
        public int getPartialAbsentCount() { return partialAbsentCount; }
        public void setPartialAbsentCount(int partialAbsentCount) { this.partialAbsentCount = partialAbsentCount; }
        public int getFullAbsentCount() { return fullAbsentCount; }
        public void setFullAbsentCount(int fullAbsentCount) { this.fullAbsentCount = fullAbsentCount; }
        public int getTotalRecords() { return totalRecords; }
        public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
        public AttendanceDistributionDTO getDistribution() { return distribution; }
        public void setDistribution(AttendanceDistributionDTO distribution) { this.distribution = distribution; }
        public List<AttendanceTrendItemDTO> getTrend() { return trend; }
        public void setTrend(List<AttendanceTrendItemDTO> trend) { this.trend = trend; }
        public List<AttendanceHeatmapItemDTO> getHeatmap() { return heatmap; }
        public void setHeatmap(List<AttendanceHeatmapItemDTO> heatmap) { this.heatmap = heatmap; }
        public List<WeeklyAttendanceSummaryDTO> getWeeklySummary() { return weeklySummary; }
        public void setWeeklySummary(List<WeeklyAttendanceSummaryDTO> weeklySummary) { this.weeklySummary = weeklySummary; }
        public List<SectionAttendanceDTO> getSectionAttendance() { return sectionAttendance; }
        public void setSectionAttendance(List<SectionAttendanceDTO> sectionAttendance) { this.sectionAttendance = sectionAttendance; }
    }

    public static class AttendanceDistributionDTO {
        private double presentPct;
        private double partialAbsentPct;
        private double fullAbsentPct;

        public AttendanceDistributionDTO() {}

        public AttendanceDistributionDTO(double presentPct, double partialAbsentPct, double fullAbsentPct) {
            this.presentPct = presentPct;
            this.partialAbsentPct = partialAbsentPct;
            this.fullAbsentPct = fullAbsentPct;
        }

        public double getPresentPct() { return presentPct; }
        public void setPresentPct(double presentPct) { this.presentPct = presentPct; }
        public double getPartialAbsentPct() { return partialAbsentPct; }
        public void setPartialAbsentPct(double partialAbsentPct) { this.partialAbsentPct = partialAbsentPct; }
        public double getFullAbsentPct() { return fullAbsentPct; }
        public void setFullAbsentPct(double fullAbsentPct) { this.fullAbsentPct = fullAbsentPct; }
    }

    public static class AttendanceTrendItemDTO {
        private String date;
        private String label;
        private double percentage;

        public AttendanceTrendItemDTO() {}

        public AttendanceTrendItemDTO(String date, String label, double percentage) {
            this.date = date;
            this.label = label;
            this.percentage = percentage;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
    }

    public static class AttendanceHeatmapItemDTO {
        private String date;
        private int dayOfWeek;
        private int weekNumber;
        private double percentage;

        public AttendanceHeatmapItemDTO() {}

        public AttendanceHeatmapItemDTO(String date, int dayOfWeek, int weekNumber, double percentage) {
            this.date = date;
            this.dayOfWeek = dayOfWeek;
            this.weekNumber = weekNumber;
            this.percentage = percentage;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public int getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
        public int getWeekNumber() { return weekNumber; }
        public void setWeekNumber(int weekNumber) { this.weekNumber = weekNumber; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
    }

    public static class WeeklyAttendanceSummaryDTO {
        private String weekLabel;
        private double attendancePct;
        private int presentCount;
        private int absentCount;

        public WeeklyAttendanceSummaryDTO() {}

        public WeeklyAttendanceSummaryDTO(String weekLabel, double attendancePct, int presentCount, int absentCount) {
            this.weekLabel = weekLabel;
            this.attendancePct = attendancePct;
            this.presentCount = presentCount;
            this.absentCount = absentCount;
        }

        public String getWeekLabel() { return weekLabel; }
        public void setWeekLabel(String weekLabel) { this.weekLabel = weekLabel; }
        public double getAttendancePct() { return attendancePct; }
        public void setAttendancePct(double attendancePct) { this.attendancePct = attendancePct; }
        public int getPresentCount() { return presentCount; }
        public void setPresentCount(int presentCount) { this.presentCount = presentCount; }
        public int getAbsentCount() { return absentCount; }
        public void setAbsentCount(int absentCount) { this.absentCount = absentCount; }
    }

    public static class SectionAttendanceDTO {
        private Long sectionId;
        private String sectionName;
        private double attendancePercentage;
        private int studentCount;

        public SectionAttendanceDTO() {}

        public SectionAttendanceDTO(Long sectionId, String sectionName, double attendancePercentage, int studentCount) {
            this.sectionId = sectionId;
            this.sectionName = sectionName;
            this.attendancePercentage = attendancePercentage;
            this.studentCount = studentCount;
        }

        public Long getSectionId() { return sectionId; }
        public void setSectionId(Long sectionId) { this.sectionId = sectionId; }
        public String getSectionName() { return sectionName; }
        public void setSectionName(String sectionName) { this.sectionName = sectionName; }
        public double getAttendancePercentage() { return attendancePercentage; }
        public void setAttendancePercentage(double attendancePercentage) { this.attendancePercentage = attendancePercentage; }
        public int getStudentCount() { return studentCount; }
        public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
    }

    public static class HodXpAnalyticsDTO {
        private long totalXp;
        private long awardXp;
        private long penaltyXp;
        private long netXp;
        private List<MonthlyXpTrendDTO> monthlyTrend;
        private AwardVsPenaltyDTO awardVsPenalty;
        private List<XpHeatmapItemDTO> xpHeatmap;
        private List<StudentXpSummaryDTO> topStudents;
        private List<StudentXpSummaryDTO> lowestStudents;

        public HodXpAnalyticsDTO() {}

        public HodXpAnalyticsDTO(long totalXp, long awardXp, long penaltyXp, long netXp,
                                List<MonthlyXpTrendDTO> monthlyTrend, AwardVsPenaltyDTO awardVsPenalty,
                                List<XpHeatmapItemDTO> xpHeatmap, List<StudentXpSummaryDTO> topStudents,
                                List<StudentXpSummaryDTO> lowestStudents) {
            this.totalXp = totalXp;
            this.awardXp = awardXp;
            this.penaltyXp = penaltyXp;
            this.netXp = netXp;
            this.monthlyTrend = monthlyTrend;
            this.awardVsPenalty = awardVsPenalty;
            this.xpHeatmap = xpHeatmap;
            this.topStudents = topStudents;
            this.lowestStudents = lowestStudents;
        }

        public long getTotalXp() { return totalXp; }
        public void setTotalXp(long totalXp) { this.totalXp = totalXp; }
        public long getAwardXp() { return awardXp; }
        public void setAwardXp(long awardXp) { this.awardXp = awardXp; }
        public long getPenaltyXp() { return penaltyXp; }
        public void setPenaltyXp(long penaltyXp) { this.penaltyXp = penaltyXp; }
        public long getNetXp() { return netXp; }
        public void setNetXp(long netXp) { this.netXp = netXp; }
        public List<MonthlyXpTrendDTO> getMonthlyTrend() { return monthlyTrend; }
        public void setMonthlyTrend(List<MonthlyXpTrendDTO> monthlyTrend) { this.monthlyTrend = monthlyTrend; }
        public AwardVsPenaltyDTO getAwardVsPenalty() { return awardVsPenalty; }
        public void setAwardVsPenalty(AwardVsPenaltyDTO awardVsPenalty) { this.awardVsPenalty = awardVsPenalty; }
        public List<XpHeatmapItemDTO> getXpHeatmap() { return xpHeatmap; }
        public void setXpHeatmap(List<XpHeatmapItemDTO> xpHeatmap) { this.xpHeatmap = xpHeatmap; }
        public List<StudentXpSummaryDTO> getTopStudents() { return topStudents; }
        public void setTopStudents(List<StudentXpSummaryDTO> topStudents) { this.topStudents = topStudents; }
        public List<StudentXpSummaryDTO> getLowestStudents() { return lowestStudents; }
        public void setLowestStudents(List<StudentXpSummaryDTO> lowestStudents) { this.lowestStudents = lowestStudents; }
    }

    public static class MonthlyXpTrendDTO {
        private String month;
        private long awardXp;
        private long penaltyXp;
        private long netXp;

        public MonthlyXpTrendDTO() {}

        public MonthlyXpTrendDTO(String month, long awardXp, long penaltyXp, long netXp) {
            this.month = month;
            this.awardXp = awardXp;
            this.penaltyXp = penaltyXp;
            this.netXp = netXp;
        }

        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        public long getAwardXp() { return awardXp; }
        public void setAwardXp(long awardXp) { this.awardXp = awardXp; }
        public long getPenaltyXp() { return penaltyXp; }
        public void setPenaltyXp(long penaltyXp) { this.penaltyXp = penaltyXp; }
        public long getNetXp() { return netXp; }
        public void setNetXp(long netXp) { this.netXp = netXp; }
    }

    public static class AwardVsPenaltyDTO {
        private long awardCount;
        private long penaltyCount;
        private long awardXp;
        private long penaltyXp;

        public AwardVsPenaltyDTO() {}

        public AwardVsPenaltyDTO(long awardCount, long penaltyCount, long awardXp, long penaltyXp) {
            this.awardCount = awardCount;
            this.penaltyCount = penaltyCount;
            this.awardXp = awardXp;
            this.penaltyXp = penaltyXp;
        }

        public long getAwardCount() { return awardCount; }
        public void setAwardCount(long awardCount) { this.awardCount = awardCount; }
        public long getPenaltyCount() { return penaltyCount; }
        public void setPenaltyCount(long penaltyCount) { this.penaltyCount = penaltyCount; }
        public long getAwardXp() { return awardXp; }
        public void setAwardXp(long awardXp) { this.awardXp = awardXp; }
        public long getPenaltyXp() { return penaltyXp; }
        public void setPenaltyXp(long penaltyXp) { this.penaltyXp = penaltyXp; }
    }

    public static class XpHeatmapItemDTO {
        private String month;
        private int weekNumber;
        private long xp;

        public XpHeatmapItemDTO() {}

        public XpHeatmapItemDTO(String month, int weekNumber, long xp) {
            this.month = month;
            this.weekNumber = weekNumber;
            this.xp = xp;
        }

        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        public int getWeekNumber() { return weekNumber; }
        public void setWeekNumber(int weekNumber) { this.weekNumber = weekNumber; }
        public long getXp() { return xp; }
        public void setXp(long xp) { this.xp = xp; }
    }

    public static class StudentXpSummaryDTO {
        private Long id;
        private String name;
        private String regNo;
        private String sectionName;
        private int xp;
        private int score;

        public StudentXpSummaryDTO() {}

        public StudentXpSummaryDTO(Long id, String name, String regNo, String sectionName, int xp, int score) {
            this.id = id;
            this.name = name;
            this.regNo = regNo;
            this.sectionName = sectionName;
            this.xp = xp;
            this.score = score;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRegNo() { return regNo; }
        public void setRegNo(String regNo) { this.regNo = regNo; }
        public String getSectionName() { return sectionName; }
        public void setSectionName(String sectionName) { this.sectionName = sectionName; }
        public int getXp() { return xp; }
        public void setXp(int xp) { this.xp = xp; }
        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
    }

    public static class HodDisciplineAnalyticsDTO {
        private long totalCases;
        private long positiveActivities;
        private long penalties;
        private long warnings;

        public HodDisciplineAnalyticsDTO() {}

        public HodDisciplineAnalyticsDTO(long totalCases, long positiveActivities, long penalties, long warnings) {
            this.totalCases = totalCases;
            this.positiveActivities = positiveActivities;
            this.penalties = penalties;
            this.warnings = warnings;
        }

        public long getTotalCases() { return totalCases; }
        public void setTotalCases(long totalCases) { this.totalCases = totalCases; }
        public long getPositiveActivities() { return positiveActivities; }
        public void setPositiveActivities(long positiveActivities) { this.positiveActivities = positiveActivities; }
        public long getPenalties() { return penalties; }
        public void setPenalties(long penalties) { this.penalties = penalties; }
        public long getWarnings() { return warnings; }
        public void setWarnings(long warnings) { this.warnings = warnings; }
    }

    public static class LeaderboardStudentDTO {
        private int rank;
        private Long id;
        private String name;
        private String regNo;
        private String sectionName;
        private int xp;
        private int disciplineScore;
        private int stage;

        public LeaderboardStudentDTO() {}

        public LeaderboardStudentDTO(int rank, Long id, String name, String regNo, String sectionName, int xp, int disciplineScore, int stage) {
            this.rank = rank;
            this.id = id;
            this.name = name;
            this.regNo = regNo;
            this.sectionName = sectionName;
            this.xp = xp;
            this.disciplineScore = disciplineScore;
            this.stage = stage;
        }

        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRegNo() { return regNo; }
        public void setRegNo(String regNo) { this.regNo = regNo; }
        public String getSectionName() { return sectionName; }
        public void setSectionName(String sectionName) { this.sectionName = sectionName; }
        public int getXp() { return xp; }
        public void setXp(int xp) { this.xp = xp; }
        public int getDisciplineScore() { return disciplineScore; }
        public void setDisciplineScore(int disciplineScore) { this.disciplineScore = disciplineScore; }
        public int getStage() { return stage; }
        public void setStage(int stage) { this.stage = stage; }
    }

    public static class SectionComparisonDTO {
        private Long sectionId;
        private String sectionName;
        private int studentCount;
        private double averageXp;
        private double attendancePct;
        private double averageDisciplineScore;

        public SectionComparisonDTO() {}

        public SectionComparisonDTO(Long sectionId, String sectionName, int studentCount, double averageXp, double attendancePct, double averageDisciplineScore) {
            this.sectionId = sectionId;
            this.sectionName = sectionName;
            this.studentCount = studentCount;
            this.averageXp = averageXp;
            this.attendancePct = attendancePct;
            this.averageDisciplineScore = averageDisciplineScore;
        }

        public Long getSectionId() { return sectionId; }
        public void setSectionId(Long sectionId) { this.sectionId = sectionId; }
        public String getSectionName() { return sectionName; }
        public void setSectionName(String sectionName) { this.sectionName = sectionName; }
        public int getStudentCount() { return studentCount; }
        public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
        public double getAverageXp() { return averageXp; }
        public void setAverageXp(double averageXp) { this.averageXp = averageXp; }
        public double getAttendancePct() { return attendancePct; }
        public void setAttendancePct(double attendancePct) { this.attendancePct = attendancePct; }
        public double getAverageDisciplineScore() { return averageDisciplineScore; }
        public void setAverageDisciplineScore(double averageDisciplineScore) { this.averageDisciplineScore = averageDisciplineScore; }
    }

    public static class YearComparisonDTO {
        private String year;
        private int studentCount;
        private double averageXp;
        private double attendancePct;
        private double averageDisciplineScore;

        public YearComparisonDTO() {}

        public YearComparisonDTO(String year, int studentCount, double averageXp, double attendancePct, double averageDisciplineScore) {
            this.year = year;
            this.studentCount = studentCount;
            this.averageXp = averageXp;
            this.attendancePct = attendancePct;
            this.averageDisciplineScore = averageDisciplineScore;
        }

        public String getYear() { return year; }
        public void setYear(String year) { this.year = year; }
        public int getStudentCount() { return studentCount; }
        public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
        public double getAverageXp() { return averageXp; }
        public void setAverageXp(double averageXp) { this.averageXp = averageXp; }
        public double getAttendancePct() { return attendancePct; }
        public void setAttendancePct(double attendancePct) { this.attendancePct = attendancePct; }
        public double getAverageDisciplineScore() { return averageDisciplineScore; }
        public void setAverageDisciplineScore(double averageDisciplineScore) { this.averageDisciplineScore = averageDisciplineScore; }
    }
}
