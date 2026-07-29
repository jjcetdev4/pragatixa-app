package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "streaks")
public class Streak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "reg_no", nullable = false)
    private String regNo;

    @Column(name = "streak_type", nullable = false, length = 50)
    private String streakType; // MONDAY_JOURNAL, ENGLISH_DIARY, C_CODING, PYTHON_CODING, ATTIRE, PUNCTUALITY,
                               // LIBRARY, COE_LAB

    @Column(name = "current_streak", nullable = false)
    private int currentStreak = 0;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "is_broken", nullable = false)
    private boolean isBroken = false;

    @Column(name = "penalty_per_break", nullable = false)
    private int penaltyPerBreak;

    public Streak() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getStreakType() {
        return streakType;
    }

    public void setStreakType(String streakType) {
        this.streakType = streakType;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public boolean isBroken() {
        return isBroken;
    }

    public void setBroken(boolean broken) {
        isBroken = broken;
    }

    public int getPenaltyPerBreak() {
        return penaltyPerBreak;
    }

    public void setPenaltyPerBreak(int penaltyPerBreak) {
        this.penaltyPerBreak = penaltyPerBreak;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Streak s = new Streak();

        public Builder student(Student v) {
            s.student = v;
            return this;
        }

        public Builder regNo(String v) {
            s.regNo = v;
            return this;
        }

        public Builder streakType(String v) {
            s.streakType = v;
            return this;
        }

        public Builder currentStreak(int v) {
            s.currentStreak = v;
            return this;
        }

        public Builder lastUpdated(LocalDateTime v) {
            s.lastUpdated = v;
            return this;
        }

        public Builder isBroken(boolean v) {
            s.isBroken = v;
            return this;
        }

        public Builder penaltyPerBreak(int v) {
            s.penaltyPerBreak = v;
            return this;
        }

        public Streak build() {
            return s;
        }
    }
}
