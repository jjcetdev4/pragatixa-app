package jjcet.PragatiX.modules.enrollment.dto;

import jjcet.PragatiX.modules.enrollment.entity.Enrollment;
import jjcet.PragatiX.modules.enrollment.enums.EnrollmentStatus;

import java.time.LocalDateTime;

public class EnrollmentDto {
    private Long id;
    private String fullName;
    private String gender;
    private String email;
    private String mobile;
    private String maskedMobile;
    private Long departmentId;
    private String departmentName;
    private String deptCode;
    private EnrollmentStatus status;
    private Long enrolledStudentId;
    private String enrolledStudentRegNo;
    private LocalDateTime enrolledAt;
    private String createdBy;
    private LocalDateTime createdAt;

    public EnrollmentDto() {}

    public static EnrollmentDto fromEntity(Enrollment e, String studentRegNo) {
        EnrollmentDto dto = new EnrollmentDto();
        dto.setId(e.getId());
        dto.setFullName(e.getFullName());
        dto.setGender(e.getGender());
        dto.setEmail(e.getEmail());
        dto.setMobile(e.getMobile());
        dto.setMaskedMobile(PendingStudentDto.maskMobile(e.getMobile()));
        if (e.getDepartment() != null) {
            dto.setDepartmentId(e.getDepartment().getId());
            dto.setDepartmentName(e.getDepartment().getName());
            dto.setDeptCode(e.getDepartment().getDeptCode() != null ? e.getDepartment().getDeptCode() : e.getDepartment().getCode());
        }
        dto.setStatus(e.getStatus());
        dto.setEnrolledStudentId(e.getEnrolledStudentId());
        dto.setEnrolledStudentRegNo(studentRegNo);
        dto.setEnrolledAt(e.getEnrolledAt());
        dto.setCreatedBy(e.getCreatedBy());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
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

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }

    public Long getEnrolledStudentId() {
        return enrolledStudentId;
    }

    public void setEnrolledStudentId(Long enrolledStudentId) {
        this.enrolledStudentId = enrolledStudentId;
    }

    public String getEnrolledStudentRegNo() {
        return enrolledStudentRegNo;
    }

    public void setEnrolledStudentRegNo(String enrolledStudentRegNo) {
        this.enrolledStudentRegNo = enrolledStudentRegNo;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
