package com.carestream.audit.kafka;

import com.carestream.audit.dto.PatientEventMessage;
import com.carestream.audit.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Phase 3 — Resilient audit event consumer.
 * DefaultErrorHandler (configured in KafkaConsumerConfig) applies exponential backoff
 * and routes to dlq.patient.events after retries are exhausted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics      = {"patient.admission", "patient.discharge", "patient.transfer"},
        groupId     = "audit-service-group",
        concurrency = "3",
        containerFactory = "auditKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        log.debug("[AUDIT-CONSUMER] topic={} partition={} offset={}",
                record.topic(), record.partition(), record.offset());

        PatientEventMessage message = deserialize(record.value());
        auditService.logPatientEvent(message);
    }

    private PatientEventMessage deserialize(String value) {
        try {
            return objectMapper.readValue(value, PatientEventMessage.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Audit deserialization failed: " + e.getMessage(), e);
        }
    }
}
