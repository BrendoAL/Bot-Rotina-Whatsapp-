package com.lambda.activity_service.goal;

public record GoalResponseDTO(
        Long id,
        Long userId,
        String category,
        int targetMinutes,
        String period,
        boolean active
) {}
