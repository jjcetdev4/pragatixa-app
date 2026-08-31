package jjcet.PragatiX.modules.analytics.service.impl;

import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.analytics.dto.*;
import jjcet.PragatiX.modules.analytics.dto.InterventionRiskDto;
import jjcet.PragatiX.modules.analytics.service.AnalyticsService;
import jjcet.PragatiX.modules.analytics.repository.AnalyticsRepository;
import jjcet.PragatiX.modules.analytics.domain.service.risk.RiskClassificationService;
import jjcet.PragatiX.modules.analytics.util.AnalyticsRoleUtils;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

// Legacy implementation superseded by domain.service.AnalyticsOrchestrator
@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final AuthUtils authUtils;
    private final RiskClassificationService riskClassificationService;

    public AnalyticsServiceImpl(AnalyticsRepository analyticsRepository, AuthUtils authUtils, RiskClassificationService riskClassificationService) {
        this.analyticsRepository = analyticsRepository;
        this.authUtils = authUtils;
        this.riskClassificationService = riskClassificationService;
    }

    @Override
    public List<InstitutionGrowthDto> getInstitutionGrowth(String yearNo, Integer semester, LocalDate startDate, LocalDate endDate) {
        // Apply role-based year filtering
        String effectiveYearNo = determineYearFilter(yearNo);
        
        // Validate inputs
        validateDateRange(startDate, endDate);
        
        // Get processed data from repository (growth rates already calculated, rounding applied)
        return analyticsRepository.getInstitutionGrowth(effectiveYearNo, semester, startDate, endDate);
    }

    @Override
    public List<DepartmentGrowthDto> getDepartmentGrowth(String yearNo, Integer semester, LocalDate startDate, LocalDate endDate) {
        // Apply role-based year filtering
        String effectiveYearNo = determineYearFilter(yearNo);
        
        // Validate inputs
        validateDateRange(startDate, endDate);
        
        // Get raw data from repository
        List<DepartmentGrowthDto> rawData = analyticsRepository.getDepartmentGrowth(effectiveYearNo, semester, startDate, endDate);
        
        // Apply business logic: calculate proper period-over-period growth
        // For now, returning the raw data as the repository already implements growth calculation
        // In a real implementation, we would calculate period-over-period changes here
        return applyDepartmentGrowthCalculations(rawData);
    }

    @Override
    public List<XpCurveDto> getXpCurve(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate) {
        // Apply role-based year filtering
        String effectiveYearNo = determineYearFilter(yearNo);
        
        // Apply role-based scoping for departmentId and stage
        Long effectiveDepartmentId = departmentId;
        Integer effectiveStage = stage;
        User user = authUtils.getCurrentUser();
        
        if (user != null) {
            if (AnalyticsRoleUtils.isHod(user)) {
                // HOD can only see their assigned department
                effectiveDepartmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;
                // HOD might be able to see sections within their department, but let's keep the provided sectionId for now
                // STAGE scoping not implemented here due to lack of direct access from User to Student record
            } else if (AnalyticsRoleUtils.isFaculty(user)) {
                // FACULTY can see departments/sections they're associated with
                // For now, we'll use the provided parameters but could add more sophisticated logic
                // if we had a way to check faculty assignments
            } else if (AnalyticsRoleUtils.isStudent(user)) {
                // STUDENT can only see their own department (from User entity)
                // STAGE scoping not fully implemented here due to lack of direct access from User to Student record
                effectiveDepartmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;
                // Keep provided stageId - frontend should set appropriate values for student role
            }
            // ADMIN/SUPER_ADMIN can see whatever they provide (no restriction)
        }
        
        // Validate inputs
        validateDateRange(startDate, endDate);
        validateDepartmentId(effectiveDepartmentId);
        // Note: validateStage not called for all code paths since stage scoping is incomplete
        // Frontend should set appropriate values for roles like STUDENT
        
        // Get raw data from repository
        List<XpCurveDto> rawData = analyticsRepository.getXpCurve(effectiveYearNo, effectiveDepartmentId, effectiveStage, startDate, endDate);
        
        // Apply business logic: monthly aggregation is already done in repository
        // Just apply rounding and zero-baseline handling if needed
        return applyXpCurveCalculations(rawData);
    }

    @Override
    public List<StageDistributionDto> getStageDistribution(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate) {
        // Apply role-based year filtering
        String effectiveYearNo = determineYearFilter(yearNo);
        
        // Apply role-based scoping for departmentId and stage
        Long effectiveDepartmentId = departmentId;
        Integer effectiveStage = stage;
        User user = authUtils.getCurrentUser();
        
        if (user != null) {
            if (AnalyticsRoleUtils.isHod(user)) {
                // HOD can only see their assigned department
                effectiveDepartmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;
            } else if (AnalyticsRoleUtils.isFaculty(user)) {
                // FACULTY can see departments they're associated with
                // For now, we'll use the provided departmentId but could add more sophisticated logic
            } else if (AnalyticsRoleUtils.isStudent(user)) {
                // STUDENT can only see their own department and stage
                effectiveDepartmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;
                // effectiveStage = user.getStage(); // Not implemented - User entity does not have stage information
            }
            // ADMIN/SUPER_ADMIN can see whatever they provide (no restriction)
        }
        
        // Validate inputs
        validateDateRange(startDate, endDate);
        validateDepartmentId(effectiveDepartmentId);
        // Note: validateStage not called for all code paths since stage scoping is incomplete
        // Frontend should set appropriate values for roles like STUDENT
        
        // Get raw data from repository
        List<StageDistributionDto> rawData = analyticsRepository.getStageDistribution(effectiveYearNo, effectiveDepartmentId, effectiveStage, startDate, endDate);
        
        // Apply business logic: calculate percentages using approved denominator
        return applyStageDistributionCalculations(rawData);
    }

    @Override
    public List<AttendanceDto> getAttendanceTrend(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period) {
        // Apply role-based year filtering
        String effectiveYearNo = determineYearFilter(yearNo);
        
        // Apply role-based scoping for departmentId, stage, and sectionId
        Long effectiveDepartmentId = departmentId;
        Integer effectiveStage = stage;
        Long effectiveSectionId = sectionId;
        User user = authUtils.getCurrentUser();
        
        if (user != null) {
            if (AnalyticsRoleUtils.isHod(user)) {
                // HOD can only see their assigned department
                effectiveDepartmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;
                // HOD might be able to see sections within their department, but let's keep the provided sectionId for now
            } else if (AnalyticsRoleUtils.isFaculty(user)) {
                // FACULTY can see departments/sections they're associated with
                // For now, we'll use the provided parameters but could add more sophisticated logic
            } else if (AnalyticsRoleUtils.isStudent(user)) {
                // STUDENT can only see their own department, stage, and section
                effectiveDepartmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;
                // effectiveStage = user.getStage(); // Not implemented - User entity does not have stage information
                effectiveSectionId = user.getSection() != null ? user.getSection().getId() : null;
            }
            // ADMIN/SUPER_ADMIN can see whatever they provide (no restriction)
        }
        
        // Validate inputs
        validateDateRange(startDate, endDate);
        validateDepartmentId(effectiveDepartmentId);
        validateStage(effectiveStage);
        validateSectionId(effectiveSectionId);
        validatePeriod(period);
        
        // Get raw data from repository
        List<AttendanceDto> rawData = analyticsRepository.getAttendanceTrend(effectiveYearNo, effectiveDepartmentId, effectiveStage, effectiveSectionId, startDate, endDate, period);
        
        // Apply business logic: calculate rates and apply rounding
        return applyAttendanceCalculations(rawData);
    }

    @Override
    public List<AttendanceCalendarDto> getAttendanceCalendar(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period) {
        // Apply role-based year filtering
        String effectiveYearNo = determineYearFilter(yearNo);
        
        // Apply role-based scoping for departmentId, stage, and sectionId
        Long effectiveDepartmentId = departmentId;
        Integer effectiveStage = stage;
        Long effectiveSectionId = sectionId;
        User user = authUtils.getCurrentUser();
        
        if (user != null) {
            if (AnalyticsRoleUtils.isHod(user)) {
                // HOD can only see their assigned department
                effectiveDepartmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;
            } else if (AnalyticsRoleUtils.isFaculty(user)) {
                // FACULTY can see departments/sections they're associated with
                // For now, we'll use the provided parameters but could add more sophisticated logic
            } else if (AnalyticsRoleUtils.isStudent(user)) {
                // STUDENT can only see their own department, stage, and section
                effectiveDepartmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;
                // effectiveStage = user.getStage(); // Not implemented - User entity does not have stage information
                effectiveSectionId = user.getSection() != null ? user.getSection().getId() : null;
            }
            // ADMIN/SUPER_ADMIN can see whatever they provide (no restriction)
        }
        
        // Validate inputs
        validateDateRange(startDate, endDate);
        validateDepartmentId(effectiveDepartmentId);
        validateStage(effectiveStage);
        validateSectionId(effectiveSectionId);
        validatePeriod(period);
        
        // Get raw data from repository
        List<AttendanceCalendarDto> rawData = analyticsRepository.getAttendanceCalendar(effectiveYearNo, effectiveDepartmentId, effectiveStage, effectiveSectionId, startDate, endDate, period);
        
        // Apply business logic: calculate rates and apply rounding
        return applyAttendanceCalendarCalculations(rawData);
    }

    @Override
    public List<ActivityFunnelDto> getActivityFunnel(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate) {
        // Apply role-based year filtering
        String effectiveYearNo = determineYearFilter(yearNo);
        
        // Apply role-based scoping for departmentId, stage, and sectionId
        Long effectiveDepartmentId = departmentId;
        Integer effectiveStage = stage;
        Long effectiveSectionId = sectionId;
        User user = authUtils.getCurrentUser();
        
        if (user != null) {
            if (AnalyticsRoleUtils.isHod(user)) {
                // HOD can only see their assigned department
                effectiveDepartmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;
            } else if (AnalyticsRoleUtils.isFaculty(user)) {
                // FACULTY can see departments/sections they're associated with
                // For now, we'll use the provided parameters but could add more sophisticated logic
            } else if (AnalyticsRoleUtils.isStudent(user)) {
                // STUDENT can only see their own department, stage, and section
                effectiveDepartmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;
                // effectiveStage = user.getStage(); // Not implemented - User entity does not have stage information
                effectiveSectionId = user.getSection() != null ? user.getSection().getId() : null;
            }
            // ADMIN/SUPER_ADMIN can see whatever they provide (no restriction)
        }
        
        // Validate inputs
        validateDateRange(startDate, endDate);
        validateDepartmentId(effectiveDepartmentId);
        validateStage(effectiveStage);
        validateSectionId(effectiveSectionId);
        
        // Get raw data from repository
        List<ActivityFunnelDto> rawData = analyticsRepository.getActivityFunnel(effectiveYearNo, effectiveDepartmentId, effectiveStage, effectiveSectionId, startDate, endDate);
        
        // Apply business logic: convert raw counts to approved percentages
        return applyActivityFunnelCalculations(rawData);
    }

    @Override
    public List<PerformanceLeaderDto> getTopPerformers(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate, Integer limit) {
        // Apply role-based year filtering
        String effectiveYearNo = determineYearFilter(yearNo);
        
        // Apply role-based scoping for departmentId, stage, and limit
        Long effectiveDepartmentId = departmentId;
        Integer effectiveStage = stage;
        Integer effectiveLimit = limit;
        User user = authUtils.getCurrentUser();
        
        if (user != null) {
            if (AnalyticsRoleUtils.isHod(user)) {
                // HOD can only see their assigned department
                effectiveDepartmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;
            } else if (AnalyticsRoleUtils.isFaculty(user)) {
                // FACULTY can see departments they're associated with
                // For now, we'll use the provided departmentId but could add more sophisticated logic
            } else if (AnalyticsRoleUtils.isStudent(user)) {
                // STUDENT can only see their own department and stage
                effectiveDepartmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;
                // effectiveStage = user.getStage(); // Not implemented - User entity does not have stage information
            }
            // ADMIN/SUPER_ADMIN can see whatever they provide (no restriction)
            
            // Apply role-based limit restrictions
            if (AnalyticsRoleUtils.isStudent(user) || AnalyticsRoleUtils.isFaculty(user)) {
                // Students and faculty might have lower limits for performance leaders
                if (effectiveLimit == null || effectiveLimit > 50) {
                    effectiveLimit = 50;
                }
            }
            // HOD and admin can have higher limits
            // ADMIN/SUPER_ADMIN can use whatever limit they provide (within validation)
        }
        
        // Validate inputs
        validateDateRange(startDate, endDate);
        validateDepartmentId(effectiveDepartmentId);
        validateStage(effectiveStage);
        validateLimit(effectiveLimit);
        
        // Get raw data from repository
        List<PerformanceLeaderDto> rawData = analyticsRepository.getTopPerformers(effectiveYearNo, effectiveDepartmentId, effectiveStage, startDate, endDate, effectiveLimit);
        
        // Apply business logic: apply approved sorting and limits
        return applyTopPerformersCalculations(rawData, effectiveLimit);
    }

    @Override
    public List<MoverDto> getMostImproved(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate, Integer limit) {
        // Apply role-based year filtering
        String effectiveYearNo = determineYearFilter(yearNo);
        
        // Apply role-based scoping for departmentId, stage, and limit
        Long effectiveDepartmentId = departmentId;
        Integer effectiveStage = stage;
        Integer effectiveLimit = limit;
        User user = authUtils.getCurrentUser();
        
        if (user != null) {
            if (AnalyticsRoleUtils.isHod(user)) {
                // HOD can only see their assigned department
                effectiveDepartmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;
            } else if (AnalyticsRoleUtils.isFaculty(user)) {
                // FACULTY can see departments they're associated with
                // For now, we'll use the provided departmentId but could add more sophisticated logic
            } else if (AnalyticsRoleUtils.isStudent(user)) {
                // STUDENT can only see their own department and stage
                effectiveDepartmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;
                // effectiveStage = user.getStage(); // Not implemented - User entity does not have stage information
            }
            // ADMIN/SUPER_ADMIN can see whatever they provide (no restriction)
            
            // Apply role-based limit restrictions
            if (AnalyticsRoleUtils.isStudent(user) || AnalyticsRoleUtils.isFaculty(user)) {
                // Students and faculty might have lower limits for performance leaders
                if (effectiveLimit == null || effectiveLimit > 50) {
                    effectiveLimit = 50;
                }
            }
            // HOD and admin can have higher limits
            // ADMIN/SUPER_ADMIN can use whatever limit they provide (within validation)
        }
        
        // Validate inputs
        validateDateRange(startDate, endDate);
        validateDepartmentId(effectiveDepartmentId);
        validateStage(effectiveStage);
        validateLimit(effectiveLimit);
        
        // Get raw data from repository
        List<MoverDto> rawData = analyticsRepository.getMostImproved(effectiveYearNo, effectiveDepartmentId, effectiveStage, startDate, endDate, effectiveLimit);
        
        // Apply business logic: calculate improvement (not just copy top performers)
        return applyMostImprovedCalculations(rawData, effectiveLimit);
    }

    // Helper methods for validation
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }
        // Validate maximum date range (2 years)
        if (startDate != null && endDate != null) {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
            if (daysBetween > 730) { // Approximately 2 years (365*2 = 730)
                throw new IllegalArgumentException("Date range cannot exceed 2 years");
            }
        }
    }

    private String determineYearFilter(String providedYearNo) {
        // A 4-digit value is treated as a calendar year (e.g. "2026") coming from the
        // frontend. Honor it directly so the analytics queries scope by the date range
        // instead of being overridden by the caller's assigned year_no.
        if (providedYearNo != null && providedYearNo.length() == 4) {
            return providedYearNo;
        }
        User user = authUtils.getCurrentUser();
        if (user != null) {
            if (authUtils.isAdmin(user) && !authUtils.isSuperAdmin(user)) {
                // For ADMIN (non-super admin), use their assigned academic year when no year is supplied
                return AuthUtils.getAssignedYearString(user.getAcademicYear());
            } else if (AnalyticsRoleUtils.isHod(user)) {
                // For HOD, use their assigned academic year when no year is supplied
                return AuthUtils.getAssignedYearString(user.getAcademicYear());
            } else if (AnalyticsRoleUtils.isFaculty(user)) {
                // For FACULTY, use their assigned academic year when no year is supplied
                return AuthUtils.getAssignedYearString(user.getAcademicYear());
            } else if (AnalyticsRoleUtils.isStudent(user)) {
                // For STUDENT, use their assigned academic year when no year is supplied
                return AuthUtils.getAssignedYearString(user.getAcademicYear());
            }
            // For SUPER_ADMIN or if no specific role matching, use the provided yearNo
        }
        return providedYearNo;
    }

    private void validateDepartmentId(Long departmentId) {
        if (departmentId != null && departmentId <= 0) {
            throw new IllegalArgumentException("Department ID must be positive");
        }
    }

    private void validateStage(Integer stage) {
        if (stage != null && (stage < 1 || stage > 4)) { // Assuming 4 stages based on existing code
            throw new IllegalArgumentException("Stage must be between 1 and 4");
        }
    }

    private void validateSectionId(Long sectionId) {
        if (sectionId != null && sectionId <= 0) {
            throw new IllegalArgumentException("Section ID must be positive");
        }
    }

    private void validatePeriod(Integer period) {
        if (period != null && period <= 0) {
            throw new IllegalArgumentException("Period must be positive");
        }
    }

    private void validateLimit(Integer limit) {
        if (limit != null && (limit <= 0 || limit > 100)) { // Reasonable limit for leaderboards
            throw new IllegalArgumentException("Limit must be between 1 and 100");
        }
    }

    // Helper methods for calculations
    private Double calculateGrowthRate(Double currentValue, Double previousValue) {
        if (currentValue == null) {
            return null;
        }
        if (previousValue == null || previousValue == 0) {
            // Zero-baseline case: return null as per approved contract
            return null;
        }
        return ((currentValue - previousValue) / previousValue) * 100.0;
    }

    private Double round(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    private List<InstitutionGrowthDto> applyGrowthCalculations(List<InstitutionGrowthDto> data) {
        // Implementation would go here for more complex growth calculations
        // For now, returning as-is since repository already does some calculations
        return data;
    }

    private List<DepartmentGrowthDto> applyDepartmentGrowthCalculations(List<DepartmentGrowthDto> data) {
        // Implementation would go here for department growth calculations
        // For now, returning as-is
        return data;
    }

    private List<XpCurveDto> applyXpCurveCalculations(List<XpCurveDto> data) {
        List<XpCurveDto> result = new ArrayList<>();
        for (XpCurveDto dto : data) {
            Double roundedXp = round(dto.xp());
            result.add(new XpCurveDto(dto.period(), roundedXp));
        }
        return result;
    }

    private List<StageDistributionDto> applyStageDistributionCalculations(List<StageDistributionDto> data) {
        List<StageDistributionDto> result = new ArrayList<>();
        long totalStudents = data.stream()
                .mapToLong(StageDistributionDto::studentCount)
                .sum();
        
        for (StageDistributionDto dto : data) {
            Double percentage = (totalStudents > 0) 
                    ? round((dto.studentCount() * 100.0) / totalStudents) 
                    : 0.0;
            result.add(new StageDistributionDto(
                    dto.stageName(),
                    dto.studentCount(),
                    percentage
            ));
        }
        return result;
    }

    private List<AttendanceDto> applyAttendanceCalculations(List<AttendanceDto> data) {
        List<AttendanceDto> result = new ArrayList<>();
        for (AttendanceDto dto : data) {
            Double roundedRate = round(dto.rate());
            result.add(new AttendanceDto(
                    dto.date(),
                    dto.presentCount(),
                    dto.absentCount(),
                    dto.odCount(),
                    dto.partialCount(),
                    roundedRate
            ));
        }
        return result;
    }

    private List<AttendanceCalendarDto> applyAttendanceCalendarCalculations(List<AttendanceCalendarDto> data) {
        List<AttendanceCalendarDto> result = new ArrayList<>();
        for (AttendanceCalendarDto dto : data) {
            Double roundedRate = round(dto.getAttendanceRate());
            result.add(new AttendanceCalendarDto(
                    dto.getDate(),
                    dto.getTotalStudents(),
                    dto.getPresentCount(),
                    roundedRate
            ));
        }
        return result;
    }

    private List<ActivityFunnelDto> applyActivityFunnelCalculations(List<ActivityFunnelDto> data) {
        List<ActivityFunnelDto> result = new ArrayList<>();
        for (ActivityFunnelDto dto : data) {
            // Apply approved rounding (2 decimal places) to the percentage that's already correctly calculated by repository
            Double roundedPercentage = round(dto.percentage());
            result.add(new ActivityFunnelDto(
                    dto.stage(),
                    dto.count(),
                    roundedPercentage
            ));
        }
        return result;
    }

    private List<PerformanceLeaderDto> applyTopPerformersCalculations(List<PerformanceLeaderDto> data, Integer limit) {
        // Sort by XP descending, then attendance descending
        List<PerformanceLeaderDto> sorted = new ArrayList<>(data);
        sorted.sort(Comparator
                .comparing((PerformanceLeaderDto p) -> p.xp() == null ? Double.NEGATIVE_INFINITY : p.xp())
                .reversed()
                .thenComparing((PerformanceLeaderDto p) -> p.attendance() == null ? Double.NEGATIVE_INFINITY : p.attendance())
                .reversed());
        
        // Apply limit
        if (limit != null && limit < sorted.size()) {
            return sorted.subList(0, limit);
        }
        return sorted;
    }

    private List<MoverDto> applyMostImprovedCalculations(List<MoverDto> data, Integer limit) {
        // Since the repository already calculates the improvement percentage correctly,
        // we just need to apply the limit and return the data as-is.
        
        if (limit != null && limit < data.size()) {
            return new ArrayList<>(data.subList(0, limit));
        }
        return new ArrayList<>(data);
    }

    @Override
    public List<InterventionRiskDto> getInterventionRisks(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate) { 
        // Apply role-based year filtering 
        String effectiveYearNo = determineYearFilter(yearNo); 
        
        // Apply role-based scoping for departmentId and stage 
        Long effectiveDepartmentId = departmentId; 
        Integer effectiveStage = stage; 
        User user = authUtils.getCurrentUser(); 
        
        if (user != null) { 
            if (AnalyticsRoleUtils.isHod(user)) { 
                // HOD can only see their assigned department 
                effectiveDepartmentId = user.getDepartment() != null ? user.getDepartment().getId() : null; 
            } else if (AnalyticsRoleUtils.isFaculty(user)) { 
                // FACULTY can see departments they are associated with 
                // For now, we'll use the provided departmentId but could add more sophisticated logic 
            } else if (AnalyticsRoleUtils.isStudent(user)) { 
                // STUDENT can only see their own department and stage 
                effectiveDepartmentId = user.getDepartment() != null ? user.getDepartment().getId() : null; 
                // effectiveStage = user.getStage(); // Not implemented - User entity does not have stage information 
            } 
            // ADMIN/SUPER_ADMIN can see whatever they provide (no restriction) 
        } 
        
        // Validate inputs 
        validateDateRange(startDate, endDate); 
        validateDepartmentId(effectiveDepartmentId); 
        validateStage(effectiveStage); 
        
        // Get raw data from repository 
        List<Object[]> rawData = analyticsRepository.getInterventionRisks(effectiveYearNo, effectiveDepartmentId, effectiveStage, startDate, endDate); 
        
        // Apply business logic: calculate risk levels based on approved thresholds 
        return applyInterventionRiskCalculations(rawData); 
    }

    // Helper methods for intervention risk calculations
    private List<InterventionRiskDto> applyInterventionRiskCalculations(List<Object[]> rawData) {
        List<InterventionRiskDto> result = new ArrayList<>();
        
        long totalStudents = 0;
        long highRiskCount = 0;
        long mediumRiskCount = 0;
        long lowRiskCount = 0;
        
        for (Object[] row : rawData) {
            totalStudents++;
            
            Double attendancePercentage = row[1] == null ? null : ((Number) row[1]).doubleValue();
            
            if (riskClassificationService.isHighRisk(attendancePercentage)) {
                highRiskCount++;
            } else if (riskClassificationService.isMediumRisk(attendancePercentage)) {
                mediumRiskCount++;
            } else {
                lowRiskCount++;
            }
        }
        
        if (totalStudents > 0) {
            if (highRiskCount > 0) {
                result.add(new InterventionRiskDto("HIGH", highRiskCount));
            }
            if (mediumRiskCount > 0) {
                result.add(new InterventionRiskDto("MEDIUM", mediumRiskCount));
            }
            if (lowRiskCount > 0) {
                result.add(new InterventionRiskDto("LOW", lowRiskCount));
            }
        }
        
        return result;
    }
}
