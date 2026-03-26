package com.lambda.activity_service.whatsappmessage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WhatsappMessageService {

    private final WhatsappMessageRepository whatsappMessageRepository;

    public Long save(WhatsappMessageRequestDTO dto) {
        WhatsappMessage message = WhatsappMessage.builder()
                .phoneNumber(dto.phoneNumber())
                .messageText(dto.messageText())
                .parsedCategory(dto.parsedCategory())
                .parsedDuration(dto.parsedDuration())
                .parsedTitle(dto.parsedTitle())
                .processed(false)
                .build();

        return whatsappMessageRepository.save(message).getId();
    }

    public void markAsProcessed(Long messageId, Long activityId) {
        WhatsappMessage message = whatsappMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Mensagem não encontrada: " + messageId));
        message.setProcessed(true);
        message.setActivityId(activityId);
        whatsappMessageRepository.save(message);
    }

    public List<WhatsappMessageResponseDTO> findByPhone(String phone) {
        return whatsappMessageRepository.findByPhoneNumberOrderByCreatedAtDesc(phone)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private WhatsappMessageResponseDTO toDTO(WhatsappMessage m) {
        return new WhatsappMessageResponseDTO(
                m.getId(), m.getPhoneNumber(), m.getMessageText(),
                m.getParsedCategory(), m.getParsedDuration(), m.getParsedTitle(),
                m.isProcessed(), m.getActivityId(), m.getCreatedAt()
        );
    }
}
