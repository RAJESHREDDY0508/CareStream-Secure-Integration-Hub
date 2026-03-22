package com.carestream.incident.controller;

import com.carestream.incident.entity.Incident;
import com.carestream.incident.entity.AlertNotification;
import com.carestream.incident.service.AlertNotificationService;
import com.carestream.incident.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for Incident lifecycle management.
 *
 * Base path: /api/v1/incidents
 *
 * GET    /                         — paginated list with filters
 * GET    /{incidentId}             — single incident
 * GET    /stats                    — dashboard statistics
 * GET    /health                   — liveness probe (open)
 * GET    /{incidentId}/notifications — notification audit trail
 * PATCH  /{incidentId}/investigate  — OPEN → INVESTIGATING
 * PATCH  /{incidentId}/contain      — INVESTIGATING → CONTAINED
 * PATCH  /{incidentId}/resolve      — CONTAINED/INVESTIGATING → RESOLVED
 * PATCH  /{incidentId}/false-positive — mark as false alarm
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService          incidentService;
    private final AlertNotificationService alertService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "incident-service"));
    }

    @GetMapping
    public ResponseEntity<Page<Incident>> listIncidents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String threatType,
            @RequestParam(required = false) String service,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                incidentService.findAll(status, severity, threatType, service, page, size));
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<Incident> getIncident(@PathVariable String incidentId) {
        return ResponseEntity.ok(incidentService.getByIncidentId(incidentId));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(incidentService.getStats());
    }

    @GetMapping("/{incidentId}/notifications")
    public ResponseEntity<List<AlertNotification>> getNotifications(@PathVariable String incidentId) {
        return ResponseEntity.ok(alertService.getNotificationsForIncident(incidentId));
    }

    @PatchMapping("/{incidentId}/investigate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SERVICE')")
    public ResponseEntity<Incident> startInvestigation(
            @PathVariable String incidentId,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        String assignee = (body != null && body.containsKey("assigneeId"))
                ? body.get("assigneeId") : userId;
        return ResponseEntity.ok(incidentService.startInvestigation(incidentId, assignee));
    }

    @PatchMapping("/{incidentId}/contain")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SERVICE')")
    public ResponseEntity<Incident> contain(
            @PathVariable String incidentId,
            @RequestBody(required = false) Map<String, String> body) {

        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(incidentService.contain(incidentId, notes));
    }

    @PatchMapping("/{incidentId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Incident> resolve(
            @PathVariable String incidentId,
            @RequestBody(required = false) Map<String, String> body) {

        String notes = body != null ? body.get("resolutionNotes") : "Resolved";
        return ResponseEntity.ok(incidentService.resolve(incidentId, notes));
    }

    @PatchMapping("/{incidentId}/false-positive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Incident> markFalsePositive(
            @PathVariable String incidentId,
            @RequestBody(required = false) Map<String, String> body) {

        String notes = body != null ? body.get("notes") : "Marked as false positive";
        return ResponseEntity.ok(incidentService.markFalsePositive(incidentId, notes));
    }
}
