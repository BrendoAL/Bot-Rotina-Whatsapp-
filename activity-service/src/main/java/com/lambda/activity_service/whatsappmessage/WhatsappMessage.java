package com.lambda.activity_service.activity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "message_text", nullable = false)
    private String messageText;

    @Column(name = "parsed_category")
    private String parsedCategory;

    @Column(name = "parsed_duration")
    private Integer parsedDuration;

    @Column(name = "parsed_title")
    private String parsedTitle;

    @Builder.Default
    @Column(nullable = false)
    private boolean processed = false;

    @Column(name = "activity_id")
    private Long activityId;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
