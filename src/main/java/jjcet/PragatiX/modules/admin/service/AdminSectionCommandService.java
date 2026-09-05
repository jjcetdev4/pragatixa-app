package jjcet.PragatiX.modules.admin.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.Department;
import jjcet.PragatiX.entity.Section;
import jjcet.PragatiX.repository.DepartmentRepository;
import jjcet.PragatiX.repository.SectionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminSectionCommandService {

    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired(required = false)
    private jjcet.PragatiX.modules.audit.service.AuditService auditService;

    public AdminSectionCommandService(DepartmentRepository departmentRepository, SectionRepository sectionRepository) {
        this.departmentRepository = departmentRepository;
        this.sectionRepository = sectionRepository;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Section>>> getSectionsOfDept(Long id) {
        if (!departmentRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Department not found"));
        }
        List<Section> sections = sectionRepository.findByDepartment_IdOrderBySectionNameAsc(id)
                .stream()
                .filter(s -> !s.isDeleted())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Sections retrieved successfully", sections));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Section>> createSection(Long id, Map<String, Object> body) {
        Department dept = departmentRepository.findById(id).orElse(null);
        if (dept == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Department not found"));
        }
        if (Boolean.FALSE.equals(dept.getSupportsSections())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("This department does not support sections."));
        }
        String sectionName = (String) body.get("name");
        if (sectionName == null || sectionName.trim().isEmpty()) {
            sectionName = (String) body.get("sectionName");
        }
        if (sectionName == null || sectionName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Section name is required"));
        }
        sectionName = sectionName.trim().toUpperCase();

        // 1. Enforce single letter only (A-Z)
        if (!sectionName.matches("^[A-Z]$")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Section must be a single letter (e.g. A, B, C)"));
        }

        // 2. Fetch existing active sections of this department
        List<Section> existingSections = sectionRepository.findByDepartment_IdOrderBySectionNameAsc(id)
                .stream()
                .filter(s -> !s.isDeleted())
                .collect(Collectors.toList());
        Set<String> existingNames = existingSections.stream()
                .map(Section::getSectionName)
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        // 3. Prevent duplicate active section letters
        if (existingNames.contains(sectionName)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Section '" + sectionName + "' already exists in this department"));
        }

        // 4. Must start from A, then B, C... in sequential alphabetical order
        Character nextExpected = null;
        for (char c = 'A'; c <= 'Z'; c++) {
            if (!existingNames.contains(String.valueOf(c))) {
                nextExpected = c;
                break;
            }
        }

        if (nextExpected == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Maximum section limit reached (A-Z). Cannot add more sections."));
        }

        if (!sectionName.equals(String.valueOf(nextExpected))) {
            if (existingNames.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("The first section for this department must start from 'A'"));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("Sections must be added sequentially. Next section must be '" + nextExpected + "'"));
            }
        }

        // If a soft-deleted section with this name already exists in this department, restore it
        Optional<Section> softDeletedOpt = sectionRepository.findDeletedByDeptIdAndSectionName(id, sectionName);
        if (softDeletedOpt.isPresent()) {
            Section sec = softDeletedOpt.get();
            sec.setDeleted(false);
            sec.setDeletedAt(null);
            sec.setPermanentDeleteAt(null);
            sec.setDeletedBy(null);
            Section saved = sectionRepository.save(sec);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Section created successfully", saved));
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
        return deleteSection(sectionId);
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteSection(Long sectionId) {
        Section section = sectionRepository.findById(sectionId).orElse(null);
        if (section == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Section not found"));
        }

        section.setDeleted(true);
        section.setDeletedAt(java.time.LocalDateTime.now());
        section.setPermanentDeleteAt(java.time.LocalDateTime.now().plusDays(30));

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            section.setDeletedBy(auth.getName());
        }

        sectionRepository.save(section);

        if (auditService != null) {
            auditService.log(
                    jjcet.PragatiX.enums.AuditAction.DELETE,
                    jjcet.PragatiX.enums.AuditModule.DEPARTMENT,
                    "SECTION",
                    section.getId(),
                    "Soft deleted section " + section.getSectionName() + " of department " + (section.getDepartment() != null ? section.getDepartment().getName() : "")
            );
        }

        return ResponseEntity.ok(ApiResponse.ok("Section deleted successfully and moved to Recycle Bin", null));
    }
}

