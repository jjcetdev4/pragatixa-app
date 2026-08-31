package jjcet.PragatiX.modules.analytics.domain.service.risk;

import jjcet.PragatiX.modules.analytics.domain.model.RiskLevel;

public interface RiskClassificationService {
    RiskLevel classify(Double attendancePercentage);

    boolean isHighRisk(Double attendancePercentage);

    boolean isMediumRisk(Double attendancePercentage);

    boolean isLowRisk(Double attendancePercentage);
}