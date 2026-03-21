package com.carestream.audit.repository;

import com.carestream.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    boolean existsByAuditId(String auditId);

    Page<AuditLog> findByActorId(String actorId, Pageable pageable);

    Page<AuditLog> findByResourceId(String resourceId, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByTimestampBetween(Instant from, Instant to, Pageable pageable);
}
