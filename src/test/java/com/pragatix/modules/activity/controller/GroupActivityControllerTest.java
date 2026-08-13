package jjcet.PragatiX.modules.activity.controller;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.dto.TeamResponse;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.modules.activity.repository.ActivityStageRepository;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.modules.student.service.XpEngineService;
import jjcet.PragatiX.repository.ActivityAssignmentRepository;
import jjcet.PragatiX.repository.StageTeamRepository;
import jjcet.PragatiX.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GroupActivityControllerTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private ActivityAssignmentRepository activityAssignmentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private XpEngineService xpEngineService;
    @Mock
    private StageTeamRepository stageTeamRepository;
    @Mock
    private ActivityStageRepository activityStageRepository;

    @InjectMocks
    private GroupActivityController groupActivityController;

    private Department deptCSE;
    private Section secA;
    private ActivityStage stage1;
    private ActivityStage stage2;
    private ActivityAssignment assignment;
    private Activity activity;
    private Team team1;
    private Team team2;

    @BeforeEach
    void setUp() {
        deptCSE = new Department();
        deptCSE.setId(10L);
        deptCSE.setName("CSE");

        secA = new Section();
        secA.setId(20L);
        secA.setSectionName("A");

        stage1 = new ActivityStage();
        stage1.setId(1L);
        stage1.setName("Stage 1");
        stage1.setDisplayOrder(1);

        stage2 = new ActivityStage();
        stage2.setId(2L);
        stage2.setName("Stage 2");
        stage2.setDisplayOrder(2);

        activity = new Activity();
        activity.setId(100L);
        activity.setActivityName("Code Sprint");
        activity.setStage(stage1);

        assignment = new ActivityAssignment();
        assignment.setId(500L);
        assignment.setActivity(activity);
        assignment.setAssignmentScope(AssignmentScope.SECTION);
        assignment.setDepartment(deptCSE);
        assignment.setYear("1");
        assignment.setSection(secA);
        assignment.setStage(stage1);

        // Student in Stage 1
        Student student1 = new Student();
        student1.setRegNo("S101");
        student1.setFullName("Alice");
        student1.setActive(true);
        student1.setStage(1);
        student1.setCurrentStage(1);

        // Student in Stage 2
        Student student2 = new Student();
        student2.setRegNo("S102");
        student2.setFullName("Bob");
        student2.setActive(true);
        student2.setStage(2);
        student2.setCurrentStage(2);

        // Team 1 -> Stage 1
        team1 = new Team();
        team1.setId(1L);
        team1.setName("Alpha Warriors");
        team1.setDepartment(deptCSE);
        team1.setSection(secA);
        team1.setYear("1");
        team1.setCaptain(student1);
        team1.setMembers(new HashSet<>(Collections.singletonList(student1)));

        // Team 2 -> Stage 2
        team2 = new Team();
        team2.setId(2L);
        team2.setName("Beta Champions");
        team2.setDepartment(deptCSE);
        team2.setSection(secA);
        team2.setYear("1");
        team2.setCaptain(student2);
        team2.setMembers(new HashSet<>(Collections.singletonList(student2)));

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("teacher1");
        SecurityContext secContext = mock(SecurityContext.class);
        when(secContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secContext);
    }

    @Test
    void testGetTeamsForAssignment_Stage1Filtering() {
        when(activityAssignmentRepository.findById(500L)).thenReturn(Optional.of(assignment));
        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.empty());
        when(teamRepository.findAll()).thenReturn(Arrays.asList(team1, team2));

        StageTeam st1 = new StageTeam();
        st1.setTeam(team1);
        st1.setStage(stage1);

        StageTeam st2 = new StageTeam();
        st2.setTeam(team2);
        st2.setStage(stage2);

        when(stageTeamRepository.findByTeamId(1L)).thenReturn(Collections.singletonList(st1));
        when(stageTeamRepository.findByTeamId(2L)).thenReturn(Collections.singletonList(st2));

        ResponseEntity<ApiResponse<List<TeamResponse>>> response = groupActivityController.getTeamsForAssignment(500L,
                1L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        List<TeamResponse> teams = response.getBody().getData();
        assertEquals(1, teams.size());
        assertEquals("Alpha Warriors", teams.get(0).getTeamName());
        assertEquals("S101", teams.get(0).getCaptainId());
    }

    @Test
    void testGetTeamsForAssignment_Stage2Filtering() {
        when(activityAssignmentRepository.findById(500L)).thenReturn(Optional.of(assignment));
        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.empty());
        when(activityStageRepository.findById(2L)).thenReturn(Optional.of(stage2));
        when(teamRepository.findAll()).thenReturn(Arrays.asList(team1, team2));

        StageTeam st1 = new StageTeam();
        st1.setTeam(team1);
        st1.setStage(stage1);

        StageTeam st2 = new StageTeam();
        st2.setTeam(team2);
        st2.setStage(stage2);

        when(stageTeamRepository.findByTeamId(1L)).thenReturn(Collections.singletonList(st1));
        when(stageTeamRepository.findByTeamId(2L)).thenReturn(Collections.singletonList(st2));

        ResponseEntity<ApiResponse<List<TeamResponse>>> response = groupActivityController.getTeamsForAssignment(500L,
                2L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        List<TeamResponse> teams = response.getBody().getData();
        assertEquals(1, teams.size());
        assertEquals("Beta Champions", teams.get(0).getTeamName());
        assertEquals("S102", teams.get(0).getCaptainId());
    }

    @Test
    void testGetTeamsForAssignment_FallbackToStudentStageWhenNoStageTeam() {
        when(activityAssignmentRepository.findById(500L)).thenReturn(Optional.of(assignment));
        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.empty());
        when(teamRepository.findAll()).thenReturn(Arrays.asList(team1, team2));

        when(stageTeamRepository.findByTeamId(anyLong())).thenReturn(Collections.emptyList());

        // Assignment default stage is Stage 1
        ResponseEntity<ApiResponse<List<TeamResponse>>> response = groupActivityController.getTeamsForAssignment(500L,
                null);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        List<TeamResponse> teams = response.getBody().getData();
        assertEquals(1, teams.size());
        assertEquals("Alpha Warriors", teams.get(0).getTeamName());
    }
}
