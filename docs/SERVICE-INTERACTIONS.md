# Service Interaction Flows

## 1. Happy Path — Patient Admission

```
EHR System                API Gateway            Auth Service         Ingestion Service        Kafka              Patient Service       Audit Service
    │                          │                       │                      │                   │                      │                    │
    │ POST /ingest/adt-event   │                       │                      │                   │                      │                    │
    │ Bearer: <JWT>            │                       │                      │                   │                      │                    │
    │─────────────────────────►│                       │                      │                   │                      │                    │
    │                          │ Validate JWT          │                      │                   │                      │                    │
    │                          │──────────────────────►│                      │                   │                      │                    │
    │                          │ {userId, role:DOCTOR} │                      │                   │                      │                    │
    │                          │◄──────────────────────│                      │                   │                      │                    │
    │                          │ Route to ingestion    │                      │                   │                      │                    │
    │                          │─────────────────────────────────────────────►│                   │                      │                    │
    │                          │                       │  1. Validate payload │                   │                      │                    │
    │                          │                       │  2. Enrich metadata  │                   │                      │                    │
    │                          │                       │  3. Publish event    │                   │                      │                    │
    │                          │                       │                      │──publish──────────►│                      │                    │
    │                          │                       │                      │  patient.admission │                      │                    │
    │                          │                       │                      │   partition=hash(P-12345)                │                    │
    │                          │ 202 Accepted          │                      │                   │                      │                    │
    │◄─────────────────────────│                       │                      │ consume            │                      │                    │
    │ {eventId, topic, offset} │                       │                      │                   │─────────────────────►│                    │
    │                          │                       │                      │                   │  Upsert patient state│                    │
    │                          │                       │                      │                   │  Persist event       │                    │
    │                          │                       │                      │                   │  patient_db          │                    │
    │                          │                       │                      │                   │                      │                    │
    │                          │                       │                      │                   │ consume              │                    │
    │                          │                       │                      │                   │──────────────────────────────────────────►│
    │                          │                       │                      │                   │                      │  Write audit_log   │
    │                          │                       │                      │                   │                      │  audit_db          │
```

---

## 2. Authentication Flow

```
Client                  API Gateway             Auth Service              Redis
  │                          │                       │                      │
  │ POST /auth/login         │                       │                      │
  │─────────────────────────►│                       │                      │
  │                          │ Forward (public route)│                      │
  │                          │──────────────────────►│                      │
  │                          │                       │ 1. Verify credentials│
  │                          │                       │    (BCrypt compare)  │
  │                          │                       │ 2. Generate JWT      │
  │                          │                       │    (RS256, 15 min)   │
  │                          │                       │ 3. Generate refresh  │
  │                          │                       │    token (7 days)    │
  │                          │                       │ 4. Cache token meta  │
  │                          │                       │──────────────────────►│
  │                          │ 200 {accessToken,     │                      │
  │                          │      refreshToken}    │                      │
  │◄─────────────────────────│                       │                      │
  │                          │                       │                      │
  │  (subsequent request)    │                       │                      │
  │ GET /patients            │                       │                      │
  │ Bearer: <JWT>            │                       │                      │
  │─────────────────────────►│                       │                      │
  │                          │ Extract JWT           │                      │
  │                          │ Check blacklist cache │                      │
  │                          │──────────────────────────────────────────────►│
  │                          │◄──────────────────────────────────────────────│
  │                          │ Verify RS256 signature│                      │
  │                          │ Check role claim      │                      │
  │                          │ Forward with user ctx │                      │
  │                          │──────► Patient Service│                      │
```

---

## 3. Threat Detection & Incident Creation

```
Attacker               API Gateway         Auth Service      Audit Service     Threat Detection     Incident Service
   │                        │                   │                  │                  │                    │
   │ POST /auth/login (x5   │                   │                  │                  │                    │
   │ wrong password)        │                   │                  │                  │                    │
   │───────────────────────►│                   │                  │                  │                    │
   │                        │──────────────────►│                  │                  │                    │
   │                        │  401 Unauthorized │                  │                  │                    │
   │◄───────────────────────│                   │ Publish          │                  │                    │
   │  (repeat x5)           │                   │ audit.events     │                  │                    │
   │                        │                   │─────────────────►│                  │                    │
   │                        │                   │ {AUTH_FAILURE,   │                  │                    │
   │                        │                   │  ip, userId,     │ Consume           │                    │
   │                        │                   │  timestamp}      │──────────────────►│                    │
   │                        │                   │                  │                  │ Apply rule:         │
   │                        │                   │                  │                  │ BRUTE_FORCE         │
   │                        │                   │                  │                  │ (5 fails / 60s)     │
   │                        │                   │                  │                  │                    │
   │                        │                   │                  │                  │ Publish             │
   │                        │                   │                  │                  │ security.alerts    │
   │                        │                   │                  │                  │────────────────────►│
   │                        │                   │                  │                  │                    │ Create Incident
   │                        │                   │                  │                  │                    │ severity=HIGH
   │                        │                   │                  │                  │                    │ Notify ADMIN
```

