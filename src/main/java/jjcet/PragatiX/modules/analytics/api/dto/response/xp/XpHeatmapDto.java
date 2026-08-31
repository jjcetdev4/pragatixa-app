package jjcet.PragatiX.modules.analytics.api.dto.response.xp;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record XpHeatmapDto(
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate date,
        Long xp,
        Integer level
) {
}