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

    @Transactional
    public ApiResponse<StudentResponse> createStudent(CreateStudentRequest request, String username) {
        User creator = userRepository.findByUsername(username).orElse(null);
        boolean isCcOrAdmin = creator != null
                && (creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"))
                        || creator.getSubRoles().stream().map(SubRole::getName)
                                .anyMatch(sr -> sr.trim().equalsIgnoreCase("CC")));
        if (!isCcOrAdmin) {
            return ApiResponse.error("Access Denied: Only Class Coordinators (CC) can add students.");
        }

        if (studentRepository.existsByRegNo(request.getRegNo())) {
            return ApiResponse.error("Student ID '" + request.getRegNo() + "' already exists");
        }
        if (studentRepository.existsByEmail(request.getEmail())) {
            return ApiResponse.error("Email '" + request.getEmail() + "' is already registered");
        }

        Department department;
        AcademicYear academicYear;
        Year year;
        Semester semester;
        Gender gender;
        Section section;
        try {
            department = studentLookupService.resolveDepartment(request.getDepartmentId(), request.getDepartmentName());
            academicYear = studentLookupService.resolveAcademicYear(request.getAcademicYearId(),
                    request.getAcademicYear());
            year = studentLookupService.resolveYear(request.getYearId(), request.getYear());
            semester = studentLookupService.resolveSemester(request.getSemesterId(), request.getSemester());
            gender = studentLookupService.resolveGender(request.getGenderId(), request.getGender());
            section = studentLookupService.resolveSection(request.getSectionId(), null, department);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }

        ActivityStage initialStage = activityStageRepository.findFirstByIsActiveTrueOrderByDisplayOrderAsc()
                .orElse(null);
        if (initialStage == null) {
            return ApiResponse.error(
                    "Validation Error: No active stages found. Please configure stages before creating students.");
        }

        Team team = request.getTeamId() != null ? teamRepository.findById(request.getTeamId()).orElse(null) : null;

        String rawPassword = request.getPassword();
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            if (request.getDateOfBirth() != null) {
                rawPassword = request.getDateOfBirth().format(DateTimeFormatter.ofPattern("ddMMyyyy"));
            } else {
                rawPassword = "123456";
            }
        }

        String sprNoStr = request.getSprNo() != null ? request.getSprNo().trim() : null;
        if (sprNoStr != null && sprNoStr.isEmpty()) {
            sprNoStr = null;
        }

        if (sprNoStr != null && studentRepository.findBySprNo(sprNoStr).isPresent()) {
            return ApiResponse.error("Student with SPR No '" + sprNoStr + "' already exists.");
        }
        if (studentRepository.existsByRegNo(request.getRegNo().trim())) {
            return ApiResponse.error("Student with Register No '" + request.getRegNo().trim() + "' already exists.");
        }
        if (studentRepository.existsByEmail(request.getEmail().trim())) {
            return ApiResponse.error("Student with Email '" + request.getEmail().trim() + "' already exists.");
        }

        Student student = Student.builder()
                .regNo(request.getRegNo().trim())
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
                .section(section)
                .team(team)
                .sprNo(sprNoStr)
                .active(true)
                .score(100)
                .stage(initialStage.getDisplayOrder())
                .currentStage(initialStage.getDisplayOrder())
                .currentStageId(initialStage.getId())
                .build();

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
            guardian = StudentGuardian.builder()
                    .student(saved)
                    .regNo(saved.getRegNo())
                    .guardianName(gDto.getGuardianName())
                    .relationship(rel)
                    .phoneNo(gDto.getPhoneNo())
                    .email(gDto.getEmail())
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

        studentRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new RuntimeException("Email already registered by another student");
            }
        });

        String sprNoStr = request.getSprNo() != null ? request.getSprNo().trim() : null;
        if (sprNoStr != null && sprNoStr.isEmpty())
            sprNoStr = null;
        if (sprNoStr != null) {
            java.util.Optional<Student> existingSpr = studentRepository.findBySprNo(sprNoStr);
            if (existingSpr.isPresent() && !existingSpr.get().getId().equals(id)) {
                return ApiResponse.error("Student with SPR No '" + sprNoStr + "' already exists.");
            }
        }

        Department department;
        AcademicYear academicYear;
        Year year;
        Semester semester;
        Gender gender;
        Section section;
        try {
            department = studentLookupService.resolveDepartment(request.getDepartmentId(), null);
            academicYear = studentLookupService.resolveAcademicYear(request.getAcademicYearId(),
                    request.getAcademicYear());
            year = studentLookupService.resolveYear(request.getYearId(), request.getYear());
            semester = studentLookupService.resolveSemester(request.getSemesterId(), request.getSemester());
            gender = studentLookupService.resolveGender(request.getGenderId(), request.getGender());
            section = studentLookupService.resolveSection(request.getSectionId(), null, department);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
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
        student.setSection(section);
        student.setTeam(team);
        student.setSprNo(sprNoStr);
        if (request.getActive() != null) {
            student.setActive(request.getActive());
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            student.setPassword(passwordEncoder.encode(request.getPassword()));
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

            guardian.setPhoneNo(gDto.getPhoneNo());
            guardian.setEmail(gDto.getEmail());
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
}
