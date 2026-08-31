package jjcet.PragatiX.modules.admin.service;

import jjcet.PragatiX.dto.ActivityCategoryCreateUpdateDto;
import jjcet.PragatiX.dto.ActivityCategoryDto;
import jjcet.PragatiX.entity.ActivityCategory;
import jjcet.PragatiX.enums.AuditAction;
import jjcet.PragatiX.enums.AuditModule;
import jjcet.PragatiX.modules.audit.service.AuditService;
import jjcet.PragatiX.repository.ActivityCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActivityCategoryServiceTest {

    @Mock
    private ActivityCategoryRepository categoryRepository;

    @Mock
    private AuditService auditService;

    private ActivityCategoryService categoryService;

    @BeforeEach
    public void setUp() {
        categoryService = new ActivityCategoryService(categoryRepository, auditService);
    }

    @Test
    public void testGetAllActiveCategories() {
        ActivityCategory c1 = ActivityCategory.builder().name("Academic").displayOrder(1).build();
        c1.setId(1L);
        ActivityCategory c2 = ActivityCategory.builder().name("Skill").displayOrder(2).build();
        c2.setId(2L);

        when(categoryRepository.findByDeletedFalseOrderByDisplayOrderAscNameAsc()).thenReturn(List.of(c1, c2));

        List<ActivityCategoryDto> list = categoryService.getAllActiveCategories();
        assertEquals(2, list.size());
        assertEquals("Academic", list.get(0).getName());
        assertEquals("Skill", list.get(1).getName());
    }

    @Test
    public void testCreateCategory_Success() {
        ActivityCategoryCreateUpdateDto dto = new ActivityCategoryCreateUpdateDto();
        dto.setName("Robotics");
        dto.setDescription("Robotics Club");
        dto.setDisplayOrder(11);

        when(categoryRepository.existsByNameIgnoreCaseAndDeletedFalse("Robotics")).thenReturn(false);
        when(categoryRepository.save(any(ActivityCategory.class))).thenAnswer(inv -> {
            ActivityCategory c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });

        ActivityCategoryDto created = categoryService.createCategory(dto, "adminUser");
        assertNotNull(created);
        assertEquals(100L, created.getId());
        assertEquals("Robotics", created.getName());

        verify(auditService).log(
                eq(AuditAction.CREATE),
                eq(AuditModule.ACTIVITY),
                eq("ACTIVITY_CATEGORY"),
                eq(100L),
                contains("Robotics")
        );
    }

    @Test
    public void testCreateCategory_Duplicate_ThrowsException() {
        ActivityCategoryCreateUpdateDto dto = new ActivityCategoryCreateUpdateDto();
        dto.setName("Academic");

        when(categoryRepository.existsByNameIgnoreCaseAndDeletedFalse("Academic")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            categoryService.createCategory(dto, "adminUser");
        });
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    public void testUpdateCategory_Success() {
        ActivityCategory existing = ActivityCategory.builder().name("Old Cat").build();
        existing.setId(5L);

        when(categoryRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNotAndDeletedFalse("New Cat", 5L)).thenReturn(false);
        when(categoryRepository.save(any(ActivityCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityCategoryCreateUpdateDto dto = new ActivityCategoryCreateUpdateDto();
        dto.setName("New Cat");
        dto.setDescription("Updated description");

        ActivityCategoryDto updated = categoryService.updateCategory(5L, dto, "adminUser");
        assertEquals("New Cat", updated.getName());
        assertEquals("Updated description", updated.getDescription());

        verify(auditService).log(
                eq(AuditAction.UPDATE),
                eq(AuditModule.ACTIVITY),
                eq("ACTIVITY_CATEGORY"),
                eq(5L),
                contains("New Cat")
        );
    }

    @Test
    public void testDeleteCategory_MovesToRecycleBin() {
        ActivityCategory existing = ActivityCategory.builder().name("To Delete").build();
        existing.setId(7L);
        existing.setDeleted(false);

        when(categoryRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(existing));

        categoryService.deleteCategory(7L, "adminUser");

        ArgumentCaptor<ActivityCategory> captor = ArgumentCaptor.forClass(ActivityCategory.class);
        verify(categoryRepository).save(captor.capture());

        ActivityCategory saved = captor.getValue();
        assertTrue(saved.isDeleted());
        assertNotNull(saved.getDeletedAt());
        assertNotNull(saved.getPermanentDeleteAt());
        assertEquals("adminUser", saved.getDeletedBy());

        verify(auditService).log(
                eq(AuditAction.DELETE),
                eq(AuditModule.ACTIVITY),
                eq("ACTIVITY_CATEGORY"),
                eq(7L),
                contains("Recycle Bin")
        );
    }
}
