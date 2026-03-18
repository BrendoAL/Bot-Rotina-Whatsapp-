package com.lambda.activity_service.activity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WhatsappMessageRepository extends JpaRepository<WhatsappMessage, Long> {
    List<WhatsappMessage> findByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);
}
