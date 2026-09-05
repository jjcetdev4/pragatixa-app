package jjcet.PragatiX.modules.authentication.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "test_super_secret_key_must_be_at_least_256_bits_long_for_hs256_algorithm");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 57600000L); // 16 hours
        ReflectionTestUtils.setField(jwtUtil, "studentExpiration", 57600000L); // 16 hours
    }

    @Test
    void testGenerateTokenAndExtractUsername() {
        UserDetails userDetails = new User("admin", "password", new ArrayList<>());
        String token = jwtUtil.generateToken(userDetails);

        assertNotNull(token);
        String extractedUsername = jwtUtil.extractUsername(token);
        assertEquals("admin", extractedUsername);
        assertEquals("USER", jwtUtil.extractTokenType(token));
    }

    @Test
    void testIsTokenValidWithin16Hours() {
        UserDetails userDetails = new User("teacher", "password", new ArrayList<>());
        String token = jwtUtil.generateToken(userDetails);

        assertTrue(jwtUtil.isTokenValid(token, userDetails));
    }

    @Test
    void testExpiredTokenIsRejected() {
        // Set an expired duration (-1000ms)
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        UserDetails userDetails = new User("teacher", "password", new ArrayList<>());
        String expiredToken = jwtUtil.generateToken(userDetails);

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> {
            jwtUtil.isTokenValid(expiredToken, userDetails);
        });
    }

    @Test
    void testStudentTokenWithin16Hours() {
        String studentToken = jwtUtil.generateStudentToken("test700", "test700@jjcet.ac.in");
        assertNotNull(studentToken);
        assertEquals("test700", jwtUtil.extractUsername(studentToken));
        assertEquals("STUDENT", jwtUtil.extractTokenType(studentToken));
        assertTrue(jwtUtil.isStudentTokenValid(studentToken, "test700"));
    }
}
