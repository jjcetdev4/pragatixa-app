package com.pragatix.modules.attendance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;
import com.pragatix.enums.AcademicYear;
import java.time.LocalDateTime;

public class CaptainRewardSettingsDTO {

    private AcademicYear academicYear;
    private Boolean engineEnabled;
    private Integer captainXp;
    private Integer viceCaptainXp;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime executionTime;

    private LocalDateTime lastExecutionDate;

    public CaptainRewardSettingsDTO() {
    }

    public AcademicYear getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(AcademicYear academicYear) {
        this.academicYear = academicYear;
    }

    public Boolean getEngineEnabled() {
        return engineEnabled;
    }

    public void setEngineEnabled(Boolean engineEnabled) {
        this.engineEnabled = engineEnabled;
    }

    public Integer getCaptainXp() {
        return captainXp;
    }

    public void setCaptainXp(Integer captainXp) {
        this.captainXp = captainXp;
    }

    public Integer getViceCaptainXp() {
        return viceCaptainXp;
    }

    public void setViceCaptainXp(Integer viceCaptainXp) {
        this.viceCaptainXp = viceCaptainXp;
    }

    public LocalTime getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(LocalTime executionTime) {
        this.executionTime = executionTime;
    }

    public LocalDateTime getLastExecutionDate() {
        return lastExecutionDate;
    }

    public void setLastExecutionDate(LocalDateTime lastExecutionDate) {
        this.lastExecutionDate = lastExecutionDate;
    }
}
