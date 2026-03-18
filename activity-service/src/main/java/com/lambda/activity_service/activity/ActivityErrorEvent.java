package com.lambda.activity_service.activitymodule;

import com.lambda.activity_service.dto.ActivityRequestDTO;

// Evento publicado quando falha ao criar atividade
// Consumido pelo notification-worker para notificar o erro no WhatsApp
public record ActivityErrorEvent(
        ActivityRequestDTO originalRequest,
        String error
) {}
