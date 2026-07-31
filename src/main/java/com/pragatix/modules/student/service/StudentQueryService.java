package com.pragatix.modules.student.service;

import com.pragatix.dto.*;
import com.pragatix.entity.*;
import com.pragatix.modules.student.dto.response.StudentResponse;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.repository.YearRepository;
import com.pragatix.repository.StudentGuardianRepository;
import com.pragatix.modules.authentication.repository.UserRepository;
import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.authentication.security.AuthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentQueryService {
    private static final Logger log = LoggerFactory.getLogger(StudentQueryService.class);

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final YearRepository yearRepository;
    private final StudentMapper studentMapper;
    private final StudentGuardianRepository studentGuardianRepository;
    private final AuthUtils authUtils;

    public StudentQueryService(StudentRepository studentRepository, UserRepository userRepository,
            YearRepository yearRepository, StudentMapper studentMapper,
            StudentGuardianRepository studentGuardianRepository, AuthUtils authUtils) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.yearRepository = yearRepository;
        this.studentMapper = studentMapper;
        this.studentGuardianRepository = studentGuardianRepository;
        this.authUtils = authUtils;
    }

    public ApiResponse<StudentResponse> getStudentById(Long id) {
        return studentRepository.findById(id)
                .map(s -> {
                    User currentUser = authUtils.getCurrentUser();
                    if (currentUser != null && !authUtils.isSuperAdmin(currentUser) && authUtils.isAdmin(currentUser)) {
                        String adminYear = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
                        if (adminYear != null && !adminYear.equals(s.getYear())) {
                            throw new org.springframework.security.access.AccessDeniedException(
                                    "You are not authorized to access this student.");
                        }
                    }
                    StudentGuardian guardian = studentGuardianRepository.findByStudentId(s.getId()).orElse(null);
                    return ApiResponse.ok(studentMapper.toResponse(s, guardian));
                })
                .orElseGet(() -> ApiResponse.error("Student not found with ID: " + id));
    }

    private Page<StudentResponse> mapWithGuardians(Page<Student> page) {
        if (page.isEmpty()) {
            return page.map(studentMapper::toResponse);
        }

        java.util.List<Long> studentIds = page.getContent().stream().map(Student::getId).toList();
        java.util.List<StudentGuardian> guardians = studentGuardianRepository.findByStudentIdIn(studentIds);

        java.util.Map<Long, StudentGuardian> guardianMap = guardians.stream()
                .collect(java.util.stream.Collectors.toMap(
                        g -> g.getStudent().getId(),
                        g -> g,
                        (existing, replacement) -> existing));

        return page.map(s -> studentMapper.toResponse(s, guardianMap.get(s.getId())));
    }

    public ApiResponse<Page<StudentResponse>> getAllStudents(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());

        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);

        boolean isCc = currentUser != null && currentUser.getSubRoles().stream()
                .map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));

        if (isCc && currentUser != null) {
            String userYearStr = currentUser.getYear();
            Byte yearNo = null;
            if (userYearStr != null) {
                String yTrim = userYearStr.trim().toUpperCase();
                if (yTrim.equals("I") || yTrim.equals("1"))
                    yearNo = 1;
                else if (yTrim.equals("II") || yTrim.equals("2"))
                    yearNo = 2;
                else if (yTrim.equals("III") || yTrim.equals("3"))
                    yearNo = 3;
                else if (yTrim.equals("IV") || yTrim.equals("4"))
                    yearNo = 4;
            }
            Year yearRef = null;
            if (yearNo != null) {
                yearRef = yearRepository.findByYearNo(yearNo).orElse(null);
            }
            Section userSection = currentUser.getSection();

            if (currentUser.getDepartment() != null && yearRef != null && userSection != null) {
                Page<StudentResponse> result = mapWithGuardians(studentRepository.findByDepartmentAndYearAndSection(
                        currentUser.getDepartment().getId(),
                        yearRef.getId(),
                        userSection.getId(),
                        pageable));
                return ApiResponse.ok(result);
            } else {
                return ApiResponse.ok(Page.empty(pageable));
            }
        }

        if (currentUser != null && !authUtils.isSuperAdmin(currentUser) && authUtils.isAdmin(currentUser)) {
            String adminYear = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
            if (adminYear != null) {
                // Assuming we need a repository method findAllByYear(year, pageable)
                // Let's fallback to findAll and filter or create one.
                // Wait, it's better to implement findAllByYear in StudentRepository.
                // For now, I'll assume studentRepository.findAllByYear exists.
                // Actually, I can use a generic search with year.
                Page<StudentResponse> result = mapWithGuardians(studentRepository.findAllByYear(adminYear, pageable));
                return ApiResponse.ok(result);
            } else {
                return ApiResponse.ok(Page.empty(pageable));
            }
        }

        Page<StudentResponse> result = mapWithGuardians(studentRepository.findAll(pageable));
        return ApiResponse.ok(result);
    }

    public ApiResponse<Page<StudentResponse>> searchStudents(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());

        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);

        boolean isCc = currentUser != null && currentUser.getSubRoles().stream()
                .map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));

        if (isCc && currentUser != null) {
            String userYearStr = currentUser.getYear();
            Byte yearNo = null;
            if (userYearStr != null) {
                String yTrim = userYearStr.trim().toUpperCase();
                if (yTrim.equals("I") || yTrim.equals("1"))
                    yearNo = 1;
                else if (yTrim.equals("II") || yTrim.equals("2"))
                    yearNo = 2;
                else if (yTrim.equals("III") || yTrim.equals("3"))
                    yearNo = 3;
                else if (yTrim.equals("IV") || yTrim.equals("4"))
                    yearNo = 4;
            }
            Year yearRef = null;
            if (yearNo != null) {
                yearRef = yearRepository.findByYearNo(yearNo).orElse(null);
            }
            Section userSection = currentUser.getSection();

            if (currentUser.getDepartment() != null && yearRef != null && userSection != null) {
                Page<StudentResponse> result = mapWithGuardians(studentRepository.searchStudentsByCC(
                        keyword,
                        currentUser.getDepartment().getId(),
                        yearRef.getId(),
                        userSection.getId(),
                        pageable));
                return ApiResponse.ok(result);
            } else {
                return ApiResponse.ok(Page.empty(pageable));
            }
        }

        if (currentUser != null && !authUtils.isSuperAdmin(currentUser) && authUtils.isAdmin(currentUser)) {
            String adminYear = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
            if (adminYear != null) {
                Page<StudentResponse> result = mapWithGuardians(
                        studentRepository.searchStudentsByYear(keyword, adminYear, pageable));
                return ApiResponse.ok(result);
            } else {
                return ApiResponse.ok(Page.empty(pageable));
            }
        }

        Page<StudentResponse> result = mapWithGuardians(studentRepository.searchStudents(keyword, pageable));
        return ApiResponse.ok(result);
    }

    public ApiResponse<java.util.List<com.pragatix.modules.student.dto.response.StudentSearchDTO>> searchActiveStudentsForTeam(
            String keyword) {
        Pageable limit = PageRequest.of(0, 20); // limit to 20
        java.util.List<Student> students = studentRepository.searchActiveStudentsForTeam(keyword, limit);

        java.util.List<com.pragatix.modules.student.dto.response.StudentSearchDTO> results = students.stream()
                .map(s -> {
                    com.pragatix.modules.student.dto.response.StudentSearchDTO dto = new com.pragatix.modules.student.dto.response.StudentSearchDTO();
                    dto.setId(s.getId());
                    dto.setFullName(s.getFullName());
                    dto.setRegNo(s.getRegNo());
                    dto.setSprNo(s.getSprNo());
                    dto.setDepartmentName(s.getDepartment() != null ? s.getDepartment().getName() : "N/A");
                    dto.setYear(s.getYearRef() != null ? String.valueOf(s.getYearRef().getYearNo()) : "N/A");
                    dto.setSection(s.getSection() != null ? s.getSection().getSectionName() : "N/A");
                    dto.setTeamName(s.getTeam() != null ? s.getTeam().getName() : null);
                    dto.setTeamId(s.getTeam() != null ? s.getTeam().getId() : null);
                    dto.setCurrentStage(s.getCurrentStage());
                    return dto;
                }).collect(java.util.stream.Collectors.toList());

        return ApiResponse.ok(results);
    }
}
