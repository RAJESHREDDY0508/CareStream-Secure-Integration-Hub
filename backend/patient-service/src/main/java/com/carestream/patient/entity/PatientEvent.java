package com.carestream.patient.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "patient_events",
       indexes = {
           @Index(name = "idx_pe_patient_id", columnList = "patient_id"),
           @Index(name = "idx_pe_event_type", columnList = "event_type"),
           @Index(name = "idx_pe_processed_at", columnList = "processed_at")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", unique = true, nullable = false, length = 255)
    private String eventId;

    @Column(name = "patient_id", nullable = false, length = 50)
    private String patientId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "correlation_id", nullable = false, length = 255)
    private String correlationId;

    @Column(name = "source", length = 100)
    private String source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "kafka_partition")
    private Integer kafkaPartition;

    @Column(name = "kafka_offset")
    private Long kafkaOffset;

    @PrePersist
    void prePersist() {
        processedAt = Instant.now();
    }
}
