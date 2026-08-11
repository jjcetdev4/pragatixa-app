package com.pragatix.modules.authentication.security;

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
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final List<String> allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
            CustomUserDetailsService customUserDetailsService,
            StudentDetailsService studentDetailsService,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
            CustomAccessDeniedHandler customAccessDeniedHandler,
            @Value("${cors.allowed-origins:}") String allowedOrigins) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.customUserDetailsService = customUserDetailsService;
        this.studentDetailsService = studentDetailsService;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
        this.allowedOrigins = StringUtils.hasText(allowedOrigins)
                ? Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toList())
                : List.of();
    }

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/api/swagger-ui/**",
            "/api/swagger-ui.html",
            "/api/api-docs/**",
            "/api/actuator/health",
            "/actuator/health",
            "/api/test-bug/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/students/me").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/students").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/students/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/students/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/students/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                        .requestMatchers("/api/activity-requests/**")
                        .hasAnyRole("TEACHER", "CLASS_COORDINATOR", "ADMIN", "STUDENT")
                        .requestMatchers("/api/v1/analytics/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN", "ROLE_SUPERADMIN", "ADMIN", "SUPER_ADMIN", "SUPERADMIN")
                        .requestMatchers("/api/v1/hod/**").hasAnyRole("HOD", "ADMIN", "SUPERADMIN", "SUPER_ADMIN", "TEACHER")
                        .requestMatchers("/api/v1/cc/**").hasAnyRole("CLASS_COORDINATOR", "CC", "ADMIN", "SUPERADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/v1/profile/**").authenticated()
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPERADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPERADMIN", "SUPER_ADMIN", "TEACHER")
                .requestMatchers("/api/penalties/**").hasAnyRole("TEACHER", "HOD", "ADMIN", "SUPERADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/teacher/**").hasAnyRole("TEACHER", "ADMIN", "SUPERADMIN", "SUPER_ADMIN")
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
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> originsList = !allowedOrigins.isEmpty()
                ? allowedOrigins
                : List.of("https://pragatix.in", "http://localhost:8080", "http://localhost:5173");
        if (!originsList.contains("https://pragatix.in")) {
            originsList = new java.util.ArrayList<>(originsList);
            originsList.add("https://pragatix.in");
        }
        if (!originsList.contains("http://localhost:*")) {
            originsList = new java.util.ArrayList<>(originsList);
            originsList.add("http://localhost:*");
        }

        configuration.setAllowedOriginPatterns(originsList);

        List<String> methods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        configuration.setAllowedMethods(methods);

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
            return source.getCorsConfiguration(request);
        };
    }
}