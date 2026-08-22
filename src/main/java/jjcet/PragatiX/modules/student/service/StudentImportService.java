package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.dto.*;
import jjcet.PragatiX.modules.activity.dto.request.*;
import jjcet.PragatiX.modules.activity.dto.response.*;
import jjcet.PragatiX.modules.student.dto.request.*;
import jjcet.PragatiX.modules.student.dto.response.*;
import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.repository.*;
import jjcet.PragatiX.modules.activity.repository.*;
import jjcet.PragatiX.modules.faculty.repository.*;
import jjcet.PragatiX.modules.student.repository.*;
import jjcet.PragatiX.repository.StudentGuardianRepository;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Optional;

@Service
public class StudentImportService {
    private static final Logger log = LoggerFactory.getLogger(StudentImportService.class);

    private final AcademicYearRepository academicYearRepository;
    private final DepartmentRepository departmentRepository;
    private final GenderRepository genderRepository;
    private final PasswordEncoder passwordEncoder;
    private final SectionRepository sectionRepository;
    private final SemesterRepository semesterRepository;
    private final StudentRepository studentRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final YearRepository yearRepository;
    private final ExcelStudentParser excelStudentParser;
    private final StudentImportResolverService resolverService;
    private final StudentGuardianRepository studentGuardianRepository;
    private final jjcet.PragatiX.modules.audit.service.AuditService auditService;
    private final ActivityStageRepository activityStageRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public StudentImportService(AcademicYearRepository academicYearRepository,
            DepartmentRepository departmentRepository, GenderRepository genderRepository,
            PasswordEncoder passwordEncoder, SectionRepository sectionRepository, SemesterRepository semesterRepository,
            StudentRepository studentRepository, TeamRepository teamRepository, UserRepository userRepository,
            YearRepository yearRepository, ExcelStudentParser excelStudentParser,
            StudentImportResolverService resolverService, StudentGuardianRepository studentGuardianRepository,
            ActivityStageRepository activityStageRepository, jjcet.PragatiX.modules.audit.service.AuditService auditService) {
        this.academicYearRepository = academicYearRepository;
        this.departmentRepository = departmentRepository;
        this.genderRepository = genderRepository;
        this.passwordEncoder = passwordEncoder;
        this.sectionRepository = sectionRepository;
        this.semesterRepository = semesterRepository;
        this.studentRepository = studentRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.yearRepository = yearRepository;
        this.excelStudentParser = excelStudentParser;
        this.resolverService = resolverService;
        this.studentGuardianRepository = studentGuardianRepository;
        this.activityStageRepository = activityStageRepository;
        this.auditService = auditService;
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        if (line == null || line.trim().isEmpty())
            return result;
        try {
            org.apache.commons.csv.CSVParser parser = org.apache.commons.csv.CSVParser.parse(
                    line,
                    org.apache.commons.csv.CSVFormat.DEFAULT);
            for (org.apache.commons.csv.CSVRecord record : parser) {
                for (String val : record) {
                    result.add(val != null ? val.trim() : "");
                }
                break;
            }
        } catch (Exception e) {
            log.error("Failed to parse CSV line using Commons CSV", e);
        }
        return result;
    }

    private String getColValue(Row row, List<String> csvRow, int idx, boolean isCsvMode, ExcelStudentParser parser) {
        if (idx < 0)
            return "";
        if (isCsvMode) {
            return (csvRow != null && idx < csvRow.size()) ? csvRow.get(idx) : "";
        } else {
            return parser.getCellValueAsString(row.getCell(idx));
        }
    }

