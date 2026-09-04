package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.dto.*;
import jjcet.PragatiX.modules.student.dto.request.*;
import jjcet.PragatiX.modules.student.dto.response.StudentResponse;
import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.repository.*;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.repository.StudentGuardianRepository;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.activity.repository.ActivityStageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class StudentCommandService {
    private static final Logger log = LoggerFactory.getLogger(StudentCommandService.class);

    @PersistenceContext
    private EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;
    private final StudentRepository studentRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final StudentLookupService studentLookupService;
    private final StudentMapper studentMapper;
    private final StudentGuardianRepository studentGuardianRepository;
    private final jjcet.PragatiX.admin.service.TeamCleanupService teamCleanupService;
    private final ActivityStageRepository activityStageRepository;
    private final jjcet.PragatiX.modules.audit.service.AuditService auditService;

    public StudentCommandService(PasswordEncoder passwordEncoder, StudentRepository studentRepository,
            TeamRepository teamRepository, UserRepository userRepository, StudentLookupService studentLookupService,
            StudentMapper studentMapper, StudentGuardianRepository studentGuardianRepository,
            jjcet.PragatiX.admin.service.TeamCleanupService teamCleanupService,
            ActivityStageRepository activityStageRepository,
            jjcet.PragatiX.modules.audit.service.AuditService auditService) {
        this.passwordEncoder = passwordEncoder;
        this.studentRepository = studentRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.studentLookupService = studentLookupService;
        this.studentMapper = studentMapper;
        this.studentGuardianRepository = studentGuardianRepository;
        this.teamCleanupService = teamCleanupService;
        this.activityStageRepository = activityStageRepository;
        this.auditService = auditService;
    }

    private static final java.util.regex.Pattern PHONE_PATTERN = java.util.regex.Pattern.compile("^\\d{10}$");
    private static final java.util.regex.Pattern EMAIL_PATTERN = java.util.regex.Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final java.util.regex.Pattern REGNO_PATTERN = java.util.regex.Pattern.compile("^8113\\d+$");
    private static final java.util.regex.Pattern SPR_PATTERN = java.util.regex.Pattern.compile("^[A-Za-z0-9]+$");

    @Transactional
    public ApiResponse<StudentResponse> createStudent(CreateStudentRequest request, String username) {
        User creator = userRepository.findByUsername(username).orElse(null);
        boolean isSuperAdmin = creator != null && creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_SUPER_ADMIN") || r.getName().equalsIgnoreCase("ROLE_SUPERADMIN"));
        boolean isAdmin = creator != null && creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
        boolean isHod = creator != null && (creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_HOD"))
                || creator.getSubRoles().stream().map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("HOD") || sr.trim().equalsIgnoreCase("HEAD_OF_DEPARTMENT")));
        boolean isCc = creator != null && creator.getSubRoles().stream().map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC") || sr.trim().equalsIgnoreCase("CLASS_COORDINATOR") || sr.trim().equalsIgnoreCase("ROLE_CC"));

        if (!isSuperAdmin && !isAdmin && !isHod && !isCc) {
            return ApiResponse.error("Access Denied: You do not have permission to add students.");
        }

        if (request.getRegNo() == null || request.getRegNo().trim().isEmpty()) {
            return ApiResponse.error("Register Number is required.");
        }
        String cleanRegNo = request.getRegNo().trim();
        if (!cleanRegNo.matches("^\\d+$")) {
            return ApiResponse.error("Register Number must contain digits only.");
        }
        if (!REGNO_PATTERN.matcher(cleanRegNo).matches()) {
            return ApiResponse.error("Register Number must start with 8113.");
        }

        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            return ApiResponse.error("Full Name is required.");
        }
        String cleanFullName = request.getFullName().trim();
        if (!cleanFullName.matches("^[a-zA-Z\\s]+$")) {
            return ApiResponse.error("Full Name must contain letters and spaces only.");
        }

        if (studentRepository.existsByRegNo(cleanRegNo)) {
            return ApiResponse.error("Student ID '" + cleanRegNo + "' already exists");
        }
        String cleanEmail = request.getEmail() != null && !request.getEmail().trim().isEmpty() ? request.getEmail().trim() : null;
        if (cleanEmail == null) {
            return ApiResponse.error("Email is required");
        }
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            return ApiResponse.error("Enter a valid email address.");
        }
        if (studentRepository.existsByEmail(cleanEmail)) {
            return ApiResponse.error("Email '" + cleanEmail + "' is already registered");
        }

        String rawPhone = request.getPhone() != null ? request.getPhone().trim() : null;
        if (rawPhone != null && !rawPhone.isEmpty()) {
            if (!rawPhone.matches("^\\d+$")) {
                return ApiResponse.error("Phone number must contain digits only.");
            }
            if (!PHONE_PATTERN.matcher(rawPhone).matches()) {
                return ApiResponse.error("Phone number must contain digits only.");
            }
        }

        if (request.getGuardian() != null) {
            GuardianDTO gDto = request.getGuardian();
            String gPhone = gDto.getPhoneNo() != null ? gDto.getPhoneNo().trim() : null;
            if (gPhone == null || gPhone.isEmpty()) {
                return ApiResponse.error("Guardian phone is required");
            }
            if (!gPhone.matches("^\\d+$") || !PHONE_PATTERN.matcher(gPhone).matches()) {
                return ApiResponse.error("Phone number must contain digits only.");
            }
            String gEmail = gDto.getEmail() != null && !gDto.getEmail().trim().isEmpty() ? gDto.getEmail().trim() : null;
            if (gEmail != null && !EMAIL_PATTERN.matcher(gEmail).matches()) {
                return ApiResponse.error("Enter a valid email address.");
            }
        }
        LocalDate dob = request.getDateOfBirth();
        if (dob != null && dob.isAfter(LocalDate.now().minusYears(16))) {
            return ApiResponse.error("Student must be at least 16 years old.");
        }

        Department department;
        Year year;
        Semester semester;
        Gender gender;
        Section section;
        try {
            department = studentLookupService.resolveDepartment(request.getDepartmentId(), request.getDepartmentName());
            year = studentLookupService.resolveYear(request.getYearId(), request.getYear());
            semester = studentLookupService.resolveSemester(request.getSemesterId(), request.getSemester());
            gender = studentLookupService.resolveGender(request.getGenderId(), request.getGender());
            section = studentLookupService.resolveSection(request.getSectionId(), null, department);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }

        // Validate Semester vs Year rules:
        // Year 1 -> Semesters 1, 2
        // Year 2 -> Semesters 3, 4
        // Year 3 -> Semesters 5, 6
        // Year 4 -> Semesters 7, 8
        if (year != null && semester != null) {
            byte yNo = year.getYearNo();
            byte sNo = semester.getSemesterNo();
            boolean valid = false;
            if (yNo == 1 && (sNo == 1 || sNo == 2)) valid = true;
            else if (yNo == 2 && (sNo == 3 || sNo == 4)) valid = true;
            else if (yNo == 3 && (sNo == 5 || sNo == 6)) valid = true;
            else if (yNo == 4 && (sNo == 7 || sNo == 8)) valid = true;

            if (!valid) {
                return ApiResponse.error("Invalid Semester " + sNo + " for Year " + yNo + ". Year 1 has Sem 1-2, Year 2 has Sem 3-4, Year 3 has Sem 5-6, Year 4 has Sem 7-8.");
            }
        }

        // Scope validation according to user's role:
        if (isSuperAdmin) {
            // Super Admin can add any student
        } else if (isAdmin) {
            // Admin can only add students for their assigned Year
            String adminAssignedYear = creator.getAssignedYear() != null ? creator.getAssignedYear().getYearName() : creator.getYear();
            if (adminAssignedYear != null && !adminAssignedYear.trim().isEmpty()) {
                String normalizedAdminYear = adminAssignedYear.replaceAll("[^0-9]", "");
                String normalizedTargetYear = year.getYearNo() != null ? year.getYearNo().toString() : (year.getYearName() != null ? year.getYearName().replaceAll("[^0-9]", "") : "");
                if (!normalizedAdminYear.isEmpty() && !normalizedTargetYear.isEmpty() && !normalizedAdminYear.equals(normalizedTargetYear)) {
                    return ApiResponse.error("Access Denied: As an Admin, you can only add students for your assigned Year (" + adminAssignedYear + ").");
                }
            }
        } else if (isHod) {
            // HOD can only add students for their department (in any year)
            if (creator.getDepartment() != null && !creator.getDepartment().getId().equals(department.getId())) {
                return ApiResponse.error("Access Denied: As HOD, you can only add students for your department (" + creator.getDepartment().getName() + ").");
            }
        } else if (isCc) {
            // CC can only add students for their assigned Class
            if (creator.getDepartment() != null && !creator.getDepartment().getId().equals(department.getId())) {
                return ApiResponse.error("Access Denied: As CC, you can only add students for your assigned department (" + creator.getDepartment().getName() + ").");
            }
            if (creator.getYear() != null && !creator.getYear().trim().isEmpty()) {
                String normalizedCcYear = creator.getYear().replaceAll("[^0-9]", "");
                String normalizedTargetYear = year.getYearNo() != null ? year.getYearNo().toString() : (year.getYearName() != null ? year.getYearName().replaceAll("[^0-9]", "") : "");
                if (!normalizedCcYear.isEmpty() && !normalizedTargetYear.isEmpty() && !normalizedCcYear.equals(normalizedTargetYear)) {
                    return ApiResponse.error("Access Denied: As CC, you can only add students for your assigned Year (" + creator.getYear() + ").");
                }
            }
            if (creator.getSection() != null && section != null && !creator.getSection().getId().equals(section.getId())) {
                return ApiResponse.error("Access Denied: As CC, you can only add students for your assigned section (" + creator.getSection().getSectionName() + ").");
            }
        }

        Team team = request.getTeamId() != null ? teamRepository.findById(request.getTeamId()).orElse(null) : null;

        String sprNoStr = request.getSprNo() != null ? request.getSprNo().trim() : null;
        if (sprNoStr != null && !sprNoStr.isEmpty()) {
            if (!SPR_PATTERN.matcher(sprNoStr).matches()) {
                return ApiResponse.error("SPR Number must contain alphanumeric characters only (no symbols).");
            }
            if (studentRepository.findBySprNo(sprNoStr).isPresent()) {
                return ApiResponse.error("Student with SPR No '" + sprNoStr + "' already exists.");
            }
        } else {
            sprNoStr = null;
        }

        if (studentRepository.existsByRegNo(cleanRegNo)) {
            return ApiResponse.error("Student with Register No '" + cleanRegNo + "' already exists.");
        }

        Student student = Student.builder()
                .regNo(cleanRegNo)
                .fullName(request.getFullName() != null ? request.getFullName().trim().toUpperCase() : null)
                .email(cleanEmail)
                .phone(rawPhone)
                .phoneNo(rawPhone)
                .dateOfBirth(request.getDateOfBirth())
                .address(request.getAddress())
                .department(department)
                .yearRef(year)
                .year(String.valueOf(year.getYearNo()))
                .semesterRef(semester)
                .semester(String.valueOf(semester.getSemesterNo()))
                .genderRef(gender)
                .gender(gender.getGenderName())
                .section(section)
                .team(team)
                .sprNo(sprNoStr)
                .active(true)
                .score(0)
                .stage(1)
                .currentStage(1)
                .build();

        // Resolve Stage 1 for this Academic Year if available
        jjcet.PragatiX.enums.AcademicYear studentAcademicYear = resolveStudentAcademicYear(year.getYearNo());
        ActivityStage initialStage = activityStageRepository
                .findByAcademicYearAndDisplayOrderAndDeletedFalse(studentAcademicYear, 1)
                .orElse(null);
        if (initialStage != null) {
            student.setStage(initialStage.getDisplayOrder() > 0 ? initialStage.getDisplayOrder() : 1);
            student.setCurrentStage(student.getStage());
            student.setCurrentStageId(initialStage.getId());
        }

        Student saved = studentRepository.save(student);

        if (team != null) {
            team.getMembers().add(saved);
            teamRepository.save(team);
            if (entityManager != null) {
                try {
                    entityManager.createNativeQuery(
                            "INSERT INTO team_members (team_id, student_id) VALUES (:tid, :sid) " +
                                    "ON DUPLICATE KEY UPDATE team_id = :tid")
                            .setParameter("tid", team.getId())
                            .setParameter("sid", saved.getId())
                            .executeUpdate();
                } catch (Exception ignored) {
                }
            }
        }

        StudentGuardian guardian = null;
        if (request.getGuardian() != null) {
            GuardianDTO gDto = request.getGuardian();
            StudentGuardian.RelationshipType rel = StudentGuardian.RelationshipType.GUARDIAN;
            if (gDto.getRelationship() != null && !gDto.getRelationship().trim().isEmpty()) {
                try {
                    rel = StudentGuardian.RelationshipType.valueOf(gDto.getRelationship().toUpperCase());
                } catch (IllegalArgumentException e) {
                    rel = StudentGuardian.RelationshipType.GUARDIAN;
                }
            }
            String gPhoneVal = (gDto.getPhoneNo() != null && !gDto.getPhoneNo().trim().isEmpty())
                    ? gDto.getPhoneNo().trim() : null;
            guardian = StudentGuardian.builder()
                    .student(saved)
                    .regNo(saved.getRegNo())
                    .guardianName(gDto.getGuardianName())
                    .relationship(rel)
                    .phoneNo(gPhoneVal)
                    .email(gDto.getEmail() != null && !gDto.getEmail().trim().isEmpty() ? gDto.getEmail().trim() : null)
                    .isPrimary(true)
                    .build();
            guardian = studentGuardianRepository.save(guardian);
        }

        auditService.log(
                jjcet.PragatiX.enums.AuditAction.CREATE,
                jjcet.PragatiX.enums.AuditModule.STUDENT,
                "STUDENT",
                saved.getId(),
                "Created student: " + saved.getRegNo(),
                null,
                saved
        );

        return ApiResponse.ok("Student created successfully", studentMapper.toResponse(saved, guardian));
    }

    @Transactional
    public ApiResponse<StudentResponse> updateStudent(Long id, UpdateStudentRequest request) {
        Student student = studentRepository.findById(id).orElse(null);
        if (student == null) {
            return ApiResponse.error("Student not found with ID: " + id);
        }

        String cleanEmail = request.getEmail() != null && !request.getEmail().trim().isEmpty() ? request.getEmail().trim() : null;
        if (cleanEmail != null) {
            if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
                return ApiResponse.error("Enter a valid email address.");
            }
            studentRepository.findByEmail(cleanEmail).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new RuntimeException("Email already registered by another student");
                }
            });
        }

        String rawPhone = request.getPhone() != null ? request.getPhone().trim() : null;
        if (rawPhone != null && !rawPhone.isEmpty()) {
            if (!rawPhone.matches("^\\d+$")) {
                return ApiResponse.error("Phone number must contain digits only.");
            }
            if (!PHONE_PATTERN.matcher(rawPhone).matches()) {
                return ApiResponse.error("Phone number must contain digits only.");
            }
        }

        if (request.getGuardian() != null) {
            GuardianDTO gDto = request.getGuardian();
            String gPhone = gDto.getPhoneNo() != null ? gDto.getPhoneNo().trim() : null;
            if (gPhone != null && !gPhone.isEmpty()) {
                if (!gPhone.matches("^\\d+$") || !PHONE_PATTERN.matcher(gPhone).matches()) {
                    return ApiResponse.error("Phone number must contain digits only.");
                }
            }
            String gEmail = gDto.getEmail() != null && !gDto.getEmail().trim().isEmpty() ? gDto.getEmail().trim() : null;
            if (gEmail != null && !EMAIL_PATTERN.matcher(gEmail).matches()) {
                return ApiResponse.error("Enter a valid email address.");
            }
        }

        LocalDate dobToCheck = request.getDateOfBirth();
        if (dobToCheck != null && dobToCheck.isAfter(LocalDate.now().minusYears(16))) {
            return ApiResponse.error("Student must be at least 16 years old.");
        }

        String sprNoStr = request.getSprNo() != null ? request.getSprNo().trim() : null;
        if (sprNoStr != null && !sprNoStr.isEmpty()) {
            if (!SPR_PATTERN.matcher(sprNoStr).matches()) {
                return ApiResponse.error("SPR Number must contain alphanumeric characters only (no symbols).");
            }
            java.util.Optional<Student> existingSpr = studentRepository.findBySprNo(sprNoStr);
            if (existingSpr.isPresent() && !existingSpr.get().getId().equals(id)) {
                return ApiResponse.error("Student with SPR No '" + sprNoStr + "' already exists.");
            }
        } else {
            sprNoStr = null;
        }

        String regNoStr = request.getRegNo() != null ? request.getRegNo().trim().toUpperCase() : null;
        if (regNoStr != null && !regNoStr.isEmpty()) {
            java.util.Optional<Student> existingReg = studentRepository.findByRegNo(regNoStr);
            if (existingReg.isPresent() && !existingReg.get().getId().equals(id)) {
                return ApiResponse.error("Student with Register Number '" + regNoStr + "' already exists.");
            }
        }

        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            return ApiResponse.error("Full Name is required.");
        }
        String cleanUpdateName = request.getFullName().trim();
        if (!cleanUpdateName.matches("^[a-zA-Z\\s]+$")) {
            return ApiResponse.error("Full Name must contain letters and spaces only.");
        }

        Department department;
        Year year;
        Semester semester;
        Gender gender;
        Section section;
        try {
            department = studentLookupService.resolveDepartment(request.getDepartmentId(), null);
            year = studentLookupService.resolveYear(request.getYearId(), request.getYear());
            semester = studentLookupService.resolveSemester(request.getSemesterId(), request.getSemester());
            gender = studentLookupService.resolveGender(request.getGenderId(), request.getGender());
            section = studentLookupService.resolveSection(request.getSectionId(), null, department);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }

        // Validate Semester vs Year rules:
        // Year 1 -> Semesters 1, 2
        // Year 2 -> Semesters 3, 4
        // Year 3 -> Semesters 5, 6
        // Year 4 -> Semesters 7, 8
        if (year != null && semester != null) {
            byte yNo = year.getYearNo();
            byte sNo = semester.getSemesterNo();
            boolean valid = false;
            if (yNo == 1 && (sNo == 1 || sNo == 2)) valid = true;
            else if (yNo == 2 && (sNo == 3 || sNo == 4)) valid = true;
            else if (yNo == 3 && (sNo == 5 || sNo == 6)) valid = true;
            else if (yNo == 4 && (sNo == 7 || sNo == 8)) valid = true;

            if (!valid) {
                return ApiResponse.error("Invalid Semester " + sNo + " for Year " + yNo + ". Year 1 has Sem 1-2, Year 2 has Sem 3-4, Year 3 has Sem 5-6, Year 4 has Sem 7-8.");
            }
        }
        Team team = request.getTeamId() != null ? teamRepository.findById(request.getTeamId()).orElse(null) : null;

        Team oldTeam = student.getTeam();
        if (team != null && (oldTeam == null || !oldTeam.getId().equals(team.getId()))) {
            if (oldTeam != null) {
                if (oldTeam.getCaptain() != null && oldTeam.getCaptain().getId().equals(student.getId())) {
                    oldTeam.setCaptain(null);
                }
                if (oldTeam.getViceCaptain() != null && oldTeam.getViceCaptain().getId().equals(student.getId())) {
                    oldTeam.setViceCaptain(null);
                }
                oldTeam.getMembers().remove(student);
                teamRepository.save(oldTeam);
                teamCleanupService.autoDeleteEmptyTeam(oldTeam);
            }
            if (!team.getMembers().contains(student)) {
                team.getMembers().add(student);
                teamRepository.save(team);
            }
            if (entityManager != null) {
                try {
                    entityManager.createNativeQuery(
                            "INSERT INTO team_members (team_id, student_id) VALUES (:tid, :sid) " +
                                    "ON DUPLICATE KEY UPDATE team_id = :tid")
                            .setParameter("tid", team.getId())
                            .setParameter("sid", student.getId())
                            .executeUpdate();
                } catch (Exception ignored) {
                }
            }
        } else if (team == null && oldTeam != null) {
            if (oldTeam.getCaptain() != null && oldTeam.getCaptain().getId().equals(student.getId())) {
                oldTeam.setCaptain(null);
            }
            if (oldTeam.getViceCaptain() != null && oldTeam.getViceCaptain().getId().equals(student.getId())) {
                oldTeam.setViceCaptain(null);
            }
            oldTeam.getMembers().remove(student);
            teamRepository.save(oldTeam);
            if (entityManager != null) {
                try {
                    entityManager.createNativeQuery("DELETE FROM team_members WHERE student_id = :sid")
                            .setParameter("sid", student.getId())
                            .executeUpdate();
                } catch (Exception ignored) {
                }
            }
            teamCleanupService.autoDeleteEmptyTeam(oldTeam);
        }

        if (regNoStr != null && !regNoStr.isEmpty()) {
            student.setRegNo(regNoStr);
            if (student.getUser() != null) {
                student.getUser().setUsername(regNoStr);
                userRepository.save(student.getUser());
            }
        }
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            student.setFullName(request.getFullName().trim().toUpperCase());
        }
        if (cleanEmail != null) {
            student.setEmail(cleanEmail);
            if (student.getUser() != null) {
                student.getUser().setEmail(cleanEmail);
                userRepository.save(student.getUser());
            }
        }
        if (rawPhone != null) {
            student.setPhoneNo(rawPhone);
        }
        student.setAddress(request.getAddress());
        if (request.getDob() != null) {
            student.setDateOfBirth(request.getDob());
        }

        student.setDepartment(department);
        student.setYearRef(year);
        student.setYear(String.valueOf(year.getYearNo()));
        student.setSemesterRef(semester);
        student.setSemester(String.valueOf(semester.getSemesterNo()));
        student.setGenderRef(gender);
        student.setGender(gender.getGenderName());
        student.setSection(section);
        student.setTeam(team);
        student.setSprNo(sprNoStr);
        if (request.getActive() != null) {
            student.setActive(request.getActive());
        }

        Student saved = studentRepository.save(student);

        StudentGuardian guardian = studentGuardianRepository.findByStudentId(saved.getId()).orElse(null);
        if (request.getGuardian() != null) {
            GuardianDTO gDto = request.getGuardian();
            if (guardian == null) {
                guardian = new StudentGuardian();
                guardian.setStudent(saved);
                guardian.setRegNo(saved.getRegNo());
                guardian.setPrimary(true);
            }
            guardian.setGuardianName(gDto.getGuardianName());

            StudentGuardian.RelationshipType rel = StudentGuardian.RelationshipType.GUARDIAN;
            if (gDto.getRelationship() != null && !gDto.getRelationship().trim().isEmpty()) {
                try {
                    rel = StudentGuardian.RelationshipType.valueOf(gDto.getRelationship().toUpperCase());
                } catch (IllegalArgumentException e) {
                    rel = StudentGuardian.RelationshipType.GUARDIAN;
                }
            }
            guardian.setRelationship(rel);

            String gPhoneVal = (gDto.getPhoneNo() != null && !gDto.getPhoneNo().trim().isEmpty())
                    ? gDto.getPhoneNo().trim() : null;
            guardian.setPhoneNo(gPhoneVal);
            guardian.setEmail(gDto.getEmail() != null && !gDto.getEmail().trim().isEmpty() ? gDto.getEmail().trim() : null);
            guardian = studentGuardianRepository.save(guardian);
        }

        java.util.Map<String, Object> newValues = new java.util.HashMap<>();
        newValues.put("regNo", saved.getRegNo());
        newValues.put("fullName", saved.getFullName());
        newValues.put("email", saved.getEmail());
        if (saved.getDepartment() != null) newValues.put("departmentId", saved.getDepartment().getId());
        if (saved.getTeam() != null) newValues.put("teamId", saved.getTeam().getId());

        auditService.log(
                jjcet.PragatiX.enums.AuditAction.UPDATE,
                jjcet.PragatiX.enums.AuditModule.STUDENT,
                "STUDENT",
                saved.getId(),
                "Updated student " + saved.getRegNo(),
                null,
                newValues
        );

        return ApiResponse.ok("Student updated successfully", studentMapper.toResponse(saved, guardian));
    }

    @Transactional
    public ApiResponse<Void> deleteStudent(Long id) {
        Student student = studentRepository.findById(id).orElse(null);
        if (student == null) {
            return ApiResponse.error("Student not found with ID: " + id);
        }

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null) {
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(
                    a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
            log.info("DeleteStudent Auth: username={}, isAdmin={}", auth.getName(), isAdmin);
            if (!isAdmin) {
                User user = userRepository.findByUsername(auth.getName()).orElse(null);
                if (user != null) {
                    boolean deptMatch = user.getDepartment() != null && student.getDepartment() != null
                            && user.getDepartment().getId().equals(student.getDepartment().getId());

                    String normalizedUserYear = user.getYear();
                    if (normalizedUserYear != null) {
                        switch (normalizedUserYear.toUpperCase().trim()) {
                            case "I":
                                normalizedUserYear = "1";
                                break;
                            case "II":
                                normalizedUserYear = "2";
                                break;
                            case "III":
                                normalizedUserYear = "3";
                                break;
                            case "IV":
                                normalizedUserYear = "4";
                                break;
                            case "V":
                                normalizedUserYear = "5";
                                break;
                        }
                    }
                    boolean yearMatch = normalizedUserYear != null && student.getYearRef() != null
                            && normalizedUserYear.equals(String.valueOf(student.getYearRef().getYearNo()));

                    boolean sectionMatch = user.getSection() != null && student.getSection() != null
                            && user.getSection().getId().equals(student.getSection().getId());

                    log.info("DeleteStudent Ownership Check: deptMatch={}, yearMatch={}, sectionMatch={}", deptMatch,
                            yearMatch, sectionMatch);
                    if (!deptMatch || !yearMatch || !sectionMatch) {
                        log.info("User Dept: {}, Student Dept: {}",
                                user.getDepartment() != null ? user.getDepartment().getId() : "null",
                                student.getDepartment() != null ? student.getDepartment().getId() : "null");
                        log.info("User Year (Normalized): {}, Student Year: {}", normalizedUserYear,
                                student.getYearRef() != null ? student.getYearRef().getYearNo() : "null");
                        log.info("User Section: {}, Student Section: {}",
                                user.getSection() != null ? user.getSection().getId() : "null",
                                student.getSection() != null ? student.getSection().getId() : "null");
                        return ApiResponse.error("You are not authorized to delete this student.");
                    }
                } else {
                    return ApiResponse.error("You are not authorized to delete this student.");
                }
            }
        }

        student.setDeleted(true);
        student.setActive(false);
        student.setDeletedAt(java.time.LocalDateTime.now());
        student.setPermanentDeleteAt(java.time.LocalDateTime.now().plusDays(30));
        if (auth != null && auth.getName() != null) {
            student.setDeletedBy(auth.getName());
        }
        
        studentRepository.save(student);

        auditService.log(
                jjcet.PragatiX.enums.AuditAction.DELETE,
                jjcet.PragatiX.enums.AuditModule.STUDENT,
                "STUDENT",
                student.getId(),
                "Soft deleted student: " + student.getRegNo()
        );

        return ApiResponse.ok("Student deleted successfully", null);
    }

    @Transactional
    public ApiResponse<Integer> batchUpdateStudents(BatchUpdateStudentsRequest request, String username) {
        if (request.getStudentIds() == null || request.getStudentIds().isEmpty()) {
            return ApiResponse.error("No students selected for batch update.");
        }

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = studentLookupService.resolveDepartment(request.getDepartmentId(), null);
        }

        Year year = null;
        if (request.getYearId() != null || (request.getYear() != null && !request.getYear().trim().isEmpty())) {
            year = studentLookupService.resolveYear(request.getYearId(), request.getYear());
        }

        Semester semester = null;
        if (request.getSemesterId() != null || (request.getSemester() != null && !request.getSemester().trim().isEmpty())) {
            semester = studentLookupService.resolveSemester(request.getSemesterId(), request.getSemester());
        }

        // Validate Semester vs Year rules:
        // Year 1 -> Semesters 1, 2
        // Year 2 -> Semesters 3, 4
        // Year 3 -> Semesters 5, 6
        // Year 4 -> Semesters 7, 8
        if (year != null && semester != null) {
            byte yNo = year.getYearNo();
            byte sNo = semester.getSemesterNo();
            boolean valid = false;
            if (yNo == 1 && (sNo == 1 || sNo == 2)) valid = true;
            else if (yNo == 2 && (sNo == 3 || sNo == 4)) valid = true;
            else if (yNo == 3 && (sNo == 5 || sNo == 6)) valid = true;
            else if (yNo == 4 && (sNo == 7 || sNo == 8)) valid = true;

            if (!valid) {
                return ApiResponse.error("Invalid Semester " + sNo + " for Year " + yNo + ". Year 1 has Sem 1-2, Year 2 has Sem 3-4, Year 3 has Sem 5-6, Year 4 has Sem 7-8.");
            }
        }

        Section section = null;
        if (request.getSectionId() != null) {
            section = studentLookupService.resolveSection(request.getSectionId(), null, department);
        }

        java.util.List<Student> students = studentRepository.findAllById(request.getStudentIds());
        int updatedCount = 0;

        for (Student student : students) {
            if (student.isDeleted()) continue;

            if (department != null) {
                student.setDepartment(department);
            }
            if (year != null) {
                student.setYearRef(year);
                student.setYear(String.valueOf(year.getYearNo()));
            }
            if (semester != null) {
                student.setSemesterRef(semester);
                student.setSemester(String.valueOf(semester.getSemesterNo()));
            }
            if (section != null) {
                student.setSection(section);
            }

            studentRepository.save(student);
            updatedCount++;
        }

        if (username != null) {
            auditService.log(
                    jjcet.PragatiX.enums.AuditAction.UPDATE,
                    jjcet.PragatiX.enums.AuditModule.STUDENT,
                    "STUDENT_BATCH",
                    0L,
                    "Batch updated " + updatedCount + " students by " + username
            );
        }

        return ApiResponse.ok("Successfully updated " + updatedCount + " students.", updatedCount);
    }

    private jjcet.PragatiX.enums.AcademicYear resolveStudentAcademicYear(Number yearNo) {
        if (yearNo == null) return jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR;
        int val = yearNo.intValue();
        if (val == 1) return jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR;
        if (val == 2) return jjcet.PragatiX.enums.AcademicYear.SECOND_YEAR;
        if (val == 3) return jjcet.PragatiX.enums.AcademicYear.THIRD_YEAR;
        if (val == 4) return jjcet.PragatiX.enums.AcademicYear.FOURTH_YEAR;
        return jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR;
    }
}
