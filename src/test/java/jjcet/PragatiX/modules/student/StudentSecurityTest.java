package jjcet.PragatiX.modules.student;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import jjcet.PragatiX.modules.authentication.security.StudentAuthResolver;
import jjcet.PragatiX.modules.leaderboard.dto.response.LeaderboardStudentResponse;
import jjcet.PragatiX.modules.leaderboard.service.LeaderboardService;
import jjcet.PragatiX.modules.student.controller.StudentController;
import jjcet.PragatiX.modules.student.dto.response.StudentResponse;
import jjcet.PragatiX.modules.student.dto.response.StudentSelfResponse;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.modules.student.service.StudentMapper;
import jjcet.PragatiX.modules.student.service.StudentQueryService;
import jjcet.PragatiX.modules.student.service.StudentService;
import jjcet.PragatiX.repository.DepartmentRepository;
import jjcet.PragatiX.repository.SectionRepository;
import jjcet.PragatiX.repository.StudentGuardianRepository;
import jjcet.PragatiX.repository.YearRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StudentSecurityTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private YearRepository yearRepository;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private StudentGuardianRepository studentGuardianRepository;
    @Mock
    private AuthUtils authUtils;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private StudentAuthResolver studentAuthResolver;
    @Mock
    private StudentService studentService;

    private StudentQueryService studentQueryService;
    private LeaderboardService leaderboardService;

    private Student studentA;
    private Year year2;
    private Department cseDept;

    @BeforeEach
    void setUp() {
        studentQueryService = new StudentQueryService(
                studentRepository, userRepository, yearRepository, studentMapper,
                studentGuardianRepository, authUtils, departmentRepository,
                sectionRepository, null
        );

        leaderboardService = new LeaderboardService(
                studentRepository, userRepository, yearRepository, departmentRepository,
                sectionRepository, studentMapper, authUtils, studentAuthResolver
        );

        year2 = new Year();
        year2.setId(2L);
        year2.setYearNo((byte) 2);
        year2.setYearName("Second Year");

        cseDept = new Department();
        cseDept.setId(10L);
        cseDept.setName("Computer Science & Engineering");

        studentA = new Student();
        studentA.setId(100L);
        studentA.setRegNo("24CS001");
        studentA.setFullName("Student Alice");
        studentA.setEmail("alice@college.edu");
        studentA.setPhoneNo("9876543210");
        studentA.setYearRef(year2);
        studentA.setYear("2");
        studentA.setDepartment(cseDept);
        studentA.setActive(true);
        studentA.setTotalXp(500);
        studentA.setScore(90);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsStudent(String username) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void authenticateAsAdmin(String username) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void authenticateAsSuperAdmin(String username) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // TEST 1: Student role calling getAllStudents in StudentQueryService throws AccessDeniedException
    @Test
    void testStudent_GetAllStudents_ThrowsAccessDenied() {
        authenticateAsStudent("alice");

        assertThrows(AccessDeniedException.class, () -> {
            studentQueryService.getAllStudents(0, 25, "fullName", null, null, null, null);
        });

        verify(studentRepository, never()).findAll();
        verify(studentRepository, never()).findByFilters(any(), any(), any(), any(), any(), any(), any());
    }

    // TEST 2: Student role calling getAllStudents with size=1000 throws AccessDeniedException
    @Test
    void testStudent_GetAllStudents_Size1000_ThrowsAccessDenied() {
        authenticateAsStudent("alice");

        assertThrows(AccessDeniedException.class, () -> {
            studentQueryService.getAllStudents(0, 1000, "fullName", null, null, null, null);
        });
    }

    // TEST 3: Student role calling searchStudents throws AccessDeniedException
    @Test
    void testStudent_SearchStudents_ThrowsAccessDenied() {
        authenticateAsStudent("alice");

        assertThrows(AccessDeniedException.class, () -> {
            studentQueryService.searchStudents("Bob", 0, 25, false);
        });
    }

    // TEST 4: Student role calling getStudentById throws AccessDeniedException
    @Test
    void testStudent_GetStudentById_ThrowsAccessDenied() {
        authenticateAsStudent("alice");

        assertThrows(AccessDeniedException.class, () -> {
            studentQueryService.getStudentById(200L); // attempting to view Student B
        });
    }

    // TEST 5: Student viewing own profile via getStudentSelfProfile
    @Test
    void testStudent_GetStudentSelfProfile_ReturnsOnlyStudentA() {
        authenticateAsStudent("alice");

        when(studentGuardianRepository.findByStudentId(100L)).thenReturn(Optional.empty());
        when(studentMapper.toSelfResponse(eq(studentA), any())).thenAnswer(inv -> {
            return StudentSelfResponse.builder()
                    .id(studentA.getId())
                    .regNo(studentA.getRegNo())
                    .fullName(studentA.getFullName())
                    .email(studentA.getEmail())
                    .phone(studentA.getPhoneNo())
                    .build();
        });

        ApiResponse<StudentSelfResponse> response = studentQueryService.getStudentSelfProfile(studentA);
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("24CS001", response.getData().getRegNo());
        assertEquals(studentA.getFullName(), response.getData().getFullName());
        assertEquals("alice@college.edu", response.getData().getEmail());
    }

    // TEST 6: Student calling Leaderboard is strictly scoped to Student A's year
    @Test
    void testStudent_GetLeaderboard_AutoScopedToStudentYear() {
        authenticateAsStudent("alice");

        when(studentAuthResolver.getLoggedInStudent()).thenReturn(studentA);
        when(studentRepository.findByYearRefId(2L)).thenReturn(List.of(studentA));
        when(studentMapper.toLeaderboardResponse(eq(studentA), eq(1))).thenAnswer(inv -> {
            LeaderboardStudentResponse r = new LeaderboardStudentResponse();
            r.setRank(1);
            r.setRegNo(studentA.getRegNo());
            r.setFullName(studentA.getFullName());
            r.setTotalXp(studentA.getTotalXp());
            r.setYear("Second Year");
            return r;
        });

        ApiResponse<List<LeaderboardStudentResponse>> response = leaderboardService.getLeaderboard(null, null, null);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals("Second Year", response.getData().get(0).getYear());
        verify(studentRepository).findByYearRefId(2L);
        verify(studentRepository, never()).findAll();
    }

    // TEST 7: Student attempting to tamper yearId (e.g. ?yearId=3) is overridden to their own year (2)
    @Test
    void testStudent_GetLeaderboard_TamperedYearId_EnforcesStudentYear() {
        authenticateAsStudent("alice");

        when(studentAuthResolver.getLoggedInStudent()).thenReturn(studentA); // Year 2
        when(studentRepository.findByYearRefId(2L)).thenReturn(List.of(studentA));
        when(studentMapper.toLeaderboardResponse(any(), anyInt())).thenReturn(new LeaderboardStudentResponse());

        // Attacker passes yearId=3
        leaderboardService.getLeaderboard(3L, null, null);

        // Verify that Year 2 was queried, NOT Year 3!
        verify(studentRepository).findByYearRefId(2L);
        verify(studentRepository, never()).findByYearRefId(3L);
    }

    // TEST 8: LeaderboardStudentResponse DTO strictly contains no phone, email, address, DOB, guardian
    @Test
    void testLeaderboardResponse_ExcludesSensitivePII() {
        LeaderboardStudentResponse response = new LeaderboardStudentResponse(
                1, "24CS001", "Student Alice", "Female",
                "CSE", "Second Year", "A", 500, "MEMBER"
        );

        // Check that only non-sensitive leaderboard fields are accessible
        assertEquals(1, response.getRank());
        assertEquals("24CS001", response.getRegNo());
        assertEquals("Student Alice", response.getFullName());
        assertEquals(500, response.getTotalXp());

        // Verify class has NO getters for sensitive fields
        assertFalse(hasMethod(LeaderboardStudentResponse.class, "getPhone"));
        assertFalse(hasMethod(LeaderboardStudentResponse.class, "getEmail"));
        assertFalse(hasMethod(LeaderboardStudentResponse.class, "getAddress"));
        assertFalse(hasMethod(LeaderboardStudentResponse.class, "getDateOfBirth"));
        assertFalse(hasMethod(LeaderboardStudentResponse.class, "getGuardian"));
        assertFalse(hasMethod(LeaderboardStudentResponse.class, "getGuardianPhone"));
    }

    // TEST 9: Pagination limit clamps to 100
    @Test
    void testPagination_ClampsTo100() {
        authenticateAsSuperAdmin("superadmin");

        User superUser = new User();
        superUser.setUsername("superadmin");
        when(userRepository.findByUsername("superadmin")).thenReturn(Optional.of(superUser));
        when(authUtils.isSuperAdmin(superUser)).thenReturn(true);
        when(studentRepository.findByFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable p = inv.getArgument(6);
                    assertEquals(100, p.getPageSize(), "Requested size 5000 must be clamped to 100");
                    return Page.empty(p);
                });

        studentQueryService.getAllStudents(0, 5000, "fullName", null, null, null, null);
    }

    // TEST 10: Admin access is scoped to assigned year
    @Test
    void testAdmin_GetAllStudents_ScopedToAssignedYear() {
        authenticateAsAdmin("admin_year2");

        User adminUser = new User();
        adminUser.setUsername("admin_year2");
        adminUser.setAssignedYear(year2);
        when(userRepository.findByUsername("admin_year2")).thenReturn(Optional.of(adminUser));
        when(authUtils.isSuperAdmin(adminUser)).thenReturn(false);
        when(authUtils.isAdmin(adminUser)).thenReturn(true);
        when(studentRepository.findByFiltersWithYearRef(any(), eq(2L), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(studentA)));

        ApiResponse<Page<StudentResponse>> res = studentQueryService.getAllStudents(0, 25, "fullName", null, null, null, null);

        assertNotNull(res);
        assertTrue(res.isSuccess());
        verify(studentRepository).findByFiltersWithYearRef(any(), eq(2L), any(), any(), any(Pageable.class));
    }

    // TEST 11: JwtAuthFilter ignores access_token query param and requires Authorization Bearer
    @Test
    void testJwtAuthFilter_QueryParamAccessToken_IsNotAuthenticated() throws Exception {
        jjcet.PragatiX.modules.authentication.security.JwtUtil jwtUtil = mock(jjcet.PragatiX.modules.authentication.security.JwtUtil.class);
        jjcet.PragatiX.modules.authentication.security.CustomUserDetailsService userDetailsService = mock(jjcet.PragatiX.modules.authentication.security.CustomUserDetailsService.class);
        jjcet.PragatiX.modules.authentication.security.StudentDetailsService studentDetailsService = mock(jjcet.PragatiX.modules.authentication.security.StudentDetailsService.class);

        jjcet.PragatiX.modules.authentication.security.JwtAuthFilter filter = new jjcet.PragatiX.modules.authentication.security.JwtAuthFilter(
                jwtUtil, userDetailsService, studentDetailsService
        );

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRequestURI("/api/v1/students");
        request.setParameter("access_token", "tampered.jwt.token");
        // No Authorization header

        org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();
        org.springframework.mock.web.MockFilterChain filterChain = new org.springframework.mock.web.MockFilterChain();

        filter.doFilter(request, response, filterChain);

        // Security context should remain empty
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Query parameter access_token must not authenticate the request");
        verifyNoInteractions(jwtUtil);
        verifyNoInteractions(userDetailsService);
        verifyNoInteractions(studentDetailsService);
    }

    private boolean hasMethod(Class<?> clazz, String methodName) {
        try {
            clazz.getMethod(methodName);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
