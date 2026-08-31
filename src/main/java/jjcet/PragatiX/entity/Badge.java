package jjcet.PragatiX.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "badges")
public class Badge implements SoftDeletable {

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "permanent_delete_at")
    private LocalDateTime permanentDeleteAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    @Column(name = "proof_required", nullable = false)
    private boolean proofRequired = true;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
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

    public boolean isProofRequired() {
        return proofRequired;
    }

    public void setProofRequired(boolean proofRequired) {
        this.proofRequired = proofRequired;
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

        public Builder proofRequired(boolean v) {
            badge.proofRequired = v;
            return this;
        }

        public Badge build() {
            return badge;
        }
    }
}

