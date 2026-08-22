package jjcet.PragatiX.modules.admin.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.Department;
import jjcet.PragatiX.entity.Section;
import jjcet.PragatiX.repository.DepartmentRepository;
import jjcet.PragatiX.repository.SectionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @PersistenceContext
    private EntityManager entityManager;

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

    /**
     * Clear all FK references to the given section before deleting it.
     * This nullifies section_id in ALL tables that reference the section table.
     */
    private void clearSectionReferences(Long sectionId) {
        String[] tables = {
            "users", "students", "faculty",
            "activity_assignments", "activity_temporary_assignments",
            "attendance_sessions", "badge_requests", "teams"
        };
        for (String table : tables) {
            try {
                entityManager.createNativeQuery("UPDATE " + table + " SET section_id = NULL WHERE section_id = :sid")
                        .setParameter("sid", sectionId)
                        .executeUpdate();
            } catch (Exception e) {
                // Table might not exist in this DB version, skip silently
                System.out.println("SECTION DELETE: Skipped table " + table + " (" + e.getMessage() + ")");
            }
        }
        entityManager.flush();
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteSection(Long id, Long sectionId) {
        if (!departmentRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Department not found"));
        }
        if (!sectionRepository.existsById(sectionId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Section not found"));
        }
        clearSectionReferences(sectionId);
        sectionRepository.deleteById(sectionId);
        return ResponseEntity.ok(ApiResponse.ok("Section deleted successfully", null));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteSection(Long sectionId) {
        if (!sectionRepository.existsById(sectionId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Section not found"));
        }
        clearSectionReferences(sectionId);
        sectionRepository.deleteById(sectionId);
        return ResponseEntity.ok(ApiResponse.ok("Section deleted successfully", null));
    }
}

