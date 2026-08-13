package jjcet.PragatiX;

import jjcet.PragatiX.admin.service.*;
import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.dto.CreateTeamRequest;
import jjcet.PragatiX.dto.TeamResponse;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.student.repository.StudentActivityXpRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TeamClassScopedValidationTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private ActivityAssignmentRepository activityAssignmentRepository;
    @Mock
    private StudentActivityXpRepository studentActivityXpRepository;
    @Mock
    private GroupDeletionAuditLogRepository auditLogRepository;
    @Mock
    private TeamRemovalRequestRepository teamRemovalRequestRepository;
    @Mock
    private TeamValidationService validationService;
    @Mock
    private TeamMapper teamMapper;
    @Mock
    private StageTeamRepository stageTeamRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private SectionRepository sectionRepository;

    private TeamCrudService teamCrudService;

    private User adminUser;
    private Department deptCS;
    private Department deptIT;
    private Section secA;
    private Section secB;

    @BeforeEach
    void setUp() {
        teamCrudService = new TeamCrudService(
                teamRepository,
                userRepository,
                studentRepository,
                activityAssignmentRepository,
                studentActivityXpRepository,
                auditLogRepository,
                teamRemovalRequestRepository,
                validationService,
                teamMapper,
                stageTeamRepository,
                departmentRepository,
                sectionRepository);

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "password",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        deptCS = new Department();
        deptCS.setId(10L);
        deptCS.setName("Cyber Security");

        deptIT = new Department();
        deptIT.setId(20L);
        deptIT.setName("Information Technology");

        secA = new Section();
        secA.setId(101L);
        secA.setSectionName("Section A");

        secB = new Section();
        secB.setId(102L);
        secB.setSectionName("Section B");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(validationService.canCreateTeam(any(), any())).thenReturn(true);
        when(teamMapper.toTeamResponse(any())).thenReturn(new TeamResponse());
        when(teamRepository.save(any(Team.class))).thenAnswer(i -> {
            Team t = i.getArgument(0);
            t.setId(System.currentTimeMillis());
            return t;
        });
    }

    private Student createMockStudent(String regNo, String name, Department dept, String year, Section sec) {
        Student s = new Student();
        s.setId(System.nanoTime());
        s.setRegNo(regNo);
        s.setFullName(name);
        s.setDepartment(dept);
        s.setYear(year);
        s.setSection(sec);
        when(studentRepository.findByRegNo(regNo)).thenReturn(Optional.of(s));
        when(teamRepository.findAllTeamsByStudentId(s.getId())).thenReturn(List.of());
        return s;
    }

    @Test
    @DisplayName("Allow same team name 'Team A' across different classes (Section, Dept, Year)")
    void testSameTeamNameAllowedInDifferentClasses() {
        // 1. Team A in CS - 1st Year - Section A
        Student student1 = createMockStudent("REG001", "Student 1", deptCS, "1st Year", secA);
        when(teamRepository.existsByTeamNameAndClass("Team A", deptCS.getId(), "1st Year", secA.getId()))
                .thenReturn(false);

        CreateTeamRequest req1 = new CreateTeamRequest();
        req1.setName("Team A");
        req1.setSize(5);
        req1.setCaptainStudentId("REG001");

        ResponseEntity<ApiResponse<TeamResponse>> res1 = teamCrudService.createTeam(req1, "admin");
        assertEquals(HttpStatus.CREATED, res1.getStatusCode());
        assertTrue(res1.getBody().isSuccess());

        // 2. Team A in CS - 1st Year - Section B (different section)
        Student student2 = createMockStudent("REG002", "Student 2", deptCS, "1st Year", secB);
        when(teamRepository.existsByTeamNameAndClass("Team A", deptCS.getId(), "1st Year", secB.getId()))
                .thenReturn(false);

        CreateTeamRequest req2 = new CreateTeamRequest();
        req2.setName("Team A");
        req2.setSize(5);
        req2.setCaptainStudentId("REG002");

        ResponseEntity<ApiResponse<TeamResponse>> res2 = teamCrudService.createTeam(req2, "admin");
        assertEquals(HttpStatus.CREATED, res2.getStatusCode());
        assertTrue(res2.getBody().isSuccess());

        // 3. Team A in IT - 1st Year - Section A (different department)
        Student student3 = createMockStudent("REG003", "Student 3", deptIT, "1st Year", secA);
        when(teamRepository.existsByTeamNameAndClass("Team A", deptIT.getId(), "1st Year", secA.getId()))
                .thenReturn(false);

        CreateTeamRequest req3 = new CreateTeamRequest();
        req3.setName("Team A");
        req3.setSize(5);
        req3.setCaptainStudentId("REG003");

        ResponseEntity<ApiResponse<TeamResponse>> res3 = teamCrudService.createTeam(req3, "admin");
        assertEquals(HttpStatus.CREATED, res3.getStatusCode());
        assertTrue(res3.getBody().isSuccess());

        // 4. Team A in CS - 2nd Year - Section A (different year)
        Student student4 = createMockStudent("REG004", "Student 4", deptCS, "2nd Year", secA);
        when(teamRepository.existsByTeamNameAndClass("Team A", deptCS.getId(), "2nd Year", secA.getId()))
                .thenReturn(false);

        CreateTeamRequest req4 = new CreateTeamRequest();
        req4.setName("Team A");
        req4.setSize(5);
        req4.setCaptainStudentId("REG004");

        ResponseEntity<ApiResponse<TeamResponse>> res4 = teamCrudService.createTeam(req4, "admin");
        assertEquals(HttpStatus.CREATED, res4.getStatusCode());
        assertTrue(res4.getBody().isSuccess());
    }

    @Test
    @DisplayName("Reject duplicate team name 'Team A' within the exact same class")
    void testDuplicateTeamNameRejectedInSameClass() {
        // Given Team A already exists in CS - 1st Year - Section A
        Student student5 = createMockStudent("REG005", "Student 5", deptCS, "1st Year", secA);
        when(teamRepository.existsByTeamNameAndClass("Team A", deptCS.getId(), "1st Year", secA.getId()))
                .thenReturn(true);

        CreateTeamRequest req = new CreateTeamRequest();
        req.setName("Team A");
        req.setSize(5);
        req.setCaptainStudentId("REG005");

        ResponseEntity<ApiResponse<TeamResponse>> res = teamCrudService.createTeam(req, "admin");
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertFalse(res.getBody().isSuccess());
        assertTrue(res.getBody().getMessage().contains("already exists in this class"));
    }
}
