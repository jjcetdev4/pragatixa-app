package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.dto.*;
import jjcet.PragatiX.modules.activity.dto.request.*;
import jjcet.PragatiX.modules.activity.dto.response.*;
import jjcet.PragatiX.modules.student.dto.request.*;
import jjcet.PragatiX.modules.student.dto.response.*;
import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.repository.*;
import jjcet.PragatiX.modules.activity.repository.*;
import jjcet.PragatiX.modules.faculty.repository.*;
import jjcet.PragatiX.modules.student.repository.*;
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

    public StudentService(StudentCrudService studentCrudService, StudentImportService studentImportService,
            StudentDisciplineService studentDisciplineService, StudentTeamService studentTeamService) {
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
    public ApiResponse<Page<StudentResponse>> getAllStudents(int page, int size, String sortBy, String keyword,
            String year, Long departmentId, Long sectionId) {
        return studentCrudService.getAllStudents(page, size, sortBy, keyword, year, departmentId, sectionId);
    }

    @Transactional(readOnly = true)
    public java.util.List<jjcet.PragatiX.entity.Department> getFilterDepartmentsByYear(String year) {
        return studentCrudService.getFilterDepartmentsByYear(year);
    }

    @Transactional(readOnly = true)
    public java.util.List<jjcet.PragatiX.entity.Section> getFilterSections(String year, Long departmentId) {
        return studentCrudService.getFilterSections(year, departmentId);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Page<StudentResponse>> searchStudents(String keyword, int page, int size, boolean unassignedOnly) {
        return studentCrudService.searchStudents(keyword, page, size, unassignedOnly);
    }

    @Transactional
    public ApiResponse<List<CreateStudentRequest>> bulkParse(MultipartFile file, String username) {
        return studentImportService.bulkParse(file, username);
    }

    public ApiResponse<String> bulkImport(List<CreateStudentRequest> requests, String username) {
        return studentImportService.bulkImport(requests, username);
    }

    public byte[] generateExcelTemplate() throws java.io.IOException {
        return studentImportService.generateExcelTemplate();
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
    public ApiResponse<List<jjcet.PragatiX.modules.student.dto.response.StudentSearchDTO>> searchActiveStudentsForTeam(
            String keyword, Long teamId, Integer currentStage) {
        return studentCrudService.searchActiveStudentsForTeam(keyword, teamId, currentStage);
    }
}
