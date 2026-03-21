package com.carestream.audit.controller;

import com.carestream.audit.entity.AuditLog;
import com.carestream.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/logs")
    public ResponseEntity<Page<AuditLog>> getLogs(
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String resourceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());

        if (actorId != null) {
            return ResponseEntity.ok(auditService.findByActorId(actorId, pageable));
        }
        if (resourceId != null) {
            return ResponseEntity.ok(auditService.findByResourceId(resourceId, pageable));
        }
        return ResponseEntity.ok(auditService.findAll(pageable));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "audit-service"));
    }
}
