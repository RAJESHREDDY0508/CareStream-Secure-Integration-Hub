package com.carestream.patient.service;

import com.carestream.patient.dto.PatientEventMessage;
import com.carestream.patient.entity.Patient;
import com.carestream.patient.entity.PatientEvent;
import com.carestream.patient.repository.PatientEventRepository;
import com.carestream.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientEventRepository patientEventRepository;

    /**
     * Processes an incoming ADT event.
     * Idempotent — skips duplicate eventIds.
     */
    @Transactional
    public void processEvent(PatientEventMessage message, int partition, long offset) {
        // Idempotency check
        if (patientEventRepository.existsByEventId(message.eventId())) {
            log.warn("[PATIENT-SVC] Duplicate event skipped: eventId={}", message.eventId());
            return;
        }

        // Upsert patient state
        Patient patient = patientRepository.findByPatientId(message.patientId())
                .orElse(Patient.builder().patientId(message.patientId()).build());

        applyEvent(patient, message);
        patientRepository.save(patient);

        // Persist event record
        PatientEvent event = PatientEvent.builder()
                .eventId(message.eventId())
                .patientId(message.patientId())
                .eventType(message.eventType())
                .correlationId(message.correlationId())
                .source(message.source())
                .payload(message.payload())
                .kafkaPartition(partition)
                .kafkaOffset(offset)
                .build();
        patientEventRepository.save(event);

        log.info("[PATIENT-SVC] Processed event={} type={} patient={} status={}",
                message.eventId(), message.eventType(), message.patientId(), patient.getCurrentStatus());
    }

    private void applyEvent(Patient patient, PatientEventMessage message) {
        Map<String, Object> payload = message.payload();
        patient.setSource(message.source());

        switch (message.eventType()) {
            case "ADMISSION" -> {
                patient.setCurrentStatus("ADMITTED");
                patient.setCurrentWard(str(payload, "ward"));
                patient.setAttendingPhysician(str(payload, "attendingPhysicianId"));
                patient.setFirstName(str(payload, "firstName"));
                patient.setLastName(str(payload, "lastName"));
                patient.setDateOfBirth(str(payload, "dateOfBirth"));
                patient.setInsuranceId(str(payload, "insuranceId"));
            }
            case "DISCHARGE" -> {
                patient.setCurrentStatus("DISCHARGED");
                patient.setCurrentWard(null);
            }
            case "TRANSFER" -> {
                patient.setCurrentStatus("TRANSFERRED");
                patient.setCurrentWard(str(payload, "toWard"));
            }
            case "LAB_UPDATE" -> {
                // Lab updates don't change patient status/ward
                log.debug("[PATIENT-SVC] Lab update for patient={}", message.patientId());
            }
            default -> log.warn("[PATIENT-SVC] Unknown event type: {}", message.eventType());
        }
    }

    public Page<Patient> findAll(Pageable pageable) {
        return patientRepository.findAll(pageable);
    }

    public Optional<Patient> findByPatientId(String patientId) {
        return patientRepository.findByPatientId(patientId);
    }

    public Page<PatientEvent> findEventsByPatientId(String patientId, Pageable pageable) {
        return patientEventRepository.findByPatientIdOrderByProcessedAtDesc(patientId, pageable);
    }

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
