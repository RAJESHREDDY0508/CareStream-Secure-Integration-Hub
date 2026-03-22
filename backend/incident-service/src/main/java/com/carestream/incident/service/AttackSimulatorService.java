package com.carestream.incident.service;

import com.carestream.incident.entity.*;
import com.carestream.incident.repository.ThreatEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Demo attack simulation service for Phase 5 / Phase 7 demo dashboard.
 *
 * Generates realistic threat event sequences that mimic real attack patterns:
 *  - BRUTE_FORCE: 3 rapid auth failures → account lockout signal
 *  - PRIVILEGE_ESCALATION: service account calling admin APIs
 *  - DATA_EXFILTRATION: bulk record access outside business hours
 *  - RANSOMWARE_PREP: large encrypted write operations (maps to ANOMALOUS_ACCESS)
 *  - INSIDER_THREAT: unusual access from internal IP at odd hours
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttackSimulatorService {

    private final ThreatEventRepository threatEventRepository;
    private final IncidentService       incidentService;

    private static final List<String> SERVICES = List.of(
            "auth-service", "api-gateway", "patient-service",
            "ingestion-service", "audit-service"
    );

    private static final Random RNG = new Random();

    /**
     * Run a named attack scenario and return the created Incidents.
     *
     * @param scenario  One of: BRUTE_FORCE, PRIVILEGE_ESCALATION, DATA_EXFILTRATION,
     *                  RANSOMWARE_PREP, INSIDER_THREAT, FULL_ATTACK_CHAIN, RANDOM
     * @param targetService  Optional service to target (null = random)
     */
    @Transactional
    public List<Incident> simulateAttack(String scenario, String targetService) {
        String service = targetService != null ? targetService : randomService();
        log.info("[SIM-ATTACK] Starting scenario='{}' on service='{}'", scenario, service);

        List<ThreatEvent> events = switch (scenario.toUpperCase()) {
            case "BRUTE_FORCE"         -> buildBruteForceSequence(service);
            case "PRIVILEGE_ESCALATION"-> buildPrivescSequence(service);
            case "DATA_EXFILTRATION"   -> buildDataExfilSequence(service);
            case "RANSOMWARE_PREP"     -> buildRansomwarePrepSequence(service);
            case "INSIDER_THREAT"      -> buildInsiderThreatSequence(service);
            case "FULL_ATTACK_CHAIN"   -> buildFullAttackChain(service);
            default                    -> buildRandomSequence(service);
        };

        // Persist events and process through detection engine
        List<Incident> incidents = new ArrayList<>();
        for (ThreatEvent event : events) {
            threatEventRepository.save(event);
            Incident incident = incidentService.processTheatEvent(event);
            if (incident != null) incidents.add(incident);
        }

        log.info("[SIM-ATTACK] Scenario='{}' generated {} events → {} incidents",
                scenario, events.size(), incidents.size());
        return incidents;
    }

    // ── Attack Scenarios ──────────────────────────────────────────────────

    private List<ThreatEvent> buildBruteForceSequence(String service) {
        return List.of(
            buildEvent("HIGH", ThreatType.BRUTE_FORCE, service,
                "15 failed login attempts in 30 seconds for user admin@carestream.com",
                "admin@carestream.com"),
            buildEvent("HIGH", ThreatType.BRUTE_FORCE, service,
                "Account lockout triggered after 20 consecutive failures",
                "admin@carestream.com"),
            buildEvent("CRITICAL", ThreatType.ACCOUNT_COMPROMISE, service,
                "Successful authentication from new IP after brute force pattern",
                "admin@carestream.com")
        );
    }

    private List<ThreatEvent> buildPrivescSequence(String service) {
        return List.of(
            buildEvent("HIGH", ThreatType.ANOMALOUS_ACCESS, service,
                "Service account 'svc-ingestion' accessed admin-only endpoint /admin/users",
                "/admin/users"),
            buildEvent("CRITICAL", ThreatType.PRIVILEGE_ESCALATION, service,
                "Role escalation from SERVICE to ADMIN detected for principal svc-ingestion",
                "svc-ingestion")
        );
    }

    private List<ThreatEvent> buildDataExfilSequence(String service) {
        return List.of(
            buildEvent("MEDIUM", ThreatType.ANOMALOUS_ACCESS, service,
                "Bulk export of patient records (500+ records) at 02:14 AM",
                "patient-records"),
            buildEvent("HIGH", ThreatType.DATA_EXFILTRATION, service,
                "3.2 GB data transfer to external IP detected — possible exfiltration",
                "patient-records"),
            buildEvent("CRITICAL", ThreatType.DATA_EXFILTRATION, service,
                "PHI data stream to unapproved external endpoint confirmed",
                "HL7-FHIR-patient-bundle")
        );
    }

    private List<ThreatEvent> buildRansomwarePrepSequence(String service) {
        return List.of(
            buildEvent("MEDIUM", ThreatType.ANOMALOUS_ACCESS, service,
                "Enumeration of all accessible file shares and database tables",
                "file-share"),
            buildEvent("HIGH", ThreatType.ANOMALOUS_ACCESS, service,
                "Encrypted write operations to 847 files in < 5 minutes — ransomware pattern",
                "backup-storage"),
            buildEvent("CRITICAL", ThreatType.INSIDER_THREAT, service,
                "Shadow copy deletion attempted — classic ransomware pre-deployment",
                "backup-storage")
        );
    }

    private List<ThreatEvent> buildInsiderThreatSequence(String service) {
        return List.of(
            buildEvent("MEDIUM", ThreatType.ANOMALOUS_ACCESS, service,
                "User accessing records outside their assigned department at 11:45 PM",
                "user-id:emp-4721"),
            buildEvent("HIGH", ThreatType.INSIDER_THREAT, service,
                "Mass download of patient records by departing employee (notice period)",
                "user-id:emp-4721")
        );
    }

    private List<ThreatEvent> buildFullAttackChain(String service) {
        List<ThreatEvent> chain = new ArrayList<>();
        chain.addAll(buildBruteForceSequence("auth-service"));
        chain.addAll(buildPrivescSequence(service));
        chain.addAll(buildDataExfilSequence("patient-service"));
        return chain;
    }

    private List<ThreatEvent> buildRandomSequence(String service) {
        ThreatType[] types = ThreatType.values();
        String[] severities = {"MEDIUM", "HIGH", "CRITICAL"};
        int count = 2 + RNG.nextInt(3);
        List<ThreatEvent> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ThreatType type = types[RNG.nextInt(types.length)];
            String sev      = severities[RNG.nextInt(severities.length)];
            events.add(buildEvent(sev, type, service,
                    String.format("Simulated %s event on %s", type, service),
                    "sim-resource-" + RNG.nextInt(1000)));
        }
        return events;
    }

    // ── Builder helpers ───────────────────────────────────────────────────

    private ThreatEvent buildEvent(String severity, ThreatType type, String service,
                                   String description, String affectedResource) {
        return ThreatEvent.builder()
                .eventId("SIM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase())
                .threatType(type)
                .sourceService(service)
                .description(description)
                .severity(severity)
                .affectedResource(affectedResource)
                .rawPayload(String.format("{\"simulated\":true,\"type\":\"%s\",\"severity\":\"%s\"}", type, severity))
                .processed(false)
                .occurredAt(Instant.now())
                .build();
    }

    private String randomService() {
        return SERVICES.get(RNG.nextInt(SERVICES.size()));
    }
}
