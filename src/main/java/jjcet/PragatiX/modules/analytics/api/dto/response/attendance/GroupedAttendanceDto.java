package jjcet.PragatiX.modules.analytics.api.dto.response.attendance;

public record GroupedAttendanceDto(
        String groupName,
        Double percentage
) {
}