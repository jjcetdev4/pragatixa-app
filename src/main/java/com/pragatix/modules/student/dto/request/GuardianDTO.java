package com.pragatix.modules.student.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class GuardianDTO {

    @NotBlank(message = "Guardian Name is required")
    @Size(max = 150)
    private String guardianName;

    @NotBlank(message = "Relationship is required")
    private String relationship;

    @NotBlank(message = "Guardian Phone is required")
    @Pattern(regexp = "^\\d{10}$", message = "Guardian phone must be exactly 10 digits")
    private String phoneNo;

    @Size(max = 150)
    private String email;

    public GuardianDTO() {
    }

    public String getGuardianName() {
        return guardianName;
    }

    public void setGuardianName(String guardianName) {
        this.guardianName = guardianName;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
