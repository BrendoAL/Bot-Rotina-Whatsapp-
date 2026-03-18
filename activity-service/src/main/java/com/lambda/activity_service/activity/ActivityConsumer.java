package com.lambda.activity_service.activity;

import com.lambda.activity_service.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityConsumer {

    private final ActivityService activityService;
    private final ActivityEventPublisher eventPublisher;

    @RabbitListener(queues = "activity.create")
    public void consume(ActivityRequestDTO request) {
        log.info("[CONSUMER] activity.create recebido — userId={} category={}", request.userId(), request.category());

        try {
            Long activityId = activityService.create(request);

            // Extrai phone da request — whatsapp-bot envia no campo source ou podemos
            // usar uma convenção: source=WHATSAPP significa que veio do bot
            String phone = "WHATSAPP".equals(request.source()) ? extractPhone(request) : null;

            eventPublisher.publishCreated(request.userId(), activityId, phone);

        } catch (UserNotFoundException e) {
            log.warn("[CONSUMER] userId não encontrado: {}", e.getMessage());
            eventPublisher.publishError(request, e.getMessage());

        } catch (Exception e) {
            log.error("[CONSUMER] Erro inesperado ao processar mensagem: {}", e.getMessage());
            eventPublisher.publishError(request, e.getMessage());
        }
        // Não relança a exceção — garante que a mensagem seja descartada (ack)
        // e não recoloque na fila indefinidamente
    }

    // O phone vem no título por convenção do whatsapp-bot:
    // title = "texto original" e a request já tem userId resolvido
    // Para passar o phone, o whatsapp-bot deve incluir no campo description
    private String extractPhone(ActivityRequestDTO request) {
        return request.description(); // whatsapp-bot salva o phone no campo description
    }
}
