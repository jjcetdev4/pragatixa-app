package com.pragatix.modules.analytics.repository;

import com.pragatix.modules.analytics.dto.*;
import java.time.LocalDate;
import java.util.List;

public interface XpAnalyticsRepository {

    List<XpAwardVsPenaltyDTO> getAwardVsPenalty(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate);
    List<GroupedXpDTO> getDepartmentRanking(String yearNo, Integer stage, LocalDate startDate, LocalDate endDate);
    List<GroupedXpDTO> getSectionRanking(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate);
    List<XpHeatmapDTO> getMonthlyHeatmap(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate);
    List<XpTopPerformerDTO> getTopPerformers(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate);
    List<LowXpStudentDTO> getLowXpStudents(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Long threshold);
    List<ActivityXpContributionDTO> getActivityXpContribution(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, String category);
    List<XpHistoryDTO> getXpHistory(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, String activityName, String type, int limit, int offset);
    long getXpHistoryCount(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, String activityName, String type);
}
