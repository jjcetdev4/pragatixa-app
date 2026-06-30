package com.spdms.auth;

import com.spdms.dto.ApiResponse;
import com.spdms.dto.AuthResponse;
import com.spdms.dto.LoginRequest;
import com.spdms.dto.StudentLoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * PRODUCTION-READY AUTHENTICATION CONTROLLER
 * 
 * This class handles all incoming HTTP requests for login.
 * It is separated from business logic (which lives in AuthService) to follow the Single Responsibility Principle.
 */
import com.spdms.repository.UserRepository;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login endpoints for teachers, admins, and students")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    /**
     * API 1: Teacher & Admin Login
     * Endpoint: POST /api/v1/auth/login
     * 
     * @param request Contains username and password (validated by @Valid)
     * @return 200 OK with JWT token if successful, or 401 Unauthorized if failed
     */
    @PostMapping("/login")
    @Operation(summary = "Teacher / Admin Login", description = "Authenticate username & password. Returns a JWT token.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        
        // Step 1: Pass the raw request to the service layer for processing
        ApiResponse<AuthResponse> response = authService.loginUser(request);
        
        // Step 2: Return HTTP 200 OK if success, else return HTTP 401 Unauthorized
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }

    /**
     * API 2: Student Login
     * Endpoint: POST /api/v1/auth/student-login
     * 
     * @param request Contains identity (studentId or email) and password
     * @return 200 OK with JWT token if successful, or 401 Unauthorized if failed
     */
    @PostMapping("/student-login")
    @Operation(summary = "Student Login", description = "Authenticate using Student ID (or email) & password. Returns a JWT token.")
    public ResponseEntity<ApiResponse<AuthResponse>> studentLogin(@Valid @RequestBody StudentLoginRequest request) {
        
        // Step 1: Pass the raw request to the service layer for processing
        ApiResponse<AuthResponse> response = authService.loginStudent(request);
        
        // Step 2: Return HTTP 200 OK if success, else return HTTP 401 Unauthorized
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get Current User Profile", description = "Returns profile details of the logged in user based on the JWT token.")
    public ResponseEntity<ApiResponse<AuthResponse>> getProfile() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        ApiResponse<AuthResponse> response = authService.getUserProfile(username);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }

    @GetMapping("/test-users")
    public ResponseEntity<?> testUsers() {
        return ResponseEntity.ok(userRepository.findAll().stream().map(u -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("username", u.getUsername());
            map.put("fullName", u.getFullName());
            map.put("roles", u.getRoles().stream().map(com.spdms.entity.Role::getName).collect(java.util.stream.Collectors.toList()));
            map.put("subRoles", u.getSubRoles());
            map.put("section", u.getSection());
            return map;
        }).collect(java.util.stream.Collectors.toList()));
    }
}
