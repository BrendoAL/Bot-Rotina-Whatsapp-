package com.lambda.activity_service.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("Usuário não encontrado: id=" + userId);
    }
}
