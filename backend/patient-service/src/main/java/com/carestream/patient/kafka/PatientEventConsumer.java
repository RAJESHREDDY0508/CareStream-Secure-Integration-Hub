package com.carestream.patient.kafka;

import com.carestream.patient.dto.PatientEventMessage;
import com.carestream.patient.service.PatientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientEventConsumer {

    private final PatientService patientService;
    private final ObjectMapper objectMapper;

    /**
     * Listens to all patient ADT topics.
     * Messages are deserialized from JSON String → PatientEventMessage.
     * Partition key = patientId ensures ordered processing per patient.
     */
    @KafkaListener(
        topics = {"patient.admission", "patient.discharge", "patient.transfer"},
        groupId = "patient-service-group",
        concurrency = "3"
    )
    public void consume(ConsumerRecord<String, String> record) {
        String topic     = record.topic();
        int partition    = record.partition();
        long offset      = record.offset();
        String key       = record.key();
        String value     = record.value();

        log.debug("[CONSUMER] Received topic={} partition={} offset={} key={}",
                topic, partition, offset, key);

        try {
            PatientEventMessage message = objectMapper.readValue(value, PatientEventMessage.class);
            patientService.processEvent(message, partition, offset);
        } catch (Exception e) {
            log.error("[CONSUMER] Failed to process record topic={} offset={} error={}",
                    topic, offset, e.getMessage(), e);
            // Phase 3: route to DLQ here
        }
    }
}
