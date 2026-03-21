package com.carestream.audit.service;

import com.carestream.audit.dto.PatientEventMessage;
import com.carestream.audit.entity.AuditLog;
import com.carestream.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logPatientEvent(PatientEventMessage message) {
        String auditId = UUID.randomUUID().toString();

        // Idempotency — one audit entry per eventId
        if (auditLogRepository.existsByAuditId(message.eventId() + "-audit")) {
            log.warn("[AUDIT-SVC] Duplicate audit skipped for eventId={}", message.eventId());
            return;
        }

        AuditLog log = AuditLog.builder()
                .auditId(message.eventId() + "-audit")
                .eventId(message.eventId())
                .action("PATIENT_" + message.eventType())
                .actorId(message.source())
                .actorRole("SERVICE")
                .resource("/patients/" + message.patientId())
                .resourceId(message.patientId())
                .outcome("SUCCESS")
                .details(Map.of(
                        "eventType", message.eventType(),
                        "correlationId", message.correlationId(),
                        "publishedBy", message.publishedBy()
                ))
                .timestamp(Instant.parse(message.timestamp()))
                .build();

        auditLogRepository.save(log);
        AuditService.log.info("[AUDIT-SVC] Logged audit for event={} patient={} action={}",
                message.eventId(), message.patientId(), log.getAction());
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
