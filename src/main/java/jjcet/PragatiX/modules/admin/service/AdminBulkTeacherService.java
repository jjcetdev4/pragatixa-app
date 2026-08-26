package jjcet.PragatiX.modules.admin.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.Department;
import jjcet.PragatiX.entity.Section;
import jjcet.PragatiX.modules.authentication.dto.request.CreateUserRequest;
import jjcet.PragatiX.repository.DepartmentRepository;
import jjcet.PragatiX.repository.SectionRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class AdminBulkTeacherService {
    private static final Logger log = LoggerFactory.getLogger(AdminBulkTeacherService.class);

    private final AdminUserService adminUserService;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;


    public AdminBulkTeacherService(AdminUserService adminUserService, DepartmentRepository departmentRepository, SectionRepository sectionRepository) {
        this.adminUserService = adminUserService;
        this.departmentRepository = departmentRepository;
        this.sectionRepository = sectionRepository;
    }

    public byte[] generateTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Instructions Sheet
            Sheet instructionsSheet = workbook.createSheet("Instructions");
            Row titleRow = instructionsSheet.createRow(0);
            titleRow.createCell(0).setCellValue("Bulk Teacher Upload Instructions");
            
            String[] instructions = {
                "1. Do not change column headers in the 'Teachers' sheet.",
                "2. Add one teacher per row.",
                "3. Username must be unique across the system.",
                "4. Email must be valid and unique.",
                "5. Department must be selected from the dropdown.",
                "6. Sub Role can be HOD, CC, Other, or blank.",
                "7. Section is optional for CC (e.g. A, B).",
                "8. HOD does not require a Section.",
                "9. Normal Teacher does not require a Section.",
                "10. Sections are automatically created for CC when needed.",
                "11. Do not manually create SQL records.",
                "12. Save as .xlsx before upload.",
                "13. CC requires a Year (I, II, III, IV). HOD does not require a Year."
            };
            
            for (int i = 0; i < instructions.length; i++) {
                Row row = instructionsSheet.createRow(i + 2);
                row.createCell(0).setCellValue(instructions[i]);
            }
            instructionsSheet.autoSizeColumn(0);

            // Teachers Data Sheet
            Sheet sheet = workbook.createSheet("Teachers");
            
            // Headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Full Name", "Email", "Department", "Sub Role", "Section", "Year"};
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Sample Row (Optional, clearly marked as sample)
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("SAMPLE TEACHER");
            sampleRow.createCell(1).setCellValue("sample@example.com");
            sampleRow.createCell(2).setCellValue("Computer Science and Engineering");
            sampleRow.createCell(3).setCellValue("CC");
            sampleRow.createCell(4).setCellValue("A");
            sampleRow.createCell(5).setCellValue("I");
            
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Create hidden _Lists sheet
            Sheet listSheet = workbook.createSheet("_Lists");
            workbook.setSheetHidden(workbook.getSheetIndex("_Lists"), true);

            // Fetch active departments, sort alphabetically
            List<Department> dbDepartments = departmentRepository.findAll();
            List<String> deptNames = new ArrayList<>();
            for (Department d : dbDepartments) {
                if (d.getDeptName() != null && !d.getDeptName().trim().isEmpty()) {
                    deptNames.add(d.getDeptName().trim());
                }
            }
            if (deptNames.isEmpty()) {
                // If there are no departments in the DB, the list remains empty.
            }
            Collections.sort(deptNames, String.CASE_INSENSITIVE_ORDER);

            // Populate _Lists sheet
            int deptCount = deptNames.size();
            for (int i = 0; i < deptCount; i++) {
                Row row = listSheet.getRow(i);
                if (row == null) row = listSheet.createRow(i);
                row.createCell(0).setCellValue(deptNames.get(i));
            }

            String[] subRoles = {"Other", "HOD", "CC"};
            for (int i = 0; i < subRoles.length; i++) {
                Row row = listSheet.getRow(i);
                if (row == null) row = listSheet.createRow(i);
                row.createCell(1).setCellValue(subRoles[i]);
            }

            String[] years = {"I", "II", "III", "IV"};
            for (int i = 0; i < years.length; i++) {
                Row row = listSheet.getRow(i);
                if (row == null) row = listSheet.createRow(i);
                row.createCell(2).setCellValue(years[i]);
            }

            // Create Named Ranges
            Name deptNameRange = workbook.createName();
            deptNameRange.setNameName("DepartmentList");
            deptNameRange.setRefersToFormula("_Lists!$A$1:$A$" + Math.max(1, deptCount));

            Name subRoleNameRange = workbook.createName();
            subRoleNameRange.setNameName("SubRoleList");
            subRoleNameRange.setRefersToFormula("_Lists!$B$1:$B$3");

            Name yearNameRange = workbook.createName();
            yearNameRange.setNameName("YearList");
            yearNameRange.setRefersToFormula("_Lists!$C$1:$C$4");

            // Data Validation (Dropdowns)
            DataValidationHelper validationHelper = sheet.getDataValidationHelper();
            
            // Department Dropdown (Col 2)
            CellRangeAddressList deptAddressList = new CellRangeAddressList(1, 1000, 2, 2);
            DataValidationConstraint deptConstraint = validationHelper.createFormulaListConstraint("DepartmentList");
            DataValidation deptValidation = validationHelper.createValidation(deptConstraint, deptAddressList);
            deptValidation.setSuppressDropDownArrow(true);
            deptValidation.setShowErrorBox(true);
            sheet.addValidationData(deptValidation);
            
            // Sub Role Dropdown (Col 3)
            CellRangeAddressList subRoleAddressList = new CellRangeAddressList(1, 1000, 3, 3);
            DataValidationConstraint subRoleConstraint = validationHelper.createFormulaListConstraint("SubRoleList");
            DataValidation subRoleValidation = validationHelper.createValidation(subRoleConstraint, subRoleAddressList);
            subRoleValidation.setSuppressDropDownArrow(true);
            subRoleValidation.setShowErrorBox(true);
            // Allow blank explicitly for DataValidation
            subRoleValidation.setEmptyCellAllowed(true);
            sheet.addValidationData(subRoleValidation);

            // Year Dropdown (Col 5)
            CellRangeAddressList yearAddressList = new CellRangeAddressList(1, 1000, 5, 5);
            DataValidationConstraint yearConstraint = validationHelper.createFormulaListConstraint("YearList");
            DataValidation yearValidation = validationHelper.createValidation(yearConstraint, yearAddressList);
            yearValidation.setSuppressDropDownArrow(true);
            yearValidation.setShowErrorBox(true);
            yearValidation.setEmptyCellAllowed(true);
            sheet.addValidationData(yearValidation);
            
            sheet.createFreezePane(0, 1);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public ApiResponse<List<CreateUserRequest>> bulkParse(MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error("File is empty.");
        }

        List<String> errors = new ArrayList<>();
        List<CreateUserRequest> validRequests = new ArrayList<>();
        Set<String> excelEmails = new HashSet<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheet("Teachers");
            if (sheet == null) {
                sheet = workbook.getSheetAt(0); 
            }

            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                return ApiResponse.error("No data found in the Excel file.");
            }

            Row headerRow = rowIterator.next();
            Map<String, Integer> columnMap = new HashMap<>();
            for (Cell cell : headerRow) {
                if (cell != null && cell.getStringCellValue() != null) {
                    columnMap.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
                }
            }

            if (!columnMap.containsKey("Full Name") || !columnMap.containsKey("Email") || !columnMap.containsKey("Department")) {
                return ApiResponse.error("Invalid template. Please download the latest template.");
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                int rowNum = row.getRowNum() + 1;

                String fullName = getCellValue(row, columnMap.get("Full Name"));
                if (fullName != null) {
                    fullName = fullName.trim().toUpperCase();
                }
                String email = getCellValue(row, columnMap.get("Email"));
                String deptName = getCellValue(row, columnMap.get("Department"));
                String subRole = columnMap.containsKey("Sub Role") ? getCellValue(row, columnMap.get("Sub Role")) : "";
                String sectionName = columnMap.containsKey("Section") ? getCellValue(row, columnMap.get("Section")).toUpperCase() : "";
                String yearStr = columnMap.containsKey("Year") ? getCellValue(row, columnMap.get("Year")) : "";

                if (fullName.isEmpty() && email.isEmpty() && deptName.isEmpty()) {
                    continue;
                }
                
                if (email.equals("sample@example.com")) {
                    continue;
                }

                if (fullName.isEmpty() || email.isEmpty() || deptName.isEmpty()) {
                    errors.add("Row " + rowNum + ": Missing required fields (Full Name, Email, or Department).");
                    continue;
                }
                
                if (excelEmails.contains(email)) {
                    errors.add("Row " + rowNum + ": Duplicate email '" + email + "' in uploaded file.");
                    continue;
                }
                excelEmails.add(email);

                if (subRole.equalsIgnoreCase("None") || subRole.equalsIgnoreCase("Other")) {
                    subRole = "";
                }
                
                if (!subRole.isEmpty() && !subRole.equalsIgnoreCase("HOD") && !subRole.equalsIgnoreCase("CC")) {
                    errors.add("Row " + rowNum + ": Invalid Sub Role '" + subRole + "'. Only HOD, CC, or Other are allowed.");
                    continue;
                }

                Optional<Department> optDept = departmentRepository.findByCode(deptName.trim());
                if (optDept.isEmpty()) {
                    optDept = departmentRepository.findByName(deptName.trim());
                }
                
                if (optDept.isEmpty()) {
                    errors.add("Row " + rowNum + ": Invalid department.");
                    continue;
                }
                
                Department department = optDept.get();
                boolean supportsSections = Boolean.TRUE.equals(department.getSupportsSections());
                
                boolean isCC = subRole.equalsIgnoreCase("CC");
                boolean isHOD = subRole.equalsIgnoreCase("HOD");
                
                if (isCC && yearStr.isEmpty()) {
                    errors.add("Row " + rowNum + ": Year is required for CC.");
                    continue;
                }

                if (isCC && !yearStr.isEmpty()) {
                    jjcet.PragatiX.enums.AcademicYear acYear = jjcet.PragatiX.enums.AcademicYear.fromString(yearStr);
                    if (acYear == null) {
                        errors.add("Row " + rowNum + ": Invalid year. Allowed values are I, II, III and IV.");
                        continue;
                    }
                }
                
                if (!isCC && !sectionName.isEmpty()) {
                    if (isHOD) {
                        errors.add("Row " + rowNum + ": HOD cannot have a Section.");
                    } else {
                        errors.add("Row " + rowNum + ": Section can only be assigned to CC.");
                    }
                    continue;
                }

                if (isHOD && !yearStr.isEmpty()) {
                    errors.add("Row " + rowNum + ": Year should be blank for HOD.");
                    continue;
                }
                
                if (!isCC && !isHOD && !yearStr.isEmpty()) {
                    errors.add("Row " + rowNum + ": Year can only be assigned to CC.");
                    continue;
                }

                CreateUserRequest request = new CreateUserRequest();
                request.setFullName(fullName);
                request.setEmail(email);
                request.setDepartmentId(department.getId());
                request.setRoles(new HashSet<>(Collections.singletonList("ROLE_TEACHER")));
                
                if (!yearStr.isEmpty()) {
                    request.setYear(yearStr);
                }
                
                if (!subRole.isEmpty()) {
                    request.setSubRoles(new HashSet<>(Collections.singletonList(subRole.toUpperCase())));
                }

                // If CC, resolve or create section.
                if (isCC && !sectionName.isEmpty()) {
                    // Check if section exists
                    Section section = sectionRepository.findByDepartmentAndSectionName(department, sectionName).orElse(null);
                            
                    if (section == null) {
                        // Auto-create section (Only for CC under section-supported department)
                        section = new Section();
                        section.setDepartment(department);
                        section.setSectionName(sectionName);
                        section = sectionRepository.save(section);
                    }
                    request.setSectionId(section.getId());
                }

                request.setPassword(UUID.randomUUID().toString().substring(0, 8));
                
                validRequests.add(request);
            }

        } catch (Exception e) {
            log.error("Failed to parse bulk upload Excel file", e);
            return ApiResponse.error("Failed to parse the Excel file: " + e.getMessage());
        }

        String errorStr = String.join("; ", errors);
        if (validRequests.isEmpty() && !errors.isEmpty()) {
            return new ApiResponse<List<CreateUserRequest>>(false, "No valid records found.", errorStr, validRequests);
        }

        return new ApiResponse<List<CreateUserRequest>>(true, "Parsed " + validRequests.size() + " valid teachers. Found " + errors.size() + " errors.", errorStr, validRequests);
    }

    public ApiResponse<String> bulkImport(List<CreateUserRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return ApiResponse.error("No data provided for import.");
        }

        int successCount = 0;
        List<String> importErrors = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            CreateUserRequest req = requests.get(i);
            try {
                if (req.getPassword() == null || req.getPassword().trim().isEmpty()) {
                    req.setPassword("Spdms@123");
                }
                if (req.getRoles() == null || req.getRoles().isEmpty()) {
                    req.setRoles(new HashSet<>(Collections.singletonList("ROLE_TEACHER")));
                }
                ResponseEntity<ApiResponse<jjcet.PragatiX.modules.authentication.dto.response.UserResponse>> responseEntity = adminUserService.createUser(req);
                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    successCount++;
                } else {
                    String errMsg = "Row " + (i + 1) + " (" + req.getEmail() + "): ";
                    if (responseEntity.getBody() != null && responseEntity.getBody().getMessage() != null) {
                        errMsg += responseEntity.getBody().getMessage();
                    } else {
                        errMsg += "Failed to create user.";
                    }
                    importErrors.add(errMsg);
                }
            } catch (Exception e) {
                importErrors.add("Row " + (i + 1) + " (" + req.getEmail() + "): " + e.getMessage());
            }
        }

        String importErrorStr = String.join("; ", importErrors);
        if (successCount == 0 && !importErrors.isEmpty()) {
            return new ApiResponse<String>(false, "Bulk import failed completely.", importErrorStr, null);
        }

        return new ApiResponse<String>(true, "Bulk import processed: " + successCount + " teachers created.", importErrorStr, null);
    }

    private String getCellValue(Row row, Integer cellIndex) {
        if (cellIndex == null) return "";
        Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }
}
