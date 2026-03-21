package com.carestream.ingestion.service;

import com.carestream.ingestion.dto.ADTEventRequest;
import com.carestream.ingestion.dto.EventAcceptedResponse;
import com.carestream.ingestion.dto.PatientEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private static final Map<String, String> TOPIC_MAP = Map.of(
            "ADMISSION",  "patient.admission",
            "DISCHARGE",  "patient.discharge",
            "TRANSFER",   "patient.transfer",
            "LAB_UPDATE", "patient.admission"
    );

    private final KafkaTemplate<String, PatientEventMessage> kafkaTemplate;

    public EventAcceptedResponse ingestEvent(ADTEventRequest request) {
        String eventId       = UUID.randomUUID().toString();
        String correlationId = request.correlationId() != null
                ? request.correlationId()
                : UUID.randomUUID().toString();
        String timestamp     = Instant.now().toString();
        String topic         = TOPIC_MAP.getOrDefault(request.eventType(), "patient.admission");

        PatientEventMessage message = new PatientEventMessage(
                eventId,
                request.eventType(),
                request.patientId(),
                correlationId,
                timestamp,
                request.source(),
                "ingestion-service",
                request.payload()
        );

        log.info("[INGESTION] Publishing event {} type={} patient={} topic={}",
                eventId, request.eventType(), request.patientId(), topic);

        try {
            SendResult<String, PatientEventMessage> result =
                    kafkaTemplate.send(topic, request.patientId(), message).get();

            RecordMetadata meta = result.getRecordMetadata();
            log.info("[INGESTION] Published event={} partition={} offset={}",
                    eventId, meta.partition(), meta.offset());

            return new EventAcceptedResponse(
                    eventId,
                    correlationId,
                    request.patientId(),
                    request.eventType(),
                    topic,
                    meta.partition(),
                    meta.offset(),
                    "ACCEPTED",
                    timestamp
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Kafka publish interrupted for event " + eventId, e);
        } catch (ExecutionException e) {
            log.error("[INGESTION] Failed to publish event={} error={}", eventId, e.getMessage());
            throw new RuntimeException("Failed to publish event " + eventId, e.getCause());
        }
    }
}
