package com.lambda.activity_service.whatsappmessage;

import java.time.LocalDateTime;

public record WhatsappMessageResponseDTO(
        Long id,
        String phoneNumber,
        String messageText,
        String parsedCategory,
        Integer parsedDuration,
        String parsedTitle,
        boolean processed,
        Long activityId,
        LocalDateTime createdAt
) {}
