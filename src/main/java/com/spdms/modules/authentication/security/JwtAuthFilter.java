package com.spdms.modules.authentication.security;
import com.spdms.modules.authentication.security.JwtUtil;
import com.spdms.modules.authentication.security.StudentDetailsService;
import com.spdms.modules.authentication.security.CustomUserDetailsService;

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
 * JWT Authentication Filter – intercepts every request and validates Bearer tokens.
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
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

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
        }

        // STEP 2 - DEBUG LOGS
        if (request.getRequestURI().contains("/api/activity-requests") && SecurityContextHolder.getContext().getAuthentication() != null) {
            System.out.println("----- MASTER PROMPT STEP 2 DEBUG -----");
            System.out.println("Authenticated Username : " + SecurityContextHolder.getContext().getAuthentication().getName());
            System.out.println("Authorities : " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
            System.out.println("--------------------------------------");
        }

        filterChain.doFilter(request, response);
    }
}
