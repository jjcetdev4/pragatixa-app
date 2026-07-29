package com.pragatix.modules.authentication.security;

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
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L); // 1 day
        ReflectionTestUtils.setField(jwtUtil, "studentExpiration", 43200000L); // 12 hours
    }

    @Test
    void testGenerateTokenAndExtractUsername() {
        UserDetails userDetails = new User("admin", "password", new ArrayList<>());
        String token = jwtUtil.generateToken(userDetails);

        assertNotNull(token);
        String extractedUsername = jwtUtil.extractUsername(token);
        assertEquals("admin", extractedUsername);
    }

    @Test
    void testIsTokenValid() {
        UserDetails userDetails = new User("teacher", "password", new ArrayList<>());
        String token = jwtUtil.generateToken(userDetails);

        assertTrue(jwtUtil.isTokenValid(token, userDetails));
    }
}
