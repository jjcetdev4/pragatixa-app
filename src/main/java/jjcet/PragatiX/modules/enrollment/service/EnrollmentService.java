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
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[0-9]{10,15}$");

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

            // Exactly 6 columns
            String[] headers = {"Sl.No.", "Name", "Gender", "Email", "Mobile", "Branch"};
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
            sampleRow1.createCell(1).setCellValue("Arun Kumar");
            sampleRow1.createCell(2).setCellValue("Male");
            sampleRow1.createCell(3).setCellValue("arun@gmail.com");
            sampleRow1.createCell(4).setCellValue("9876543210");
            sampleRow1.createCell(5).setCellValue("Computer Science and Engineering");

            // Sample row 2
            Row sampleRow2 = sheet.createRow(2);
            sampleRow2.createCell(0).setCellValue(2);
            sampleRow2.createCell(1).setCellValue("Priya Kumar");
            sampleRow2.createCell(2).setCellValue("Female");
            sampleRow2.createCell(3).setCellValue("priya@gmail.com");
            sampleRow2.createCell(4).setCellValue("9876543211");
            sampleRow2.createCell(5).setCellValue("Information Technology");

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

            // 2. Branch Lookup (9 Approved Student Departments)
            List<String> branchNames = new ArrayList<>(APPROVED_STUDENT_DEPARTMENTS);

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
                }
            }

            // Default standard positions if not explicitly named
            if (nameCol == -1 && lastCellNum >= 2) nameCol = 1;
            if (genderCol == -1 && lastCellNum >= 3) genderCol = 2;
            if (emailCol == -1 && lastCellNum >= 4) emailCol = 3;
            if (mobileCol == -1 && lastCellNum >= 5) mobileCol = 4;
            if (branchCol == -1 && lastCellNum >= 6) branchCol = 5;

            if (nameCol == -1 || emailCol == -1 || mobileCol == -1 || branchCol == -1) {
                result.addError("Invalid template format. The template must contain: Sl.No., Name, Gender, Email, Mobile, Branch.");
                return result;
            }

            int lastRowNum = sheet.getLastRowNum();
            log.info("Detected {} total rows in enrollment Excel sheet.", lastRowNum);

            // Phase 1: Read and collect candidate values for batch pre-fetching
            List<Object[]> rawRows = new ArrayList<>();
            Set<String> candidateEmails = new HashSet<>();
            Set<String> candidateMobiles = new HashSet<>();

            for (int r = 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                int rowNum = r + 1; // 1-based display row
                String name = getCellValue(row.getCell(nameCol)).trim();
                String gender = genderCol != -1 ? getCellValue(row.getCell(genderCol)).trim() : "Male";
                String email = getCellValue(row.getCell(emailCol)).trim().toLowerCase();
                String mobile = cleanMobileNumber(getCellValue(row.getCell(mobileCol)).trim());
                String branch = getCellValue(row.getCell(branchCol)).trim();

                rawRows.add(new Object[]{rowNum, name, gender, email, mobile, branch});
                if (!email.isEmpty()) candidateEmails.add(email);
                if (!mobile.isEmpty()) candidateMobiles.add(mobile);
            }

            result.setTotalRows(rawRows.size());

            // Phase 2: Batch fetch existing conflicts from database in O(1) batch queries
            Set<String> existingEnrollmentEmails = candidateEmails.isEmpty() ? Collections.emptySet() : enrollmentRepository.findExistingEmailsIn(candidateEmails);
            Set<String> existingEnrollmentMobiles = candidateMobiles.isEmpty() ? Collections.emptySet() : enrollmentRepository.findExistingMobilesIn(candidateMobiles);
            Set<String> existingStudentEmails = candidateEmails.isEmpty() ? Collections.emptySet() : studentRepository.findExistingEmailsIn(candidateEmails);
            Set<String> existingStudentPhones = candidateMobiles.isEmpty() ? Collections.emptySet() : studentRepository.findExistingPhonesIn(candidateMobiles);

            Set<String> seenEmailsInFile = new HashSet<>();
            Set<String> seenMobilesInFile = new HashSet<>();

            // Phase 3: Validate rows in-memory
            for (Object[] raw : rawRows) {
                int rowNum = (int) raw[0];
                String name = (String) raw[1];
                String gender = (String) raw[2];
                String email = (String) raw[3];
                String mobile = (String) raw[4];
                String branch = (String) raw[5];

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

                String lowerBranch = branch.trim().toLowerCase();
                if (lowerBranch.contains("tamil") || lowerBranch.contains("chemistry") || lowerBranch.contains("math") || lowerBranch.contains("english") || lowerBranch.contains("physics")) {
                    result.addError("Row " + rowNum + ": " + branch + " is not allowed for enrollment. Only the 9 main departments are supported.");
                    continue;
                }

                Department resolvedDept = resolveDepartment(branch, allDepts);
                if (resolvedDept == null || !isMainStudentDepartment(resolvedDept)) {
                    result.addError("Row " + rowNum + ": " + branch + " is not allowed for enrollment. Only the 9 main departments are supported.");
                    continue;
                }

                // Validation 6: In-File Duplicate Check
                if (seenEmailsInFile.contains(email)) {
                    result.addError("Row " + rowNum + ": Duplicate email in uploaded file ('" + email + "').");
                    continue;
                }
                if (seenMobilesInFile.contains(mobile)) {
                    result.addError("Row " + rowNum + ": Duplicate mobile in uploaded file ('" + mobile + "').");
                    continue;
                }

                // Validation 7: Existing Pending Enrollment Duplicate Check
                if (existingEnrollmentEmails.contains(email)) {
                    result.addError("Row " + rowNum + ": Student with email '" + email + "' already exists in enrollment list.");
                    continue;
                }
                if (existingEnrollmentMobiles.contains(mobile)) {
                    result.addError("Row " + rowNum + ": Student with mobile '" + mobile + "' already exists in enrollment list.");
                    continue;
                }

                // Validation 8: Existing Student Duplicate Check
                if (existingStudentEmails.contains(email)) {
                    result.addError("Row " + rowNum + ": Student already exists with email '" + email + "'.");
                    continue;
                }
                if (existingStudentPhones.contains(mobile)) {
                    result.addError("Row " + rowNum + ": Student already exists with mobile '" + mobile + "'.");
                    continue;
                }

                seenEmailsInFile.add(email);
                seenMobilesInFile.add(mobile);

                // Create Pending Enrollment Entity (NO user or student record created)
                Enrollment enrollment = new Enrollment();
                enrollment.setFullName(name);
                enrollment.setGender(gender);
                enrollment.setEmail(email);
                enrollment.setMobile(mobile);
                enrollment.setDepartment(resolvedDept);
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

        String name = dto.getFullName() != null ? dto.getFullName().trim() : "";
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
            throw new IllegalArgumentException("A valid 10-15 digit mobile number is required.");
        }

        if (dto.getDepartmentId() == null) {
            throw new IllegalArgumentException("Department is required.");
        }

        Department dept = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Selected department not found."));

        if (dept.isDeleted() || !isMainStudentDepartment(dept)) {
            throw new IllegalArgumentException(dept.getName() + " is not allowed for enrollment. Only the 9 main engineering departments are supported.");
        }

        // Duplicate checks in Enrollment
        if (enrollmentRepository.existsByEmailAndDeletedFalse(email)) {
            throw new IllegalArgumentException("Student with email '" + email + "' already exists in enrollment list.");
        }
        if (enrollmentRepository.existsByMobileAndDeletedFalse(mobile)) {
            throw new IllegalArgumentException("Student with mobile '" + mobile + "' already exists in enrollment list.");
        }

        // Duplicate checks in Student
        if (studentRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Student already exists with email '" + email + "'.");
        }
        if (studentRepository.existsByPhoneNo(mobile)) {
            throw new IllegalArgumentException("Student already exists with mobile '" + mobile + "'.");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setFullName(name);
        enrollment.setGender(gender);
        enrollment.setEmail(email);
        enrollment.setMobile(mobile);
        enrollment.setDepartment(dept);
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

        String name = dto.getFullName() != null ? dto.getFullName().trim() : "";
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
            throw new IllegalArgumentException("A valid 10-15 digit mobile number is required.");
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
        if (enrollmentRepository.existsByMobileAndIdNotAndDeletedFalse(mobile, id)) {
            throw new IllegalArgumentException("Another student with mobile '" + mobile + "' already exists in enrollment list.");
        }

        // Duplicate checks in active Student table (if changed)
        if (!email.equalsIgnoreCase(enrollment.getEmail()) && studentRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Student already exists with email '" + email + "'.");
        }
        if (!mobile.equals(enrollment.getMobile()) && studentRepository.existsByPhoneNo(mobile)) {
            throw new IllegalArgumentException("Student already exists with mobile '" + mobile + "'.");
        }

        enrollment.setFullName(name);
        enrollment.setGender(gender);
        enrollment.setEmail(email);
        enrollment.setMobile(mobile);
        enrollment.setDepartment(dept);
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

        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new IllegalStateException("Cannot delete an already enrolled student record.");
        }

        enrollment.setDeleted(true);
        enrollment.setDeletedBy(deletedBy != null ? deletedBy : "ADMIN");
        enrollment.setDeletedAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        // Audit Log
        try {
            auditService.log(
                    AuditAction.DELETE,
                    AuditModule.ENROLLMENT,
                    "Enrollment",
                    enrollment.getId(),
                    "Deleted pending student enrollment: " + enrollment.getFullName(),
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
        List<Department> allDepts = departmentRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        Set<Long> addedDeptIds = new HashSet<>();

        for (String approvedName : APPROVED_STUDENT_DEPARTMENTS) {
            Department dept = resolveDepartment(approvedName, allDepts);
            if (dept != null && !dept.isDeleted() && addedDeptIds.add(dept.getId())) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", dept.getId());
                map.put("name", dept.getName());
                map.put("deptCode", dept.getDeptCode() != null ? dept.getDeptCode() : (dept.getCode() != null ? dept.getCode() : dept.getName()));
                result.add(map);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingDepartments() {
        if (!isEnrollmentEnabled()) {
            return Collections.emptyList();
        }
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
                PendingStudentDto.maskMobile(e.getMobile()),
                e.getDepartment() != null ? e.getDepartment().getId() : null,
                e.getDepartment() != null ? e.getDepartment().getName() : "",
                e.getDepartment() != null ? (e.getDepartment().getDeptCode() != null ? e.getDepartment().getDeptCode() : e.getDepartment().getCode()) : ""
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

        // 3. Duplicate checks in Student entity
        if (studentRepository.existsByEmail(enrollment.getEmail())) {
            throw new IllegalStateException("Student already exists with email: " + enrollment.getEmail());
        }
        if (studentRepository.existsByPhoneNo(enrollment.getMobile())) {
            throw new IllegalStateException("Student already exists with mobile: " + enrollment.getMobile());
        }

        // 4. Resolve necessary dependencies for Student creation
        Department dept = enrollment.getDepartment();

        Gender genderRef = genderRepository.findByGenderName(enrollment.getGender()).orElse(null);
        if (genderRef == null) {
            genderRef = genderRepository.findAll().stream().findFirst().orElse(null);
        }

        AcademicYear academicYear = academicYearRepository.findAll().stream()
                .findFirst()
                .orElse(null);

        Year yearRef = yearRepository.findByYearNo((byte) 1)
                .orElseGet(() -> yearRepository.findAll().stream().findFirst().orElse(null));

        Semester semesterRef = semesterRepository.findBySemesterNo((byte) 1)
                .orElseGet(() -> semesterRepository.findAll().stream().findFirst().orElse(null));

        ActivityStage initialStage = activityStageRepository.findFirstByIsActiveTrueOrderByDisplayOrderAsc()
                .orElseGet(() -> activityStageRepository.findAll().stream().findFirst().orElse(null));

        // Generate unique regNo format
        String regNo = generateUniqueRegNo(dept);

        // 5. Create Student record (NO user record created)
        Student student = Student.builder()
                .regNo(regNo)
                .fullName(enrollment.getFullName().trim())
                .email(enrollment.getEmail().trim())
                .phone(enrollment.getMobile().trim())
                .phoneNo(enrollment.getMobile().trim())
                .gender(genderRef != null ? genderRef.getGenderName() : enrollment.getGender())
                .genderRef(genderRef)
                .department(dept)
                .academicYearRef(academicYear)
                .academicYear(academicYear != null ? academicYear.getAcademicYear() : "1")
                .yearRef(yearRef)
                .year(yearRef != null ? String.valueOf(yearRef.getYearNo()) : "1")
                .semesterRef(semesterRef)
                .semester(semesterRef != null ? String.valueOf(semesterRef.getSemesterNo()) : "1")
                .password(passwordEncoder.encode("123456"))
                .active(true)
                .score(100)
                .totalXp(0)
                .groupXp(0)
                .individualXp(0)
                .mustXp(0)
                .stage(initialStage != null ? initialStage.getDisplayOrder() : 1)
                .currentStage(initialStage != null ? initialStage.getDisplayOrder() : 1)
                .currentStageId(initialStage != null ? initialStage.getId() : null)
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
        if (d == null || d.getName() == null) return false;
        String name = d.getName().trim().toLowerCase();
        if (name.contains("tamil") || name.contains("chemistry") || name.contains("math") || name.contains("english") || name.contains("physics")) {
            return false;
        }
        for (String approved : APPROVED_STUDENT_DEPARTMENTS) {
            if (approved.equalsIgnoreCase(d.getName().trim())) {
                return true;
            }
        }
        return false;
    }

    public Set<Long> getMainDepartmentIds() {
        List<Department> allDepts = departmentRepository.findAll();
        Set<Long> ids = new HashSet<>();
        for (String approvedName : APPROVED_STUDENT_DEPARTMENTS) {
            Department dept = resolveDepartment(approvedName, allDepts);
            if (dept != null && !dept.isDeleted()) {
                ids.add(dept.getId());
            }
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
