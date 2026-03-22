package com.carestream.incident.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Core incident entity. Created automatically from security.alerts Kafka events
 * or manually via REST. Lifecycle: OPEN → INVESTIGATING → CONTAINED → RESOLVED.
 */
@Entity
@Table(name = "incidents", indexes = {
        @Index(name = "idx_incident_status",        columnList = "status"),
        @Index(name = "idx_incident_severity",      columnList = "severity"),
        @Index(name = "idx_incident_type",          columnList = "threat_type"),
        @Index(name = "idx_incident_source",        columnList = "source_service"),
        @Index(name = "idx_incident_created",       columnList = "created_at"),
        @Index(name = "idx_incident_id",            columnList = "incident_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", unique = true, nullable = false, length = 50)
    private String incidentId;          // INC-XXXXXXXX

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "threat_type", nullable = false, length = 30)
    private ThreatType threatType;

    @Column(name = "source_service", length = 100)
    private String sourceService;       // Which microservice triggered this

    @Column(name = "source_alert_id", length = 100)
    private String sourceAlertId;       // correlationId from security.alerts event

    @Column(name = "affected_resource", length = 255)
    private String affectedResource;    // CVE ID, endpoint, user ID, etc.

    @Column(name = "assignee_id", length = 100)
    private String assigneeId;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    // Timestamps for MTTD / MTTR KPI calculations
    @Column(name = "detected_at")
    private Instant detectedAt;

    @Column(name = "investigation_started_at")
    private Instant investigationStartedAt;

    @Column(name = "contained_at")
    private Instant containedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
