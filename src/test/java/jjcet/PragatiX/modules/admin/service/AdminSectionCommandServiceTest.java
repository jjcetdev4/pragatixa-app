package jjcet.PragatiX.modules.admin.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.Department;
import jjcet.PragatiX.entity.Section;
import jjcet.PragatiX.repository.DepartmentRepository;
import jjcet.PragatiX.repository.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSectionCommandServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private AdminSectionCommandService adminSectionCommandService;

    private Department department;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(1L);
        department.setName("Computer Science");
        department.setCode("CSE");
        department.setSupportsSections(true);
    }

    private Section createSection(Long id, String name, Department dept) {
        Section s = new Section();
        s.setId(id);
        s.setSectionName(name);
        s.setDepartment(dept);
        return s;
    }

    @Test
    @DisplayName("Successfully creates first section 'A' when department has no sections")
    void testCreateFirstSectionA() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(sectionRepository.findByDepartment_IdOrderBySectionNameAsc(1L)).thenReturn(Collections.emptyList());
        when(sectionRepository.save(any(Section.class))).thenAnswer(inv -> {
            Section s = inv.getArgument(0);
            s.setId(101L);
            return s;
        });

        Map<String, Object> body = Map.of("sectionName", "A");
        ResponseEntity<ApiResponse<Section>> response = adminSectionCommandService.createSection(1L, body);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("A", response.getBody().getData().getSectionName());
    }

    @Test
    @DisplayName("Fails when first section is not 'A'")
    void testCreateFirstSectionNotA_Fails() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(sectionRepository.findByDepartment_IdOrderBySectionNameAsc(1L)).thenReturn(Collections.emptyList());

        Map<String, Object> body = Map.of("sectionName", "B");
        ResponseEntity<ApiResponse<Section>> response = adminSectionCommandService.createSection(1L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("must start from 'A'"));
    }

    @Test
    @DisplayName("Fails when entering multiple characters (e.g. 'Section A' or 'AB')")
    void testCreateNonSingleLetter_Fails() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        Map<String, Object> body = Map.of("sectionName", "Section A");
        ResponseEntity<ApiResponse<Section>> response = adminSectionCommandService.createSection(1L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("single letter"));
    }

    @Test
    @DisplayName("Fails when duplicate section is added")
    void testCreateDuplicateSection_Fails() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        Section existingA = createSection(101L, "A", department);
        when(sectionRepository.findByDepartment_IdOrderBySectionNameAsc(1L)).thenReturn(List.of(existingA));

        Map<String, Object> body = Map.of("sectionName", "A");
        ResponseEntity<ApiResponse<Section>> response = adminSectionCommandService.createSection(1L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Successfully creates next section 'B' when 'A' already exists")
    void testCreateSectionBAfterA_Success() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        Section existingA = createSection(101L, "A", department);
        when(sectionRepository.findByDepartment_IdOrderBySectionNameAsc(1L)).thenReturn(List.of(existingA));
        when(sectionRepository.save(any(Section.class))).thenAnswer(inv -> {
            Section s = inv.getArgument(0);
            s.setId(102L);
            return s;
        });

        Map<String, Object> body = Map.of("sectionName", "b"); // lowercase should be converted to uppercase
        ResponseEntity<ApiResponse<Section>> response = adminSectionCommandService.createSection(1L, body);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("B", response.getBody().getData().getSectionName());
    }

    @Test
    @DisplayName("Fails when skipping a letter in sequence (e.g. adding 'C' when only 'A' exists)")
    void testSkipLetterInSequence_Fails() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        Section existingA = createSection(101L, "A", department);
        when(sectionRepository.findByDepartment_IdOrderBySectionNameAsc(1L)).thenReturn(List.of(existingA));

        Map<String, Object> body = Map.of("sectionName", "C");
        ResponseEntity<ApiResponse<Section>> response = adminSectionCommandService.createSection(1L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Next section must be 'B'"));
    }

    @Test
    @DisplayName("Fails when department does not support sections")
    void testDepartmentDoesNotSupportSections_Fails() {
        department.setSupportsSections(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        Map<String, Object> body = Map.of("sectionName", "A");
        ResponseEntity<ApiResponse<Section>> response = adminSectionCommandService.createSection(1L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("does not support sections"));
    }
}
