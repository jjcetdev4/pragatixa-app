package jjcet.PragatiX.modules.analytics.dto;

import java.util.List;

public class InterventionStatusDto {
    private Long total;
    private Long high;
    private Long medium;
    private Long low;
    private List<InterventionRiskDto> risks;

    public InterventionStatusDto() {
    }

    public InterventionStatusDto(Long total, Long high, Long medium, Long low, List<InterventionRiskDto> risks) {
        this.total = total;
        this.high = high;
        this.medium = medium;
        this.low = low;
        this.risks = risks;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getHigh() {
        return high;
    }

    public void setHigh(Long high) {
        this.high = high;
    }

    public Long getMedium() {
        return medium;
    }

    public void setMedium(Long medium) {
        this.medium = medium;
    }

    public Long getLow() {
        return low;
    }

    public void setLow(Long low) {
        this.low = low;
    }

    public List<InterventionRiskDto> getRisks() {
        return risks;
    }

    public void setRisks(List<InterventionRiskDto> risks) {
        this.risks = risks;
    }
}
