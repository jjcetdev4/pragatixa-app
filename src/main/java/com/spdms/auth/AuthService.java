package com.spdms.auth;

import com.spdms.dto.ApiResponse;
import com.spdms.dto.AuthResponse;
import com.spdms.dto.LoginRequest;
import com.spdms.dto.StudentLoginRequest;
import com.spdms.entity.Student;
import com.spdms.entity.User;
import com.spdms.repository.StudentRepository;
import com.spdms.repository.UserRepository;
import com.spdms.security.JwtUtil;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PRODUCTION-READY AUTHENTICATION SERVICE
 * 
 * Contains all business logic securely authenticating users.
 * Generates JWT tokens which the Frontend uses to stay logged in.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager; // Verifies hashed passwords automatically
    private final UserDetailsService userDetailsService;       // Fetches Users from database
    private final StudentRepository studentRepository;         // Fetches Students from database
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;                           // Generates Secure JWT Tokens
    private final PasswordEncoder passwordEncoder;             // Used to check raw password vs hashed password

    public AuthService(AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService,
                       StudentRepository studentRepository,
                       UserRepository userRepository,
                       JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    // ====================================================================================
    // API 1: TEACHER & ADMIN LOGIN LOGIC
    // ====================================================================================
    
    @Transactional(readOnly = true)
    public ApiResponse<AuthResponse> loginUser(LoginRequest request) {
        try {
            // STEP 1: Verify the username & password
            // This safely hashes the provided password and compares it to the database hash.
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for username: {}", request.getUsername());
            return ApiResponse.error("Invalid username or password");
        } catch (DisabledException e) {
            return ApiResponse.error("Account is disabled. Please contact admin.");
        }

        // STEP 2: Fetch the user's details and roles
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        
        // STEP 3: Generate the JWT Token (Access Token)
        String token = jwtUtil.generateToken(userDetails);

        // STEP 4: Convert roles into a simple List of Strings (e.g. ["ROLE_ADMIN"])
        List<String> roles = userDetails.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .collect(Collectors.toList());

        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        List<String> subRolesList = new ArrayList<>(user.getSubRoles());

        String userType = "USER";
        if (roles.contains("ROLE_ADMIN")) {
            userType = "ADMIN";
        } else if (roles.contains("ROLE_TEACHER")) {
            userType = "TEACHER";
        } else if (roles.contains("ROLE_TRANSPORT")) {
            userType = "TRANSPORT";
        }

        // STEP 5: Build a clean response object to send to the frontend
        AuthResponse response = AuthResponse.builder()
            .token(token)
            .type("Bearer")
            .username(userDetails.getUsername())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .roles(roles)         // Contains ROLE_TEACHER or ROLE_ADMIN
            .subRoles(subRolesList)
            .userType(userType)   // Helps frontend know this is a staff member
            .section(user.getSection())
            .year(user.getYear())
            .build();

        log.info("Teacher/Admin logged in successfully: {}", request.getUsername());
        return ApiResponse.ok("Login successful", response);
    }

    // ====================================================================================
    // API 2: STUDENT LOGIN LOGIC
    // ====================================================================================
    
    @Transactional(readOnly = true)
    public ApiResponse<AuthResponse> loginStudent(StudentLoginRequest request) {
        
        // STEP 1: Find the student in the database using their Student ID or Email
        Student student = studentRepository.findByStudentId(request.getIdentity())
            .or(() -> studentRepository.findByEmail(request.getIdentity()))
            .orElse(null);

        // Fail if student does not exist
        if (student == null) {
            log.warn("Student not found: {}", request.getIdentity());
            return ApiResponse.error("Invalid student ID or email");
        }

        // Fail if student account is inactive
        if (!student.isActive()) {
            return ApiResponse.error("Student account is inactive. Please contact admin.");
        }

        // STEP 2: Compare Passwords securely
        // We use BCrypt encoder to compare raw password against the encrypted hash in DB
        if (!passwordEncoder.matches(request.getPassword(), student.getPassword())) {
            log.warn("Wrong password for student: {}", request.getIdentity());
            return ApiResponse.error("Invalid password");
        }

        // STEP 3: Generate a lightweight Student JWT Token
        String token = jwtUtil.generateStudentToken(student.getStudentId(), student.getEmail());

        // STEP 4: Build response for frontend
        AuthResponse response = AuthResponse.builder()
            .token(token)
            .type("Bearer")
            .username(student.getStudentId())
            .fullName(student.getFullName())
            .email(student.getEmail())
            .roles(List.of("ROLE_STUDENT")) // Students inherently get the STUDENT role
            .userType("STUDENT")            // Helps frontend route to student dashboard
            .section(student.getSection())
            .year(student.getYear())
            .build();

        log.info("Student logged in successfully: {}", student.getStudentId());
        return ApiResponse.ok("Student login successful", response);
    }

    @Transactional(readOnly = true)
    public ApiResponse<AuthResponse> getUserProfile(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            List<String> rolesList = user.getRoles().stream()
                    .map(com.spdms.entity.Role::getName)
                    .collect(java.util.stream.Collectors.toList());

            String userType = "USER";
            if (rolesList.contains("ROLE_ADMIN")) {
                userType = "ADMIN";
            } else if (rolesList.contains("ROLE_TEACHER")) {
                userType = "TEACHER";
            } else if (rolesList.contains("ROLE_TRANSPORT")) {
                userType = "TRANSPORT";
            }

            AuthResponse response = AuthResponse.builder()
                    .token(null)
                    .type("Bearer")
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .roles(rolesList)
                    .subRoles(new ArrayList<>(user.getSubRoles()))
                    .userType(userType)
                    .section(user.getSection())
                    .year(user.getYear())
                    .build();
            return ApiResponse.ok("Profile loaded", response);
        }

        Student student = studentRepository.findByStudentId(username).orElse(null);
        if (student == null) {
            student = studentRepository.findByEmail(username).orElse(null);
        }
        if (student != null) {
            AuthResponse response = AuthResponse.builder()
                    .token(null)
                    .type("Bearer")
                    .username(student.getStudentId())
                    .fullName(student.getFullName())
                    .email(student.getEmail())
                    .roles(List.of("ROLE_STUDENT"))
                    .subRoles(new ArrayList<>())
                    .userType("STUDENT")
                    .section(student.getSection())
                    .year(student.getYear())
                    .build();
            return ApiResponse.ok("Profile loaded", response);
        }

        return ApiResponse.error("User not found");
    }
}
