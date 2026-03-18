package com.lambda.activity_service.dto;

import java.time.LocalDate;

public record ActivityResponseDTO(
        Long id,
        Long userId,
        String title,
        String description,
        String category,
        int durationMinutes,
        LocalDate date,
        String source
) {}
