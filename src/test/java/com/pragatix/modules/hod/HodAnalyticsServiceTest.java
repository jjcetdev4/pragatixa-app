package jjcet.PragatiX.modules.hod;

import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.hod.dto.HodDashboardResponse;
import jjcet.PragatiX.modules.hod.service.HodAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class HodAnalyticsServiceTest {

    @Autowired
    private HodAnalyticsService hodAnalyticsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        User user = userRepository.findByUsername("sharu").orElse(null);
        if (user != null) {
            org.springframework.security.core.userdetails.UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    Collections.singletonList(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TEACHER")));
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null,
                    userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
    }

    @Test
    void testGetDashboardDataAllYears() {
        HodDashboardResponse response = hodAnalyticsService.getDashboardData("All Years");
        assertNotNull(response);
        assertNotNull(response.getDepartmentInfo());
        assertNotNull(response.getOverview());
        assertNotNull(response.getAttendance());
        assertNotNull(response.getXp());
        assertNotNull(response.getDiscipline());
        assertNotNull(response.getLeaderboard());
        assertNotNull(response.getSectionComparison());
        assertNotNull(response.getYearComparison());

        System.out.println("=== TEST SUCCESS: HOD Analytics All Years ===");
        System.out.println("Dept: " + response.getDepartmentInfo().getName());
        System.out.println("Total Students: " + response.getOverview().getTotalStudents());
        System.out.println("Total Teachers: " + response.getOverview().getTotalTeachers());
        System.out.println("Total Sections: " + response.getOverview().getTotalSections());
        System.out.println("Avg XP: " + response.getOverview().getAverageXp());
        System.out.println("Available Years: " + response.getAvailableYears());
    }

    @Test
    void testGetDashboardDataSingleYear() {
        HodDashboardResponse response = hodAnalyticsService.getDashboardData("2026");
        assertNotNull(response);
        assertEquals("2026", response.getSelectedYear());
        assertNotNull(response.getOverview());
        System.out.println("=== TEST SUCCESS: HOD Analytics Year 2026 ===");
        System.out.println("Selected Year: " + response.getSelectedYear());
        System.out.println("Total Students (2026): " + response.getOverview().getTotalStudents());
    }

    @Test
    void testDashboardEndpointHttp200() throws Exception {
        mockMvc.perform(get("/api/v1/hod/analytics/dashboard")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.departmentInfo.name").value("Cyber Security"))
                .andExpect(jsonPath("$.data.overview.totalStudents").isNumber())
                .andExpect(jsonPath("$.data.overview.totalSections").isNumber());
        System.out.println("=== TEST SUCCESS: GET /api/v1/hod/analytics/dashboard HTTP 200 OK ===");
    }
}
