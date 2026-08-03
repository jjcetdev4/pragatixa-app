package com.pragatix.modules.analytics.repository;

import com.pragatix.modules.analytics.dto.*;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceAnalyticsRepository {
    AnalyticsOverviewDTO getOverview(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period);
    List<AttendanceTrendDTO> getTrend(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period);
    AttendanceDistributionDTO getDistribution(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period);
    List<GroupedAttendanceDTO> getDepartmentWiseAttendance(String yearNo, LocalDate startDate, LocalDate endDate, Integer period);
    List<LowAttendanceStudentDTO> getLowAttendanceStudents(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period, Double threshold);
    List<GroupedAttendanceDTO> getSectionWiseAttendance(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate, Integer period);
    List<AttendanceSummaryRowDTO> getSummaryTable(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period);
    List<AttendanceExportDTO> getExportData(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period);
}
