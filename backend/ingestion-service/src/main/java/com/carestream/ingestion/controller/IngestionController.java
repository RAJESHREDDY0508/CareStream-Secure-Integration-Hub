package com.carestream.ingestion.controller;

import com.carestream.ingestion.dto.ADTEventRequest;
import com.carestream.ingestion.dto.EventAcceptedResponse;
import com.carestream.ingestion.service.IngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/ingest")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;

    /**
     * POST /api/v1/ingest/adt-event
     * Accepts a patient ADT event, validates it, and publishes it to Kafka.
     */
    @PostMapping("/adt-event")
    public ResponseEntity<EventAcceptedResponse> ingestAdtEvent(
            @Valid @RequestBody ADTEventRequest request) {

        log.info("[CONTROLLER] Received ADT event: type={} patient={}",
                request.eventType(), request.patientId());

        EventAcceptedResponse response = ingestionService.ingestEvent(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * GET /api/v1/ingest/health
     * Simple health check — no auth required.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ingestion-service"));
    }
}
