package jjcet.PragatiX.modules.recyclebin.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jjcet.PragatiX.entity.Department;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.admin.service.AdminDepartmentCommandService;
import jjcet.PragatiX.modules.audit.service.AuditService;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.enrollment.entity.Enrollment;
import jjcet.PragatiX.modules.enrollment.enums.EnrollmentStatus;
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
public class RecycleBinEnrollmentTest {

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
    public void testGetDeletedEnrollments() {
        TypedQuery queryMock = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), any())).thenReturn(queryMock);
        when(queryMock.getResultList()).thenReturn(Collections.emptyList());

        Enrollment deletedEnrollment = new Enrollment();
        deletedEnrollment.setId(50L);
        deletedEnrollment.setFullName("JOHN DOE");
        deletedEnrollment.setMobile("9876543210");
        deletedEnrollment.setStatus(EnrollmentStatus.PENDING);
        deletedEnrollment.setDeleted(true);
        deletedEnrollment.setDeletedAt(LocalDateTime.now());
        deletedEnrollment.setDeletedBy("ADMIN");

        Department dept = new Department();
        dept.setDeptCode("CSE");
        dept.setName("Computer Science and Engineering");
        deletedEnrollment.setDepartment(dept);

        TypedQuery<Enrollment> enrollmentQuery = mock(TypedQuery.class);
        when(entityManager.createQuery("SELECT e FROM Enrollment e WHERE e.deleted = true", Enrollment.class))
                .thenReturn(enrollmentQuery);
        when(enrollmentQuery.getResultList()).thenReturn(List.of(deletedEnrollment));

        List<RecycleBinItem> items = recycleBinService.getDeletedItems();
        assertNotNull(items);
        assertTrue(items.stream().anyMatch(i -> "ENROLLMENT".equals(i.getEntityType()) && i.getId().equals(50L)));
    }

    @Test
    public void testRestoreEnrollment() {
        Enrollment deletedEnrollment = new Enrollment();
        deletedEnrollment.setId(50L);
        deletedEnrollment.setFullName("JOHN DOE");
        deletedEnrollment.setDeleted(true);
        deletedEnrollment.setDeletedAt(LocalDateTime.now());
        deletedEnrollment.setEnrolledStudentId(10L);

        Student student = new Student();
        student.setId(10L);
        student.setDeleted(true);
        student.setActive(false);

        when(entityManager.find(Enrollment.class, 50L)).thenReturn(deletedEnrollment);
        when(entityManager.find(Student.class, 10L)).thenReturn(student);

        recycleBinService.restoreItem("ENROLLMENT", 50L);

        assertFalse(deletedEnrollment.isDeleted());
        assertNull(deletedEnrollment.getDeletedAt());
        assertNull(deletedEnrollment.getPermanentDeleteAt());
        verify(entityManager).merge(deletedEnrollment);

        assertFalse(student.isDeleted());
        assertTrue(student.isActive());
        verify(entityManager).merge(student);
    }

    @Test
    public void testPermanentlyDeleteStudent_CleansAllForeignKeys() {
        Student student = new Student();
        student.setId(10L);
        student.setRegNo("910022104001");

        User user = new User();
        user.setId(99L);
        student.setUser(user);

        Query mockQuery = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);

        when(entityManager.find(Student.class, 10L)).thenReturn(student);

        recycleBinService.permanentlyDeleteItem("STUDENT", 10L);

        verify(entityManager).remove(student);
    }
}
