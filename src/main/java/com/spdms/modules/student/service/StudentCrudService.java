package com.spdms.modules.student.service;

import com.spdms.dto.*;
import com.spdms.modules.student.dto.request.*;
import com.spdms.modules.student.dto.response.StudentResponse;
import com.spdms.common.response.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class StudentCrudService {
    
    private final StudentCommandService studentCommandService;
    private final StudentQueryService studentQueryService;

    public StudentCrudService(StudentCommandService studentCommandService, StudentQueryService studentQueryService) {
        this.studentCommandService = studentCommandService;
        this.studentQueryService = studentQueryService;
    }

    public ApiResponse<StudentResponse> createStudent(CreateStudentRequest request, String username) {
        return studentCommandService.createStudent(request, username);
    }

    public ApiResponse<StudentResponse> updateStudent(Long id, UpdateStudentRequest request) {
        return studentCommandService.updateStudent(id, request);
    }

    public ApiResponse<Void> deleteStudent(Long id) {
        return studentCommandService.deleteStudent(id);
    }

    public ApiResponse<StudentResponse> getStudentById(Long id) {
        return studentQueryService.getStudentById(id);
    }

    public ApiResponse<Page<StudentResponse>> getAllStudents(int page, int size, String sortBy) {
        return studentQueryService.getAllStudents(page, size, sortBy);
    }

    public ApiResponse<Page<StudentResponse>> searchStudents(String keyword, int page, int size) {
        return studentQueryService.searchStudents(keyword, page, size);
    }

    public ApiResponse<java.util.List<com.spdms.modules.student.dto.response.StudentSearchDTO>> searchActiveStudentsForTeam(String keyword) {
        return studentQueryService.searchActiveStudentsForTeam(keyword);
    }
}
