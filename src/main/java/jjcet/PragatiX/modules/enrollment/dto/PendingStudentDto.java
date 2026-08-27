package jjcet.PragatiX.modules.enrollment.dto;

public class PendingStudentDto {
    private Long id;
    private String fullName;
    private String email;
    private String maskedEmail;
    private String maskedMobile;
    private Long departmentId;
    private String departmentName;
    private String deptCode;

    public PendingStudentDto() {}

    public PendingStudentDto(Long id, String fullName, String email, String maskedEmail, String maskedMobile, Long departmentId, String departmentName, String deptCode) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.maskedEmail = maskedEmail;
        this.maskedMobile = maskedMobile;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.deptCode = deptCode;
    }

    public PendingStudentDto(Long id, String fullName, String maskedMobile, Long departmentId, String departmentName, String deptCode) {
        this.id = id;
        this.fullName = fullName;
        this.maskedMobile = maskedMobile;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.deptCode = deptCode;
    }

    public static String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "";
        }
        String clean = email.trim();
        int atIndex = clean.indexOf('@');
        if (atIndex <= 2) {
            return clean;
        }
        String namePart = clean.substring(0, atIndex);
        String domainPart = clean.substring(atIndex);
        if (namePart.length() <= 3) {
            return namePart.charAt(0) + "***" + domainPart;
        }
        return namePart.substring(0, 2) + "***" + namePart.substring(namePart.length() - 1) + domainPart;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMaskedEmail() {
        return maskedEmail;
    }

    public void setMaskedEmail(String maskedEmail) {
        this.maskedEmail = maskedEmail;
    }

    public static String maskMobile(String mobile) {
        if (mobile == null || mobile.trim().isEmpty()) {
            return "******";
        }
        String clean = mobile.trim();
        if (clean.length() <= 4) {
            return "******" + clean;
        }
        String last4 = clean.substring(clean.length() - 4);
        return "******" + last4;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getMaskedMobile() {
        return maskedMobile;
    }

    public void setMaskedMobile(String maskedMobile) {
        this.maskedMobile = maskedMobile;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }
}
