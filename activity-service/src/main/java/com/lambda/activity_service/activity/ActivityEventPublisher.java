package com.lambda.activity_service.activitymodule;

import com.lambda.activity_service.dto.ActivityRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishCreated(Long userId, Long activityId, String phone) {
        // phone é null quando a origem é API — notification-worker ignora nesse caso
        ActivityCreatedEvent event = new ActivityCreatedEvent(userId, activityId, phone, null, 0);
        rabbitTemplate.convertAndSend("activity.created", event);
        log.info("[PUBLISHER] activity.created — userId={} activityId={}", userId, activityId);
    }

    public void publishError(ActivityRequestDTO originalRequest, String error) {
        ActivityErrorEvent event = new ActivityErrorEvent(originalRequest, error);
        rabbitTemplate.convertAndSend("activity.error", event);
        log.warn("[PUBLISHER] activity.error — userId={} error={}", originalRequest.userId(), error);
    }
}
