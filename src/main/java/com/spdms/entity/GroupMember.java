package com.spdms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "group_members", uniqueConstraints = {
    @UniqueConstraint(name = "uk_group_student", columnNames = {"group_id", "student_id"})
})
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "is_captain", nullable = false)
    private boolean isCaptain;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup studentGroup;

    public GroupMember() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public boolean isCaptain() { return isCaptain; }
    public void setCaptain(boolean captain) { isCaptain = captain; }

    public StudentGroup getStudentGroup() { return studentGroup; }
    public void setStudentGroup(StudentGroup studentGroup) { this.studentGroup = studentGroup; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final GroupMember gm = new GroupMember();
        public Builder student(Student v) { gm.student = v; return this; }
        public Builder isCaptain(boolean v) { gm.isCaptain = v; return this; }
        public Builder studentGroup(StudentGroup v) { gm.studentGroup = v; return this; }
        public GroupMember build() { return gm; }
    }
}
