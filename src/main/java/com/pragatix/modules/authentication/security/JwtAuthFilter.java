package com.pragatix.modules.authentication.security;

import com.pragatix.modules.authentication.security.JwtUtil;
import com.pragatix.modules.authentication.security.StudentDetailsService;
import com.pragatix.modules.authentication.security.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter – intercepts every request and validates Bearer
 * tokens.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final StudentDetailsService studentDetailsService;

    public JwtAuthFilter(JwtUtil jwtUtil,
            CustomUserDetailsService userDetailsService,
            StudentDetailsService studentDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.studentDetailsService = studentDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.equals("/api/actuator/health") || path.equals("/actuator/health");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        } else if (request.getParameter("access_token") != null) {
            jwt = request.getParameter("access_token");
        }

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String username = jwtUtil.extractUsername(jwt);
            final String tokenType = jwtUtil.extractTokenType(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                if ("USER".equals(tokenType)) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtUtil.isTokenValid(jwt, userDetails)) {
                        var authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        
                        if (request.getRequestURI().contains("/api/v1/analytics")) {
                            System.out.println("\n====== FORENSIC: JWT AUTHENTICATION ======");
                            System.out.println("Requested URI: " + request.getRequestURI());
                            System.out.println("Username: " + username);
                            System.out.println("Token Type: " + tokenType);
                            System.out.println("Granted Authorities: " + userDetails.getAuthorities());
                            System.out.println("Authenticated: true");
                            System.out.println("==========================================\n");
                        }
                    }
                } else if ("STUDENT".equals(tokenType)) {
                    UserDetails userDetails = studentDetailsService.loadUserByUsername(username);
                    if (jwtUtil.isTokenValid(jwt, userDetails)) {
                        var authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            
            com.pragatix.common.response.ApiResponse<Void> apiResponse = com.pragatix.common.response.ApiResponse.error("Unauthorized. Please login.");
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            response.getWriter().write(mapper.writeValueAsString(apiResponse));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
