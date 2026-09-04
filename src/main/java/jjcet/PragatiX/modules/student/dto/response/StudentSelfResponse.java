package jjcet.PragatiX.modules.student.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import jjcet.PragatiX.dto.StreakResponse;
import jjcet.PragatiX.modules.student.dto.request.GuardianDTO;

public class StudentSelfResponse {
    private Long id;
    private String regNo;
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private Long genderId;
    private LocalDate dateOfBirth;
    private String address;
    private Long departmentId;
    private String departmentName;
    private String semester;
    private Long semesterId;
    private String year;
    private Long yearId;
    private String section;
    private Long sectionId;
    private String sectionName;
    private boolean active;
    private LocalDateTime createdAt;
    private String sprNo;
    private int score;
    private int totalXp;
    private int currentXp;
    private int mustXp;
    private int individualXp;
    private int groupXp;
    private Long teamId;
    private String teamName;
    private String teamRole;
    private GuardianDTO guardian;
    private int currentStage = 1;
    private int currentStreak = 0;
    private List<StreakResponse> streaks;

    public StudentSelfResponse() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Long getGenderId() {
        return genderId;
    }

    public void setGenderId(Long genderId) {
        this.genderId = genderId;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public Long getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Long semesterId) {
        this.semesterId = semesterId;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public Long getYearId() {
        return yearId;
    }

    public void setYearId(Long yearId) {
        this.yearId = yearId;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public int getCurrentXp() {
        return currentXp;
    }

    public void setCurrentXp(int currentXp) {
        this.currentXp = currentXp;
    }

    public int getMustXp() {
        return mustXp;
    }

    public void setMustXp(int mustXp) {
        this.mustXp = mustXp;
    }

    public int getIndividualXp() {
        return individualXp;
    }

    public void setIndividualXp(int individualXp) {
        this.individualXp = individualXp;
    }

    public int getGroupXp() {
        return groupXp;
    }

    public void setGroupXp(int groupXp) {
        this.groupXp = groupXp;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamRole() {
        return teamRole;
    }

    public void setTeamRole(String teamRole) {
        this.teamRole = teamRole;
    }

    public GuardianDTO getGuardian() {
        return guardian;
    }

    public void setGuardian(GuardianDTO guardian) {
        this.guardian = guardian;
    }

    public int getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(int currentStage) {
        this.currentStage = currentStage;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public List<StreakResponse> getStreaks() {
        return streaks;
    }

    public void setStreaks(List<StreakResponse> streaks) {
        this.streaks = streaks;
    }

    public static class Builder {
        private final StudentSelfResponse response = new StudentSelfResponse();

        public Builder id(Long id) { response.setId(id); return this; }
        public Builder regNo(String regNo) { response.setRegNo(regNo); return this; }
        public Builder fullName(String fullName) { response.setFullName(fullName); return this; }
        public Builder email(String email) { response.setEmail(email); return this; }
        public Builder phone(String phone) { response.setPhone(phone); return this; }
        public Builder gender(String gender) { response.setGender(gender); return this; }
        public Builder genderId(Long genderId) { response.setGenderId(genderId); return this; }
        public Builder dateOfBirth(LocalDate dateOfBirth) { response.setDateOfBirth(dateOfBirth); return this; }
        public Builder address(String address) { response.setAddress(address); return this; }
        public Builder departmentId(Long departmentId) { response.setDepartmentId(departmentId); return this; }
        public Builder departmentName(String departmentName) { response.setDepartmentName(departmentName); return this; }
        public Builder semester(String semester) { response.setSemester(semester); return this; }
        public Builder semesterId(Long semesterId) { response.setSemesterId(semesterId); return this; }
        public Builder year(String year) { response.setYear(year); return this; }
        public Builder yearId(Long yearId) { response.setYearId(yearId); return this; }
        public Builder section(String section) { response.setSection(section); return this; }
        public Builder sectionId(Long sectionId) { response.setSectionId(sectionId); return this; }
        public Builder sectionName(String sectionName) { response.setSectionName(sectionName); return this; }
        public Builder active(boolean active) { response.setActive(active); return this; }
        public Builder createdAt(LocalDateTime createdAt) { response.setCreatedAt(createdAt); return this; }
        public Builder sprNo(String sprNo) { response.setSprNo(sprNo); return this; }
        public Builder score(int score) { response.setScore(score); return this; }
        public Builder totalXp(int totalXp) { response.setTotalXp(totalXp); return this; }
        public Builder currentXp(int currentXp) { response.setCurrentXp(currentXp); return this; }
        public Builder mustXp(int mustXp) { response.setMustXp(mustXp); return this; }
        public Builder individualXp(int individualXp) { response.setIndividualXp(individualXp); return this; }
        public Builder groupXp(int groupXp) { response.setGroupXp(groupXp); return this; }
        public Builder teamId(Long teamId) { response.setTeamId(teamId); return this; }
        public Builder teamName(String teamName) { response.setTeamName(teamName); return this; }
        public Builder teamRole(String teamRole) { response.setTeamRole(teamRole); return this; }
        public Builder guardian(GuardianDTO guardian) { response.setGuardian(guardian); return this; }
        public Builder currentStage(int currentStage) { response.setCurrentStage(currentStage); return this; }
        public Builder currentStreak(int currentStreak) { response.setCurrentStreak(currentStreak); return this; }
        public Builder streaks(List<StreakResponse> streaks) { response.setStreaks(streaks); return this; }

        public StudentSelfResponse build() {
            return response;
        }
    }
}
