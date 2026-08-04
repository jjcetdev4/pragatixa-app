package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_activity_xp")
public class StudentActivityXp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assignment_id", nullable = false)
    private ActivityAssignment assignment;

    @Column(name = "xp_awarded", nullable = false)
    private int xpAwarded;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "result", nullable = false, length = 50)
    private String result = "PASS";

    @Column(name = "awarded_at", nullable = false)
    private LocalDateTime awardedAt;

    @Column(name = "stage_order", columnDefinition = "int default 1")
    private Integer stage;

    public StudentActivityXp() {
    }

    public StudentActivityXp(Student student, Activity activity, User teacher, ActivityAssignment assignment,
            int xpAwarded, String remarks, LocalDateTime awardedAt, Integer stage) {
        this.student = student;
        this.activity = activity;
        this.teacher = teacher;
        this.assignment = assignment;
        this.xpAwarded = xpAwarded;
        this.remarks = remarks;
        this.awardedAt = awardedAt;
        this.result = xpAwarded >= 0 ? "PASS" : "FAIL";
        this.stage = stage;
    }

    public StudentActivityXp(Student student, Activity activity, User teacher, ActivityAssignment assignment,
            int xpAwarded, String remarks, String result, LocalDateTime awardedAt, Integer stage) {
        this.student = student;
        this.activity = activity;
        this.teacher = teacher;
        this.assignment = assignment;
        this.xpAwarded = xpAwarded;
        this.remarks = remarks;
        this.result = result;
        this.awardedAt = awardedAt;
        this.stage = stage;
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

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public User getTeacher() {
        return teacher;
    }

    public void setTeacher(User teacher) {
        this.teacher = teacher;
    }

    public ActivityAssignment getAssignment() {
        return assignment;
    }

    public void setAssignment(ActivityAssignment assignment) {
        this.assignment = assignment;
    }

    public int getXpAwarded() {
        return xpAwarded;
    }

    public void setXpAwarded(int xpAwarded) {
        this.xpAwarded = xpAwarded;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getResult() {
        return result != null ? result : "PASS";
    }

    public void setResult(String result) {
        this.result = result;
    }

    public LocalDateTime getAwardedAt() {
        return awardedAt;
    }

    public void setAwardedAt(LocalDateTime awardedAt) {
        this.awardedAt = awardedAt;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }
}
