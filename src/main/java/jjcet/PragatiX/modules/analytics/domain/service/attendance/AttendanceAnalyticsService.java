package jjcet.PragatiX.modules.analytics.domain.service.attendance;

import jjcet.PragatiX.modules.analytics.api.dto.request.AttendanceFilter;
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
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.DepartmentXpAttendanceDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jjcet.PragatiX.modules.analytics.infrastructure.repository.AnalyticsViewRepository;

@Service
public class AttendanceAnalyticsService {

    private final AnalyticsViewRepository repository;

    public AttendanceAnalyticsService(AnalyticsViewRepository repository) {
        this.repository = repository;
    }

    public AnalyticsOverviewDto getOverview(AttendanceFilter filter) {
        return repository.findAttendanceOverview(filter);
    }

    public List<AttendanceTrendDto> getTrend(AttendanceFilter filter) {
        return repository.findAttendanceTrends(filter);
    }

    public AttendanceDistributionDto getDistribution(AttendanceFilter filter) {
        return repository.findAttendanceDistribution(filter);
    }

    public List<GroupedAttendanceDto> getDepartmentWiseAttendance(AttendanceFilter filter) {
        return repository.findDepartmentWiseAttendance(filter);
    }

    public List<LowAttendanceStudentDto> getLowAttendanceStudents(AttendanceFilter filter) {
        return repository.findLowAttendanceStudents(filter);
    }

    public List<GroupedAttendanceDto> getSectionWiseAttendance(AttendanceFilter filter) {
        return repository.findSectionWiseAttendance(filter);
    }

    public List<AttendanceSummaryRowDto> getSummaryTable(AttendanceFilter filter) {
        return repository.findAttendanceSummaryTable(filter);
    }

    public List<AttendanceCalendarDto> getAttendanceCalendar(AttendanceFilter filter) {
        return repository.findAttendanceCalendar(filter);
    }

    public AttendanceSummaryDto getAttendanceSummaryByDate(AttendanceFilter filter) {
        return repository.findAttendanceSummaryByDate(filter);
    }

    public List<DepartmentXpAttendanceDto> getMonthlyAttendanceByDepartment(AttendanceFilter filter) {
        return repository.findMonthlyAttendanceByDepartment(filter);
    }

    public List<AttendanceDto> getAttendanceTrend(AttendanceFilter filter) {
        return repository.findAttendanceTrend(filter);
    }

    public byte[] exportAttendanceReport(AttendanceFilter filter) {
        List<AttendanceExportDto> rawData = repository.findAttendanceExportData(filter);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Attendance Report");

            XSSFCellStyle headerStyle = workbook.createCellStyle();
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            XSSFCellStyle deptStyle = workbook.createCellStyle();
            XSSFFont deptFont = workbook.createFont();
            deptFont.setBold(true);
            deptFont.setFontHeightInPoints((short) 14);
            deptStyle.setFont(deptFont);
            deptStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            deptStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle secStyle = workbook.createCellStyle();
            XSSFFont secFont = workbook.createFont();
            secFont.setBold(true);
            secFont.setFontHeightInPoints((short) 12);
            secStyle.setFont(secFont);

            XSSFCellStyle pctGreen = workbook.createCellStyle();
            pctGreen.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            pctGreen.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            pctGreen.setAlignment(HorizontalAlignment.CENTER);

            XSSFCellStyle pctOrange = workbook.createCellStyle();
            pctOrange.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
            pctOrange.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            pctOrange.setAlignment(HorizontalAlignment.CENTER);

            XSSFCellStyle pctRed = workbook.createCellStyle();
            pctRed.setFillForegroundColor(IndexedColors.CORAL.getIndex());
            pctRed.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            pctRed.setAlignment(HorizontalAlignment.CENTER);

            class StudentRow {
                String regNo;
                String name;
                String dept;
                String section;
                String dateRange;
                Map<Integer, String> periods = new HashMap<>();
                int presentCount = 0;
                int absentCount = 0;
                int totalCount = 0;
            }

            Map<String, Map<String, Map<String, StudentRow>>> deptSecStudentMap = new LinkedHashMap<>();

            String dateRangeStr = "";
            if (filter.startDate() != null && filter.endDate() != null && !filter.startDate().equals(filter.endDate())) {
                dateRangeStr = filter.startDate() + " to " + filter.endDate();
            } else if (filter.startDate() != null) {
                dateRangeStr = filter.startDate().toString();
            } else if (filter.endDate() != null) {
                dateRangeStr = filter.endDate().toString();
            }

            for (AttendanceExportDto row : rawData) {
                String dept = row.departmentName();
                String sec = row.sectionName() != null && !row.sectionName().isEmpty() ? row.sectionName() : "None";
                String reg = row.registerNumber();

                deptSecStudentMap.putIfAbsent(dept, new LinkedHashMap<>());
                deptSecStudentMap.get(dept).putIfAbsent(sec, new LinkedHashMap<>());

                StudentRow sRow = deptSecStudentMap.get(dept).get(sec).get(reg);
                if (sRow == null) {
                    sRow = new StudentRow();
                    sRow.regNo = reg;
                    sRow.name = row.studentName();
                    sRow.dept = dept;
                    sRow.section = sec;
                    sRow.dateRange = dateRangeStr;
                    deptSecStudentMap.get(dept).get(sec).put(reg, sRow);
                }

                if (row.period() != null) {
                    sRow.periods.put(row.period(), row.status());
                }
                sRow.totalCount++;
                if ("PRESENT".equalsIgnoreCase(row.status()) || "OD".equalsIgnoreCase(row.status())) {
                    sRow.presentCount++;
                } else if ("ABSENT".equalsIgnoreCase(row.status()) || "LEAVE".equalsIgnoreCase(row.status())) {
                    sRow.absentCount++;
                }
            }

            int rowIdx = 0;
            String[] headers = {"S.No", "Register Number", "Student Name", "Department", "Section", "Date",
                    "P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8",
                    "Present Count", "Absent Count", "Attendance %"};

            for (Map.Entry<String, Map<String, Map<String, StudentRow>>> deptEntry : deptSecStudentMap.entrySet()) {
                String dept = deptEntry.getKey();
                XSSFRow dRow = sheet.createRow(rowIdx++);
                XSSFCell dCell = dRow.createCell(0);
                dCell.setCellValue("Department : " + dept);
                dCell.setCellStyle(deptStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, headers.length - 1));

                int deptTotalStudents = 0;
                double deptSumPct = 0;
                double deptMaxPct = -1;
                double deptMinPct = 101;

                for (Map.Entry<String, Map<String, StudentRow>> secEntry : deptEntry.getValue().entrySet()) {
                    String sec = secEntry.getKey();
                    if (!"None".equals(sec)) {
                        XSSFRow sRow = sheet.createRow(rowIdx++);
                        XSSFCell sCell = sRow.createCell(0);
                        sCell.setCellValue("Section : " + sec);
                        sCell.setCellStyle(secStyle);
                        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, headers.length - 1));
                    }

