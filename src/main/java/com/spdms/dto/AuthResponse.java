package com.spdms.dto;

import java.util.List;

public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private String username;
    private String fullName;
    private String email;
    private List<String> roles;
    private List<String> subRoles;
    private String userType;
    private String section;
    private String year;

    public AuthResponse() {}

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public List<String> getSubRoles() { return subRoles; }
    public void setSubRoles(List<String> subRoles) { this.subRoles = subRoles; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final AuthResponse r = new AuthResponse();
        public Builder token(String v) { r.token = v; return this; }
        public Builder type(String v) { r.type = v; return this; }
        public Builder username(String v) { r.username = v; return this; }
        public Builder fullName(String v) { r.fullName = v; return this; }
        public Builder email(String v) { r.email = v; return this; }
        public Builder roles(List<String> v) { r.roles = v; return this; }
        public Builder subRoles(List<String> v) { r.subRoles = v; return this; }
        public Builder userType(String v) { r.userType = v; return this; }
        public Builder section(String v) { r.section = v; return this; }
        public Builder year(String v) { r.year = v; return this; }
        public AuthResponse build() { return r; }
    }
}
