package jjcet.PragatiX.modules.analytics.infrastructure.repository;

import jjcet.PragatiX.modules.analytics.api.dto.request.AnalyticsFilter;
import jjcet.PragatiX.modules.analytics.api.dto.request.AttendanceFilter;
import jjcet.PragatiX.modules.analytics.api.dto.request.FunnelFilter;
import jjcet.PragatiX.modules.analytics.api.dto.request.XpFilter;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AnalyticsOverviewDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceCalendarDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceDistributionDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceExportDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceSummaryDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceSummaryRowDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceTrendDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.GroupedAttendanceDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.LowAttendanceStudentDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.funnel.ActivityFunnelDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.overview.DepartmentGrowthDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.overview.InstitutionGrowthDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.overview.StageDistributionDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.performance.MostImprovedDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.performance.PerformanceLeaderDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.risk.RiskProfileDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.risk.RiskSignalDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.ActivityXpContributionDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.DepartmentXpAttendanceDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.EliteTeamDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.GroupedXpDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.LowXpStudentDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.TopPerformerDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpAwardVsPenaltyDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpCurveDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpDistributionDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpHeatmapDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpHistoryDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpTopPerformerDto;
import jjcet.PragatiX.modules.analytics.domain.service.risk.RiskClassificationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AnalyticsViewRepositoryImpl implements AnalyticsViewRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final RiskClassificationService riskClassificationService;

    public AnalyticsViewRepositoryImpl(RiskClassificationService riskClassificationService) {
        this.riskClassificationService = riskClassificationService;
    }

    private static class DateRange {
        final LocalDate start;
        final LocalDate end;
        DateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }
    }

    private DateRange resolveDateRange(String yearNo, LocalDate startDate, LocalDate endDate) {
        LocalDate effectiveStart = startDate;
        LocalDate effectiveEnd = endDate;
        if (effectiveStart == null && effectiveEnd == null) {
            if (yearNo != null && !yearNo.isEmpty() && yearNo.length() == 4) {
                int year = Integer.parseInt(yearNo);
                effectiveStart = LocalDate.of(year, 1, 1);
                effectiveEnd = LocalDate.of(year, 12, 31);
            } else {
                effectiveStart = LocalDate.of(2000, 1, 1);
                effectiveEnd = LocalDate.of(2030, 12, 31);
            }
        }
        if (effectiveStart == null) effectiveStart = LocalDate.of(2000, 1, 1);
        if (effectiveEnd == null) effectiveEnd = LocalDate.of(2030, 12, 31);
        return new DateRange(effectiveStart, effectiveEnd);
    }

    private String yearPredicate() {
        return "(:yearNo IS NULL OR y.year_no = :yearNo)";
    }

    private String yearPredicateXp() {
        return "(:yearNo IS NULL OR y.year_no = :yearNo)";
    }

    private String joinYearFilter() {
        return "JOIN years y ON s.year_id = y.id";
    }

    private String joinYearFilterXp() {
        return "LEFT JOIN years y ON s.year_id = y.id";
    }

    private void setYearParam(Query q, String yearNo) {
        q.setParameter("yearNo", yearNo);
    }

    private void setYearParamOrEmpty(Query q, String yearNo) {
        q.setParameter("yearNo", yearNo != null ? yearNo : "");
    }

    private static Double roundDouble(Double v) {
        return v == null ? null : Math.round(v * 100.0) / 100.0;
    }

    private static Double calculateGrowthRate(Double current, Double previous) {
        if (current == null) return null;
        if (previous == null || previous == 0.0) return null;
        return roundDouble(((current - previous) / previous) * 100.0);
    }

    @Override
    public List<InstitutionGrowthDto> findInstitutionGrowth(AnalyticsFilter filter) {
        DateRange range = resolveDateRange(filter.yearNo(), filter.startDate(), filter.endDate());

        String sql = """
            SELECT DATE_FORMAT(x.submitted_at, '%%Y-%%m') AS period,
                   SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS totalXp,
                   COUNT(DISTINCT x.student_id) AS activeStudents,
                   AVG(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS avgXpPerStudent
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            WHERE x.status = 'APPROVED'
                AND x.submitted_at >= :startDate
                AND x.submitted_at < :endDatePlusOne
            GROUP BY DATE_FORMAT(x.submitted_at, '%%Y-%%m')
            ORDER BY period
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("startDate", range.start);
        q.setParameter("endDatePlusOne", range.end.plusDays(1));

        List<Object[]> rows = q.getResultList();
        List<InstitutionGrowthDto> result = new ArrayList<>();
        Double prevXp = null;
        Double prevActive = null;
        Double prevAvg = null;
        for (Object[] row : rows) {
            String period = (String) row[0];
            Double totalXp = ((Number) row[1]).doubleValue();
            Long activeStudents = ((Number) row[2]).longValue();
            Double avgXp = ((Number) row[3]).doubleValue();
            Double overallGrowth = calculateGrowthRate(activeStudents.doubleValue(), prevActive);
            Double xpGrowth = calculateGrowthRate(totalXp, prevXp);
            Double engagement = calculateGrowthRate(avgXp, prevAvg);
            result.add(new InstitutionGrowthDto(period, overallGrowth, xpGrowth, null, engagement, 0));
            prevXp = totalXp;
            prevActive = activeStudents.doubleValue();
            prevAvg = avgXp;
        }
        return result;
    }

    @Override
    public List<DepartmentGrowthDto> findDepartmentGrowth(AnalyticsFilter filter) {
        DateRange range = resolveDateRange(filter.yearNo(), filter.startDate(), filter.endDate());

        String sql = """
            SELECT d.id AS departmentId,
                   d.dept_name AS departmentName,
                   SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS totalXp
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            JOIN departments d ON s.department_id = d.id
            WHERE x.status = 'APPROVED'
                AND x.submitted_at >= :startDate
                AND x.submitted_at < :endDatePlusOne
            GROUP BY d.id, d.dept_name
            ORDER BY totalXp DESC
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("startDate", range.start);
        q.setParameter("endDatePlusOne", range.end.plusDays(1));

        List<Object[]> rows = q.getResultList();
        List<DepartmentGrowthDto> result = new ArrayList<>();
        if (rows.isEmpty()) return result;
        double maxXp = 0.0;
        for (Object[] row : rows) {
            double xp = ((Number) row[2]).doubleValue();
            if (xp > maxXp) maxXp = xp;
        }
        for (Object[] row : rows) {
            Long id = ((Number) row[0]).longValue();
            String name = (String) row[1];
            double xp = ((Number) row[2]).doubleValue();
            double growth = maxXp > 0 ? xp / maxXp * 100.0 : 0.0;
            result.add(new DepartmentGrowthDto(id, name, roundDouble(growth), (long) xp));
        }
        return result;
    }

    @Override
    public List<XpCurveDto> findXpCurve(AnalyticsFilter filter) {
        DateRange range = resolveDateRange(filter.yearNo(), filter.startDate(), filter.endDate());

        String sql = """
            SELECT DATE_FORMAT(x.submitted_at, '%%Y-%%m-%%d') AS period,
                   SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS xp
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            WHERE x.status = 'APPROVED'
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND x.submitted_at >= :startDate
                AND x.submitted_at < :endDatePlusOne
            GROUP BY DATE_FORMAT(x.submitted_at, '%%Y-%%m-%%d')
            ORDER BY period
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stage());
        q.setParameter("startDate", range.start);
        q.setParameter("endDatePlusOne", range.end.plusDays(1));

        List<Object[]> rows = q.getResultList();
        List<XpCurveDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new XpCurveDto((String) row[0], ((Number) row[1]).doubleValue()));
        }
        return result;
    }

    @Override
    public List<StageDistributionDto> findStageDistribution(AnalyticsFilter filter) {
        String sql = """
            SELECT s.current_stage AS stageName, COUNT(s.id) AS studentCount
            FROM students s
            WHERE (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND s.active = 1
            GROUP BY s.current_stage
            ORDER BY s.current_stage
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stage());

        List<Object[]> rows = q.getResultList();
        List<StageDistributionDto> result = new ArrayList<>();
        long total = 0;
        for (Object[] row : rows) {
            total += ((Number) row[1]).longValue();
        }
        for (Object[] row : rows) {
            String stageName = row[0] != null ? String.valueOf(((Number) row[0]).intValue()) : "Unknown";
            long count = ((Number) row[1]).longValue();
            double pct = total > 0 ? roundDouble(count * 100.0 / total) : 0.0;
            result.add(new StageDistributionDto(stageName, count, pct));
        }
        return result;
    }

    @Override
    public List<AttendanceDto> findAttendanceTrend(AttendanceFilter filter) {
        DateRange range = resolveDateRange(null, filter.startDate(), filter.endDate());

        String sql = """
            SELECT a.attendance_date AS date,
                   SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS presentCount,
                   SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absentCount,
                   SUM(CASE WHEN a.status = 'OD' THEN 1 ELSE 0 END) AS odCount,
                   SUM(CASE WHEN a.status = 'LEAVE' THEN 1 ELSE 0 END) AS partialCount,
                   COUNT(a.id) AS totalRecords
            FROM attendance a
            JOIN students s ON a.student_id = s.id
            WHERE (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
                AND (:period IS NULL OR a.period_no = :period)
                AND a.attendance_date >= :startDate
                AND a.attendance_date <= :endDate
            GROUP BY a.attendance_date
            ORDER BY a.attendance_date
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stageId());
        q.setParameter("sectionId", filter.sectionId());
        q.setParameter("period", filter.period());
        q.setParameter("startDate", range.start);
        q.setParameter("endDate", range.end);

        List<Object[]> rows = q.getResultList();
        List<AttendanceDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            long present = ((Number) row[1]).longValue();
            long absent = ((Number) row[2]).longValue();
            long od = ((Number) row[3]).longValue();
            long partial = ((Number) row[4]).longValue();
            long total = ((Number) row[5]).longValue();
            double rate = total > 0 ? roundDouble(present * 100.0 / total) : 0.0;
            result.add(new AttendanceDto(date, present, absent, od, partial, rate));
        }
        return result;
    }

    @Override
    public List<AttendanceCalendarDto> findAttendanceCalendar(AttendanceFilter filter) {
        DateRange range = resolveDateRange(null, filter.startDate(), filter.endDate());

        String sql = """
            SELECT a.attendance_date AS date,
                   COUNT(DISTINCT s.id) AS totalStudents,
                   SUM(CASE WHEN a.status = 'PRESENT' OR a.status = 'OD' THEN 1 ELSE 0 END) AS presentCount
            FROM attendance a
            JOIN students s ON a.student_id = s.id
            WHERE (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
                AND (:period IS NULL OR a.period_no = :period)
                AND a.attendance_date >= :startDate
                AND a.attendance_date <= :endDate
            GROUP BY a.attendance_date
            ORDER BY a.attendance_date
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stageId());
        q.setParameter("sectionId", filter.sectionId());
        q.setParameter("period", filter.period());
        q.setParameter("startDate", range.start);
        q.setParameter("endDate", range.end);

        List<Object[]> rows = q.getResultList();
        List<AttendanceCalendarDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            long total = ((Number) row[1]).longValue();
            long present = ((Number) row[2]).longValue();
            double rate = total > 0 ? roundDouble(present * 100.0 / total) : 0.0;
            result.add(new AttendanceCalendarDto(date, total, present, rate));
        }
        return result;
    }

    @Override
    public List<ActivityFunnelDto> findActivityFunnel(FunnelFilter filter) {
        String sql = """
            SELECT
                (SELECT COUNT(DISTINCT aa.id) FROM activity_assignments aa WHERE aa.status = 'ACTIVE'
                    AND (:departmentId IS NULL OR aa.department_id = :departmentId)) AS assigned,
                (SELECT COUNT(DISTINCT acr.id) FROM activity_completion_requests acr WHERE acr.status = 'PENDING'
                    AND (:departmentId IS NULL OR EXISTS (SELECT 1 FROM students s WHERE s.id = acr.student_id AND s.department_id = :departmentId))) AS started,
                (SELECT COUNT(DISTINCT acr.id) FROM activity_completion_requests acr WHERE acr.status IN ('PENDING', 'APPROVED', 'REJECTED')
                    AND (:departmentId IS NULL OR EXISTS (SELECT 1 FROM students s WHERE s.id = acr.student_id AND s.department_id = :departmentId))) AS submitted,
                (SELECT COUNT(DISTINCT acr.id) FROM activity_completion_requests acr WHERE acr.status = 'APPROVED'
                    AND (:departmentId IS NULL OR EXISTS (SELECT 1 FROM students s WHERE s.id = acr.student_id AND s.department_id = :departmentId))) AS approved,
                (SELECT COALESCE(SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END), 0)
                    FROM xp_transactions x JOIN students s ON x.student_id = s.id WHERE x.status = 'APPROVED'
                    AND (:departmentId IS NULL OR s.department_id = :departmentId)) AS xpAwarded
            FROM DUAL
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("departmentId", filter.departmentId());

        Object[] r = (Object[]) q.getSingleResult();
        long assigned = r[0] != null ? ((Number) r[0]).longValue() : 0L;
        long started = r[1] != null ? ((Number) r[1]).longValue() : 0L;
        long submitted = r[2] != null ? ((Number) r[2]).longValue() : 0L;
        long approved = r[3] != null ? ((Number) r[3]).longValue() : 0L;

        List<ActivityFunnelDto> result = new ArrayList<>();
        if (assigned > 0) {
            result.add(new ActivityFunnelDto("Assigned", assigned, 100.0));
            result.add(new ActivityFunnelDto("Started", started, roundDouble(started * 100.0 / assigned)));
            result.add(new ActivityFunnelDto("Submitted", submitted, roundDouble(submitted * 100.0 / assigned)));
            result.add(new ActivityFunnelDto("Approved", approved, roundDouble(approved * 100.0 / assigned)));
        } else {
            result.add(new ActivityFunnelDto("Assigned", 0L, 0.0));
            result.add(new ActivityFunnelDto("Started", 0L, 0.0));
            result.add(new ActivityFunnelDto("Submitted", 0L, 0.0));
            result.add(new ActivityFunnelDto("Approved", 0L, 0.0));
        }
        return result;
    }

    @Override
    public List<RiskProfileDto> findRiskProfiles(AnalyticsFilter filter) {
        DateRange range = resolveDateRange(filter.yearNo(), filter.startDate(), filter.endDate());

        String sql = """
            SELECT s.id AS studentId,
                   COALESCE(
                       (SUM(CASE WHEN a.id IS NOT NULL AND a.status = 'PRESENT' THEN 1 ELSE 0 END) * 100.0) /
                       NULLIF(SUM(CASE WHEN a.id IS NOT NULL THEN 1 ELSE 0 END), 0), 0
                   ) AS attendancePercentage,
                   DATEDIFF(NOW(), MAX(a.attendance_date)) AS daysSinceLastAttendance,
                   COALESCE(SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE 0 END), 0) AS totalXp,
                   COALESCE(SUM(CASE WHEN x.is_penalty = TRUE THEN x.xp_points ELSE 0 END), 0) AS penaltyXp
            FROM students s
            LEFT JOIN attendance a ON s.id = a.student_id
                AND a.attendance_date >= :startDate
                AND a.attendance_date <= :endDate
            LEFT JOIN xp_transactions x ON s.id = x.student_id
                AND x.status = 'APPROVED'
                AND x.submitted_at >= :startDate
                AND x.submitted_at <= :endDate
            WHERE s.active = 1
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
            GROUP BY s.id
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("startDate", range.start);
        q.setParameter("endDate", range.end);
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stage());

        List<Object[]> rows = q.getResultList();
        List<RiskProfileDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long studentId = ((Number) row[0]).longValue();
            Double attendance = row[1] != null ? ((Number) row[1]).doubleValue() : null;
            Long daysSince = row[2] != null ? ((Number) row[2]).longValue() : null;
            Long totalXp = ((Number) row[3]).longValue();
            Long penaltyXp = ((Number) row[4]).longValue();
            result.add(new RiskProfileDto(studentId, null, null, attendance, daysSince, totalXp, penaltyXp));
        }
        return result;
    }

    @Override
    public List<RiskSignalDto> aggregateRiskSignals(List<RiskProfileDto> profiles) {
        long high = 0, medium = 0, low = 0;
        for (RiskProfileDto profile : profiles) {
            switch (riskClassificationService.classify(profile.attendancePercentage())) {
                case HIGH -> high++;
                case MEDIUM -> medium++;
                case LOW -> low++;
            }
        }
        List<RiskSignalDto> result = new ArrayList<>();
        if (high > 0) result.add(new RiskSignalDto("HIGH", high));
        if (medium > 0) result.add(new RiskSignalDto("MEDIUM", medium));
        if (low > 0) result.add(new RiskSignalDto("LOW", low));
        return result;
    }

    @Override
    public List<PerformanceLeaderDto> findTopPerformers(AnalyticsFilter filter, int limit) {
        DateRange range = resolveDateRange(filter.yearNo(), filter.startDate(), filter.endDate());
        int effectiveLimit = limit > 0 ? limit : 10;

        String sql = """
            SELECT s.id AS studentId,
                   s.full_name AS studentName,
                   s.reg_no AS rollNumber,
                   SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS totalXp,
                   COALESCE(
                       SUM(CASE WHEN a.id IS NOT NULL AND a.status = 'PRESENT' THEN 1 ELSE 0 END) * 100.0 /
                       NULLIF(SUM(CASE WHEN a.id IS NOT NULL THEN 1 ELSE 0 END), 0), 0
                   ) AS attendanceRate
            FROM students s
            LEFT JOIN xp_transactions x ON s.id = x.student_id AND x.status = 'APPROVED'
                AND x.submitted_at >= :startDate AND x.submitted_at <= :endDate
            LEFT JOIN attendance a ON s.id = a.student_id
                AND a.attendance_date BETWEEN :startDate AND :endDate
            WHERE s.active = 1
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
            GROUP BY s.id, s.full_name, s.reg_no
            ORDER BY totalXp DESC, attendanceRate DESC
            LIMIT :limit
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("startDate", range.start);
        q.setParameter("endDate", range.end);
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stage());
        q.setParameter("limit", effectiveLimit);

        List<Object[]> rows = q.getResultList();
        List<PerformanceLeaderDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long id = ((Number) row[0]).longValue();
            String name = (String) row[1];
            String roll = row[2] != null ? (String) row[2] : null;
            Double xp = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            Double att = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
            result.add(new PerformanceLeaderDto(id, name, roll, xp, att));
        }
        return result;
    }

    @Override
    public List<MostImprovedDto> findMostImproved(AnalyticsFilter filter, int limit) {
        DateRange range = resolveDateRange(filter.yearNo(), filter.startDate(), filter.endDate());

        String sql = """
            SELECT s.id AS studentId,
                   s.full_name AS studentName,
                   s.reg_no AS rollNumber,
                   COALESCE(SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END), 0) AS improvement
            FROM students s
            LEFT JOIN xp_transactions x ON s.id = x.student_id AND x.status = 'APPROVED'
                AND x.submitted_at >= :startDate AND x.submitted_at <= :endDate
            WHERE s.active = 1
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
            GROUP BY s.id, s.full_name, s.reg_no
            ORDER BY improvement DESC
            LIMIT :limit
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("startDate", range.start);
        q.setParameter("endDate", range.end);
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stage());
        q.setParameter("limit", limit > 0 ? limit : 10);

        List<Object[]> rows = q.getResultList();
        List<MostImprovedDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long id = ((Number) row[0]).longValue();
            String name = (String) row[1];
            String roll = row[2] != null ? (String) row[2] : null;
            Double improvement = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            result.add(new MostImprovedDto(id, name, roll, roundDouble(improvement)));
        }
        return result;
    }

    @Override
    public AnalyticsOverviewDto findAttendanceOverview(AttendanceFilter filter) {
        DateRange range = resolveDateRange(null, filter.startDate(), filter.endDate());

        String sql = """
            WITH StudentAgg AS (
                SELECT ar.student_id,
                       SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) AS present_count,
                       SUM(CASE WHEN ar.status IN ('ABSENT', 'LEAVE') THEN 1 ELSE 0 END) AS absent_count
                FROM attendance ar
                JOIN students s ON ar.student_id = s.id
                WHERE (:departmentId IS NULL OR s.department_id = :departmentId)
                    AND (:stage IS NULL OR s.current_stage = :stage)
                    AND (:sectionId IS NULL OR s.section_id = :sectionId)
                    AND (:period IS NULL OR ar.period_no = :period)
                    AND ar.attendance_date >= :startDate
                    AND ar.attendance_date <= :endDate
                GROUP BY ar.student_id
            )
            SELECT
                CAST((SUM(present_count) * 100.0) / NULLIF(SUM(present_count + absent_count), 0) AS DECIMAL(5,2)) AS overall_pct,
                SUM(CASE WHEN absent_count = 0 AND present_count > 0 THEN 1 ELSE 0 END) AS present_students,
                SUM(CASE WHEN absent_count > 0 AND present_count > 0 THEN 1 ELSE 0 END) AS partial_absentees,
                SUM(CASE WHEN present_count = 0 AND absent_count > 0 THEN 1 ELSE 0 END) AS full_absentees,
                COUNT(student_id) AS total_students
            FROM StudentAgg
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stageId());
        q.setParameter("sectionId", filter.sectionId());
        q.setParameter("period", filter.period());
        q.setParameter("startDate", range.start);
        q.setParameter("endDate", range.end);

        try {
            Object[] r = (Object[]) q.getSingleResult();
            if (r == null || r[0] == null) return new AnalyticsOverviewDto(0.0, 0, 0, 0, 0);
            double pct = r[0] != null ? ((BigDecimal) r[0]).doubleValue() : 0.0;
            int present = r[1] != null ? ((Number) r[1]).intValue() : 0;
            int partial = r[2] != null ? ((Number) r[2]).intValue() : 0;
            int full = r[3] != null ? ((Number) r[3]).intValue() : 0;
            int total = r[4] != null ? ((Number) r[4]).intValue() : 0;
            return new AnalyticsOverviewDto(pct, present, partial, full, total);
        } catch (Exception e) {
            return new AnalyticsOverviewDto(0.0, 0, 0, 0, 0);
        }
    }

    @Override
    public List<AttendanceTrendDto> findAttendanceTrends(AttendanceFilter filter) {
        DateRange range = resolveDateRange(null, filter.startDate(), filter.endDate());

        String sql = """
            SELECT ar.attendance_date,
                   CAST((SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) * 100.0) / NULLIF(COUNT(*), 0) AS DECIMAL(5,2)) AS pct
            FROM attendance ar
            JOIN students s ON ar.student_id = s.id
            WHERE (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
                AND (:period IS NULL OR ar.period_no = :period)
                AND ar.attendance_date >= :startDate
                AND ar.attendance_date <= :endDate
            GROUP BY ar.attendance_date
            ORDER BY ar.attendance_date ASC
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stageId());
        q.setParameter("sectionId", filter.sectionId());
        q.setParameter("period", filter.period());
        q.setParameter("startDate", range.start);
        q.setParameter("endDate", range.end);

        List<Object[]> rows = q.getResultList();
        List<AttendanceTrendDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            double pct = row[1] != null ? ((BigDecimal) row[1]).doubleValue() : 0.0;
            result.add(new AttendanceTrendDto(date, pct));
        }
        return result;
    }

    @Override
    public AttendanceDistributionDto findAttendanceDistribution(AttendanceFilter filter) {
        AnalyticsOverviewDto overview = findAttendanceOverview(filter);
        int total = overview.presentStudents() + overview.partialAbsentees() + overview.fullDayAbsentees();
        if (total == 0) return new AttendanceDistributionDto(0.0, 0.0, 0.0);
        double presentPct = overview.presentStudents() * 100.0 / total;
        double partialPct = overview.partialAbsentees() * 100.0 / total;
        double fullPct = overview.fullDayAbsentees() * 100.0 / total;
        return new AttendanceDistributionDto(presentPct, partialPct, fullPct);
    }

    @Override
    public List<GroupedAttendanceDto> findDepartmentWiseAttendance(AttendanceFilter filter) {
        DateRange range = resolveDateRange(null, filter.startDate(), filter.endDate());

        String sql = """
            SELECT d.dept_name,
                   CAST((SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) * 100.0) / NULLIF(COUNT(*), 0) AS DECIMAL(5,2)) AS pct
            FROM attendance ar
            JOIN students s ON ar.student_id = s.id
            JOIN departments d ON s.department_id = d.id
            WHERE (:period IS NULL OR ar.period_no = :period)
                AND ar.attendance_date >= :startDate
                AND ar.attendance_date <= :endDate
            GROUP BY d.dept_name
            ORDER BY pct DESC
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("period", filter.period());
        q.setParameter("startDate", range.start);
        q.setParameter("endDate", range.end);

        List<Object[]> rows = q.getResultList();
        List<GroupedAttendanceDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new GroupedAttendanceDto((String) row[0],
                    row[1] != null ? ((BigDecimal) row[1]).doubleValue() : 0.0));
        }
        return result;
    }

    @Override
    public List<LowAttendanceStudentDto> findLowAttendanceStudents(AttendanceFilter filter) {
        DateRange range = resolveDateRange(null, filter.startDate(), filter.endDate());
        double threshold = filter.threshold() != null ? filter.threshold() : 75.0;

        String sql = """
            WITH StudentAgg AS (
                SELECT s.reg_no,
                       s.full_name,
                       SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) AS present_count,
                       SUM(CASE WHEN ar.status IN ('ABSENT', 'LEAVE') THEN 1 ELSE 0 END) AS absent_count
                FROM attendance ar
                JOIN students s ON ar.student_id = s.id
                WHERE (:departmentId IS NULL OR s.department_id = :departmentId)
                    AND (:stage IS NULL OR s.current_stage = :stage)
                    AND (:sectionId IS NULL OR s.section_id = :sectionId)
                    AND (:period IS NULL OR ar.period_no = :period)
                    AND ar.attendance_date >= :startDate
                    AND ar.attendance_date <= :endDate
                GROUP BY s.reg_no, s.full_name
            )
            SELECT reg_no, full_name,
                   CAST((SUM(present_count) * 100.0) / NULLIF(SUM(present_count + absent_count), 0) AS DECIMAL(5,2)) AS pct
            FROM StudentAgg
            GROUP BY reg_no, full_name
            HAVING CAST((SUM(present_count) * 100.0) / NULLIF(SUM(present_count + absent_count), 0) AS DECIMAL(5,2)) < :threshold
            ORDER BY pct ASC
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stageId());
        q.setParameter("sectionId", filter.sectionId());
        q.setParameter("period", filter.period());
        q.setParameter("startDate", range.start);
        q.setParameter("endDate", range.end);
        q.setParameter("threshold", threshold);

        List<Object[]> rows = q.getResultList();
        List<LowAttendanceStudentDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new LowAttendanceStudentDto((String) row[0], (String) row[1],
                    row[2] != null ? ((BigDecimal) row[2]).doubleValue() : 0.0));
        }
        return result;
    }

    @Override
    public List<GroupedAttendanceDto> findSectionWiseAttendance(AttendanceFilter filter) {
        DateRange range = resolveDateRange(null, filter.startDate(), filter.endDate());

        String sql = """
            SELECT sec.section_name,
                   CAST((SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) * 100.0) / NULLIF(COUNT(*), 0) AS DECIMAL(5,2)) AS pct
            FROM attendance ar
            JOIN students s ON ar.student_id = s.id
            JOIN section sec ON s.section_id = sec.id
            WHERE (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND (:period IS NULL OR ar.period_no = :period)
                AND ar.attendance_date >= :startDate
                AND ar.attendance_date <= :endDate
            GROUP BY sec.section_name
            ORDER BY sec.section_name ASC
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stageId());
        q.setParameter("period", filter.period());
        q.setParameter("startDate", range.start);
        q.setParameter("endDate", range.end);

        List<Object[]> rows = q.getResultList();
        List<GroupedAttendanceDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new GroupedAttendanceDto((String) row[0],
                    row[1] != null ? ((BigDecimal) row[1]).doubleValue() : 0.0));
        }
        return result;
    }

    @Override
    public List<AttendanceSummaryRowDto> findAttendanceSummaryTable(AttendanceFilter filter) {
        DateRange range = resolveDateRange(null, filter.startDate(), filter.endDate());

        String sql = """
            WITH StudentAgg AS (
                SELECT s.id AS student_id,
                       d.dept_name AS department_name,
                       SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) AS present_count,
                       SUM(CASE WHEN ar.status IN ('ABSENT', 'LEAVE') THEN 1 ELSE 0 END) AS absent_count
                FROM attendance ar
                JOIN students s ON ar.student_id = s.id
                JOIN departments d ON s.department_id = d.id
                WHERE (:departmentId IS NULL OR s.department_id = :departmentId)
                    AND (:stage IS NULL OR s.current_stage = :stage)
                    AND (:sectionId IS NULL OR s.section_id = :sectionId)
                    AND (:period IS NULL OR ar.period_no = :period)
                    AND ar.attendance_date >= :startDate
                    AND ar.attendance_date <= :endDate
                GROUP BY s.id, d.dept_name
            )
            SELECT department_name,
                   COUNT(student_id) AS total_students,
                   SUM(CASE WHEN absent_count = 0 THEN 1 ELSE 0 END) AS present_only,
                   SUM(CASE WHEN present_count > 0 AND absent_count > 0 THEN 1 ELSE 0 END) AS partial,
                   SUM(CASE WHEN present_count = 0 THEN 1 ELSE 0 END) AS absent_only,
                   CAST((SUM(present_count) * 100.0) / NULLIF(SUM(present_count + absent_count), 0) AS DECIMAL(5,2)) AS pct
            FROM StudentAgg
            GROUP BY department_name
            ORDER BY department_name
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stageId());
        q.setParameter("sectionId", filter.sectionId());
        q.setParameter("period", filter.period());
        q.setParameter("startDate", range.start);
        q.setParameter("endDate", range.end);

        List<Object[]> rows = q.getResultList();
        List<AttendanceSummaryRowDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            int total = ((Number) row[1]).intValue();
            int p = ((Number) row[2]).intValue();
            int pt = ((Number) row[3]).intValue();
            int a = ((Number) row[4]).intValue();
            double pct = row[5] != null ? ((BigDecimal) row[5]).doubleValue() : 0.0;
            result.add(new AttendanceSummaryRowDto((String) row[0], p, pt, a, pct, total));
        }
        return result;
    }

    @Override
    public List<AttendanceExportDto> findAttendanceExportData(AttendanceFilter filter) {
        DateRange range = resolveDateRange(null, filter.startDate(), filter.endDate());

        String sql = """
            SELECT s.reg_no, s.full_name, d.dept_name, sec.section_name,
                   ar.attendance_date, ar.period_no, ar.status
            FROM attendance ar
            JOIN students s ON ar.student_id = s.id
            JOIN departments d ON s.department_id = d.id
            LEFT JOIN section sec ON s.section_id = sec.id
            WHERE (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
                AND (:period IS NULL OR ar.period_no = :period)
                AND ar.attendance_date >= :startDate
                AND ar.attendance_date <= :endDate
            ORDER BY d.dept_name, sec.section_name, s.reg_no, ar.attendance_date, ar.period_no
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stageId());
        q.setParameter("sectionId", filter.sectionId());
        q.setParameter("period", filter.period());
        q.setParameter("startDate", range.start);
        q.setParameter("endDate", range.end);

        List<Object[]> rows = q.getResultList();
        List<AttendanceExportDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new AttendanceExportDto(
                    (String) row[0], (String) row[1], (String) row[2],
                    row[3] != null ? (String) row[3] : "",
                    ((java.sql.Date) row[4]).toLocalDate(),
                    ((Number) row[5]).intValue(),
                    (String) row[6]));
        }
        return result;
    }

    @Override
    public AttendanceSummaryDto findAttendanceSummaryByDate(AttendanceFilter filter) {
        DateRange range = resolveDateRange(null, filter.startDate(), filter.endDate());
        LocalDate targetDate = filter.date() != null ? filter.date() : LocalDate.now();

        String sql = """
            SELECT COUNT(*) AS totalStudents,
                   SUM(CASE WHEN ar.status = 'PRESENT' THEN 1 ELSE 0 END) AS presentCount,
                   SUM(CASE WHEN ar.status = 'ABSENT' THEN 1 ELSE 0 END) AS absentCount,
                   SUM(CASE WHEN ar.status = 'OD' THEN 1 ELSE 0 END) AS odCount,
                   SUM(CASE WHEN ar.status = 'LEAVE' THEN 1 ELSE 0 END) AS leaveCount,
                   ROUND(SUM(CASE WHEN ar.status = 'PRESENT' OR ar.status = 'OD' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) AS presentPercentage,
                   ROUND(SUM(CASE WHEN ar.status = 'ABSENT' OR ar.status = 'LEAVE' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) AS absentPercentage
            FROM attendance ar
            JOIN students s ON ar.student_id = s.id
            WHERE ar.attendance_date = :date
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
                AND (:period IS NULL OR ar.period_no = :period)
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("date", targetDate);
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stageId());
        q.setParameter("sectionId", filter.sectionId());
        q.setParameter("period", filter.period());

        Object[] r = (Object[]) q.getSingleResult();
        return new AttendanceSummaryDto(
                r[0] != null ? ((Number) r[0]).longValue() : 0L,
                r[1] != null ? ((Number) r[1]).longValue() : 0L,
                r[2] != null ? ((Number) r[2]).longValue() : 0L,
                r[3] != null ? ((Number) r[3]).longValue() : 0L,
                r[4] != null ? ((Number) r[4]).longValue() : 0L,
                r[5] != null ? ((Number) r[5]).doubleValue() : 0.0,
                r[6] != null ? ((Number) r[6]).doubleValue() : 0.0);
    }

    @Override
    public List<DepartmentXpAttendanceDto> findMonthlyAttendanceByDepartment(AttendanceFilter filter) {
        DateRange range = resolveDateRange(null, filter.startDate(), filter.endDate());

        String sql = """
            SELECT d.dept_name AS department,
                   DATE_FORMAT(ar.attendance_date, '%%Y-%%m') AS month,
                   NULL AS avgXp,
                   ROUND(SUM(CASE WHEN ar.status = 'PRESENT' OR ar.status = 'OD' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) AS attendanceRate
            FROM attendance ar
            JOIN students s ON ar.student_id = s.id
            JOIN departments d ON s.department_id = d.id
            WHERE (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
                AND (:period IS NULL OR ar.period_no = :period)
                AND ar.attendance_date >= :startDate
                AND ar.attendance_date <= :endDate
            GROUP BY d.dept_name, DATE_FORMAT(ar.attendance_date, '%%Y-%%m')
            ORDER BY month, d.dept_name
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stageId());
        q.setParameter("sectionId", filter.sectionId());
        q.setParameter("period", filter.period());
        q.setParameter("startDate", range.start);
        q.setParameter("endDate", range.end);

        List<Object[]> rows = q.getResultList();
        List<DepartmentXpAttendanceDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new DepartmentXpAttendanceDto(
                    (String) row[0], (String) row[1],
                    row[2] != null ? ((Number) row[2]).doubleValue() : null,
                    ((Number) row[3]).doubleValue()));
        }
        return result;
    }

    @Override
    public List<XpAwardVsPenaltyDto> findAwardVsPenalty(XpFilter filter) {
        DateRange range = resolveDateRange(filter.academicYear(), filter.startDate(), filter.endDate());

        String sql = """
            SELECT d.dept_name,
                   COALESCE(SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE 0 END), 0) AS awardXp,
                   COALESCE(SUM(CASE WHEN x.is_penalty = TRUE THEN ABS(x.xp_points) ELSE 0 END), 0) AS penaltyXp
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            JOIN departments d ON s.department_id = d.id
            %s
            WHERE x.status = 'APPROVED'
                AND %s
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
                AND x.submitted_at >= :startDate
                AND x.submitted_at <= :endDate
            GROUP BY d.dept_name
            ORDER BY d.dept_name
        """.formatted(joinYearFilterXp(), yearPredicateXp());

        Query q = entityManager.createNativeQuery(sql);
        setYearParam(q, filter.academicYear());
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stageId());
        q.setParameter("sectionId", filter.sectionId());
        q.setParameter("startDate", range.start);
        q.setParameter("endDate", range.end);

        List<Object[]> rows = q.getResultList();
        List<XpAwardVsPenaltyDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new XpAwardVsPenaltyDto((String) row[0],
                    ((Number) row[1]).longValue(), ((Number) row[2]).longValue()));
        }
        return result;
    }

    @Override
    public List<GroupedXpDto> findDepartmentRanking(XpFilter filter) {
        String sql = """
            SELECT d.dept_name,
                   COALESCE(AVG(tx.net_xp), 0) AS avgXp,
                   COALESCE(SUM(tx.net_xp), 0) AS totalXp,
                   COUNT(s.id) AS studentCount
            FROM departments d
            LEFT JOIN students s ON s.department_id = d.id AND s.active = 1
            %s
            LEFT JOIN (
                SELECT x.student_id, SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END) AS net_xp
                FROM xp_transactions x
                WHERE x.status = 'APPROVED'
                GROUP BY x.student_id
            ) tx ON tx.student_id = s.id
            WHERE %s
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND s.id IS NOT NULL
            GROUP BY d.dept_name
            ORDER BY totalXp DESC
        """.formatted(joinYearFilterXp(), yearPredicateXp());

        Query q = entityManager.createNativeQuery(sql);
        setYearParam(q, filter.academicYear());
        q.setParameter("stage", filter.stageId());

        List<Object[]> rows = q.getResultList();
        List<GroupedXpDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new GroupedXpDto((String) row[0], ((Number) row[1]).doubleValue(),
                    ((Number) row[2]).longValue(), ((Number) row[3]).longValue()));
        }
        return result;
    }

    @Override
    public List<GroupedXpDto> findSectionRanking(XpFilter filter) {
        String sql = """
            SELECT sec.section_name,
                   COALESCE(AVG(tx.net_xp), 0) AS avgXp,
                   COALESCE(SUM(tx.net_xp), 0) AS totalXp,
                   COUNT(s.id) AS studentCount
            FROM section sec
            LEFT JOIN students s ON s.section_id = sec.id AND s.active = 1
            %s
            LEFT JOIN (
                SELECT x.student_id, SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END) AS net_xp
                FROM xp_transactions x
                WHERE x.status = 'APPROVED'
                GROUP BY x.student_id
            ) tx ON tx.student_id = s.id
            WHERE %s
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND s.id IS NOT NULL
            GROUP BY sec.section_name
            ORDER BY totalXp DESC
        """.formatted(joinYearFilterXp(), yearPredicateXp());

        Query q = entityManager.createNativeQuery(sql);
        setYearParam(q, filter.academicYear());
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stageId());

        List<Object[]> rows = q.getResultList();
        List<GroupedXpDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new GroupedXpDto((String) row[0], ((Number) row[1]).doubleValue(),
                    ((Number) row[2]).longValue(), ((Number) row[3]).longValue()));
        }
        return result;
    }

    @Override
    public List<XpHeatmapDto> findMonthlyHeatmap(XpFilter filter) {
        DateRange range = resolveDateRange(filter.academicYear(), filter.startDate(), filter.endDate());

        String sql = """
            SELECT DATE(x.submitted_at),
                   SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END) AS netXp
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            %s
            WHERE x.status = 'APPROVED'
                AND %s
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
                AND x.submitted_at >= :startDate
                AND x.submitted_at <= :endDate
            GROUP BY DATE(x.submitted_at)
            ORDER BY DATE(x.submitted_at)
        """.formatted(joinYearFilterXp(), yearPredicateXp());

        Query q = entityManager.createNativeQuery(sql);
        setYearParam(q, filter.academicYear());
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stageId());
        q.setParameter("sectionId", filter.sectionId());
        q.setParameter("startDate", range.start);
        q.setParameter("endDate", range.end);

        List<Object[]> rows = q.getResultList();
        List<XpHeatmapDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            LocalDate date = row[0] != null ? ((java.sql.Date) row[0]).toLocalDate() : null;
            long xp = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            result.add(new XpHeatmapDto(date, xp, levelForXp(xp)));
        }
        return result;
    }

    @Override
    public List<XpTopPerformerDto> findXpTopPerformers(XpFilter filter) {
        String sql = """
            SELECT s.id, s.full_name AS studentName, s.reg_no, d.dept_name, sec.section_name,
                   COALESCE(tx.net_xp, 0) AS currentXp, COALESCE(tx.award_xp, 0) AS awardXp, COALESCE(tx.penalty_xp, 0) AS penaltyXp
            FROM students s
            JOIN departments d ON s.department_id = d.id
            LEFT JOIN section sec ON s.section_id = sec.id
            %s
            LEFT JOIN (
                SELECT x.student_id,
                       SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END) AS net_xp,
                       SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE 0 END) AS award_xp,
                       SUM(CASE WHEN x.is_penalty = TRUE THEN ABS(x.xp_points) ELSE 0 END) AS penalty_xp
                FROM xp_transactions x
                WHERE x.status = 'APPROVED'
                GROUP BY x.student_id
            ) tx ON tx.student_id = s.id
            WHERE s.active = 1
                AND %s
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
            GROUP BY s.id, s.full_name, s.reg_no, d.dept_name, sec.section_name, tx.net_xp, tx.award_xp, tx.penalty_xp
            ORDER BY currentXp DESC, studentName ASC
            LIMIT 50
        """.formatted(joinYearFilter(), yearPredicate());

        Query q = entityManager.createNativeQuery(sql);
        setYearParam(q, filter.academicYear());
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stageId());
        q.setParameter("sectionId", filter.sectionId());

        List<Object[]> rows = q.getResultList();
        List<XpTopPerformerDto> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            result.add(new XpTopPerformerDto(rank++, (String) row[1], (String) row[2], (String) row[3],
                    (String) row[4], ((Number) row[5]).longValue(),
                    ((Number) row[6]).longValue(), ((Number) row[7]).longValue()));
        }
        return result;
    }

    @Override
    public List<LowXpStudentDto> findLowXpStudents(XpFilter filter) {
        long threshold = filter.threshold() != null && filter.threshold() > 0 ? filter.threshold() : 20L;

        String sql = """
            SELECT s.full_name AS studentName, s.reg_no, d.dept_name, sec.section_name,
                   COALESCE(tx.net_xp, 0) AS currentXp
            FROM students s
            JOIN departments d ON s.department_id = d.id
            LEFT JOIN section sec ON s.section_id = sec.id
            %s
            LEFT JOIN (
                SELECT x.student_id, SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END) AS net_xp
                FROM xp_transactions x
                WHERE x.status = 'APPROVED'
                GROUP BY x.student_id
            ) tx ON tx.student_id = s.id
            WHERE s.active = 1
                AND %s
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
                AND COALESCE(tx.net_xp, 0) < :threshold
            GROUP BY s.full_name, s.reg_no, d.dept_name, sec.section_name, tx.net_xp
            ORDER BY currentXp ASC, studentName ASC
        """.formatted(joinYearFilter(), yearPredicate());

        Query q = entityManager.createNativeQuery(sql);
        setYearParam(q, filter.academicYear());
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stageId());
        q.setParameter("sectionId", filter.sectionId());
        q.setParameter("threshold", threshold);

        List<Object[]> rows = q.getResultList();
        List<LowXpStudentDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            long currentXp = ((Number) row[4]).longValue();
            result.add(new LowXpStudentDto((String) row[0], (String) row[1], (String) row[2], (String) row[3],
                    currentXp, threshold - currentXp));
        }
        return result;
    }

    @Override
    public List<ActivityXpContributionDto> findActivityXpContribution(XpFilter filter) {
        DateRange range = resolveDateRange(filter.academicYear(), filter.startDate(), filter.endDate());
        String categoryPredicate = (filter.category() != null && !filter.category().isEmpty())
                ? " AND x.activity_name = :activityName" : "";

        String sql = """
            SELECT x.activity_name, x.category,
                   COALESCE(SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE 0 END), 0) AS awardXp,
                   COALESCE(SUM(CASE WHEN x.is_penalty = TRUE THEN ABS(x.xp_points) ELSE 0 END), 0) AS penaltyXp,
                   COALESCE(SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END), 0) AS netXp
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            %s
            WHERE x.status = 'APPROVED'
                AND %s
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
                AND x.submitted_at >= :startDate
                AND x.submitted_at <= :endDate
                %s
            GROUP BY x.activity_name, x.category
            ORDER BY netXp DESC
        """.formatted(joinYearFilter(), yearPredicate(), categoryPredicate);

        Query q = entityManager.createNativeQuery(sql);
        setYearParam(q, filter.academicYear());
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stageId());
        q.setParameter("sectionId", filter.sectionId());
        q.setParameter("startDate", range.start);
        q.setParameter("endDate", range.end);
        if (filter.category() != null && !filter.category().isEmpty()) {
            q.setParameter("activityName", filter.category());
        }

        List<Object[]> rows = q.getResultList();
        List<ActivityXpContributionDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            long award = ((Number) row[2]).longValue();
            long penalty = ((Number) row[3]).longValue();
            result.add(new ActivityXpContributionDto((String) row[0], (String) row[1], award, penalty, award - penalty));
        }
        return result;
    }

    @Override
    public List<XpHistoryDto> findXpHistory(XpFilter filter) {
        DateRange range = resolveDateRange(filter.academicYear(), filter.startDate(), filter.endDate());
        StringBuilder typePredicate = new StringBuilder();
        if (filter.type() != null && !filter.type().isEmpty()) {
            if ("PENALTY".equalsIgnoreCase(filter.type())) {
                typePredicate.append(" AND x.is_penalty = TRUE");
            } else if ("AWARD".equalsIgnoreCase(filter.type())) {
                typePredicate.append(" AND x.is_penalty = FALSE");
            }
        }
        String activityPredicate = (filter.category() != null && !filter.category().isEmpty())
                ? " AND x.activity_name = :activityName" : "";

        String sql = """
            SELECT x.submitted_at, s.full_name, s.reg_no, d.dept_name, sec.section_name, x.activity_name,
                   CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE 0 END,
                   CASE WHEN x.is_penalty = TRUE THEN ABS(x.xp_points) ELSE 0 END,
                   CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END,
                   s.total_xp, x.approved_by
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            JOIN departments d ON s.department_id = d.id
            LEFT JOIN section sec ON s.section_id = sec.id
            %s
            WHERE x.status = 'APPROVED'
                AND %s
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
                AND x.submitted_at >= :startDate
                AND x.submitted_at <= :endDate
                %s
                %s
            ORDER BY x.submitted_at DESC
            LIMIT :limit OFFSET :offset
        """.formatted(joinYearFilter(), yearPredicateXp(), activityPredicate, typePredicate);

        Query q = entityManager.createNativeQuery(sql);
        setYearParam(q, filter.academicYear());
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stageId());
        q.setParameter("sectionId", filter.sectionId());
        q.setParameter("startDate", range.start);
        q.setParameter("endDate", range.end);
        if (filter.category() != null && !filter.category().isEmpty()) {
            q.setParameter("activityName", filter.category());
        }
        int limit = filter.size() > 0 ? filter.size() : 20;
        int offset = filter.page() * limit;
        q.setParameter("limit", limit);
        q.setParameter("offset", offset);

        List<Object[]> rows = q.getResultList();
        List<XpHistoryDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            int awd = row[6] != null ? ((Number) row[6]).intValue() : 0;
            int pen = row[7] != null ? ((Number) row[7]).intValue() : 0;
            result.add(new XpHistoryDto(
                    row[0] != null ? ((Timestamp) row[0]).toLocalDateTime() : null,
                    (String) row[1], (String) row[2], (String) row[3], (String) row[4], (String) row[5],
                    awd, pen, awd - pen,
                    row[9] != null ? ((Number) row[9]).longValue() : 0L,
                    (String) row[10]));
        }
        return result;
    }

    @Override
    public long countXpHistory(XpFilter filter) {
        DateRange range = resolveDateRange(filter.academicYear(), filter.startDate(), filter.endDate());
        StringBuilder typePredicate = new StringBuilder();
        if (filter.type() != null && !filter.type().isEmpty()) {
            if ("PENALTY".equalsIgnoreCase(filter.type())) {
                typePredicate.append(" AND x.is_penalty = TRUE");
            } else if ("AWARD".equalsIgnoreCase(filter.type())) {
                typePredicate.append(" AND x.is_penalty = FALSE");
            }
        }
        String activityPredicate = (filter.category() != null && !filter.category().isEmpty())
                ? " AND x.activity_name = :activityName" : "";

        String sql = """
            SELECT COUNT(*)
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            %s
            WHERE x.status = 'APPROVED'
                AND %s
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
                AND x.submitted_at >= :startDate
                AND x.submitted_at <= :endDate
                %s
                %s
        """.formatted(joinYearFilter(), yearPredicateXp(), activityPredicate, typePredicate);

        Query q = entityManager.createNativeQuery(sql);
        setYearParam(q, filter.academicYear());
        q.setParameter("departmentId", filter.departmentId());
        q.setParameter("stage", filter.stageId());
        q.setParameter("sectionId", filter.sectionId());
        q.setParameter("startDate", range.start);
        q.setParameter("endDate", range.end);
        if (filter.category() != null && !filter.category().isEmpty()) {
            q.setParameter("activityName", filter.category());
        }
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    public List<XpDistributionDto> findXpDistribution(String yearNo) {
        String sql = """
            SELECT CASE
                       WHEN net_xp = 0 THEN '0'
                       WHEN net_xp BETWEEN 1 AND 24 THEN '1-24'
                       WHEN net_xp BETWEEN 25 AND 49 THEN '25-49'
                       WHEN net_xp BETWEEN 50 AND 99 THEN '50-99'
                       WHEN net_xp BETWEEN 100 AND 199 THEN '100-199'
                       ELSE '200+'
                   END AS xpRange,
                   COUNT(*) AS studentCount
            FROM (
                SELECT s.id,
                       COALESCE(SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END), 0) AS net_xp
                FROM students s
                %s
                LEFT JOIN xp_transactions x ON x.student_id = s.id AND x.status = 'APPROVED'
                WHERE s.active = 1
                    AND %s
                GROUP BY s.id
            ) t
            GROUP BY xpRange
            ORDER BY xpRange
        """.formatted(joinYearFilterXp(), yearPredicateXp());

        Query q = entityManager.createNativeQuery(sql);
        setYearParam(q, yearNo);

        List<Object[]> rows = q.getResultList();
        Map<String, Long> rangeCounts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            rangeCounts.put((String) row[0], ((Number) row[1]).longValue());
        }
        long total = rangeCounts.values().stream().mapToLong(Long::longValue).sum();
        List<XpDistributionDto> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : rangeCounts.entrySet()) {
            double pct = total > 0 ? entry.getValue() * 100.0 / total : 0.0;
            result.add(new XpDistributionDto(entry.getKey(), entry.getValue(), roundDouble(pct)));
        }
        return result;
    }

    @Override
    public List<DepartmentXpAttendanceDto> findMonthlyAvgXpByDepartment(String yearNo) {
        String yearFilter = "";
        if (yearNo != null && !yearNo.isEmpty()) {
            yearFilter = " AND y.year_no = :yearNo";
        }
        String sql = """
            SELECT d.dept_name,
                   DATE_FORMAT(x.submitted_at, '%%Y-%%m') AS month,
                   COALESCE(AVG(tx.net_xp), 0) AS avgXp
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            JOIN departments d ON s.department_id = d.id
            %s
            LEFT JOIN (
                SELECT x.student_id, SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END) AS net_xp
                FROM xp_transactions x
                WHERE x.status = 'APPROVED'
                GROUP BY x.student_id
            ) tx ON tx.student_id = s.id
            WHERE x.status = 'APPROVED'
                %s
                AND s.id IS NOT NULL
            GROUP BY d.dept_name, DATE_FORMAT(x.submitted_at, '%%Y-%%m')
            ORDER BY month, d.dept_name
        """.formatted(joinYearFilter(), yearFilter);

        Query q = entityManager.createNativeQuery(sql);
        if (yearNo != null && !yearNo.isEmpty()) {
            setYearParam(q, yearNo);
        }

        List<Object[]> rows = q.getResultList();
        List<DepartmentXpAttendanceDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new DepartmentXpAttendanceDto((String) row[0], (String) row[1],
                    ((Number) row[2]).doubleValue(), null));
        }
        return result;
    }

    @Override
    public List<DepartmentXpAttendanceDto> findAllTimeAvgXpByDepartment(String yearNo) {
        String yearFilter = "";
        if (yearNo != null && !yearNo.isEmpty()) {
            yearFilter = " AND y.year_no = :yearNo";
        }
        String sql = """
            SELECT d.dept_name,
                   'ALL' AS month,
                   COALESCE(AVG(tx.net_xp), 0) AS avgXp
            FROM departments d
            LEFT JOIN students s ON s.department_id = d.id AND s.active = 1
            %s
            LEFT JOIN (
                SELECT x.student_id, SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END) AS net_xp
                FROM xp_transactions x
                WHERE x.status = 'APPROVED'
                GROUP BY x.student_id
            ) tx ON tx.student_id = s.id
            WHERE 1=1
                %s
                AND s.id IS NOT NULL
            GROUP BY d.dept_name
            ORDER BY avgXp DESC
        """.formatted(joinYearFilter(), yearFilter);

        Query q = entityManager.createNativeQuery(sql);
        if (yearNo != null && !yearNo.isEmpty()) {
            setYearParam(q, yearNo);
        }

        List<Object[]> rows = q.getResultList();
        List<DepartmentXpAttendanceDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new DepartmentXpAttendanceDto((String) row[0], (String) row[1],
                    ((Number) row[2]).doubleValue(), null));
        }
        return result;
    }

    @Override
    public List<TopPerformerDto> findTopPerformersNew(String yearNo, int limit) {
        int effectiveLimit = limit > 0 ? limit : 50;
        String sql = """
            SELECT s.reg_no, s.full_name AS fullName, d.dept_name,
                   COALESCE(tx.net_xp, 0) AS totalXp, s.current_stage,
                   COALESCE(sb.badgeCount, 0), COALESCE(st.current_streak, 0)
            FROM students s
            JOIN departments d ON s.department_id = d.id
            %s
            LEFT JOIN (
                SELECT x.student_id, SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END) AS net_xp
                FROM xp_transactions x
                WHERE x.status = 'APPROVED'
                GROUP BY x.student_id
            ) tx ON tx.student_id = s.id
            LEFT JOIN (SELECT student_id, COUNT(*) AS badgeCount FROM student_badges GROUP BY student_id) sb
                ON sb.student_id = s.id
            LEFT JOIN streaks st ON st.student_id = s.id
            WHERE s.active = 1
                AND %s
            ORDER BY totalXp DESC, fullName ASC
            LIMIT :limit
        """.formatted(joinYearFilter(), yearPredicate());

        Query q = entityManager.createNativeQuery(sql);
        setYearParam(q, yearNo);
        q.setParameter("limit", effectiveLimit);

        List<Object[]> rows = q.getResultList();
        List<TopPerformerDto> result = new ArrayList<>();
        long rank = 1;
        for (Object[] row : rows) {
            result.add(new TopPerformerDto((String) row[0], (String) row[1], (String) row[2],
                    row[3] != null ? ((Number) row[3]).intValue() : 0,
                    row[4] != null ? ((Number) row[4]).intValue() : 0,
                    ((Number) row[5]).longValue(),
                    row[6] != null ? ((Number) row[6]).intValue() : 0,
                    rank++));
        }
        return result;
    }

    @Override
    public List<EliteTeamDto> findEliteTeams(String yearNo, int limit) {
        int effectiveLimit = limit > 0 ? limit : 20;
        String sql = """
            SELECT t.id, t.name, d.dept_name, t.size,
                   COALESCE(SUM(tx.net_xp), 0) AS totalTeamXp, COALESCE(AVG(tx.net_xp), 0) AS avgTeamXp,
                   COALESCE(SUM(s.group_xp), 0), COALESCE(SUM(sb.badgeCount), 0)
            FROM teams t
            LEFT JOIN departments d ON t.department_id = d.id
            LEFT JOIN team_members tm ON tm.team_id = t.id
            LEFT JOIN students s ON s.id = tm.student_id AND s.active = 1
            %s
            LEFT JOIN (
                SELECT x.student_id, SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END) AS net_xp
                FROM xp_transactions x
                WHERE x.status = 'APPROVED'
                GROUP BY x.student_id
            ) tx ON tx.student_id = s.id
            LEFT JOIN (SELECT student_id, COUNT(*) AS badgeCount FROM student_badges GROUP BY student_id) sb
                ON sb.student_id = s.id
            WHERE %s
            GROUP BY t.id, t.name, d.dept_name, t.size
            ORDER BY avgTeamXp DESC, totalTeamXp DESC
            LIMIT :limit
        """.formatted(joinYearFilterXp(), yearPredicateXp());

        Query q = entityManager.createNativeQuery(sql);
        setYearParam(q, yearNo);
        q.setParameter("limit", effectiveLimit);

        List<Object[]> rows = q.getResultList();
        List<EliteTeamDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new EliteTeamDto(((Number) row[0]).longValue(), (String) row[1], (String) row[2],
                    row[3] != null ? ((Number) row[3]).intValue() : 0,
                    ((Number) row[4]).longValue(), ((Number) row[5]).doubleValue(),
                    ((Number) row[6]).longValue(), ((Number) row[7]).longValue()));
        }
        return result;
    }

    private Integer levelForXp(Long xp) {
        if (xp == null || xp <= 0) return 0;
        if (xp < 10) return 1;
        if (xp < 25) return 2;
        if (xp < 50) return 3;
        if (xp < 100) return 4;
        return 5;
    }
}
