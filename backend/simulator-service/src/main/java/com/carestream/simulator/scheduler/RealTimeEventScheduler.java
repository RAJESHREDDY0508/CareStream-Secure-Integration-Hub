package com.carestream.simulator.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Produces a continuous mixed stream of realistic healthcare security events.
 *
 * Schedule:
 *  Every 20s → 2-4 patient ADT events  (patient.admission / discharge / transfer)
 *  Every 30s → 1-2 vulnerability findings  (vulnerability.scan.results)
 *  Every 45s → 1   direct security alert   (security.alerts)
 *  Every 60s → 1   SLA pressure finding    (vulnerability.scan.results, always CRITICAL)
 *
 * Downstream effect chain:
 *  patient.admission → audit-service (audit_logs) + patient-service (patient records)
 *  vulnerability.scan.results → vulnerability-service → vulnerability.findings + security.alerts
 *  security.alerts → incident-service → incidents table
 *  All → frontend polls every 30s → KPI cards update in real time
 *  All → Prometheus scrapes every 15s → Grafana dashboards refresh
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealTimeEventScheduler {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // ── Patient demographics ──────────────────────────────────────────────────

    private static final String[] FIRST_NAMES = {
            "Emma", "Liam", "Olivia", "Noah", "Ava", "Ethan", "Sophia", "Mason",
            "Isabella", "William", "Mia", "James", "Charlotte", "Benjamin", "Amelia",
            "Lucas", "Harper", "Henry", "Evelyn", "Alexander", "Abigail", "Michael",
            "Emily", "Daniel", "Elizabeth", "Owen", "Sofia", "Sebastian", "Avery", "Jack"
    };

    private static final String[] LAST_NAMES = {
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
            "Martinez", "Hernandez", "Lopez", "Wilson", "Anderson", "Thomas", "Taylor",
            "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White", "Harris",
            "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker", "Young"
    };

    private static final String[] DEPARTMENTS = {
            "EMERGENCY", "CARDIOLOGY", "ONCOLOGY", "NEUROLOGY", "ICU",
            "SURGERY", "RADIOLOGY", "PEDIATRICS", "ORTHOPEDICS", "PSYCHIATRY",
            "NEPHROLOGY", "GASTROENTEROLOGY", "PULMONOLOGY", "DERMATOLOGY", "UROLOGY"
    };

    private static final String[] DIAGNOSES = {
            "Acute myocardial infarction", "Pneumonia", "Appendicitis",
            "Cerebral vascular accident", "Diabetes mellitus type 2",
            "Sepsis", "Congestive heart failure", "Pulmonary embolism",
            "Hip fracture", "Acute kidney injury", "COPD exacerbation",
            "Gastrointestinal bleeding", "Hypertensive crisis", "Atrial fibrillation",
            "Deep vein thrombosis", "Ischemic stroke", "Acute pancreatitis",
            "Traumatic brain injury", "Cellulitis", "Urinary tract infection"
    };

    private static final String[] ATTENDING_PHYSICIANS = {
            "Dr. Sarah Mitchell", "Dr. James Patel", "Dr. Elena Rodriguez",
            "Dr. Marcus Chen", "Dr. Priya Sharma", "Dr. David Kim",
            "Dr. Rachel Thompson", "Dr. Michael Okonkwo", "Dr. Lisa Nakamura",
            "Dr. Anthony Reyes"
    };

    private static final String[] WARDS = {
            "3A", "3B", "4C", "5A", "ICU-1", "ICU-2", "CCU", "ED-1", "ED-2",
            "OR-1", "OR-2", "PICU", "NICU", "6B", "7A"
    };

    // ── Vulnerability scan data ───────────────────────────────────────────────

    private static final String[] SERVICES = {
            "auth-service", "patient-service", "audit-service", "api-gateway",
            "vulnerability-service", "incident-service", "ingestion-service"
    };

    private static final String[][] COMPONENTS_BY_SEVERITY = {
            // CRITICAL
            { "OpenSSL 3.0.7", "Apache Log4j 2.14.1", "Spring Framework 5.3.27",
              "Jackson 2.13.4", "Netty 4.1.77", "Apache Tomcat 9.0.62",
              "PostgreSQL JDBC 42.3.3", "Bouncy Castle 1.68" },
            // HIGH
            { "Spring Security 5.7.4", "Hibernate 5.6.10", "Apache Commons Text 1.9",
              "H2 Database 2.1.212", "Micrometer 1.9.4", "Flyway 9.1.6",
              "Caffeine Cache 3.1.1", "MapStruct 1.5.3" },
            // MEDIUM
            { "Lombok 1.18.24", "SLF4J 1.7.36", "Logback 1.2.11",
              "Guava 31.1", "OkHttp 4.10.0", "Retrofit 2.9.0",
              "Feign 11.10", "Resilience4j 1.7.1" },
            // LOW
            { "Mockito 4.8.0", "JUnit 5.9.1", "AssertJ 3.23.1",
              "TestContainers 1.17.5", "WireMock 2.34.0", "ArchUnit 0.23.1",
              "Jacoco 0.8.8", "Checkstyle 10.3.3" }
    };

    private static final String[] CVE_PREFIXES = { "2023", "2024", "2025" };

    private static final double[][] CVSS_RANGES = {
            { 9.0, 10.0 },   // CRITICAL
            { 7.0, 8.9 },    // HIGH
            { 4.0, 6.9 },    // MEDIUM
            { 1.0, 3.9 }     // LOW
    };

    private static final String[] SEVERITY_NAMES = { "CRITICAL", "HIGH", "MEDIUM", "LOW" };

    // Realistic distribution: CRITICAL 7%, HIGH 13%, MEDIUM 50%, LOW 30%
    // Mirrors a real enterprise scanner: most findings are medium/low noise,
    // critical/high are rare and actionable.
    private static final int[] SEVERITY_WEIGHTS = { 7, 13, 50, 30 };

    private static final String[] CVE_DESCRIPTIONS = {
            "Remote code execution via deserialization of untrusted data",
            "SQL injection in prepared statement bypass",
            "Authentication bypass through JWT algorithm confusion",
            "Denial of service via infinite loop in XML parser",
            "Path traversal allowing arbitrary file read",
            "Server-side request forgery in URL validation",
            "Cross-site scripting via unsanitized error messages",
            "Privilege escalation through misconfigured RBAC",
            "Memory corruption in native library",
            "Information disclosure via stack trace exposure",
            "Broken object-level authorization in REST endpoint",
            "Insecure direct object reference in patient record API",
            "Missing rate limiting on authentication endpoint",
            "Weak cryptographic algorithm (MD5) in session token generation",
            "Unvalidated redirect to external attacker-controlled URL"
    };

    // ── Security alert types ─────────────────────────────────────────────────

    private static final String[] ALERT_TYPES = {
            "BRUTE_FORCE_ATTEMPT", "UNAUTHORIZED_ACCESS",
            "SUSPICIOUS_LOGIN", "PRIVILEGE_ESCALATION",
            "DATA_ACCESS_ANOMALY", "CREDENTIAL_STUFFING",
            "INSIDER_THREAT_INDICATOR", "LATERAL_MOVEMENT_DETECTED"
    };

    private static final String[] THREAT_SOURCES = {
            "auth-service", "api-gateway", "patient-service", "ingestion-service"
    };

    private static final String[] SUSPICIOUS_IPS = {
            "185.220.101.42", "45.33.32.156", "198.51.100.23",
            "203.0.113.17", "192.0.2.88", "172.16.254.1",
            "10.0.0.254", "91.108.4.55", "194.165.16.78"
    };

    // ── Counters for log context ──────────────────────────────────────────────

    private long patientEventCount  = 0;
    private long vulnEventCount     = 0;
    private long alertEventCount    = 0;

    // ─────────────────────────────────────────────────────────────────────────
    // SCHEDULED TASKS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Patient ADT events — every 20 seconds.
     * Publishes 2-4 mixed admission/discharge/transfer events.
     */
    @Scheduled(fixedRate = 20_000, initialDelay = 5_000)
    public void publishPatientEvents() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int count = rnd.nextInt(2, 5);  // 2-4 events

        for (int i = 0; i < count; i++) {
            String eventType = weightedEventType(rnd);
            String topic     = topicForType(eventType);
            String patientId = String.format("P-%04d", rnd.nextInt(1000, 9999));
            String eventId   = UUID.randomUUID().toString();

            try {
                String firstName  = FIRST_NAMES[rnd.nextInt(FIRST_NAMES.length)];
                String lastName   = LAST_NAMES[rnd.nextInt(LAST_NAMES.length)];
                String department = DEPARTMENTS[rnd.nextInt(DEPARTMENTS.length)];
                String diagnosis  = DIAGNOSES[rnd.nextInt(DIAGNOSES.length)];
                String physician  = ATTENDING_PHYSICIANS[rnd.nextInt(ATTENDING_PHYSICIANS.length)];
                String ward       = WARDS[rnd.nextInt(WARDS.length)];

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("patientName",   firstName + " " + lastName);
                payload.put("firstName",     firstName);
                payload.put("lastName",      lastName);
                payload.put("dateOfBirth",   randomDob(rnd));
                payload.put("gender",        rnd.nextBoolean() ? "M" : "F");
                payload.put("department",    department);
                payload.put("ward",          ward);
                payload.put("bedNumber",     ward + "-" + rnd.nextInt(1, 21));
                payload.put("diagnosis",     diagnosis);
                payload.put("attendingPhysician", physician);
                payload.put("insuranceId",   "INS-" + rnd.nextInt(100000, 999999));
                payload.put("admissionType", eventType.equals("ADMISSION")
                        ? (rnd.nextBoolean() ? "EMERGENCY" : "ELECTIVE") : null);

                if ("TRANSFER".equals(eventType)) {
                    String targetDept = DEPARTMENTS[rnd.nextInt(DEPARTMENTS.length)];
                    payload.put("fromDepartment", department);
                    payload.put("toDepartment",   targetDept);
                    payload.put("transferReason", "Clinical decision — level of care adjustment");
                }

                Map<String, Object> message = new LinkedHashMap<>();
                message.put("eventId",       eventId);
                message.put("eventType",     eventType);
                message.put("patientId",     patientId);
                message.put("correlationId", UUID.randomUUID().toString());
                message.put("timestamp",     Instant.now().toString());
                message.put("source",        "hl7-simulator");
                message.put("publishedBy",   "simulator-service");
                message.put("payload",       payload);

                kafkaTemplate.send(topic, patientId, objectMapper.writeValueAsString(message));
                patientEventCount++;
                log.info("[SIM-PATIENT] #{} type={} patient={} dept={} topic={}",
                        patientEventCount, eventType, patientId, department, topic);

            } catch (Exception e) {
                log.error("[SIM-PATIENT] Failed for patient={}: {}", patientId, e.getMessage());
            }
        }
    }

    /**
     * Vulnerability scan results — every 30 seconds.
     * Publishes 1-2 findings to vulnerability.scan.results.
     * vulnerability-service consumes this and saves to DB + publishes to vulnerability.findings.
     */
    @Scheduled(fixedRate = 30_000, initialDelay = 10_000)
    public void publishVulnerabilityFindings() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int count = rnd.nextBoolean() ? 1 : 2;

        for (int i = 0; i < count; i++) {
            int severityIdx  = weightedSeverityIndex(rnd);
            String severity  = SEVERITY_NAMES[severityIdx];
            String[] comps   = COMPONENTS_BY_SEVERITY[severityIdx];
            double[] cvssRange = CVSS_RANGES[severityIdx];

            String findingId  = "FIND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String scanId     = "SCAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String cveYear    = CVE_PREFIXES[rnd.nextInt(CVE_PREFIXES.length)];
            String cveId      = "CVE-" + cveYear + "-" + rnd.nextInt(10000, 99999);
            String component  = comps[rnd.nextInt(comps.length)];
            String service    = SERVICES[rnd.nextInt(SERVICES.length)];
            double cvss       = Math.round((cvssRange[0] + rnd.nextDouble() * (cvssRange[1] - cvssRange[0])) * 10.0) / 10.0;
            String description = CVE_DESCRIPTIONS[rnd.nextInt(CVE_DESCRIPTIONS.length)];

            try {
                Map<String, Object> message = new LinkedHashMap<>();
                message.put("findingId",         findingId);
                message.put("scanId",            scanId);
                message.put("cveId",             cveId);
                message.put("severity",          severity);
                message.put("affectedComponent", component);
                message.put("affectedService",   service);
                message.put("cvssScore",         cvss);
                message.put("description",       description);
                message.put("scannerSource",     "realtime-simulator");
                message.put("detectedAt",        Instant.now().toString());

                kafkaTemplate.send("vulnerability.scan.results", findingId,
                        objectMapper.writeValueAsString(message));
                vulnEventCount++;
                log.info("[SIM-VULN] #{} findingId={} cveId={} severity={} cvss={} service={}",
                        vulnEventCount, findingId, cveId, severity, cvss, service);

            } catch (Exception e) {
                log.error("[SIM-VULN] Failed for finding={}: {}", findingId, e.getMessage());
            }
        }
    }

    /**
     * Direct security alerts — every 45 seconds.
     * Simulates auth/access threats that bypass the vulnerability pipeline.
     * Consumed directly by incident-service → creates incidents immediately.
     */
    @Scheduled(fixedRate = 45_000, initialDelay = 15_000)
    public void publishSecurityAlerts() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        String alertId    = UUID.randomUUID().toString();
        String alertType  = ALERT_TYPES[rnd.nextInt(ALERT_TYPES.length)];
        String sourceIp   = SUSPICIOUS_IPS[rnd.nextInt(SUSPICIOUS_IPS.length)];
        String source     = THREAT_SOURCES[rnd.nextInt(THREAT_SOURCES.length)];
        // Realistic alert distribution: ~10% CRITICAL, ~20% HIGH, ~45% MEDIUM, ~25% LOW
        String[] sevs     = { "CRITICAL", "HIGH", "HIGH", "MEDIUM", "MEDIUM", "MEDIUM", "MEDIUM", "LOW", "LOW", "LOW" };
        String severity   = sevs[rnd.nextInt(sevs.length)];

        String description = buildAlertDescription(alertType, sourceIp, source, rnd);

        try {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("sourceIp",       sourceIp);
            evidence.put("targetService",  source);
            evidence.put("attemptCount",   rnd.nextInt(3, 150));
            evidence.put("userAgent",      randomUserAgent(rnd));
            evidence.put("requestPath",    randomPath(alertType, rnd));
            evidence.put("geoLocation",    randomGeo(rnd));

            Map<String, Object> alert = new LinkedHashMap<>();
            alert.put("alertId",       alertId);
            alert.put("alertType",     alertType);
            alert.put("severity",      severity);
            alert.put("sourceService", "simulator-service");
            alert.put("detectedAt",    Instant.now().toString());
            alert.put("description",   description);
            alert.put("evidence",      evidence);
            alert.put("status",        "OPEN");

            kafkaTemplate.send("security.alerts", alertId,
                    objectMapper.writeValueAsString(alert));
            alertEventCount++;
            log.info("[SIM-ALERT] #{} alertType={} severity={} source={} ip={}",
                    alertEventCount, alertType, severity, source, sourceIp);

        } catch (Exception e) {
            log.error("[SIM-ALERT] Failed for alertId={}: {}", alertId, e.getMessage());
        }
    }

    /**
     * Scheduled scan pulse — every 60 seconds.
     * Emits 1 finding using realistic weighted severity (same distribution as the main scan job).
     * Occasionally (1-in-5 chance) emits a HIGH or CRITICAL to keep SLA metrics non-trivial,
     * but the majority will be MEDIUM/LOW — matching real-world scanner output.
     */
    @Scheduled(fixedRate = 60_000, initialDelay = 30_000)
    public void publishScheduledScanPulse() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        // 80% use the same realistic weighted distribution;
        // 20% force HIGH so critical/high findings appear over time but not every minute
        int severityIdx;
        if (rnd.nextInt(100) < 20) {
            severityIdx = rnd.nextBoolean() ? 0 : 1;  // CRITICAL or HIGH
        } else {
            severityIdx = weightedSeverityIndex(rnd);  // realistic weighted pick
        }

        String severity  = SEVERITY_NAMES[severityIdx];
        String[] comps   = COMPONENTS_BY_SEVERITY[severityIdx];
        double[] cvssRange = CVSS_RANGES[severityIdx];

        String findingId = "SCAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String cveId     = "CVE-2025-" + rnd.nextInt(10000, 99999);
        String service   = SERVICES[rnd.nextInt(SERVICES.length)];
        String component = comps[rnd.nextInt(comps.length)];
        double cvss      = Math.round((cvssRange[0] + rnd.nextDouble() * (cvssRange[1] - cvssRange[0])) * 10.0) / 10.0;

        try {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("findingId",         findingId);
            message.put("scanId",            "PULSE-SCAN");
            message.put("cveId",             cveId);
            message.put("severity",          severity);
            message.put("affectedComponent", component);
            message.put("affectedService",   service);
            message.put("cvssScore",         cvss);
            message.put("description",       CVE_DESCRIPTIONS[rnd.nextInt(CVE_DESCRIPTIONS.length)]);
            message.put("scannerSource",     "realtime-simulator-pulse");
            message.put("detectedAt",        Instant.now().toString());

            kafkaTemplate.send("vulnerability.scan.results", findingId,
                    objectMapper.writeValueAsString(message));
            log.info("[SIM-PULSE] Pulse findingId={} severity={} cvss={} service={}",
                    findingId, severity, cvss, service);

        } catch (Exception e) {
            log.error("[SIM-PULSE] Failed: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** ADMISSION 50% | DISCHARGE 30% | TRANSFER 20% */
    private String weightedEventType(ThreadLocalRandom rnd) {
        int n = rnd.nextInt(100);
        if (n < 50) return "ADMISSION";
        if (n < 80) return "DISCHARGE";
        return "TRANSFER";
    }

    private String topicForType(String eventType) {
        return switch (eventType) {
            case "DISCHARGE" -> "patient.discharge";
            case "TRANSFER"  -> "patient.transfer";
            default          -> "patient.admission";
        };
    }

    /** Weighted: CRITICAL 7% | HIGH 13% | MEDIUM 50% | LOW 30% */
    private int weightedSeverityIndex(ThreadLocalRandom rnd) {
        int n = rnd.nextInt(100);
        if (n < 7)  return 0;   // CRITICAL
        if (n < 20) return 1;   // HIGH
        if (n < 70) return 2;   // MEDIUM
        return 3;               // LOW
    }

    private String randomDob(ThreadLocalRandom rnd) {
        int year  = rnd.nextInt(1940, 2005);
        int month = rnd.nextInt(1, 13);
        int day   = rnd.nextInt(1, 28);
        return String.format("%04d-%02d-%02d", year, month, day);
    }

    private String buildAlertDescription(String type, String ip, String service, ThreadLocalRandom rnd) {
        return switch (type) {
            case "BRUTE_FORCE_ATTEMPT"      -> String.format(
                    "%d failed login attempts from %s targeting %s within 5 minutes",
                    rnd.nextInt(50, 500), ip, service);
            case "UNAUTHORIZED_ACCESS"      -> String.format(
                    "Unauthorized access to protected resource from %s on %s — RBAC violation",
                    ip, service);
            case "SUSPICIOUS_LOGIN"         -> String.format(
                    "Login from anomalous geo-location %s — previous sessions from different country",
                    ip);
            case "PRIVILEGE_ESCALATION"     -> String.format(
                    "User escalated privileges beyond assigned role in %s — possible insider threat",
                    service);
            case "DATA_ACCESS_ANOMALY"      -> String.format(
                    "Unusual bulk patient record access from %s — %d records in 60 seconds",
                    ip, rnd.nextInt(200, 5000));
            case "CREDENTIAL_STUFFING"      -> String.format(
                    "Credential stuffing attack detected from %s — %d unique username attempts",
                    ip, rnd.nextInt(100, 1000));
            case "INSIDER_THREAT_INDICATOR" -> String.format(
                    "Employee accessed %d patient records outside normal working hours from %s",
                    rnd.nextInt(50, 500), ip);
            case "LATERAL_MOVEMENT_DETECTED" -> String.format(
                    "Unusual inter-service API calls from %s — possible lateral movement in %s",
                    ip, service);
            default -> "Security anomaly detected from " + ip;
        };
    }

    private String randomUserAgent(ThreadLocalRandom rnd) {
        String[] agents = {
                "python-requests/2.28.0", "curl/7.85.0", "Go-http-client/2.0",
                "Mozilla/5.0 (compatible; Burp Suite Professional)",
                "sqlmap/1.7.2#stable (https://sqlmap.org)",
                "Nikto/2.1.6", "masscan/1.3.2", "nmap scripting engine"
        };
        return agents[rnd.nextInt(agents.length)];
    }

    private String randomPath(String alertType, ThreadLocalRandom rnd) {
        String[] paths = switch (alertType) {
            case "BRUTE_FORCE_ATTEMPT", "CREDENTIAL_STUFFING" ->
                    new String[]{ "/api/v1/auth/login", "/api/v1/auth/token", "/api/v1/auth/refresh" };
            case "DATA_ACCESS_ANOMALY", "INSIDER_THREAT_INDICATOR" ->
                    new String[]{ "/api/v1/patients", "/api/v1/patients/search", "/api/v1/audit/logs" };
            case "PRIVILEGE_ESCALATION" ->
                    new String[]{ "/api/v1/admin/users", "/api/v1/admin/roles", "/api/v1/vulnerabilities/admin" };
            default ->
                    new String[]{ "/api/v1/internal", "/actuator/env", "/actuator/beans", "/api/v1/system" };
        };
        return paths[rnd.nextInt(paths.length)];
    }

    private String randomGeo(ThreadLocalRandom rnd) {
        String[] geos = {
                "CN-Beijing", "RU-Moscow", "KP-Pyongyang", "IR-Tehran",
                "NG-Lagos", "BR-São Paulo", "UA-Kyiv", "VN-Ho Chi Minh City"
        };
        return geos[rnd.nextInt(geos.length)];
    }
}
