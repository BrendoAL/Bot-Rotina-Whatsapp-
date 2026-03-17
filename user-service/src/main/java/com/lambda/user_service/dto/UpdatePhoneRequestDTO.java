package com.lambda.user_service.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePhoneRequestDTO(@NotBlank String phone) {}
