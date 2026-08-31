package jjcet.PragatiX.modules.analytics.api.dto.request;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public record AttendanceFilter(
        String academicYear,
        Long departmentId,
        Integer stageId,
        Long sectionId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        Integer period,
        Double threshold
) {
    public static AttendanceFilterBuilder builder() {
        return new AttendanceFilterBuilder();
    }

    public static class AttendanceFilterBuilder {
        private String academicYear;
        private Long departmentId;
        private Integer stageId;
        private Long sectionId;
        private LocalDate date;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer period;
        private Double threshold;

        public AttendanceFilterBuilder academicYear(String academicYear) {
            this.academicYear = academicYear;
            return this;
        }

        public AttendanceFilterBuilder departmentId(Long departmentId) {
            this.departmentId = departmentId;
            return this;
        }

        public AttendanceFilterBuilder stageId(Integer stageId) {
            this.stageId = stageId;
            return this;
        }

        public AttendanceFilterBuilder sectionId(Long sectionId) {
            this.sectionId = sectionId;
            return this;
        }

        public AttendanceFilterBuilder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public AttendanceFilterBuilder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public AttendanceFilterBuilder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public AttendanceFilterBuilder period(Integer period) {
            this.period = period;
            return this;
        }

        public AttendanceFilterBuilder threshold(Double threshold) {
            this.threshold = threshold;
            return this;
        }

        public AttendanceFilter build() {
            return new AttendanceFilter(academicYear, departmentId, stageId, sectionId, date, startDate, endDate, period, threshold);
        }
    }
}