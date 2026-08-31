package jjcet.PragatiX.modules.analytics.api.dto.response.attendance;

public record LowAttendanceStudentDto(
        String registerNumber,
        String studentName,
        Double attendancePercentage
) {
}