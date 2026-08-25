package jjcet.PragatiX.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Entity
@Table(name = "faculty")

@Filter(name = "deletedFilter")
public class Faculty implements SoftDeletable {
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "permanent_delete_at")
    private LocalDateTime permanentDeleteAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dept_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "section_id", nullable = true)
    private Section section;

    @Column(nullable = false, length = 100)
    private String designation;

    @Convert(converter = jjcet.PragatiX.util.crypto.AesGcmAttributeConverter.class)
    @Column(name = "phone_no", nullable = false, length = 255)
    private String phoneNo;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Faculty() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    @Override
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public LocalDateTime getPermanentDeleteAt() {
        return permanentDeleteAt;
    }

    @Override
    public void setPermanentDeleteAt(LocalDateTime permanentDeleteAt) {
        this.permanentDeleteAt = permanentDeleteAt;
    }

    @Override
    public String getDeletedBy() {
        return deletedBy;
    }

    @Override
    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Faculty f = new Faculty();

        public Builder user(User v) {
            f.user = v;
            return this;
        }

        public Builder department(Department v) {
            f.department = v;
            return this;
        }

        public Builder section(Section v) {
            f.section = v;
            return this;
        }

        public Builder designation(String v) {
            f.designation = v;
            return this;
        }

        public Builder phoneNo(String v) {
            f.phoneNo = v;
            return this;
        }

        public Builder deleted(boolean v) {
            f.deleted = v;
            return this;
        }

        public Builder deletedAt(LocalDateTime v) {
            f.deletedAt = v;
            return this;
        }

        public Builder permanentDeleteAt(LocalDateTime v) {
            f.permanentDeleteAt = v;
            return this;
        }

        public Builder deletedBy(String v) {
            f.deletedBy = v;
            return this;
        }

        public Faculty build() {
            return f;
        }
    }
}
