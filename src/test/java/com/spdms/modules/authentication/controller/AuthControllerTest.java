package com.pragatix.modules.authentication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pragatix.BaseIntegrationTest;
import com.pragatix.modules.authentication.dto.request.LoginRequest;
import com.pragatix.modules.authentication.dto.request.StudentLoginRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled("Requires Docker Desktop to be running properly on Windows for Testcontainers")
public class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testAdminLogin_InvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("wronguser");
        request.setPassword("wrongpass");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bad credentials"));
    }

    @Test
    public void testStudentLogin_InvalidCredentials() throws Exception {
        StudentLoginRequest request = new StudentLoginRequest();
        request.setIdentity("12345");
        request.setPassword("wrongpass");

        mockMvc.perform(post("/api/v1/auth/student-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bad credentials"));
    }
}
