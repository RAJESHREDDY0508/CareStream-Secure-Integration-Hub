# Simulation & Demo Plan

This document describes where and how simulation capabilities are built into each phase.

---

## Where Simulation Fits in the Phase Plan

| Phase | Simulation Component | Purpose |
|---|---|---|
| Phase 1 (Core Backend) | Basic event publisher script | Smoke test Kafka pipeline without a real EHR |
| Phase 3 (Event-Driven Excellence) | **High-volume Kafka producer** (primary) | Prove throughput at scale |
| Phase 4 (Vulnerability Management) | Vuln dataset + random generator | Populate dashboard without real scans |
| Phase 5 (Threat Detection) | Attack simulation scripts | Trigger real incident workflow |
| Phase 6 (Observability) | Metrics + log generators | Drive Grafana/CloudWatch dashboards |
| Phase 7 (Frontend) | Scenario buttons + replay mode | Interactive live demo |

---

## Phase 3 — High-Volume Patient Event Simulation

**Goal**: Prove the system handles 10,000+ events/min and doesn't lose data.

### Script: `tools/simulate/patient_event_producer.py`

```python
from kafka import KafkaProducer
import json, random, time, uuid

BOOTSTRAP_SERVERS = 'localhost:9092'
PATIENT_COUNT = 1000
HOSPITALS = ['H-NORTH', 'H-SOUTH', 'H-EAST', 'H-WEST']
EVENT_TYPES = ['ADMISSION', 'DISCHARGE', 'TRANSFER', 'LAB_UPDATE']
WARDS = ['ICU-1', 'ICU-2', 'ER-1', 'ER-2', 'CARDIO', 'NEURO', 'ORTHO']
TOPIC_MAP = {
    'ADMISSION': 'patient.admission',
    'DISCHARGE': 'patient.discharge',
    'TRANSFER':  'patient.transfer',
    'LAB_UPDATE': 'patient.admission',  # reuse for lab demo
}

producer = KafkaProducer(
    bootstrap_servers=BOOTSTRAP_SERVERS,
    value_serializer=lambda v: json.dumps(v).encode('utf-8'),
    key_serializer=lambda k: k.encode('utf-8'),
    acks='all',
    retries=3,
)

patients = [f"P-{i:05d}" for i in range(1, PATIENT_COUNT + 1)]

def generate_event(event_type, patient_id):
    base = {
        "eventId": str(uuid.uuid4()),
        "eventType": event_type,
        "patientId": patient_id,
        "correlationId": str(uuid.uuid4()),
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "source": random.choice(HOSPITALS),
        "publishedBy": "simulation-producer",
    }
    if event_type == 'ADMISSION':
        base["payload"] = {
            "ward": random.choice(WARDS),
            "diagnosisCode": f"J{random.randint(10,99)}.{random.randint(0,9)}",
            "attendingPhysicianId": f"DOC-{random.randint(1,50):03d}",
        }
    elif event_type == 'DISCHARGE':
        base["payload"] = {
            "dischargeDisposition": random.choice(["HOME", "REHAB", "SNF", "DECEASED"]),
        }
    elif event_type == 'TRANSFER':
        wards = random.sample(WARDS, 2)
        base["payload"] = {"fromWard": wards[0], "toWard": wards[1]}
    return base

def run_simulation(count=5000, delay_ms=10):
    print(f"Starting simulation: {count} events, {delay_ms}ms delay")
    sent = 0
    for _ in range(count):
        patient_id = random.choice(patients)
        event_type = random.choice(EVENT_TYPES)
        event = generate_event(event_type, patient_id)
        topic = TOPIC_MAP[event_type]
        producer.send(topic, key=patient_id, value=event)
        sent += 1
        if sent % 500 == 0:
            print(f"  Sent {sent}/{count} events")
        time.sleep(delay_ms / 1000.0)
    producer.flush()
    print(f"Done. {sent} events sent.")

if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--count', type=int, default=5000)
    parser.add_argument('--delay-ms', type=int, default=10)
    args = parser.parse_args()
    run_simulation(args.count, args.delay_ms)
```

**Usage:**
```bash
# 5000 events at 10ms delay (~100 events/sec)
python tools/simulate/patient_event_producer.py --count 5000 --delay-ms 10

# High-volume burst (no delay, ~max throughput)
python tools/simulate/patient_event_producer.py --count 50000 --delay-ms 0
```

---

## Phase 4 — Vulnerability Simulation

**Goal**: Populate the vulnerability dashboard without running real security scanners.

### Approach 1: Predefined Dataset
File: `tools/simulate/vulnerabilities.json`

```json
[
  {
    "findingId": "VUL-001",
    "cveId": "CVE-2024-38819",
    "severity": "CRITICAL",
    "affectedComponent": "spring-web:6.1.0",
    "affectedService": "api-gateway",
    "cvssScore": 9.8,
    "description": "Path traversal in Spring MVC"
  },
  {
    "findingId": "VUL-002",
    "cveId": "CVE-2024-22233",
    "severity": "HIGH",
    "affectedComponent": "spring-security:6.1.5",
    "affectedService": "auth-service",
    "cvssScore": 7.5,
    "description": "Authorization bypass with special characters"
  }
]
```

### Approach 2: Random Generator
File: `tools/simulate/vuln_generator.py`

