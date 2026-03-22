package com.carestream.incident.kafka;

import com.carestream.incident.entity.ThreatEvent;
import com.carestream.incident.entity.ThreatType;
import com.carestream.incident.metrics.IncidentMetrics;
import com.carestream.incident.repository.ThreatEventRepository;
import com.carestream.incident.service.IncidentService;
import com.carestream.incident.service.ThreatDetectionEngine;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Kafka consumer for the shared security.alerts topic.
 *
 * Messages are published by:
 *  - vulnerability-service  (SLA breaches, CRITICAL/HIGH CVEs)
 *  - incident-service itself (re-publishing resolved incidents)
 *  - Future services (auth anomalies, ingestion spikes, etc.)
 *
 * Each message is:
 *  1. Persisted as a ThreatEvent (raw audit trail)
 *  2. Evaluated by ThreatDetectionEngine
 *  3. Auto-escalated to an Incident if rules match
 *
 * Consumer group: incident-service-group
 * Partition strategy: each alert processed exactly once (manual ACK)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityAlertConsumer {

    private final ThreatEventRepository threatEventRepository;
    private final ThreatDetectionEngine  detectionEngine;
    private final IncidentService        incidentService;
    private final IncidentMetrics        incidentMetrics;
    private final ObjectMapper           objectMapper;

    @KafkaListener(
            topics   = "${kafka.topics.security-alerts:security.alerts}",
            groupId  = "${spring.kafka.consumer.group-id:incident-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSecurityAlert(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String key     = record.key();
        String payload = record.value();
        log.info("[CONSUMER] Received security.alerts key={} partition={} offset={}",
                key, record.partition(), record.offset());

        try {
            Map<String, Object> alert = objectMapper.readValue(
                    payload, new TypeReference<>() {});

            // Skip events we published ourselves to avoid processing loops
            String sourceService = (String) alert.getOrDefault("sourceService", "");
            if ("incident-service".equals(sourceService) && alert.containsKey("incidentId")) {
                log.debug("[CONSUMER] Skipping self-published incident event key={}", key);
                ack.acknowledge();
                return;
            }

            String eventId = (String) alert.getOrDefault("alertId", key);

            // Idempotency check
            if (threatEventRepository.existsByEventId(eventId)) {
                log.debug("[CONSUMER] Duplicate event ignored: {}", eventId);
                ack.acknowledge();
                return;
            }

            String alertType  = (String) alert.getOrDefault("alertType", "UNKNOWN");
            String severity   = (String) alert.getOrDefault("severity", "MEDIUM");
            String desc       = (String) alert.getOrDefault("description", "Security alert received");
            String affected   = extractAffectedResource(alert);
            Instant occurred  = parseInstant(alert.get("detectedAt"));

            ThreatType threatType = detectionEngine.resolveThreatType(alertType);

            ThreatEvent event = ThreatEvent.builder()
                    .eventId(eventId)
                    .threatType(threatType)
                    .sourceService(sourceService)
                    .description(desc)
                    .severity(severity)
                    .affectedResource(affected)
                    .rawPayload(payload)
                    .processed(false)
                    .occurredAt(occurred)
                    .build();

            threatEventRepository.save(event);
            incidentMetrics.recordThreatEventReceived();
            incidentService.processTheatEvent(event);

            ack.acknowledge();

        } catch (Exception e) {
            log.error("[CONSUMER] Failed to process security alert key={}: {}", key, e.getMessage(), e);
            // Do NOT ACK — let Kafka retry (up to max.poll.records retries)
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private String extractAffectedResource(Map<String, Object> alert) {
        // Try common field names from vulnerability-service's publisher
        for (String field : new String[]{"cveId", "findingId", "affectedComponent", "resource"}) {
            Object val = alert.get(field);
            if (val != null) return val.toString();
        }
        return (String) alert.getOrDefault("alertId", "unknown");
    }

    private Instant parseInstant(Object value) {
        if (value == null) return Instant.now();
        try {
            return Instant.parse(value.toString());
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
