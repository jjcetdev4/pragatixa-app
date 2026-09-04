package jjcet.PragatiX.modules.leaderboard.dto.response;

public class LeaderboardStudentResponse {
    private int rank;
    private String regNo;
    private String fullName;
    private String gender;
    private String departmentName;
    private String year;
    private String sectionName;
    private int totalXp;
    private String teamRole;

    public LeaderboardStudentResponse() {
    }

    public LeaderboardStudentResponse(int rank, String regNo, String fullName, String gender,
            String departmentName, String year, String sectionName, int totalXp, String teamRole) {
        this.rank = rank;
        this.regNo = regNo;
        this.fullName = fullName;
        this.gender = gender;
        this.departmentName = departmentName;
        this.year = year;
        this.sectionName = sectionName;
        this.totalXp = totalXp;
        this.teamRole = teamRole;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(int totalXp) {
        this.totalXp = totalXp;
    }

    public String getTeamRole() {
        return teamRole;
    }

    public void setTeamRole(String teamRole) {
        this.teamRole = teamRole;
    }
}
