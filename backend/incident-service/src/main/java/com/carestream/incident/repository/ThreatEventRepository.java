package com.carestream.incident.repository;

import com.carestream.incident.entity.ThreatEvent;
import com.carestream.incident.entity.ThreatType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ThreatEventRepository extends JpaRepository<ThreatEvent, Long> {

    Optional<ThreatEvent> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    List<ThreatEvent> findByProcessedFalseOrderByReceivedAtAsc();

    Page<ThreatEvent> findByThreatType(ThreatType threatType, Pageable pageable);

    Page<ThreatEvent> findBySourceService(String sourceService, Pageable pageable);

    long countByProcessed(boolean processed);
}
