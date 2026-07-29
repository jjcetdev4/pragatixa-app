package com.pragatix.modules.authentication.dto.response;

import java.util.Set;

public class UserResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private boolean active;
    private Set<String> roles;
    private Set<String> subRoles;
    private Long departmentId;
    private String departmentName;
    private Long sectionId;
    private String sectionName;
    private String section;
    private String year;

    public UserResponse() {
    }

    public UserResponse(Long id, String username, String fullName, String email, boolean active, Set<String> roles,
            Set<String> subRoles, Long departmentId, String departmentName, Long sectionId, String sectionName,
            String section, String year) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.active = active;
        this.roles = roles;
        this.subRoles = subRoles;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.sectionId = sectionId;
        this.sectionName = sectionName;
        this.section = section;
        this.year = year;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getSubRoles() {
        return subRoles;
    }

    public void setSubRoles(Set<String> subRoles) {
        this.subRoles = subRoles;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String username;
        private String fullName;
        private String email;
        private boolean active;
        private Set<String> roles;
        private Set<String> subRoles;
        private Long departmentId;
        private String departmentName;
        private Long sectionId;
        private String sectionName;
        private String section;
        private String year;

        public Builder id(Long v) {
            this.id = v;
            return this;
        }

        public Builder username(String v) {
            this.username = v;
            return this;
        }

        public Builder fullName(String v) {
            this.fullName = v;
            return this;
        }

        public Builder email(String v) {
            this.email = v;
            return this;
        }

        public Builder active(boolean v) {
            this.active = v;
            return this;
        }

        public Builder roles(Set<String> v) {
            this.roles = v;
            return this;
        }

        public Builder subRoles(Set<String> v) {
            this.subRoles = v;
            return this;
        }

        public Builder departmentId(Long v) {
            this.departmentId = v;
            return this;
        }

        public Builder departmentName(String v) {
            this.departmentName = v;
            return this;
        }

        public Builder sectionId(Long v) {
            this.sectionId = v;
            return this;
        }

        public Builder sectionName(String v) {
            this.sectionName = v;
            return this;
        }

        public Builder section(String v) {
            this.section = v;
            return this;
        }

        public Builder year(String v) {
            this.year = v;
            return this;
        }

        public UserResponse build() {
            return new UserResponse(id, username, fullName, email, active, roles, subRoles, departmentId,
                    departmentName, sectionId, sectionName, section, year);
        }
    }
}
