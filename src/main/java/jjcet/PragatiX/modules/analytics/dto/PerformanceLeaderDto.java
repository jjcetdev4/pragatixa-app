package jjcet.PragatiX.modules.analytics.dto;

public record PerformanceLeaderDto(
        Long studentId,
        String studentName,
        String rollNumber,
        Double xp,
        Double attendance
) {}
