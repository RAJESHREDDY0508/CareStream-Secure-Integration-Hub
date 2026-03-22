package com.carestream.incident.kafka;

import com.carestream.incident.entity.Incident;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes incident lifecycle events to Kafka.
 *
 * Topics:
 *  - incident.events   : INCIDENT_CREATED, INCIDENT_UPDATED, INCIDENT_RESOLVED
 *  - security.alerts   : Re-publishes CRITICAL incidents to the shared alert bus
 */
@Slf4j
@Component
public class IncidentKafkaPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.incident-events:incident.events}")
    private String incidentEventsTopic;

    @Value("${kafka.topics.security-alerts:security.alerts}")
    private String securityAlertsTopic;

    public IncidentKafkaPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper  = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void publishIncidentCreated(Incident incident) {
        Map<String, Object> payload = buildBasePayload(incident, "INCIDENT_CREATED");
        publish(incidentEventsTopic, incident.getIncidentId(), payload);

        // Re-publish CRITICAL and HIGH to the shared security.alerts bus
        if (incident.getSeverity().name().equals("CRITICAL") ||
            incident.getSeverity().name().equals("HIGH")) {
            Map<String, Object> alert = buildAlertPayload(incident, "INCIDENT_CREATED");
            publish(securityAlertsTopic, incident.getIncidentId(), alert);
        }
    }

    public void publishStatusUpdate(Incident incident, String previousStatus) {
        Map<String, Object> payload = buildBasePayload(incident, "INCIDENT_UPDATED");
        payload.put("previousStatus", previousStatus);
        publish(incidentEventsTopic, incident.getIncidentId(), payload);
    }

    public void publishIncidentResolved(Incident incident) {
        Map<String, Object> payload = buildBasePayload(incident, "INCIDENT_RESOLVED");
        if (incident.getDetectedAt() != null && incident.getResolvedAt() != null) {
            long mttrMinutes = (incident.getResolvedAt().toEpochMilli()
                                - incident.getDetectedAt().toEpochMilli()) / 60_000;
            payload.put("mttrMinutes", mttrMinutes);
        }
        publish(incidentEventsTopic, incident.getIncidentId(), payload);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private Map<String, Object> buildBasePayload(Incident incident, String eventType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId",       UUID.randomUUID().toString());
        payload.put("eventType",     eventType);
        payload.put("incidentId",    incident.getIncidentId());
        payload.put("title",         incident.getTitle());
        payload.put("severity",      incident.getSeverity().name());
        payload.put("status",        incident.getStatus().name());
        payload.put("threatType",    incident.getThreatType().name());
        payload.put("sourceService", incident.getSourceService());
        payload.put("detectedAt",    incident.getDetectedAt());
        payload.put("timestamp",     Instant.now().toString());
        return payload;
    }

    private Map<String, Object> buildAlertPayload(Incident incident, String alertType) {
        Map<String, Object> alert = new LinkedHashMap<>();
        alert.put("alertId",       UUID.randomUUID().toString());
        alert.put("alertType",     alertType);
        alert.put("severity",      incident.getSeverity().name());
        alert.put("sourceService", "incident-service");
        alert.put("incidentId",    incident.getIncidentId());
        alert.put("description",   incident.getTitle());
        alert.put("detectedAt",    incident.getDetectedAt());
        alert.put("timestamp",     Instant.now().toString());
        return alert;
    }

    private void publish(String topic, String key, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, json);
            log.debug("[KAFKA] Published to {} key={} type={}",
                    topic, key, payload.get("eventType"));
        } catch (Exception e) {
            log.error("[KAFKA] Failed to publish to {}: {}", topic, e.getMessage(), e);
        }
    }
}
