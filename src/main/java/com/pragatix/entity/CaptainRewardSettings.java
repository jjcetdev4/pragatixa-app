package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalTime;
import com.pragatix.enums.AcademicYear;
import java.time.LocalDateTime;

@Entity
@Table(name = "captain_reward_settings")
public class CaptainRewardSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "academic_year", unique = true, nullable = false)
    private AcademicYear academicYear;

    @Column(name = "engine_enabled")
    private Boolean engineEnabled = false;

    @Column(name = "captain_xp")
    private Integer captainXp = 0;

    @Column(name = "vice_captain_xp")
    private Integer viceCaptainXp = 0;

    @Column(name = "execution_time")
    private LocalTime executionTime;

    @Column(name = "last_execution_date")
    private LocalDateTime lastExecutionDate;

    public CaptainRewardSettings() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