    private LocalDate parseLocalDateFromString(String val) {
        if (val == null || val.trim().isEmpty())
            return null;
        val = val.trim();
        try {
            return LocalDate.parse(val);
        } catch (Exception e) {
        }
        try {
            return LocalDate.parse(val, java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (Exception e) {
        }
        try {
            return LocalDate.parse(val, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
        }
        try {
            return LocalDate.parse(val, java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        } catch (Exception e) {
        }
        return null;
    }

    public byte[] generateExcelTemplate(String username) throws java.io.IOException {
        User creator = userRepository.findByUsername(username).orElse(null);
        boolean isSuperAdmin = false;
        boolean isAdmin = false;
        boolean isCC = false;
        if (creator != null) {
            isSuperAdmin = creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_SUPER_ADMIN"));
            isAdmin = creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
            isCC = creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_CLASS_COORDINATOR") || r.getName().equalsIgnoreCase("ROLE_CC"));
        }

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Students");
            Row headerRow = sheet.createRow(0);
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            List<String> headerList = new ArrayList<>();
            
            if (isSuperAdmin) {
                headerList.addAll(Arrays.asList("Full Name", "Register Number", "Email", "Department", "Section", "Year"));
            } else if (isAdmin) {
                headerList.addAll(Arrays.asList("Full Name", "Register Number", "Email", "Department", "Section"));
            } else {
                // CC or other roles: keep existing CC template behavior minus Academic Year and Team Name
                headerList.addAll(Arrays.asList(
                    "Student Name", "Register Number", "SPR Number", "Email", "Phone",
                    "Address", "Date of Birth", "Department", "Year",
                    "Semester", "Gender", "Section", "Guardian Name",
                    "Relationship", "Guardian Phone", "Guardian Email"
                ));
            }

            int deptColIdx = -1;
            int yearColIdx = -1;
            int secColIdx = -1;

            for (int i = 0; i < headerList.size(); i++) {
                Cell cell = headerRow.createCell(i);
                String headerName = headerList.get(i);
                cell.setCellValue(headerName);
                cell.setCellStyle(headerStyle);
                if (headerName.equalsIgnoreCase("Department")) deptColIdx = i;
                if (headerName.equalsIgnoreCase("Year")) yearColIdx = i;
                if (headerName.equalsIgnoreCase("Section")) secColIdx = i;
            }
            
            Row sampleRow = sheet.createRow(1);
            for (int i = 0; i < headerList.size(); i++) {
                String headerName = headerList.get(i);
                Cell cell = sampleRow.createCell(i);
                switch(headerName) {
                    case "Student Name":
                    case "Full Name": cell.setCellValue("Arun Kumar"); break;
                    case "Register Number": cell.setCellValue("24CSC101"); break;
                    case "SPR Number": cell.setCellValue("SPR001"); break;
                    case "Email": cell.setCellValue("arun@example.com"); break;
                    case "Phone": cell.setCellValue("9876543210"); break;
                    case "Address": cell.setCellValue("123 Main St, City"); break;
                    case "Date of Birth": cell.setCellValue("2000-01-15"); break;
                    case "Department": cell.setCellValue("Computer Science and Engineering"); break;
                    case "Year": cell.setCellValue("I"); break;
                    case "Semester": cell.setCellValue("I"); break;
                    case "Gender": cell.setCellValue("Male"); break;
                    case "Section": cell.setCellValue("A"); break;
                    case "Guardian Name": cell.setCellValue("Ravi Kumar"); break;
                    case "Relationship": cell.setCellValue("Father"); break;
                    case "Guardian Phone": cell.setCellValue("9988776655"); break;
                    case "Guardian Email": cell.setCellValue("ravi@example.com"); break;
                }
            }
            
            for (int i = 0; i < headerList.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            sheet.createFreezePane(0, 1);

            // Create hidden _Lists sheet for Dropdowns
            Sheet listSheet = workbook.createSheet("_Lists");
            workbook.setSheetHidden(workbook.getSheetIndex("_Lists"), true);

            List<Department> dbDepartments = departmentRepository.findAll();
            List<String> deptNames = new ArrayList<>();
            for (Department d : dbDepartments) {
                if (d.getDeptName() != null && !d.getDeptName().trim().isEmpty() && !d.isDeleted()) {
                    deptNames.add(d.getDeptName().trim());
                }
            }
            java.util.Collections.sort(deptNames, String.CASE_INSENSITIVE_ORDER);

            for (int i = 0; i < deptNames.size(); i++) {
                Row row = listSheet.getRow(i);
                if (row == null) row = listSheet.createRow(i);
                row.createCell(0).setCellValue(deptNames.get(i));
            }

            String[] years = {"I", "II", "III", "IV"};
            for (int i = 0; i < years.length; i++) {
                Row row = listSheet.getRow(i);
                if (row == null) row = listSheet.createRow(i);
                row.createCell(1).setCellValue(years[i]);
            }

            Name deptNameRange = workbook.createName();
            deptNameRange.setNameName("DepartmentList");
            deptNameRange.setRefersToFormula("_Lists!$A$1:$A$" + Math.max(1, deptNames.size()));

            Name yearNameRange = workbook.createName();
            yearNameRange.setNameName("YearList");
            yearNameRange.setRefersToFormula("_Lists!$B$1:$B$4");

            DataValidationHelper validationHelper = sheet.getDataValidationHelper();

            if (deptColIdx != -1) {
                org.apache.poi.ss.util.CellRangeAddressList deptAddressList = new org.apache.poi.ss.util.CellRangeAddressList(1, 1000, deptColIdx, deptColIdx);
                DataValidationConstraint deptConstraint = validationHelper.createFormulaListConstraint("DepartmentList");
                DataValidation validation = validationHelper.createValidation(deptConstraint, deptAddressList);
                validation.setShowErrorBox(true);
                sheet.addValidationData(validation);
            }

            if (yearColIdx != -1) {
                org.apache.poi.ss.util.CellRangeAddressList yearAddressList = new org.apache.poi.ss.util.CellRangeAddressList(1, 1000, yearColIdx, yearColIdx);
                DataValidationConstraint yearConstraint = validationHelper.createFormulaListConstraint("YearList");
                DataValidation validation = validationHelper.createValidation(yearConstraint, yearAddressList);
                validation.setShowErrorBox(true);
                sheet.addValidationData(validation);
            }

            // Note: Section validation is harder as it depends on Department/Year, so omitted here or user types it

            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Transactional
    public ApiResponse<List<CreateStudentRequest>> bulkParse(MultipartFile file, String username) {
        if (file.isEmpty())
            return ApiResponse.error("Please upload an Excel file.");
        User creator = userRepository.findByUsername(username).orElse(null);
        boolean isSuperAdmin = false;
        boolean isAdmin = false;
        boolean isCc = false;
        String adminAssignedYear = null;

        if (creator != null) {
            isSuperAdmin = creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_SUPER_ADMIN"));
            isAdmin = creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
            isCc = creator.getSubRoles().stream().map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));
            adminAssignedYear = creator.getAssignedYear() != null ? creator.getAssignedYear().getYearName() : creator.getYear(); // Ensure Admin's year is fetched
        }

        if (!isSuperAdmin && !isAdmin && !isCc) {
            return ApiResponse.error("Access Denied: Only Super Admin, Admin, and CC can parse student import files.");
        }

        if (isAdmin && !isSuperAdmin && (adminAssignedYear == null || adminAssignedYear.trim().isEmpty())) {
            return ApiResponse.error("Access Denied: Admin is not assigned to any Year.");
        }

        try (java.io.InputStream is = file.getInputStream();
                Workbook workbook = WorkbookFactory.create(is)) {
            List<CreateStudentRequest> parsedList = new ArrayList<>();
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null)
                return ApiResponse.error("Spreadsheet is empty or missing headers");

            boolean isCsvMode = false;
            List<String> headerCols = new ArrayList<>();
            if (headerRow.getLastCellNum() == 1) {
                String cellVal = excelStudentParser.getCellValueAsString(headerRow.getCell(0));
                if (cellVal.contains(",")) {
                    isCsvMode = true;
                    headerCols = parseCsvLine(cellVal);
                    log.info("Enabled automatic CSV Recovery Mode for malformed Excel file.");
                }
            }

            int nameIdx = -1, deptIdx = -1, sprIdx = -1, regIdx = -1, dobIdx = -1;
            int phoneIdx = -1, emailIdx = -1, genderIdx = -1;
            int yearIdx = -1, semIdx = -1, secIdx = -1, addressIdx = -1;
            int guardNameIdx = -1, guardRelIdx = -1, guardPhoneIdx = -1, guardEmailIdx = -1;

            int colCount = isCsvMode ? headerCols.size() : headerRow.getLastCellNum();
            for (int i = 0; i < colCount; i++) {
                String headerRaw;
                if (isCsvMode) {
                    headerRaw = headerCols.get(i);
                } else {
                    Cell cell = headerRow.getCell(i);
                    if (cell == null)
                        continue;
                    headerRaw = excelStudentParser.getCellValueAsString(cell);
                }

                if (headerRaw == null || headerRaw.trim().isEmpty())
                    continue;

                String header = headerRaw.toLowerCase().replaceAll("[_\\-\\s]", "");

                if (header.contains("name") && !header.contains("dept") && !header.contains("guardian"))
                    nameIdx = i;
                else if (header.contains("dept") || header.contains("department"))
                    deptIdx = i;
                else if (header.contains("spr"))
                    sprIdx = i;
                else if (header.contains("reg") || header.contains("register"))
                    regIdx = i;
                else if (header.contains("dob") || header.contains("dateofbirth") || header.contains("birth"))
                    dobIdx = i;
                else if (header.contains("phone") || header.contains("mobile"))
                    phoneIdx = i;
                else if (header.contains("email"))
                    emailIdx = i;
                else if (header.contains("gender") || header.contains("sex"))
                    genderIdx = i;
                else if (header.equals("year") || header.contains("currentyear"))
                    yearIdx = i;
                else if (header.contains("semester") || header.contains("sem"))
                    semIdx = i;
                else if (header.contains("section") || header.contains("sec"))
                    secIdx = i;
                else if (header.contains("address"))
                    addressIdx = i;
                else if (header.contains("guardianname"))
                    guardNameIdx = i;
                else if (header.contains("relationship") || header.contains("relation"))
                    guardRelIdx = i;
                else if (header.contains("guardianphone") || header.contains("parentphone"))
                    guardPhoneIdx = i;
                else if (header.contains("guardianemail") || header.contains("parentemail"))
                    guardEmailIdx = i;
            }

            boolean usingFallback = false;
            if (nameIdx == -1 && regIdx == -1 && emailIdx == -1) {
                log.info("No headers matched. Falling back to default column indices based on standard template.");
                usingFallback = true;
                nameIdx = 0;
                regIdx = 1;
                sprIdx = 2;
                emailIdx = 3;
                phoneIdx = 4;
                addressIdx = 5;
                dobIdx = 6;
                deptIdx = 7;
                yearIdx = 8;
                semIdx = 9;
                genderIdx = 10;
                secIdx = 11;
                guardNameIdx = 12;
                guardRelIdx = 13;
                guardPhoneIdx = 14;
                guardEmailIdx = 15;
            }

            int startRow = usingFallback ? 0 : 1;

            for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null)
                    continue;

                List<String> csvRow = null;
                if (isCsvMode) {
                    String rowVal = excelStudentParser.getCellValueAsString(row.getCell(0));
                    csvRow = parseCsvLine(rowVal);
                }

                String name = getColValue(row, csvRow, nameIdx, isCsvMode, excelStudentParser);
                String deptName = getColValue(row, csvRow, deptIdx, isCsvMode, excelStudentParser);
                String sprNo = getColValue(row, csvRow, sprIdx, isCsvMode, excelStudentParser);
                String regNo = getColValue(row, csvRow, regIdx, isCsvMode, excelStudentParser);

                LocalDate dob = null;
                if (dobIdx >= 0) {
                    if (isCsvMode) {
                        dob = parseLocalDateFromString(getColValue(row, csvRow, dobIdx, true, excelStudentParser));
                    } else {
                        dob = excelStudentParser.parseLocalDate(row.getCell(dobIdx));
                    }
                }

                String phoneNo = getColValue(row, csvRow, phoneIdx, isCsvMode, excelStudentParser);
                String email = getColValue(row, csvRow, emailIdx, isCsvMode, excelStudentParser);
                String gender = getColValue(row, csvRow, genderIdx, isCsvMode, excelStudentParser);
                String year = getColValue(row, csvRow, yearIdx, isCsvMode, excelStudentParser);
                
                if (isAdmin && !isSuperAdmin) {
                    year = adminAssignedYear;
                }
                
                String semester = getColValue(row, csvRow, semIdx, isCsvMode, excelStudentParser);
                String section = getColValue(row, csvRow, secIdx, isCsvMode, excelStudentParser);
                String address = getColValue(row, csvRow, addressIdx, isCsvMode, excelStudentParser);

                String gName = getColValue(row, csvRow, guardNameIdx, isCsvMode, excelStudentParser);
                String gRel = getColValue(row, csvRow, guardRelIdx, isCsvMode, excelStudentParser);
                String gPhone = getColValue(row, csvRow, guardPhoneIdx, isCsvMode, excelStudentParser);
                String gEmail = getColValue(row, csvRow, guardEmailIdx, isCsvMode, excelStudentParser);

                if (regNo.isEmpty() && email.isEmpty() && name.isEmpty()) {
                    continue; // Skip completely empty rows
                }

                List<String> errors = new ArrayList<>();
                if (regNo.isEmpty())
                    errors.add("Register Number missing");
                if (email.isEmpty())
                    errors.add("Email missing");
                if (name.isEmpty())
                    errors.add("Student Name missing");

                if (guardNameIdx != -1 && guardPhoneIdx != -1) {
                    if (gName.isEmpty())
                        errors.add("Guardian Name is required");
                    if (gRel.isEmpty())
                        errors.add("Guardian Relationship is required");
                    if (gPhone.isEmpty())
                        errors.add("Guardian Phone is required");
                    else if (!gPhone.matches("^\\d{10}$"))
                        errors.add("Guardian Phone must be exactly 10 digits");
                }

                CreateStudentRequest req = new CreateStudentRequest();
                req.setFullName(name);
                req.setDepartmentName(deptName);
                req.setSprNo(sprNo);
                req.setRegNo(regNo);
                req.setDateOfBirth(dob);
                req.setPhone(phoneNo);
                req.setEmail(email);
                req.setGender(gender);
                req.setYear(year);
                req.setSemester(semester);
                req.setSection(section);
                req.setAddress(address);
                req.setActive(true);

                if (guardNameIdx != -1) {
                    GuardianDTO guardian = new GuardianDTO();
                    guardian.setGuardianName(gName);
                    guardian.setRelationship(gRel.isEmpty() ? "Parent" : gRel);
                    guardian.setPhoneNo(gPhone);
                    guardian.setEmail(gEmail);
                    req.setGuardian(guardian);
                }

                Long dId = resolverService.resolveDepartment(deptName);
                if (dId == null && !deptName.isEmpty())
                    errors.add("Department not found: " + deptName);
                req.setDepartmentId(dId);

                Long gId = resolverService.resolveGender(gender);
                if (gId == null && !gender.isEmpty())
                    errors.add("Gender not found: " + gender);
                req.setGenderId(gId);

                Long yId = resolverService.resolveYear(year);
                if (yId == null && !year.isEmpty())
                    errors.add("Year not found: " + year);
                req.setYearId(yId);

                Long sId = resolverService.resolveSemester(semester);
                if (sId == null && !semester.isEmpty())
                    errors.add("Semester not found: " + semester);
                req.setSemesterId(sId);

                Long secId = resolverService.resolveSection(section, dId);
                if (secId == null && !section.isEmpty())
                    errors.add("Section not found: " + section);
                req.setSectionId(secId);

                if (guardNameIdx != -1 && guardPhoneIdx != -1
                        && (errors.isEmpty() || errors.stream().noneMatch(e -> e.contains("Guardian")))) {
                    GuardianDTO gDto = new GuardianDTO();
                    gDto.setGuardianName(gName);
                    gDto.setRelationship(gRel);
                    gDto.setPhoneNo(gPhone);
                    gDto.setEmail(gEmail);
                    req.setGuardian(gDto);
                }

                if (!errors.isEmpty()) {
                    req.setErrorReason(String.join(", ", errors));
                }

                parsedList.add(req);
            }
            if (!parsedList.isEmpty()) {
                CreateStudentRequest first = parsedList.get(0);
                log.info("=== STEP 1: PARSED REQUEST FOR FIRST STUDENT ===");
                log.info("FullName: {}", first.getFullName());
                log.info("RegNo: {}", first.getRegNo());
                log.info("SprNo: {}", first.getSprNo());
                log.info("Email: {}", first.getEmail());
                log.info("Phone: {}", first.getPhone());
                log.info("DOB: {}", first.getDateOfBirth());
                log.info("Department: {}", first.getDepartmentName());
                log.info("DepartmentId: {}", first.getDepartmentId());
                log.info("Year: {}", first.getYear());
                log.info("YearId: {}", first.getYearId());
                log.info("Section: {}", first.getSection());
                log.info("SectionId: {}", first.getSectionId());
                log.info("Semester: {}", first.getSemester());
                log.info("SemesterId: {}", first.getSemesterId());
                log.info("Gender: {}", first.getGender());
                log.info("GenderId: {}", first.getGenderId());
            }
            return ApiResponse.ok("Spreadsheet file parsed successfully", parsedList);
        } catch (Exception e) {
            log.error("Bulk parse failed", e);
            return ApiResponse.error("Failed to parse spreadsheet file: " + e.getMessage());
        }
    }

    public ApiResponse<String> bulkImport(List<CreateStudentRequest> requests, String username) {
        if (requests != null && !requests.isEmpty()) {
            CreateStudentRequest first = requests.get(0);
            log.info("=== STEP 2: INCOMING IMPORT REQUEST FOR FIRST STUDENT ===");
            log.info("FullName: {}", first.getFullName());
            log.info("AcademicYear: {}", first.getAcademicYear());
            log.info("AcademicYearId: {}", first.getAcademicYearId());
            log.info("Department: {}", first.getDepartmentName());
            log.info("DepartmentId: {}", first.getDepartmentId());
            log.info("Year: {}", first.getYear());
            log.info("YearId: {}", first.getYearId());
            log.info("Section: {}", first.getSection());
            log.info("SectionId: {}", first.getSectionId());
            log.info("Semester: {}", first.getSemester());
            log.info("SemesterId: {}", first.getSemesterId());
            log.info("Gender: {}", first.getGender());
            log.info("GenderId: {}", first.getGenderId());
        }
        User creator = userRepository.findByUsername(username).orElse(null);
        boolean isSuperAdmin = false;
        boolean isAdmin = false;
        boolean isCc = false;
        String adminAssignedYear = null;

        if (creator != null) {
            isSuperAdmin = creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_SUPER_ADMIN") || r.getName().equalsIgnoreCase("ROLE_SUPERADMIN"));
            isAdmin = creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
            isCc = creator.getSubRoles().stream().map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));
            adminAssignedYear = creator.getAssignedYear() != null ? creator.getAssignedYear().getYearName() : creator.getYear(); // Ensure Admin's year is fetched
        }

