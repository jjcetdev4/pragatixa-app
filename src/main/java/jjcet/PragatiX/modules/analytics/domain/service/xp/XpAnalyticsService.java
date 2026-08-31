package jjcet.PragatiX.modules.analytics.domain.service.xp;

import jjcet.PragatiX.modules.analytics.api.dto.request.XpFilter;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.ActivityXpContributionDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.DepartmentXpAttendanceDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.EliteTeamDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.GroupedXpDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.LowXpStudentDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.TopPerformerDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpAwardVsPenaltyDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpDistributionDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpHeatmapDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpHistoryDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpTopPerformerDto;
import jjcet.PragatiX.modules.analytics.infrastructure.repository.AnalyticsViewRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class XpAnalyticsService {

    private final AnalyticsViewRepository repository;

    public XpAnalyticsService(AnalyticsViewRepository repository) {
        this.repository = repository;
    }

    public List<XpAwardVsPenaltyDto> getAwardVsPenalty(XpFilter filter) {
        return repository.findAwardVsPenalty(filter);
    }

    public List<GroupedXpDto> getDepartmentRanking(XpFilter filter) {
        return repository.findDepartmentRanking(filter);
    }

    public List<GroupedXpDto> getSectionRanking(XpFilter filter) {
        return repository.findSectionRanking(filter);
    }

    public List<XpHeatmapDto> getMonthlyHeatmap(XpFilter filter) {
        return repository.findMonthlyHeatmap(filter);
    }

    public List<XpTopPerformerDto> getTopPerformers(XpFilter filter) {
        return repository.findXpTopPerformers(filter);
    }

    public List<LowXpStudentDto> getLowXpStudents(XpFilter filter) {
        return repository.findLowXpStudents(filter);
    }

    public List<ActivityXpContributionDto> getActivityXpContribution(XpFilter filter) {
        return repository.findActivityXpContribution(filter);
    }

    public List<XpHistoryDto> getXpHistory(XpFilter filter) {
        int limit = filter.size() > 0 ? filter.size() : 20;
        int offset = filter.page() * limit;
        return repository.findXpHistory(XpFilter.builder()
                .academicYear(filter.academicYear())
                .departmentId(filter.departmentId())
                .stageId(filter.stageId())
                .sectionId(filter.sectionId())
                .startDate(filter.startDate())
                .endDate(filter.endDate())
                .category(filter.category())
                .type(filter.type())
                .size(limit)
                .page(0)
                .build());
    }

    public long getXpHistoryCount(XpFilter filter) {
        return repository.countXpHistory(filter);
    }

    public byte[] exportXpHistory(XpFilter filter) {
        XpFilter exportFilter = XpFilter.builder()
                .academicYear(filter.academicYear())
                .departmentId(filter.departmentId())
                .stageId(filter.stageId())
                .sectionId(filter.sectionId())
                .startDate(filter.startDate())
                .endDate(filter.endDate())
                .category(filter.category())
                .type(filter.type())
                .size(Integer.MAX_VALUE)
                .page(0)
                .build();
        List<XpHistoryDto> data = repository.findXpHistory(exportFilter);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("XP History");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] columns = {"Date", "Student", "Register No", "Department", "Section",
                    "Activity", "Award XP", "Penalty XP", "Net XP", "Current Total XP", "Approved By"};

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (XpHistoryDto dto : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.date() != null ? dto.date().toLocalDate().toString() : "");
                row.createCell(1).setCellValue(dto.studentName() != null ? dto.studentName() : "");
                row.createCell(2).setCellValue(dto.registerNumber() != null ? dto.registerNumber() : "");
                row.createCell(3).setCellValue(dto.department() != null ? dto.department() : "");
                row.createCell(4).setCellValue(dto.section() != null ? dto.section() : "");
                row.createCell(5).setCellValue(dto.activityName() != null ? dto.activityName() : "");
                row.createCell(6).setCellValue(dto.awardXp() != null ? dto.awardXp() : 0);
                row.createCell(7).setCellValue(dto.penaltyXp() != null ? dto.penaltyXp() : 0);
                row.createCell(8).setCellValue(dto.netXp() != null ? dto.netXp() : 0);
                row.createCell(9).setCellValue(dto.currentTotalXp() != null ? dto.currentTotalXp() : 0);
                row.createCell(10).setCellValue(dto.approvedBy() != null ? dto.approvedBy() : "");
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

    public List<XpDistributionDto> getXpDistribution(String yearNo) {
        return repository.findXpDistribution(yearNo);
    }

    public List<DepartmentXpAttendanceDto> getMonthlyAvgXpByDepartment(String yearNo) {
        return repository.findMonthlyAvgXpByDepartment(yearNo);
    }

    public List<DepartmentXpAttendanceDto> getAllTimeAvgXpByDepartment(String yearNo) {
        return repository.findAllTimeAvgXpByDepartment(yearNo);
    }

    public List<TopPerformerDto> getTopPerformersNew(String yearNo, int limit) {
        return repository.findTopPerformersNew(yearNo, limit);
    }

    public List<EliteTeamDto> getTopEliteTeams(String yearNo, int limit) {
        return repository.findEliteTeams(yearNo, limit);
    }
}