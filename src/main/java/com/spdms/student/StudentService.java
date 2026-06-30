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

    public StudentService(StudentRepository studentRepository,
                          DepartmentRepository departmentRepository,
                          PasswordEncoder passwordEncoder,
                          UserRepository userRepository,
                          ActivitySubgroupRepository activitySubgroupRepository,
                          DisciplineLogRepository disciplineLogRepository) {
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.disciplineLogRepository = disciplineLogRepository;
    }

    // ── Create ───────────────────────────────────────

    @Transactional
    public ApiResponse<StudentResponse> createStudent(CreateStudentRequest request, String username) {
        User creator = userRepository.findByUsername(username).orElse(null);
        boolean isCcOrAdmin = creator != null && (creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"))
                || creator.getSubRoles().stream().anyMatch(sr -> sr.trim().equalsIgnoreCase("CC")));
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
            if (department == null) {
                return ApiResponse.error("Department not found with ID: " + request.getDepartmentId());
            }
        }

        // Set default password to DOB (yyyy-MM-dd) if not specified
        String rawPassword = request.getPassword();
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            if (request.getDateOfBirth() != null) {
                rawPassword = request.getDateOfBirth().toString();
            } else {
                rawPassword = "123456";
            }
        }

        String finalYear = request.getYear();
        String finalSection = request.getSection();
        boolean isCc = creator != null && creator.getSubRoles().stream().anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));
        if (isCc) {
            if (creator.getYear() != null && !creator.getYear().trim().isEmpty()) {
                finalYear = creator.getYear();
            }
            if (creator.getSection() != null && !creator.getSection().trim().isEmpty()) {
                finalSection = creator.getSection();
            }
        }

        Student student = Student.builder()
            .studentId(request.getStudentId())
            .fullName(request.getFullName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(rawPassword))
            .phone(request.getPhone())
            .gender(request.getGender())
            .dateOfBirth(request.getDateOfBirth())
            .address(request.getAddress())
            .department(department)
            .semester(request.getSemester())
            .academicYear(request.getAcademicYear())
            .year(finalYear)
            .section(finalSection)
            .sprNo(request.getSprNo())
            .active(true)
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
                || creator.getSubRoles().stream().anyMatch(sr -> sr.trim().equalsIgnoreCase("CC")));
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

                // Columns layout: 1: Name, 2: Department, 3: SPR_No, 4: Reg_No, 5: DOB, 6: Phone_No, 7: Email
                String name = getCellValueAsString(row.getCell(1));
                String deptName = getCellValueAsString(row.getCell(2));
                String sprNo = getCellValueAsString(row.getCell(3));
                String regNo = getCellValueAsString(row.getCell(4));
                LocalDate dob = parseLocalDate(row.getCell(5));
                String phoneNo = getCellValueAsString(row.getCell(6));
                String email = getCellValueAsString(row.getCell(7));

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
                || creator.getSubRoles().stream().anyMatch(sr -> sr.trim().equalsIgnoreCase("CC")));
        if (!isCcOrAdmin) {
            return ApiResponse.error("Access Denied: Only Class Coordinators (CC) can import students.");
        }
        try {
            boolean isCc = creator != null && creator.getSubRoles().stream().anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));
            String defaultYear = null;
            String defaultSection = null;
            if (isCc) {
                defaultYear = creator.getYear();
                defaultSection = creator.getSection();
            }

            int successCount = 0;
            int updateCount = 0;

            for (CreateStudentRequest request : requests) {
                if (request.getStudentId() == null || request.getStudentId().trim().isEmpty() ||
                    request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                    continue;
                }

                String regNo = request.getStudentId().trim();
                String email = request.getEmail().trim();

                // Resolve Department
                Department department = null;
                String deptNameOrCode = request.getDepartmentName();
                if (deptNameOrCode != null && !deptNameOrCode.trim().isEmpty()) {
                    String cleanCode = deptNameOrCode.toUpperCase().trim();
                    if (cleanCode.length() > 10) {
                        cleanCode = generateShortCode(deptNameOrCode);
                    }
                    final String finalCleanCode = cleanCode;
                    department = departmentRepository.findByCode(finalCleanCode)
                        .orElseGet(() -> departmentRepository.findByName(deptNameOrCode)
                        .orElseGet(() -> {
                            Department newDept = Department.builder()
                                .code(finalCleanCode)
                                .name(deptNameOrCode)
                                .description("Created automatically during bulk student import")
                                .build();
                            return departmentRepository.save(newDept);
                        }));
                }

                // Default password to DOB (yyyy-MM-dd) or regNo if dob is null
                LocalDate dob = request.getDateOfBirth();
                String rawPassword = dob != null ? dob.toString() : regNo;
                String encodedPassword = passwordEncoder.encode(rawPassword);

                Student student = studentRepository.findByStudentId(regNo)
                    .or(() -> studentRepository.findByEmail(email))
                    .orElse(null);

                if (student != null) {
                    student.setFullName(request.getFullName().trim());
                    student.setDepartment(department);
                    student.setSprNo(request.getSprNo() != null ? request.getSprNo().trim() : null);
                    student.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
                    student.setDateOfBirth(dob);
                    student.setEmail(email);
                    student.setPassword(encodedPassword);
                    if (isCc) {
                        if (defaultYear != null) student.setYear(defaultYear);
                        if (defaultSection != null) student.setSection(defaultSection);
                    } else {
                        if (request.getYear() != null) student.setYear(request.getYear());
                        if (request.getSection() != null) student.setSection(request.getSection());
                    }
                    studentRepository.save(student);
                    updateCount++;
                } else {
                    String finalYear = request.getYear();
                    String finalSection = request.getSection();
                    if (isCc) {
                        if (defaultYear != null) finalYear = defaultYear;
                        if (defaultSection != null) finalSection = defaultSection;
                    }

                    student = Student.builder()
                        .studentId(regNo)
                        .fullName(request.getFullName().trim())
                        .email(email)
                        .password(encodedPassword)
                        .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                        .gender("MALE")
                        .dateOfBirth(dob)
                        .department(department)
                        .year(finalYear)
                        .section(finalSection)
                        .sprNo(request.getSprNo() != null ? request.getSprNo().trim() : null)
                        .active(true)
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
            if (department == null) {
                return ApiResponse.error("Department not found with ID: " + request.getDepartmentId());
            }
        }

        student.setFullName(request.getFullName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setGender(request.getGender());
        student.setDepartment(department);
        student.setSemester(request.getSemester());
        student.setAcademicYear(request.getAcademicYear());
        student.setActive(request.isActive());
        student.setSprNo(request.getSprNo());

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
                || creator.getSubRoles().stream().anyMatch(sr -> sr.trim().equalsIgnoreCase("HOD"));

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