        if (!isSuperAdmin && !isAdmin && !isCc) {
            return ApiResponse.error("Access denied. You do not have permission to bulk import students.");
        }

        if (isAdmin && !isSuperAdmin && (adminAssignedYear == null || adminAssignedYear.trim().isEmpty())) {
            return ApiResponse.error("Access Denied: Admin is not assigned to any Year.");
        }
        try {
            int successCount = 0;
            int updateCount = 0;
            java.util.Set<String> processedStudentIds = new java.util.HashSet<>();
            java.util.Set<String> processedEmails = new java.util.HashSet<>();
            java.util.Set<String> processedSprs = new java.util.HashSet<>();
            List<Student> studentsToSave = new ArrayList<>();
            List<StudentGuardian> guardiansToSave = new ArrayList<>();

            ActivityStage initialStage = activityStageRepository.findFirstByIsActiveTrueOrderByDisplayOrderAsc()
                    .orElse(null);
            if (initialStage == null) {
                return ApiResponse.error(
                        "Validation Error: No active stages found. Please configure stages before creating students.");
            }
            java.util.Map<Long, Department> deptMap = new java.util.HashMap<>();
            java.util.Map<Long, Section> sectionMap = new java.util.HashMap<>();
            java.util.Map<Long, Gender> genderMap = new java.util.HashMap<>();
            java.util.Map<Long, AcademicYear> academicYearMap = new java.util.HashMap<>();
            java.util.Map<Long, Year> yearMap = new java.util.HashMap<>();
            java.util.Map<Long, Semester> semesterMap = new java.util.HashMap<>();
            java.util.Map<Long, Team> teamMap = new java.util.HashMap<>();

            for (CreateStudentRequest request : requests) {
                if (isAdmin && !isSuperAdmin) {
                    request.setYear(adminAssignedYear);
                    request.setYearId(resolverService.resolveYear(adminAssignedYear));
                }
                
                
                if (request.getRegNo() == null || request.getRegNo().trim().isEmpty() ||
                        request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                    continue;
                }

                String regNo = request.getRegNo().trim();
                String email = request.getEmail().trim();

                if (processedStudentIds.contains(regNo))
                    return ApiResponse.error("Duplicate Register No '" + regNo + "' found in the uploaded batch.");
                if (processedEmails.contains(email))
                    return ApiResponse.error("Duplicate Email '" + email + "' found in the uploaded batch.");
                if (request.getSprNo() != null && !request.getSprNo().trim().isEmpty()) {
                    String cleanSpr = request.getSprNo().trim();
                    if (processedSprs.contains(cleanSpr))
                        return ApiResponse
                                .error("Duplicate SPR Number '" + cleanSpr + "' found in the uploaded batch.");
                    processedSprs.add(cleanSpr);
                }
                processedStudentIds.add(regNo);
                processedEmails.add(email);

                // Reduced row-level logging to prevent console flooding
                // log.debug("Validating row for {}", request.getFullName());

                if (request.getDepartmentId() == null)
                    request.setDepartmentId(resolverService.resolveDepartment(request.getDepartmentName()));
                if (request.getGenderId() == null)
                    request.setGenderId(resolverService.resolveGender(request.getGender()));

                if (request.getYearId() == null)
                    request.setYearId(resolverService.resolveYear(request.getYear()));
                if (request.getSemesterId() == null)
                    request.setSemesterId(resolverService.resolveSemester(request.getSemester()));
                if (request.getSectionId() == null)
                    request.setSectionId(
                            resolverService.resolveSection(request.getSection(), request.getDepartmentId()));

                if (request.getDepartmentId() == null)
                    return ApiResponse
                            .error("Department is missing or not registered for student: " + request.getFullName());
                if (request.getGenderId() == null)
                    return ApiResponse
                            .error("Gender is missing or not registered for student: " + request.getFullName());
                if (request.getYearId() == null)
                    return ApiResponse.error("Year is missing or not registered for student: " + request.getFullName());
                if (request.getSemesterId() == null)
                    return ApiResponse
                            .error("Semester is missing or not registered for student: " + request.getFullName());

                Department department = deptMap.computeIfAbsent(request.getDepartmentId(),
                        id -> departmentRepository.findById(id).orElse(null));
                Section section = request.getSectionId() != null ? sectionMap.computeIfAbsent(request.getSectionId(),
                        id -> sectionRepository.findById(id).orElse(null)) : null;
                Gender gender = genderMap.computeIfAbsent(request.getGenderId(),
                        id -> genderRepository.findById(id).orElse(null));
                AcademicYear academicYear = academicYearRepository.findAll().stream()
                        .filter(a -> a.getStatus() == AcademicYear.Status.ACTIVE)
                        .findFirst().orElse(null);
                
                if (academicYear == null) {
                    return ApiResponse.error("No active Academic Year found. Please configure an active Academic Year first.");
                }
                
                Year year = yearMap.computeIfAbsent(request.getYearId(),
                        id -> yearRepository.findById(id).orElse(null));
                Semester semester = semesterMap.computeIfAbsent(request.getSemesterId(),
                        id -> semesterRepository.findById(id).orElse(null));

                LocalDate dob = request.getDateOfBirth();
                String rawPassword = dob != null ? dob.format(DateTimeFormatter.ofPattern("ddMMyyyy")) : regNo;
                String encodedPassword = passwordEncoder.encode(rawPassword);

                Student student = studentRepository.findByRegNo(regNo).or(() -> studentRepository.findByEmail(email))
                        .orElse(null);

                if (student != null) {
                    if (request.getSprNo() != null && !request.getSprNo().trim().isEmpty()) {
                        String cleanSpr = request.getSprNo().trim();
                        java.util.Optional<Student> duplicateSpr = studentRepository.findBySprNo(cleanSpr);
                        if (duplicateSpr.isPresent() && !duplicateSpr.get().getId().equals(student.getId())) {
                            return ApiResponse.error("SPR Number '" + cleanSpr + "' is already assigned to student: "
                                    + duplicateSpr.get().getFullName());
                        }
                    }
                    java.util.Optional<Student> duplicateEmail = studentRepository.findByEmail(email);
                    if (duplicateEmail.isPresent() && !duplicateEmail.get().getId().equals(student.getId())) {
                        return ApiResponse.error("Email '" + email + "' is already assigned to student: "
                                + duplicateEmail.get().getFullName());
                    }

                    student.setFullName(request.getFullName().trim());
                    student.setDepartment(department);
                    student.setSection(section);
                    student.setGenderRef(gender);
                    student.setAcademicYearRef(academicYear);
                    student.setYearRef(year);
                    student.setSemesterRef(semester);
                    student.setSprNo(request.getSprNo() != null && !request.getSprNo().trim().isEmpty()
                            ? request.getSprNo().trim()
                            : null);
                    student.setPhone(request.getPhone() != null && !request.getPhone().trim().isEmpty()
                            ? request.getPhone().trim()
                            : null);
                    student.setPhoneNo(request.getPhone() != null && !request.getPhone().trim().isEmpty()
                            ? request.getPhone().trim()
                            : "0000000000");
                    student.setDateOfBirth(dob);
                    student.setEmail(email);
                    student.setPassword(encodedPassword);
                    student.setAddress(request.getAddress());
                    student.setActive(request.getActive() != null ? request.getActive() : true);
                    student.setAcademicYear(academicYear.getAcademicYear());
                    student.setYear(String.valueOf(year.getYearNo()));
                    student.setSemester(String.valueOf(semester.getSemesterNo()));
                    student.setGender(gender.getGenderName());

                    studentsToSave.add(student);

                    if (request.getGuardian() != null) {
                        GuardianDTO gDto = request.getGuardian();
                        StudentGuardian guardian = studentGuardianRepository.findByStudentId(student.getId())
                                .orElse(new StudentGuardian());
                        guardian.setStudent(student);
                        guardian.setRegNo(student.getRegNo());
                        guardian.setGuardianName(gDto.getGuardianName());
                        try {
                            guardian.setRelationship(
                                    StudentGuardian.RelationshipType.valueOf(gDto.getRelationship().toUpperCase()));
                        } catch (Exception e) {
                            guardian.setRelationship(StudentGuardian.RelationshipType.GUARDIAN);
                        }
                        guardian.setPhoneNo(gDto.getPhoneNo());
                        guardian.setEmail(gDto.getEmail());
                        guardian.setPrimary(true);
                        guardiansToSave.add(guardian);
                    }

                    updateCount++;
                } else {
                    if (studentRepository.existsByRegNo(regNo))
                        return ApiResponse.error("Student Register No '" + regNo + "' already exists.");
                    if (studentRepository.existsByEmail(email))
                        return ApiResponse.error("Email '" + email + "' already exists.");
                    if (request.getSprNo() != null && !request.getSprNo().trim().isEmpty()) {
                        String cleanSpr = request.getSprNo().trim();
                        if (studentRepository.findBySprNo(cleanSpr).isPresent())
                            return ApiResponse.error("SPR Number '" + cleanSpr + "' already exists.");
                    }

                    student = Student.builder()
                            .regNo(regNo)
                            .fullName(request.getFullName().trim())
                            .email(email)
                            .password(encodedPassword)
                            .phone(request.getPhone() != null && !request.getPhone().trim().isEmpty()
                                    ? request.getPhone().trim()
                                    : null)
                            .phoneNo(request.getPhone() != null && !request.getPhone().trim().isEmpty()
                                    ? request.getPhone().trim()
                                    : "0000000000")
                            .genderRef(gender)
                            .dateOfBirth(dob)
                            .department(department)
                            .section(section)
                            .academicYearRef(academicYear)
                            .yearRef(year)
                            .semesterRef(semester)
                            .academicYear(academicYear.getAcademicYear())
                            .year(String.valueOf(year.getYearNo()))
                            .semester(String.valueOf(semester.getSemesterNo()))
                            .gender(gender.getGenderName())
                            .score(100)
                            .stage(initialStage.getDisplayOrder())
                            .currentStage(initialStage.getDisplayOrder())
                            .currentStageId(initialStage.getId())
                            .sprNo(request.getSprNo() != null && !request.getSprNo().trim().isEmpty()
                                    ? request.getSprNo().trim()
                                    : null)
                            .address(request.getAddress())
                            .active(request.getActive() != null ? request.getActive() : true)
                            .build();
                    studentsToSave.add(student);

                    if (request.getGuardian() != null) {
                        GuardianDTO gDto = request.getGuardian();
                        StudentGuardian guardian = new StudentGuardian();
                        guardian.setStudent(student);
                        guardian.setRegNo(regNo);
                        guardian.setGuardianName(gDto.getGuardianName());
                        try {
                            guardian.setRelationship(
                                    StudentGuardian.RelationshipType.valueOf(gDto.getRelationship().toUpperCase()));
                        } catch (Exception e) {
                            guardian.setRelationship(StudentGuardian.RelationshipType.GUARDIAN);
                        }
                        guardian.setPhoneNo(gDto.getPhoneNo());
                        guardian.setEmail(gDto.getEmail());
                        guardian.setPrimary(true);
                        guardiansToSave.add(guardian);
                    }

                    successCount++;
                }
            }
            if (!studentsToSave.isEmpty()) {
                studentRepository.saveAllAndFlush(studentsToSave);
            }
            if (!guardiansToSave.isEmpty()) {
                studentGuardianRepository.saveAllAndFlush(guardiansToSave);
            }
            if (successCount > 0) {
                auditService.log(
                    jjcet.PragatiX.enums.AuditAction.CREATE,
                    jjcet.PragatiX.enums.AuditModule.STUDENT,
                    "Bulk Import",
                    creator != null ? creator.getId() : 0L,
                    "Bulk imported " + successCount + " students successfully" + (updateCount > 0 ? " and updated " + updateCount : "")
                );
            }

            return ApiResponse.ok(
                    "Bulk import processed: " + successCount + " students created, " + updateCount + " updated.", null);
        } catch (Exception e) {
            log.error("Bulk import failed", e);
            Throwable root = e;
            while (root.getCause() != null)
                root = root.getCause();
            return ApiResponse.error("Bulk import failed: " + root.getMessage());
        }
    }
}