```python
import random, uuid, json
from datetime import datetime, timedelta

SERVICES = ['api-gateway', 'auth-service', 'ingestion-service',
            'patient-service', 'audit-service']
SEVERITY_SLA_HOURS = {'CRITICAL': 24, 'HIGH': 168, 'MEDIUM': 720, 'LOW': 2160}
COMPONENTS = [
    'spring-web:6.1.0', 'log4j:2.14.0', 'jackson-databind:2.14.0',
    'kafka-clients:3.5.0', 'postgresql:42.6.0', 'docker-base:ubuntu-22.04',
    'nginx:1.24.0', 'openssl:3.1.0',
]

def generate_vuln(finding_id_num):
    severity = random.choices(
        ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'],
        weights=[5, 15, 40, 40]
    )[0]
    detected = datetime.utcnow() - timedelta(days=random.randint(0, 30))
    sla_hours = SEVERITY_SLA_HOURS[severity]
    sla_deadline = detected + timedelta(hours=sla_hours)
    status = random.choices(
        ['OPEN', 'IN_PROGRESS', 'REMEDIATED', 'OVERDUE'],
        weights=[40, 30, 20, 10]
    )[0]
    return {
        "findingId": f"VUL-{finding_id_num:03d}",
        "scanId": f"SCAN-{uuid.uuid4().hex[:8].upper()}",
        "cveId": f"CVE-{random.randint(2022,2025)}-{random.randint(10000,99999)}",
        "severity": severity,
        "affectedComponent": random.choice(COMPONENTS),
        "affectedService": random.choice(SERVICES),
        "cvssScore": round(random.uniform(4.0, 10.0), 1),
        "slaDeadline": sla_deadline.isoformat() + "Z",
        "status": status,
        "detectedAt": detected.isoformat() + "Z",
    }

if __name__ == '__main__':
    vulns = [generate_vuln(i) for i in range(1, 101)]
    print(json.dumps(vulns, indent=2))
```

---

## Phase 5 — Security Attack Simulation

**Goal**: Trigger the Threat Detection and Incident Response workflow end-to-end.

### Script: `tools/simulate/attack_simulator.py`

```python
import requests, time, random

GATEWAY = 'http://localhost:8080/api/v1'

def simulate_brute_force(target_username='dr.smith', attempts=6):
    """Trigger BRUTE_FORCE detection rule."""
    print(f"Simulating brute force on '{target_username}'...")
    for i in range(attempts):
        r = requests.post(f'{GATEWAY}/auth/login', json={
            'username': target_username,
            'password': f'WrongPassword{i}'
        })
        print(f"  Attempt {i+1}: {r.status_code}")
        time.sleep(0.5)
    print("Done. Check Incident Service for new incident.")

def simulate_anomalous_access(admin_token):
    """Simulate rapid data access to trigger anomaly detection."""
    headers = {'Authorization': f'Bearer {admin_token}'}
    print("Simulating anomalous high-frequency access...")
    for i in range(50):
        patient_id = f"P-{random.randint(1,1000):05d}"
        requests.get(f'{GATEWAY}/patients/{patient_id}', headers=headers)
    print("Done.")

def simulate_vulnerability_escalation(admin_token):
    """Trigger SLA breach by backdating a critical vulnerability."""
    headers = {'Authorization': f'Bearer {admin_token}'}
    r = requests.post(f'{GATEWAY}/simulate/vulnerabilities',
                      headers=headers,
                      json={'count': 1, 'severityDistribution': {'CRITICAL': 1}})
    print(f"Injected critical vuln: {r.status_code} - watch for SLA breach incident")

if __name__ == '__main__':
    simulate_brute_force()
```

---

## Phase 6 — Metrics & Log Simulation

**Prometheus metrics exposed by every service (via Spring Actuator):**

| Metric | Labels | Type |
|---|---|---|
| `carestream_events_total` | service, eventType, status | Counter |
| `carestream_kafka_publish_duration_seconds` | topic | Histogram |
| `carestream_kafka_consume_lag` | topic, consumerGroup | Gauge |
| `carestream_auth_failures_total` | reason | Counter |
| `carestream_vulnerability_open_total` | severity | Gauge |
| `carestream_incident_mttr_minutes` | severity | Histogram |
| `carestream_sla_compliance_percent` | | Gauge |
| `carestream_api_request_duration_seconds` | method, endpoint, status | Histogram |

---

## Phase 7 — Interactive Demo Scenarios

The React dashboard will include a **Demo Control Panel** with:

| Button | Action | Endpoint |
|---|---|---|
| "Generate 500 patient events" | POST /simulate/patient-events {count:500} | Ingestion |
| "Trigger critical vulnerability" | POST /simulate/vulnerabilities {CRITICAL:1} | Vulnerability |
| "Simulate brute force attack" | POST /simulate/threat-attack {BRUTE_FORCE} | Gateway |
| "Run Full Demo Scenario" | POST /simulate/replay {FULL_DEMO} | Ingestion |

**Replay Scenarios:**
- `MORNING_RUSH` — 300 admissions in 60 seconds
- `MASS_ADMISSION` — 1000 admissions simulating disaster scenario
- `SECURITY_BREACH` — brute force + anomalous access + vuln escalation combo
- `FULL_DEMO` — 10-minute end-to-end walkthrough of all features
