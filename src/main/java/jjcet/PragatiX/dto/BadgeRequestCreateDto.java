package jjcet.PragatiX.dto;

import jakarta.validation.constraints.NotNull;

public class BadgeRequestCreateDto {
    @NotNull(message = "Badge ID is required")
    private Long badgeId;

    private String proofLink;

    public BadgeRequestCreateDto() {
    }

    public Long getBadgeId() {
        return badgeId;
    }

    public void setBadgeId(Long badgeId) {
        this.badgeId = badgeId;
    }

    public String getProofLink() {
        return proofLink;
    }

    public void setProofLink(String proofLink) {
        this.proofLink = proofLink;
    }
}

