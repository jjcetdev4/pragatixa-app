package jjcet.PragatiX.modules.analytics.dto;

import java.util.List;

public record ActivityFunnelWrapperDto(
        List<ActivityFunnelDto> stages
) {}
