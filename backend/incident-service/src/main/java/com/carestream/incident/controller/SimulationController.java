package com.carestream.incident.controller;

import com.carestream.incident.entity.Incident;
import com.carestream.incident.service.AttackSimulatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Demo simulation endpoints — Phase 7 dashboard "Demo Control Panel".
 *
 * POST /api/v1/simulate/attack
 * Body:
 * {
 *   "scenario":      "BRUTE_FORCE",        (or PRIVILEGE_ESCALATION | DATA_EXFILTRATION |
 *                                            RANSOMWARE_PREP | INSIDER_THREAT |
 *                                            FULL_ATTACK_CHAIN | RANDOM)
 *   "targetService": "auth-service"         (optional)
 * }
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/simulate")
@RequiredArgsConstructor
public class SimulationController {

    private final AttackSimulatorService attackSimulatorService;

    @PostMapping("/attack")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> simulateAttack(
            @RequestBody(required = false) Map<String, Object> body) {

        String scenario     = body != null ? (String) body.get("scenario")      : "RANDOM";
        String targetService = body != null ? (String) body.get("targetService") : null;

        if (scenario == null) scenario = "RANDOM";

        log.info("[SIM] Attack simulation requested: scenario={}, target={}", scenario, targetService);

        List<Incident> incidents = attackSimulatorService.simulateAttack(scenario, targetService);

        Map<String, Object> response = Map.of(
                "scenario",        scenario,
                "targetService",   targetService != null ? targetService : "random",
                "incidentsCreated", incidents.size(),
                "incidents",       incidents.stream().map(i -> Map.of(
                        "incidentId", i.getIncidentId(),
                        "title",      i.getTitle(),
                        "severity",   i.getSeverity().name(),
                        "status",     i.getStatus().name(),
                        "threatType", i.getThreatType().name()
                )).toList(),
                "message",         "Attack simulation complete. Incidents are available at GET /api/v1/incidents"
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
