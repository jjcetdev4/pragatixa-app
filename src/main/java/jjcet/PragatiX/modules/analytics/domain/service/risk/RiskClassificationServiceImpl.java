package jjcet.PragatiX.modules.analytics.domain.service.risk;

import jjcet.PragatiX.modules.analytics.domain.model.RiskLevel;
import org.springframework.stereotype.Service;

@Service
public class RiskClassificationServiceImpl implements RiskClassificationService {

    public static final double HIGH_RISK_ATTENDANCE_THRESHOLD = 75.0;
    public static final double MEDIUM_RISK_ATTENDANCE_THRESHOLD = 85.0;

    @Override
    public RiskLevel classify(Double attendancePercentage) {
        if (isHighRisk(attendancePercentage)) {
            return RiskLevel.HIGH;
        }
        if (isMediumRisk(attendancePercentage)) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    @Override
    public boolean isHighRisk(Double attendancePercentage) {
        if (attendancePercentage == null) {
            return true;
        }
        return attendancePercentage < HIGH_RISK_ATTENDANCE_THRESHOLD;
    }

    @Override
    public boolean isMediumRisk(Double attendancePercentage) {
        if (isHighRisk(attendancePercentage)) {
            return false;
        }
        if (attendancePercentage == null) {
            return false;
        }
        return attendancePercentage < MEDIUM_RISK_ATTENDANCE_THRESHOLD;
    }

    @Override
    public boolean isLowRisk(Double attendancePercentage) {
        return !isHighRisk(attendancePercentage) && !isMediumRisk(attendancePercentage);
    }
}