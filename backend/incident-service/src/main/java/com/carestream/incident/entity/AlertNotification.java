package com.carestream.incident.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Simulated alert notification (email / SMS / PagerDuty).
 * Persisted for audit trail — real integrations would call external APIs.
 */
@Entity
@Table(name = "alert_notifications", indexes = {
        @Index(name = "idx_notif_incident",  columnList = "incident_id"),
        @Index(name = "idx_notif_channel",   columnList = "channel"),
        @Index(name = "idx_notif_sent",      columnList = "sent_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false, length = 50)
    private String incidentId;

    @Column(nullable = false, length = 20)
    private String channel;         // EMAIL | SMS | PAGERDUTY | SLACK

    @Column(nullable = false, length = 255)
    private String recipient;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false, length = 20)
    private String status;          // SENT | FAILED | SIMULATED

    @Column(name = "sent_at")
    private Instant sentAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
