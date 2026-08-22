package jjcet.PragatiX.modules.hod.service;

import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import jjcet.PragatiX.modules.faculty.repository.FacultyRepository;
import jjcet.PragatiX.modules.hod.dto.HodDashboardResponse;
import jjcet.PragatiX.modules.hod.dto.HodDashboardResponse.*;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.repository.DepartmentRepository;
import jjcet.PragatiX.repository.SectionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class HodAnalyticsService {

    private final StudentRepository studentRepository;
    private final SectionRepository sectionRepository;
    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final AuthUtils authUtils;

    @PersistenceContext
    private EntityManager entityManager;

    private static final List<String> STANDARD_YEARS = List.of(
            "All Years", "First Year", "Second Year", "Third Year", "Fourth Year");

    public HodAnalyticsService(StudentRepository studentRepository,
            SectionRepository sectionRepository,
            FacultyRepository facultyRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            AuthUtils authUtils) {
        this.studentRepository = studentRepository;
        this.sectionRepository = sectionRepository;
        this.facultyRepository = facultyRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.authUtils = authUtils;
    }

    public HodDashboardResponse getDashboardData(String requestedYear) {
        User currentUser = authUtils.getCurrentUser();
        Long deptId = 1L;
        if (currentUser != null && currentUser.getDepartment() != null) {
            deptId = currentUser.getDepartment().getId();
        } else {
            List<Department> depts = departmentRepository.findAll();
            if (!depts.isEmpty()) {
                deptId = depts.get(0).getId();
            }
        }
        return getDashboardData(deptId, requestedYear);
    }

    public HodDashboardResponse getDashboardData(Long deptId, String requestedYear) {
        // 1. Fetch Department Info
        Department dept = departmentRepository.findById(deptId).orElse(null);
        String deptName = dept != null ? dept.getName() : "Department";
        String deptCode = dept != null ? dept.getCode() : "DEPT";
        DepartmentInfoDTO deptInfo = new DepartmentInfoDTO(deptId, deptName, deptCode);

        // 2. Fetch all sections & students in this department
        List<Section> deptSections = sectionRepository.findByDepartment_Id(deptId);
        List<Student> allDeptStudents = studentRepository.findByDepartmentId(deptId);

        List<String> availableYears = new ArrayList<>(STANDARD_YEARS);

        // Normalize requested year
        String selectedYear = "All Years";
        if (requestedYear != null && !requestedYear.trim().isEmpty()
                && !requestedYear.equalsIgnoreCase("All Years")
                && !requestedYear.equalsIgnoreCase("all")) {
            String trimmed = requestedYear.trim();
            for (String std : STANDARD_YEARS) {
                if (std.equalsIgnoreCase(trimmed) || matchesYearString(std, trimmed)) {
                    selectedYear = std;
                    break;
                }
            }
            if ("All Years".equals(selectedYear)) {
                selectedYear = trimmed;
            }
        }

        final String activeYear = selectedYear;

        // 3. Filter students by selected year
        List<Student> filteredStudents;
        if (activeYear.equalsIgnoreCase("All Years")) {
            filteredStudents = allDeptStudents;
        } else {
            filteredStudents = allDeptStudents.stream()
                    .filter(s -> matchesYear(s, activeYear))
                    .collect(Collectors.toList());
        }

        List<Long> studentIds = filteredStudents.stream().map(Student::getId).collect(Collectors.toList());

        // 4. Department Overview
        long totalStudents = filteredStudents.size();
        long totalTeachers = facultyRepository.countByDepartmentId(deptId);
        if (totalTeachers == 0) {
            totalTeachers = userRepository.countByDepartmentId(deptId);
        }
        long totalSections = deptSections.size();
        double averageXp = filteredStudents.isEmpty() ? 0.0
                : filteredStudents.stream().mapToDouble(Student::getTotalXp).average().orElse(0.0);

        // Average Discipline Score MUST NEVER EXCEED 100
        double rawAvgScore = filteredStudents.isEmpty() ? 100.0
                : filteredStudents.stream().mapToDouble(Student::getScore).average().orElse(100.0);
        double averageDisciplineScore = Math.min(100.0, Math.max(0.0, rawAvgScore));

        DepartmentOverviewDTO overview = new DepartmentOverviewDTO(
                totalStudents, totalTeachers, totalSections,
                Math.round(averageXp * 10.0) / 10.0,
                Math.round(averageDisciplineScore * 10.0) / 10.0);

        // 5. Attendance Analytics
        HodAttendanceAnalyticsDTO attendance = calculateAttendanceAnalytics(deptId, studentIds, activeYear,
                deptSections, filteredStudents);

        // 6. XP Analytics
        HodXpAnalyticsDTO xp = calculateXpAnalytics(deptId, studentIds, filteredStudents);

        // 7. Discipline Analytics
        HodDisciplineAnalyticsDTO discipline = calculateDisciplineAnalytics(deptId, studentIds);

        // 8. Department Leaderboard (Top 10)
        List<LeaderboardStudentDTO> leaderboard = calculateLeaderboard(filteredStudents);

        // 9. Section Comparison
        List<SectionComparisonDTO> sectionComparison = calculateSectionComparison(deptSections, filteredStudents);

        // 10. Recent Penalty History (Newest First)
        List<RecentPenaltyDTO> recentPenalties = calculateRecentPenalties(deptId, studentIds);

        return new HodDashboardResponse(
                deptInfo, availableYears, activeYear, overview,
                attendance, xp, discipline, leaderboard, sectionComparison,
                recentPenalties, Collections.emptyList());
    }

    private boolean matchesYearString(String standardYear, String query) {
        if (standardYear.equalsIgnoreCase(query))
            return true;
        if (query.equals("1") && standardYear.equals("First Year"))
            return true;
        if (query.equals("2") && standardYear.equals("Second Year"))
            return true;
        if (query.equals("3") && standardYear.equals("Third Year"))
            return true;
        if (query.equals("4") && standardYear.equals("Fourth Year"))
            return true;
        return false;
    }

    private boolean matchesYear(Student s, String year) {
        if (year == null || year.isEmpty() || year.equalsIgnoreCase("All Years"))
            return true;

        int targetYearNo = 0;
        String lowerYear = year.toLowerCase();
        if (lowerYear.contains("first") || lowerYear.equals("1") || lowerYear.contains("1st")) {
            targetYearNo = 1;
        } else if (lowerYear.contains("second") || lowerYear.equals("2") || lowerYear.contains("2nd")) {
            targetYearNo = 2;
        } else if (lowerYear.contains("third") || lowerYear.equals("3") || lowerYear.contains("3rd")) {
            targetYearNo = 3;
        } else if (lowerYear.contains("fourth") || lowerYear.equals("4") || lowerYear.contains("4th")) {
            targetYearNo = 4;
        }

        if (targetYearNo > 0) {
            if (s.getYearRef() != null) {
                if (s.getYearRef().getYearNo() == targetYearNo)
                    return true;
                if (s.getYearRef().getYearName() != null
                        && s.getYearRef().getYearName().toLowerCase().contains(lowerYear))
                    return true;
            }
            if (s.getYear() != null) {
                String yStr = s.getYear().trim().toLowerCase();
                if (yStr.equals(String.valueOf(targetYearNo)) || yStr.contains(lowerYear))
                    return true;
            }
        }

        // Generic fallback match
        if (s.getYearRef() != null && s.getYearRef().getYearName() != null
                && s.getYearRef().getYearName().equalsIgnoreCase(year))
            return true;
        if (s.getYear() != null && s.getYear().equalsIgnoreCase(year))
            return true;
        if (s.getAcademicYear() != null && s.getAcademicYear().equalsIgnoreCase(year))
            return true;

        return false;
    }

    private HodAttendanceAnalyticsDTO calculateAttendanceAnalytics(Long deptId, List<Long> studentIds,
            String activeYear,
            List<Section> deptSections, List<Student> filteredStudents) {
        if (studentIds.isEmpty()) {
            return new HodAttendanceAnalyticsDTO(
                    0.0, 0, 0, 0, 0,
                    new AttendanceDistributionDTO(0.0, 0.0, 0.0),
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }

        try {
            // Aggregate overview using standard attendance table
            String overviewSql = "SELECT " +
                    "  COUNT(*) as total_records, " +
                    "  SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) as present_count, " +
                    "  SUM(CASE WHEN ar.status IN ('ABSENT', 'LEAVE') THEN 1 ELSE 0 END) as absent_count " +
                    "FROM attendance ar " +
                    "WHERE ar.student_id IN (:studentIds)";

            Query overviewQuery = entityManager.createNativeQuery(overviewSql);
            overviewQuery.setParameter("studentIds", studentIds);

            Object[] overviewRow = (Object[]) overviewQuery.getSingleResult();
            int totalRecords = overviewRow != null && overviewRow[0] != null ? ((Number) overviewRow[0]).intValue() : 0;
            int presentCount = overviewRow != null && overviewRow[1] != null ? ((Number) overviewRow[1]).intValue() : 0;
            int absentCount = overviewRow != null && overviewRow[2] != null ? ((Number) overviewRow[2]).intValue() : 0;

            double overallPct = totalRecords > 0 ? (presentCount * 100.0) / totalRecords : 0.0;

            // Student-level aggregation for Partial vs Full Absent
            String studentAggSql = "SELECT " +
                    "  ar.student_id, " +
                    "  SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) as p_cnt, " +
                    "  SUM(CASE WHEN ar.status IN ('ABSENT', 'LEAVE') THEN 1 ELSE 0 END) as a_cnt " +
                    "FROM attendance ar " +
                    "WHERE ar.student_id IN (:studentIds) " +
                    "GROUP BY ar.student_id";

            Query studentAggQuery = entityManager.createNativeQuery(studentAggSql);
            studentAggQuery.setParameter("studentIds", studentIds);

            @SuppressWarnings("unchecked")
            List<Object[]> studentAggRows = studentAggQuery.getResultList();

            int fullPresentStudents = 0;
            int partialAbsentStudents = 0;
            int fullAbsentStudents = 0;

            for (Object[] r : studentAggRows) {
                int p = r[1] != null ? ((Number) r[1]).intValue() : 0;
                int a = r[2] != null ? ((Number) r[2]).intValue() : 0;
                if (a == 0 && p > 0)
                    fullPresentStudents++;
                else if (p > 0 && a > 0)
                    partialAbsentStudents++;
                else if (p == 0 && a > 0)
                    fullAbsentStudents++;
            }

            int totalDistinct = studentAggRows.size();
            double presentPct = totalDistinct > 0 ? (fullPresentStudents * 100.0) / totalDistinct : 0.0;
            double partialAbsentPct = totalDistinct > 0 ? (partialAbsentStudents * 100.0) / totalDistinct : 0.0;
            double fullAbsentPct = totalDistinct > 0 ? (fullAbsentStudents * 100.0) / totalDistinct : 0.0;

            AttendanceDistributionDTO distribution = new AttendanceDistributionDTO(
                    Math.round(presentPct * 10.0) / 10.0,
                    Math.round(partialAbsentPct * 10.0) / 10.0,
                    Math.round(fullAbsentPct * 10.0) / 10.0);

            // Daily Trend (Last 7 distinct active days)
            // attendance_records has no date column – date lives in attendance_sessions
            String trendSql = "SELECT " +
                    "  sess.attendance_date, " +
                    "  CAST((SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) * 100.0) / NULLIF(COUNT(ar.id), 0) AS DECIMAL(5,2)) as daily_pct "
                    +
                    "FROM attendance_records ar " +
                    "JOIN attendance_sessions sess ON ar.attendance_session_id = sess.id " +
                    "WHERE ar.student_id IN (:studentIds) " +
                    "GROUP BY sess.attendance_date " +
                    "ORDER BY sess.attendance_date DESC LIMIT 7";

            Query trendQuery = entityManager.createNativeQuery(trendSql);
            trendQuery.setParameter("studentIds", studentIds);

            @SuppressWarnings("unchecked")
            List<Object[]> trendRows = trendQuery.getResultList();
            List<AttendanceTrendItemDTO> trend = new ArrayList<>();
            DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("MMM dd");

            for (Object[] r : trendRows) {
                Object dObj = r[0];
                LocalDate d = null;
                if (dObj instanceof java.sql.Date)
                    d = ((java.sql.Date) dObj).toLocalDate();
                else if (dObj instanceof LocalDate)
                    d = (LocalDate) dObj;
                else if (dObj instanceof String)
                    d = LocalDate.parse((String) dObj);

                double pct = r[1] != null ? ((BigDecimal) r[1]).doubleValue() : 0.0;
                String label = d != null ? d.format(dFmt) : (dObj != null ? dObj.toString() : "");
                trend.add(new AttendanceTrendItemDTO(d != null ? d.toString() : "", label, pct));
            }
            Collections.reverse(trend); // Chronological

            // Weekly Summary (Last 4 weeks)
            String weekSql = "SELECT " +
                    "  CONCAT('Week ', WEEK(sess.attendance_date, 1)) as w_label, " +
                    "  CAST((SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) * 100.0) / NULLIF(COUNT(ar.id), 0) AS DECIMAL(5,2)) as w_pct, "
                    +
                    "  SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) as p_cnt, " +
                    "  SUM(CASE WHEN ar.status IN ('ABSENT', 'LEAVE') THEN 1 ELSE 0 END) as a_cnt " +
                    "FROM attendance_records ar " +
                    "JOIN attendance_sessions sess ON ar.attendance_session_id = sess.id " +
                    "WHERE ar.student_id IN (:studentIds) " +
                    "GROUP BY w_label " +
                    "ORDER BY MIN(sess.attendance_date) DESC LIMIT 4";

            Query weekQuery = entityManager.createNativeQuery(weekSql);
            weekQuery.setParameter("studentIds", studentIds);

            @SuppressWarnings("unchecked")
            List<Object[]> weekRows = weekQuery.getResultList();
            List<WeeklyAttendanceSummaryDTO> weeklySummary = new ArrayList<>();

            for (Object[] r : weekRows) {
                String wLabel = (String) r[0];
                double wPct = r[1] != null ? ((BigDecimal) r[1]).doubleValue() : 0.0;
                int pCnt = r[2] != null ? ((Number) r[2]).intValue() : 0;
                int aCnt = r[3] != null ? ((Number) r[3]).intValue() : 0;
                weeklySummary.add(new WeeklyAttendanceSummaryDTO(wLabel, wPct, pCnt, aCnt));
            }
            Collections.reverse(weeklySummary);

            // Section Attendance Breakdown
            List<SectionAttendanceDTO> sectionAttendance = new ArrayList<>();
            for (Section sec : deptSections) {
                List<Long> secStudentIds = filteredStudents.stream()
                        .filter(s -> s.getSection() != null && s.getSection().getId().equals(sec.getId()))
                        .map(Student::getId)
                        .collect(Collectors.toList());

                if (secStudentIds.isEmpty()) {
                    sectionAttendance.add(new SectionAttendanceDTO(sec.getId(), sec.getSectionName(), 0.0, 0));
                    continue;
                }

                String secSql = "SELECT " +
                        "  CAST((SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) * 100.0) / NULLIF(COUNT(ar.id), 0) AS DECIMAL(5,2)) as sec_pct "
                        +
                        "FROM attendance ar " +
                        "WHERE ar.student_id IN (:secStudentIds)";

                Query secQuery = entityManager.createNativeQuery(secSql);
                secQuery.setParameter("secStudentIds", secStudentIds);
                Object res = secQuery.getSingleResult();
                double secPct = res != null ? ((BigDecimal) res).doubleValue() : 0.0;

                sectionAttendance
                        .add(new SectionAttendanceDTO(sec.getId(), sec.getSectionName(), secPct, secStudentIds.size()));
            }

            return new HodAttendanceAnalyticsDTO(
                    Math.round(overallPct * 10.0) / 10.0,
                    presentCount,
                    partialAbsentStudents,
                    fullAbsentStudents,
                    totalRecords,
                    distribution,
                    trend,
                    Collections.emptyList(),
                    weeklySummary,
                    sectionAttendance);

        } catch (Exception e) {
            e.printStackTrace();
            return new HodAttendanceAnalyticsDTO(
                    0.0, 0, 0, 0, 0,
                    new AttendanceDistributionDTO(0.0, 0.0, 0.0),
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
    }

    private HodXpAnalyticsDTO calculateXpAnalytics(Long deptId, List<Long> studentIds, List<Student> filteredStudents) {
        if (studentIds.isEmpty()) {
            return new HodXpAnalyticsDTO(
                    0, 0, 0, 0,
                    Collections.emptyList(), new AwardVsPenaltyDTO(0, 0, 0, 0),
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }

        try {
            long totalNetXp = filteredStudents.stream().mapToLong(Student::getTotalXp).sum();

            // Query XP transactions aggregated
            String xpSql = "SELECT " +
                    "  COALESCE(SUM(CASE WHEN is_penalty = false THEN xp_points ELSE 0 END), 0) as award_xp, " +
                    "  COALESCE(SUM(CASE WHEN is_penalty = true THEN xp_points ELSE 0 END), 0) as penalty_xp, " +
                    "  COALESCE(SUM(CASE WHEN is_penalty = false THEN 1 ELSE 0 END), 0) as award_count, " +
                    "  COALESCE(SUM(CASE WHEN is_penalty = true THEN 1 ELSE 0 END), 0) as penalty_count " +
                    "FROM xp_transactions xt " +
                    "WHERE xt.student_id IN (:studentIds) AND xt.status = 'APPROVED'";

            Query xpQuery = entityManager.createNativeQuery(xpSql);
            xpQuery.setParameter("studentIds", studentIds);

            Object[] xpRow = (Object[]) xpQuery.getSingleResult();
            long awardXp = xpRow != null && xpRow[0] != null ? ((Number) xpRow[0]).longValue() : 0;
            long penaltyXp = xpRow != null && xpRow[1] != null ? ((Number) xpRow[1]).longValue() : 0;
            long awardCount = xpRow != null && xpRow[2] != null ? ((Number) xpRow[2]).longValue() : 0;
            long penaltyCount = xpRow != null && xpRow[3] != null ? ((Number) xpRow[3]).longValue() : 0;
            long netXp = awardXp - penaltyXp;

            // Monthly Trend
            String monthlySql = "SELECT " +
                    "  DATE_FORMAT(xt.submitted_at, '%Y-%m') as ym, " +
                    "  COALESCE(SUM(CASE WHEN is_penalty = false THEN xp_points ELSE 0 END), 0) as m_award, " +
                    "  COALESCE(SUM(CASE WHEN is_penalty = true THEN xp_points ELSE 0 END), 0) as m_penalty " +
                    "FROM xp_transactions xt " +
                    "WHERE xt.student_id IN (:studentIds) AND xt.status = 'APPROVED' " +
                    "GROUP BY ym " +
                    "ORDER BY ym ASC";

            Query monthlyQuery = entityManager.createNativeQuery(monthlySql);
            monthlyQuery.setParameter("studentIds", studentIds);

            @SuppressWarnings("unchecked")
            List<Object[]> monthlyRows = monthlyQuery.getResultList();
            List<MonthlyXpTrendDTO> monthlyTrend = new ArrayList<>();
            List<XpHeatmapItemDTO> xpHeatmap = new ArrayList<>();

            for (Object[] r : monthlyRows) {
                String ym = (String) r[0];
                long aXp = r[1] != null ? ((Number) r[1]).longValue() : 0;
                long pXp = r[2] != null ? ((Number) r[2]).longValue() : 0;
                monthlyTrend.add(new MonthlyXpTrendDTO(ym, aXp, pXp, aXp - pXp));
                xpHeatmap.add(new XpHeatmapItemDTO(ym, 1, aXp));
            }

            if (monthlyTrend.isEmpty()) {
                String currentYm = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                monthlyTrend.add(new MonthlyXpTrendDTO(currentYm, awardXp, penaltyXp, netXp));
                xpHeatmap.add(new XpHeatmapItemDTO(currentYm, 1, netXp));
            }

            AwardVsPenaltyDTO awardVsPenalty = new AwardVsPenaltyDTO(awardCount, penaltyCount, awardXp, penaltyXp);

            // Top and Lowest Students
            List<Student> sortedStudents = new ArrayList<>(filteredStudents);
            sortedStudents.sort(Comparator.comparingInt(Student::getTotalXp).reversed());

            List<StudentXpSummaryDTO> topStudents = sortedStudents.stream().limit(5)
                    .map(s -> new StudentXpSummaryDTO(
                            s.getId(), s.getFullName(), s.getRegNo(),
                            s.getSection() != null ? s.getSection().getSectionName() : "N/A",
                            s.getTotalXp(), Math.min(100, Math.max(0, s.getScore()))))
                    .collect(Collectors.toList());

            List<StudentXpSummaryDTO> lowestStudents = sortedStudents.stream()
                    .sorted(Comparator.comparingInt(Student::getTotalXp))
                    .limit(5)
                    .map(s -> new StudentXpSummaryDTO(
                            s.getId(), s.getFullName(), s.getRegNo(),
                            s.getSection() != null ? s.getSection().getSectionName() : "N/A",
                            s.getTotalXp(), Math.min(100, Math.max(0, s.getScore()))))
                    .collect(Collectors.toList());

            return new HodXpAnalyticsDTO(
                    totalNetXp, awardXp, penaltyXp, netXp,
                    monthlyTrend, awardVsPenalty, xpHeatmap, topStudents, lowestStudents);

        } catch (Exception e) {
            e.printStackTrace();
            return new HodXpAnalyticsDTO(
                    0, 0, 0, 0,
                    Collections.emptyList(), new AwardVsPenaltyDTO(0, 0, 0, 0),
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
    }

    private HodDisciplineAnalyticsDTO calculateDisciplineAnalytics(Long deptId, List<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return new HodDisciplineAnalyticsDTO(0, 0, 0, 0);
        }

        try {
            // Query discipline logs count from discipline_logs entity table
            String logSql = "SELECT " +
                    "  COUNT(*) as total_cases, " +
                    "  COALESCE(SUM(CASE WHEN points > 0 THEN 1 ELSE 0 END), 0) as pos_count, " +
                    "  COALESCE(SUM(CASE WHEN points < 0 THEN 1 ELSE 0 END), 0) as neg_count, " +
                    "  COALESCE(SUM(CASE WHEN points = 0 OR LOWER(remarks) LIKE '%warn%' THEN 1 ELSE 0 END), 0) as warn_count "
                    +
                    "FROM discipline_logs dl " +
                    "WHERE dl.student_id IN (:studentIds)";

            Query logQuery = entityManager.createNativeQuery(logSql);
            logQuery.setParameter("studentIds", studentIds);

            Object[] logRow = (Object[]) logQuery.getSingleResult();
            long totalCases = logRow != null && logRow[0] != null ? ((Number) logRow[0]).longValue() : 0;
            long posCount = logRow != null && logRow[1] != null ? ((Number) logRow[1]).longValue() : 0;
            long negCount = logRow != null && logRow[2] != null ? ((Number) logRow[2]).longValue() : 0;
            long warnCount = logRow != null && logRow[3] != null ? ((Number) logRow[3]).longValue() : 0;

            // Penalty Requests from penalty_requests entity table
            String penaltySql = "SELECT COUNT(*) FROM penalty_requests pr WHERE pr.student_id IN (:studentIds)";
            Query pQuery = entityManager.createNativeQuery(penaltySql);
            pQuery.setParameter("studentIds", studentIds);
            Number pCount = (Number) pQuery.getSingleResult();
            long penaltiesTotal = negCount + (pCount != null ? pCount.longValue() : 0);

            return new HodDisciplineAnalyticsDTO(totalCases + (pCount != null ? pCount.longValue() : 0), posCount,
                    penaltiesTotal, warnCount);
        } catch (Exception e) {
            e.printStackTrace();
            return new HodDisciplineAnalyticsDTO(0, 0, 0, 0);
        }
    }

    private List<LeaderboardStudentDTO> calculateLeaderboard(List<Student> filteredStudents) {
        List<Student> sorted = new ArrayList<>(filteredStudents);
        sorted.sort(Comparator.comparingInt(Student::getTotalXp).reversed());

        List<LeaderboardStudentDTO> list = new ArrayList<>();
        int rank = 1;
        for (Student s : sorted) {
            if (rank > 10)
                break;
            list.add(new LeaderboardStudentDTO(
                    rank++,
                    s.getId(),
                    s.getFullName(),
                    s.getRegNo(),
                    s.getSection() != null ? s.getSection().getSectionName() : "N/A",
                    s.getTotalXp(),
                    Math.min(100, Math.max(0, s.getScore())),
                    s.getStage()));
        }
        return list;
    }

    private List<SectionComparisonDTO> calculateSectionComparison(List<Section> deptSections,
            List<Student> filteredStudents) {
        List<SectionComparisonDTO> list = new ArrayList<>();

        for (Section sec : deptSections) {
            List<Student> secStudents = filteredStudents.stream()
                    .filter(s -> s.getSection() != null && s.getSection().getId().equals(sec.getId()))
                    .collect(Collectors.toList());

            int studentCount = secStudents.size();
            double avgXp = secStudents.isEmpty() ? 0.0
                    : secStudents.stream().mapToDouble(Student::getTotalXp).average().orElse(0.0);
            double avgScore = secStudents.isEmpty() ? 100.0
                    : secStudents.stream().mapToDouble(Student::getScore).average().orElse(100.0);

            // Attendance % for this section
            double attendancePct = 0.0;
            if (!secStudents.isEmpty()) {
                List<Long> secStudentIds = secStudents.stream().map(Student::getId).collect(Collectors.toList());
                try {
                    String attSql = "SELECT " +
                            "  CAST((SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) * 100.0) / NULLIF(COUNT(ar.id), 0) AS DECIMAL(5,2)) "
                            +
                            "FROM attendance ar " +
                            "WHERE ar.student_id IN (:secStudentIds)";
                    Query q = entityManager.createNativeQuery(attSql);
                    q.setParameter("secStudentIds", secStudentIds);
                    Object res = q.getSingleResult();
                    if (res != null) {
                        attendancePct = ((BigDecimal) res).doubleValue();
                    }
                } catch (Exception ignored) {
                }
            }

            list.add(new SectionComparisonDTO(
                    sec.getId(),
                    sec.getSectionName(),
                    studentCount,
                    Math.round(avgXp * 10.0) / 10.0,
                    Math.round(attendancePct * 10.0) / 10.0,
                    Math.round(Math.min(100.0, Math.max(0.0, avgScore)) * 10.0) / 10.0));
        }

        return list;
    }

    private List<RecentPenaltyDTO> calculateRecentPenalties(Long deptId, List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecentPenaltyDTO> list = new ArrayList<>();

        try {
            // 1. From discipline_logs
            // faculty table has no full_name – name is in users via faculty.user_id
            // dl.recorded_by_id already points to users table directly
            String dlSql = "SELECT " +
                    "  dl.id, dl.student_id, dl.points, dl.reason, " +
                    "  COALESCE(dl.incident_date, dl.created_at) as p_date, " +
                    "  COALESCE(u.full_name, 'Faculty') as staff_name, " +
                    "  s.full_name, s.reg_no, sec.section_name " +
                    "FROM discipline_logs dl " +
                    "LEFT JOIN students s ON dl.student_id = s.id " +
                    "LEFT JOIN section sec ON s.section_id = sec.id " +
                    "LEFT JOIN users u ON dl.recorded_by_id = u.id " +
                    "WHERE dl.student_id IN (:studentIds) AND dl.points < 0 " +
                    "ORDER BY p_date DESC LIMIT 30";

            Query dlQuery = entityManager.createNativeQuery(dlSql);
            dlQuery.setParameter("studentIds", studentIds);

            @SuppressWarnings("unchecked")
            List<Object[]> dlRows = dlQuery.getResultList();
            for (Object[] r : dlRows) {
                Long id = r[0] != null ? ((Number) r[0]).longValue() : 0L;
                Long sId = r[1] != null ? ((Number) r[1]).longValue() : 0L;
                int points = r[2] != null ? Math.abs(((Number) r[2]).intValue()) : 0;
                String reason = r[3] != null ? (String) r[3] : "Discipline Penalty";
                String pDate = r[4] != null ? r[4].toString() : "";
                String staff = r[5] != null ? (String) r[5] : "Faculty";
                String sName = r[6] != null ? (String) r[6] : "Student";
                String regNo = r[7] != null ? (String) r[7] : "-";
                String secName = r[8] != null ? (String) r[8] : "-";

                list.add(new RecentPenaltyDTO(id, sId, sName, regNo, secName, reason, points, pDate, staff));
            }

            // 2. From penalty_requests
            String prSql = "SELECT " +
                    "  pr.id, pr.student_id, pr.penalty_xp, pr.reason, " +
                    "  pr.created_at, " +
                    "  COALESCE(pr.teacher_name, u.full_name, 'Teacher') as staff_name, " +
                    "  s.full_name, s.reg_no, sec.section_name " +
                    "FROM penalty_requests pr " +
                    "LEFT JOIN students s ON pr.student_id = s.id " +
                    "LEFT JOIN section sec ON s.section_id = sec.id " +
                    "LEFT JOIN users u ON pr.teacher_id = u.id " +
                    "WHERE pr.student_id IN (:studentIds) " +
                    "ORDER BY pr.created_at DESC LIMIT 30";

            Query prQuery = entityManager.createNativeQuery(prSql);
            prQuery.setParameter("studentIds", studentIds);

            @SuppressWarnings("unchecked")
            List<Object[]> prRows = prQuery.getResultList();
            for (Object[] r : prRows) {
                Long id = r[0] != null ? ((Number) r[0]).longValue() : 0L;
                Long sId = r[1] != null ? ((Number) r[1]).longValue() : 0L;
                int xp = r[2] != null ? Math.abs(((Number) r[2]).intValue()) : 0;
                String reason = r[3] != null ? (String) r[3] : "Penalty Request";
                String pDate = r[4] != null ? r[4].toString() : "";
                String staff = r[5] != null ? (String) r[5] : "Teacher";
                String sName = r[6] != null ? (String) r[6] : "Student";
                String regNo = r[7] != null ? (String) r[7] : "-";
                String secName = r[8] != null ? (String) r[8] : "-";

                list.add(new RecentPenaltyDTO(id, sId, sName, regNo, secName, reason, xp, pDate, staff));
            }

            // Sort newest first
            list.sort((a, b) -> {
                if (a.getPenaltyDate() == null)
                    return 1;
                if (b.getPenaltyDate() == null)
                    return -1;
                return b.getPenaltyDate().compareTo(a.getPenaltyDate());
            });

            if (list.size() > 50) {
                return list.subList(0, 50);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}
