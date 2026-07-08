package com.spdms.student;

import com.spdms.dto.*;
import com.spdms.entity.*;
import com.spdms.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Business logic for Student management.
 */
@Service
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final DisciplineLogRepository disciplineLogRepository;
    private final AcademicYearRepository academicYearRepository;
    private final YearRepository yearRepository;
    private final SemesterRepository semesterRepository;
    private final GenderRepository genderRepository;
    private final SectionRepository sectionRepository;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;

    public StudentService(StudentRepository studentRepository,
                          DepartmentRepository departmentRepository,
                          PasswordEncoder passwordEncoder,
                          UserRepository userRepository,
                          ActivitySubgroupRepository activitySubgroupRepository,
                          DisciplineLogRepository disciplineLogRepository,
                          AcademicYearRepository academicYearRepository,
                          YearRepository yearRepository,
                          SemesterRepository semesterRepository,
                          GenderRepository genderRepository,
                          SectionRepository sectionRepository,
                          RoleRepository roleRepository,
                          GroupRepository groupRepository) {
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.disciplineLogRepository = disciplineLogRepository;
        this.academicYearRepository = academicYearRepository;
        this.yearRepository = yearRepository;
        this.semesterRepository = semesterRepository;
        this.genderRepository = genderRepository;
        this.sectionRepository = sectionRepository;
        this.roleRepository = roleRepository;
        this.groupRepository = groupRepository;
    }

    // ── Create ───────────────────────────────────────

    @Transactional
    public ApiResponse<StudentResponse> createStudent(CreateStudentRequest request, String username) {
        User creator = userRepository.findByUsername(username).orElse(null);
        boolean isCcOrAdmin = creator != null && (creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"))
                || creator.getSubRoles().stream().map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC")));
        if (!isCcOrAdmin) {
            return ApiResponse.error("Access Denied: Only Class Coordinators (CC) can add students.");
        }

        if (studentRepository.existsByStudentId(request.getStudentId())) {
            return ApiResponse.error("Student ID '" + request.getStudentId() + "' already exists");
        }
        if (studentRepository.existsByEmail(request.getEmail())) {
            return ApiResponse.error("Email '" + request.getEmail() + "' is already registered");
        }

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId()).orElse(null);
        }
        if (department == null) {
            return ApiResponse.error("Department is required");
        }

        AcademicYear academicYear = null;
        if (request.getAcademicYearId() != null) {
            academicYear = academicYearRepository.findById(request.getAcademicYearId()).orElse(null);
        }
        if (academicYear == null) {
            return ApiResponse.error("Academic Year is required");
        }

        Year year = null;
        if (request.getYearId() != null) {
            year = yearRepository.findById(request.getYearId()).orElse(null);
        }
        if (year == null) {
            return ApiResponse.error("Year is required");
        }

        Semester semester = null;
        if (request.getSemesterId() != null) {
            semester = semesterRepository.findById(request.getSemesterId()).orElse(null);
        }
        if (semester == null) {
            return ApiResponse.error("Semester is required");
        }

        Gender gender = null;
        if (request.getGenderId() != null) {
            gender = genderRepository.findById(request.getGenderId()).orElse(null);
        }
        if (gender == null) {
            return ApiResponse.error("Gender is required");
        }

        Section section = request.getSectionId() != null ? sectionRepository.findById(request.getSectionId()).orElse(null) : null;
        Group group = request.getGroupId() != null ? groupRepository.findById(request.getGroupId()).orElse(null) : null;

        // Set default password to DOB (ddMMyyyy) if not specified
        String rawPassword = request.getPassword();
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            if (request.getDateOfBirth() != null) {
                rawPassword = request.getDateOfBirth().format(DateTimeFormatter.ofPattern("ddMMyyyy"));
            } else {
                rawPassword = "123456";
            }
        }

        Student student = Student.builder()
            .studentId(request.getStudentId().trim())
            .fullName(request.getFullName().trim())
            .email(request.getEmail().trim())
            .password(passwordEncoder.encode(rawPassword))
            .phone(request.getPhone() != null ? request.getPhone().trim() : null)
            .phoneNo(request.getPhone() != null ? request.getPhone().trim() : "0000000000")
            .dateOfBirth(request.getDateOfBirth())
            .address(request.getAddress())
            .department(department)
            .academicYearRef(academicYear)
            .academicYear(academicYear.getAcademicYear())
            .yearRef(year)
            .year(String.valueOf(year.getYearNo()))
            .semesterRef(semester)
            .semester(String.valueOf(semester.getSemesterNo()))
            .genderRef(gender)
            .gender(gender.getGenderName())
            .sectionRef(section)
            .section(section != null ? section.getSectionName() : null)
            .group(group)
            .sprNo(request.getSprNo() != null ? request.getSprNo().trim() : null)
            .active(true)
            .score(100)
            .build();

        Student saved = studentRepository.save(student);
        log.info("Registered new student: {} ({})", saved.getFullName(), saved.getStudentId());
        return ApiResponse.ok("Student created successfully", toResponse(saved));
    }

    // ── Bulk Parse & Import ─────────────────────────

    @Transactional
    public ApiResponse<List<CreateStudentRequest>> bulkParse(MultipartFile file, String username) {
        User creator = userRepository.findByUsername(username).orElse(null);
        boolean isCcOrAdmin = creator != null && (creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"))
                || creator.getSubRoles().stream().map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC")));
        if (!isCcOrAdmin) {
            return ApiResponse.error("Access Denied: Only Class Coordinators (CC) can parse student import files.");
        }
        try {
            List<CreateStudentRequest> parsedList = new ArrayList<>();
            Workbook workbook = WorkbookFactory.create(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                // Columns layout: 
                // 1: Name, 2: Department, 3: SPR_No, 4: Reg_No, 5: DOB, 6: Phone_No, 7: Email,
                // 8: Gender, 9: Academic Year, 10: Year, 11: Semester, 12: Section, 13: Group Name, 14: Address
                String name = getCellValueAsString(row.getCell(1));
                String deptName = getCellValueAsString(row.getCell(2));
                String sprNo = getCellValueAsString(row.getCell(3));
                String regNo = getCellValueAsString(row.getCell(4));
                LocalDate dob = parseLocalDate(row.getCell(5));
                String phoneNo = getCellValueAsString(row.getCell(6));
                String email = getCellValueAsString(row.getCell(7));
                String gender = getCellValueAsString(row.getCell(8));
                String academicYear = getCellValueAsString(row.getCell(9));
                String year = getCellValueAsString(row.getCell(10));
                String semester = getCellValueAsString(row.getCell(11));
                String section = getCellValueAsString(row.getCell(12));
                String groupName = getCellValueAsString(row.getCell(13));
                String address = getCellValueAsString(row.getCell(14));

                if (regNo.isEmpty() || email.isEmpty() || name.isEmpty()) {
                    continue; // Skip invalid rows
                }

                CreateStudentRequest req = new CreateStudentRequest();
                req.setFullName(name);
                req.setDepartmentName(deptName);
                req.setSprNo(sprNo);
                req.setStudentId(regNo);
                req.setDateOfBirth(dob);
                req.setPhone(phoneNo);
                req.setEmail(email);
                req.setGender(gender);
                req.setAcademicYear(academicYear);
                req.setYear(year);
                req.setSemester(semester);
                req.setSection(section);
                req.setAddress(address);
                req.setActive(true);

                // Auto-resolve/Auto-create Department
                if (!deptName.isEmpty()) {
                    String trimmedDept = deptName.trim();
                    String code = trimmedDept.length() > 6 ? trimmedDept.substring(0, 4).toUpperCase() : trimmedDept.toUpperCase();
                    
                    Department d = departmentRepository.findByDeptCode(code)
                        .or(() -> departmentRepository.findByCode(code))
                        .or(() -> departmentRepository.findByName(trimmedDept))
                        .orElseGet(() -> {
                            Department newDept = Department.builder()
                                .deptCode(code)
                                .deptName(trimmedDept)
                                .code(code)
                                .name(trimmedDept)
                                .description("Auto-created during bulk import")
                                .build();
                            return departmentRepository.save(newDept);
                        });
                    req.setDepartmentId(d.getId());
                    req.setDepartmentName(d.getName());
                }
                
                // Auto-resolve/Auto-create Gender
                if (!gender.isEmpty()) {
                    String genderTrim = gender.trim();
                    Gender g = genderRepository.findByGenderName(genderTrim)
                        .orElseGet(() -> genderRepository.save(
                            Gender.builder().genderName(genderTrim).build()
                        ));
                    req.setGenderId(g.getId());
                } else {
                    genderRepository.findAll().stream()
                        .filter(g -> g.getGenderName().equalsIgnoreCase("Male"))
                        .findFirst()
                        .ifPresentOrElse(
                            g -> req.setGenderId(g.getId()),
                            () -> {
                                Gender defaultMale = genderRepository.save(
                                    Gender.builder().genderName("Male").build()
                                );
                                req.setGenderId(defaultMale.getId());
                            }
                        );
                }

                // Auto-resolve/Auto-create Academic Year
                if (!academicYear.isEmpty()) {
                    String ayTrim = normalizeAcademicYear(academicYear);
                    AcademicYear ay = academicYearRepository.findByAcademicYear(ayTrim)
                        .orElseGet(() -> academicYearRepository.save(
                            AcademicYear.builder()
                                .academicYear(ayTrim)
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusYears(1))
                                .status(AcademicYear.Status.ACTIVE)
                                .build()
                        ));
                    req.setAcademicYearId(ay.getId());
                }

                // Auto-resolve/Auto-create Year
                if (!year.isEmpty()) {
                    try {
                        byte yNo = Byte.parseByte(year.trim());
                        Year y = yearRepository.findByYearNo(yNo)
                            .orElseGet(() -> yearRepository.save(
                                Year.builder().yearNo(yNo).yearName(yNo + " Year").build()
                            ));
                        req.setYearId(y.getId());
                    } catch (Exception e) {
                        byte fallbackVal = 1;
                        if (year.toLowerCase().contains("2") || year.toLowerCase().contains("second")) fallbackVal = 2;
                        else if (year.toLowerCase().contains("3") || year.toLowerCase().contains("third")) fallbackVal = 3;
                        else if (year.toLowerCase().contains("4") || year.toLowerCase().contains("fourth")) fallbackVal = 4;
                        
                        final byte finalFallback = fallbackVal;
                        Year y = yearRepository.findByYearNo(finalFallback)
                            .orElseGet(() -> yearRepository.save(
                                Year.builder().yearNo(finalFallback).yearName(finalFallback + " Year").build()
                            ));
                        req.setYearId(y.getId());
                    }
                }

                // Auto-resolve/Auto-create Semester
                if (!semester.isEmpty()) {
                    try {
                        byte sNo = Byte.parseByte(semester.trim());
                        Semester s = semesterRepository.findBySemesterNo(sNo)
                            .orElseGet(() -> semesterRepository.save(
                                Semester.builder().semesterNo(sNo).semesterName("Semester " + sNo).build()
                            ));
                        req.setSemesterId(s.getId());
                    } catch (Exception e) {
                        byte fallbackSem = 1;
                        for (byte i = 1; i <= 8; i++) {
                            if (semester.contains(String.valueOf(i))) {
                                fallbackSem = i;
                                break;
                            }
                        }
                        final byte finalFallbackSem = fallbackSem;
                        Semester s = semesterRepository.findBySemesterNo(finalFallbackSem)
                            .orElseGet(() -> semesterRepository.save(
                                Semester.builder().semesterNo(finalFallbackSem).semesterName("Semester " + finalFallbackSem).build()
                            ));
                        req.setSemesterId(s.getId());
                    }
                }

                // Auto-resolve/Auto-create Section
                if (!section.isEmpty() && req.getDepartmentId() != null) {
                    Department d = departmentRepository.findById(req.getDepartmentId()).orElse(null);
                    if (d != null) {
                        String secTrim = section.trim();
                        Section sec = sectionRepository.findByDepartmentAndSectionName(d, secTrim)
                            .orElseGet(() -> sectionRepository.save(
                                Section.builder().department(d).sectionName(secTrim).build()
                            ));
                        req.setSectionId(sec.getId());
                    }
                }

                // Auto-resolve/Auto-create Group
                if (!groupName.isEmpty()) {
                    String gTrim = groupName.trim();
                    Group g = groupRepository.findByName(gTrim)
                        .orElseGet(() -> groupRepository.save(
                            Group.builder().name(gTrim).build()
                        ));
                    req.setGroupId(g.getId());
                }

                parsedList.add(req);
            }
            workbook.close();
            return ApiResponse.ok("Spreadsheet file parsed successfully", parsedList);
        } catch (Exception e) {
            log.error("Bulk parse failed", e);
            return ApiResponse.error("Failed to parse spreadsheet file: " + e.getMessage());
        }
    }

    public ApiResponse<String> bulkImport(List<CreateStudentRequest> requests, String username) {
        User creator = userRepository.findByUsername(username).orElse(null);
        boolean isCcOrAdmin = creator != null && (creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"))
                || creator.getSubRoles().stream().map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC")));
        if (!isCcOrAdmin) {
            return ApiResponse.error("Access Denied: Only Class Coordinators (CC) can import students.");
        }
        try {
            int successCount = 0;
            int updateCount = 0;
            java.util.Set<String> processedStudentIds = new java.util.HashSet<>();
            java.util.Set<String> processedEmails = new java.util.HashSet<>();
            java.util.Set<String> processedSprs = new java.util.HashSet<>();

            for (CreateStudentRequest request : requests) {
                if (request.getStudentId() == null || request.getStudentId().trim().isEmpty() ||
                    request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                    continue;
                }

                String regNo = request.getStudentId().trim();
                String email = request.getEmail().trim();

                if (processedStudentIds.contains(regNo)) {
                    return ApiResponse.error("Duplicate Register No '" + regNo + "' found in the uploaded batch.");
                }
                if (processedEmails.contains(email)) {
                    return ApiResponse.error("Duplicate Email '" + email + "' found in the uploaded batch.");
                }
                if (request.getSprNo() != null && !request.getSprNo().trim().isEmpty()) {
                    String cleanSpr = request.getSprNo().trim();
                    if (processedSprs.contains(cleanSpr)) {
                        return ApiResponse.error("Duplicate SPR Number '" + cleanSpr + "' found in the uploaded batch.");
                    }
                    processedSprs.add(cleanSpr);
                }
                processedStudentIds.add(regNo);
                processedEmails.add(email);

                // 1. Auto-resolve missing Department ID
                if (request.getDepartmentId() == null && request.getDepartmentName() != null && !request.getDepartmentName().trim().isEmpty()) {
                    String deptName = request.getDepartmentName().trim();
                    String code = deptName.length() > 6 ? deptName.substring(0, 4).toUpperCase() : deptName.toUpperCase();
                    Department d = departmentRepository.findByDeptCode(code)
                        .or(() -> departmentRepository.findByCode(code))
                        .or(() -> departmentRepository.findByName(deptName))
                        .orElseGet(() -> departmentRepository.save(
                            Department.builder().deptCode(code).deptName(deptName).code(code).name(deptName).description("Auto-created during bulk import").build()
                        ));
                    request.setDepartmentId(d.getId());
                }

                // 2. Auto-resolve missing Gender ID
                if (request.getGenderId() == null && request.getGender() != null && !request.getGender().trim().isEmpty()) {
                    String genderTrim = request.getGender().trim();
                    Gender g = genderRepository.findByGenderName(genderTrim)
                        .orElseGet(() -> genderRepository.save(
                            Gender.builder().genderName(genderTrim).build()
                        ));
                    request.setGenderId(g.getId());
                }

                // 3. Auto-resolve missing Academic Year ID
                if (request.getAcademicYearId() == null && request.getAcademicYear() != null && !request.getAcademicYear().trim().isEmpty()) {
                    String ayTrim = normalizeAcademicYear(request.getAcademicYear());
                    AcademicYear ay = academicYearRepository.findByAcademicYear(ayTrim)
                        .orElseGet(() -> academicYearRepository.save(
                            AcademicYear.builder().academicYear(ayTrim).startDate(LocalDate.now()).endDate(LocalDate.now().plusYears(1)).status(AcademicYear.Status.ACTIVE).build()
                        ));
                    request.setAcademicYearId(ay.getId());
                }

                // 4. Auto-resolve missing Year ID
                if (request.getYearId() == null && request.getYear() != null && !request.getYear().trim().isEmpty()) {
                    try {
                        byte yNo = Byte.parseByte(request.getYear().trim());
                        Year y = yearRepository.findByYearNo(yNo)
                            .orElseGet(() -> yearRepository.save(
                                Year.builder().yearNo(yNo).yearName(yNo + " Year").build()
                            ));
                        request.setYearId(y.getId());
                    } catch (Exception e) {
                        byte fallbackVal = 1;
                        String yrLower = request.getYear().toLowerCase();
                        if (yrLower.contains("2") || yrLower.contains("second")) fallbackVal = 2;
                        else if (yrLower.contains("3") || yrLower.contains("third")) fallbackVal = 3;
                        else if (yrLower.contains("4") || yrLower.contains("fourth")) fallbackVal = 4;
                        final byte finalFallback = fallbackVal;
                        Year y = yearRepository.findByYearNo(finalFallback)
                            .orElseGet(() -> yearRepository.save(
                                Year.builder().yearNo(finalFallback).yearName(finalFallback + " Year").build()
                            ));
                        request.setYearId(y.getId());
                    }
                }

                // 5. Auto-resolve missing Semester ID
                if (request.getSemesterId() == null && request.getSemester() != null && !request.getSemester().trim().isEmpty()) {
                    try {
                        byte sNo = Byte.parseByte(request.getSemester().trim());
                        Semester s = semesterRepository.findBySemesterNo(sNo)
                            .orElseGet(() -> semesterRepository.save(
                                Semester.builder().semesterNo(sNo).semesterName("Semester " + sNo).build()
                            ));
                        request.setSemesterId(s.getId());
                    } catch (Exception e) {
                        byte fallbackSem = 1;
                        for (byte i = 1; i <= 8; i++) {
                            if (request.getSemester().contains(String.valueOf(i))) {
                                fallbackSem = i;
                                break;
                            }
                        }
                        final byte finalFallbackSem = fallbackSem;
                        Semester s = semesterRepository.findBySemesterNo(finalFallbackSem)
                            .orElseGet(() -> semesterRepository.save(
                                Semester.builder().semesterNo(finalFallbackSem).semesterName("Semester " + finalFallbackSem).build()
                            ));
                        request.setSemesterId(s.getId());
                    }
                }

                // 6. Auto-resolve missing Section ID
                if (request.getSectionId() == null && request.getSection() != null && !request.getSection().trim().isEmpty() && request.getDepartmentId() != null) {
                    Department d = departmentRepository.findById(request.getDepartmentId()).orElse(null);
                    if (d != null) {
                        String secTrim = request.getSection().trim();
                        Section sec = sectionRepository.findByDepartmentAndSectionName(d, secTrim)
                            .orElseGet(() -> sectionRepository.save(
                                Section.builder().department(d).sectionName(secTrim).build()
                            ));
                        request.setSectionId(sec.getId());
                    }
                }



                // Fallbacks for missing Year
                if (request.getYearId() == null) {
                    byte fallbackNo = 1;
                    Year firstYear = yearRepository.findByYearNo(fallbackNo)
                        .orElseGet(() -> yearRepository.save(
                            Year.builder().yearNo(fallbackNo).yearName("1 Year").build()
                        ));
                    request.setYearId(firstYear.getId());
                    request.setYear("1");
                }

                // Fallbacks for missing Semester
                if (request.getSemesterId() == null) {
                    byte fallbackNo = 1;
                    Semester firstSemester = semesterRepository.findBySemesterNo(fallbackNo)
                        .orElseGet(() -> semesterRepository.save(
                            Semester.builder().semesterNo(fallbackNo).semesterName("Semester 1").build()
                        ));
                    request.setSemesterId(firstSemester.getId());
                    request.setSemester("1");
                }

                // Fallbacks for missing Gender
                if (request.getGenderId() == null) {
                    Gender g = genderRepository.findAll().stream()
                        .filter(gen -> gen.getGenderName().equalsIgnoreCase("Male"))
                        .findFirst()
                        .orElseGet(() -> genderRepository.save(
                            Gender.builder().genderName("Male").build()
                        ));
                    request.setGenderId(g.getId());
                    request.setGender(g.getGenderName());
                }

                // Validation checks to prevent throwing RuntimeExceptions in Transaction boundary
                if (request.getDepartmentId() == null) {
                    return ApiResponse.error("Department is missing or not registered for student: " + request.getFullName());
                }
                if (request.getGenderId() == null) {
                    return ApiResponse.error("Gender is missing or not registered for student: " + request.getFullName());
                }
                if (request.getAcademicYearId() == null) {
                    return ApiResponse.error("Academic Year is missing or not registered for student: " + request.getFullName());
                }
                if (request.getYearId() == null) {
                    return ApiResponse.error("Year is missing or not registered for student: " + request.getFullName());
                }
                if (request.getSemesterId() == null) {
                    return ApiResponse.error("Semester is missing or not registered for student: " + request.getFullName());
                }

                // Load required lookup entities
                Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElse(null);
                if (department == null) {
                    return ApiResponse.error("Invalid Department ID for student: " + request.getFullName());
                }

                Section section = request.getSectionId() != null ? sectionRepository.findById(request.getSectionId()).orElse(null) : null;
                
                Gender gender = genderRepository.findById(request.getGenderId()).orElse(null);
                if (gender == null) {
                    return ApiResponse.error("Invalid Gender ID for student: " + request.getFullName());
                }

                AcademicYear academicYear = academicYearRepository.findById(request.getAcademicYearId()).orElse(null);
                if (academicYear == null) {
                    return ApiResponse.error("Invalid Academic Year ID for student: " + request.getFullName());
                }

                Year year = yearRepository.findById(request.getYearId()).orElse(null);
                if (year == null) {
                    return ApiResponse.error("Invalid Year ID for student: " + request.getFullName());
                }

                Semester semester = semesterRepository.findById(request.getSemesterId()).orElse(null);
                if (semester == null) {
                    return ApiResponse.error("Invalid Semester ID for student: " + request.getFullName());
                }

                Group group = request.getGroupId() != null ? groupRepository.findById(request.getGroupId()).orElse(null) : null;

                // Default password to DOB (ddMMyyyy) or regNo if dob is null
                LocalDate dob = request.getDateOfBirth();
                String rawPassword = dob != null ? dob.format(DateTimeFormatter.ofPattern("ddMMyyyy")) : regNo;
                String encodedPassword = passwordEncoder.encode(rawPassword);

                Student student = studentRepository.findByStudentId(regNo)
                    .or(() -> studentRepository.findByEmail(email))
                    .orElse(null);

                if (student != null) {
                    // Check database for duplicates before modifying
                    if (request.getSprNo() != null && !request.getSprNo().trim().isEmpty()) {
                        String cleanSpr = request.getSprNo().trim();
                        java.util.Optional<Student> duplicateSpr = studentRepository.findBySprNo(cleanSpr);
                        if (duplicateSpr.isPresent() && !duplicateSpr.get().getId().equals(student.getId())) {
                            return ApiResponse.error("SPR Number '" + cleanSpr + "' is already assigned to student: " + duplicateSpr.get().getFullName());
                        }
                    }
                    java.util.Optional<Student> duplicateEmail = studentRepository.findByEmail(email);
                    if (duplicateEmail.isPresent() && !duplicateEmail.get().getId().equals(student.getId())) {
                        return ApiResponse.error("Email '" + email + "' is already assigned to student: " + duplicateEmail.get().getFullName());
                    }

                    student.setFullName(request.getFullName().trim());
                    student.setDepartment(department);
                    student.setSectionRef(section);
                    student.setGenderRef(gender);
                    student.setAcademicYearRef(academicYear);
                    student.setYearRef(year);
                    student.setSemesterRef(semester);
                    student.setGroup(group);
                    student.setSprNo(request.getSprNo() != null && !request.getSprNo().trim().isEmpty() ? request.getSprNo().trim() : null);
                    student.setPhone(request.getPhone() != null && !request.getPhone().trim().isEmpty() ? request.getPhone().trim() : null);
                    student.setPhoneNo(request.getPhone() != null && !request.getPhone().trim().isEmpty() ? request.getPhone().trim() : "0000000000");
                    student.setDateOfBirth(dob);
                    student.setEmail(email);
                    student.setPassword(encodedPassword);
                    student.setAddress(request.getAddress());
                    student.setActive(request.getActive() != null ? request.getActive() : true);
                    // Set String properties
                    student.setAcademicYear(academicYear.getAcademicYear());
                    student.setYear(String.valueOf(year.getYearNo()));
                    student.setSemester(String.valueOf(semester.getSemesterNo()));
                    student.setGender(gender.getGenderName());
                    student.setSection(section != null ? section.getSectionName() : null);
                    
                    studentRepository.saveAndFlush(student);
                    updateCount++;
                } else {
                    // Check database for duplicates before inserting
                    if (studentRepository.existsByStudentId(regNo)) {
                        return ApiResponse.error("Student Register No '" + regNo + "' already exists.");
                    }
                    if (studentRepository.existsByEmail(email)) {
                        return ApiResponse.error("Email '" + email + "' already exists.");
                    }
                    if (request.getSprNo() != null && !request.getSprNo().trim().isEmpty()) {
                        String cleanSpr = request.getSprNo().trim();
                        if (studentRepository.findBySprNo(cleanSpr).isPresent()) {
                            return ApiResponse.error("SPR Number '" + cleanSpr + "' already exists.");
                        }
                    }

                    student = Student.builder()
                        .studentId(regNo)
                        .fullName(request.getFullName().trim())
                        .email(email)
                        .password(encodedPassword)
                        .phone(request.getPhone() != null && !request.getPhone().trim().isEmpty() ? request.getPhone().trim() : null)
                        .phoneNo(request.getPhone() != null && !request.getPhone().trim().isEmpty() ? request.getPhone().trim() : "0000000000")
                        .genderRef(gender)
                        .dateOfBirth(dob)
                        .department(department)
                        .sectionRef(section)
                        .academicYearRef(academicYear)
                        .yearRef(year)
                        .semesterRef(semester)
                        // String properties
                        .academicYear(academicYear.getAcademicYear())
                        .year(String.valueOf(year.getYearNo()))
                        .semester(String.valueOf(semester.getSemesterNo()))
                        .gender(gender.getGenderName())
                        .section(section != null ? section.getSectionName() : null)
                        .score(100)
                        .group(group)
                        .sprNo(request.getSprNo() != null && !request.getSprNo().trim().isEmpty() ? request.getSprNo().trim() : null)
                        .address(request.getAddress())
                        .active(request.getActive() != null ? request.getActive() : true)
                        .build();
                    studentRepository.saveAndFlush(student);
                    successCount++;
                }
            }
            return ApiResponse.ok("Bulk import processed: " + successCount + " students created, " + updateCount + " updated.", null);
        } catch (Exception e) {
            log.error("Bulk import failed", e);
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("import_error.log"))) {
                e.printStackTrace(pw);
            } catch (Exception ex) {
                // ignore
            }
            // Dig out the root cause SQL message if possible
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            return ApiResponse.error("Bulk import failed: " + root.getMessage());
        }
    }

    private LocalDate parseLocalDate(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        } else if (cell.getCellType() == CellType.STRING) {
            String val = cell.getStringCellValue().trim();
            if (val.isEmpty()) return null;
            try {
                return LocalDate.parse(val); // Try YYYY-MM-DD
            } catch (Exception e) {
                try {
                    DateTimeFormatter f = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                    return LocalDate.parse(val, f);
                } catch (Exception ex) {
                    try {
                        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        return LocalDate.parse(val, f);
                    } catch (Exception exc) {
                        log.warn("Unable to parse DOB string: {}", val);
                    }
                }
            }
        }
        return null;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.format("%.0f", cell.getNumericCellValue()).trim();
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue()).trim();
        }
        return "";
    }

    private String generateShortCode(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "DEPT";
        }
        String cleaned = name.replaceAll("[^a-zA-Z0-9 ]", "").trim();
        String[] words = cleaned.split("\\s+");
        if (words.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (String w : words) {
                if (!w.isEmpty() && !w.equalsIgnoreCase("and") && !w.equalsIgnoreCase("of") && !w.equalsIgnoreCase("the")) {
                    sb.append(w.charAt(0));
                }
            }
            String code = sb.toString().toUpperCase();
            return code.length() > 10 ? code.substring(0, 10) : (code.isEmpty() ? "DEPT" : code);
        } else {
            String code = cleaned.toUpperCase();
            return code.length() > 10 ? code.substring(0, 10) : (code.isEmpty() ? "DEPT" : code);
        }
    }

    // ── Get Single / List / Update ──────────────────

    @Transactional(readOnly = true)
    public ApiResponse<StudentResponse> getStudentById(Long id) {
        return studentRepository.findById(id)
            .map(s -> ApiResponse.ok(toResponse(s)))
            .orElseGet(() -> ApiResponse.error("Student not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public ApiResponse<Page<StudentResponse>> getAllStudents(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        Page<StudentResponse> result = studentRepository.findAll(pageable).map(this::toResponse);
        return ApiResponse.ok(result);
    }

    // ── Search ───────────────────────────────────────

    @Transactional(readOnly = true)
    public ApiResponse<Page<StudentResponse>> searchStudents(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());
        Page<StudentResponse> result = studentRepository.searchStudents(keyword, pageable).map(this::toResponse);
        return ApiResponse.ok(result);
    }

    // ── Delete ───────────────────────────────────────

    @Transactional
    public ApiResponse<Void> deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            return ApiResponse.error("Student not found with ID: " + id);
        }
        studentRepository.deleteById(id);
        log.info("Deleted student with ID: {}", id);
        return ApiResponse.ok("Student deleted successfully", null);
    }

    @Transactional
    public ApiResponse<StudentResponse> updateStudent(Long id, UpdateStudentRequest request) {
        Student student = studentRepository.findById(id).orElse(null);
        if (student == null) {
            return ApiResponse.error("Student not found with ID: " + id);
        }

        studentRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new RuntimeException("Email already registered by another student");
            }
        });

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId()).orElse(null);
        }
        if (department == null) {
            return ApiResponse.error("Department is required");
        }

        AcademicYear academicYear = null;
        if (request.getAcademicYearId() != null) {
            academicYear = academicYearRepository.findById(request.getAcademicYearId()).orElse(null);
        }
        if (academicYear == null) {
            return ApiResponse.error("Academic Year is required");
        }

        Year year = null;
        if (request.getYearId() != null) {
            year = yearRepository.findById(request.getYearId()).orElse(null);
        }
        if (year == null) {
            return ApiResponse.error("Year is required");
        }

        Semester semester = null;
        if (request.getSemesterId() != null) {
            semester = semesterRepository.findById(request.getSemesterId()).orElse(null);
        }
        if (semester == null) {
            return ApiResponse.error("Semester is required");
        }

        Gender gender = null;
        if (request.getGenderId() != null) {
            gender = genderRepository.findById(request.getGenderId()).orElse(null);
        }
        if (gender == null) {
            return ApiResponse.error("Gender is required");
        }

        Section section = request.getSectionId() != null ? sectionRepository.findById(request.getSectionId()).orElse(null) : null;
        Group group = request.getGroupId() != null ? groupRepository.findById(request.getGroupId()).orElse(null) : null;

        student.setFullName(request.getFullName().trim());
        student.setEmail(request.getEmail().trim());
        student.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        student.setPhoneNo(request.getPhone() != null ? request.getPhone().trim() : "0000000000");
        student.setAddress(request.getAddress());
        if (request.getDob() != null) {
            student.setDateOfBirth(request.getDob());
        }
        
        student.setDepartment(department);
        student.setAcademicYearRef(academicYear);
        student.setAcademicYear(academicYear.getAcademicYear());
        student.setYearRef(year);
        student.setYear(String.valueOf(year.getYearNo()));
        student.setSemesterRef(semester);
        student.setSemester(String.valueOf(semester.getSemesterNo()));
        student.setGenderRef(gender);
        student.setGender(gender.getGenderName());
        student.setSectionRef(section);
        student.setSection(section != null ? section.getSectionName() : null);
        student.setGroup(group);
        student.setSprNo(request.getSprNo() != null ? request.getSprNo().trim() : null);
        student.setActive(request.isActive());

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            student.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        Student saved = studentRepository.save(student);
        log.info("Updated student: {} ({})", saved.getFullName(), saved.getStudentId());
        return ApiResponse.ok("Student updated successfully", toResponse(saved));
    }

    // ── Mapper ───────────────────────────────────────

    private StudentResponse toResponse(Student student) {
        Long groupId = student.getGroup() != null ? student.getGroup().getId() : null;
        String groupName = student.getGroup() != null ? student.getGroup().getName() : null;
        boolean isCap = student.getGroup() != null && student.getGroup().getCaptain() != null && student.getGroup().getCaptain().getId().equals(student.getId());

        return StudentResponse.builder()
            .id(student.getId())
            .studentId(student.getStudentId())
            .fullName(student.getFullName())
            .email(student.getEmail())
            .phone(student.getPhone())
            .gender(student.getGender())
            .dateOfBirth(student.getDateOfBirth())
            .address(student.getAddress())
            .departmentName(student.getDepartment() != null ? student.getDepartment().getName() : null)
            .semester(student.getSemester())
            .academicYear(student.getAcademicYear())
            .year(student.getYear())
            .section(student.getSection())
            .active(student.isActive())
            .createdAt(student.getCreatedAt())
            .sprNo(student.getSprNo())
            .score(student.getScore())
            .groupId(groupId)
            .groupName(groupName)
            .isCaptain(isCap)
            .build();
    }

    @Transactional
    public ApiResponse<StudentResponse> adjustPoints(Long studentId, PointAdjustmentRequest request, String username) {
        User creator = userRepository.findByUsername(username).orElse(null);
        if (creator == null) {
            return ApiResponse.error("Unauthorized");
        }

        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            return ApiResponse.error("Student not found");
        }

        ActivitySubgroup subgroup = null;
        if (request.getSubgroupId() != null) {
            subgroup = activitySubgroupRepository.findById(request.getSubgroupId()).orElse(null);
            if (subgroup == null) {
                return ApiResponse.error("Activity subgroup not found");
            }

            // Verify assignment:
            if (subgroup.getAssignedFaculty() != null) {
                // If it is assigned to a specific faculty, verify that the logged-in user matches the assignee
                if (!subgroup.getAssignedFaculty().getId().equals(creator.getId())) {
                    boolean isAdmin = creator.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
                    if (!isAdmin) {
                        return ApiResponse.error("Access Denied: Only the assigned faculty (" + subgroup.getAssignedFaculty().getFullName() + ") can award points for this activity.");
                    }
                }
            }
        }

        // Adjust point score
        student.setScore(student.getScore() + request.getPoints());
        Student saved = studentRepository.save(student);

        // Record log
        DisciplineLog logEntry = DisciplineLog.builder()
                .student(student)
                .points(request.getPoints())
                .reason(request.getReason())
                .subgroup(subgroup)
                .recordedBy(creator)
                .incidentDate(LocalDateTime.now())
                .build();
        disciplineLogRepository.save(logEntry);

        log.info("Teacher {} adjusted student {} points by {}. Reason: {}", creator.getUsername(), student.getStudentId(), request.getPoints(), request.getReason());
        return ApiResponse.ok("Points updated successfully", toResponse(saved));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<DisciplineLog>> getDisciplineLogs(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            return ApiResponse.error("Student not found");
        }
        List<DisciplineLog> logs = disciplineLogRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        return ApiResponse.ok("Discipline logs loaded", logs);
    }

    @Transactional(readOnly = true)
    public ApiResponse<DepartmentPerformanceResponse> getDepartmentPerformance(String username) {
        User creator = userRepository.findByUsername(username).orElse(null);
        if (creator == null) {
            return ApiResponse.error("Unauthorized");
        }

        boolean isHodOrAdmin = creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"))
                || creator.getSubRoles().stream().map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("HOD"));

        if (!isHodOrAdmin) {
            return ApiResponse.error("Access Denied: Only Head of Department (HOD) can see department performance.");
        }

        Department department = creator.getDepartment();
        if (department == null) {
            return ApiResponse.error("No department assigned to this user.");
        }

        List<Student> students = studentRepository.findByDepartmentId(department.getId());
        long totalStudents = students.size();
        double overallAverage = students.stream()
                .mapToDouble(Student::getScore)
                .average()
                .orElse(100.0);

        Map<String, Double> yearWiseAverage = students.stream()
                .filter(s -> s.getAcademicYear() != null && !s.getAcademicYear().trim().isEmpty())
                .collect(Collectors.groupingBy(
                        Student::getAcademicYear,
                        TreeMap::new,
                        Collectors.averagingDouble(Student::getScore)
                ));

        DepartmentPerformanceResponse response = new DepartmentPerformanceResponse(
                department.getName(),
                overallAverage,
                totalStudents,
                yearWiseAverage
        );

        return ApiResponse.ok("Department performance metrics loaded", response);
    }

    @Transactional
    public ApiResponse<Void> promoteToCaptain(Long studentId) {
        Optional<Student> studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            return ApiResponse.error("Student not found");
        }
        Student student = studentOpt.get();
        Group group = student.getGroup();
        if (group == null) {
            String defaultGroupName = student.getFullName().trim() + "'s Group";
            if (groupRepository.existsByName(defaultGroupName)) {
                defaultGroupName = student.getFullName().trim() + " (" + student.getStudentId().trim() + ")'s Group";
            }
            if (groupRepository.existsByName(defaultGroupName)) {
                defaultGroupName = student.getFullName().trim() + " Group " + System.currentTimeMillis();
            }
            
            group = Group.builder()
                    .name(defaultGroupName)
                    .size(10) // Default max size of 10
                    .captain(student)
                    .build();
            group = groupRepository.save(group);
            student.setGroup(group);
            studentRepository.save(student);
        } else {
            group.setCaptain(student);
            groupRepository.save(group);
        }

        return ApiResponse.ok("Student promoted to Captain of group: " + group.getName(), null);
    }

    @Transactional
    public ApiResponse<Void> removeCaptain(Long studentId) {
        Optional<Student> studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            return ApiResponse.error("Student not found");
        }
        Student student = studentOpt.get();
        Group group = student.getGroup();
        if (group == null) {
            return ApiResponse.error("Student is not assigned to any group");
        }

        if (group.getCaptain() == null || !group.getCaptain().getId().equals(student.getId())) {
            return ApiResponse.error("Student is not the Captain of their group");
        }

        group.setCaptain(null);
        groupRepository.save(group);

        return ApiResponse.ok("Student removed from Captain of group: " + group.getName(), null);
    }

    private String normalizeAcademicYear(String input) {
        if (input == null) return "";
        String cleaned = input.replaceAll("\\s+", ""); // Remove all spaces
        if (cleaned.matches("\\d{4}-\\d{2}")) { // e.g. "2024-25"
            String start = cleaned.substring(0, 4);
            String endPrefix = cleaned.substring(0, 2);
            String endSuffix = cleaned.substring(5, 7);
            return start + "-" + endPrefix + endSuffix; // becomes "2024-2025"
        }
        return cleaned;
    }
}
