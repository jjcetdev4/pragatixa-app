package com.pragatix.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public class BadgeRequestCreateDto {
    @NotNull(message = "Badge ID is required")
    private Long badgeId;

    @NotBlank(message = "Proof Link is required")
    @URL(message = "Proof Link must be a valid URL")
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
