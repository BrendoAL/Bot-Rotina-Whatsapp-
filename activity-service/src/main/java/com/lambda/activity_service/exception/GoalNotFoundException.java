package com.lambda.activity_service.exception;

public class GoalNotFoundException extends RuntimeException {
    public GoalNotFoundException(Long id) {
        super("Meta não encontrada: id=" + id);
    }
}