---

## 4. Vulnerability SLA Flow

```
Scan Simulator        Vulnerability Service          security_db           Dashboard / Admin
      │                        │                          │                       │
      │ Trigger mock scan      │                          │                       │
      │───────────────────────►│                          │                       │
      │                        │ Generate findings        │                       │
      │                        │ Assign SLA:              │                       │
      │                        │  CRITICAL → 24h          │                       │
      │                        │  HIGH     → 7d           │                       │
      │                        │  MEDIUM   → 30d          │                       │
      │                        │  LOW      → 90d          │                       │
      │                        │──────────────────────────►│                       │
      │                        │                          │ Store findings        │
      │                        │                          │                       │
      │                  (scheduler every 1h)             │                       │
      │                        │ Check SLA breaches       │                       │
      │                        │──────────────────────────►│                       │
      │                        │◄──────────────────────────│                       │
      │                        │ Update status=OVERDUE    │                       │
      │                        │ Publish security.alerts  │                       │
      │                        │ → Incident auto-created  │                       │
      │                        │                          │                       │
      │                        │ GET /vulnerabilities/stats│                       │
      │                        │◄──────────────────────────────────────────────────│
      │                        │──────────────────────────────────────────────────►│
      │                        │ {critical:5, slaCompliance:82%}                   │
```

---

## 5. Simulation Flow (Demo Mode)

```
Admin / Demo User          API Gateway         Ingestion Service          Kafka           All Consumers
       │                        │                      │                    │                   │
       │ POST /simulate/         │                      │                    │                   │
       │   patient-events       │                      │                    │                   │
       │ {count:500,            │                      │                    │                   │
       │  eventTypes:[...]}     │                      │                    │                   │
       │───────────────────────►│──────────────────────►│                    │                   │
       │                        │                      │ Loop 500x:         │                   │
       │                        │                      │  - Pick random     │                   │
       │                        │                      │    patientId       │                   │
       │                        │                      │  - Pick event type │                   │
       │                        │                      │  - Publish w/ 10ms │                   │
       │                        │                      │    delay           │                   │
       │                        │                      │───────────────────►│                   │
       │                        │                      │  (high-throughput) │───────────────────►│
       │                        │                      │                    │ patient.admission  │ Patient Service
       │                        │                      │                    │ patient.discharge  │ Audit Service
       │ 202 {simulationId}     │                      │                    │ patient.transfer   │ Threat Detection
       │◄───────────────────────│                      │                    │                   │
       │                        │                      │                    │                   │
       │  [React Dashboard shows real-time event counter updates via WebSocket]                 │
```

---

## 6. Service Port Map

| Service | Internal Port | Docker Network | Health Endpoint |
|---|---|---|---|
| API Gateway | 8080 | carestream-net | GET /actuator/health |
| Auth Service | 8081 | carestream-net | GET /actuator/health |
| Ingestion Service | 8082 | carestream-net | GET /ingest/health |
| Patient Service | 8083 | carestream-net | GET /actuator/health |
| Audit Service | 8084 | carestream-net | GET /actuator/health |
| Vulnerability Service | 8085 | carestream-net | GET /actuator/health |
| Threat Detection Service | 8086 | carestream-net | GET /actuator/health |
| Incident Service | 8087 | carestream-net | GET /actuator/health |
| Kafka Broker | 9092 | carestream-net | — |
| Zookeeper | 2181 | carestream-net | — |
| PostgreSQL | 5432 | carestream-net | — |
| Redis | 6379 | carestream-net | — |
| Prometheus | 9090 | carestream-net | — |
| Grafana | 3000 | carestream-net | — |
| React Frontend | 3001 | host | — |
