package jjcet.PragatiX.modules.enrollment.dto;

public class PendingStudentDto {
    private Long id;
    private String fullName;
    private String maskedMobile;
    private Long departmentId;
    private String departmentName;
    private String deptCode;

    public PendingStudentDto() {}

    public PendingStudentDto(Long id, String fullName, String maskedMobile, Long departmentId, String departmentName, String deptCode) {
        this.id = id;
        this.fullName = fullName;
        this.maskedMobile = maskedMobile;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.deptCode = deptCode;
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
