package com.lambda.activity_service.consumer;

import com.lambda.activity_service.activity.ActivityConsumer;
import com.lambda.activity_service.activity.ActivityEventPublisher;
import com.lambda.activity_service.activity.ActivityRequestDTO;
import com.lambda.activity_service.activity.ActivityService;
import com.lambda.activity_service.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityConsumer")
class ActivityConsumerTest {

    @Mock
    ActivityService activityService;
    @Mock
    ActivityEventPublisher eventPublisher;
    @InjectMocks
    ActivityConsumer activityConsumer;

    private ActivityRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new ActivityRequestDTO(
                1L, "Estudar Java", null, "ESTUDO", 90, LocalDate.now(), "WHATSAPP"
        );
    }

    // ─────────────────────────────────────────────────────────────
    // consume()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("consume()")
    class Consume {

        @Test
        @DisplayName("deve criar atividade e publicar activity.created quando mensagem válida")
        void shouldCreateActivityAndPublishCreatedEvent() {
            when(activityService.create(any())).thenReturn(10L);

            activityConsumer.consume(validRequest);

            verify(activityService).create(validRequest);
            verify(eventPublisher).publishCreated(eq(1L), eq(10L), any());
        }

        @Test
        @DisplayName("deve publicar activity.error quando userId não encontrado")
        void shouldPublishErrorWhenUserNotFound() {
            when(activityService.create(any())).thenThrow(new UserNotFoundException(1L));

            activityConsumer.consume(validRequest);

            verify(eventPublisher).publishError(eq(validRequest), any(String.class));
            verify(eventPublisher, never()).publishCreated(any(), any(), any());
        }

        @Test
        @DisplayName("deve publicar activity.error quando ocorre erro inesperado")
        void shouldPublishErrorOnUnexpectedException() {
            when(activityService.create(any())).thenThrow(new RuntimeException("erro inesperado"));

            activityConsumer.consume(validRequest);

            verify(eventPublisher).publishError(eq(validRequest), any(String.class));
            verify(eventPublisher, never()).publishCreated(any(), any(), any());
        }

        @Test
        @DisplayName("não deve lançar exceção para o RabbitMQ — erros são tratados internamente")
        void shouldNotThrowException() {
            when(activityService.create(any())).thenThrow(new RuntimeException("erro"));

            org.assertj.core.api.Assertions.assertThatNoException()
                    .isThrownBy(() -> activityConsumer.consume(validRequest));
        }
    }
}
