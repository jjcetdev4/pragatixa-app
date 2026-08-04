package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "years")
public class Year {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "year_no", nullable = false, unique = true)
    private Byte yearNo;

    @Column(name = "year_name", nullable = false, unique = true, length = 30)
    private String yearName;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Year() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Byte getYearNo() {
        return yearNo;
    }

    public void setYearNo(Byte yearNo) {
        this.yearNo = yearNo;
    }

    public String getYearName() {
        return yearName;
    }

    public void setYearName(String yearName) {
        this.yearName = yearName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Year y = new Year();

        public Builder yearNo(Byte v) {
            y.yearNo = v;
            return this;
        }

        public Builder yearName(String v) {
            y.yearName = v;
            return this;
        }

        public Year build() {
            return y;
        }
    }
}
