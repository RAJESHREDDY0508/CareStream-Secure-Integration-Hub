package com.carestream.incident.entity;

public enum IncidentStatus {
    OPEN,           // Newly created, not yet triaged
    INVESTIGATING,  // Under active investigation
    CONTAINED,      // Threat neutralized, not yet fully resolved
    RESOLVED,       // Fully resolved and closed
    FALSE_POSITIVE  // Determined to be a false alarm
}
