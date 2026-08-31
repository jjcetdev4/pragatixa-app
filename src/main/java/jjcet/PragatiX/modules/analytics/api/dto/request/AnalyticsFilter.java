package jjcet.PragatiX.modules.analytics.api.dto.request;

import java.time.LocalDate;

public record AnalyticsFilter(
        String yearNo,
        Integer semester,
        Long departmentId,
        Integer stage,
        Long sectionId,
        LocalDate startDate,
        LocalDate endDate
) {
    public static AnalyticsFilterBuilder builder() {
        return new AnalyticsFilterBuilder();
    }

    public static class AnalyticsFilterBuilder {
        private String yearNo;
        private Integer semester;
        private Long departmentId;
        private Integer stage;
        private Long sectionId;
        private LocalDate startDate;
        private LocalDate endDate;

        public AnalyticsFilterBuilder yearNo(String yearNo) {
            this.yearNo = yearNo;
            return this;
        }

        public AnalyticsFilterBuilder semester(Integer semester) {
            this.semester = semester;
            return this;
        }

        public AnalyticsFilterBuilder departmentId(Long departmentId) {
            this.departmentId = departmentId;
            return this;
        }

        public AnalyticsFilterBuilder stage(Integer stage) {
            this.stage = stage;
            return this;
        }

        public AnalyticsFilterBuilder sectionId(Long sectionId) {
            this.sectionId = sectionId;
            return this;
        }

        public AnalyticsFilterBuilder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public AnalyticsFilterBuilder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public AnalyticsFilter build() {
            return new AnalyticsFilter(yearNo, semester, departmentId, stage, sectionId, startDate, endDate);
        }
    }
}