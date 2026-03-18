package com.lambda.activity_service.consumer;

import com.lambda.activity_service.activity.ActivityCreatedEvent;
import com.lambda.activity_service.activity.ActivityErrorEvent;
import com.lambda.activity_service.activity.ActivityEventPublisher;
import com.lambda.activity_service.activity.ActivityRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityEventPublisher")
class ActivityEventPublisherTest {

    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks
    ActivityEventPublisher eventPublisher;

    // ─────────────────────────────────────────────────────────────
    // publishCreated()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("publishCreated()")
    class PublishCreated {

        @Test
        @DisplayName("deve publicar na fila activity.created com os dados corretos")
        void shouldPublishToCreatedQueue() {
            eventPublisher.publishCreated(1L, 10L, "5511999999999");

            verify(rabbitTemplate).convertAndSend(
                    eq("activity.created"),
                    argThat(payload -> {
                        ActivityCreatedEvent event = (ActivityCreatedEvent) payload;
                        return event.userId().equals(1L)
                                && event.activityId().equals(10L)
                                && event.phone().equals("5511999999999");
                    })
            );
        }

        @Test
        @DisplayName("deve publicar mesmo quando phone é nulo (origem API)")
        void shouldPublishWhenPhoneIsNull() {
            eventPublisher.publishCreated(1L, 10L, null);

            verify(rabbitTemplate).convertAndSend(eq("activity.created"), any());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // publishError()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("publishError()")
    class PublishError {

        @Test
        @DisplayName("deve publicar na fila activity.error com a mensagem original e o erro")
        void shouldPublishToErrorQueue() {
            ActivityRequestDTO request = new ActivityRequestDTO(
                    99L, "Treino", null, "TREINO", 45, LocalDate.now(), "WHATSAPP"
            );

            eventPublisher.publishError(request, "Usuário não encontrado: id=99");

            verify(rabbitTemplate).convertAndSend(
                    eq("activity.error"),
                    argThat(payload -> {
                        ActivityErrorEvent event = (ActivityErrorEvent) payload;
                        return event.originalRequest().equals(request)
                                && event.error().contains("99");
                    })
            );
        }
    }
}
