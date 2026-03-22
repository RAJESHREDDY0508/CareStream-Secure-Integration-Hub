package com.carestream.incident.repository;

import com.carestream.incident.entity.Incident;
import com.carestream.incident.entity.IncidentSeverity;
import com.carestream.incident.entity.IncidentStatus;
import com.carestream.incident.entity.ThreatType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findByIncidentId(String incidentId);

    Optional<Incident> findBySourceAlertId(String sourceAlertId);

    Page<Incident> findByStatus(IncidentStatus status, Pageable pageable);

    Page<Incident> findBySeverity(IncidentSeverity severity, Pageable pageable);

    Page<Incident> findByThreatType(ThreatType threatType, Pageable pageable);

    Page<Incident> findByStatusAndSeverity(IncidentStatus status, IncidentSeverity severity, Pageable pageable);

    @Query("SELECT i FROM Incident i WHERE " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:severity IS NULL OR i.severity = :severity) AND " +
           "(:threatType IS NULL OR i.threatType = :threatType) AND " +
           "(:service IS NULL OR i.sourceService = :service)")
    Page<Incident> findWithFilters(
            @Param("status")     IncidentStatus status,
            @Param("severity")   IncidentSeverity severity,
            @Param("threatType") ThreatType threatType,
            @Param("service")    String service,
            Pageable pageable);

    List<Incident> findByStatusIn(List<IncidentStatus> statuses);

    long countByStatus(IncidentStatus status);

    long countBySeverity(IncidentSeverity severity);

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.status IN ('OPEN', 'INVESTIGATING', 'CONTAINED')")
    long countActive();

    @Query("SELECT i FROM Incident i WHERE i.createdAt >= :since ORDER BY i.createdAt DESC")
    List<Incident> findRecentIncidents(@Param("since") Instant since);

    @Query("SELECT i FROM Incident i WHERE i.status = 'OPEN' AND i.severity = 'CRITICAL'")
    List<Incident> findOpenCritical();

    boolean existsBySourceAlertId(String sourceAlertId);
}
