package com.lambda.activity_service.whatsappmessage;

public record WhatsappMessageRequestDTO(
        String phoneNumber,
        String messageText,
        String parsedCategory,
        Integer parsedDuration,
        String parsedTitle
) {}
