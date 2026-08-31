package jjcet.PragatiX.modules.analytics.repository.impl;

import jjcet.PragatiX.modules.analytics.dto.*;
import jjcet.PragatiX.modules.analytics.repository.AnalyticsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

@Repository
public class AnalyticsRepositoryImpl implements AnalyticsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    // Helper class to hold date range
    private static class DateRange {
        private final LocalDate start;
        private final LocalDate end;
        DateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }
        LocalDate start() { return start; }
        LocalDate end() { return end; }
    }

    /**
     * Computes the effective date range based on yearNo, semester, startDate, and endDate.
     * If startDate and endDate are provided, they are used.
     * Otherwise, if yearNo is provided, the academic year is used.
     * If semester is provided, the range is adjusted to that semester (assuming semester 1: Jan-Jun, semester 2: Jul-Dec).
     * This is a simplified implementation; adjust as per academic calendar.
     */
    private DateRange getDateRange(String yearNo, Integer semester, LocalDate startDate, LocalDate endDate) {
        LocalDate effectiveStart = startDate;
        LocalDate effectiveEnd = endDate;

        if (effectiveStart == null && effectiveEnd == null) {
            if (yearNo != null && !yearNo.isEmpty()) {
                int year = Integer.parseInt(yearNo);
                // Assuming academic year starts in January and ends in December
                effectiveStart = LocalDate.of(year, 1, 1);
                effectiveEnd = LocalDate.of(year, 12, 31);
                if (semester != null) {
                    if (semester == 1) {
                        effectiveEnd = LocalDate.of(year, 6, 30);
                    } else if (semester == 2) {
                        effectiveStart = LocalDate.of(year, 7, 1);
                    }
                }
            } else {
                // Default to a wide range if no year provided
                effectiveStart = LocalDate.of(2000, 1, 1);
                effectiveEnd = LocalDate.of(2030, 12, 31);
            }
        }
        // If only one of startDate or endDate is null, we keep the other as is and let the query handle it?
        // But for safety, we set defaults for the missing one if the other is provided.
        if (effectiveStart == null) {
            effectiveStart = LocalDate.of(2000, 1, 1);
        }
        if (effectiveEnd == null) {
            effectiveEnd = LocalDate.of(2030, 12, 31);
        }
        return new DateRange(effectiveStart, effectiveEnd);
    }

    private static Double calculateGrowthRate(Double currentValue, Double previousValue) {
        if (currentValue == null) return null;
        if (previousValue == null || previousValue == 0.0) return null;
        return roundDouble(((currentValue - previousValue) / previousValue) * 100.0);
    }

    private static Double roundDouble(Double value) {
        if (value == null) return null;
        return Math.round(value * 100.0) / 100.0;
    }

    private Map<String, Double> getPeriodAttendanceRates(String yearNo, Integer semester, LocalDate startDate, LocalDate endDate) {
        DateRange range = getDateRange(yearNo, semester, startDate, endDate);
        LocalDate rangeStart = range.start();
        LocalDate rangeEnd = range.end();

        String sql = """
            SELECT
                DATE_FORMAT(a.attendance_date, '%Y-%m') AS period,
                SUM(CASE WHEN a.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(a.id), 0) AS attendanceRate
            FROM attendance a
            JOIN students s ON a.student_id = s.id
            WHERE (:yearNo IS NULL OR YEAR(a.attendance_date) = CAST(:yearNo AS UNSIGNED))
                AND a.attendance_date >= :startDate
                AND a.attendance_date <= :endDate
            GROUP BY DATE_FORMAT(a.attendance_date, '%Y-%m')
            ORDER BY period
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("startDate", rangeStart);
        query.setParameter("endDate", rangeEnd);

        List<Object[]> results = query.getResultList();
        Map<String, Double> map = new java.util.LinkedHashMap<>();
        for (Object[] row : results) {
            String period = (String) row[0];
            Double rate = ((Number) row[1]).doubleValue();
            map.put(period, rate);
        }
        return map;
    }

    @Override
    public List<InstitutionGrowthDto> getInstitutionGrowth(String yearNo, Integer semester, LocalDate startDate, LocalDate endDate) {
        DateRange range = getDateRange(yearNo, semester, startDate, endDate);
        LocalDate rangeStart = range.start();
        LocalDate rangeEnd = range.end();

        String sql = """
            SELECT
                DATE_FORMAT(x.submitted_at, '%Y-%m') AS period,
                SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS totalXp,
                COUNT(DISTINCT x.student_id) AS activeStudents,
                AVG(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS avgXpPerStudent
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            
            WHERE x.status = 'APPROVED'
                AND (:yearNo IS NULL OR YEAR(x.submitted_at) = CAST(:yearNo AS UNSIGNED) OR :yearNo IS NULL OR YEAR(CURDATE()) = CAST(:yearNo AS UNSIGNED))
                AND x.submitted_at >= :startDate
                AND x.submitted_at < :endDatePlusOne
            GROUP BY DATE_FORMAT(x.submitted_at, '%Y-%m')
            ORDER BY period
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("startDate", rangeStart);
        query.setParameter("endDatePlusOne", rangeEnd.plusDays(1));

        List<Object[]> results = query.getResultList();
        List<InstitutionGrowthDto> growthList = new ArrayList<>();

        // Calculate period-over-period growth rates
        Double prevTotalXp = null;
        Double prevActiveStudents = null;
        Double prevAvgXp = null;
        Double prevAttendanceRate = null;

        for (Object[] row : results) {
            String period = (String) row[0];
            Double totalXp = ((Number) row[1]).doubleValue();
            Long activeStudents = ((Number) row[2]).longValue();
            Double avgXpPerStudent = ((Number) row[3]).doubleValue();

            // overallGrowth = growth rate of activeStudents (period-over-period)
            Double overallGrowth = calculateGrowthRate(activeStudents.doubleValue(), prevActiveStudents);
            // xpGrowth = growth rate of totalXp (period-over-period)
            Double xpGrowth = calculateGrowthRate(totalXp, prevTotalXp);
            // attendance = growth rate of attendance rate (period-over-period, fetched separately below)
            Double attendance = null;
            // engagement = growth rate of avgXpPerStudent (period-over-period)
            Double engagement = calculateGrowthRate(avgXpPerStudent, prevAvgXp);

            Integer stage = 0; // not applicable for institution-wide

            growthList.add(new InstitutionGrowthDto(period, overallGrowth, xpGrowth, attendance, engagement, stage));

            prevTotalXp = totalXp;
            prevActiveStudents = activeStudents.doubleValue();
            prevAvgXp = avgXpPerStudent;
        }

        // Fill in attendance growth rate by querying attendance trend for the same periods
        if (!growthList.isEmpty()) {
            Map<String, Double> periodAttendanceRate = getPeriodAttendanceRates(yearNo, semester, startDate, endDate);
            Double prevRate = null;
            int idx = 0;
            for (InstitutionGrowthDto dto : growthList) {
                Double rate = periodAttendanceRate.get(dto.period());
                Double attGrowth = rate != null ? calculateGrowthRate(rate, prevRate) : null;
                growthList.set(idx, new InstitutionGrowthDto(
                    dto.period(), dto.overallGrowth(), dto.xpGrowth(), attGrowth, dto.engagement(), dto.stage()
                ));
                if (rate != null) prevRate = rate;
                idx++;
            }
        }

        return growthList;
    }

    @Override
    public List<DepartmentGrowthDto> getDepartmentGrowth(String yearNo, Integer semester, LocalDate startDate, LocalDate endDate) {
        DateRange range = getDateRange(yearNo, semester, startDate, endDate);
        LocalDate rangeStart = range.start();
        LocalDate rangeEnd = range.end();

        String sql = """
            SELECT
                d.id AS departmentId,
                d.name AS departmentName,
                SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS totalXp
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            JOIN departments d ON s.department_id = d.id
            
            WHERE x.status = 'APPROVED'
                AND (:yearNo IS NULL OR YEAR(x.submitted_at) = CAST(:yearNo AS UNSIGNED) OR :yearNo IS NULL OR YEAR(CURDATE()) = CAST(:yearNo AS UNSIGNED))
                AND x.submitted_at >= :startDate
                AND x.submitted_at < :endDatePlusOne
            GROUP BY d.id, d.name
            ORDER BY totalXp DESC
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("startDate", rangeStart);
        query.setParameter("endDatePlusOne", rangeEnd.plusDays(1));

        List<Object[]> results = query.getResultList();
        List<DepartmentGrowthDto> growthList = new ArrayList<>();

        if (!results.isEmpty()) {
            // Find the maximum totalXp to calculate growth rate
            Double maxXp = 0.0;
            for (Object[] row : results) {
                Double xp = ((Number) row[2]).doubleValue();
                if (xp > maxXp) {
                    maxXp = xp;
                }
            }

            for (Object[] row : results) {
                Long departmentId = ((Number) row[0]).longValue();
                String departmentName = (String) row[1];
                Double totalXp = ((Number) row[2]).doubleValue();
                Double growthRate = (maxXp > 0) ? (totalXp / maxXp * 100.0) : 0.0;

                growthList.add(new DepartmentGrowthDto(departmentId, departmentName, growthRate, totalXp.longValue()));
            }
        }

        return growthList;
    }

    @Override
    public List<XpCurveDto> getXpCurve(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate) {
        DateRange range = getDateRange(yearNo, null, startDate, endDate);
        LocalDate rangeStart = range.start();
        LocalDate rangeEnd = range.end();

        String sql = """
            SELECT
                DATE_FORMAT(x.submitted_at, '%Y-%m-%d') AS period,
                SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS dailyXp
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            
            WHERE x.status = 'APPROVED'
                AND (:yearNo IS NULL OR YEAR(x.submitted_at) = CAST(:yearNo AS UNSIGNED) OR :yearNo IS NULL OR YEAR(CURDATE()) = CAST(:yearNo AS UNSIGNED))
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.stage = :stage)
                AND x.submitted_at >= :startDate
                AND x.submitted_at < :endDatePlusOne
            GROUP BY DATE_FORMAT(x.submitted_at, '%Y-%m-%d')
            ORDER BY period
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("departmentId", departmentId);
        query.setParameter("stage", stage);
        query.setParameter("startDate", rangeStart);
        query.setParameter("endDatePlusOne", rangeEnd.plusDays(1));

        List<Object[]> results = query.getResultList();
        List<XpCurveDto> xpCurveList = new ArrayList<>();

        for (Object[] row : results) {
            String period = (String) row[0];
            Double xp = ((Number) row[1]).doubleValue();
            xpCurveList.add(new XpCurveDto(period, xp));
        }

        return xpCurveList;
    }

    @Override
    public List<StageDistributionDto> getStageDistribution(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate) {
        DateRange range = getDateRange(yearNo, null, startDate, endDate);
        LocalDate rangeStart = range.start();
        LocalDate rangeEnd = range.end();

        String sql = """
            SELECT
                s.stage AS stageName,
                COUNT(s.id) AS studentCount
            FROM students s
            
            WHERE (:yearNo IS NULL OR YEAR(CURDATE()) = CAST(:yearNo AS UNSIGNED))
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.stage = :stage)
            GROUP BY s.stage
            ORDER BY s.stage
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("departmentId", departmentId);
        query.setParameter("stage", stage);

        List<Object[]> results = query.getResultList();
        List<StageDistributionDto> distributionList = new ArrayList<>();

        Long totalStudents = 0L;
        for (Object[] row : results) {
            totalStudents += ((Number) row[1]).longValue();
        }

        for (Object[] row : results) {
            String stageName = row[0] != null ? String.valueOf(((Number) row[0]).intValue()) : "Unknown";
            Long studentCount = ((Number) row[1]).longValue();
            Double percentage = (totalStudents > 0) ? (studentCount.doubleValue() / totalStudents * 100.0) : 0.0;

            distributionList.add(new StageDistributionDto(stageName, studentCount, percentage));
        }

        return distributionList;
    }

    @Override
    public List<AttendanceDto> getAttendanceTrend(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period) {
        // Note: period is not used in this simplified implementation; it would determine the grouping (daily, weekly, monthly)
        // For simplicity, we'll use daily grouping.
        DateRange range = getDateRange(yearNo, null, startDate, endDate);
        LocalDate rangeStart = range.start();
        LocalDate rangeEnd = range.end();

        String sql = """
            SELECT 
                a.attendance_date AS date,
                SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS presentCount,
                SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absentCount,
                SUM(CASE WHEN a.status = 'OD' THEN 1 ELSE 0 END) AS odCount,
                SUM(CASE WHEN a.status = 'LEAVE' THEN 1 ELSE 0 END) AS partialCount,
                COUNT(a.id) AS totalRecords
            FROM attendance a
            JOIN students s ON a.student_id = s.id
            
            WHERE (:yearNo IS NULL OR YEAR(CURDATE()) = CAST(:yearNo AS UNSIGNED))
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
                AND a.attendance_date >= :startDate
                AND a.attendance_date <= :endDate
            GROUP BY a.attendance_date
            ORDER BY a.attendance_date
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("departmentId", departmentId);
        query.setParameter("stage", stage);
        query.setParameter("sectionId", sectionId);
        query.setParameter("startDate", rangeStart);
        query.setParameter("endDate", rangeEnd);

        List<Object[]> results = query.getResultList();
        List<AttendanceDto> trendList = new ArrayList<>();

        for (Object[] row : results) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long presentCount = ((Number) row[1]).longValue();
            Long absentCount = ((Number) row[2]).longValue();
            Long odCount = ((Number) row[3]).longValue();
            Long partialCount = ((Number) row[4]).longValue();
            Long totalRecords = ((Number) row[5]).longValue();
            Double rate = (totalRecords > 0) ? (presentCount.doubleValue() / totalRecords * 100.0) : 0.0;

            trendList.add(new AttendanceDto(date, presentCount, absentCount, odCount, partialCount, rate));
        }

        return trendList;
    }

    @Override
    public List<AttendanceCalendarDto> getAttendanceCalendar(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period) {
        // Similar to getAttendanceTrend but returns AttendanceCalendarDto
        DateRange range = getDateRange(yearNo, null, startDate, endDate);
        LocalDate rangeStart = range.start();
        LocalDate rangeEnd = range.end();

        String sql = """
            SELECT
                a.attendance_date AS date,
                COUNT(DISTINCT s.id) AS totalStudents,
                SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS presentCount
            FROM attendance a
            JOIN students s ON a.student_id = s.id
            
            WHERE (:yearNo IS NULL OR YEAR(CURDATE()) = CAST(:yearNo AS UNSIGNED))
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
                AND a.attendance_date >= :startDate
                AND a.attendance_date <= :endDate
            GROUP BY a.attendance_date
            ORDER BY a.attendance_date
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("departmentId", departmentId);
        query.setParameter("stage", stage);
        query.setParameter("sectionId", sectionId);
        query.setParameter("startDate", rangeStart);
        query.setParameter("endDate", rangeEnd);

        List<Object[]> results = query.getResultList();
        List<AttendanceCalendarDto> calendarList = new ArrayList<>();

        for (Object[] row : results) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long totalStudents = ((Number) row[1]).longValue();
            Long presentCount = ((Number) row[2]).longValue();
            Double attendanceRate = (totalStudents > 0) ? (presentCount.doubleValue() / totalStudents * 100.0) : 0.0;

            calendarList.add(new AttendanceCalendarDto(date, totalStudents, presentCount, attendanceRate));
        }

        return calendarList;
    }

    @Override
    public List<ActivityFunnelDto> getActivityFunnel(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate) {
        DateRange range = getDateRange(yearNo, null, startDate, endDate);
        LocalDate rangeStart = range.start();
        LocalDate rangeEnd = range.end();

        String sql = """
            SELECT 
                (SELECT COUNT(DISTINCT aa.id) FROM activity_assignments aa WHERE aa.status = 'ACTIVE' AND (:departmentId IS NULL OR aa.department_id = :departmentId)) AS assigned,
                (SELECT COUNT(DISTINCT acr.id) FROM activity_completion_requests acr WHERE acr.status = 'PENDING' AND (:departmentId IS NULL OR EXISTS (SELECT 1 FROM students s WHERE s.id = acr.student_id AND s.department_id = :departmentId))) AS started,
                (SELECT COUNT(DISTINCT acr.id) FROM activity_completion_requests acr WHERE acr.status IN ('PENDING', 'APPROVED', 'REJECTED') AND (:departmentId IS NULL OR EXISTS (SELECT 1 FROM students s WHERE s.id = acr.student_id AND s.department_id = :departmentId))) AS submitted,
                (SELECT COUNT(DISTINCT acr.id) FROM activity_completion_requests acr WHERE acr.status = 'APPROVED' AND (:departmentId IS NULL OR EXISTS (SELECT 1 FROM students s WHERE s.id = acr.student_id AND s.department_id = :departmentId))) AS approved,
                (SELECT COALESCE(SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END), 0) FROM xp_transactions x JOIN students s ON x.student_id = s.id WHERE x.status = 'APPROVED' AND (:departmentId IS NULL OR s.department_id = :departmentId)) AS xpAwarded
            FROM DUAL
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("departmentId", departmentId);

        Object[] result = (Object[]) query.getSingleResult();
        Long assigned = result[0] != null ? ((Number) result[0]).longValue() : 0L;
        Long started = result[1] != null ? ((Number) result[1]).longValue() : 0L;
        Long submitted = result[2] != null ? ((Number) result[2]).longValue() : 0L;
        Long approved = result[3] != null ? ((Number) result[3]).longValue() : 0L;
        Long xpAwarded = result[4] != null ? ((Number) result[4]).longValue() : 0L;

        List<ActivityFunnelDto> funnel = new ArrayList<>();
        if (assigned > 0) {
            funnel.add(new ActivityFunnelDto("Assigned", assigned, 100.0));
            funnel.add(new ActivityFunnelDto("Started", started, (double) started / assigned * 100.0));
            funnel.add(new ActivityFunnelDto("Submitted", submitted, (double) submitted / assigned * 100.0));
            funnel.add(new ActivityFunnelDto("Approved", approved, (double) approved / assigned * 100.0));
        } else {
            funnel.add(new ActivityFunnelDto("Assigned", 0L, 0.0));
            funnel.add(new ActivityFunnelDto("Started", 0L, 0.0));
            funnel.add(new ActivityFunnelDto("Submitted", 0L, 0.0));
            funnel.add(new ActivityFunnelDto("Approved", 0L, 0.0));
        }

        return funnel;
    }

    @Override
    public List<PerformanceLeaderDto> getTopPerformers(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate, Integer limit) {
        DateRange range = getDateRange(yearNo, null, startDate, endDate);
        LocalDate rangeStart = range.start();
        LocalDate rangeEnd = range.end();
        int effectiveLimit = (limit != null && limit > 0) ? limit : 10;

        String sql = """
            SELECT 
                s.id AS studentId,
                CONCAT(s.full_name, ' (', s.reg_no, ')') AS studentName,
                SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS totalXp,
                COALESCE(
                    SUM(CASE WHEN a.id IS NOT NULL AND a.status = 'PRESENT' THEN 1 ELSE 0 END) * 100.0 /
                    NULLIF(SUM(CASE WHEN a.id IS NOT NULL THEN 1 ELSE 0 END), 0), 0
                ) AS attendanceRate
            FROM students s
            
             LEFT JOIN xp_transactions x ON s.id = x.student_id AND x.status = 'APPROVED'
            LEFT JOIN attendance a ON s.id = a.student_id
                AND a.attendance_date BETWEEN :startDate AND :endDate
            WHERE (:yearNo IS NULL OR YEAR(x.submitted_at) = CAST(:yearNo AS UNSIGNED) OR :yearNo IS NULL OR YEAR(CURDATE()) = CAST(:yearNo AS UNSIGNED))
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.stage = :stage)
            GROUP BY s.id, s.full_name, s.reg_no
            ORDER BY totalXp DESC, attendanceRate DESC
            LIMIT :limit
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("departmentId", departmentId);
        query.setParameter("stage", stage);
        query.setParameter("startDate", rangeStart);
        query.setParameter("endDate", rangeEnd);
        query.setParameter("limit", effectiveLimit);

        List<Object[]> results = query.getResultList();
        List<PerformanceLeaderDto> leaders = new ArrayList<>();

        for (Object[] row : results) {
            Long studentId = ((Number) row[0]).longValue();
            String studentName = (String) row[1];
            Double xp = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            Double attendance = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            leaders.add(new PerformanceLeaderDto(studentId, studentName, null, xp, attendance));
        }

        return leaders;
    }

    @Override
    public List<MoverDto> getMostImproved(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate, Integer limit) {
        DateRange range = getDateRange(yearNo, null, startDate, endDate);
        LocalDate rangeStart = range.start();
        LocalDate rangeEnd = range.end();

        // Determine current and previous period for improvement calculation
        // If a specific year is provided, current = that year, previous = year-1
        // If no year, use the effective range as current and prior 6-month period as previous
        String currentYearNo = yearNo;
        String previousYearNo = null;
        if (yearNo != null && yearNo.length() == 4) {
            int currentYear = Integer.parseInt(yearNo);
            previousYearNo = String.valueOf(currentYear - 1);
        }

        String sql = """
            SELECT 
                s.id AS studentId,
                CONCAT(s.full_name, ' (', s.reg_no, ')') AS studentName,
                -- Current period total XP
                CURRENT_XP.total_xp AS currentXp,
                -- Previous period total XP  
                PREVIOUS_XP.total_xp AS previousXp,
                -- Improvement: current - previous
                (COALESCE(CURRENT_XP.total_xp, 0) - COALESCE(PREVIOUS_XP.total_xp, 0)) AS improvement
            FROM students s
            
            LEFT JOIN (
                SELECT x.student_id AS student_id,
                       SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS total_xp
                FROM xp_transactions x
                JOIN students s2 ON x.student_id = s2.id
                JOIN years y2 ON s2.year_id = y2.id
                WHERE x.status = 'APPROVED'
                    AND (:yearNo IS NULL OR y2.year_no = :yearNo OR CHAR_LENGTH(:yearNo) = 4)
                    AND x.submitted_at >= :startDate
                    AND x.submitted_at < :endDatePlusOne
                GROUP BY x.student_id
            ) CURRENT_XP ON CURRENT_XP.student_id = s.id
            LEFT JOIN (
                SELECT x.student_id AS student_id,
                       SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS total_xp
                FROM xp_transactions x
                JOIN students s3 ON x.student_id = s3.id
                JOIN years y3 ON s3.year_id = y3.id
                WHERE x.status = 'APPROVED'
                    AND (:previousYearNo IS NULL OR y3.year_no = :previousYearNo OR CHAR_LENGTH(:previousYearNo) = 4)
                    AND x.submitted_at >= :startDate
                    AND x.submitted_at < :endDatePlusOne
                GROUP BY x.student_id
) PREVIOUS_XP ON PREVIOUS_XP.student_id = s.id
             WHERE (:yearNo IS NULL OR YEAR(CURDATE()) = CAST(:yearNo AS UNSIGNED))
                 AND (:departmentId IS NULL OR s.department_id = :departmentId)
                 AND (:stage IS NULL OR s.stage = :stage)
             ORDER BY improvement DESC
             LIMIT :limit
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("previousYearNo", previousYearNo);
        query.setParameter("departmentId", departmentId);
        query.setParameter("stage", stage);
        query.setParameter("startDate", rangeStart);
        query.setParameter("endDatePlusOne", rangeEnd.plusDays(1));
        query.setParameter("limit", limit);

        List<Object[]> results = query.getResultList();
        List<MoverDto> movers = new ArrayList<>();

        for (Object[] row : results) {
            Long studentId = ((Number) row[0]).longValue();
            String studentName = (String) row[1];
            Double currentXp = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            Double previousXp = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            Double improvement = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;

            // Calculate improvement percentage relative to previous period
            Double improvementPercent = (previousXp != null && previousXp != 0)
                    ? ((currentXp - previousXp) / previousXp) * 100.0
                    : null;

            movers.add(new MoverDto(studentId, studentName, null, improvementPercent));
        }

        return movers;
    }

