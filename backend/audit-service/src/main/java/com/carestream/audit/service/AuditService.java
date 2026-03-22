package com.carestream.audit.service;

import com.carestream.audit.dto.PatientEventMessage;
import com.carestream.audit.entity.AuditLog;
import com.carestream.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final KafkaTemplate<String, String> auditEventsKafkaTemplate;
    private final ObjectMapper objectMapper;

    public AuditService(
            AuditLogRepository auditLogRepository,
            @Qualifier("auditEventsKafkaTemplate") KafkaTemplate<String, String> auditEventsKafkaTemplate,
            ObjectMapper objectMapper) {
        this.auditLogRepository       = auditLogRepository;
        this.auditEventsKafkaTemplate = auditEventsKafkaTemplate;
        this.objectMapper             = objectMapper;
    }

    @Transactional
    public void logPatientEvent(PatientEventMessage message) {

        // Idempotency — one audit entry per eventId
        if (auditLogRepository.existsByAuditId(message.eventId() + "-audit")) {
            log.warn("[AUDIT-SVC] Duplicate audit skipped for eventId={}", message.eventId());
            return;
        }

        AuditLog auditLog = AuditLog.builder()
                .auditId(message.eventId() + "-audit")
                .eventId(message.eventId())
                .action("PATIENT_" + message.eventType())
                .actorId(message.source())
                .actorRole("SERVICE")
                .resource("/patients/" + message.patientId())
                .resourceId(message.patientId())
                .outcome("SUCCESS")
                .details(Map.of(
                        "eventType",     message.eventType(),
                        "correlationId", message.correlationId(),
                        "publishedBy",   message.publishedBy()
                ))
                .timestamp(Instant.parse(message.timestamp()))
                .build();

        auditLogRepository.save(auditLog);
        log.info("[AUDIT-SVC] Logged audit for event={} patient={} action={}",
                message.eventId(), message.patientId(), auditLog.getAction());

        // ── Publish to audit.events so Kafka UI / downstream consumers see it ──
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("auditId",    auditLog.getAuditId());
            payload.put("eventId",    auditLog.getEventId());
            payload.put("action",     auditLog.getAction());
            payload.put("actorId",    auditLog.getActorId());
            payload.put("actorRole",  auditLog.getActorRole());
            payload.put("resource",   auditLog.getResource());
            payload.put("resourceId", auditLog.getResourceId());
            payload.put("outcome",    auditLog.getOutcome());
            payload.put("timestamp",  auditLog.getTimestamp().toString());
            payload.put("details",    auditLog.getDetails());

            String json = objectMapper.writeValueAsString(payload);
            auditEventsKafkaTemplate.send("audit.events", auditLog.getResourceId(), json);
            log.info("[AUDIT-SVC] Published to audit.events auditId={} resourceId={}",
                    auditLog.getAuditId(), auditLog.getResourceId());
        } catch (Exception e) {
            log.error("[AUDIT-SVC] Failed to publish to audit.events for auditId={}: {}",
                    auditLog.getAuditId(), e.getMessage(), e);
        }
    }

    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    public Page<AuditLog> findByActorId(String actorId, Pageable pageable) {
        return auditLogRepository.findByActorId(actorId, pageable);
    }

    public Page<AuditLog> findByResourceId(String resourceId, Pageable pageable) {
        return auditLogRepository.findByResourceId(resourceId, pageable);
    }
}
