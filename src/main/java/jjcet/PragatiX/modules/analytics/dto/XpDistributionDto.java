package jjcet.PragatiX.modules.analytics.dto;

public class XpDistributionDto {
    private String xpRange;
    private Long studentCount;
    private Double percentage;

    public XpDistributionDto() {
    }

    public XpDistributionDto(String xpRange, Long studentCount, Double percentage) {
        this.xpRange = xpRange;
        this.studentCount = studentCount;
        this.percentage = percentage;
    }

    public String getXpRange() {
        return xpRange;
    }

    public void setXpRange(String xpRange) {
        this.xpRange = xpRange;
    }

    public Long getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(Long studentCount) {
        this.studentCount = studentCount;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }
}