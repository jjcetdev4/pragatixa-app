package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.admin.dto.request.CreateDepartmentRequest;
import com.pragatix.entity.Department;
import com.pragatix.entity.Section;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AdminDepartmentService {

    private final AdminDepartmentCommandService adminDepartmentCommandService;
    private final AdminSectionCommandService adminSectionCommandService;
    private final AdminClassCoordinatorQueryService adminClassCoordinatorQueryService;

    public AdminDepartmentService(AdminDepartmentCommandService adminDepartmentCommandService,
            AdminSectionCommandService adminSectionCommandService,
            AdminClassCoordinatorQueryService adminClassCoordinatorQueryService) {
        this.adminDepartmentCommandService = adminDepartmentCommandService;
        this.adminSectionCommandService = adminSectionCommandService;
        this.adminClassCoordinatorQueryService = adminClassCoordinatorQueryService;
    }

    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllDepartments() {
        return adminDepartmentCommandService.getAllDepartments();
    }

    public ResponseEntity<ApiResponse<Department>> createDepartment(CreateDepartmentRequest request) {
        return adminDepartmentCommandService.createDepartment(request);
    }

    public ResponseEntity<ApiResponse<Department>> updateDepartment(Long id, CreateDepartmentRequest request) {
        return adminDepartmentCommandService.updateDepartment(id, request);
    }

    public ResponseEntity<ApiResponse<Void>> deleteDepartment(Long id) {
        return adminDepartmentCommandService.deleteDepartment(id);
    }

    public ResponseEntity<ApiResponse<List<Section>>> getSectionsOfDept(Long id) {
        return adminSectionCommandService.getSectionsOfDept(id);
    }

    public ResponseEntity<ApiResponse<Section>> createSection(Long id, Map<String, Object> body) {
        return adminSectionCommandService.createSection(id, body);
    }

    public ResponseEntity<ApiResponse<Void>> deleteSection(Long id, Long sectionId) {
        return adminSectionCommandService.deleteSection(id, sectionId);
    }

    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getClassCoordinators() {
        return adminClassCoordinatorQueryService.getClassCoordinators();
    }
}
