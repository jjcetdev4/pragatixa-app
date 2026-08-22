package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.modules.student.dto.response.StudentResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import jjcet.PragatiX.modules.audit.service.AuditService;
import jjcet.PragatiX.enums.AuditAction;
import jjcet.PragatiX.enums.AuditModule;

@Service
public class StudentExportService {

    private final StudentQueryService studentQueryService;
    private final AuditService auditService;

    public StudentExportService(StudentQueryService studentQueryService, AuditService auditService) {
        this.studentQueryService = studentQueryService;
        this.auditService = auditService;
    }

    @Transactional

    public byte[] exportStudentsToExcel(String keyword, String year, Long departmentId, Long sectionId) throws IOException {
        // Fetch all matching students without pagination
        Page<StudentResponse> studentPage = studentQueryService.getAllStudents(0, Integer.MAX_VALUE, "fullName", keyword, year, departmentId, sectionId).getData();
        List<StudentResponse> students = studentPage != null ? studentPage.getContent() : List.of();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Students");

            // Header styling
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Create Headers
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Full Name", "Register Number", "Email", "Department", "Section", "Year"};
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256); // approx 20 chars
            }

            // Data rows
            int rowIdx = 1;
            for (StudentResponse s : students) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(s.getFullName() != null ? s.getFullName() : "");
                row.createCell(1).setCellValue(s.getRegNo() != null ? s.getRegNo() : "");
                row.createCell(2).setCellValue(s.getEmail() != null ? s.getEmail() : "");
                row.createCell(3).setCellValue(s.getDepartmentName() != null ? s.getDepartmentName() : "");
                row.createCell(4).setCellValue(s.getSection() != null ? s.getSection() : "");
                row.createCell(5).setCellValue(s.getYear() != null ? s.getYear() : "");
            }

            auditService.log(
                AuditAction.EXPORT, 
                AuditModule.STUDENT, 
                "Export", 
                0L, 
                "Exported " + students.size() + " students to Excel. Filters - Keyword: " + keyword + ", Year: " + year + ", DeptId: " + departmentId + ", SectionId: " + sectionId
            );

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
