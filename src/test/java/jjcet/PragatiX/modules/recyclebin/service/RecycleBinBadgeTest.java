package jjcet.PragatiX.modules.recyclebin.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jjcet.PragatiX.entity.Badge;
import jjcet.PragatiX.modules.admin.service.AdminDepartmentCommandService;
import jjcet.PragatiX.modules.audit.service.AuditService;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.recyclebin.dto.RecycleBinItem;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.modules.activity.repository.ActivityRepository;
import jjcet.PragatiX.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecycleBinBadgeTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private AuditService auditService;
    @Mock
    private AdminDepartmentCommandService adminDepartmentCommandService;

    private RecycleBinService recycleBinService;

    @BeforeEach
    public void setUp() {
        recycleBinService = new RecycleBinService(
                userRepository,
                studentRepository,
                activityRepository,
                teamRepository,
                entityManager,
                auditService,
                adminDepartmentCommandService
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetDeletedBadges() {
        TypedQuery queryMock = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), any())).thenReturn(queryMock);
        when(queryMock.getResultList()).thenReturn(Collections.emptyList());

        Badge deletedBadge = Badge.builder().name("Star Contributor").proofRequired(true).build();
        deletedBadge.setId(100L);
        deletedBadge.setDeleted(true);
        deletedBadge.setDeletedAt(LocalDateTime.now());
        deletedBadge.setDeletedBy("admin");

        TypedQuery<Badge> badgeQuery = mock(TypedQuery.class);
        when(entityManager.createQuery("SELECT b FROM Badge b WHERE b.deleted = true", Badge.class))
                .thenReturn(badgeQuery);
        when(badgeQuery.getResultList()).thenReturn(List.of(deletedBadge));

        List<RecycleBinItem> items = recycleBinService.getDeletedItems();
        assertNotNull(items);
        assertTrue(items.stream().anyMatch(i -> "BADGE".equals(i.getEntityType()) && i.getId().equals(100L)));
    }

    @Test
    public void testRestoreBadge() {
        Badge deletedBadge = Badge.builder().name("Star Contributor").proofRequired(true).build();
        deletedBadge.setId(100L);
        deletedBadge.setDeleted(true);
        deletedBadge.setDeletedAt(LocalDateTime.now());

        when(entityManager.find(Badge.class, 100L)).thenReturn(deletedBadge);

        recycleBinService.restoreItem("BADGE", 100L);

        assertFalse(deletedBadge.isDeleted());
        assertNull(deletedBadge.getDeletedAt());
        assertNull(deletedBadge.getPermanentDeleteAt());
        assertNull(deletedBadge.getDeletedBy());
        verify(entityManager).merge(deletedBadge);
    }

    @Test
    public void testPermanentlyDeleteBadge_CleansUpForeignKeys() {
        Badge deletedBadge = Badge.builder().name("Star Contributor").proofRequired(true).build();
        deletedBadge.setId(100L);

        Query setFk0 = mock(Query.class);
        Query setFk1 = mock(Query.class);
        Query deleteBadgeRequests = mock(Query.class);
        Query deleteStudentBadges = mock(Query.class);

        when(entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0")).thenReturn(setFk0);
        when(entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1")).thenReturn(setFk1);
        when(entityManager.createNativeQuery("DELETE FROM badge_requests WHERE badge_id = :id")).thenReturn(deleteBadgeRequests);
        when(entityManager.createNativeQuery("DELETE FROM student_badges WHERE badge_id = :id")).thenReturn(deleteStudentBadges);
        when(deleteBadgeRequests.setParameter(anyString(), any())).thenReturn(deleteBadgeRequests);
        when(deleteStudentBadges.setParameter(anyString(), any())).thenReturn(deleteStudentBadges);

        when(entityManager.find(Badge.class, 100L)).thenReturn(deletedBadge);

        recycleBinService.permanentlyDeleteItem("BADGE", 100L);

        verify(deleteBadgeRequests).setParameter("id", 100L);
        verify(deleteBadgeRequests).executeUpdate();
        verify(deleteStudentBadges).setParameter("id", 100L);
        verify(deleteStudentBadges).executeUpdate();
        verify(entityManager).remove(deletedBadge);
    }
}
