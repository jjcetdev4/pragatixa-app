package jjcet.PragatiX.dto;

import jjcet.PragatiX.entity.Badge;

public class BadgeDto {
    private Long id;
    private String name;
    private String tier;
    private String description;
    private int xpRequired;
    private String iconUrl;
    private String approvalAuthority;
    private String rarity;
    private boolean proofRequired;

    public BadgeDto() {
    }

    public BadgeDto(Badge badge) {
        if (badge != null) {
            this.id = badge.getId();
            this.name = badge.getName();
            this.tier = badge.getTier();
            this.description = badge.getDescription();
            this.xpRequired = badge.getXpRequired();
            this.iconUrl = badge.getIconUrl();
            this.approvalAuthority = badge.getApprovalAuthority();
            this.rarity = badge.getRarity();
            this.proofRequired = badge.isProofRequired();
        }
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

    public boolean isProofRequired() {
        return proofRequired;
    }

    public void setProofRequired(boolean proofRequired) {
        this.proofRequired = proofRequired;
    }
}
