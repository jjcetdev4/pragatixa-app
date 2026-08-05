package com.pragatix.modules.authentication.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private Long sectionId;
    private String sectionName;
    private String year;
    private String department;
    private Long departmentId;
    private String phone;
    private String semester;
    private String sprNo;
    private int score;
    private int totalXp;
    private int stage;
    @JsonProperty("teamRole")
    private String teamRole;
    private String teamName;
    private String academicYear;
    private int currentStage;
    private int currentLevel;
    private int groupXP;
    private int individualXP;
    private int mustXP;
    private int rank;
    private Long teamId;
    private int memberCount;
    @JsonProperty("isCaptain")
    private boolean isCaptain;
    @JsonProperty("isViceCaptain")
    private boolean isViceCaptain;
    @JsonProperty("isMember")
    private boolean isMember;

    public AuthResponse() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getSubRoles() {
        return subRoles;
    }

    public void setSubRoles(List<String> subRoles) {
        this.subRoles = subRoles;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
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

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getSprNo() {
        return sprNo;
    }

    public void setSprNo(String sprNo) {
        this.sprNo = sprNo;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(int totalXp) {
        this.totalXp = totalXp;
    }

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public String getTeamRole() {
        return teamRole;
    }

    public void setTeamRole(String teamRole) {
        this.teamRole = teamRole;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public int getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(int currentStage) {
        this.currentStage = currentStage;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public int getGroupXP() {
        return groupXP;
    }

    public void setGroupXP(int groupXP) {
        this.groupXP = groupXP;
    }

    public int getIndividualXP() {
        return individualXP;
    }

    public void setIndividualXP(int individualXP) {
        this.individualXP = individualXP;
    }

    public int getMustXP() {
        return mustXP;
    }

    public void setMustXP(int mustXP) {
        this.mustXP = mustXP;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    @JsonProperty("isCaptain")
    public boolean isCaptain() {
        return isCaptain;
    }

    public void setCaptain(boolean isCaptain) {
        this.isCaptain = isCaptain;
    }

    @JsonProperty("isViceCaptain")
    public boolean isViceCaptain() {
        return isViceCaptain;
    }

    public void setViceCaptain(boolean isViceCaptain) {
        this.isViceCaptain = isViceCaptain;
    }

    @JsonProperty("isMember")
    public boolean isMember() {
        return isMember;
    }

    public void setMember(boolean isMember) {
        this.isMember = isMember;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final AuthResponse r = new AuthResponse();

        public Builder token(String v) {
            r.token = v;
            return this;
        }

        public Builder type(String v) {
            r.type = v;
            return this;
        }

        public Builder username(String v) {
            r.username = v;
            return this;
        }

        public Builder fullName(String v) {
            r.fullName = v;
            return this;
        }

        public Builder email(String v) {
            r.email = v;
            return this;
        }

        public Builder roles(List<String> v) {
            r.roles = v;
            return this;
        }

        public Builder subRoles(List<String> v) {
            r.subRoles = v;
            return this;
        }

        public Builder userType(String v) {
            r.userType = v;
            return this;
        }

        public Builder section(String v) {
            r.section = v;
            return this;
        }

        public Builder sectionId(Long v) {
            r.sectionId = v;
            return this;
        }

        public Builder sectionName(String v) {
            r.sectionName = v;
            return this;
        }

        public Builder year(String v) {
            r.year = v;
            return this;
        }

        public Builder department(String v) {
            r.department = v;
            return this;
        }

        public Builder departmentId(Long v) {
            r.departmentId = v;
            return this;
        }

        public Builder phone(String v) {
            r.phone = v;
            return this;
        }

        public Builder semester(String v) {
            r.semester = v;
            return this;
        }

        public Builder sprNo(String v) {
            r.sprNo = v;
            return this;
        }

        public Builder score(int v) {
            r.score = v;
            return this;
        }

        public Builder totalXp(int v) {
            r.totalXp = v;
            return this;
        }

        public Builder stage(int v) {
            r.stage = v;
            return this;
        }

        public Builder teamRole(String v) {
            r.teamRole = v;
            return this;
        }

        public Builder teamName(String v) {
            r.teamName = v;
            return this;
        }

        public Builder academicYear(String v) {
            r.academicYear = v;
            return this;
        }

        public Builder currentStage(int v) {
            r.currentStage = v;
            return this;
        }

        public Builder currentLevel(int v) {
            r.currentLevel = v;
            return this;
        }

        public Builder groupXP(int v) {
            r.groupXP = v;
            return this;
        }

        public Builder individualXP(int v) {
            r.individualXP = v;
            return this;
        }

        public Builder mustXP(int v) {
            r.mustXP = v;
            return this;
        }

        public Builder rank(int v) {
            r.rank = v;
            return this;
        }

        public Builder teamId(Long v) {
            r.teamId = v;
            return this;
        }

        public Builder memberCount(int v) {
            r.memberCount = v;
            return this;
        }

        public Builder isCaptain(boolean v) {
            r.isCaptain = v;
            return this;
        }

        public Builder isViceCaptain(boolean v) {
            r.isViceCaptain = v;
            return this;
        }

        public Builder isMember(boolean v) {
            r.isMember = v;
            return this;
        }

        public AuthResponse build() {
            return r;
        }
    }
}
