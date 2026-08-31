package jjcet.PragatiX.modules.analytics.api.dto.response.risk;

import java.util.List;

public record InterventionStatusDto(
        Long total,
        Long high,
        Long medium,
        Long low,
        List<RiskSignalDto> risks
) {
    public static InterventionStatusDto empty() {
        return new InterventionStatusDto(0L, 0L, 0L, 0L, List.of());
    }
}