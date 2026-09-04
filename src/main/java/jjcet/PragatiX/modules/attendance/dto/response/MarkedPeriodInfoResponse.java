package jjcet.PragatiX.modules.attendance.dto.response;

public class MarkedPeriodInfoResponse {
    private Integer period;
    private boolean canViewHistory;
    private boolean isMarkedByMe;
    private String markedByFacultyName;
    private String markedByFacultyDepartment;
    private String markedAt;

    public MarkedPeriodInfoResponse() {
    }

    public MarkedPeriodInfoResponse(Integer period, boolean canViewHistory, boolean isMarkedByMe, String markedByFacultyName, String markedByFacultyDepartment, String markedAt) {
        this.period = period;
        this.canViewHistory = canViewHistory;
        this.isMarkedByMe = isMarkedByMe;
        this.markedByFacultyName = markedByFacultyName;
        this.markedByFacultyDepartment = markedByFacultyDepartment;
        this.markedAt = markedAt;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public boolean isCanViewHistory() {
        return canViewHistory;
    }

    public void setCanViewHistory(boolean canViewHistory) {
        this.canViewHistory = canViewHistory;
    }

    public boolean isMarkedByMe() {
        return isMarkedByMe;
    }

    public void setMarkedByMe(boolean isMarkedByMe) {
        this.isMarkedByMe = isMarkedByMe;
    }

    public String getMarkedByFacultyName() {
        return markedByFacultyName;
    }

    public void setMarkedByFacultyName(String markedByFacultyName) {
        this.markedByFacultyName = markedByFacultyName;
    }

    public String getMarkedByFacultyDepartment() {
        return markedByFacultyDepartment;
    }

    public void setMarkedByFacultyDepartment(String markedByFacultyDepartment) {
        this.markedByFacultyDepartment = markedByFacultyDepartment;
    }

    public String getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(String markedAt) {
        this.markedAt = markedAt;
    }
}
