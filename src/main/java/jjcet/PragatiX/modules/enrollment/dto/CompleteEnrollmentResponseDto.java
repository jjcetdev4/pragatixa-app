package jjcet.PragatiX.modules.enrollment.dto;

public class CompleteEnrollmentResponseDto {
    private boolean success;
    private String message;
    private Long studentId;
    private String studentName;
    private String departmentName;

    public CompleteEnrollmentResponseDto() {}

    public CompleteEnrollmentResponseDto(boolean success, String message, Long studentId, String studentName, String departmentName) {
        this.success = success;
        this.message = message;
        this.studentId = studentId;
        this.studentName = studentName;
        this.departmentName = departmentName;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }
}
