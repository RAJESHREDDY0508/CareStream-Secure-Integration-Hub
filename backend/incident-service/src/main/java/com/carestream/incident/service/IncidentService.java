package com.carestream.incident.service;

import com.carestream.incident.entity.*;
import com.carestream.incident.kafka.IncidentKafkaPublisher;
import com.carestream.incident.metrics.IncidentMetrics;
import com.carestream.incident.repository.IncidentRepository;
import com.carestream.incident.repository.ThreatEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository       incidentRepository;
    private final ThreatEventRepository    threatEventRepository;
    private final ThreatDetectionEngine    detectionEngine;
    private final AlertNotificationService alertService;
    private final IncidentKafkaPublisher   kafkaPublisher;
    private final IncidentMetrics          incidentMetrics;

    // ── Auto-creation from ThreatEvent ───────────────────────────────────

    /**
     * Evaluate a ThreatEvent and auto-create an Incident if detection rules match.
     * Idempotent: if an Incident already exists for this alertId, returns it.
     */
    @Transactional
    public Incident processTheatEvent(ThreatEvent event) {
        // Idempotency check
        if (incidentRepository.existsBySourceAlertId(event.getEventId())) {
            return incidentRepository.findBySourceAlertId(event.getEventId()).orElseThrow();
        }

        if (!detectionEngine.shouldCreateIncident(event)) {
            log.debug("[DETECTION] Event {} below incident threshold (sev={}, type={})",
                    event.getEventId(), event.getSeverity(), event.getThreatType());
            markEventProcessed(event, null);
            return null;
        }

        Incident incident = Incident.builder()
                .incidentId(generateIncidentId())
                .title(detectionEngine.generateTitle(event))
                .description(event.getDescription())
                .severity(detectionEngine.resolveIncidentSeverity(event.getSeverity()))
                .status(IncidentStatus.OPEN)
                .threatType(event.getThreatType())
                .sourceService(event.getSourceService())
                .sourceAlertId(event.getEventId())
                .affectedResource(event.getAffectedResource())
                .detectedAt(event.getOccurredAt() != null ? event.getOccurredAt() : Instant.now())
                .build();

        incident = incidentRepository.save(incident);
        markEventProcessed(event, incident.getIncidentId());
        incidentMetrics.recordIncidentCreated(incident.getSeverity().name());

        log.info("[INCIDENT] Created {} ({}) from threat event {} — {}",
                incident.getIncidentId(), incident.getSeverity(),
                event.getEventId(), incident.getTitle());

        // Send alerts
        alertService.sendIncidentAlerts(incident);

        // Publish to Kafka
        kafkaPublisher.publishIncidentCreated(incident);

        return incident;
    }

    // ── CRUD & Lifecycle ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Incident getByIncidentId(String incidentId) {
        return incidentRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));
    }

    @Transactional(readOnly = true)
    public Page<Incident> findAll(String status, String severity, String threatType,
                                  String service, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());

        IncidentStatus statusEnum   = status     != null ? IncidentStatus.valueOf(status.toUpperCase())       : null;
        IncidentSeverity sevEnum    = severity   != null ? IncidentSeverity.valueOf(severity.toUpperCase())    : null;
        ThreatType typeEnum         = threatType != null ? ThreatType.valueOf(threatType.toUpperCase())        : null;

        return incidentRepository.findWithFilters(statusEnum, sevEnum, typeEnum, service, pr);
    }

    @Transactional
    public Incident startInvestigation(String incidentId, String assigneeId) {
        Incident incident = getByIncidentId(incidentId);
        validateTransition(incident.getStatus(), IncidentStatus.INVESTIGATING);

        incident.setStatus(IncidentStatus.INVESTIGATING);
        incident.setAssigneeId(assigneeId);
        incident.setInvestigationStartedAt(Instant.now());
        incident = incidentRepository.save(incident);

        log.info("[INCIDENT] {} → INVESTIGATING (assignee={})", incidentId, assigneeId);
        kafkaPublisher.publishStatusUpdate(incident, "OPEN");
        return incident;
    }

    @Transactional
    public Incident contain(String incidentId, String notes) {
        Incident incident = getByIncidentId(incidentId);
        validateTransition(incident.getStatus(), IncidentStatus.CONTAINED);

        incident.setStatus(IncidentStatus.CONTAINED);
        incident.setContainedAt(Instant.now());
        if (notes != null) incident.setResolutionNotes(notes);
        incident = incidentRepository.save(incident);

        log.info("[INCIDENT] {} → CONTAINED", incidentId);
        kafkaPublisher.publishStatusUpdate(incident, "INVESTIGATING");
        return incident;
    }

    @Transactional
    public Incident resolve(String incidentId, String resolutionNotes) {
        Incident incident = getByIncidentId(incidentId);
        validateTransition(incident.getStatus(), IncidentStatus.RESOLVED);

        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(Instant.now());
        incident.setResolutionNotes(resolutionNotes);
        incident = incidentRepository.save(incident);
        incidentMetrics.recordIncidentResolved();

        log.info("[INCIDENT] {} → RESOLVED", incidentId);
        kafkaPublisher.publishIncidentResolved(incident);
        return incident;
    }

    @Transactional
    public Incident markFalsePositive(String incidentId, String notes) {
        Incident incident = getByIncidentId(incidentId);
        incident.setStatus(IncidentStatus.FALSE_POSITIVE);
        incident.setResolutionNotes(notes);
        incident.setResolvedAt(Instant.now());
        incident = incidentRepository.save(incident);

        log.info("[INCIDENT] {} → FALSE_POSITIVE", incidentId);
        return incident;
    }

    // ── Stats (dashboard data) ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        long total      = incidentRepository.count();
        long open       = incidentRepository.countByStatus(IncidentStatus.OPEN);
        long investing  = incidentRepository.countByStatus(IncidentStatus.INVESTIGATING);
        long contained  = incidentRepository.countByStatus(IncidentStatus.CONTAINED);
        long resolved   = incidentRepository.countByStatus(IncidentStatus.RESOLVED);
        long falsePos   = incidentRepository.countByStatus(IncidentStatus.FALSE_POSITIVE);
        long active     = incidentRepository.countActive();

        Map<String, Long> bySeverity = Map.of(
                "CRITICAL", incidentRepository.countBySeverity(IncidentSeverity.CRITICAL),
                "HIGH",     incidentRepository.countBySeverity(IncidentSeverity.HIGH),
                "MEDIUM",   incidentRepository.countBySeverity(IncidentSeverity.MEDIUM),
                "LOW",      incidentRepository.countBySeverity(IncidentSeverity.LOW)
        );

        List<Incident> openCritical = incidentRepository.findOpenCritical();

        // MTTR: avg hours from detectedAt → resolvedAt for resolved incidents
        // (simplified — production would use DB aggregation)
        double mttrHours = computeMttrHours();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total",         total);
        stats.put("open",          open);
        stats.put("investigating", investing);
        stats.put("contained",     contained);
        stats.put("resolved",      resolved);
        stats.put("falsePositive", falsePos);
        stats.put("active",        active);
        stats.put("bySeverity",    bySeverity);
        stats.put("openCriticalCount", openCritical.size());
        stats.put("mttrHours",     mttrHours);
        return stats;
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private String generateIncidentId() {
        return "INC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private void markEventProcessed(ThreatEvent event, String incidentId) {
        event.setProcessed(true);
        event.setIncidentId(incidentId);
        threatEventRepository.save(event);
    }

    private void validateTransition(IncidentStatus current, IncidentStatus target) {
        boolean valid = switch (target) {
            case INVESTIGATING -> current == IncidentStatus.OPEN;
            case CONTAINED     -> current == IncidentStatus.INVESTIGATING;
            case RESOLVED      -> current == IncidentStatus.CONTAINED
                                   || current == IncidentStatus.INVESTIGATING;
            default            -> true;
        };
        if (!valid) {
            throw new IllegalStateException(
                    String.format("Cannot transition from %s to %s", current, target));
        }
    }

    private double computeMttrHours() {
        List<Incident> resolved = incidentRepository.findByStatusIn(
                List.of(IncidentStatus.RESOLVED));
        if (resolved.isEmpty()) return 0.0;

        double totalHours = resolved.stream()
                .filter(i -> i.getDetectedAt() != null && i.getResolvedAt() != null)
                .mapToDouble(i -> (i.getResolvedAt().toEpochMilli() - i.getDetectedAt().toEpochMilli())
                                  / 3_600_000.0)
                .average()
                .orElse(0.0);

        return Math.round(totalHours * 100.0) / 100.0;
    }
}
