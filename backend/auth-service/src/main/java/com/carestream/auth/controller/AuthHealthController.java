package com.carestream.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Phase 1 stub — health only.
 * Full login/token endpoints added in Phase 2.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthHealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "auth-service",
                "phase", "1 — full JWT coming in Phase 2"
        ));
    }
}
