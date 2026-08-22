package jjcet.PragatiX.modules.authentication.controller;

import jjcet.PragatiX.modules.authentication.service.AuthService;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.authentication.dto.response.AuthResponse;
import jjcet.PragatiX.modules.authentication.dto.request.LoginRequest;
import jjcet.PragatiX.modules.authentication.dto.request.StudentLoginRequest;
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
import jjcet.PragatiX.modules.authentication.repository.UserRepository;

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
     * @param request Contains identity (regNo or email) and password
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

    @PostMapping("/request-otp")
    @Operation(summary = "Request Login OTP", description = "Generates and sends a 4-digit OTP to the user's email if valid.")
    public ResponseEntity<ApiResponse<String>> requestOtp(
            @Valid @RequestBody jjcet.PragatiX.modules.authentication.dto.request.OtpRequest request) {
        ApiResponse<String> response = authService.requestOtp(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify Login OTP", description = "Verifies the 4-digit OTP and returns a JWT token.")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody jjcet.PragatiX.modules.authentication.dto.request.OtpVerifyRequest request) {
        ApiResponse<AuthResponse> response = authService.verifyOtp(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get Current User Profile", description = "Returns profile details of the logged in user based on the JWT token.")
    public ResponseEntity<ApiResponse<AuthResponse>> getProfile() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        ApiResponse<AuthResponse> response = authService.getUserProfile(username);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }

}
