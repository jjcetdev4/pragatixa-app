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
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;

import java.util.List;


@Service
public class StudentService {
    private final StudentCrudService studentCrudService;
    private final StudentImportService studentImportService;
    private final StudentDisciplineService studentDisciplineService;
    private final StudentTeamService studentTeamService;

    public StudentService(StudentCrudService studentCrudService, StudentImportService studentImportService, StudentDisciplineService studentDisciplineService, StudentTeamService studentTeamService) {
        this.studentCrudService = studentCrudService;
        this.studentImportService = studentImportService;
        this.studentDisciplineService = studentDisciplineService;
        this.studentTeamService = studentTeamService;
    }

    @Transactional
        public ApiResponse<StudentResponse> createStudent(CreateStudentRequest request, String username) {
        return studentCrudService.createStudent(request, username);
    }

    @Transactional
        public ApiResponse<StudentResponse> updateStudent(Long id, UpdateStudentRequest request) {
        return studentCrudService.updateStudent(id, request);
    }

    @Transactional
        public ApiResponse<Void> deleteStudent(Long id) {
        return studentCrudService.deleteStudent(id);
    }

    @Transactional(readOnly = true)
        public ApiResponse<StudentResponse> getStudentById(Long id) {
        return studentCrudService.getStudentById(id);
    }

    @Transactional(readOnly = true)
        public ApiResponse<Page<StudentResponse>> getAllStudents(int page, int size, String sortBy) {
        return studentCrudService.getAllStudents(page, size, sortBy);
    }

    @Transactional(readOnly = true)
        public ApiResponse<Page<StudentResponse>> searchStudents(String keyword, int page, int size) {
        return studentCrudService.searchStudents(keyword, page, size);
    }

    @Transactional
        public ApiResponse<List<CreateStudentRequest>> bulkParse(MultipartFile file, String username) {
        return studentImportService.bulkParse(file, username);
    }

    public ApiResponse<String> bulkImport(List<CreateStudentRequest> requests, String username) {
        return studentImportService.bulkImport(requests, username);
    }

    @Transactional
        public ApiResponse<StudentResponse> adjustPoints(Long regNo, PointAdjustmentRequest request, String username) {
        return studentDisciplineService.adjustPoints(regNo, request, username);
    }

    @Transactional(readOnly = true)
        public ApiResponse<List<DisciplineLog>> getDisciplineLogs(Long regNo) {
        return studentDisciplineService.getDisciplineLogs(regNo);
    }

    @Transactional(readOnly = true)
        public ApiResponse<DepartmentPerformanceResponse> getDepartmentPerformance(String username) {
        return studentDisciplineService.getDepartmentPerformance(username);
    }

    @Transactional
        public ApiResponse<Void> promoteToTeamCaptain(Long regNo) {
        return studentTeamService.promoteToTeamCaptain(regNo);
    }

    @Transactional
        public ApiResponse<Void> removeTeamCaptain(Long regNo) {
        return studentTeamService.removeTeamCaptain(regNo);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<com.spdms.modules.student.dto.response.StudentSearchDTO>> searchActiveStudentsForTeam(String keyword) {
        return studentCrudService.searchActiveStudentsForTeam(keyword);
    }
}
