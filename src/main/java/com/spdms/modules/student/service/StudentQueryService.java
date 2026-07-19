package com.spdms.modules.student.service;

import com.spdms.dto.*;
import com.spdms.entity.*;
import com.spdms.modules.student.dto.response.StudentResponse;
import com.spdms.modules.student.repository.StudentRepository;
import com.spdms.repository.YearRepository;
import com.spdms.modules.authentication.repository.UserRepository;
import com.spdms.common.response.ApiResponse;
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

    public StudentQueryService(StudentRepository studentRepository, UserRepository userRepository, YearRepository yearRepository, StudentMapper studentMapper) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.yearRepository = yearRepository;
        this.studentMapper = studentMapper;
    }

    public ApiResponse<StudentResponse> getStudentById(Long id) {
        return studentRepository.findById(id)
            .map(s -> ApiResponse.ok(studentMapper.toResponse(s)))
            .orElseGet(() -> ApiResponse.error("Student not found with ID: " + id));
    }

    public ApiResponse<Page<StudentResponse>> getAllStudents(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        
        boolean isCc = currentUser != null && currentUser.getSubRoles().stream()
                .map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));
                
        if (isCc && currentUser != null) {
            String userYearStr = currentUser.getYear();
            Byte yearNo = null;
            if (userYearStr != null) {
                String yTrim = userYearStr.trim().toUpperCase();
                if (yTrim.equals("I") || yTrim.equals("1")) yearNo = 1;
                else if (yTrim.equals("II") || yTrim.equals("2")) yearNo = 2;
                else if (yTrim.equals("III") || yTrim.equals("3")) yearNo = 3;
                else if (yTrim.equals("IV") || yTrim.equals("4")) yearNo = 4;
            }
            Year yearRef = null;
            if (yearNo != null) {
                yearRef = yearRepository.findByYearNo(yearNo).orElse(null);
            }
            Section userSection = currentUser.getSection();
            
            if (currentUser.getDepartment() != null && yearRef != null && userSection != null) {
                Page<StudentResponse> result = studentRepository.findByDepartmentAndYearAndSection(
                    currentUser.getDepartment().getId(),
                    yearRef.getId(),
                    userSection.getId(),
                    pageable
                ).map(studentMapper::toResponse);
                return ApiResponse.ok(result);
            } else {
                return ApiResponse.ok(Page.empty(pageable));
            }
        }
        
        Page<StudentResponse> result = studentRepository.findAll(pageable).map(studentMapper::toResponse);
        return ApiResponse.ok(result);
    }

    public ApiResponse<Page<StudentResponse>> searchStudents(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());
        
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        
        boolean isCc = currentUser != null && currentUser.getSubRoles().stream()
                .map(SubRole::getName).anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));
                
        if (isCc && currentUser != null) {
            String userYearStr = currentUser.getYear();
            Byte yearNo = null;
            if (userYearStr != null) {
                String yTrim = userYearStr.trim().toUpperCase();
                if (yTrim.equals("I") || yTrim.equals("1")) yearNo = 1;
                else if (yTrim.equals("II") || yTrim.equals("2")) yearNo = 2;
                else if (yTrim.equals("III") || yTrim.equals("3")) yearNo = 3;
                else if (yTrim.equals("IV") || yTrim.equals("4")) yearNo = 4;
            }
            Year yearRef = null;
            if (yearNo != null) {
                yearRef = yearRepository.findByYearNo(yearNo).orElse(null);
            }
            Section userSection = currentUser.getSection();
            
            if (currentUser.getDepartment() != null && yearRef != null && userSection != null) {
                Page<StudentResponse> result = studentRepository.searchStudentsByCC(
                    keyword,
                    currentUser.getDepartment().getId(),
                    yearRef.getId(),
                    userSection.getId(),
                    pageable
                ).map(studentMapper::toResponse);
                return ApiResponse.ok(result);
            } else {
                return ApiResponse.ok(Page.empty(pageable));
            }
        }
        
        Page<StudentResponse> result = studentRepository.searchStudents(keyword, pageable).map(studentMapper::toResponse);
        return ApiResponse.ok(result);
    }
}
