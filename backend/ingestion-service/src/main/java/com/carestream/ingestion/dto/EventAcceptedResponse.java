package com.carestream.ingestion.dto;

public record EventAcceptedResponse(
        String eventId,
        String correlationId,
        String patientId,
        String eventType,
        String topic,
        Integer partition,
        Long offset,
        String status,
        String timestamp
) {}
