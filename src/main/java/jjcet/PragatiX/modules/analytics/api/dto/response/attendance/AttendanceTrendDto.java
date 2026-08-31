package jjcet.PragatiX.modules.analytics.api.dto.response.attendance;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record AttendanceTrendDto(
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate date,
        Double percentage
) {
}