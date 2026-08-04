package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_assignments")
public class ActivityAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id", nullable = true)
    private Department department;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "section_id", nullable = true)
    private Section section;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "teacher_id", nullable = true)
    private User teacher;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_scope", nullable = false)
    private AssignmentScope assignmentScope;

    @Column(name = "year", length = 50, nullable = true)
    private String year;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private User assignedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stage_id", nullable = true)
    private ActivityStage stage;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    public ActivityAssignment() {
    }

    public ActivityStage getStage() {
        return stage;
    }

    public void setStage(ActivityStage stage) {
        this.stage = stage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }

    public User getTeacher() {
        return teacher;
    }

    public void setTeacher(User teacher) {
        this.teacher = teacher;
    }

    public AssignmentScope getAssignmentScope() {
        return assignmentScope;
    }

    public void setAssignmentScope(AssignmentScope assignmentScope) {
        this.assignmentScope = assignmentScope;
    }

    public User getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(User assignedBy) {
        this.assignedBy = assignedBy;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}
