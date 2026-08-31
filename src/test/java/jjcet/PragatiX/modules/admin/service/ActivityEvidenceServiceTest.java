package jjcet.PragatiX.modules.admin.service;

import jjcet.PragatiX.dto.ActivityEvidenceCreateUpdateDto;
import jjcet.PragatiX.dto.ActivityEvidenceDto;
import jjcet.PragatiX.entity.ActivityEvidence;
import jjcet.PragatiX.enums.AuditAction;
import jjcet.PragatiX.enums.AuditModule;
import jjcet.PragatiX.modules.audit.service.AuditService;
import jjcet.PragatiX.repository.ActivityEvidenceRepository;
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
public class ActivityEvidenceServiceTest {

    @Mock
    private ActivityEvidenceRepository evidenceRepository;

    @Mock
    private AuditService auditService;

    private ActivityEvidenceService evidenceService;

    @BeforeEach
    public void setUp() {
        evidenceService = new ActivityEvidenceService(evidenceRepository, auditService);
    }

    @Test
    public void testGetAllActiveEvidences() {
        ActivityEvidence e1 = ActivityEvidence.builder().name("Handwritten").displayOrder(1).build();
        e1.setId(1L);
        ActivityEvidence e2 = ActivityEvidence.builder().name("Soft Copy").displayOrder(2).build();
        e2.setId(2L);

        when(evidenceRepository.findByDeletedFalseOrderByDisplayOrderAscNameAsc()).thenReturn(List.of(e1, e2));

        List<ActivityEvidenceDto> list = evidenceService.getAllActiveEvidences();
        assertEquals(2, list.size());
        assertEquals("Handwritten", list.get(0).getName());
        assertEquals("Soft Copy", list.get(1).getName());
    }

    @Test
    public void testCreateEvidence_Success() {
        ActivityEvidenceCreateUpdateDto dto = new ActivityEvidenceCreateUpdateDto();
        dto.setName("Github PR");
        dto.setDescription("Pull Request link");
        dto.setDisplayOrder(9);

        when(evidenceRepository.existsByNameIgnoreCaseAndDeletedFalse("Github PR")).thenReturn(false);

        ActivityEvidence saved = ActivityEvidence.builder()
                .name("Github PR")
                .description("Pull Request link")
                .displayOrder(9)
                .deleted(false)
                .build();
        saved.setId(50L);

        when(evidenceRepository.save(any(ActivityEvidence.class))).thenReturn(saved);

        ActivityEvidenceDto res = evidenceService.createEvidence(dto, "testAdmin");

        assertNotNull(res);
        assertEquals(50L, res.getId());
        assertEquals("Github PR", res.getName());

        verify(auditService, times(1)).log(
                eq(AuditAction.CREATE),
                eq(AuditModule.ACTIVITY),
                eq("ACTIVITY_EVIDENCE"),
                eq(50L),
                contains("Github PR")
        );
    }

    @Test
    public void testCreateEvidence_Duplicate_ThrowsException() {
        ActivityEvidenceCreateUpdateDto dto = new ActivityEvidenceCreateUpdateDto();
        dto.setName("Handwritten");

        when(evidenceRepository.existsByNameIgnoreCaseAndDeletedFalse("Handwritten")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            evidenceService.createEvidence(dto, "testAdmin");
        });

        assertTrue(ex.getMessage().contains("already exists"));
        verify(evidenceRepository, never()).save(any());
    }

    @Test
    public void testUpdateEvidence_Success() {
        ActivityEvidence ev = ActivityEvidence.builder().name("Old Name").description("Desc").build();
        ev.setId(10L);

        when(evidenceRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(ev));
        when(evidenceRepository.existsByNameIgnoreCaseAndIdNotAndDeletedFalse("New Name", 10L)).thenReturn(false);
        when(evidenceRepository.save(any(ActivityEvidence.class))).thenAnswer(i -> i.getArgument(0));

        ActivityEvidenceCreateUpdateDto updateDto = new ActivityEvidenceCreateUpdateDto();
        updateDto.setName("New Name");
        updateDto.setDescription("Updated Desc");

        ActivityEvidenceDto res = evidenceService.updateEvidence(10L, updateDto, "testAdmin");

        assertEquals("New Name", res.getName());
        assertEquals("Updated Desc", res.getDescription());

        verify(auditService, times(1)).log(
                eq(AuditAction.UPDATE),
                eq(AuditModule.ACTIVITY),
                eq("ACTIVITY_EVIDENCE"),
                eq(10L),
                contains("New Name")
        );
    }

    @Test
    public void testDeleteEvidence_MovesToRecycleBin() {
        ActivityEvidence ev = ActivityEvidence.builder().name("To Delete").deleted(false).build();
        ev.setId(7L);

        when(evidenceRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(ev));

        evidenceService.deleteEvidence(7L, "adminUser");

        ArgumentCaptor<ActivityEvidence> captor = ArgumentCaptor.forClass(ActivityEvidence.class);
        verify(evidenceRepository).save(captor.capture());

        ActivityEvidence captured = captor.getValue();
        assertTrue(captured.isDeleted());
        assertNotNull(captured.getDeletedAt());
        assertNotNull(captured.getPermanentDeleteAt());
        assertEquals("adminUser", captured.getDeletedBy());

        verify(auditService, times(1)).log(
                eq(AuditAction.DELETE),
                eq(AuditModule.ACTIVITY),
                eq("ACTIVITY_EVIDENCE"),
                eq(7L),
                contains("Recycle Bin")
        );
    }
}
