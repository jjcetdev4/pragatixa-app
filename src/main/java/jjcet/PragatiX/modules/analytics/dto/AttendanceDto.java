package jjcet.PragatiX.modules.analytics.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public record AttendanceDto(
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate date,
        Long presentCount,
        Long absentCount,
        Long odCount,
        Long partialCount,
        Double rate
) {}