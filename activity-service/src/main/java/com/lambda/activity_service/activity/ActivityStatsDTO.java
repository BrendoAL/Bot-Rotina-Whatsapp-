package com.lambda.activity_service.dto;

public record ActivityStatsDTO(
        int todayCount,
        int todayMinutes,
        int weekCount,
        int weekMinutes
) {}
