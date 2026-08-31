package jjcet.PragatiX.modules.analytics.api.dto.request;

import java.time.LocalDate;

public record XpFilter(
        String academicYear,
        Long departmentId,
        Integer stageId,
        Long sectionId,
        LocalDate startDate,
        LocalDate endDate,
        String category,
        String type,
        Long threshold,
        int page,
        int size
) {
    public static XpFilterBuilder builder() {
        return new XpFilterBuilder();
    }

    public static class XpFilterBuilder {
        private String academicYear;
        private Long departmentId;
        private Integer stageId;
        private Long sectionId;
        private LocalDate startDate;
        private LocalDate endDate;
        private String category;
        private String type;
        private Long threshold = 20L;
        private int page = 0;
        private int size = 20;

        public XpFilterBuilder academicYear(String academicYear) {
            this.academicYear = academicYear;
            return this;
        }

        public XpFilterBuilder departmentId(Long departmentId) {
            this.departmentId = departmentId;
            return this;
        }

        public XpFilterBuilder stageId(Integer stageId) {
            this.stageId = stageId;
            return this;
        }

        public XpFilterBuilder sectionId(Long sectionId) {
            this.sectionId = sectionId;
            return this;
        }

        public XpFilterBuilder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public XpFilterBuilder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public XpFilterBuilder category(String category) {
            this.category = category;
            return this;
        }

        public XpFilterBuilder type(String type) {
            this.type = type;
            return this;
        }

        public XpFilterBuilder threshold(Long threshold) {
            this.threshold = threshold;
            return this;
        }

        public XpFilterBuilder page(int page) {
            this.page = page;
            return this;
        }

        public XpFilterBuilder size(int size) {
            this.size = size;
            return this;
        }

        public XpFilter build() {
            return new XpFilter(academicYear, departmentId, stageId, sectionId, startDate, endDate, category, type, threshold, page, size);
        }
    }
}