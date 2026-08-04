package com.pragatix.modules.authentication.dto.request;

import jakarta.validation.constraints.NotBlank;

public class StudentLoginRequest {
    @NotBlank(message = "Student ID or email is required")
    private String identity;

    @NotBlank(message = "Password is required")
    private String password;

    public StudentLoginRequest() {
    }

    public StudentLoginRequest(String identity, String password) {
        this.identity = identity;
        this.password = password;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
