package com.carestream.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

public record ADTEventRequest(

        @NotBlank(message = "eventType is required")
        @Pattern(
            regexp = "ADMISSION|DISCHARGE|TRANSFER|LAB_UPDATE",
            message = "eventType must be ADMISSION, DISCHARGE, TRANSFER, or LAB_UPDATE"
        )
        String eventType,

        @NotBlank(message = "patientId is required")
        @Pattern(regexp = "^P-\\d{5}$", message = "patientId must match P-NNNNN format")
        String patientId,

        @NotBlank(message = "source is required")
        String source,

        // Optional — auto-generated if absent
        String correlationId,

        @NotNull(message = "payload is required")
        Map<String, Object> payload
) {}
