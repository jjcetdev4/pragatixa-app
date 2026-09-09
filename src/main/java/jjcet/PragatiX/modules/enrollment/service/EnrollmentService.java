package jjcet.PragatiX.modules.enrollment.service;

import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.enums.AuditAction;
import jjcet.PragatiX.enums.AuditModule;
import jjcet.PragatiX.modules.activity.repository.ActivityStageRepository;
import jjcet.PragatiX.modules.audit.service.AuditService;
import jjcet.PragatiX.modules.enrollment.dto.*;
import jjcet.PragatiX.modules.enrollment.entity.Enrollment;
import jjcet.PragatiX.modules.enrollment.entity.EnrollmentSetting;
import jjcet.PragatiX.modules.enrollment.enums.EnrollmentStatus;
import jjcet.PragatiX.modules.enrollment.repository.EnrollmentRepository;
import jjcet.PragatiX.modules.enrollment.repository.EnrollmentSettingRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[0-9]{10}$");

    public static final List<String> APPROVED_STUDENT_DEPARTMENTS = Arrays.asList(
            "Aeronautical Engineering",
            "Mechanical Engineering",
            "Civil Engineering",
            "Computer Science and Engineering",
            "Computer Science and Engineering (Cyber Security)",
            "Electrical and Electronics Engineering",
            "Electronics and Communication Engineering",
            "Information Technology",
            "Artificial Intelligence and Data Science"
    );

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentSettingRepository enrollmentSettingRepository;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final GenderRepository genderRepository;
    private final AcademicYearRepository academicYearRepository;
    private final YearRepository yearRepository;
    private final SemesterRepository semesterRepository;
    private final StudentRepository studentRepository;
    private final ActivityStageRepository activityStageRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            EnrollmentSettingRepository enrollmentSettingRepository,
            DepartmentRepository departmentRepository,
            SectionRepository sectionRepository,
            GenderRepository genderRepository,
            AcademicYearRepository academicYearRepository,
            YearRepository yearRepository,
            SemesterRepository semesterRepository,
            StudentRepository studentRepository,
            ActivityStageRepository activityStageRepository,
            AuditService auditService,
            PasswordEncoder passwordEncoder) {
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentSettingRepository = enrollmentSettingRepository;
        this.departmentRepository = departmentRepository;
        this.sectionRepository = sectionRepository;
        this.genderRepository = genderRepository;
        this.academicYearRepository = academicYearRepository;
        this.yearRepository = yearRepository;
        this.semesterRepository = semesterRepository;
        this.studentRepository = studentRepository;
        this.activityStageRepository = activityStageRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    // ==========================================
    // 1. ENROLLMENT ON / OFF SETTINGS
    // ==========================================

    @Transactional(readOnly = true)
    public boolean isEnrollmentEnabled() {
        return enrollmentSettingRepository.findAll().stream()
                .findFirst()
                .map(EnrollmentSetting::isEnrollmentEnabled)
                .orElse(false);
    }

    @Transactional
    public void setEnrollmentEnabled(boolean enabled, String updatedBy) {
        EnrollmentSetting setting = enrollmentSettingRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    EnrollmentSetting newSetting = new EnrollmentSetting();
                    newSetting.setEnrollmentEnabled(false);
                    return newSetting;
                });

        boolean oldStatus = setting.isEnrollmentEnabled();
        setting.setEnrollmentEnabled(enabled);
        setting.setUpdatedBy(updatedBy);
        setting.setUpdatedAt(LocalDateTime.now());
        enrollmentSettingRepository.save(setting);

        try {
            auditService.log(
                    AuditAction.ENROLLMENT_SETTING_CHANGED,
                    AuditModule.ENROLLMENT,
                    "EnrollmentSetting",
                    setting.getId(),
                    "Enrollment status changed from " + oldStatus + " to " + enabled + " by " + updatedBy,
                    Collections.singletonMap("enabled", oldStatus),
                    Collections.singletonMap("enabled", enabled)
            );
        } catch (Exception e) {
            log.warn("Failed to record audit log for enrollment setting change: {}", e.getMessage());
        }
    }

    // ==========================================
    // 2. EXCEL TEMPLATE GENERATION (EXACTLY 6 COLUMNS)
    // ==========================================

    public byte[] generateExcelTemplate() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Student_Enrollment");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Columns: Sl.No., Name, Gender, Email, Mobile, Branch, Section (Optional)
            String[] headers = {"Sl.No.", "Name", "Gender", "Email", "Mobile", "Branch", "Section (Optional)"};
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(26);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample row 1
            Row sampleRow1 = sheet.createRow(1);
            sampleRow1.createCell(0).setCellValue(1);
            sampleRow1.createCell(1).setCellValue("ARUN KUMAR");
            sampleRow1.createCell(2).setCellValue("Male");
            sampleRow1.createCell(3).setCellValue("arun@gmail.com");
            sampleRow1.createCell(4).setCellValue("9876543210");
            sampleRow1.createCell(5).setCellValue("Computer Science and Engineering");
            sampleRow1.createCell(6).setCellValue("A");

            // Sample row 2
            Row sampleRow2 = sheet.createRow(2);
            sampleRow2.createCell(0).setCellValue(2);
            sampleRow2.createCell(1).setCellValue("PRIYA KUMAR");
            sampleRow2.createCell(2).setCellValue("Female");
            sampleRow2.createCell(3).setCellValue("priya@gmail.com");
            sampleRow2.createCell(4).setCellValue("9876543211");
            sampleRow2.createCell(5).setCellValue("Information Technology");
            sampleRow2.createCell(6).setCellValue("B");

            // Sample row 3: Student in department with sections, but has no section (Section is optional)
            Row sampleRow3 = sheet.createRow(3);
            sampleRow3.createCell(0).setCellValue(3);
            sampleRow3.createCell(1).setCellValue("KARTHIK S");
            sampleRow3.createCell(2).setCellValue("Male");
            sampleRow3.createCell(3).setCellValue("karthik@gmail.com");
            sampleRow3.createCell(4).setCellValue("9876543212");
            sampleRow3.createCell(5).setCellValue("Mechanical Engineering");
            sampleRow3.createCell(6).setCellValue("");

            // Auto-size columns with minimum padding
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.max(currentWidth + 1200, 4500));
            }
            sheet.createFreezePane(0, 1);

            // Dropdowns via Hidden Lists Sheet
            XSSFSheet listSheet = workbook.createSheet("_LookupData");
            workbook.setSheetHidden(workbook.getSheetIndex("_LookupData"), true);

            // 1. Gender Lookup
            List<String> genderList = Arrays.asList("Male", "Female", "Other");
            for (int r = 0; r < genderList.size(); r++) {
                Row row = listSheet.getRow(r);
                if (row == null) row = listSheet.createRow(r);
                row.createCell(0).setCellValue(genderList.get(r));
            }

            // 2. Branch Lookup (MAIN Approved Student Departments from Database)
            List<Department> mainDepts = departmentRepository.findByDepartmentTypeAndDeletedFalse(jjcet.PragatiX.enums.DepartmentType.MAIN);
            List<String> branchNames = new ArrayList<>();
            for (Department d : mainDepts) {
                branchNames.add(d.getName());
            }
            if (branchNames.isEmpty()) {
                branchNames.addAll(APPROVED_STUDENT_DEPARTMENTS);
            }

            for (int r = 0; r < branchNames.size(); r++) {
                Row row = listSheet.getRow(r);
                if (row == null) row = listSheet.createRow(r);
                row.createCell(1).setCellValue(branchNames.get(r));
            }

            // Create Data Validations for 500 rows
            XSSFDataValidationHelper validationHelper = new XSSFDataValidationHelper(sheet);

            // Gender dropdown validation (Column index 2: C)
            CellRangeAddressList genderRange = new CellRangeAddressList(1, 500, 2, 2);
            DataValidationConstraint genderConstraint = validationHelper.createFormulaListConstraint("_LookupData!$A$1:$A$" + genderList.size());
            XSSFDataValidation genderValidation = (XSSFDataValidation) validationHelper.createValidation(genderConstraint, genderRange);
            genderValidation.setSuppressDropDownArrow(true);
            genderValidation.setShowErrorBox(true);
            sheet.addValidationData(genderValidation);

            // Branch dropdown validation (Column index 5: F)
            CellRangeAddressList branchRange = new CellRangeAddressList(1, 500, 5, 5);
            DataValidationConstraint branchConstraint = validationHelper.createFormulaListConstraint("_LookupData!$B$1:$B$" + branchNames.size());
            XSSFDataValidation branchValidation = (XSSFDataValidation) validationHelper.createValidation(branchConstraint, branchRange);
            branchValidation.setSuppressDropDownArrow(true);
            branchValidation.setShowErrorBox(true);
            sheet.addValidationData(branchValidation);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ==========================================
    // 3. BULK IMPORT EXCEL (CREATES PENDING ENROLLMENTS ONLY)
    // ==========================================

    @Transactional
    public EnrollmentImportResultDto importEnrollmentExcel(MultipartFile file, String createdBy) throws IOException {
        EnrollmentImportResultDto result = new EnrollmentImportResultDto();

        if (file == null || file.isEmpty()) {
            result.addError("Uploaded file is empty.");
            return result;
        }

        log.info("Starting bulk student enrollment Excel import by: {}", createdBy);

        List<Department> allDepts = departmentRepository.findAll();
        List<Enrollment> pendingToSave = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                result.addError("Excel sheet not found.");
                return result;
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                result.addError("Excel sheet is missing header row.");
                return result;
            }

            // Identify column positions
            int nameCol = -1;
            int genderCol = -1;
            int emailCol = -1;
            int mobileCol = -1;
            int branchCol = -1;
            int sectionCol = -1;

            int lastCellNum = headerRow.getLastCellNum();
            for (int c = 0; c < lastCellNum; c++) {
                Cell cell = headerRow.getCell(c);
                String val = getCellValue(cell).trim().toLowerCase();
                if (val.equals("name") || val.equals("student name") || val.equals("full name")) {
                    nameCol = c;
                } else if (val.equals("gender")) {
                    genderCol = c;
                } else if (val.equals("email") || val.equals("email id")) {
                    emailCol = c;
                } else if (val.equals("mobile") || val.equals("phone") || val.equals("mobile number") || val.equals("phone number")) {
                    mobileCol = c;
                } else if (val.equals("branch") || val.equals("department")) {
                    branchCol = c;
                } else if (val.equals("section") || val.equals("sec") || val.startsWith("section") || val.startsWith("sec")) {
                    sectionCol = c;
                }
            }

            // Default standard positions if not explicitly named
            if (nameCol == -1 && lastCellNum >= 2) nameCol = 1;
            if (genderCol == -1 && lastCellNum >= 3) genderCol = 2;
            if (emailCol == -1 && lastCellNum >= 4) emailCol = 3;
            if (mobileCol == -1 && lastCellNum >= 5) mobileCol = 4;
            if (branchCol == -1 && lastCellNum >= 6) branchCol = 5;
            if (sectionCol == -1 && lastCellNum >= 7) sectionCol = 6;

            if (nameCol == -1 || emailCol == -1 || mobileCol == -1 || branchCol == -1) {
                result.addError("Invalid template format. The template must contain: Sl.No., Name, Gender, Email, Mobile, Branch.");
                return result;
            }

            int lastRowNum = sheet.getLastRowNum();
            log.info("Detected {} total rows in enrollment Excel sheet.", lastRowNum);

            // Phase 1: Read and collect candidate values for batch pre-fetching
            List<Object[]> rawRows = new ArrayList<>();
            Set<String> candidateEmails = new HashSet<>();

            for (int r = 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                int rowNum = r + 1; // 1-based display row
                String name = getCellValue(row.getCell(nameCol)).trim().toUpperCase();
                String gender = genderCol != -1 ? getCellValue(row.getCell(genderCol)).trim() : "Male";
                String email = getCellValue(row.getCell(emailCol)).trim().toLowerCase();
                String mobile = cleanMobileNumber(getCellValue(row.getCell(mobileCol)).trim());
                String branch = getCellValue(row.getCell(branchCol)).trim();
                String section = sectionCol != -1 ? getCellValue(row.getCell(sectionCol)).trim() : "";

                rawRows.add(new Object[]{rowNum, name, gender, email, mobile, branch, section});
                if (!email.isEmpty()) candidateEmails.add(email);
            }

            result.setTotalRows(rawRows.size());

            // Phase 2: Batch fetch existing email conflicts from database in O(1) batch queries
            Set<String> existingEnrollmentEmails = candidateEmails.isEmpty() ? Collections.emptySet() : enrollmentRepository.findExistingEmailsIn(candidateEmails);
            Set<String> existingStudentEmails = candidateEmails.isEmpty() ? Collections.emptySet() : studentRepository.findExistingEmailsIn(candidateEmails);

            Set<String> seenEmailsInFile = new HashSet<>();

            // Phase 3: Validate rows in-memory
            for (Object[] raw : rawRows) {
                int rowNum = (int) raw[0];
                String name = (String) raw[1];
                String gender = (String) raw[2];
                String email = (String) raw[3];
                String mobile = (String) raw[4];
                String branch = (String) raw[5];
                String sectionName = (String) raw[6];

                // Validation 1: Name
                if (name.isEmpty()) {
                    result.addError("Row " + rowNum + ": Name is required.");
                    continue;
                }

                // Validation 2: Gender
                if (gender.isEmpty()) {
                    gender = "Male";
                }
                gender = normalizeGender(gender);

                // Validation 3: Email
                if (email.isEmpty()) {
                    result.addError("Row " + rowNum + ": Email is required.");
                    continue;
                }
                if (!EMAIL_PATTERN.matcher(email).matches()) {
                    result.addError("Row " + rowNum + ": Invalid email format ('" + email + "').");
                    continue;
                }

                // Validation 4: Mobile
                if (mobile.isEmpty()) {
                    result.addError("Row " + rowNum + ": Mobile is required.");
                    continue;
                }
                if (!MOBILE_PATTERN.matcher(mobile).matches()) {
                    result.addError("Row " + rowNum + ": Invalid mobile number format ('" + mobile + "').");
                    continue;
                }

                // Validation 5: Branch / Department Resolution
                if (branch.isEmpty()) {
                    result.addError("Row " + rowNum + ": Branch is required.");
                    continue;
                }

                Department resolvedDept = resolveDepartment(branch, allDepts);
                if (resolvedDept == null || !isMainStudentDepartment(resolvedDept)) {
                    result.addError("Row " + rowNum + ": " + branch + " is not allowed for student enrollment. Only MAIN departments are supported.");
                    continue;
                }

                // Validation 6: In-File Duplicate Check (Email must be unique)
                if (seenEmailsInFile.contains(email)) {
                    result.addError("Row " + rowNum + ": Duplicate email in uploaded file ('" + email + "').");
                    continue;
                }

                // Validation 7: Existing Pending Enrollment Duplicate Check (Email)
                if (existingEnrollmentEmails.contains(email)) {
                    result.addError("Row " + rowNum + ": Student with email '" + email + "' already exists in enrollment list.");
                    continue;
                }

                // Validation 8: Existing Student Duplicate Check (Email)
                if (existingStudentEmails.contains(email)) {
                    result.addError("Row " + rowNum + ": Student already exists with email '" + email + "'.");
                    continue;
                }

                seenEmailsInFile.add(email);

                // Create Pending Enrollment Entity (NO user or student record created)
                Enrollment enrollment = new Enrollment();
                enrollment.setFullName(name);
                enrollment.setGender(gender);
                enrollment.setEmail(email);
                enrollment.setMobile(mobile);
                enrollment.setDepartment(resolvedDept);
                if (sectionName != null && !sectionName.trim().isEmpty()) {
                    Section resolvedSection = resolveOrCreateSection(resolvedDept, sectionName, null);
                    enrollment.setSection(resolvedSection);
                }
                enrollment.setStatus(EnrollmentStatus.PENDING);
                enrollment.setCreatedBy(createdBy != null ? createdBy : "ADMIN");

                pendingToSave.add(enrollment);
                result.incrementImported();
            }
        }

        if (!pendingToSave.isEmpty()) {
            enrollmentRepository.saveAll(pendingToSave);
            log.info("Successfully persisted {} pending student enrollment records.", pendingToSave.size());

            Map<String, Object> auditDetails = new LinkedHashMap<>();
            auditDetails.put("operation", "BULK_IMPORT");
            auditDetails.put("totalRows", result.getTotalRows());
            auditDetails.put("successfulRows", pendingToSave.size());
            auditDetails.put("failedRows", result.getSkippedCount());

            auditService.log(
                    AuditAction.CREATE,
                    AuditModule.ENROLLMENT,
                    "ENROLLMENT_IMPORT",
                    null,
                    "Bulk student enrollment import: " + pendingToSave.size() + " students imported",
                    null,
                    auditDetails
            );
        } else {
            log.warn("Bulk student enrollment import finished with 0 imported records. Errors: {}", result.getErrors().size());
        }

        return result;
    }

    @Transactional
    public EnrollmentDto createSingleEnrollment(SingleEnrollmentRequestDto dto, String createdBy) {
        if (dto == null) {
            throw new IllegalArgumentException("Enrollment details cannot be empty.");
        }

        String name = dto.getFullName() != null ? dto.getFullName().trim().toUpperCase() : "";
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Student name is required.");
        }

        String gender = normalizeGender(dto.getGender());

        String email = dto.getEmail() != null ? dto.getEmail().trim().toLowerCase() : "";
        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("A valid email address is required.");
        }

        String mobile = dto.getMobile() != null ? dto.getMobile().trim() : "";
        if (mobile.isEmpty() || !MOBILE_PATTERN.matcher(mobile).matches()) {
            throw new IllegalArgumentException("A valid 10-digit mobile number is required.");
        }

        if (dto.getDepartmentId() == null) {
            throw new IllegalArgumentException("Department is required.");
        }

        Department dept = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Selected department not found."));

        if (dept.isDeleted() || !isMainStudentDepartment(dept)) {
            throw new IllegalArgumentException(dept.getName() + " is not allowed for enrollment. Only the 9 main engineering departments are supported.");
        }

        // Duplicate checks in Enrollment (Email must be unique)
        if (enrollmentRepository.existsByEmailAndDeletedFalse(email)) {
            throw new IllegalArgumentException("Student with email '" + email + "' already exists in enrollment list.");
        }

        // Duplicate checks in Student (Email must be unique)
        if (studentRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Student already exists with email '" + email + "'.");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setFullName(name);
        enrollment.setGender(gender);
        enrollment.setEmail(email);
        enrollment.setMobile(mobile);
        enrollment.setDepartment(dept);
        Section resolvedSection = resolveOrCreateSection(dept, dto.getSection(), dto.getSectionId());
        enrollment.setSection(resolvedSection);
        enrollment.setStatus(EnrollmentStatus.PENDING);
        enrollment.setCreatedBy(createdBy != null ? createdBy : "ADMIN");

        Enrollment saved = enrollmentRepository.save(enrollment);

        // Audit Log
        try {
            auditService.log(
                    AuditAction.CREATE,
                    AuditModule.ENROLLMENT,
                    "Enrollment",
                    saved.getId(),
                    "Added single pending student enrollment: " + saved.getFullName() + " (" + dept.getName() + ")",
                    null,
                    saved
            );
        } catch (Exception e) {
            log.warn("Failed to log audit for single enrollment: {}", e.getMessage());
        }

        return EnrollmentDto.fromEntity(saved, null);
    }

    @Transactional
    public EnrollmentDto updateEnrollment(Long id, SingleEnrollmentRequestDto dto, String updatedBy) {
        if (id == null) {
            throw new IllegalArgumentException("Enrollment ID is required.");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Enrollment details cannot be empty.");
        }

        Enrollment enrollment = enrollmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment record not found with ID: " + id));

        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new IllegalStateException("Cannot edit an already enrolled student record.");
        }

        String name = dto.getFullName() != null ? dto.getFullName().trim().toUpperCase() : "";
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Student name is required.");
        }

        String gender = normalizeGender(dto.getGender());

        String email = dto.getEmail() != null ? dto.getEmail().trim().toLowerCase() : "";
        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("A valid email address is required.");
        }

        String mobile = dto.getMobile() != null ? dto.getMobile().trim() : "";
        if (mobile.isEmpty() || !MOBILE_PATTERN.matcher(mobile).matches()) {
            throw new IllegalArgumentException("A valid 10-digit mobile number is required.");
        }

        if (dto.getDepartmentId() == null) {
            throw new IllegalArgumentException("Department is required.");
        }

        Department dept = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Selected department not found."));

        if (dept.isDeleted() || !isMainStudentDepartment(dept)) {
            throw new IllegalArgumentException(dept.getName() + " is not allowed for enrollment. Only the 9 main engineering departments are supported.");
        }

        // Duplicate checks in Enrollment (excluding this ID)
        if (enrollmentRepository.existsByEmailAndIdNotAndDeletedFalse(email, id)) {
            throw new IllegalArgumentException("Another student with email '" + email + "' already exists in enrollment list.");
        }

        // Duplicate checks in active Student table (if changed)
        if (!email.equalsIgnoreCase(enrollment.getEmail()) && studentRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Student already exists with email '" + email + "'.");
        }

        enrollment.setFullName(name);
        enrollment.setGender(gender);
        enrollment.setEmail(email);
        enrollment.setMobile(mobile);
        enrollment.setDepartment(dept);
        Section updatedSection = resolveOrCreateSection(dept, dto.getSection(), dto.getSectionId());
        enrollment.setSection(updatedSection);
        enrollment.setUpdatedBy(updatedBy != null ? updatedBy : "ADMIN");

        Enrollment saved = enrollmentRepository.save(enrollment);

        // Audit Log
        try {
            auditService.log(
                    AuditAction.UPDATE,
                    AuditModule.ENROLLMENT,
                    "Enrollment",
                    saved.getId(),
                    "Updated pending student enrollment: " + saved.getFullName() + " (" + dept.getName() + ")",
                    null,
                    saved
            );
        } catch (Exception e) {
            log.warn("Failed to log audit for enrollment update: {}", e.getMessage());
        }

        return EnrollmentDto.fromEntity(saved, null);
    }

    @Transactional
    public void deleteEnrollment(Long id, String deletedBy) {
        if (id == null) {
            throw new IllegalArgumentException("Enrollment ID is required.");
        }

        Enrollment enrollment = enrollmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment record not found with ID: " + id));

        enrollment.setDeleted(true);
        enrollment.setDeletedBy(deletedBy != null ? deletedBy : "ADMIN");
        enrollment.setDeletedAt(LocalDateTime.now());
        enrollment.setPermanentDeleteAt(LocalDateTime.now().plusDays(30));
        enrollmentRepository.save(enrollment);

        // If this was an enrolled student, also soft-delete the Student entity!
        if (enrollment.getEnrolledStudentId() != null) {
            Student student = studentRepository.findById(enrollment.getEnrolledStudentId()).orElse(null);
            if (student != null) {
                student.setDeleted(true);
                student.setActive(false);
                student.setDeletedAt(LocalDateTime.now());
                student.setPermanentDeleteAt(LocalDateTime.now().plusDays(30));
                student.setDeletedBy(deletedBy != null ? deletedBy : "ADMIN");
                studentRepository.save(student);
            }
        }

        // Audit Log
        try {
            auditService.log(
                    AuditAction.DELETE,
                    AuditModule.ENROLLMENT,
                    "Enrollment",
                    enrollment.getId(),
                    "Moved student enrollment to Recycle Bin: " + enrollment.getFullName(),
                    enrollment,
                    null
            );
        } catch (Exception e) {
            log.warn("Failed to log audit for enrollment delete: {}", e.getMessage());
        }
    }

    // ==========================================
    // 4. PUBLIC & ADMIN ENROLLMENT SELECTION FLOW
    // ==========================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMainStudentDepartments() {
        List<Department> mainDepts = departmentRepository.findByDepartmentTypeAndDeletedFalse(jjcet.PragatiX.enums.DepartmentType.MAIN);
        if (mainDepts == null || mainDepts.isEmpty()) {
            List<Department> all = departmentRepository.findAll();
            mainDepts = all.stream()
                    .filter(d -> !d.isDeleted() && (d.getDepartmentType() == null || d.getDepartmentType() == jjcet.PragatiX.enums.DepartmentType.MAIN))
                    .collect(Collectors.toList());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Department dept : mainDepts) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", dept.getId());
            map.put("name", dept.getName() != null ? dept.getName() : "");
            map.put("deptCode", dept.getDeptCode() != null ? dept.getDeptCode() : (dept.getCode() != null ? dept.getCode() : (dept.getName() != null ? dept.getName() : "")));
            map.put("departmentType", dept.getDepartmentType() != null ? dept.getDepartmentType().name() : "MAIN");
            result.add(map);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingDepartments() {
        return getMainStudentDepartments();
    }

    @Transactional(readOnly = true)
    public List<String> getPendingAlphabets(Long departmentId) {
        if (!isEnrollmentEnabled() || departmentId == null) {
            return Collections.emptyList();
        }

        return enrollmentRepository.findDistinctAlphabetsByDeptAndStatus(departmentId, EnrollmentStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<PendingStudentDto> getPendingStudents(Long departmentId, String letter) {
        if (!isEnrollmentEnabled() || departmentId == null || letter == null || letter.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String prefix = letter.trim().substring(0, 1).toUpperCase();
        List<Enrollment> list = enrollmentRepository.findPendingStudentsByDeptAndLetter(departmentId, prefix, EnrollmentStatus.PENDING);

        return list.stream().map(e -> new PendingStudentDto(
                e.getId(),
                e.getFullName(),
                PendingStudentDto.maskEmail(e.getEmail()),
                PendingStudentDto.maskEmail(e.getEmail()),
                PendingStudentDto.maskMobile(e.getMobile()),
                e.getDepartment() != null ? e.getDepartment().getId() : null,
                e.getDepartment() != null ? e.getDepartment().getName() : "",
                e.getDepartment() != null ? (e.getDepartment().getDeptCode() != null ? e.getDepartment().getDeptCode() : e.getDepartment().getCode()) : "",
                e.getSection() != null ? e.getSection().getId() : null,
                e.getSection() != null ? e.getSection().getSectionName() : null
        )).collect(Collectors.toList());
    }

    // ==========================================
    // 5. FINAL ATOMIC ENROLLMENT COMPLETION
    // ==========================================

    @Transactional
    public CompleteEnrollmentResponseDto completeEnrollment(Long enrollmentId) {
        if (!isEnrollmentEnabled()) {
            throw new IllegalStateException("Student enrollment is currently closed.");
        }

        if (enrollmentId == null) {
            throw new IllegalArgumentException("Enrollment ID is required.");
        }

        // 1. Acquire pessimistic lock to prevent concurrent duplicate enrollment
        Enrollment enrollment = enrollmentRepository.findByIdForUpdate(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment record not found."));

        // 2. Verify status is PENDING
        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new IllegalStateException("This student has already enrolled or is not pending enrollment.");
        }

        // 3. Duplicate checks in Student entity (Email must be unique)
        if (studentRepository.existsByEmail(enrollment.getEmail())) {
            throw new IllegalStateException("Student already exists with email: " + enrollment.getEmail());
        }

        // 4. Resolve necessary dependencies for Student creation
        Department dept = enrollment.getDepartment();

        // Resolve Gender safely: never allow genderRef to be null
        Gender genderRef = null;
        String rawGender = enrollment.getGender();
        if (rawGender != null && !rawGender.trim().isEmpty()) {
            String trimmedGender = rawGender.trim();
            genderRef = genderRepository.findByGenderName(trimmedGender).orElse(null);
            if (genderRef == null) {
                String norm = trimmedGender.toUpperCase();
                for (Gender g : genderRepository.findAll()) {
                    String gName = g.getGenderName() != null ? g.getGenderName().trim() : "";
                    if (gName.equalsIgnoreCase(norm) ||
                        (norm.startsWith("M") && gName.equalsIgnoreCase("Male")) ||
                        (norm.startsWith("F") && gName.equalsIgnoreCase("Female")) ||
                        (norm.startsWith("O") && gName.equalsIgnoreCase("Other"))) {
                        genderRef = g;
                        break;
                    }
                }
            }
        }
        if (genderRef == null) {
            genderRef = genderRepository.findByGenderName("Male").orElse(null);
            if (genderRef == null) {
                genderRef = genderRepository.findAll().stream().findFirst().orElse(null);
            }
            if (genderRef == null) {
                Gender defaultG = new Gender();
                defaultG.setGenderName("Male");
                genderRef = genderRepository.save(defaultG);
            }
        }

        // Resolve Section safely: if student has a section that is soft-deleted, restore it.
        // If department supports sections, ensure section is active in department.
        Section resolvedSection = enrollment.getSection();
        if (resolvedSection != null) {
            if (resolvedSection.isDeleted()) {
                resolvedSection.setDeleted(false);
                resolvedSection.setDeletedAt(null);
                resolvedSection.setPermanentDeleteAt(null);
                resolvedSection.setDeletedBy(null);
                resolvedSection = sectionRepository.save(resolvedSection);
            }
        }

        AcademicYear academicYear = academicYearRepository.findAll().stream()
                .findFirst()
                .orElse(null);

        Year yearRef = yearRepository.findByYearNo((byte) 1)
                .orElseGet(() -> yearRepository.findAll().stream().findFirst().orElse(null));

        Semester semesterRef = semesterRepository.findBySemesterNo((byte) 1)
                .orElseGet(() -> semesterRepository.findAll().stream().findFirst().orElse(null));

        ActivityStage initialStage = activityStageRepository
                .findByAcademicYearAndDisplayOrderAndDeletedFalse(jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR, 1)
                .orElse(null);

        // Generate unique regNo format
        String regNo = generateUniqueRegNo(dept);

        String mobileVal = (enrollment.getMobile() != null && enrollment.getMobile().trim().matches("^\\d+$"))
                ? enrollment.getMobile().trim() : null;

        int stageOrder = (initialStage != null && initialStage.getDisplayOrder() > 0)
                ? initialStage.getDisplayOrder() : 1;
        Long stageId = initialStage != null ? initialStage.getId() : null;

        Student student = Student.builder()
                .regNo(regNo)
                .fullName(enrollment.getFullName().trim())
                .email(enrollment.getEmail().trim())
                .phoneNo(mobileVal)
                .gender(genderRef != null ? genderRef.getGenderName() : enrollment.getGender())
                .genderRef(genderRef)
                .department(dept)
                .section(resolvedSection)
                .yearRef(yearRef)
                .year(yearRef != null ? String.valueOf(yearRef.getYearNo()) : "1")
                .semesterRef(semesterRef)
                .semester(semesterRef != null ? String.valueOf(semesterRef.getSemesterNo()) : "1")
                .active(true)
                .score(0)
                .totalXp(0)
                .groupXp(0)
                .individualXp(0)
                .mustXp(0)
                .stage(stageOrder)
                .currentStage(stageOrder)
                .currentStageId(stageId)
                .build();

        Student savedStudent = studentRepository.save(student);

        // 6. Transition enrollment status: PENDING -> ENROLLED
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        enrollment.setEnrolledStudentId(savedStudent.getId());
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        // 7. Audit log
        try {
            auditService.log(
                    AuditAction.ENROLLMENT_COMPLETED,
                    AuditModule.ENROLLMENT,
                    "Student",
                    savedStudent.getId(),
                    "Student enrolled successfully: " + savedStudent.getFullName() + " (" + (dept != null ? dept.getName() : "") + ")",
                    null,
                    savedStudent
            );
        } catch (Exception e) {
            log.warn("Failed to record audit log for enrollment completion: {}", e.getMessage());
        }

        return new CompleteEnrollmentResponseDto(
                true,
                "Enrollment completed successfully for " + savedStudent.getFullName() + ".",
                savedStudent.getId(),
                savedStudent.getFullName(),
                dept != null ? dept.getName() : ""
        );
    }

    // ==========================================
    // 6. ADMIN PENDING & ENROLLED LISTS
    // ==========================================

    @Transactional(readOnly = true)
    public Page<EnrollmentDto> getPendingList(Pageable pageable, String search, Long deptId) {
        return getEnrollmentPage(EnrollmentStatus.PENDING, pageable, search, deptId, null);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentDto> getEnrolledList(Pageable pageable, String search, Long deptId) {
        Page<Enrollment> page = fetchFilteredEnrollmentEntities(EnrollmentStatus.ENROLLED, pageable, search, deptId);

        // Collect student IDs to map regNos
        Set<Long> studentIds = page.getContent().stream()
                .map(Enrollment::getEnrolledStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> regNoMap = new HashMap<>();
        if (!studentIds.isEmpty()) {
            List<Student> students = studentRepository.findAllById(studentIds);
            for (Student s : students) {
                regNoMap.put(s.getId(), s.getRegNo());
            }
        }

        List<EnrollmentDto> dtos = page.getContent().stream()
                .map(e -> EnrollmentDto.fromEntity(e, regNoMap.get(e.getEnrolledStudentId())))
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    private Page<EnrollmentDto> getEnrollmentPage(EnrollmentStatus status, Pageable pageable, String search, Long deptId, Map<Long, String> regNoMap) {
        Page<Enrollment> page = fetchFilteredEnrollmentEntities(status, pageable, search, deptId);
        List<EnrollmentDto> dtos = page.getContent().stream()
                .map(e -> EnrollmentDto.fromEntity(e, regNoMap != null ? regNoMap.get(e.getEnrolledStudentId()) : null))
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    private Page<Enrollment> fetchFilteredEnrollmentEntities(EnrollmentStatus status, Pageable pageable, String search, Long deptId) {
        Set<Long> allowedDeptIds = getMainDepartmentIds();
        if (allowedDeptIds.isEmpty()) {
            return Page.empty(pageable);
        }

        boolean hasSearch = search != null && !search.trim().isEmpty();
        String cleanSearch = hasSearch ? search.trim() : null;

        if (deptId != null && deptId > 0) {
            if (hasSearch) {
                return enrollmentRepository.findByStatusAndDepartmentIdAndSearch(status, deptId, cleanSearch, pageable);
            } else {
                return enrollmentRepository.findByStatusAndDepartmentId(status, deptId, pageable);
            }
        } else {
            if (hasSearch) {
                return enrollmentRepository.findByStatusAndAllowedDeptIdsAndSearch(status, allowedDeptIds, cleanSearch, pageable);
            } else {
                return enrollmentRepository.findByStatusAndAllowedDeptIds(status, allowedDeptIds, pageable);
            }
        }
    }

    public boolean isMainStudentDepartment(Department d) {
        if (d == null || d.isDeleted()) return false;
        if (d.getDepartmentType() == null) return true;
        return d.getDepartmentType() == jjcet.PragatiX.enums.DepartmentType.MAIN;
    }

    public Set<Long> getMainDepartmentIds() {
        Set<Long> ids = departmentRepository.findByDepartmentTypeAndDeletedFalse(jjcet.PragatiX.enums.DepartmentType.MAIN)
                .stream()
                .map(Department::getId)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            ids = departmentRepository.findAll().stream()
                    .filter(d -> !d.isDeleted() && (d.getDepartmentType() == null || d.getDepartmentType() == jjcet.PragatiX.enums.DepartmentType.MAIN))
                    .map(Department::getId)
                    .collect(Collectors.toSet());
        }
        return ids;
    }

    // ==========================================
    // 7. HELPER UTILITIES
    // ==========================================

    private String generateUniqueRegNo(Department dept) {
        String deptPrefix = (dept != null && dept.getDeptCode() != null) ? dept.getDeptCode().toUpperCase() : "STU";
        deptPrefix = deptPrefix.replaceAll("[^A-Z0-9]", "");
        if (deptPrefix.length() > 5) {
            deptPrefix = deptPrefix.substring(0, 5);
        }

        long timestamp = System.currentTimeMillis() % 1000000;
        int random = new Random().nextInt(900) + 100;
        String regNo = deptPrefix + timestamp + random;

        while (studentRepository.existsByRegNo(regNo)) {
            random = new Random().nextInt(900) + 100;
            regNo = deptPrefix + (System.currentTimeMillis() % 1000000) + random;
        }
        return regNo;
    }

    private Department resolveDepartment(String branchName, List<Department> allDepts) {
        if (branchName == null || branchName.trim().isEmpty()) return null;
        String clean = branchName.trim();

        // 1. Direct name match
        for (Department d : allDepts) {
            if (d.getName() != null && d.getName().equalsIgnoreCase(clean)) {
                return d;
            }
        }

        // 2. Dept Code match
        for (Department d : allDepts) {
            if (d.getDeptCode() != null && d.getDeptCode().equalsIgnoreCase(clean)) {
                return d;
            }
            if (d.getCode() != null && d.getCode().equalsIgnoreCase(clean)) {
                return d;
            }
        }

        // 3. Normalized string similarity
        String simplifiedBranch = clean.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        for (Department d : allDepts) {
            if (d.getName() != null) {
                String simName = d.getName().replaceAll("[^A-Za-z0-9]", "").toLowerCase();
                if (simName.equals(simplifiedBranch) || simName.contains(simplifiedBranch) || simplifiedBranch.contains(simName)) {
                    return d;
                }
            }
        }

        return null;
    }

    private String normalizeGender(String gender) {
        if (gender == null) return "Male";
        String lower = gender.trim().toLowerCase();
        if (lower.startsWith("m")) return "Male";
        if (lower.startsWith("f")) return "Female";
        return "Other";
    }

    private String cleanMobileNumber(String raw) {
        if (raw == null) return "";
        // Remove decimal formatting from Excel numeric cells (e.g. 9876543210.0 -> 9876543210)
        String s = raw.replaceAll("\\.0$", "");
        s = s.replaceAll("[^0-9]", "");
        return s;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double numVal = cell.getNumericCellValue();
                if (numVal == (long) numVal) {
                    return String.format("%d", (long) numVal);
                } else {
                    return String.format("%s", numVal);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    public Section resolveOrCreateSection(Department dept, String sectionName, Long sectionId) {
        if (dept == null || !isMainStudentDepartment(dept)) {
            return null;
        }
        if (sectionId != null) {
            Section sec = sectionRepository.findById(sectionId).orElse(null);
            if (sec != null) {
                if (sec.isDeleted()) {
                    sec.setDeleted(false);
                    sec.setDeletedAt(null);
                    sec.setPermanentDeleteAt(null);
                    sec.setDeletedBy(null);
                    return sectionRepository.save(sec);
                }
                return sec;
            }
        }
        if (sectionName == null || sectionName.trim().isEmpty() || sectionName.trim().equalsIgnoreCase("none") || sectionName.trim().equalsIgnoreCase("no section") || sectionName.trim().equalsIgnoreCase("n/a")) {
            return null;
        }
        String cleanSection = sectionName.trim().toUpperCase();
        Optional<Section> existing = sectionRepository.findByDepartmentAndSectionName(dept, cleanSection);
        if (existing.isPresent()) {
            Section sec = existing.get();
            if (sec.isDeleted()) {
                sec.setDeleted(false);
                sec.setDeletedAt(null);
                sec.setPermanentDeleteAt(null);
                sec.setDeletedBy(null);
                return sectionRepository.save(sec);
            }
            return sec;
        }
        // Check if soft-deleted version exists
        Optional<Section> deletedSec = sectionRepository.findDeletedByDeptIdAndSectionName(dept.getId(), cleanSection);
        if (deletedSec.isPresent()) {
            Section sec = deletedSec.get();
            sec.setDeleted(false);
            sec.setDeletedAt(null);
            sec.setPermanentDeleteAt(null);
            sec.setDeletedBy(null);
            return sectionRepository.save(sec);
        }
        Section newSec = new Section();
        newSec.setDepartment(dept);
        newSec.setSectionName(cleanSection);
        return sectionRepository.save(newSec);
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
