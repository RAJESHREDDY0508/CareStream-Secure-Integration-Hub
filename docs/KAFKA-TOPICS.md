# Kafka Topics & Event Schemas

## Topic Configuration

| Topic | Partitions | Replication Factor | Retention | Purpose |
|---|---|---|---|---|
| `patient.admission` | 6 | 3 | 7 days | Patient admit events |
| `patient.discharge` | 6 | 3 | 7 days | Patient discharge events |
| `patient.transfer` | 6 | 3 | 7 days | Patient transfer events |
| `audit.events` | 12 | 3 | 90 days | All system audit events |
| `security.alerts` | 3 | 3 | 30 days | Threat detection alerts |
| `vulnerability.findings` | 3 | 3 | 30 days | Scan findings |
| `dlq.patient.events` | 3 | 3 | 14 days | Dead letter queue |

**Partition key**: `patientId` (ensures ordered processing per patient)

---

## Event Schemas

### `patient.admission`
```json
{
  "eventId": "uuid-v4",
  "eventType": "ADMISSION",
  "patientId": "P-12345",
  "correlationId": "uuid-v4",
  "timestamp": "2026-03-20T10:15:30Z",
  "source": "EHR_SYSTEM_A",
  "publishedBy": "ingestion-service",
  "payload": {
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1985-06-15",
    "admissionDate": "2026-03-20T10:00:00Z",
    "ward": "ICU-3",
    "attendingPhysicianId": "DOC-001",
    "diagnosisCode": "J18.9",
    "insuranceId": "INS-99887"
  }
}
```

### `patient.discharge`
```json
{
  "eventId": "uuid-v4",
  "eventType": "DISCHARGE",
  "patientId": "P-12345",
  "correlationId": "uuid-v4",
  "timestamp": "2026-03-20T16:45:00Z",
  "source": "EHR_SYSTEM_A",
  "publishedBy": "ingestion-service",
  "payload": {
    "dischargeDate": "2026-03-20T16:30:00Z",
    "dischargeDisposition": "HOME",
    "attendingPhysicianId": "DOC-001",
    "summaryNotes": "Patient stable, follow-up in 2 weeks"
  }
}
```

### `patient.transfer`
```json
{
  "eventId": "uuid-v4",
  "eventType": "TRANSFER",
  "patientId": "P-12345",
  "correlationId": "uuid-v4",
  "timestamp": "2026-03-20T13:00:00Z",
  "source": "EHR_SYSTEM_A",
  "publishedBy": "ingestion-service",
  "payload": {
    "fromWard": "ER-1",
    "toWard": "ICU-3",
    "transferReason": "Critical condition",
    "transferDate": "2026-03-20T12:55:00Z"
  }
}
```

### `audit.events`
```json
{
  "auditId": "uuid-v4",
  "eventId": "uuid-v4",
  "action": "PATIENT_ADMITTED | PATIENT_DISCHARGED | API_CALL | AUTH_SUCCESS | AUTH_FAILURE",
  "actorId": "user-or-service-id",
  "actorRole": "ADMIN | DOCTOR | SERVICE",
  "resource": "/api/v1/patients/P-12345",
  "resourceId": "P-12345",
  "outcome": "SUCCESS | FAILURE",
  "ipAddress": "10.0.1.45",
  "timestamp": "2026-03-20T10:15:30Z",
  "details": {}
}
```

### `security.alerts`
```json
{
  "alertId": "uuid-v4",
  "alertType": "BRUTE_FORCE | ANOMALOUS_ACCESS | POLICY_VIOLATION | UNUSUAL_VOLUME",
  "severity": "CRITICAL | HIGH | MEDIUM | LOW",
  "sourceService": "threat-detection-service",
  "detectedAt": "2026-03-20T10:15:30Z",
  "description": "5 failed login attempts from IP 203.0.113.5",
  "evidence": {
    "ipAddress": "203.0.113.5",
    "userId": "user-abc",
    "attemptCount": 5,
    "windowSeconds": 60
  },
  "status": "OPEN"
}
```

### `vulnerability.findings`
```json
{
  "findingId": "uuid-v4",
  "scanId": "scan-uuid",
  "cveId": "CVE-2024-12345",
  "severity": "CRITICAL | HIGH | MEDIUM | LOW",
  "affectedComponent": "spring-security:6.1.0",
  "affectedService": "auth-service",
  "description": "Authentication bypass via malformed JWT",
  "cvssScore": 9.8,
  "slaDeadline": "2026-03-21T10:00:00Z",
  "status": "OPEN",
  "detectedAt": "2026-03-20T10:00:00Z"
}
```
