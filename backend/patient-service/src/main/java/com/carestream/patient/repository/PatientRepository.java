package com.carestream.patient.repository;

import com.carestream.patient.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByPatientId(String patientId);

    Page<Patient> findByCurrentStatus(String currentStatus, Pageable pageable);

    Page<Patient> findByCurrentWard(String ward, Pageable pageable);
}
