package jjcet.PragatiX.modules.student.dto.response;

public class PenaltyActivityDto {
    private Long id;
    private String name;
    private String description;
    private Integer penaltyXp;
    private Boolean penaltyEnabled;

    public PenaltyActivityDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPenaltyXp() {
        return penaltyXp;
    }

    public void setPenaltyXp(Integer penaltyXp) {
        this.penaltyXp = penaltyXp;
    }

    public Boolean getPenaltyEnabled() {
        return penaltyEnabled;
    }

    public void setPenaltyEnabled(Boolean penaltyEnabled) {
        this.penaltyEnabled = penaltyEnabled;
    }
}
