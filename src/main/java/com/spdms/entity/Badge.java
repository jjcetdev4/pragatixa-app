package com.pragatix.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "badges")
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String tier; // FOUNDATION, ACHIEVEMENT, EXCELLENCE, ELITE, LEGACY

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "xp_required", nullable = false)
    private int xpRequired;

    @Column(name = "icon_url", length = 255)
    private String iconUrl;

    @Column(name = "approval_authority", nullable = false, length = 100)
    private String approvalAuthority;

    @Column(nullable = false, length = 50)
    private String rarity;

    public Badge() {
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

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getXpRequired() {
        return xpRequired;
    }

    public void setXpRequired(int xpRequired) {
        this.xpRequired = xpRequired;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getApprovalAuthority() {
        return approvalAuthority;
    }

    public void setApprovalAuthority(String approvalAuthority) {
        this.approvalAuthority = approvalAuthority;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Badge badge = new Badge();

        public Builder name(String v) {
            badge.name = v;
            return this;
        }

        public Builder tier(String v) {
            badge.tier = v;
            return this;
        }

        public Builder description(String v) {
            badge.description = v;
            return this;
        }

        public Builder xpRequired(int v) {
            badge.xpRequired = v;
            return this;
        }

        public Builder iconUrl(String v) {
            badge.iconUrl = v;
            return this;
        }

        public Builder approvalAuthority(String v) {
            badge.approvalAuthority = v;
            return this;
        }

        public Builder rarity(String v) {
            badge.rarity = v;
            return this;
        }

        public Badge build() {
            return badge;
        }
    }
}
