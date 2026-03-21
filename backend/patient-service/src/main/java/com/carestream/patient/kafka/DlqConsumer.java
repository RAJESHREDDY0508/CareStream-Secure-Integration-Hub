package com.carestream.patient.kafka;

import com.carestream.patient.entity.DlqEntry;
import com.carestream.patient.repository.DlqEntryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * Monitors the dlq.patient.events topic.
 * Persists every dead-lettered message to the database for:
 *   - Dashboard visibility ("N messages in DLQ")
 *   - Manual investigation
 *   - Reprocess or discard via REST API
 *
 * Spring Kafka adds these diagnostic headers automatically:
 *   kafka_dlt-exception-fqcn        — fully qualified exception class
 *   kafka_dlt-exception-message     — exception message
 *   kafka_dlt-original-topic        — original topic
 *   kafka_dlt-original-partition    — original partition
 *   kafka_dlt-original-offset       — original offset
 *   kafka_dlt-original-consumer-group — consumer group
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DlqConsumer {

    private final DlqEntryRepository dlqEntryRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics  = "dlq.patient.events",
        groupId = "dlq-monitor-group"
    )
    public void consume(ConsumerRecord<String, String> record) {
        log.warn("[DLQ-CONSUMER] Dead-lettered message | partition={} offset={} key={}",
                record.partition(), record.offset(), record.key());

        try {
            String errorClass     = header(record, "kafka_dlt-exception-fqcn");
            String errorMessage   = header(record, "kafka_dlt-exception-message");
            String originalTopic  = header(record, "kafka_dlt-original-topic");
            String originalPart   = header(record, "kafka_dlt-original-partition");
            String originalOffset = header(record, "kafka_dlt-original-offset");
            String consumerGroup  = header(record, "kafka_dlt-original-consumer-group");

            // Parse original payload
            Map<String, Object> payload = null;
            String eventId   = null;
            String patientId = record.key();

            try {
                payload  = objectMapper.readValue(record.value(), new TypeReference<>() {});
                eventId  = (String) payload.get("eventId");
                if (patientId == null) patientId = (String) payload.get("patientId");
            } catch (Exception e) {
                log.warn("[DLQ-CONSUMER] Could not parse payload: {}", e.getMessage());
                payload = Map.of("raw", record.value() != null ? record.value() : "null");
            }

            DlqEntry entry = DlqEntry.builder()
                    .eventId(eventId)
                    .patientId(patientId)
                    .originalTopic(originalTopic != null ? originalTopic : record.topic())
                    .originalPartition(originalPart != null ? Integer.parseInt(originalPart) : record.partition())
                    .originalOffset(originalOffset != null ? Long.parseLong(originalOffset) : record.offset())
                    .originalConsumerGroup(consumerGroup)
                    .errorClass(errorClass)
                    .errorMessage(errorMessage)
                    .originalPayload(payload)
                    .dlqPublishedAt(Instant.now())
                    .status("PENDING")
                    .build();

            dlqEntryRepository.save(entry);
            log.warn("[DLQ-CONSUMER] Persisted DLQ entry id={} eventId={} errorClass={}",
                    entry.getId(), eventId, errorClass);

        } catch (Exception e) {
            log.error("[DLQ-CONSUMER] Failed to persist DLQ entry: {}", e.getMessage(), e);
        }
    }

    private String header(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header == null) return null;
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
