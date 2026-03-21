package com.carestream.ingestion.dto;

import java.util.Map;

/**
 * The canonical Kafka message published to all patient.* topics.
 * Consumers (patient-service, audit-service) deserialize this same structure.
 */
public record PatientEventMessage(
        String eventId,
        String eventType,
        String patientId,
        String correlationId,
        String timestamp,
        String source,
        String publishedBy,
        Map<String, Object> payload
) {}
