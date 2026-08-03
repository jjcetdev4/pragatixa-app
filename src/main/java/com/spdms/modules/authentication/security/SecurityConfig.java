package com.spdms.modules.authentication.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security configuration – JWT stateless, CORS enabled, Swagger
 * whitelisted.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final StudentDetailsService studentDetailsService;
    private final List<String> allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          CustomUserDetailsService customUserDetailsService,
                          StudentDetailsService studentDetailsService,
                          @Value("${cors.allowed-origins:}") String allowedOrigins) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.customUserDetailsService = customUserDetailsService;
        this.studentDetailsService = studentDetailsService;
        this.allowedOrigins = StringUtils.hasText(allowedOrigins)
                ? Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toList())
                : List.of();
    }

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/swagger-ui/**",
            "/api/swagger-ui/**",
            "/swagger-ui.html",
            "/api/swagger-ui.html",
            "/api-docs/**",
            "/api/api-docs/**",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/students/me").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/students").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/students/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/students/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/students/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                        .requestMatchers("/api/activity-requests/**").hasAnyRole("TEACHER", "CLASS_COORDINATOR", "ADMIN", "STUDENT")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider staffAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public DaoAuthenticationProvider studentAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(studentDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(
                staffAuthenticationProvider(),
                studentAuthenticationProvider());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        System.out.println("----------------------------------------");
        System.out.println("Configured CORS");
        System.out.println("----------------------------------------");
        System.out.println("Allowed Origins: " + allowedOrigins);

        CorsConfiguration configuration = new CorsConfiguration();
        List<String> originsList = !allowedOrigins.isEmpty()
                ? allowedOrigins
                : List.of("https://pragatix.in", "http://localhost:5173", "http://localhost:3000");
        if (!originsList.contains("https://pragatix.in")) {
            originsList = new java.util.ArrayList<>(originsList);
            originsList.add("https://pragatix.in");
        }
        
        configuration.setAllowedOriginPatterns(originsList);
        System.out.println("Allowed Origin Patterns: " + originsList);

        List<String> methods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        configuration.setAllowedMethods(methods);
        System.out.println("Allowed Methods: " + methods);

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setMaxAge(3600L);

        boolean hasWildcard = originsList.contains("*");
        boolean allowCredentials = !hasWildcard;
        configuration.setAllowCredentials(allowCredentials);
        System.out.println("Allow Credentials: " + allowCredentials);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return request -> {
            System.out.println("----------------------------------------");
            System.out.println("Incoming CORS Request");
            System.out.println("----------------------------------------");
            System.out.println("Origin: " + request.getHeader("Origin"));
            System.out.println("Method: " + request.getMethod());
            System.out.println("URI: " + request.getRequestURI());
            System.out.println("Host: " + request.getHeader("Host"));
            System.out.println("X-Forwarded-Host: " + request.getHeader("X-Forwarded-Host"));
            System.out.println("X-Forwarded-Proto: " + request.getHeader("X-Forwarded-Proto"));
            System.out.println("X-Forwarded-For: " + request.getHeader("X-Forwarded-For"));
            System.out.println("Access-Control-Request-Method: " + request.getHeader("Access-Control-Request-Method"));
            System.out.println("Access-Control-Request-Headers: " + request.getHeader("Access-Control-Request-Headers"));
            System.out.println("Referer: " + request.getHeader("Referer"));
            System.out.println("User-Agent: " + request.getHeader("User-Agent"));
            System.out.println("Remote Address: " + request.getRemoteAddr());
            System.out.println("----------------------------------------");
    return source.getCorsConfiguration(request);
        };
    }
}
