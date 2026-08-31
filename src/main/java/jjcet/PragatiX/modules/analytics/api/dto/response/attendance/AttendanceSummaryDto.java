package jjcet.PragatiX.modules.analytics.api.dto.response.attendance;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record AttendanceSummaryDto(
        Long totalStudents,
        Long presentCount,
        Long absentCount,
        Long odCount,
        Long leaveCount,
        Double presentPercentage,
        Double absentPercentage
) {
    public static AttendanceSummaryDto empty() {
        return new AttendanceSummaryDto(0L, 0L, 0L, 0L, 0L, 0.0, 0.0);
    }
}