package com.lambda.activity_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record ActivityRequestDTO(
        @NotNull Long userId,
        String title,
        String description,
        @NotBlank String category,
        @Positive int durationMinutes,
        @NotNull LocalDate date,
        String source
) {}
