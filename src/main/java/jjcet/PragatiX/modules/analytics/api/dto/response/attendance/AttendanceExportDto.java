package jjcet.PragatiX.modules.analytics.api.dto.response.attendance;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record AttendanceExportDto(
        String registerNumber,
        String studentName,
        String departmentName,
        String sectionName,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate date,
        Integer period,
        String status
) {
}