                    XSSFRow hRow = sheet.createRow(rowIdx++);
                    for (int i = 0; i < headers.length; i++) {
                        XSSFCell cell = hRow.createCell(i);
                        cell.setCellValue(headers[i]);
                        cell.setCellStyle(headerStyle);
                    }

                    int sNo = 1;
                    for (StudentRow s : secEntry.getValue().values()) {
                        deptTotalStudents++;
                        XSSFRow dataRow = sheet.createRow(rowIdx++);
                        dataRow.createCell(0).setCellValue(sNo++);
                        dataRow.createCell(1).setCellValue(s.regNo);
                        dataRow.createCell(2).setCellValue(s.name);
                        dataRow.createCell(3).setCellValue(s.dept);
                        dataRow.createCell(4).setCellValue(s.section);
                        dataRow.createCell(5).setCellValue(s.dateRange);

                        for (int p = 1; p <= 8; p++) {
                            XSSFCell pCell = dataRow.createCell(5 + p);
                            String status = s.periods.get(p);
                            if (status == null) status = "-";
                            else if ("PRESENT".equalsIgnoreCase(status)) status = "P";
                            else if ("ABSENT".equalsIgnoreCase(status)) status = "A";
                            else if ("LEAVE".equalsIgnoreCase(status)) status = "L";
                            pCell.setCellValue(status);
                            pCell.setCellStyle(centerStyle);
                        }

                        dataRow.createCell(14).setCellValue(s.presentCount);
                        dataRow.getCell(14).setCellStyle(centerStyle);
                        dataRow.createCell(15).setCellValue(s.absentCount);
                        dataRow.getCell(15).setCellStyle(centerStyle);

                        double pct = 0;
                        if ((s.presentCount + s.absentCount) > 0) {
                            pct = (s.presentCount * 100.0) / (s.presentCount + s.absentCount);
                        }
                        deptSumPct += pct;
                        if (pct > deptMaxPct) deptMaxPct = pct;
                        if (pct < deptMinPct) deptMinPct = pct;

                        XSSFCell pctCell = dataRow.createCell(16);
                        pctCell.setCellValue(String.format("%.2f%%", pct));
                        if (pct >= 95) pctCell.setCellStyle(pctGreen);
                        else if (pct >= 75) pctCell.setCellStyle(pctOrange);
                        else pctCell.setCellStyle(pctRed);
                    }
                }

                rowIdx++;
                XSSFRow summaryHeader = sheet.createRow(rowIdx++);
                summaryHeader.createCell(0).setCellValue("Department Summary");
                summaryHeader.getCell(0).setCellStyle(secStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 3));

                XSSFRow sumRow1 = sheet.createRow(rowIdx++);
                sumRow1.createCell(0).setCellValue("Total Students:");
                sumRow1.createCell(1).setCellValue(deptTotalStudents);

                double avgPct = deptTotalStudents > 0 ? (deptSumPct / deptTotalStudents) : 0;
                XSSFRow sumRow2 = sheet.createRow(rowIdx++);
                sumRow2.createCell(0).setCellValue("Average Attendance %:");
                sumRow2.createCell(1).setCellValue(String.format("%.2f%%", avgPct));

                XSSFRow sumRow3 = sheet.createRow(rowIdx++);
                sumRow3.createCell(0).setCellValue("Highest Attendance %:");
                sumRow3.createCell(1).setCellValue(deptMaxPct != -1 ? String.format("%.2f%%", deptMaxPct) : "0.00%");

                XSSFRow sumRow4 = sheet.createRow(rowIdx++);
                sumRow4.createCell(0).setCellValue("Lowest Attendance %:");
                sumRow4.createCell(1).setCellValue(deptMinPct != 101 ? String.format("%.2f%%", deptMinPct) : "0.00%");

                rowIdx++;
            }

            sheet.createFreezePane(0, 1);
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }
}