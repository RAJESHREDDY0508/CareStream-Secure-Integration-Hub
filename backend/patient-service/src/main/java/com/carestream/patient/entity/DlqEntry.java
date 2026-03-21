package com.carestream.patient.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Persisted record of a dead-lettered Kafka message.
 * Populated by DlqConsumer when a message arrives in dlq.patient.events.
 * Provides a REST-queryable audit trail of all processing failures.
 */
@Entity
@Table(name = "dlq_entries",
       indexes = {
           @Index(name = "idx_dlq_patient_id",  columnList = "patient_id"),
           @Index(name = "idx_dlq_status",       columnList = "status"),
           @Index(name = "idx_dlq_published_at", columnList = "dlq_published_at")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DlqEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ─── Original message metadata ─────────────────────────

    @Column(name = "event_id", length = 255)
    private String eventId;

    @Column(name = "patient_id", length = 50)
    private String patientId;

    @Column(name = "original_topic", nullable = false, length = 200)
    private String originalTopic;

    @Column(name = "original_partition")
    private Integer originalPartition;

    @Column(name = "original_offset")
    private Long originalOffset;

    @Column(name = "original_consumer_group", length = 200)
    private String originalConsumerGroup;

    // ─── Error information (from Spring Kafka DLT headers) ─

    @Column(name = "error_class", length = 500)
    private String errorClass;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    // ─── Original payload ──────────────────────────────────

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "original_payload", columnDefinition = "jsonb")
    private Map<String, Object> originalPayload;

    // ─── DLQ lifecycle ─────────────────────────────────────

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING";   // PENDING | REPROCESSED | DISCARDED

    @Column(name = "dlq_published_at", nullable = false)
    private Instant dlqPublishedAt;

    @Column(name = "reprocessed_at")
    private Instant reprocessedAt;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
