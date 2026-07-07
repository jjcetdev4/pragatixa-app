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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashSet;
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

    @Transactional(readOnly = true)
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

                // Auto-resolve Department
                if (!deptName.isEmpty()) {
                    departmentRepository.findByDeptCode(deptName.toUpperCase().trim())
                        .or(() -> departmentRepository.findByCode(deptName.toUpperCase().trim()))
                        .or(() -> departmentRepository.findByName(deptName))
                        .ifPresent(d -> {
                            req.setDepartmentId(d.getId());
                            req.setDepartmentName(d.getName());
                        });
                }
                
                // Auto-resolve Gender
                if (!gender.isEmpty()) {
                    genderRepository.findByGenderName(gender.trim())
                        .ifPresent(g -> req.setGenderId(g.getId()));
                } else {
                    genderRepository.findAll().stream()
                        .filter(g -> g.getGenderName().equalsIgnoreCase("Male"))
                        .findFirst()
                        .ifPresent(g -> req.setGenderId(g.getId()));
                }

                // Auto-resolve Academic Year
                if (!academicYear.isEmpty()) {
                    academicYearRepository.findByAcademicYear(academicYear.trim())
                        .ifPresent(ay -> req.setAcademicYearId(ay.getId()));
                }

                // Auto-resolve Year
                if (!year.isEmpty()) {
                    try {
                        byte yNo = Byte.parseByte(year.trim());
                        yearRepository.findByYearNo(yNo)
                            .ifPresent(y -> req.setYearId(y.getId()));
                    } catch (Exception e) {
                        if (year.toLowerCase().contains("1") || year.toLowerCase().contains("first")) {
                            yearRepository.findByYearNo((byte) 1).ifPresent(y -> req.setYearId(y.getId()));
                        } else if (year.toLowerCase().contains("2") || year.toLowerCase().contains("second")) {
                            yearRepository.findByYearNo((byte) 2).ifPresent(y -> req.setYearId(y.getId()));
                        } else if (year.toLowerCase().contains("3") || year.toLowerCase().contains("third")) {
                            yearRepository.findByYearNo((byte) 3).ifPresent(y -> req.setYearId(y.getId()));
                        } else if (year.toLowerCase().contains("4") || year.toLowerCase().contains("fourth")) {
                            yearRepository.findByYearNo((byte) 4).ifPresent(y -> req.setYearId(y.getId()));
                        }
                    }
                }

                // Auto-resolve Semester
                if (!semester.isEmpty()) {
                    try {
                        byte sNo = Byte.parseByte(semester.trim());
                        semesterRepository.findBySemesterNo(sNo)
                            .ifPresent(s -> req.setSemesterId(s.getId()));
                    } catch (Exception e) {
                        for (byte i = 1; i <= 8; i++) {
                            if (semester.contains(String.valueOf(i))) {
                                final byte currentI = i;
                                semesterRepository.findBySemesterNo(currentI).ifPresent(s -> req.setSemesterId(s.getId()));
                                break;
                            }
                        }
                    }
                }

                // Auto-resolve Section
                if (!section.isEmpty() && req.getDepartmentId() != null) {
                    departmentRepository.findById(req.getDepartmentId()).ifPresent(d -> {
                        sectionRepository.findByDepartmentAndSectionName(d, section.trim())
                            .ifPresent(sec -> req.setSectionId(sec.getId()));
                    });
                }

                // Auto-resolve Group
                if (!groupName.isEmpty()) {
                    groupRepository.findByName(groupName.trim())
                        .ifPresent(g -> req.setGroupId(g.getId()));
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

    @Transactional
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

            for (CreateStudentRequest request : requests) {
                if (request.getStudentId() == null || request.getStudentId().trim().isEmpty() ||
                    request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                    continue;
                }

                String regNo = request.getStudentId().trim();
                String email = request.getEmail().trim();

                // Load required lookup entities
                Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department is required and must be valid for student: " + regNo));
                Section section = request.getSectionId() != null ? sectionRepository.findById(request.getSectionId()).orElse(null) : null;
                Gender gender = genderRepository.findById(request.getGenderId())
                    .orElseThrow(() -> new RuntimeException("Gender is required and must be valid for student: " + regNo));
                AcademicYear academicYear = academicYearRepository.findById(request.getAcademicYearId())
                    .orElseThrow(() -> new RuntimeException("Academic Year is required and must be valid for student: " + regNo));
                Year year = yearRepository.findById(request.getYearId())
                    .orElseThrow(() -> new RuntimeException("Year is required and must be valid for student: " + regNo));
                Semester semester = semesterRepository.findById(request.getSemesterId())
                    .orElseThrow(() -> new RuntimeException("Semester is required and must be valid for student: " + regNo));
                Group group = request.getGroupId() != null ? groupRepository.findById(request.getGroupId()).orElse(null) : null;

                // Default password to DOB (ddMMyyyy) or regNo if dob is null
                LocalDate dob = request.getDateOfBirth();
                String rawPassword = dob != null ? dob.format(DateTimeFormatter.ofPattern("ddMMyyyy")) : regNo;
                String encodedPassword = passwordEncoder.encode(rawPassword);

                Student student = studentRepository.findByStudentId(regNo)
                    .or(() -> studentRepository.findByEmail(email))
                    .orElse(null);

                if (student != null) {
                    student.setFullName(request.getFullName().trim());
                    student.setDepartment(department);
                    student.setSectionRef(section);
                    student.setGenderRef(gender);
                    student.setAcademicYearRef(academicYear);
                    student.setYearRef(year);
                    student.setSemesterRef(semester);
                    student.setGroup(group);
                    student.setSprNo(request.getSprNo() != null ? request.getSprNo().trim() : null);
                    student.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
                    student.setPhoneNo(request.getPhone() != null ? request.getPhone().trim() : "0000000000");
                    student.setDateOfBirth(dob);
                    student.setEmail(email);
                    student.setPassword(encodedPassword);
                    student.setAddress(request.getAddress());
                    student.setActive(request.getActive() != null ? request.getActive() : true);
                    
                    studentRepository.save(student);
                    updateCount++;
                } else {
                    student = Student.builder()
                        .studentId(regNo)
                        .fullName(request.getFullName().trim())
                        .email(email)
                        .password(encodedPassword)
                        .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                        .phoneNo(request.getPhone() != null ? request.getPhone().trim() : "0000000000")
                        .genderRef(gender)
                        .dateOfBirth(dob)
                        .department(department)
                        .sectionRef(section)
                        .academicYearRef(academicYear)
                        .yearRef(year)
                        .semesterRef(semester)
                        .group(group)
                        .sprNo(request.getSprNo() != null ? request.getSprNo().trim() : null)
                        .address(request.getAddress())
                        .active(request.getActive() != null ? request.getActive() : true)
                        .build();
                    studentRepository.save(student);
                    successCount++;
                }
            }
            return ApiResponse.ok("Bulk import processed: " + successCount + " students created, " + updateCount + " updated.", null);
        } catch (Exception e) {
            log.error("Bulk import failed", e);
            return ApiResponse.error("Bulk import failed: " + e.getMessage());
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
}
