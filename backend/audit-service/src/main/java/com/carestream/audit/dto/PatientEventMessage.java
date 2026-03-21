package com.carestream.audit.dto;

import java.util.Map;

/**
 * Mirror of ingestion-service PatientEventMessage.
 * Each service owns its own copy — no shared library coupling.
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
