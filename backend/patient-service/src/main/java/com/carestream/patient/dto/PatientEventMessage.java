package com.carestream.patient.dto;

import java.util.Map;

/**
 * Mirrors the message published by ingestion-service.
 * Each service defines its own copy — no shared library coupling.
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
