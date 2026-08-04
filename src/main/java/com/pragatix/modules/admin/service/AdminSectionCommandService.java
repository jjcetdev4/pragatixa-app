package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.Department;
import com.pragatix.entity.Section;
import com.pragatix.repository.DepartmentRepository;
import com.pragatix.repository.SectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AdminSectionCommandService {

    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;

    public AdminSectionCommandService(DepartmentRepository departmentRepository, SectionRepository sectionRepository) {
        this.departmentRepository = departmentRepository;
        this.sectionRepository = sectionRepository;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Section>>> getSectionsOfDept(Long id) {
        if (!departmentRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Department not found"));
        }
        List<Section> sections = sectionRepository.findByDepartment_IdOrderBySectionNameAsc(id);
        return ResponseEntity.ok(ApiResponse.ok("Sections retrieved successfully", sections));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Section>> createSection(Long id, Map<String, Object> body) {
        Department dept = departmentRepository.findById(id).orElse(null);
        if (dept == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Department not found"));
        }
        String sectionName = (String) body.get("sectionName");
        if (sectionName == null || sectionName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Section name is required"));
        }
        sectionName = sectionName.trim().toUpperCase();
        if (sectionRepository.findByDepartmentAndSectionName(dept, sectionName).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Section already exists in this department"));
        }
        Section sec = Section.builder()
                .department(dept)
                .sectionName(sectionName)
                .build();
        Section saved = sectionRepository.save(sec);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Section created successfully", saved));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteSection(Long id, Long sectionId) {
        if (!departmentRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Department not found"));
        }
        if (!sectionRepository.existsById(sectionId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Section not found"));
        }
        sectionRepository.deleteById(sectionId);
        return ResponseEntity.ok(ApiResponse.ok("Section deleted successfully", null));
    }
}
