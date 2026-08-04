package com.pragatix.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "custom_frequencies")
public class CustomFrequency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(name = "cap_type", nullable = false, length = 20)
    private String capType; // "UNLIMITED" or "MANUAL_CAP"

    @Column(name = "default_cap")
    private Integer defaultCap;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CustomFrequency() {
    }

    public CustomFrequency(String name, String capType, Integer defaultCap) {
        this.name = name;
        this.capType = capType;
        this.defaultCap = defaultCap;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCapType() {
        return capType;
    }

    public void setCapType(String capType) {
        this.capType = capType;
    }

    public Integer getDefaultCap() {
        return defaultCap;
    }

    public void setDefaultCap(Integer defaultCap) {
        this.defaultCap = defaultCap;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
