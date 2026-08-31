package jjcet.PragatiX.modules.analytics.api.dto.response.attendance;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record AttendanceDto(
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate date,
        Long presentCount,
        Long absentCount,
        Long odCount,
        Long partialCount,
        Double rate
) {
}