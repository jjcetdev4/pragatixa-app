package com.pragatix.modules.analytics.repository;

import com.pragatix.modules.analytics.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class XpAnalyticsRepositoryCustomImpl implements XpAnalyticsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private String buildWhereClause(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, boolean includeWhere) {
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
            conditions.add("x.submitted_at >= :startDate");
        }
        if (endDate != null) {
            // End of day
            conditions.add("x.submitted_at < :endDatePlusOne");
        }

        if (!conditions.isEmpty()) {
            if (includeWhere) sb.append(" WHERE ");
            else sb.append(" AND ");
            sb.append(String.join(" AND ", conditions));
        }
        return sb.toString();
    }

    private void setParameters(Query query, String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate) {
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
            query.setParameter("startDate", startDate.atStartOfDay());
        }
        if (endDate != null) {
            query.setParameter("endDatePlusOne", endDate.plusDays(1).atStartOfDay());
        }
    }




    @Override
    public List<XpAwardVsPenaltyDTO> getAwardVsPenalty(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT d.name, " +
                " SUM(CASE WHEN x.is_penalty = false THEN x.xp_points ELSE 0 END) as award, " +
                " SUM(CASE WHEN x.is_penalty = true THEN x.xp_points ELSE 0 END) as penalty " +
                "FROM xp_transactions x " +
                "JOIN students st ON x.student_id = st.id " +
                "JOIN departments d ON st.department_id = d.id " +
                "JOIN years y ON st.year_id = y.id " +
                buildWhereClause(yearNo, departmentId, stage, sectionId, startDate, endDate, true) +
                " GROUP BY d.id, d.name ORDER BY award DESC";

        Query query = entityManager.createNativeQuery(sql);
        setParameters(query, yearNo, departmentId, stage, sectionId, startDate, endDate);

        List<Object[]> rows = query.getResultList();
        List<XpAwardVsPenaltyDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new XpAwardVsPenaltyDTO((String) row[0], ((Number) row[1]).longValue(), ((Number) row[2]).longValue()));
        }
        return result;
    }

    @Override
    public List<GroupedXpDTO> getDepartmentRanking(String yearNo, Integer stage, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT d.name, AVG(st.total_xp), SUM(st.total_xp), COUNT(st.id) " +
                "FROM students st " +
                "JOIN departments d ON st.department_id = d.id " +
                "JOIN years y ON st.year_id = y.id ";
                
        List<String> conds = new ArrayList<>();
        if (yearNo != null && !yearNo.isEmpty()) conds.add("y.year_no = :yearNo");
        if (stage != null) conds.add("st.stage = :stage");
        if (!conds.isEmpty()) sql += " WHERE " + String.join(" AND ", conds);
        
        sql += " GROUP BY d.id, d.name ORDER BY AVG(st.total_xp) DESC";

        Query query = entityManager.createNativeQuery(sql);
        if (yearNo != null && !yearNo.isEmpty()) query.setParameter("yearNo", Byte.parseByte(yearNo));
        if (stage != null) query.setParameter("stage", stage);

        List<Object[]> rows = query.getResultList();
        List<GroupedXpDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new GroupedXpDTO((String) row[0], ((Number) row[1]).doubleValue(), ((Number) row[2]).longValue(), ((Number) row[3]).longValue()));
        }
        return result;
    }

    @Override
    public List<GroupedXpDTO> getSectionRanking(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT sec.section_name, AVG(COALESCE(x.net_xp, 0)), SUM(COALESCE(x.net_xp, 0)), COUNT(st.id) " +
                "FROM students st " +
                "JOIN section sec ON st.section_id = sec.id " +
                "JOIN years y ON st.year_id = y.id " +
                "LEFT JOIN (" +
                "  SELECT student_id, SUM(CASE WHEN is_penalty = false THEN xp_points ELSE -xp_points END) as net_xp " +
                "  FROM xp_transactions WHERE status='APPROVED' ";
                
        if (startDate != null) sql += " AND submitted_at >= :startDate ";
        if (endDate != null) sql += " AND submitted_at < :endDatePlusOne ";
        
        sql += "  GROUP BY student_id" +
                ") x ON x.student_id = st.id " +
                "WHERE st.department_id = :departmentId ";
                
        if (yearNo != null && !yearNo.isEmpty()) sql += " AND y.year_no = :yearNo ";
        if (stage != null) sql += " AND st.stage = :stage ";
        
        sql += " GROUP BY sec.id, sec.section_name ORDER BY AVG(COALESCE(x.net_xp, 0)) DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("departmentId", departmentId);
        if (yearNo != null && !yearNo.isEmpty()) query.setParameter("yearNo", Byte.parseByte(yearNo));
        if (stage != null) query.setParameter("stage", stage);
        if (startDate != null) query.setParameter("startDate", startDate.atStartOfDay());
        if (endDate != null) query.setParameter("endDatePlusOne", endDate.plusDays(1).atStartOfDay());

        List<Object[]> rows = query.getResultList();
        List<GroupedXpDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new GroupedXpDTO((String) row[0], ((Number) row[1]).doubleValue(), ((Number) row[2]).longValue(), ((Number) row[3]).longValue()));
        }
        return result;
    }

    @Override
    public List<XpHeatmapDTO> getMonthlyHeatmap(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT CAST(x.submitted_at AS DATE) as t_date, " +
                " SUM(CASE WHEN x.is_penalty = false THEN x.xp_points ELSE -x.xp_points END) as net_xp " +
                "FROM xp_transactions x " +
                "JOIN students st ON x.student_id = st.id " +
                "JOIN years y ON st.year_id = y.id " +
                buildWhereClause(yearNo, departmentId, stage, sectionId, startDate, endDate, true) +
                " GROUP BY CAST(x.submitted_at AS DATE) ORDER BY t_date ASC";

        Query query = entityManager.createNativeQuery(sql);
        setParameters(query, yearNo, departmentId, stage, sectionId, startDate, endDate);

        List<Object[]> rows = query.getResultList();
        List<XpHeatmapDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long total = ((Number) row[1]).longValue();
            
            Integer level = 0;
            if (total >= 101) level = 5;
            else if (total >= 51) level = 4;
            else if (total >= 26) level = 3;
            else if (total >= 11) level = 2;
            else if (total >= 1) level = 1;
            else level = 0;
            
            result.add(new XpHeatmapDTO(date, total, level));
        }
        return result;
    }

    @Override
    public List<XpTopPerformerDTO> getTopPerformers(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT st.full_name, st.reg_no, d.name as dept, sec.section_name, COALESCE(x.net_xp, 0), COALESCE(x.awd, 0), COALESCE(x.pen, 0) " +
                "FROM students st " +
                "JOIN departments d ON st.department_id = d.id " +
                "LEFT JOIN section sec ON st.section_id = sec.id " +
                "JOIN years y ON st.year_id = y.id " +
                "LEFT JOIN (" +
                "  SELECT student_id, " +
                "  SUM(CASE WHEN is_penalty = false THEN xp_points ELSE -xp_points END) as net_xp, " +
                "  SUM(CASE WHEN is_penalty = false THEN xp_points ELSE 0 END) as awd, " +
                "  SUM(CASE WHEN is_penalty = true THEN xp_points ELSE 0 END) as pen " +
                "  FROM xp_transactions WHERE status='APPROVED' ";
                
        if (startDate != null) sql += " AND submitted_at >= :startDate ";
        if (endDate != null) sql += " AND submitted_at < :endDatePlusOne ";
        
        sql += "  GROUP BY student_id" +
                ") x ON x.student_id = st.id ";
                
        List<String> conds = new ArrayList<>();
        if (yearNo != null && !yearNo.isEmpty()) conds.add("y.year_no = :yearNo");
        if (departmentId != null) conds.add("st.department_id = :departmentId");
        if (stage != null) conds.add("st.stage = :stage");
        if (sectionId != null) conds.add("st.section_id = :sectionId");
        if (!conds.isEmpty()) sql += " WHERE " + String.join(" AND ", conds);
        
        sql += " ORDER BY COALESCE(x.net_xp, 0) DESC LIMIT 100";

        Query query = entityManager.createNativeQuery(sql);
        if (yearNo != null && !yearNo.isEmpty()) query.setParameter("yearNo", Byte.parseByte(yearNo));
        if (departmentId != null) query.setParameter("departmentId", departmentId);
        if (stage != null) query.setParameter("stage", stage);
        if (sectionId != null) query.setParameter("sectionId", sectionId);
        if (startDate != null) query.setParameter("startDate", startDate.atStartOfDay());
        if (endDate != null) query.setParameter("endDatePlusOne", endDate.plusDays(1).atStartOfDay());

        List<Object[]> rows = query.getResultList();
        List<XpTopPerformerDTO> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            result.add(new XpTopPerformerDTO(rank++, (String) row[0], (String) row[1], (String) row[2], (String) row[3], 
                    ((Number) row[4]).longValue(), ((Number) row[5]).longValue(), ((Number) row[6]).longValue()));
        }
        return result;
    }

    @Override
    public List<LowXpStudentDTO> getLowXpStudents(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Long threshold) {
        String sql = "SELECT st.full_name, st.reg_no, d.name as dept, sec.section_name, COALESCE(x.net_xp, 0) " +
                "FROM students st " +
                "JOIN departments d ON st.department_id = d.id " +
                "LEFT JOIN section sec ON st.section_id = sec.id " +
                "JOIN years y ON st.year_id = y.id " +
                "LEFT JOIN (" +
                "  SELECT student_id, SUM(CASE WHEN is_penalty = false THEN xp_points ELSE -xp_points END) as net_xp " +
                "  FROM xp_transactions WHERE status='APPROVED' ";
                
        if (startDate != null) sql += " AND submitted_at >= :startDate ";
        if (endDate != null) sql += " AND submitted_at < :endDatePlusOne ";
        
        sql += "  GROUP BY student_id" +
                ") x ON x.student_id = st.id " +
                "WHERE COALESCE(x.net_xp, 0) < :threshold ";
                
        if (yearNo != null && !yearNo.isEmpty()) sql += " AND y.year_no = :yearNo ";
        if (departmentId != null) sql += " AND st.department_id = :departmentId ";
        if (stage != null) sql += " AND st.stage = :stage ";
        if (sectionId != null) sql += " AND st.section_id = :sectionId ";
        
        sql += " ORDER BY COALESCE(x.net_xp, 0) ASC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("threshold", threshold);
        if (yearNo != null && !yearNo.isEmpty()) query.setParameter("yearNo", Byte.parseByte(yearNo));
        if (departmentId != null) query.setParameter("departmentId", departmentId);
        if (stage != null) query.setParameter("stage", stage);
        if (sectionId != null) query.setParameter("sectionId", sectionId);
        if (startDate != null) query.setParameter("startDate", startDate.atStartOfDay());
        if (endDate != null) query.setParameter("endDatePlusOne", endDate.plusDays(1).atStartOfDay());

        List<Object[]> rows = query.getResultList();
        List<LowXpStudentDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long currentXp = ((Number) row[4]).longValue();
            result.add(new LowXpStudentDTO((String) row[0], (String) row[1], (String) row[2], (String) row[3], currentXp, threshold - currentXp));
        }
        return result;
    }

    @Override
    public List<ActivityXpContributionDTO> getActivityXpContribution(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, String category) {
        String sql = "SELECT x.activity_name, x.category, " +
                " SUM(CASE WHEN x.is_penalty = false THEN x.xp_points ELSE 0 END) as award, " +
                " SUM(CASE WHEN x.is_penalty = true THEN x.xp_points ELSE 0 END) as penalty " +
                "FROM xp_transactions x " +
                "JOIN students st ON x.student_id = st.id " +
                "JOIN years y ON st.year_id = y.id " +
                buildWhereClause(yearNo, departmentId, stage, sectionId, startDate, endDate, true);
                
        if (category != null && !category.isEmpty()) {
            sql += (sql.contains("WHERE") ? " AND " : " WHERE ") + " x.category = :category ";
        }
        
        sql += " GROUP BY x.activity_name, x.category ORDER BY award DESC";

        Query query = entityManager.createNativeQuery(sql);
        setParameters(query, yearNo, departmentId, stage, sectionId, startDate, endDate);
        if (category != null && !category.isEmpty()) query.setParameter("category", category);

        List<Object[]> rows = query.getResultList();
        List<ActivityXpContributionDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long award = ((Number) row[2]).longValue();
            Long penalty = ((Number) row[3]).longValue();
            result.add(new ActivityXpContributionDTO((String) row[0], (String) row[1], award, penalty, award - penalty));
        }
        return result;
    }

    @Override
    public List<XpHistoryDTO> getXpHistory(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, String activityName, String type, int limit, int offset) {
        String sql = "SELECT x.submitted_at, st.full_name, st.reg_no, d.name as dept, sec.section_name, x.activity_name, " +
                " CASE WHEN x.is_penalty = false THEN x.xp_points ELSE 0 END as awd, " +
                " CASE WHEN x.is_penalty = true THEN x.xp_points ELSE 0 END as pen, " +
                " st.total_xp, x.approved_by " +
                "FROM xp_transactions x " +
                "JOIN students st ON x.student_id = st.id " +
                "JOIN departments d ON st.department_id = d.id " +
                "LEFT JOIN section sec ON st.section_id = sec.id " +
                "JOIN years y ON st.year_id = y.id " +
                buildWhereClause(yearNo, departmentId, stage, sectionId, startDate, endDate, true);

        if (activityName != null && !activityName.isEmpty()) {
            sql += (sql.contains("WHERE") ? " AND " : " WHERE ") + " x.activity_name LIKE :activityName ";
        }
        if (type != null && !type.isEmpty()) {
            boolean isPen = type.equalsIgnoreCase("PENALTY");
            sql += (sql.contains("WHERE") ? " AND " : " WHERE ") + " x.is_penalty = " + isPen;
        }

        sql += " ORDER BY x.submitted_at DESC LIMIT :limit OFFSET :offset";

        Query query = entityManager.createNativeQuery(sql);
        setParameters(query, yearNo, departmentId, stage, sectionId, startDate, endDate);
        if (activityName != null && !activityName.isEmpty()) query.setParameter("activityName", "%" + activityName + "%");
        query.setParameter("limit", limit);
        query.setParameter("offset", offset);

        List<Object[]> rows = query.getResultList();
        List<XpHistoryDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            int awd = ((Number) row[6]).intValue();
            int pen = ((Number) row[7]).intValue();
            result.add(new XpHistoryDTO(
                row[0] != null ? ((Timestamp) row[0]).toLocalDateTime() : null,
                (String) row[1], (String) row[2], (String) row[3], (String) row[4], (String) row[5],
                awd, pen, awd - pen, ((Number) row[8]).longValue(), (String) row[9]
            ));
        }
        return result;
    }
    
    @Override
    public long getXpHistoryCount(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, String activityName, String type) {
        String sql = "SELECT COUNT(x.id) " +
                "FROM xp_transactions x " +
                "JOIN students st ON x.student_id = st.id " +
                "JOIN years y ON st.year_id = y.id " +
                buildWhereClause(yearNo, departmentId, stage, sectionId, startDate, endDate, true);

        if (activityName != null && !activityName.isEmpty()) {
            sql += (sql.contains("WHERE") ? " AND " : " WHERE ") + " x.activity_name LIKE :activityName ";
        }
        if (type != null && !type.isEmpty()) {
            boolean isPen = type.equalsIgnoreCase("PENALTY");
            sql += (sql.contains("WHERE") ? " AND " : " WHERE ") + " x.is_penalty = " + isPen;
        }

        Query query = entityManager.createNativeQuery(sql);
        setParameters(query, yearNo, departmentId, stage, sectionId, startDate, endDate);
        if (activityName != null && !activityName.isEmpty()) query.setParameter("activityName", "%" + activityName + "%");

        return ((Number) query.getSingleResult()).longValue();
    }
}
