package jjcet.PragatiX.modules.analytics.api.dto.response.xp;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record XpHistoryDto(
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime date,
        String studentName,
        String registerNumber,
        String department,
        String section,
        String activityName,
        Integer awardXp,
        Integer penaltyXp,
        Integer netXp,
        Long currentTotalXp,
        String approvedBy
) {
}