package com.lambda.activity_service.activity;

public record ActivityStatsDTO(
        int todayCount,
        int todayMinutes,
        int weekCount,
        int weekMinutes
) {}
