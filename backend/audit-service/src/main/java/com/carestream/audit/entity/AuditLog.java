package com.carestream.audit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_logs",
       indexes = {
           @Index(name = "idx_audit_actor",     columnList = "actor_id"),
           @Index(name = "idx_audit_resource",  columnList = "resource_id"),
           @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
           @Index(name = "idx_audit_action",    columnList = "action")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "audit_id", unique = true, nullable = false, length = 255)
    private String auditId;

    @Column(name = "event_id", length = 255)
    private String eventId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "actor_id", nullable = false, length = 255)
    private String actorId;

    @Column(name = "actor_role", nullable = false, length = 50)
    private String actorRole;

    @Column(name = "resource", length = 500)
    private String resource;

    @Column(name = "resource_id", length = 255)
    private String resourceId;

    @Column(name = "outcome", nullable = false, length = 20)
    private String outcome;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
