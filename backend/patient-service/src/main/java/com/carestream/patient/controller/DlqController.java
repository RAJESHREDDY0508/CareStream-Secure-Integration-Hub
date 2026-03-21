package com.carestream.patient.controller;

import com.carestream.patient.entity.DlqEntry;
import com.carestream.patient.service.DlqService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST API for DLQ monitoring and management.
 * ADMIN-only — these are operational endpoints.
 */
@RestController
@RequestMapping("/api/v1/dlq")
@RequiredArgsConstructor
public class DlqController {

    private final DlqService dlqService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<DlqEntry>> listEntries(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("dlqPublishedAt").descending());
        return ResponseEntity.ok(dlqService.listEntries(status, pageable));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(dlqService.getStats());
    }

    @PostMapping("/{id}/discard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DlqEntry> discard(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.getOrDefault("note", "Manually discarded") : "Manually discarded";
        return ResponseEntity.ok(dlqService.discard(id, note));
    }

    @PostMapping("/{id}/reprocess")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DlqEntry> reprocess(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.getOrDefault("note", "Manually reprocessed") : "Manually reprocessed";
        return ResponseEntity.ok(dlqService.markReprocessed(id, note));
    }
}
