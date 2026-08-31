package jjcet.PragatiX.modules.analytics.api.dto.response.funnel;

import java.util.List;

public record ActivityFunnelWrapperDto(
        List<ActivityFunnelDto> stages
) {
    public static ActivityFunnelWrapperDto empty() {
        return new ActivityFunnelWrapperDto(List.of());
    }
}