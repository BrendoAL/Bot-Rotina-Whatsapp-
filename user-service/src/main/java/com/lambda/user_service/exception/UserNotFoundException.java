package com.lambda.user_service.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("Usuário não encontrado: id=" + id);
    }
    public UserNotFoundException(String phone) {
        super("Usuário não encontrado: phone=" + phone);
    }
}
