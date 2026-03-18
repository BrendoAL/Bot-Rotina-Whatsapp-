package com.lambda.activity_service.activity;

// Evento publicado quando atividade é criada com sucesso
// Consumido pelo notification-worker para responder no WhatsApp
public record ActivityCreatedEvent(
        Long userId,
        Long activityId,
        String phone,           // número WhatsApp — null se origem for API
        String category,
        int durationMinutes
) {}
