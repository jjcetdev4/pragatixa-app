package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.dto.*;
import jjcet.PragatiX.modules.student.dto.request.*;
import jjcet.PragatiX.modules.student.dto.response.StudentResponse;
import jjcet.PragatiX.common.response.ApiResponse;
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

    public ApiResponse<Page<StudentResponse>> getAllStudents(int page, int size, String sortBy, String keyword,
            String year, Long departmentId, Long sectionId) {
        return studentQueryService.getAllStudents(page, size, sortBy, keyword, year, departmentId, sectionId);
    }

    public java.util.List<jjcet.PragatiX.entity.Department> getFilterDepartmentsByYear(String year) {
        return studentQueryService.getFilterDepartmentsByYear(year);
    }

    public java.util.List<jjcet.PragatiX.entity.Section> getFilterSections(String year, Long departmentId) {
        return studentQueryService.getFilterSections(year, departmentId);
    }

    public ApiResponse<Page<StudentResponse>> searchStudents(String keyword, int page, int size, boolean unassignedOnly) {
        return studentQueryService.searchStudents(keyword, page, size, unassignedOnly);
    }

    public ApiResponse<java.util.List<jjcet.PragatiX.modules.student.dto.response.StudentSearchDTO>> searchActiveStudentsForTeam(
            String keyword, Long teamId, Integer currentStage) {
        return studentQueryService.searchActiveStudentsForTeam(keyword, teamId, currentStage);
    }
}
