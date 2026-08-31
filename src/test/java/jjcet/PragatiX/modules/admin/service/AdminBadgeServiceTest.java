package jjcet.PragatiX.modules.admin.service;

import jjcet.PragatiX.dto.BadgeCreateUpdateDto;
import jjcet.PragatiX.dto.BadgeDto;
import jjcet.PragatiX.entity.Badge;
import jjcet.PragatiX.enums.AuditAction;
import jjcet.PragatiX.enums.AuditModule;
import jjcet.PragatiX.modules.audit.service.AuditService;
import jjcet.PragatiX.repository.BadgeRepository;
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
public class AdminBadgeServiceTest {

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private AuditService auditService;

    private AdminBadgeService adminBadgeService;

    @BeforeEach
    public void setUp() {
        adminBadgeService = new AdminBadgeService(badgeRepository, auditService);
    }

    @Test
    public void testGetAllActiveBadges() {
        Badge b1 = Badge.builder().name("Badge 1").tier("FOUNDATION").proofRequired(true).build();
        b1.setId(1L);
        Badge b2 = Badge.builder().name("Badge 2").tier("ACHIEVEMENT").proofRequired(false).build();
        b2.setId(2L);

        when(badgeRepository.findByDeletedFalse()).thenReturn(List.of(b1, b2));

        List<BadgeDto> results = adminBadgeService.getAllActiveBadges();
        assertEquals(2, results.size());
        assertEquals("Badge 1", results.get(0).getName());
        assertTrue(results.get(0).isProofRequired());
        assertEquals("Badge 2", results.get(1).getName());
        assertFalse(results.get(1).isProofRequired());
    }

    @Test
    public void testCreateBadge_Success() {
        BadgeCreateUpdateDto dto = new BadgeCreateUpdateDto();
        dto.setName("Hackathon Winner");
        dto.setTier("EXCELLENCE");
        dto.setDescription("Winner of Hackathon");
        dto.setXpRequired(200);
        dto.setProofRequired(true);
        dto.setRarity("EPIC");
        dto.setApprovalAuthority("Admin");

        when(badgeRepository.existsByNameAndDeletedFalse("Hackathon Winner")).thenReturn(false);
        when(badgeRepository.save(any(Badge.class))).thenAnswer(invocation -> {
            Badge b = invocation.getArgument(0);
            b.setId(10L);
            return b;
        });

        BadgeDto created = adminBadgeService.createBadge(dto, "adminUser");
        assertNotNull(created);
        assertEquals(10L, created.getId());
        assertEquals("Hackathon Winner", created.getName());
        assertTrue(created.isProofRequired());

        verify(auditService).log(
                eq(AuditAction.CREATE),
                eq(AuditModule.BADGE),
                eq("BADGE"),
                eq(10L),
                contains("Hackathon Winner")
        );
    }

    @Test
    public void testCreateBadge_DuplicateName_ThrowsException() {
        BadgeCreateUpdateDto dto = new BadgeCreateUpdateDto();
        dto.setName("Existing Badge");
        when(badgeRepository.existsByNameAndDeletedFalse("Existing Badge")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            adminBadgeService.createBadge(dto, "adminUser");
        });
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    public void testUpdateBadge_Success() {
        Badge existing = Badge.builder().name("Old Name").tier("FOUNDATION").proofRequired(true).build();
        existing.setId(5L);

        when(badgeRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(existing));
        when(badgeRepository.existsByNameAndIdNotAndDeletedFalse("New Name", 5L)).thenReturn(false);
        when(badgeRepository.save(any(Badge.class))).thenAnswer(inv -> inv.getArgument(0));

        BadgeCreateUpdateDto dto = new BadgeCreateUpdateDto();
        dto.setName("New Name");
        dto.setTier("ACHIEVEMENT");
        dto.setProofRequired(false);
        dto.setXpRequired(150);

        BadgeDto updated = adminBadgeService.updateBadge(5L, dto, "adminUser");
        assertEquals("New Name", updated.getName());
        assertFalse(updated.isProofRequired());
        assertEquals(150, updated.getXpRequired());

        verify(auditService).log(
                eq(AuditAction.UPDATE),
                eq(AuditModule.BADGE),
                eq("BADGE"),
                eq(5L),
                contains("New Name")
        );
    }

    @Test
    public void testDeleteBadge_MovesToRecycleBin() {
        Badge existing = Badge.builder().name("Badge To Delete").proofRequired(true).build();
        existing.setId(7L);
        existing.setDeleted(false);

        when(badgeRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(existing));

        adminBadgeService.deleteBadge(7L, "adminUser");

        ArgumentCaptor<Badge> captor = ArgumentCaptor.forClass(Badge.class);
        verify(badgeRepository).save(captor.capture());

        Badge saved = captor.getValue();
        assertTrue(saved.isDeleted());
        assertNotNull(saved.getDeletedAt());
        assertNotNull(saved.getPermanentDeleteAt());
        assertEquals("adminUser", saved.getDeletedBy());

        verify(auditService).log(
                eq(AuditAction.DELETE),
                eq(AuditModule.BADGE),
                eq("BADGE"),
                eq(7L),
                contains("Recycle Bin")
        );
    }
}
