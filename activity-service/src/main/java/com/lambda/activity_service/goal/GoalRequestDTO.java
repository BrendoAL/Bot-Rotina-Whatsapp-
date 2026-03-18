package com.lambda.activity_service.goal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GoalRequestDTO(
        @NotNull Long userId,
        @NotBlank String category,
        @Positive int targetMinutes,
        @NotBlank String period
) {}
