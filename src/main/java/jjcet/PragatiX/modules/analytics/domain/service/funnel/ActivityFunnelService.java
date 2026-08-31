package jjcet.PragatiX.modules.analytics.domain.service.funnel;

import jjcet.PragatiX.modules.analytics.api.dto.request.FunnelFilter;
import jjcet.PragatiX.modules.analytics.api.dto.response.funnel.ActivityFunnelDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.funnel.ActivityFunnelWrapperDto;
import jjcet.PragatiX.modules.analytics.infrastructure.repository.AnalyticsViewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityFunnelService {

    private final AnalyticsViewRepository repository;

    public ActivityFunnelService(AnalyticsViewRepository repository) {
        this.repository = repository;
    }

    public ActivityFunnelWrapperDto getFunnel(FunnelFilter filter) {
        List<ActivityFunnelDto> stages = repository.findActivityFunnel(filter);
        return new ActivityFunnelWrapperDto(stages);
    }
}