package com.carestream.incident.entity;

public enum ThreatType {
    BRUTE_FORCE,            // Repeated authentication failures
    PRIVILEGE_ESCALATION,   // Unauthorized role elevation
    DATA_EXFILTRATION,      // Abnormal data access patterns
    SQL_INJECTION,          // SQL injection attempt
    SLA_BREACH,             // Critical SLA breach detected
    VULNERABILITY_EXPLOIT,  // Active exploitation of known CVE
    ANOMALOUS_ACCESS,       // Access from unexpected source
    ACCOUNT_COMPROMISE,     // Suspected account takeover
    DENIAL_OF_SERVICE,      // DoS pattern detected
    INSIDER_THREAT          // Suspicious internal activity
}
