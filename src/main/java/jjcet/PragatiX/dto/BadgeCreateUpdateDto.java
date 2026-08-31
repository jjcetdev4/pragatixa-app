package jjcet.PragatiX.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BadgeCreateUpdateDto {

    @NotBlank(message = "Badge name is required")
    private String name;

    @NotBlank(message = "Tier is required")
    private String tier; // FOUNDATION, ACHIEVEMENT, EXCELLENCE, ELITE, LEGACY

    private String description;

    @NotNull(message = "XP required must be specified")
    private Integer xpRequired;

    private String iconUrl;

    private String approvalAuthority;

    private String rarity; // COMMON, RARE, EPIC, LEGENDARY

    private Boolean proofRequired;

    public BadgeCreateUpdateDto() {
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

    public Integer getXpRequired() {
        return xpRequired;
    }

    public void setXpRequired(Integer xpRequired) {
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

    public Boolean getProofRequired() {
        return proofRequired;
    }

    public void setProofRequired(Boolean proofRequired) {
        this.proofRequired = proofRequired;
    }
}
