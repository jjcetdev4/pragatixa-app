package com.spdms.modules.student.service;

import com.spdms.dto.*;
import com.spdms.modules.activity.dto.request.*;
import com.spdms.modules.activity.dto.response.*;
import com.spdms.modules.student.dto.request.*;
import com.spdms.modules.student.dto.response.*;
import com.spdms.common.response.ApiResponse;
import com.spdms.entity.*;
import com.spdms.repository.*;
import com.spdms.modules.activity.repository.*;
import com.spdms.modules.faculty.repository.*;
import com.spdms.modules.student.repository.*;
import com.spdms.modules.authentication.repository.UserRepository;
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

    public StudentImportService(AcademicYearRepository academicYearRepository, DepartmentRepository departmentRepository, GenderRepository genderRepository, PasswordEncoder passwordEncoder, SectionRepository sectionRepository, SemesterRepository semesterRepository, StudentRepository studentRepository, TeamRepository teamRepository, UserRepository userRepository, YearRepository yearRepository, ExcelStudentParser excelStudentParser, StudentImportResolverService resolverService) {
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
    }

    @Transactional
    public ApiResponse<List<CreateStudentRequest>> bulkParse(MultipartFile file, String username) {
        User creator = userRepository.findByUsername(username).orElse(null);
        boolean isCcOrAdmin = creator != null && (creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"))
                || creator.getSubRoles().stream().map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC")));
        if (!isCcOrAdmin) {
            return ApiResponse.error("Access Denied: Only Class Coordinators (CC) can parse student import files.");
        }
        try (java.io.InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {
            List<CreateStudentRequest> parsedList = new ArrayList<>();
            Sheet sheet = workbook.getSheetAt(0);

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String name = excelStudentParser.getCellValueAsString(row.getCell(1));
                String deptName = excelStudentParser.getCellValueAsString(row.getCell(2));
                String sprNo = excelStudentParser.getCellValueAsString(row.getCell(3));
                String regNo = excelStudentParser.getCellValueAsString(row.getCell(4));
                LocalDate dob = excelStudentParser.parseLocalDate(row.getCell(5));
                String phoneNo = excelStudentParser.getCellValueAsString(row.getCell(6));
                String email = excelStudentParser.getCellValueAsString(row.getCell(7));
                String gender = excelStudentParser.getCellValueAsString(row.getCell(8));
                String academicYear = excelStudentParser.getCellValueAsString(row.getCell(9));
                String year = excelStudentParser.getCellValueAsString(row.getCell(10));
                String semester = excelStudentParser.getCellValueAsString(row.getCell(11));
                String section = excelStudentParser.getCellValueAsString(row.getCell(12));
                String teamName = excelStudentParser.getCellValueAsString(row.getCell(13));
                String address = excelStudentParser.getCellValueAsString(row.getCell(14));

                if (regNo.isEmpty() || email.isEmpty() || name.isEmpty()) {
                    continue;
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
                req.setAcademicYear(academicYear);
                req.setYear(year);
                req.setSemester(semester);
                req.setSection(section);
                req.setAddress(address);
                req.setActive(true);

                req.setDepartmentId(resolverService.resolveDepartment(deptName));
                req.setGenderId(resolverService.resolveGender(gender));
                req.setAcademicYearId(resolverService.resolveAcademicYear(academicYear));
                req.setYearId(resolverService.resolveYear(year));
                req.setSemesterId(resolverService.resolveSemester(semester));
                req.setSectionId(resolverService.resolveSection(section, req.getDepartmentId()));

                if (!teamName.isEmpty()) {
                    String gTrim = teamName.trim();
                    Team g = teamRepository.findByName(gTrim).orElseGet(() -> teamRepository.save(Team.builder().name(gTrim).build()));
                    req.setTeamId(g.getId());
                }

                parsedList.add(req);
            }
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
            List<Student> studentsToSave = new ArrayList<>();
            java.util.Map<Long, Department> deptMap = new java.util.HashMap<>();
            java.util.Map<Long, Section> sectionMap = new java.util.HashMap<>();
            java.util.Map<Long, Gender> genderMap = new java.util.HashMap<>();
            java.util.Map<Long, AcademicYear> academicYearMap = new java.util.HashMap<>();
            java.util.Map<Long, Year> yearMap = new java.util.HashMap<>();
            java.util.Map<Long, Semester> semesterMap = new java.util.HashMap<>();
            java.util.Map<Long, Team> teamMap = new java.util.HashMap<>();


            for (CreateStudentRequest request : requests) {
                if (request.getRegNo() == null || request.getRegNo().trim().isEmpty() ||
                    request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                    continue;
                }

                String regNo = request.getRegNo().trim();
                String email = request.getEmail().trim();

                if (processedStudentIds.contains(regNo)) return ApiResponse.error("Duplicate Register No '" + regNo + "' found in the uploaded batch.");
                if (processedEmails.contains(email)) return ApiResponse.error("Duplicate Email '" + email + "' found in the uploaded batch.");
                if (request.getSprNo() != null && !request.getSprNo().trim().isEmpty()) {
                    String cleanSpr = request.getSprNo().trim();
                    if (processedSprs.contains(cleanSpr)) return ApiResponse.error("Duplicate SPR Number '" + cleanSpr + "' found in the uploaded batch.");
                    processedSprs.add(cleanSpr);
                }
                processedStudentIds.add(regNo);
                processedEmails.add(email);

                if (request.getDepartmentId() == null) request.setDepartmentId(resolverService.resolveDepartment(request.getDepartmentName()));
                if (request.getGenderId() == null) request.setGenderId(resolverService.resolveGender(request.getGender()));
                if (request.getAcademicYearId() == null) request.setAcademicYearId(resolverService.resolveAcademicYear(request.getAcademicYear()));
                if (request.getYearId() == null) request.setYearId(resolverService.resolveYear(request.getYear()));
                if (request.getSemesterId() == null) request.setSemesterId(resolverService.resolveSemester(request.getSemester()));
                if (request.getSectionId() == null) request.setSectionId(resolverService.resolveSection(request.getSection(), request.getDepartmentId()));

                if (request.getDepartmentId() == null) return ApiResponse.error("Department is missing or not registered for student: " + request.getFullName());
                if (request.getGenderId() == null) return ApiResponse.error("Gender is missing or not registered for student: " + request.getFullName());
                if (request.getAcademicYearId() == null) return ApiResponse.error("Academic Year is missing or not registered for student: " + request.getFullName());
                if (request.getYearId() == null) return ApiResponse.error("Year is missing or not registered for student: " + request.getFullName());
                if (request.getSemesterId() == null) return ApiResponse.error("Semester is missing or not registered for student: " + request.getFullName());

                Department department = deptMap.computeIfAbsent(request.getDepartmentId(), id -> departmentRepository.findById(id).orElse(null));
                Section section = request.getSectionId() != null ? sectionMap.computeIfAbsent(request.getSectionId(), id -> sectionRepository.findById(id).orElse(null)) : null;
                Gender gender = genderMap.computeIfAbsent(request.getGenderId(), id -> genderRepository.findById(id).orElse(null));
                AcademicYear academicYear = academicYearMap.computeIfAbsent(request.getAcademicYearId(), id -> academicYearRepository.findById(id).orElse(null));
                Year year = yearMap.computeIfAbsent(request.getYearId(), id -> yearRepository.findById(id).orElse(null));
                Semester semester = semesterMap.computeIfAbsent(request.getSemesterId(), id -> semesterRepository.findById(id).orElse(null));
                Team team = request.getTeamId() != null ? teamMap.computeIfAbsent(request.getTeamId(), id -> teamRepository.findById(id).orElse(null)) : null;

                LocalDate dob = request.getDateOfBirth();
                String rawPassword = dob != null ? dob.format(DateTimeFormatter.ofPattern("ddMMyyyy")) : regNo;
                String encodedPassword = passwordEncoder.encode(rawPassword);

                Student student = studentRepository.findByRegNo(regNo).or(() -> studentRepository.findByEmail(email)).orElse(null);

                if (student != null) {
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
                    student.setSection(section);
                    student.setGenderRef(gender);
                    student.setAcademicYearRef(academicYear);
                    student.setYearRef(year);
                    student.setSemesterRef(semester);
                    student.setTeam(team);
                    student.setSprNo(request.getSprNo() != null && !request.getSprNo().trim().isEmpty() ? request.getSprNo().trim() : null);
                    student.setPhone(request.getPhone() != null && !request.getPhone().trim().isEmpty() ? request.getPhone().trim() : null);
                    student.setPhoneNo(request.getPhone() != null && !request.getPhone().trim().isEmpty() ? request.getPhone().trim() : "0000000000");
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
                    updateCount++;
                } else {
                    if (studentRepository.existsByRegNo(regNo)) return ApiResponse.error("Student Register No '" + regNo + "' already exists.");
                    if (studentRepository.existsByEmail(email)) return ApiResponse.error("Email '" + email + "' already exists.");
                    if (request.getSprNo() != null && !request.getSprNo().trim().isEmpty()) {
                        String cleanSpr = request.getSprNo().trim();
                        if (studentRepository.findBySprNo(cleanSpr).isPresent()) return ApiResponse.error("SPR Number '" + cleanSpr + "' already exists.");
                    }

                    student = Student.builder()
                        .regNo(regNo)
                        .fullName(request.getFullName().trim())
                        .email(email)
                        .password(encodedPassword)
                        .phone(request.getPhone() != null && !request.getPhone().trim().isEmpty() ? request.getPhone().trim() : null)
                        .phoneNo(request.getPhone() != null && !request.getPhone().trim().isEmpty() ? request.getPhone().trim() : "0000000000")
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
                        .team(team)
                        .sprNo(request.getSprNo() != null && !request.getSprNo().trim().isEmpty() ? request.getSprNo().trim() : null)
                        .address(request.getAddress())
                        .active(request.getActive() != null ? request.getActive() : true)
                        .build();
                    studentsToSave.add(student);
                    successCount++;
                }
            }
            if (!studentsToSave.isEmpty()) {
                studentRepository.saveAllAndFlush(studentsToSave);
            }
            return ApiResponse.ok("Bulk import processed: " + successCount + " students created, " + updateCount + " updated.", null);
        } catch (Exception e) {
            log.error("Bulk import failed", e);
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            return ApiResponse.error("Bulk import failed: " + root.getMessage());
        }
    }
}
