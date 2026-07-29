package com.pragatix.common.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 3 configuration with JWT Bearer authentication.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SPDMS – Student Performance & Discipline Management System")
                        .description("""
                                Backend REST API for SPDMS.

                                **Authentication:**
                                - Teacher/Admin: `POST /api/v1/auth/login` → use returned token as `Bearer <token>`
                                - Student: `POST /api/v1/auth/student-login` → use returned token as `Bearer <token>`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SPDMS Team")
                                .email("admin@pragatix.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter the JWT token obtained from the login endpoint")));
    }
}
