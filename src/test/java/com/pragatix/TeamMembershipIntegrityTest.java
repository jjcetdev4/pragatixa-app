package jjcet.PragatiX;

import jjcet.PragatiX.admin.service.*;
import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.dto.CreateTeamRequest;
import jjcet.PragatiX.dto.TeamResponse;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.student.repository.StudentActivityXpRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.modules.student.service.StudentXpValidator;
import jjcet.PragatiX.repository.*;
import org.junit.jupiter.api.AfterEach;
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TeamMembershipIntegrityTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StageTeamRepository stageTeamRepository;
    @Mock
    private TeamRemovalRequestRepository teamRemovalRequestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ActivityAssignmentRepository activityAssignmentRepository;
    @Mock
    private StudentActivityXpRepository studentActivityXpRepository;
    @Mock
    private GroupDeletionAuditLogRepository auditLogRepository;
    @Mock
    private TeamValidationService validationService;
    @Mock
    private TeamMapper teamMapper;
    @Mock
    private TeamCleanupService teamCleanupService;
    @Mock
    private CaptainSelectionService captainSelectionService;
    @Mock
    private LeadershipSyncService leadershipSyncService;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private SectionRepository sectionRepository;

    private TeamCrudService teamCrudService;
    private TeamMemberService teamMemberService;
    private StudentXpValidator studentXpValidator;

    private Student student1;
    private Student student2;
    private Student captainStudent;
    private Team teamA;
    private Team teamB;
    private User adminUser;

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(validationService.canCreateTeam(any(), any())).thenReturn(true);

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

        teamMemberService = new TeamMemberService(
                teamRepository,
                studentRepository,
                userRepository,
                validationService,
                captainSelectionService,
                teamMapper,
                stageTeamRepository,
                teamCleanupService,
                leadershipSyncService);

        studentXpValidator = new StudentXpValidator(studentActivityXpRepository, null, teamRepository,
                activityAssignmentRepository);

        Department dept = new Department();
        dept.setId(1L);
        dept.setDeptName("CSE");

        Section sec = new Section();
        sec.setId(1L);
        sec.setSectionName("A");

        student1 = new Student();
        student1.setId(101L);
        student1.setRegNo("REG101");
        student1.setFullName("Student One");
        student1.setDepartment(dept);
        student1.setYear("1");
        student1.setSection(sec);
        student1.setStage(1);

        student2 = new Student();
        student2.setId(102L);
        student2.setRegNo("REG102");
        student2.setFullName("Student Two");
        student2.setDepartment(dept);
        student2.setYear("1");
        student2.setSection(sec);
        student2.setStage(1);

        captainStudent = new Student();
        captainStudent.setId(100L);
        captainStudent.setRegNo("REG100");
        captainStudent.setFullName("Captain Student");
        captainStudent.setDepartment(dept);
        captainStudent.setYear("1");
        captainStudent.setSection(sec);
        captainStudent.setStage(1);

        teamA = new Team();
        teamA.setId(1L);
        teamA.setName("Team Alpha");
        teamA.setSize(10);
        teamA.setDepartment(dept);
        teamA.setYear("1");
        teamA.setSection(sec);
        teamA.setCaptain(captainStudent);
        teamA.setMembers(new HashSet<>(Set.of(captainStudent)));

        teamB = new Team();
        teamB.setId(2L);
        teamB.setName("Team Beta");
        teamB.setSize(10);
        teamB.setDepartment(dept);
        teamB.setYear("1");
        teamB.setSection(sec);
        teamB.setCaptain(null);
        teamB.setMembers(new HashSet<>());

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setRoles(new HashSet<>(Set.of(new Role(1L, "ROLE_ADMIN"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── 1. Team Creation Flow Integrity Tests ─────────────────────────────────

    @Test
    @DisplayName("Create Team: Should reject when selected student already belongs to another team")
    void createTeam_rejectsWhenStudentAlreadyInTeam() {
        // Arrange
        student1.setTeam(teamA); // Student 1 is already in Team A

        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("Team New");
        request.setSize(10);
        request.setCaptainStudentId("REG100");
        request.setMemberStudentIds(List.of("REG101"));

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(studentRepository.findByRegNo("admin")).thenReturn(Optional.empty());
        when(studentRepository.findByRegNo("REG100")).thenReturn(Optional.of(captainStudent));
        when(studentRepository.findByRegNoIn(List.of("REG101"))).thenReturn(List.of(student1));

        // Act
        ResponseEntity<ApiResponse<TeamResponse>> response = teamCrudService.createTeam(request, "admin");

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("is already assigned to a team"));
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    @DisplayName("Create Team: Should reject when captain is already in another team")
    void createTeam_rejectsWhenCaptainAlreadyInTeam() {
        // Arrange
        captainStudent.setTeam(teamA);

        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("Team New");
        request.setSize(10);
        request.setCaptainStudentId("REG100");
        request.setMemberStudentIds(List.of());

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(studentRepository.findByRegNo("admin")).thenReturn(Optional.empty());
        when(studentRepository.findByRegNo("REG100")).thenReturn(Optional.of(captainStudent));

        // Act
        ResponseEntity<ApiResponse<TeamResponse>> response = teamCrudService.createTeam(request, "admin");

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("already belongs to an existing team"));
    }

    // ── 2. Add Member Flow Integrity Tests ───────────────────────────────────

    @Test
    @DisplayName("Add Member: Should reject when student already belongs to another team")
    void addMember_rejectsWhenStudentAlreadyInTeam() {
        // Arrange
        student1.setTeam(teamA);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(teamRepository.findById(teamB.getId())).thenReturn(Optional.of(teamB));
        when(studentRepository.findByRegNo(student1.getRegNo())).thenReturn(Optional.of(student1));

        // Act
        ResponseEntity<ApiResponse<Void>> response = teamMemberService.addMemberToTeam(teamB.getId(),
                student1.getRegNo());

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("already belongs to an existing team"));
        assertFalse(teamB.getMembers().contains(student1));
    }

    @Test
    @DisplayName("Add Member: Should succeed when student is unassigned and update relationships")
    void addMember_succeedsWhenStudentIsFree() {
        // Arrange
        student1.setTeam(null);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(teamRepository.findById(teamB.getId())).thenReturn(Optional.of(teamB));
        when(studentRepository.findByRegNo(student1.getRegNo())).thenReturn(Optional.of(student1));
        when(teamRepository.findAllTeamsByStudentId(student1.getId())).thenReturn(List.of());

        // Act
        ResponseEntity<ApiResponse<Void>> response = teamMemberService.addMemberToTeam(teamB.getId(),
                student1.getRegNo());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(teamB, student1.getTeam());
        assertTrue(teamB.getMembers().contains(student1));
        verify(studentRepository).save(student1);
        verify(teamRepository).save(teamB);
    }

    // ── 3. Remove Member Flow Integrity Tests ─────────────────────────────────

    @Test
    @DisplayName("Remove Member: Should unlink student and clear vice-captain if assigned")
    void removeMember_unlinksAndCleansUp() {
        // Arrange
        teamA.getMembers().add(student1);
        student1.setTeam(teamA);
        teamA.setViceCaptain(student1);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(teamRepository.findById(teamA.getId())).thenReturn(Optional.of(teamA));
        when(studentRepository.findByRegNo(student1.getRegNo())).thenReturn(Optional.of(student1));
        when(stageTeamRepository.findByTeamId(teamA.getId())).thenReturn(List.of());

        // Act
        ResponseEntity<ApiResponse<TeamResponse>> response = teamMemberService.removeMemberFromTeam(teamA.getId(),
                student1.getRegNo());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(student1.getTeam());
        assertFalse(teamA.getMembers().contains(student1));
        assertNull(teamA.getViceCaptain());
        verify(studentRepository).save(student1);
        verify(teamRepository).save(teamA);
    }

    // ── 4. XP Module Validator Integrity Test ─────────────────────────────────

    @Test
    @DisplayName("StudentXpValidator: Should throw when multiple teams exist in DB query")
    void xpValidator_throwsWhenNonUniqueTeamsExist() {
        // Arrange
        Activity activity = new Activity();
        activity.setId(10L);
        activity.setModeType("Group");
        activity.setAwardFrequency("One Time");

        when(teamRepository.findTeamByStudentId(student1.getId()))
                .thenThrow(new IllegalStateException("Data integrity violation: Student is assigned to 2 teams!"));

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            studentXpValidator.checkAwardLimit(student1, activity);
        });

        assertTrue(ex.getMessage().contains("Data integrity violation"));
    }
}
