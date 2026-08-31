package jjcet.PragatiX.modules.analytics.domain.service.performance;

import jjcet.PragatiX.modules.analytics.api.dto.request.AnalyticsFilter;
import jjcet.PragatiX.modules.analytics.api.dto.response.performance.MostImprovedDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.performance.PerformanceLeaderDto;
import jjcet.PragatiX.modules.analytics.infrastructure.repository.AnalyticsViewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PerformanceService {

    private final AnalyticsViewRepository repository;

    public PerformanceService(AnalyticsViewRepository repository) {
        this.repository = repository;
    }

    public List<PerformanceLeaderDto> getTopPerformers(AnalyticsFilter filter, int limit) {
        int effectiveLimit = limit > 0 ? limit : 10;
        return repository.findTopPerformers(filter, effectiveLimit);
    }

    public List<MostImprovedDto> getMostImproved(AnalyticsFilter filter, int limit) {
        int effectiveLimit = limit > 0 ? limit : 10;
        return repository.findMostImproved(filter, effectiveLimit);
    }
}