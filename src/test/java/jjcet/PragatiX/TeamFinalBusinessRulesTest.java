package jjcet.PragatiX;

import jjcet.PragatiX.admin.service.LeadershipSyncService;
import jjcet.PragatiX.admin.service.TeamQueryService;
import jjcet.PragatiX.admin.service.TeamValidationService;
import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.dto.TeamResponse;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.enums.TeamRole;
import jjcet.PragatiX.modules.activity.repository.ActivityStageRepository;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.modules.student.service.TeamAssignmentService;
import jjcet.PragatiX.repository.StageTeamRepository;
import jjcet.PragatiX.repository.TeamRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class TeamFinalBusinessRulesTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private StageTeamRepository stageTeamRepository;
    @Mock
    private ActivityStageRepository activityStageRepository;
    @Mock
    private LeadershipSyncService leadershipSyncService;
    @Mock
    private AuthUtils authUtils;

    private TeamAssignmentService teamAssignmentService;
    private TeamValidationService teamValidationService;

    private Department dept;
    private Section sec;
    private ActivityStage stage1;
    private ActivityStage stage2;
    private ActivityStage stage3;

    @BeforeEach
    void setUp() {
        teamAssignmentService = new TeamAssignmentService(
                studentRepository,
                teamRepository,
                stageTeamRepository,
                leadershipSyncService
        );
        teamValidationService = new TeamValidationService(null, authUtils);

        dept = new Department();
        dept.setId(1L);
        dept.setName("CSE");

        sec = new Section();
        sec.setId(10L);
        sec.setSectionName("A");

        stage1 = new ActivityStage();
        stage1.setId(101L);
        stage1.setStageName("Ignite");
        stage1.setDisplayOrder(1);

        stage2 = new ActivityStage();
        stage2.setId(102L);
        stage2.setStageName("Stage 2");
        stage2.setDisplayOrder(2);

        stage3 = new ActivityStage();
        stage3.setId(103L);
        stage3.setStageName("Stage 3");
        stage3.setDisplayOrder(3);
    }

    // ==========================================
    // TEST 1 — Stage 1 Automatic Prevention
    // ==========================================
    @Test
    @DisplayName("Stage 1: assignTeamOnPromotion MUST NOT create teams or assign students")
    void testStage1_assignTeamOnPromotion_NeverCreatesTeam() {
        Student s = new Student();
        s.setId(1L);
        s.setRegNo("S101");
        s.setStage(1);
        s.setDepartment(dept);
        s.setSection(sec);
        s.setYear("1");

        teamAssignmentService.assignTeamOnPromotion(s, stage1);

        // Verify zero team creations and zero saves
        verify(teamRepository, never()).save(any());
        verify(stageTeamRepository, never()).save(any());
        assertNull(s.getTeam());
    }

    @Test
    @DisplayName("Stage 1: getMyTeam is purely read-only and returns 404 when no team assigned")
    void testStage1_getMyTeam_ReadOnly() {
        TeamQueryService queryService = new TeamQueryService(
                teamRepository, studentRepository, null, null,
                null, null, null, stageTeamRepository,
                teamAssignmentService, activityStageRepository
        );

        Student s = new Student();
        s.setId(1L);
        s.setRegNo("S101");
        s.setStage(1);

        when(teamRepository.findTeamByStudentId(1L)).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<TeamResponse>> res = queryService.getMyTeam(s);
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
        verify(teamRepository, never()).save(any());
    }

    // ==========================================
    // TEST 2 — Stage 1 Manual Authorization
    // ==========================================
    @Test
    @DisplayName("Stage 1 Manual Authorization: Super Admin, Admin, and CC allowed; HOD/Faculty denied")
    void testStage1_ManualAuthorization_StrictRoles() {
        User superAdmin = new User();
        when(authUtils.isSuperAdmin(superAdmin)).thenReturn(true);
        assertTrue(teamValidationService.canCreateTeam(superAdmin, null));

        User admin = new User();
        when(authUtils.isAdmin(admin)).thenReturn(true);
        assertTrue(teamValidationService.canCreateTeam(admin, null));

        User cc = new User();
        SubRole ccSubRole = new SubRole();
        ccSubRole.setName("CC");
        cc.setSubRoles(Collections.singleton(ccSubRole));
        assertTrue(teamValidationService.canCreateTeam(cc, null));

        User hod = new User();
        SubRole hodSubRole = new SubRole();
        hodSubRole.setName("HOD");
        hod.setSubRoles(Collections.singleton(hodSubRole));
        assertFalse(teamValidationService.canCreateTeam(hod, null));

        User faculty = new User();
        assertFalse(teamValidationService.canCreateTeam(faculty, null));
    }

    // ==========================================
    // TEST 3 — Stage 2 Exact Sequence and Zigzag Pattern
    // ==========================================
    @Test
    @DisplayName("Stage 2: Exact Zigzag mapping in blocks of 6 for 18 students")
    void testStage2_ExactZigzagBlocks() {
        // Setup 6 teams in mock
        List<Team> createdTeams = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Team t = new Team();
            t.setId((long) (i + 1));
            t.setName("Stage 2 - Team " + (char) ('A' + i));
            t.setDepartment(dept);
            t.setSection(sec);
            t.setYear("1");
            createdTeams.add(t);
        }

        when(teamRepository.findExactTeam(anyString(), any(), any(), any())).thenAnswer(inv -> {
            String name = inv.getArgument(0);
            return createdTeams.stream().filter(t -> t.getName().equals(name)).findFirst();
        });

        when(stageTeamRepository.findByStageIdAndTeamId(anyLong(), anyLong())).thenAnswer(inv -> {
            Long tid = inv.getArgument(1);
            StageTeam st = new StageTeam();
            st.setId(tid * 10);
            st.setTeam(createdTeams.get((int) (tid - 1)));
            st.setStage(stage2);
            return Optional.of(st);
        });

        String[] expectedOrder = {
                "S5", "S22", "S55", "S11", "S6", "S10",
                "S33", "S25", "S1", "S9", "S20", "S44",
                "S29", "S13", "S2", "S8", "S17", "S45"
        };

        // Expected Team mapping (Team A=1, B=2, C=3, D=4, E=5, F=6)
        String[] expectedTeamLetter = {
                "Team A", "Team B", "Team C", "Team D", "Team E", "Team F", // Block 1: Forward (Captains)
                "Team F", "Team E", "Team D", "Team C", "Team B", "Team A", // Block 2: Backward (Vice Captains)
                "Team A", "Team B", "Team C", "Team D", "Team E", "Team F"  // Block 3: Forward (Members)
        };

        TeamRole[] expectedRoles = {
                TeamRole.CAPTAIN, TeamRole.CAPTAIN, TeamRole.CAPTAIN, TeamRole.CAPTAIN, TeamRole.CAPTAIN, TeamRole.CAPTAIN,
                TeamRole.VICE_CAPTAIN, TeamRole.VICE_CAPTAIN, TeamRole.VICE_CAPTAIN, TeamRole.VICE_CAPTAIN, TeamRole.VICE_CAPTAIN, TeamRole.VICE_CAPTAIN,
                TeamRole.MEMBER, TeamRole.MEMBER, TeamRole.MEMBER, TeamRole.MEMBER, TeamRole.MEMBER, TeamRole.MEMBER
        };

        for (int seq = 1; seq <= 18; seq++) {
            Student s = new Student();
            s.setId((long) (seq + 100));
            s.setRegNo(expectedOrder[seq - 1]);
            s.setFullName("Student " + expectedOrder[seq - 1]);
            s.setDepartment(dept);
            s.setSection(sec);
            s.setYear("1");
            s.setStage(2);

            final int currentSeq = seq;
            when(studentRepository.findMaxPromotionOrderByClass(eq(1L), eq(10L), any(), eq(2))).thenReturn(seq - 1);
            when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

            teamAssignmentService.assignTeamOnPromotion(s, stage2);

            assertEquals(seq, s.getPromotionOrder(), "Sequence mismatch for " + s.getRegNo());
            assertNotNull(s.getTeam(), "Team must be assigned for " + s.getRegNo());
            assertTrue(s.getTeam().getName().endsWith(expectedTeamLetter[seq - 1]),
                    "Expected " + expectedTeamLetter[seq - 1] + " for " + s.getRegNo() + " (Seq " + seq + ") but got " + s.getTeam().getName());
        }
    }

    // ==========================================
    // TEST 10 & 11 — Stage 3 Lineage & Missing Lineage
    // ==========================================
    @Test
    @DisplayName("Stage 3: Inherits Stage 2 team lineage and assigns 1st=Captain, 2nd=VC, 3rd+=Member")
    void testStage3_InheritsLineageAndAssignsRoles() {
        Team stage2TeamA = new Team();
        stage2TeamA.setId(1L);
        stage2TeamA.setName("Stage 2 - Team A");
        stage2TeamA.setDepartment(dept);
        stage2TeamA.setSection(sec);
        stage2TeamA.setYear("1");

        Team stage3TeamA = new Team();
        stage3TeamA.setId(11L);
        stage3TeamA.setName("Stage 3 - Team A");
        stage3TeamA.setDepartment(dept);
        stage3TeamA.setSection(sec);
        stage3TeamA.setYear("1");

        StageTeam stage3StA = new StageTeam();
        stage3StA.setId(110L);
        stage3StA.setTeam(stage3TeamA);
        stage3StA.setStage(stage3);

        when(teamRepository.findExactTeam(eq("Stage 3 - Team A"), eq(1L), eq(10L), any())).thenReturn(Optional.of(stage3TeamA));
        when(stageTeamRepository.findByStageIdAndTeamId(103L, 11L)).thenReturn(Optional.of(stage3StA));

        // 1st student from Team A reaching Stage 3 -> S29
        Student s29 = new Student();
        s29.setId(29L);
        s29.setRegNo("S29");
        s29.setTeam(stage2TeamA);
        s29.setDepartment(dept);
        s29.setSection(sec);
        s29.setYear("1");
        s29.setStage(3);

        teamAssignmentService.assignTeamOnPromotion(s29, stage3);
        assertEquals(stage3TeamA, s29.getTeam());
        assertEquals(s29, stage3TeamA.getCaptain(), "1st student must be Captain");

        // 2nd student from Team A reaching Stage 3 -> S5
        Student s5 = new Student();
        s5.setId(5L);
        s5.setRegNo("S5");
        s5.setTeam(stage2TeamA);
        s5.setDepartment(dept);
        s5.setSection(sec);
        s5.setYear("1");
        s5.setStage(3);

        teamAssignmentService.assignTeamOnPromotion(s5, stage3);
        assertEquals(stage3TeamA, s5.getTeam());
        assertEquals(s5, stage3TeamA.getViceCaptain(), "2nd student must be Vice Captain");

        // 3rd student from Team A reaching Stage 3 -> S44
        Student s44 = new Student();
        s44.setId(44L);
        s44.setRegNo("S44");
        s44.setTeam(stage2TeamA);
        s44.setDepartment(dept);
        s44.setSection(sec);
        s44.setYear("1");
        s44.setStage(3);

        teamAssignmentService.assignTeamOnPromotion(s44, stage3);
        assertEquals(stage3TeamA, s44.getTeam());
        assertEquals(s29, stage3TeamA.getCaptain(), "Captain remains S29");
        assertEquals(s5, stage3TeamA.getViceCaptain(), "Vice Captain remains S5");
    }

    @Test
    @DisplayName("Stage 3: Missing Stage 2 lineage leaves student unassigned without fallback")
    void testStage3_MissingLineage_LeavesUnassigned() {
        Student sOrphan = new Student();
        sOrphan.setId(999L);
        sOrphan.setRegNo("S_ORPHAN");
        sOrphan.setTeam(null); // No Stage 2 lineage!
        sOrphan.setDepartment(dept);
        sOrphan.setSection(sec);
        sOrphan.setYear("1");
        sOrphan.setStage(3);

        teamAssignmentService.assignTeamOnPromotion(sOrphan, stage3);

        assertNull(sOrphan.getTeam(), "Student must remain unassigned if Stage 2 lineage is missing");
        verify(teamRepository, never()).save(any());
    }
}
