package com.carestream.patient.repository;

import com.carestream.patient.entity.DlqEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface DlqEntryRepository extends JpaRepository<DlqEntry, UUID> {

    Page<DlqEntry> findByStatus(String status, Pageable pageable);

    Page<DlqEntry> findByPatientId(String patientId, Pageable pageable);

    Page<DlqEntry> findByOriginalTopic(String topic, Pageable pageable);

    long countByStatus(String status);

    @Query("SELECT d.originalTopic, COUNT(d) FROM DlqEntry d GROUP BY d.originalTopic")
    java.util.List<Object[]> countByTopic();
}
