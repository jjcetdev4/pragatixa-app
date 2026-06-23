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

/**
 * Business logic for Student management.
 */
@Service
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository,
                          DepartmentRepository departmentRepository,
                          PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Create ───────────────────────────────────────

    @Transactional
    public ApiResponse<StudentResponse> createStudent(CreateStudentRequest request) {
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

        Student student = Student.builder()
            .studentId(request.getStudentId())
            .fullName(request.getFullName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .phone(request.getPhone())
            .gender(request.getGender())
            .dateOfBirth(request.getDateOfBirth())
            .address(request.getAddress())
            .department(department)
            .semester(request.getSemester())
            .academicYear(request.getAcademicYear())
            .active(true)
            .build();

        Student saved = studentRepository.save(student);
        log.info("Created student: {} ({})", saved.getFullName(), saved.getStudentId());
        return ApiResponse.ok("Student created successfully", toResponse(saved));
    }

    // ── Read All ─────────────────────────────────────

    @Transactional(readOnly = true)
    public ApiResponse<Page<StudentResponse>> getAllStudents(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        Page<StudentResponse> result = studentRepository.findAll(pageable).map(this::toResponse);
        return ApiResponse.ok(result);
    }

    // ── Read By ID ───────────────────────────────────

    @Transactional(readOnly = true)
    public ApiResponse<StudentResponse> getStudentById(Long id) {
        return studentRepository.findById(id)
            .map(s -> ApiResponse.ok("Student found", toResponse(s)))
            .orElseGet(() -> ApiResponse.error("Student not found with ID: " + id));
    }

    // ── Search ───────────────────────────────────────

    @Transactional(readOnly = true)
    public ApiResponse<Page<StudentResponse>> searchStudents(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());
        Page<StudentResponse> result = studentRepository.searchStudents(keyword, pageable).map(this::toResponse);
        return ApiResponse.ok(result);
    }

    // ── Mapper ───────────────────────────────────────

    private StudentResponse toResponse(Student student) {
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
            .active(student.isActive())
            .createdAt(student.getCreatedAt())
            .build();
    }
}
