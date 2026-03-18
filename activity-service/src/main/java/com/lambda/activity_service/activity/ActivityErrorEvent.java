package com.lambda.activity_service.activity;

// Evento publicado quando falha ao criar atividade
// Consumido pelo notification-worker para notificar o erro no WhatsApp
public record ActivityErrorEvent(
        ActivityRequestDTO originalRequest,
        String error
) {}
