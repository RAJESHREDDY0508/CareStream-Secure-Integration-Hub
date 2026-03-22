package com.carestream.incident.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Raw threat signal received from Kafka or ingested via REST.
 * The ThreatDetectionEngine evaluates these against rules to create Incidents.
 */
@Entity
@Table(name = "threat_events", indexes = {
        @Index(name = "idx_threat_type",        columnList = "threat_type"),
        @Index(name = "idx_threat_service",     columnList = "source_service"),
        @Index(name = "idx_threat_processed",   columnList = "processed"),
        @Index(name = "idx_threat_occurred",    columnList = "occurred_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreatEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", unique = true, nullable = false, length = 100)
    private String eventId;             // alertId from Kafka payload

    @Enumerated(EnumType.STRING)
    @Column(name = "threat_type", nullable = false, length = 30)
    private ThreatType threatType;

    @Column(name = "source_service", length = 100)
    private String sourceService;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;          // Full JSON from Kafka for audit

    @Column(name = "severity", length = 20)
    private String severity;            // CRITICAL / HIGH / MEDIUM / LOW

    @Column(name = "affected_resource", length = 255)
    private String affectedResource;

    @Column(nullable = false)
    private boolean processed;          // Whether an Incident was created

    @Column(name = "incident_id", length = 50)
    private String incidentId;          // Set when processed

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @CreationTimestamp
    @Column(name = "received_at", updatable = false)
    private Instant receivedAt;
}