@Override
    public List<Object[]> getInterventionRisks(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate) {
        DateRange range = getDateRange(yearNo, null, startDate, endDate);
        LocalDate rangeStart = range.start();
        LocalDate rangeEnd = range.end();

        String sql = """
            SELECT
                s.id AS studentId,
                COALESCE(
                    (SUM(CASE WHEN a.id IS NOT NULL AND a.status = 'PRESENT' THEN 1 ELSE 0 END) * 100.0) /
                    NULLIF(SUM(CASE WHEN a.id IS NOT NULL THEN 1 ELSE 0 END), 0), 0
                ) AS attendancePercentage,
                DATEDIFF(NOW(), MAX(a.attendance_date)) AS daysSinceLastAttendance,
                COALESCE(SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE 0 END), 0) AS totalXpEarned,
                COALESCE(SUM(CASE WHEN x.is_penalty = TRUE THEN x.xp_points ELSE 0 END), 0) AS totalPenaltyXp
            FROM students s
            
            LEFT JOIN attendance a ON s.id = a.student_id
                AND a.attendance_date >= :startDate
                AND a.attendance_date <= :endDate
            LEFT JOIN xp_transactions x ON s.id = x.student_id
                 AND x.status = 'APPROVED'
                 AND x.submitted_at >= :startDate
                 AND x.submitted_at <= :endDate
            WHERE (:yearNo IS NULL OR YEAR(CURDATE()) = CAST(:yearNo AS UNSIGNED))
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.stage = :stage)
            GROUP BY s.id, s.full_name, s.reg_no
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("departmentId", departmentId);
        query.setParameter("stage", stage);
        query.setParameter("startDate", rangeStart);
        query.setParameter("endDate", rangeEnd);

        return query.getResultList();
    }
}