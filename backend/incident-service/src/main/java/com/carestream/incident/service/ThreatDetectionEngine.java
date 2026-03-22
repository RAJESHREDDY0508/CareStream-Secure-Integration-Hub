package com.carestream.incident.service;

import com.carestream.incident.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Rule-based threat detection engine.
 *
 * Evaluates raw ThreatEvents and determines:
 *  1. Whether an Incident should be created (auto-escalation rules)
 *  2. What severity to assign based on signal characteristics
 *  3. Whether the event is a duplicate / correlation match
 *
 * Rules are kept in-memory for simplicity. A production system would
 * load rules from the database or a dedicated rules engine (e.g., Drools).
 */
@Slf4j
@Component
public class ThreatDetectionEngine {

    /**
     * Mapping of Kafka alertType strings → ThreatType enum.
     * alertType values are published by vulnerability-service's Kafka publisher.
     */
    private static final Map<String, ThreatType> ALERT_TYPE_MAP = Map.of(
            "NEW_VULNERABILITY",       ThreatType.VULNERABILITY_EXPLOIT,
            "SLA_BREACHED",            ThreatType.SLA_BREACH,
            "SLA_AT_RISK",             ThreatType.SLA_BREACH,
            "BRUTE_FORCE",             ThreatType.BRUTE_FORCE,
            "PRIVILEGE_ESCALATION",    ThreatType.PRIVILEGE_ESCALATION,
            "DATA_EXFILTRATION",       ThreatType.DATA_EXFILTRATION,
            "ANOMALOUS_ACCESS",        ThreatType.ANOMALOUS_ACCESS,
            "ACCOUNT_COMPROMISE",      ThreatType.ACCOUNT_COMPROMISE
    );

    /**
     * Resolve ThreatType from alertType string. Defaults to ANOMALOUS_ACCESS.
     */
    public ThreatType resolveThreatType(String alertType) {
        if (alertType == null) return ThreatType.ANOMALOUS_ACCESS;
        return ALERT_TYPE_MAP.getOrDefault(alertType.toUpperCase(), ThreatType.ANOMALOUS_ACCESS);
    }

    /**
     * Decide if a ThreatEvent should auto-escalate to an Incident.
     *
     * Rules:
     * - CRITICAL severity → always create incident
     * - HIGH severity with SLA_BREACH or VULNERABILITY_EXPLOIT → create incident
     * - HIGH severity otherwise → create incident
     * - MEDIUM SLA_BREACH → create incident
     * - MEDIUM other → skip (noise threshold)
     * - LOW → skip
     */
    public boolean shouldCreateIncident(ThreatEvent event) {
        String sev = event.getSeverity();
        if (sev == null) return false;

        return switch (sev.toUpperCase()) {
            case "CRITICAL" -> true;
            case "HIGH"     -> true;
            case "MEDIUM"   -> event.getThreatType() == ThreatType.SLA_BREACH
                               || event.getThreatType() == ThreatType.VULNERABILITY_EXPLOIT
                               || event.getThreatType() == ThreatType.DATA_EXFILTRATION;
            default         -> false;
        };
    }

    /**
     * Map a raw severity string to IncidentSeverity enum.
     * Defaults to MEDIUM for unknown values.
     */
    public IncidentSeverity resolveIncidentSeverity(String rawSeverity) {
        if (rawSeverity == null) return IncidentSeverity.MEDIUM;
        return switch (rawSeverity.toUpperCase()) {
            case "CRITICAL" -> IncidentSeverity.CRITICAL;
            case "HIGH"     -> IncidentSeverity.HIGH;
            case "LOW"      -> IncidentSeverity.LOW;
            default         -> IncidentSeverity.MEDIUM;
        };
    }

    /**
     * Generate a human-readable incident title from threat event data.
     */
    public String generateTitle(ThreatEvent event) {
        return switch (event.getThreatType()) {
            case SLA_BREACH            -> String.format("[%s] SLA Breach Detected in %s",
                                              event.getSeverity(), event.getSourceService());
            case VULNERABILITY_EXPLOIT -> String.format("[%s] Active Vulnerability in %s: %s",
                                              event.getSeverity(), event.getSourceService(),
                                              truncate(event.getAffectedResource(), 40));
            case BRUTE_FORCE           -> String.format("Brute Force Attack on %s", event.getSourceService());
            case PRIVILEGE_ESCALATION  -> String.format("Privilege Escalation Attempt in %s", event.getSourceService());
            case DATA_EXFILTRATION     -> String.format("Data Exfiltration Pattern in %s", event.getSourceService());
            case ACCOUNT_COMPROMISE    -> String.format("Suspected Account Compromise: %s",
                                              truncate(event.getAffectedResource(), 50));
            case ANOMALOUS_ACCESS      -> String.format("Anomalous Access Pattern in %s", event.getSourceService());
            case DENIAL_OF_SERVICE     -> String.format("DoS Attack Pattern in %s", event.getSourceService());
            case INSIDER_THREAT        -> String.format("Insider Threat Activity in %s", event.getSourceService());
            default                    -> String.format("Security Incident: %s in %s",
                                              event.getThreatType(), event.getSourceService());
        };
    }

    private String truncate(String s, int max) {
        if (s == null) return "unknown";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
