package com.lambda.user_service.exception;

public class InactiveUserException extends RuntimeException {
    public InactiveUserException() {
        super("Usuário inativo");
    }
}
