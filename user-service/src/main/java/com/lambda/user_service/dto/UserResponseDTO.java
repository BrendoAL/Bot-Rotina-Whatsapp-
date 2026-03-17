package com.lambda.user_service.dto;

public record UserResponseDTO(
        Long id,
        String name,
        String email,
        String phone,
        boolean active
) {}
