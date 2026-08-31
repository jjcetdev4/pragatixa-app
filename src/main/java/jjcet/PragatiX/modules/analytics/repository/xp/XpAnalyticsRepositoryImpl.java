package jjcet.PragatiX.modules.analytics.repository.xp;

import jjcet.PragatiX.modules.analytics.dto.*;
import jjcet.PragatiX.modules.analytics.dto.AllInOneDashboardDto;
import jjcet.PragatiX.modules.analytics.dto.DepartmentXpAttendanceDto;
import jjcet.PragatiX.modules.analytics.dto.EliteTeamDto;
import jjcet.PragatiX.modules.analytics.dto.TopPerformerDto;
import jjcet.PragatiX.modules.analytics.dto.XpDistributionDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class XpAnalyticsRepositoryImpl implements XpAnalyticsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Builds the year-scoping predicate. Supports a 4-digit calendar year (e.g. "2026")
     * which is treated as a date-range scope, or a 1-4 year_no which matches the
     * students.year_id -> years.year_no. This mirrors AnalyticsRepositoryImpl behavior.
     */
    private String yearPredicate() {
        return "(:yearNo IS NULL OR y.year_no = :yearNo OR CHAR_LENGTH(:yearNo) = 4)";
    }

    private LocalDate effectiveStart(LocalDate start, String yearNo) {
        if (start != null) return start;
        if (yearNo != null && yearNo.length() == 4) return LocalDate.of(Integer.parseInt(yearNo), 1, 1);
        return LocalDate.of(2000, 1, 1);
    }

    private LocalDate effectiveEnd(LocalDate end, String yearNo) {
        if (end != null) return end;
        if (yearNo != null && yearNo.length() == 4) return LocalDate.of(Integer.parseInt(yearNo), 12, 31);
        return LocalDate.of(2030, 12, 31);
    }

    @Override
    public List<XpAwardVsPenaltyDTO> getAwardVsPenalty(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = effectiveStart(startDate, yearNo);
        LocalDate end = effectiveEnd(endDate, yearNo);

        String sql = """
            SELECT d.dept_name AS departmentName,
                   COALESCE(SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE 0 END), 0) AS awardXp,
                   COALESCE(SUM(CASE WHEN x.is_penalty = TRUE THEN ABS(x.xp_points) ELSE 0 END), 0) AS penaltyXp
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            JOIN departments d ON s.department_id = d.id
            JOIN years y ON s.year_id = y.id
            WHERE x.status = 'APPROVED'
                AND %s
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
                AND x.submitted_at >= :startDate
                AND x.submitted_at <= :endDate
            GROUP BY d.dept_name
            ORDER BY d.dept_name
            """.replace("%s", yearPredicate());

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("departmentId", departmentId);
        query.setParameter("stage", stage);
        query.setParameter("sectionId", sectionId);
        query.setParameter("startDate", start);
        query.setParameter("endDate", end);

        List<Object[]> rows = query.getResultList();
        List<XpAwardVsPenaltyDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new XpAwardVsPenaltyDTO(
                    (String) row[0],
                    ((Number) row[1]).longValue(),
                    ((Number) row[2]).longValue()));
        }
        return result;
    }

    @Override
    public List<GroupedXpDTO> getDepartmentRanking(String yearNo, Integer stage, LocalDate startDate, LocalDate endDate) {
        LocalDate start = effectiveStart(startDate, yearNo);
        LocalDate end = effectiveEnd(endDate, yearNo);

        String sql = """
            SELECT d.dept_name AS groupName,
                   COALESCE(AVG(tx.net_xp), 0) AS averageXp,
                   COALESCE(SUM(tx.net_xp), 0) AS totalXp,
                   COUNT(s.id) AS studentCount
            FROM departments d
            LEFT JOIN students s ON s.department_id = d.id AND s.active = 1
            LEFT JOIN years y ON s.year_id = y.id
            LEFT JOIN (
                SELECT x.student_id AS student_id,
                       SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END) AS net_xp
                FROM xp_transactions x
                WHERE x.status = 'APPROVED'
                GROUP BY x.student_id
            ) tx ON tx.student_id = s.id
            WHERE %s
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND s.id IS NOT NULL
            GROUP BY d.dept_name
            ORDER BY totalXp DESC
            """.replace("%s", yearPredicate());

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("stage", stage);

        List<Object[]> rows = query.getResultList();
        List<GroupedXpDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new GroupedXpDTO(
                    (String) row[0],
                    ((Number) row[1]).doubleValue(),
                    ((Number) row[2]).longValue(),
                    ((Number) row[3]).longValue()));
        }
        return result;
    }

    @Override
    public List<GroupedXpDTO> getSectionRanking(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate) {
        LocalDate start = effectiveStart(startDate, yearNo);
        LocalDate end = effectiveEnd(endDate, yearNo);

        String sql = """
            SELECT sec.section_name AS groupName,
                   COALESCE(AVG(tx.net_xp), 0) AS averageXp,
                   COALESCE(SUM(tx.net_xp), 0) AS totalXp,
                   COUNT(s.id) AS studentCount
            FROM section sec
            LEFT JOIN students s ON s.section_id = sec.id AND s.active = 1
            LEFT JOIN years y ON s.year_id = y.id
            LEFT JOIN (
                SELECT x.student_id AS student_id,
                       SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END) AS net_xp
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
            """.replace("%s", yearPredicate());

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("departmentId", departmentId);
        query.setParameter("stage", stage);

        List<Object[]> rows = query.getResultList();
        List<GroupedXpDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new GroupedXpDTO(
                    (String) row[0],
                    ((Number) row[1]).doubleValue(),
                    ((Number) row[2]).longValue(),
                    ((Number) row[3]).longValue()));
        }
        return result;
    }

    @Override
    public List<XpHeatmapDTO> getMonthlyHeatmap(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = effectiveStart(startDate, yearNo);
        LocalDate end = effectiveEnd(endDate, yearNo);

        String sql = """
            SELECT DATE(x.submitted_at) AS date,
                   SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END) AS xp
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            JOIN years y ON s.year_id = y.id
            WHERE x.status = 'APPROVED'
                AND %s
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
                AND x.submitted_at >= :startDate
                AND x.submitted_at <= :endDate
            GROUP BY DATE(x.submitted_at)
            ORDER BY date
            """.replace("%s", yearPredicate());

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("departmentId", departmentId);
        query.setParameter("stage", stage);
        query.setParameter("sectionId", sectionId);
        query.setParameter("startDate", start);
        query.setParameter("endDate", end);

        List<Object[]> rows = query.getResultList();
        List<XpHeatmapDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            LocalDate date = row[0] != null ? ((java.sql.Date) row[0]).toLocalDate() : null;
            Long xp = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            Integer level = levelForXp(xp);
            result.add(new XpHeatmapDTO(date, xp, level));
        }
        return result;
    }

    private Integer levelForXp(Long xp) {
        if (xp == null) return 0;
        if (xp <= 0) return 0;
        if (xp < 10) return 1;
        if (xp < 25) return 2;
        if (xp < 50) return 3;
        if (xp < 100) return 4;
        return 5;
    }

    @Override
    public List<XpTopPerformerDTO> getTopPerformers(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = effectiveStart(startDate, yearNo);
        LocalDate end = effectiveEnd(endDate, yearNo);

        String sql = """
            SELECT s.id AS studentId,
                   s.full_name AS studentName,
                   s.reg_no AS registerNumber,
                   d.dept_name AS department,
                   sec.section_name AS section,
                   COALESCE(tx.net_xp, 0) AS currentXp,
                   COALESCE(tx.award_xp, 0) AS awardedXp,
                   COALESCE(tx.penalty_xp, 0) AS penaltyXp
            FROM students s
            JOIN departments d ON s.department_id = d.id
            LEFT JOIN section sec ON s.section_id = sec.id
            JOIN years y ON s.year_id = y.id
            LEFT JOIN (
                SELECT x.student_id AS student_id,
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
            ORDER BY currentXp DESC, studentName ASC
            LIMIT 50
            """.replace("%s", yearPredicate());

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("departmentId", departmentId);
        query.setParameter("stage", stage);
        query.setParameter("sectionId", sectionId);

        List<Object[]> rows = query.getResultList();
        List<XpTopPerformerDTO> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            result.add(new XpTopPerformerDTO(
                    rank++,
                    (String) row[1],
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    ((Number) row[5]).longValue(),
                    ((Number) row[6]).longValue(),
                    ((Number) row[7]).longValue()));
        }
        return result;
    }

    @Override
    public List<LowXpStudentDTO> getLowXpStudents(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Long threshold) {
        Long effectiveThreshold = (threshold != null && threshold > 0) ? threshold : 20L;
        LocalDate start = effectiveStart(startDate, yearNo);
        LocalDate end = effectiveEnd(endDate, yearNo);

        String sql = """
            SELECT s.full_name AS studentName,
                   s.reg_no AS registerNumber,
                   d.dept_name AS department,
                   sec.section_name AS section,
                   COALESCE(tx.net_xp, 0) AS currentXp
            FROM students s
            JOIN departments d ON s.department_id = d.id
            LEFT JOIN section sec ON s.section_id = sec.id
            JOIN years y ON s.year_id = y.id
            LEFT JOIN (
                SELECT x.student_id AS student_id,
                       SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END) AS net_xp
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
            ORDER BY currentXp ASC, studentName ASC
            """.replace("%s", yearPredicate());

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("departmentId", departmentId);
        query.setParameter("stage", stage);
        query.setParameter("sectionId", sectionId);
        query.setParameter("threshold", effectiveThreshold);

        List<Object[]> rows = query.getResultList();
        List<LowXpStudentDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long currentXp = ((Number) row[4]).longValue();
            result.add(new LowXpStudentDTO(
                    (String) row[0],
                    (String) row[1],
                    (String) row[2],
                    (String) row[3],
                    currentXp,
                    effectiveThreshold - currentXp));
        }
        return result;
    }

    @Override
    public List<ActivityXpContributionDTO> getActivityXpContribution(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, String category) {
        LocalDate start = effectiveStart(startDate, yearNo);
        LocalDate end = effectiveEnd(endDate, yearNo);

        String categoryPredicate = (category != null && !category.isEmpty())
                ? " AND x.category = :category" : "";

        String sql = """
            SELECT x.activity_name AS activityName,
                   x.category AS category,
                   COALESCE(SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE 0 END), 0) AS totalAwardXp,
                   COALESCE(SUM(CASE WHEN x.is_penalty = TRUE THEN ABS(x.xp_points) ELSE 0 END), 0) AS totalPenaltyXp,
                   COALESCE(SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END), 0) AS netXp
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            JOIN years y ON s.year_id = y.id
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
            """.formatted(yearPredicate(), categoryPredicate);

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("departmentId", departmentId);
        query.setParameter("stage", stage);
        query.setParameter("sectionId", sectionId);
        query.setParameter("startDate", start);
        query.setParameter("endDate", end);
        if (category != null && !category.isEmpty()) {
            query.setParameter("category", category);
        }

        List<Object[]> rows = query.getResultList();
        List<ActivityXpContributionDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new ActivityXpContributionDTO(
                    (String) row[0],
                    (String) row[1],
                    ((Number) row[2]).longValue(),
                    ((Number) row[3]).longValue(),
                    ((Number) row[4]).longValue()));
        }
        return result;
    }

    @Override
    public List<XpHistoryDTO> getXpHistory(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, String activityName, String type, int limit, int offset) {
        LocalDate start = effectiveStart(startDate, yearNo);
        LocalDate end = effectiveEnd(endDate, yearNo);

        StringBuilder sql = new StringBuilder("""
            SELECT x.submitted_at AS date,
                   s.full_name AS studentName,
                   s.reg_no AS registerNumber,
                   d.dept_name AS department,
                   sec.section_name AS section,
                   x.activity_name AS activityName,
                   CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE 0 END AS awardXp,
                   CASE WHEN x.is_penalty = TRUE THEN ABS(x.xp_points) ELSE 0 END AS penaltyXp,
                   CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END AS netXp,
                   s.total_xp AS currentTotalXp,
                   x.approved_by AS approvedBy
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            JOIN departments d ON s.department_id = d.id
            LEFT JOIN section sec ON s.section_id = sec.id
            JOIN years y ON s.year_id = y.id
            WHERE x.status = 'APPROVED'
                AND %s
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
                AND x.submitted_at >= :startDate
                AND x.submitted_at <= :endDate
            """.replace("%s", yearPredicate()));

        if (activityName != null && !activityName.isEmpty()) {
            sql.append(" AND x.activity_name = :activityName");
        }
        if (type != null && !type.isEmpty()) {
            if ("PENALTY".equalsIgnoreCase(type)) {
                sql.append(" AND x.is_penalty = TRUE");
            } else if ("AWARD".equalsIgnoreCase(type)) {
                sql.append(" AND x.is_penalty = FALSE");
            }
        }
        sql.append(" ORDER BY x.submitted_at DESC LIMIT :limit OFFSET :offset");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("yearNo", yearNo);
        query.setParameter("departmentId", departmentId);
        query.setParameter("stage", stage);
        query.setParameter("sectionId", sectionId);
        query.setParameter("startDate", start);
        query.setParameter("endDate", end);
        if (activityName != null && !activityName.isEmpty()) {
            query.setParameter("activityName", activityName);
        }
        query.setParameter("limit", limit);
        query.setParameter("offset", offset);

        List<Object[]> rows = query.getResultList();
        List<XpHistoryDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new XpHistoryDTO(
                    row[0] != null ? ((java.sql.Timestamp) row[0]).toLocalDateTime() : null,
                    (String) row[1],
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    (String) row[5],
                    row[6] != null ? ((Number) row[6]).intValue() : 0,
                    row[7] != null ? ((Number) row[7]).intValue() : 0,
                    row[8] != null ? ((Number) row[8]).intValue() : 0,
                    row[9] != null ? ((Number) row[9]).longValue() : 0L,
                    (String) row[10]));
        }
        return result;
    }

    @Override
    public long getXpHistoryCount(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, String activityName, String type) {
        LocalDate start = effectiveStart(startDate, yearNo);
        LocalDate end = effectiveEnd(endDate, yearNo);

        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*)
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            JOIN years y ON s.year_id = y.id
            WHERE x.status = 'APPROVED'
                AND %s
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.current_stage = :stage)
                AND (:sectionId IS NULL OR s.section_id = :sectionId)
                AND x.submitted_at >= :startDate
                AND x.submitted_at <= :endDate
            """.replace("%s", yearPredicate()));

        if (activityName != null && !activityName.isEmpty()) {
            sql.append(" AND x.activity_name = :activityName");
        }
        if (type != null && !type.isEmpty()) {
            if ("PENALTY".equalsIgnoreCase(type)) {
                sql.append(" AND x.is_penalty = TRUE");
            } else if ("AWARD".equalsIgnoreCase(type)) {
                sql.append(" AND x.is_penalty = FALSE");
            }
        }

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("yearNo", yearNo);
        query.setParameter("departmentId", departmentId);
        query.setParameter("stage", stage);
        query.setParameter("sectionId", sectionId);
        query.setParameter("startDate", start);
        query.setParameter("endDate", end);
        if (activityName != null && !activityName.isEmpty()) {
            query.setParameter("activityName", activityName);
        }

        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public List<XpDistributionDto> getXpDistribution(String yearNo) {
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
                SELECT s.id AS id,
                       COALESCE(SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END), 0) AS net_xp
                FROM students s
                JOIN years y ON s.year_id = y.id
                LEFT JOIN xp_transactions x ON x.student_id = s.id AND x.status = 'APPROVED'
                WHERE s.active = 1
                    AND %s
                GROUP BY s.id
            ) t
            GROUP BY xpRange
            ORDER BY xpRange
            """.replace("%s", yearPredicate());

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);

        List<Object[]> rows = query.getResultList();
        Map<String, Long> rangeCounts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            rangeCounts.put((String) row[0], ((Number) row[1]).longValue());
        }

        long total = rangeCounts.values().stream().mapToLong(Long::longValue).sum();
        List<XpDistributionDto> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : rangeCounts.entrySet()) {
            double pct = total > 0 ? (entry.getValue() * 100.0 / total) : 0.0;
            result.add(new XpDistributionDto(entry.getKey(), entry.getValue(), pct));
        }
        return result;
    }

    @Override
    public List<DepartmentXpAttendanceDto> getMonthlyAvgXpByDepartment(String yearNo) {
        String sql = """
            SELECT d.dept_name AS department,
                   DATE_FORMAT(x.submitted_at, '%Y-%m') AS month,
                   COALESCE(AVG(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END), 0) AS avgXp
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            JOIN departments d ON s.department_id = d.id
            JOIN years y ON s.year_id = y.id
            WHERE x.status = 'APPROVED'
                AND %s
            GROUP BY d.dept_name, DATE_FORMAT(x.submitted_at, '%Y-%m')
            ORDER BY month, d.dept_name
            """.replace("%s", yearPredicate());

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);

        List<Object[]> rows = query.getResultList();
        List<DepartmentXpAttendanceDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new DepartmentXpAttendanceDto(
                    (String) row[0],
                    (String) row[1],
                    ((Number) row[2]).doubleValue(),
                    null));
        }
        return result;
    }

    @Override
    public List<DepartmentXpAttendanceDto> getAllTimeAvgXpByDepartment(String yearNo) {
        String sql = """
            SELECT d.dept_name AS department,
                   'ALL' AS month,
                   COALESCE(AVG(tx.net_xp), 0) AS avgXp
            FROM departments d
            LEFT JOIN students s ON s.department_id = d.id AND s.active = 1
            LEFT JOIN years y ON s.year_id = y.id
            LEFT JOIN (
                SELECT x.student_id AS student_id,
                       SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END) AS net_xp
                FROM xp_transactions x
                WHERE x.status = 'APPROVED'
                GROUP BY x.student_id
            ) tx ON tx.student_id = s.id
            WHERE %s
                AND s.id IS NOT NULL
            GROUP BY d.dept_name
            ORDER BY avgXp DESC
            """.replace("%s", yearPredicate());

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);

        List<Object[]> rows = query.getResultList();
        List<DepartmentXpAttendanceDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new DepartmentXpAttendanceDto(
                    (String) row[0],
                    (String) row[1],
                    ((Number) row[2]).doubleValue(),
                    null));
        }
        return result;
    }

    @Override
    public List<TopPerformerDto> getTopPerformersNew(String yearNo, int limit) {
        int effectiveLimit = (limit > 0) ? limit : 50;
        String sql = """
            SELECT s.reg_no AS regNo,
                   s.full_name AS fullName,
                   d.dept_name AS department,
                   COALESCE(tx.net_xp, 0) AS totalXp,
                   s.current_stage AS currentStage,
                   COALESCE(sb.badgeCount, 0) AS badgesEarned,
                   COALESCE(st.current_streak, 0) AS currentStreak
            FROM students s
            JOIN departments d ON s.department_id = d.id
            JOIN years y ON s.year_id = y.id
            LEFT JOIN (
                SELECT x.student_id AS student_id,
                       SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END) AS net_xp
                FROM xp_transactions x
                WHERE x.status = 'APPROVED'
                GROUP BY x.student_id
            ) tx ON tx.student_id = s.id
            LEFT JOIN (
                SELECT student_id, COUNT(*) AS badgeCount
                FROM student_badges
                GROUP BY student_id
            ) sb ON sb.student_id = s.id
            LEFT JOIN streaks st ON st.student_id = s.id
            WHERE s.active = 1
                AND %s
            ORDER BY totalXp DESC, fullName ASC
            LIMIT :limit
            """.replace("%s", yearPredicate());

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("limit", effectiveLimit);

        List<Object[]> rows = query.getResultList();
        List<TopPerformerDto> result = new ArrayList<>();
        long rank = 1;
        for (Object[] row : rows) {
            result.add(new TopPerformerDto(
                    (String) row[0],
                    (String) row[1],
                    (String) row[2],
                    row[3] != null ? ((Number) row[3]).intValue() : 0,
                    row[4] != null ? ((Number) row[4]).intValue() : 0,
                    ((Number) row[5]).longValue(),
                    row[6] != null ? ((Number) row[6]).intValue() : 0,
                    rank++));
        }
        return result;
    }

    @Override
    public List<EliteTeamDto> getTopEliteTeams(String yearNo, int limit) {
        int effectiveLimit = (limit > 0) ? limit : 20;
        String sql = """
            SELECT t.id AS teamId,
                   t.name AS teamName,
                   d.dept_name AS department,
                   t.size AS teamSize,
                   COALESCE(SUM(tx.net_xp), 0) AS totalTeamXp,
                   COALESCE(AVG(tx.net_xp), 0) AS avgTeamXp,
                   COALESCE(SUM(s.group_xp), 0) AS groupActivityXp,
                   COALESCE(SUM(sb.badgeCount), 0) AS badgesEarnedByTeam
            FROM teams t
            LEFT JOIN departments d ON t.department_id = d.id
            LEFT JOIN team_members tm ON tm.team_id = t.id
            LEFT JOIN students s ON s.id = tm.student_id AND s.active = 1
            LEFT JOIN years y ON s.year_id = y.id
            LEFT JOIN (
                SELECT x.student_id AS student_id,
                       SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -ABS(x.xp_points) END) AS net_xp
                FROM xp_transactions x
                WHERE x.status = 'APPROVED'
                GROUP BY x.student_id
            ) tx ON tx.student_id = s.id
            LEFT JOIN (
                SELECT student_id, COUNT(*) AS badgeCount
                FROM student_badges
                GROUP BY student_id
            ) sb ON sb.student_id = s.id
            WHERE %s
            GROUP BY t.id, t.name, d.dept_name, t.size
            ORDER BY avgTeamXp DESC, totalTeamXp DESC
            LIMIT :limit
            """.replace("%s", yearPredicate());

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yearNo", yearNo);
        query.setParameter("limit", effectiveLimit);

        List<Object[]> rows = query.getResultList();
        List<EliteTeamDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new EliteTeamDto(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    row[3] != null ? ((Number) row[3]).intValue() : 0,
                    ((Number) row[4]).longValue(),
                    ((Number) row[5]).doubleValue(),
                    ((Number) row[6]).longValue(),
                    ((Number) row[7]).longValue()));
        }
        return result;
    }

    @Override
    public AllInOneDashboardDto getDashboardSummary(String yearNo) {
        // Total students count
        String totalStudentsSql = """
            SELECT COUNT(*) FROM students s
            JOIN years y ON s.year_id = y.id
            WHERE s.active = 1
                AND (%s)
            """.replace("%s", yearPredicate());

        Query totalStudentsQuery = entityManager.createNativeQuery(totalStudentsSql);
        totalStudentsQuery.setParameter("yearNo", yearNo);
        Long totalStudents = ((Number) totalStudentsQuery.getSingleResult()).longValue();

        // Institution growth: monthly totalXp/activeStudents/avgXp
        String growthSql = """
            SELECT
                DATE_FORMAT(x.submitted_at, '%Y-%m') AS period,
                SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS totalXp,
                COUNT(DISTINCT x.student_id) AS activeStudents,
                AVG(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS avgXpPerStudent
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            JOIN years y ON s.year_id = y.id
            WHERE x.status = 'APPROVED'
                AND (%s)
                AND x.submitted_at >= :startDate
                AND x.submitted_at < :endDatePlusOne
            GROUP BY DATE_FORMAT(x.submitted_at, '%Y-%m')
            ORDER BY period
        """.replace("%s", yearPredicate());

        Query growthQuery = entityManager.createNativeQuery(growthSql);
        growthQuery.setParameter("yearNo", yearNo);
        growthQuery.setParameter("startDate", LocalDate.of(2000, 1, 1));
        growthQuery.setParameter("endDatePlusOne", LocalDate.of(2030, 12, 31));
        List<Object[]> growthResults = growthQuery.getResultList();

        List<InstitutionGrowthDto> institutionGrowth = new ArrayList<>();
        if (!growthResults.isEmpty()) {
            Double maxXp = 0.0;
            Long maxActiveStudents = 0L;
            for (Object[] row : growthResults) {
                Double totalXp = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                Long activeStudents = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                if (totalXp > maxXp) maxXp = totalXp;
                if (activeStudents > maxActiveStudents) maxActiveStudents = activeStudents;
            }
            for (Object[] row : growthResults) {
                String period = (String) row[0];
                Double totalXp = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                Long activeStudents = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                Double avgXpPerStudent = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
                Double growthRate = (maxXp > 0) ? (totalXp / maxXp * 100.0) : 0.0;
                institutionGrowth.add(new InstitutionGrowthDto(period, growthRate, totalXp, avgXpPerStudent, growthRate, 0));
            }
        }

        // Department growth
        String deptGrowthSql = """
            SELECT
                d.id AS departmentId,
                d.name AS departmentName,
                SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS totalXp
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            JOIN departments d ON s.department_id = d.id
            JOIN years y ON s.year_id = y.id
            WHERE x.status = 'APPROVED'
                AND (%s)
                AND x.submitted_at >= :startDate
                AND x.submitted_at < :endDatePlusOne
            GROUP BY d.id, d.name
            ORDER BY totalXp DESC
        """.replace("%s", yearPredicate());

        Query deptGrowthQuery = entityManager.createNativeQuery(deptGrowthSql);
        deptGrowthQuery.setParameter("yearNo", yearNo);
        deptGrowthQuery.setParameter("startDate", LocalDate.of(2000, 1, 1));
        deptGrowthQuery.setParameter("endDatePlusOne", LocalDate.of(2030, 12, 31));
        List<Object[]> deptResults = deptGrowthQuery.getResultList();

        List<DepartmentGrowthDto> departmentGrowth = new ArrayList<>();
        if (!deptResults.isEmpty()) {
            Double maxXp = 0.0;
            for (Object[] row : deptResults) {
                Double totalXp = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
                if (totalXp > maxXp) maxXp = totalXp;
            }
            for (Object[] row : deptResults) {
                Long deptId = ((Number) row[0]).longValue();
                String deptName = (String) row[1];
                Double totalXp = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
                Double growthRate = (maxXp > 0) ? (totalXp / maxXp * 100.0) : 0.0;
                departmentGrowth.add(new DepartmentGrowthDto(deptId, deptName, growthRate, totalXp.longValue()));
            }
        }

        // XP curve
        String xpCurveSql = """
            SELECT
                DATE_FORMAT(x.submitted_at, '%Y-%m-%d') AS period,
                SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS dailyXp
            FROM xp_transactions x
            JOIN students s ON x.student_id = s.id
            JOIN years y ON s.year_id = y.id
            WHERE x.status = 'APPROVED'
                AND (%s)
                AND x.submitted_at >= :startDate
                AND x.submitted_at < :endDatePlusOne
            GROUP BY DATE_FORMAT(x.submitted_at, '%Y-%m-%d')
            ORDER BY period
        """.replace("%s", yearPredicate());

        Query xpCurveQuery = entityManager.createNativeQuery(xpCurveSql);
        xpCurveQuery.setParameter("yearNo", yearNo);
        xpCurveQuery.setParameter("startDate", LocalDate.of(2000, 1, 1));
        xpCurveQuery.setParameter("endDatePlusOne", LocalDate.of(2030, 12, 31));
        List<Object[]> xpCurveResults = xpCurveQuery.getResultList();

        List<XpCurveDto> xpCurve = new ArrayList<>();
        for (Object[] row : xpCurveResults) {
            String period = (String) row[0];
            Double dailyXp = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            xpCurve.add(new XpCurveDto(period, dailyXp));
        }

        // Stage distribution
        String stageSql = """
            SELECT
                s.stage AS stageName,
                COUNT(s.id) AS studentCount
            FROM students s
            JOIN years y ON s.year_id = y.id
            WHERE (%s)
            GROUP BY s.stage
            ORDER BY s.stage
        """.replace("%s", yearPredicate());

        Query stageQuery = entityManager.createNativeQuery(stageSql);
        stageQuery.setParameter("yearNo", yearNo);
        List<Object[]> stageResults = stageQuery.getResultList();

        List<StageDistributionDto> stageDistribution = new ArrayList<>();
        Long totalStudentsFromStage = 0L;
        for (Object[] row : stageResults) {
            String stageName = row[0] != null ? String.valueOf(((Number) row[0]).intValue()) : "Unknown";
            Long studentCount = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            totalStudentsFromStage += studentCount;
            Double percentage = totalStudentsFromStage > 0 ? (studentCount * 100.0 / totalStudentsFromStage) : 0.0;
            stageDistribution.add(new StageDistributionDto(stageName, studentCount, percentage));
        }

        // Top performers (limit 10)
        String topPerformersSql = """
            SELECT 
                s.id AS studentId,
                s.full_name AS studentName,
                s.reg_no AS rollNumber,
                SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS totalXp,
                COALESCE(
                    SUM(CASE WHEN a.id IS NOT NULL AND a.status = 'PRESENT' THEN 1 ELSE 0 END) * 100.0 /
                    NULLIF(SUM(CASE WHEN a.id IS NOT NULL THEN 1 ELSE 0 END), 0), 0
                ) AS attendanceRate
            FROM students s
            JOIN years y ON s.year_id = y.id
             LEFT JOIN xp_transactions x ON s.id = x.student_id AND x.status = 'APPROVED'
            LEFT JOIN attendance a ON s.id = a.student_id
                AND a.attendance_date BETWEEN :startDate AND :endDate
            WHERE (%s)
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.stage = :stage)
            GROUP BY s.id, s.full_name, s.reg_no
            ORDER BY totalXp DESC, attendanceRate DESC
            LIMIT 10
        """.replace("%s", yearPredicate());

        Query topPerformersQuery = entityManager.createNativeQuery(topPerformersSql);
        topPerformersQuery.setParameter("yearNo", yearNo);
        topPerformersQuery.setParameter("departmentId", null);
        topPerformersQuery.setParameter("stage", null);
        topPerformersQuery.setParameter("startDate", LocalDate.of(2000, 1, 1));
        topPerformersQuery.setParameter("endDate", LocalDate.of(2030, 12, 31));
        List<Object[]> topPerformersResults = topPerformersQuery.getResultList();

        List<PerformanceLeaderDto> topPerformers = new ArrayList<>();
        for (Object[] row : topPerformersResults) {
            Long studentId = ((Number) row[0]).longValue();
            String studentName = (String) row[1];
            String rollNumber = row[2] != null ? (String) row[2] : null;
            Double xp = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            Double attendance = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
            topPerformers.add(new PerformanceLeaderDto(studentId, studentName, rollNumber, xp, attendance));
        }

        // Most improved
        String improvedSql = """
            SELECT 
                s.id AS studentId,
                s.full_name AS studentName,
                s.reg_no AS rollNumber,
                CURRENT_XP.total_xp AS currentXp,
                PREVIOUS_XP.total_xp AS previousXp,
                (COALESCE(CURRENT_XP.total_xp, 0) - COALESCE(PREVIOUS_XP.total_xp, 0)) AS improvement
            FROM students s
            JOIN years y ON s.year_id = y.id
            LEFT JOIN (
                SELECT x.student_id AS student_id,
                       SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS total_xp
                FROM xp_transactions x
                JOIN years y2 ON s.year_id = y2.id
                WHERE x.status = 'APPROVED'
                    AND (%s)
                    AND x.submitted_at >= :startDate
                    AND x.submitted_at < :endDatePlusOne
                GROUP BY x.student_id
            ) CURRENT_XP ON CURRENT_XP.student_id = s.id
            LEFT JOIN (
                SELECT x.student_id AS student_id,
                       SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END) AS total_xp
                FROM xp_transactions x
                JOIN years y3 ON s.year_id = y3.id
                WHERE x.status = 'APPROVED'
                    AND (%s)
                    AND x.submitted_at >= :startDate
                    AND x.submitted_at < :endDatePlusOne
                GROUP BY x.student_id
            ) PREVIOUS_XP ON PREVIOUS_XP.student_id = s.id
            WHERE (%s)
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.stage = :stage)
            ORDER BY improvement DESC
            LIMIT 10
        """.replace("%s", yearPredicate());

        Query improvedQuery = entityManager.createNativeQuery(improvedSql);
        improvedQuery.setParameter("yearNo", yearNo);
        improvedQuery.setParameter("startDate", LocalDate.of(2000, 1, 1));
        improvedQuery.setParameter("endDatePlusOne", LocalDate.of(2030, 12, 31));
        improvedQuery.setParameter("departmentId", null);
        improvedQuery.setParameter("stage", null);
        List<Object[]> improvedResults = improvedQuery.getResultList();

        List<MoverDto> mostImproved = new ArrayList<>();
        for (Object[] row : improvedResults) {
            Long studentId = ((Number) row[0]).longValue();
            String studentName = (String) row[1];
            String rollNumber = row[2] != null ? (String) row[2] : null;
            Double currentXp = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            Double previousXp = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
            Double improvement = row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;
            Double improvementPercent = (previousXp != null && previousXp != 0)
                    ? ((currentXp - previousXp) / previousXp) * 100.0
                    : null;
            mostImproved.add(new MoverDto(studentId, studentName, rollNumber, improvementPercent));
        }

        // Activity funnel
        String funnelSql = """
            SELECT 
                (SELECT COUNT(DISTINCT aa.id) FROM activity_assignments aa WHERE aa.status = 'ACTIVE' AND (:departmentId IS NULL OR aa.department_id = :departmentId)) AS assigned,
                (SELECT COUNT(DISTINCT acr.id) FROM activity_completion_requests acr WHERE acr.status = 'PENDING' AND (:departmentId IS NULL OR EXISTS (SELECT 1 FROM students s WHERE s.id = acr.student_id AND s.department_id = :departmentId))) AS started,
                (SELECT COUNT(DISTINCT acr.id) FROM activity_completion_requests acr WHERE acr.status IN ('PENDING', 'APPROVED', 'REJECTED') AND (:departmentId IS NULL OR EXISTS (SELECT 1 FROM students s WHERE s.id = acr.student_id AND s.department_id = :departmentId))) AS submitted,
                (SELECT COUNT(DISTINCT acr.id) FROM activity_completion_requests acr WHERE acr.status = 'APPROVED' AND (:departmentId IS NULL OR EXISTS (SELECT 1 FROM students s WHERE s.id = acr.student_id AND s.department_id = :departmentId))) AS approved,
                (SELECT COALESCE(SUM(CASE WHEN x.is_penalty = FALSE THEN x.xp_points ELSE -x.xp_points END), 0) FROM xp_transactions x JOIN students s ON x.student_id = s.id WHERE x.status = 'APPROVED' AND (:departmentId IS NULL OR s.department_id = :departmentId)) AS xpAwarded
            FROM DUAL
        """.replace("%s", yearPredicate());

        Query funnelQuery = entityManager.createNativeQuery(funnelSql);
        funnelQuery.setParameter("departmentId", null);
        Object[] funnelResult = (Object[]) funnelQuery.getSingleResult();
        Long assigned = funnelResult[0] != null ? ((Number) funnelResult[0]).longValue() : 0L;
        Long started = funnelResult[1] != null ? ((Number) funnelResult[1]).longValue() : 0L;
        Long submitted = funnelResult[2] != null ? ((Number) funnelResult[2]).longValue() : 0L;
        Long approved = funnelResult[3] != null ? ((Number) funnelResult[3]).longValue() : 0L;
        Long xpAwarded = funnelResult[4] != null ? ((Number) funnelResult[4]).longValue() : 0L;

        List<ActivityFunnelDto> activityFunnel = new ArrayList<>();
        if (assigned > 0) {
            activityFunnel.add(new ActivityFunnelDto("Assigned", assigned, 100.0));
            activityFunnel.add(new ActivityFunnelDto("Started", started, (double) started / assigned * 100.0));
            activityFunnel.add(new ActivityFunnelDto("Submitted", submitted, (double) submitted / assigned * 100.0));
            activityFunnel.add(new ActivityFunnelDto("Approved", approved, (double) approved / assigned * 100.0));
        } else {
            activityFunnel.add(new ActivityFunnelDto("Assigned", 0L, 0.0));
            activityFunnel.add(new ActivityFunnelDto("Started", 0L, 0.0));
            activityFunnel.add(new ActivityFunnelDto("Submitted", 0L, 0.0));
            activityFunnel.add(new ActivityFunnelDto("Approved", 0L, 0.0));
        }

        // Intervention risks
        String risksSql = """
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
            JOIN years y ON s.year_id = y.id
            LEFT JOIN attendance a ON s.id = a.student_id
                AND a.attendance_date >= :startDate
                AND a.attendance_date <= :endDate
             LEFT JOIN xp_transactions x ON s.id = x.student_id
                AND x.status = 'APPROVED'
                AND x.submitted_at >= :startDate
                AND x.submitted_at <= :endDate
            WHERE (%s)
                AND (:departmentId IS NULL OR s.department_id = :departmentId)
                AND (:stage IS NULL OR s.stage = :stage)
            GROUP BY s.id, s.full_name, s.reg_no
        """.replace("%s", yearPredicate());

        Query risksQuery = entityManager.createNativeQuery(risksSql);
        risksQuery.setParameter("yearNo", yearNo);
        risksQuery.setParameter("startDate", LocalDate.of(2000, 1, 1));
        risksQuery.setParameter("endDate", LocalDate.of(2030, 12, 31));
        risksQuery.setParameter("endDatePlusOne", LocalDate.of(2030, 12, 31));
        risksQuery.setParameter("departmentId", null);
        risksQuery.setParameter("stage", null);
        List<Object[]> risksResults = risksQuery.getResultList();

        List<InterventionRiskDto> interventionRisks = new ArrayList<>();
        long riskTotalStudents = 0, highRiskCount = 0, mediumRiskCount = 0, lowRiskCount = 0;
        for (Object[] row : risksResults) {
            riskTotalStudents++;
            Double attendancePercentage = row[0] != null ? ((Number) row[0]).doubleValue() : null;
            if (attendancePercentage != null) {
                if (attendancePercentage < 75.0) highRiskCount++;
                else if (attendancePercentage < 85.0) mediumRiskCount++;
                else lowRiskCount++;
            }
        }
        InterventionStatusDto riskStatus = new InterventionStatusDto(
                riskTotalStudents,
                highRiskCount,
                mediumRiskCount,
                lowRiskCount,
                interventionRisks
        );

        // Build AllInOneDashboardDto
        AllInOneDashboardDto dto = new AllInOneDashboardDto();
        dto.setTotalStudents(totalStudents);
        dto.setInstitutionGrowth(institutionGrowth);
        dto.setDepartmentGrowth(departmentGrowth);
        dto.setXpCurve(xpCurve);
        dto.setStageDistribution(stageDistribution);
        dto.setTopPerformers(topPerformers);
        dto.setMostImproved(mostImproved);
        dto.setActivityFunnel(new ActivityFunnelWrapperDto(activityFunnel));
        dto.setInterventionRisk(riskStatus);

        return dto;
    }
}
