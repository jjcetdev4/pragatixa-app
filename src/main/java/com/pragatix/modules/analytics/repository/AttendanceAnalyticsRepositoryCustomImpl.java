package com.pragatix.modules.analytics.repository;

import com.pragatix.modules.analytics.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AttendanceAnalyticsRepositoryCustomImpl implements AttendanceAnalyticsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private String buildWhereClause(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period, boolean includeWhere) {
        StringBuilder sb = new StringBuilder();
        List<String> conditions = new ArrayList<>();
        
        if (yearNo != null && !yearNo.isEmpty()) {
            conditions.add("y.year_no = :yearNo");
        }
        if (departmentId != null) {
            conditions.add("st.department_id = :departmentId");
        }
        if (stage != null) {
            conditions.add("st.stage = :stage");
        }
        if (sectionId != null) {
            conditions.add("st.section_id = :sectionId");
        }
        if (startDate != null) {
            conditions.add("ar.attendance_date >= :startDate");
        }
        if (endDate != null) {
            conditions.add("ar.attendance_date <= :endDate");
        }
        if (period != null) {
            conditions.add("ar.period_no = :period");
        }

        if (!conditions.isEmpty()) {
            if (includeWhere) {
                sb.append(" WHERE ");
            } else {
                sb.append(" AND ");
            }
            sb.append(String.join(" AND ", conditions));
        }
        
        return sb.toString();
    }

    private void setParameters(Query query, String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period) {
        if (yearNo != null && !yearNo.isEmpty()) {
            query.setParameter("yearNo", Byte.parseByte(yearNo));
        }
        if (departmentId != null) {
            query.setParameter("departmentId", departmentId);
        }
        if (stage != null) {
            query.setParameter("stage", stage);
        }
        if (sectionId != null) {
            query.setParameter("sectionId", sectionId);
        }
        if (startDate != null) {
            query.setParameter("startDate", startDate);
        }
        if (endDate != null) {
            query.setParameter("endDate", endDate);
        }
        if (period != null) {
            query.setParameter("period", period);
        }
    }

    @Override
    public AnalyticsOverviewDTO getOverview(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period) {
        String sql = "WITH StudentAgg AS (" +
                "  SELECT " +
                "    ar.student_id," +
                "    SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) as present_count," +
                "    SUM(CASE WHEN ar.status IN ('ABSENT', 'LEAVE') THEN 1 ELSE 0 END) as absent_count " +
                "  FROM attendance ar " +
                "  JOIN students st ON ar.student_id = st.id " +
                "  JOIN years y ON st.year_id = y.id " +
                buildWhereClause(yearNo, departmentId, stage, sectionId, startDate, endDate, period, true) +
                "  GROUP BY ar.student_id" +
                ") " +
                "SELECT " +
                "  CAST((SUM(present_count) * 100.0) / NULLIF(SUM(present_count + absent_count), 0) AS DECIMAL(5,2)) as overall_pct, " +
                "  SUM(CASE WHEN absent_count = 0 AND present_count > 0 THEN 1 ELSE 0 END) as present_students, " +
                "  SUM(CASE WHEN absent_count > 0 AND present_count > 0 THEN 1 ELSE 0 END) as partial_absentees, " +
                "  SUM(CASE WHEN present_count = 0 AND absent_count > 0 THEN 1 ELSE 0 END) as full_absentees, " +
                "  COUNT(student_id) as total_students " +
                "FROM StudentAgg";

        Query query = entityManager.createNativeQuery(sql);
        setParameters(query, yearNo, departmentId, stage, sectionId, startDate, endDate, period);

        Object[] result = (Object[]) query.getSingleResult();
        if (result == null || result[0] == null) {
            return new AnalyticsOverviewDTO(0.0, 0, 0, 0, 0);
        }

        Double pct = result[0] != null ? ((BigDecimal) result[0]).doubleValue() : 0.0;
        Integer present = result[1] != null ? ((Number) result[1]).intValue() : 0;
        Integer partial = result[2] != null ? ((Number) result[2]).intValue() : 0;
        Integer full = result[3] != null ? ((Number) result[3]).intValue() : 0;
        Integer total = result[4] != null ? ((Number) result[4]).intValue() : 0;

        return new AnalyticsOverviewDTO(pct, present, partial, full, total);
    }

    @Override
    public List<AttendanceTrendDTO> getTrend(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period) {
        String sql = "SELECT " +
                "  ar.attendance_date, " +
                "  CAST((SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) * 100.0) / NULLIF(COUNT(*), 0) AS DECIMAL(5,2)) as pct " +
                "FROM attendance ar " +
                "JOIN students st ON ar.student_id = st.id " +
                "JOIN years y ON st.year_id = y.id " +
                buildWhereClause(yearNo, departmentId, stage, sectionId, startDate, endDate, period, true) +
                " GROUP BY ar.attendance_date " +
                "ORDER BY ar.attendance_date ASC";

        Query query = entityManager.createNativeQuery(sql);
        setParameters(query, yearNo, departmentId, stage, sectionId, startDate, endDate, period);

        List<Object[]> rows = query.getResultList();
        List<AttendanceTrendDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Double pct = row[1] != null ? ((BigDecimal) row[1]).doubleValue() : 0.0;
            result.add(new AttendanceTrendDTO(date, pct));
        }
        return result;
    }

    @Override
    public AttendanceDistributionDTO getDistribution(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period) {
        AnalyticsOverviewDTO overview = getOverview(yearNo, departmentId, stage, sectionId, startDate, endDate, period);
        int total = overview.getPresentStudents() + overview.getPartialAbsentees() + overview.getFullDayAbsentees();
        
        if (total == 0) {
            return new AttendanceDistributionDTO(0.0, 0.0, 0.0);
        }

        double presentPct = (overview.getPresentStudents() * 100.0) / total;
        double partialPct = (overview.getPartialAbsentees() * 100.0) / total;
        double fullPct = (overview.getFullDayAbsentees() * 100.0) / total;

        return new AttendanceDistributionDTO(presentPct, partialPct, fullPct);
    }

    @Override
    public List<GroupedAttendanceDTO> getDepartmentWiseAttendance(String yearNo, LocalDate startDate, LocalDate endDate, Integer period) {
        String sql = "SELECT " +
                "  d.name, " +
                "  CAST((SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) * 100.0) / NULLIF(COUNT(*), 0) AS DECIMAL(5,2)) as pct " +
                "FROM attendance ar " +
                "JOIN students st ON ar.student_id = st.id " +
                "JOIN years y ON st.year_id = y.id " +
                "JOIN departments d ON st.department_id = d.id " +
                buildWhereClause(yearNo, null, null, null, startDate, endDate, period, true) +
                " GROUP BY d.name " +
                "ORDER BY pct DESC";

        Query query = entityManager.createNativeQuery(sql);
        setParameters(query, yearNo, null, null, null, startDate, endDate, period);

        List<Object[]> rows = query.getResultList();
        List<GroupedAttendanceDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new GroupedAttendanceDTO((String) row[0], row[1] != null ? ((BigDecimal) row[1]).doubleValue() : 0.0));
        }
        return result;
    }

    @Override
    public List<LowAttendanceStudentDTO> getLowAttendanceStudents(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period, Double threshold) {
        String sql = "WITH StudentAgg AS (" +
                "  SELECT " +
                "    st.reg_no, " +
                "    st.full_name, " +
                "    SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) as present_count, " +
                "    SUM(CASE WHEN ar.status IN ('ABSENT', 'LEAVE') THEN 1 ELSE 0 END) as absent_count " +
                "  FROM attendance ar " +
                "  JOIN students st ON ar.student_id = st.id " +
                "  JOIN years y ON st.year_id = y.id " +
                buildWhereClause(yearNo, departmentId, stage, sectionId, startDate, endDate, period, true) +
                "  GROUP BY st.reg_no, st.full_name" +
                ") " +
                "SELECT " +
                "  reg_no, " +
                "  full_name, " +
                "  CAST((SUM(present_count) * 100.0) / NULLIF(SUM(present_count + absent_count), 0) AS DECIMAL(5,2)) as pct " +
                "FROM StudentAgg " +
                "GROUP BY reg_no, full_name " +
                "HAVING CAST((SUM(present_count) * 100.0) / NULLIF(SUM(present_count + absent_count), 0) AS DECIMAL(5,2)) < :threshold " +
                "ORDER BY pct ASC";

        Query query = entityManager.createNativeQuery(sql);
        setParameters(query, yearNo, departmentId, stage, sectionId, startDate, endDate, period);
        query.setParameter("threshold", threshold);

        List<Object[]> rows = query.getResultList();
        List<LowAttendanceStudentDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new LowAttendanceStudentDTO((String) row[0], (String) row[1], row[2] != null ? ((BigDecimal) row[2]).doubleValue() : 0.0));
        }
        return result;
    }

    @Override
    public List<GroupedAttendanceDTO> getSectionWiseAttendance(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate, Integer period) {
        String sql = "SELECT " +
                "  sec.section_name, " +
                "  CAST((SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) * 100.0) / NULLIF(COUNT(*), 0) AS DECIMAL(5,2)) as pct " +
                "FROM attendance ar " +
                "JOIN students st ON ar.student_id = st.id " +
                "JOIN years y ON st.year_id = y.id " +
                "JOIN section sec ON st.section_id = sec.id " +
                buildWhereClause(yearNo, departmentId, stage, null, startDate, endDate, period, true) +
                " GROUP BY sec.section_name " +
                "ORDER BY sec.section_name ASC";

        Query query = entityManager.createNativeQuery(sql);
        setParameters(query, yearNo, departmentId, stage, null, startDate, endDate, period);

        List<Object[]> rows = query.getResultList();
        List<GroupedAttendanceDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new GroupedAttendanceDTO((String) row[0], row[1] != null ? ((BigDecimal) row[1]).doubleValue() : 0.0));
        }
        return result;
    }

    @Override
    public List<AttendanceSummaryRowDTO> getSummaryTable(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period) {
        String sql = "WITH StudentAgg AS (" +
                "  SELECT " +
                "    st.id as student_id, " +
                "    d.name as department_name, " +
                "    SUM(CASE WHEN ar.status IN ('PRESENT', 'OD') THEN 1 ELSE 0 END) as present_count, " +
                "    SUM(CASE WHEN ar.status IN ('ABSENT', 'LEAVE') THEN 1 ELSE 0 END) as absent_count " +
                "  FROM attendance ar " +
                "  JOIN students st ON ar.student_id = st.id " +
                "  JOIN departments d ON st.department_id = d.id " +
                "  JOIN years y ON st.year_id = y.id " +
                buildWhereClause(yearNo, departmentId, stage, sectionId, startDate, endDate, period, true) +
                "  GROUP BY st.id, d.name" +
                ") " +
                "SELECT " +
                "  department_name, " +
                "  COUNT(student_id) as total_students, " +
                "  SUM(CASE WHEN absent_count = 0 THEN 1 ELSE 0 END) as present_only, " +
                "  SUM(CASE WHEN present_count > 0 AND absent_count > 0 THEN 1 ELSE 0 END) as partial, " +
                "  SUM(CASE WHEN present_count = 0 THEN 1 ELSE 0 END) as absent_only, " +
                "  CAST((SUM(present_count) * 100.0) / NULLIF(SUM(present_count + absent_count), 0) AS DECIMAL(5,2)) as pct " +
                "FROM StudentAgg " +
                "GROUP BY department_name " +
                "ORDER BY department_name";

        Query query = entityManager.createNativeQuery(sql);
        setParameters(query, yearNo, departmentId, stage, sectionId, startDate, endDate, period);

        List<Object[]> rows = query.getResultList();
        List<AttendanceSummaryRowDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            int total = ((Number) row[1]).intValue();
            int p = ((Number) row[2]).intValue();
            int pt = ((Number) row[3]).intValue();
            int a = ((Number) row[4]).intValue();
            double pct = row[5] != null ? ((BigDecimal) row[5]).doubleValue() : 0.0;
            result.add(new AttendanceSummaryRowDTO((String) row[0], p, pt, a, pct, total));
        }
        return result;
    }

    @Override
    public List<AttendanceExportDTO> getExportData(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period) {
        String sql = "SELECT " +
                "  st.reg_no, " +
                "  st.full_name, " +
                "  d.name as department_name, " +
                "  sec.section_name, " +
                "  ar.attendance_date, " +
                "  ar.period_no, " +
                "  ar.status " +
                "FROM attendance ar " +
                "JOIN students st ON ar.student_id = st.id " +
                "JOIN departments d ON st.department_id = d.id " +
                "LEFT JOIN section sec ON st.section_id = sec.id " +
                "JOIN years y ON st.year_id = y.id " +
                buildWhereClause(yearNo, departmentId, stage, sectionId, startDate, endDate, period, true) +
                " ORDER BY d.name, sec.section_name, st.reg_no, ar.attendance_date, ar.period_no";

        Query query = entityManager.createNativeQuery(sql);
        setParameters(query, yearNo, departmentId, stage, sectionId, startDate, endDate, period);

        List<Object[]> rows = query.getResultList();
        List<AttendanceExportDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new AttendanceExportDTO(
                (String) row[0],
                (String) row[1],
                (String) row[2],
                row[3] != null ? (String) row[3] : "",
                ((java.sql.Date) row[4]).toLocalDate(),
                ((Number) row[5]).intValue(),
                (String) row[6]
            ));
        }
        return result;
    }
}
