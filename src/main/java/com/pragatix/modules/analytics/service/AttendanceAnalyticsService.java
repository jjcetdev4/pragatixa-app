package com.pragatix.modules.analytics.service;

import com.pragatix.entity.User;
import com.pragatix.modules.analytics.dto.*;
import com.pragatix.modules.analytics.repository.AttendanceAnalyticsRepository;
import com.pragatix.modules.authentication.security.AuthUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AttendanceAnalyticsService {

    private final AttendanceAnalyticsRepository analyticsRepository;
    private final AuthUtils authUtils;

    public AttendanceAnalyticsService(AttendanceAnalyticsRepository analyticsRepository, AuthUtils authUtils) {
        this.analyticsRepository = analyticsRepository;
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

    public AnalyticsOverviewDTO getOverview(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period) {
        String effectiveYearNo = determineYearFilter(yearNo);
        return analyticsRepository.getOverview(effectiveYearNo, departmentId, stage, sectionId, startDate, endDate, period);
    }

    public List<AttendanceTrendDTO> getTrend(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period) {
        String effectiveYearNo = determineYearFilter(yearNo);
        return analyticsRepository.getTrend(effectiveYearNo, departmentId, stage, sectionId, startDate, endDate, period);
    }

    public AttendanceDistributionDTO getDistribution(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period) {
        String effectiveYearNo = determineYearFilter(yearNo);
        return analyticsRepository.getDistribution(effectiveYearNo, departmentId, stage, sectionId, startDate, endDate, period);
    }

    public List<GroupedAttendanceDTO> getDepartmentWiseAttendance(String yearNo, LocalDate startDate, LocalDate endDate, Integer period) {
        String effectiveYearNo = determineYearFilter(yearNo);
        return analyticsRepository.getDepartmentWiseAttendance(effectiveYearNo, startDate, endDate, period);
    }

    public List<LowAttendanceStudentDTO> getLowAttendanceStudents(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period, Double threshold) {
        String effectiveYearNo = determineYearFilter(yearNo);
        return analyticsRepository.getLowAttendanceStudents(effectiveYearNo, departmentId, stage, sectionId, startDate, endDate, period, threshold);
    }

    public List<GroupedAttendanceDTO> getSectionWiseAttendance(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate, Integer period) {
        String effectiveYearNo = determineYearFilter(yearNo);
        return analyticsRepository.getSectionWiseAttendance(effectiveYearNo, departmentId, stage, startDate, endDate, period);
    }

    public List<AttendanceSummaryRowDTO> getSummaryTable(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period) {
        String effectiveYearNo = determineYearFilter(yearNo);
        return analyticsRepository.getSummaryTable(effectiveYearNo, departmentId, stage, sectionId, startDate, endDate, period);
    }

    public byte[] exportAttendanceReport(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period) {
        String effectiveYearNo = determineYearFilter(yearNo);
        List<AttendanceExportDTO> rawData = analyticsRepository.getExportData(effectiveYearNo, departmentId, stage, sectionId, startDate, endDate, period);
        
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.createSheet("Attendance Report");
            
            // Format setup
            org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.xssf.usermodel.XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            org.apache.poi.xssf.usermodel.XSSFCellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            org.apache.poi.xssf.usermodel.XSSFCellStyle deptStyle = workbook.createCellStyle();
            org.apache.poi.xssf.usermodel.XSSFFont deptFont = workbook.createFont();
            deptFont.setBold(true);
            deptFont.setFontHeightInPoints((short)14);
            deptStyle.setFont(deptFont);
            deptStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            deptStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            
            org.apache.poi.xssf.usermodel.XSSFCellStyle secStyle = workbook.createCellStyle();
            org.apache.poi.xssf.usermodel.XSSFFont secFont = workbook.createFont();
            secFont.setBold(true);
            secFont.setFontHeightInPoints((short)12);
            secStyle.setFont(secFont);

            org.apache.poi.xssf.usermodel.XSSFCellStyle pctGreenStyle = workbook.createCellStyle();
            pctGreenStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_GREEN.getIndex());
            pctGreenStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            pctGreenStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            org.apache.poi.xssf.usermodel.XSSFCellStyle pctOrangeStyle = workbook.createCellStyle();
            pctOrangeStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_ORANGE.getIndex());
            pctOrangeStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            pctOrangeStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            org.apache.poi.xssf.usermodel.XSSFCellStyle pctRedStyle = workbook.createCellStyle();
            pctRedStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.CORAL.getIndex());
            pctRedStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            pctRedStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            // Data Structures for Pivoting
            class StudentRow {
                String regNo;
                String name;
                String dept;
                String section;
                String dateRange;
                java.util.Map<Integer, String> periods = new java.util.HashMap<>();
                int presentCount = 0;
                int absentCount = 0;
                int totalCount = 0;
            }

            java.util.Map<String, java.util.Map<String, java.util.Map<String, StudentRow>>> deptSecStudentMap = new java.util.LinkedHashMap<>();
            
            String dateRangeStr = "";
            if (startDate != null && endDate != null && !startDate.equals(endDate)) {
                dateRangeStr = startDate + " to " + endDate;
            } else if (startDate != null) {
                dateRangeStr = startDate.toString();
            } else if (endDate != null) {
                dateRangeStr = endDate.toString();
            }

            for (AttendanceExportDTO row : rawData) {
                String dept = row.getDepartmentName();
                String sec = row.getSectionName() != null && !row.getSectionName().isEmpty() ? row.getSectionName() : "None";
                String reg = row.getRegNo();
                
                deptSecStudentMap.putIfAbsent(dept, new java.util.LinkedHashMap<>());
                deptSecStudentMap.get(dept).putIfAbsent(sec, new java.util.LinkedHashMap<>());
                
                StudentRow sRow = deptSecStudentMap.get(dept).get(sec).get(reg);
                if (sRow == null) {
                    sRow = new StudentRow();
                    sRow.regNo = reg;
                    sRow.name = row.getStudentName();
                    sRow.dept = dept;
                    sRow.section = sec;
                    sRow.dateRange = dateRangeStr;
                    deptSecStudentMap.get(dept).get(sec).put(reg, sRow);
                }
                
                if (row.getPeriod() != null) {
                    sRow.periods.put(row.getPeriod(), row.getStatus());
                }
                sRow.totalCount++;
                if ("PRESENT".equalsIgnoreCase(row.getStatus()) || "OD".equalsIgnoreCase(row.getStatus())) {
                    sRow.presentCount++;
                } else if ("ABSENT".equalsIgnoreCase(row.getStatus()) || "LEAVE".equalsIgnoreCase(row.getStatus())) {
                    sRow.absentCount++;
                }
            }

            int rowIdx = 0;
            String[] headers = {"S.No", "Register Number", "Student Name", "Department", "Section", "Date", "P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "Present Count", "Absent Count", "Attendance %"};

            for (java.util.Map.Entry<String, java.util.Map<String, java.util.Map<String, StudentRow>>> deptEntry : deptSecStudentMap.entrySet()) {
                String dept = deptEntry.getKey();
                
                // Department Heading
                org.apache.poi.xssf.usermodel.XSSFRow dRow = sheet.createRow(rowIdx++);
                org.apache.poi.xssf.usermodel.XSSFCell dCell = dRow.createCell(0);
                dCell.setCellValue("Department : " + dept);
                dCell.setCellStyle(deptStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx-1, rowIdx-1, 0, headers.length - 1));
                
                int deptTotalStudents = 0;
                double deptSumPct = 0;
                double deptMaxPct = -1;
                double deptMinPct = 101;

                for (java.util.Map.Entry<String, java.util.Map<String, StudentRow>> secEntry : deptEntry.getValue().entrySet()) {
                    String sec = secEntry.getKey();
                    
                    if (!"None".equals(sec)) {
                        org.apache.poi.xssf.usermodel.XSSFRow sRow = sheet.createRow(rowIdx++);
                        org.apache.poi.xssf.usermodel.XSSFCell sCell = sRow.createCell(0);
                        sCell.setCellValue("Section : " + sec);
                        sCell.setCellStyle(secStyle);
                        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx-1, rowIdx-1, 0, headers.length - 1));
                    }
                    
                    // Headers
                    org.apache.poi.xssf.usermodel.XSSFRow hRow = sheet.createRow(rowIdx++);
                    for (int i=0; i<headers.length; i++) {
                        org.apache.poi.xssf.usermodel.XSSFCell cell = hRow.createCell(i);
                        cell.setCellValue(headers[i]);
                        cell.setCellStyle(headerStyle);
                    }
                    
                    int sNo = 1;
                    for (StudentRow s : secEntry.getValue().values()) {
                        deptTotalStudents++;
                        
                        org.apache.poi.xssf.usermodel.XSSFRow dataRow = sheet.createRow(rowIdx++);
                        dataRow.createCell(0).setCellValue(sNo++);
                        dataRow.createCell(1).setCellValue(s.regNo);
                        dataRow.createCell(2).setCellValue(s.name);
                        dataRow.createCell(3).setCellValue(s.dept);
                        dataRow.createCell(4).setCellValue(s.section);
                        dataRow.createCell(5).setCellValue(s.dateRange);
                        
                        // P1 to P8
                        for (int p = 1; p <= 8; p++) {
                            org.apache.poi.xssf.usermodel.XSSFCell pCell = dataRow.createCell(5 + p);
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
                        
                        org.apache.poi.xssf.usermodel.XSSFCell pctCell = dataRow.createCell(16);
                        pctCell.setCellValue(String.format("%.2f%%", pct));
                        if (pct >= 95) pctCell.setCellStyle(pctGreenStyle);
                        else if (pct >= 75) pctCell.setCellStyle(pctOrangeStyle);
                        else pctCell.setCellStyle(pctRedStyle);
                    }
                }
                
                // Department Summary
                rowIdx++; // Empty row
                org.apache.poi.xssf.usermodel.XSSFRow summaryHeader = sheet.createRow(rowIdx++);
                summaryHeader.createCell(0).setCellValue("Department Summary");
                summaryHeader.getCell(0).setCellStyle(secStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx-1, rowIdx-1, 0, 3));
                
                org.apache.poi.xssf.usermodel.XSSFRow sumRow1 = sheet.createRow(rowIdx++);
                sumRow1.createCell(0).setCellValue("Total Students:");
                sumRow1.createCell(1).setCellValue(deptTotalStudents);
                
                double avgPct = deptTotalStudents > 0 ? (deptSumPct / deptTotalStudents) : 0;
                org.apache.poi.xssf.usermodel.XSSFRow sumRow2 = sheet.createRow(rowIdx++);
                sumRow2.createCell(0).setCellValue("Average Attendance %:");
                sumRow2.createCell(1).setCellValue(String.format("%.2f%%", avgPct));
                
                org.apache.poi.xssf.usermodel.XSSFRow sumRow3 = sheet.createRow(rowIdx++);
                sumRow3.createCell(0).setCellValue("Highest Attendance %:");
                sumRow3.createCell(1).setCellValue(deptMaxPct != -1 ? String.format("%.2f%%", deptMaxPct) : "0.00%");
                
                org.apache.poi.xssf.usermodel.XSSFRow sumRow4 = sheet.createRow(rowIdx++);
                sumRow4.createCell(0).setCellValue("Lowest Attendance %:");
                sumRow4.createCell(1).setCellValue(deptMinPct != 101 ? String.format("%.2f%%", deptMinPct) : "0.00%");
                
                rowIdx++; // Empty row
            }

            sheet.createFreezePane(0, 1);
            for(int i=0; i<headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }
}
