package com.pragatix.modules.analytics.service;

import com.pragatix.entity.User;
import com.pragatix.modules.analytics.dto.*;
import com.pragatix.modules.analytics.repository.XpAnalyticsRepository;
import com.pragatix.modules.authentication.security.AuthUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
public class XpAnalyticsService {

    private final XpAnalyticsRepository xpAnalyticsRepository;
    private final AuthUtils authUtils;

    public XpAnalyticsService(XpAnalyticsRepository xpAnalyticsRepository, AuthUtils authUtils) {
        this.xpAnalyticsRepository = xpAnalyticsRepository;
        this.authUtils = authUtils;
    }

    private String determineYearFilter(String providedYearNo) {
        User user = authUtils.getCurrentUser();
        if (user != null) {
            if (authUtils.isAdmin(user) && !authUtils.isSuperAdmin(user)) {
                return AuthUtils.getAssignedYearString(user.getAcademicYear());
            }
        }
        return providedYearNo;
    }




    public List<XpAwardVsPenaltyDTO> getAwardVsPenalty(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate) {
        return xpAnalyticsRepository.getAwardVsPenalty(determineYearFilter(yearNo), departmentId, stage, sectionId, startDate, endDate);
    }

    public List<GroupedXpDTO> getDepartmentRanking(String yearNo, Integer stage, LocalDate startDate, LocalDate endDate) {
        return xpAnalyticsRepository.getDepartmentRanking(determineYearFilter(yearNo), stage, startDate, endDate);
    }

    public List<GroupedXpDTO> getSectionRanking(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate) {
        return xpAnalyticsRepository.getSectionRanking(determineYearFilter(yearNo), departmentId, stage, startDate, endDate);
    }

    public List<XpHeatmapDTO> getMonthlyHeatmap(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate) {
        return xpAnalyticsRepository.getMonthlyHeatmap(determineYearFilter(yearNo), departmentId, stage, sectionId, startDate, endDate);
    }

    public List<XpTopPerformerDTO> getTopPerformers(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate) {
        return xpAnalyticsRepository.getTopPerformers(determineYearFilter(yearNo), departmentId, stage, sectionId, startDate, endDate);
    }

    public List<LowXpStudentDTO> getLowXpStudents(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Long threshold) {
        return xpAnalyticsRepository.getLowXpStudents(determineYearFilter(yearNo), departmentId, stage, sectionId, startDate, endDate, threshold);
    }

    public List<ActivityXpContributionDTO> getActivityXpContribution(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, String category) {
        return xpAnalyticsRepository.getActivityXpContribution(determineYearFilter(yearNo), departmentId, stage, sectionId, startDate, endDate, category);
    }

    public List<XpHistoryDTO> getXpHistory(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, String activityName, String type, int limit, int offset) {
        return xpAnalyticsRepository.getXpHistory(determineYearFilter(yearNo), departmentId, stage, sectionId, startDate, endDate, activityName, type, limit, offset);
    }
    
    public long getXpHistoryCount(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, String activityName, String type) {
        return xpAnalyticsRepository.getXpHistoryCount(determineYearFilter(yearNo), departmentId, stage, sectionId, startDate, endDate, activityName, type);
    }

    public byte[] exportXpHistory(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, String activityName, String type) {
        // Fetch all data for export (no pagination limit)
        List<XpHistoryDTO> data = getXpHistory(yearNo, departmentId, stage, sectionId, startDate, endDate, activityName, type, Integer.MAX_VALUE, 0);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("XP History");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] columns = {"Date", "Student", "Register No", "Department", "Section", "Activity", "Award XP", "Penalty XP", "Net XP", "Current Total XP", "Approved By"};
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (XpHistoryDTO dto : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getDate() != null ? dto.getDate().toLocalDate().toString() : "");
                row.createCell(1).setCellValue(dto.getStudentName() != null ? dto.getStudentName() : "");
                row.createCell(2).setCellValue(dto.getRegisterNumber() != null ? dto.getRegisterNumber() : "");
                row.createCell(3).setCellValue(dto.getDepartment() != null ? dto.getDepartment() : "");
                row.createCell(4).setCellValue(dto.getSection() != null ? dto.getSection() : "");
                row.createCell(5).setCellValue(dto.getActivityName() != null ? dto.getActivityName() : "");
                row.createCell(6).setCellValue(dto.getAwardXp() != null ? dto.getAwardXp() : 0);
                row.createCell(7).setCellValue(dto.getPenaltyXp() != null ? dto.getPenaltyXp() : 0);
                row.createCell(8).setCellValue(dto.getNetXp() != null ? dto.getNetXp() : 0);
                row.createCell(9).setCellValue(dto.getCurrentTotalXp() != null ? dto.getCurrentTotalXp() : 0);
                row.createCell(10).setCellValue(dto.getApprovedBy() != null ? dto.getApprovedBy() : "");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate XP history report", e);
        }
    }
}
