package jjcet.PragatiX.modules.analytics.api.dto.response.attendance;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AttendanceDistributionDto(
        Double presentPercentage,
        Double partialPercentage,
        Double absentPercentage
) {
    @JsonProperty("partialAbsentPercentage")
    public Double getPartialAbsentPercentage() {
        return partialPercentage;
    }

    @JsonProperty("fullAbsentPercentage")
    public Double getFullAbsentPercentage() {
        return absentPercentage;
    }
}