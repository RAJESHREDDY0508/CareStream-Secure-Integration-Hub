package com.carestream.audit.kafka;

import com.carestream.audit.dto.PatientEventMessage;
import com.carestream.audit.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    /**
     * Listens to all patient ADT topics to build an immutable audit trail.
     * Separate consumer group from patient-service — each gets all messages.
     */
    @KafkaListener(
        topics = {"patient.admission", "patient.discharge", "patient.transfer"},
        groupId = "audit-service-group",
        concurrency = "3"
    )
    public void consume(ConsumerRecord<String, String> record) {
        log.debug("[AUDIT-CONSUMER] topic={} partition={} offset={} key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            PatientEventMessage message = objectMapper.readValue(record.value(), PatientEventMessage.class);
            auditService.logPatientEvent(message);
        } catch (Exception e) {
            log.error("[AUDIT-CONSUMER] Failed to process record topic={} offset={} error={}",
                    record.topic(), record.offset(), e.getMessage(), e);
            // Phase 3: route to DLQ
        }
    }
}
