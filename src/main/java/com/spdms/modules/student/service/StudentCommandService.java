package com.spdms.modules.student.service;

import com.spdms.dto.*;
import com.spdms.modules.student.dto.request.*;
import com.spdms.modules.student.dto.response.StudentResponse;
import com.spdms.common.response.ApiResponse;
import com.spdms.entity.*;
import com.spdms.repository.*;
import com.spdms.modules.student.repository.StudentRepository;
import com.spdms.modules.authentication.repository.UserRepository;
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

    public StudentCommandService(PasswordEncoder passwordEncoder, StudentRepository studentRepository, TeamRepository teamRepository, UserRepository userRepository, StudentLookupService studentLookupService, StudentMapper studentMapper) {
        this.passwordEncoder = passwordEncoder;
        this.studentRepository = studentRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.studentLookupService = studentLookupService;
        this.studentMapper = studentMapper;
    }

    @Transactional
    public ApiResponse<StudentResponse> createStudent(CreateStudentRequest request, String username) {
        User creator = userRepository.findByUsername(username).orElse(null);
        boolean isCcOrAdmin = creator != null && (creator.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"))
                || creator.getSubRoles().stream().map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC")));
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
            academicYear = studentLookupService.resolveAcademicYear(request.getAcademicYearId(), request.getAcademicYear());
            year = studentLookupService.resolveYear(request.getYearId(), request.getYear());
            semester = studentLookupService.resolveSemester(request.getSemesterId(), request.getSemester());
            gender = studentLookupService.resolveGender(request.getGenderId(), request.getGender());
            section = studentLookupService.resolveSection(request.getSectionId(), null, department);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
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
            .sprNo(request.getSprNo() != null ? request.getSprNo().trim() : null)
            .active(true)
            .score(100)
            .build();

        Student saved = studentRepository.save(student);
        return ApiResponse.ok("Student created successfully", studentMapper.toResponse(saved));
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

        Department department;
        AcademicYear academicYear;
        Year year;
        Semester semester;
        Gender gender;
        Section section;
        try {
            department = studentLookupService.resolveDepartment(request.getDepartmentId(), null);
            academicYear = studentLookupService.resolveAcademicYear(request.getAcademicYearId(), request.getAcademicYear());
            year = studentLookupService.resolveYear(request.getYearId(), request.getYear());
            semester = studentLookupService.resolveSemester(request.getSemesterId(), request.getSemester());
            gender = studentLookupService.resolveGender(request.getGenderId(), request.getGender());
            section = studentLookupService.resolveSection(request.getSectionId(), null, department);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
        Team team = request.getTeamId() != null ? teamRepository.findById(request.getTeamId()).orElse(null) : null;

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
        student.setSprNo(request.getSprNo() != null ? request.getSprNo().trim() : null);
        student.setActive(request.isActive());

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            student.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        Student saved = studentRepository.save(student);
        return ApiResponse.ok("Student updated successfully", studentMapper.toResponse(saved));
    }

    @Transactional
    public ApiResponse<Void> deleteStudent(Long id) {
        Student student = studentRepository.findById(id).orElse(null);
        if (student == null) {
            return ApiResponse.error("Student not found with ID: " + id);
        }

        entityManager.createNativeQuery("DELETE FROM xp_transactions WHERE reg_no = :sid").setParameter("sid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM discipline_logs WHERE reg_no = :sid").setParameter("sid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM student_activity_xp WHERE reg_no = :sid").setParameter("sid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM team_removal_requests WHERE reg_no = :sid OR captain_id = :sid").setParameter("sid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM team_members WHERE reg_no = :sid").setParameter("sid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM student_badges WHERE reg_no = :sid").setParameter("sid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM streaks WHERE reg_no = :sid").setParameter("sid", id).executeUpdate();
        entityManager.createNativeQuery("UPDATE teams SET captain_id = NULL WHERE captain_id = :sid").setParameter("sid", id).executeUpdate();

        User user = student.getUser();
        studentRepository.delete(student);

        if (user != null) {
            entityManager.createNativeQuery("DELETE FROM user_roles WHERE user_id = :uid").setParameter("uid", user.getId()).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM user_sub_roles WHERE user_id = :uid").setParameter("uid", user.getId()).executeUpdate();
            userRepository.delete(user);
        }

        return ApiResponse.ok("Student deleted successfully", null);
    }
}
