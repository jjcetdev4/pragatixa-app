package jjcet.PragatiX.modules.analytics.api.dto.request;

import java.time.LocalDate;

public record FunnelFilter(
        String academicYear,
        Long departmentId,
        Integer stageId,
        Long sectionId,
        LocalDate startDate,
        LocalDate endDate
) {
    public static FunnelFilterBuilder builder() {
        return new FunnelFilterBuilder();
    }

    public static class FunnelFilterBuilder {
        private String academicYear;
        private Long departmentId;
        private Integer stageId;
        private Long sectionId;
        private LocalDate startDate;
        private LocalDate endDate;

        public FunnelFilterBuilder academicYear(String academicYear) {
            this.academicYear = academicYear;
            return this;
        }

        public FunnelFilterBuilder departmentId(Long departmentId) {
            this.departmentId = departmentId;
            return this;
        }

        public FunnelFilterBuilder stageId(Integer stageId) {
            this.stageId = stageId;
            return this;
        }

        public FunnelFilterBuilder sectionId(Long sectionId) {
            this.sectionId = sectionId;
            return this;
        }

        public FunnelFilterBuilder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public FunnelFilterBuilder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public FunnelFilter build() {
            return new FunnelFilter(academicYear, departmentId, stageId, sectionId, startDate, endDate);
        }
    }
}