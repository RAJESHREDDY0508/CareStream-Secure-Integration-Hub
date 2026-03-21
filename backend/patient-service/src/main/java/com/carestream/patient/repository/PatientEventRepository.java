package com.carestream.patient.repository;

import com.carestream.patient.entity.PatientEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PatientEventRepository extends JpaRepository<PatientEvent, UUID> {

    boolean existsByEventId(String eventId);

    Optional<PatientEvent> findByEventId(String eventId);

    Page<PatientEvent> findByPatientIdOrderByProcessedAtDesc(String patientId, Pageable pageable);
}
