package com.carestream.patient.kafka;

import com.carestream.patient.dto.PatientEventMessage;
import com.carestream.patient.service.PatientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.stereotype.Component;

/**
 * Phase 3 — Resilient patient event consumer.
 *
 * The DefaultErrorHandler (configured in KafkaConsumerConfig) handles:
 *   - Transient failures:   exponential backoff (1s → 2s → 4s), max 3 retries
 *   - Permanent failures:   route to dlq.patient.events after retries exhausted
 *   - Non-retryable errors: direct DLQ (JSON parse errors, illegal args)
 *
 * Manual ACK is used so an unhandled exception causes the offset NOT to be committed
 * until the error handler processes it (retry or DLQ routing).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PatientEventConsumer {

    private final PatientService patientService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics     = {"patient.admission", "patient.discharge", "patient.transfer"},
        groupId    = "patient-service-group",
        concurrency = "3"
    )
    public void consume(ConsumerRecord<String, String> record) {
        log.debug("[CONSUMER] topic={} partition={} offset={} key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        // Any exception thrown here is caught by DefaultErrorHandler,
        // which applies exponential backoff and eventually routes to DLQ.
        PatientEventMessage message = deserialize(record.value());
        patientService.processEvent(message, record.partition(), record.offset());
    }

    private PatientEventMessage deserialize(String value) {
        try {
            return objectMapper.readValue(value, PatientEventMessage.class);
        } catch (Exception e) {
            // Non-retryable — will be routed directly to DLQ
            throw new IllegalArgumentException("Failed to deserialize patient event: " + e.getMessage(), e);
        }
    }
}
