# CareStream Secure Integration Hub

A distributed, event-driven healthcare integration platform with real-time patient event
streaming and an enterprise Security Operations layer.

## What This System Does

- Streams patient ADT events (Admit/Discharge/Transfer) in real time via Apache Kafka
- Enforces HIPAA-grade security: JWT auth, RBAC, encrypted PII, immutable audit log
- Provides a Security Operations platform: vulnerability management, threat detection, incident response
- Exposes an executive dashboard with real-time security + system metrics

## Phase Status

| Phase | Name | Status |
|---|---|---|
| 0 | Planning & System Design | ✅ Complete |
| 1 | Core Backend Foundation | ✅ Complete |
| 2 | Security Foundation | ✅ Complete |
| 3 | Event-Driven Excellence | ✅ Complete |
| 4 | Vulnerability Management | ✅ Complete |
| 5 | Threat Detection & Incident Response | ✅ Complete |
| 6 | Observability & Metrics | ✅ Complete |
| 7 | Frontend Dashboard | ✅ Complete |
| 8 | Deployment & DevOps | — |

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17 + Spring Boot 3.2 |
| API Gateway | Spring Cloud Gateway |
| Event Bus | Apache Kafka 3.6 |
| Database | PostgreSQL 15 |
| Cache | Redis 7 |
| Frontend | React 18 + TypeScript |
| Observability | Prometheus + Grafana + CloudWatch |
| Container | Docker + Docker Compose |
| Cloud | AWS ECS + RDS + MSK |

## Documentation (Phase 0)

| Document | Description |
|---|---|
| [docs/HLD.md](docs/HLD.md) | High-Level Design — architecture diagram, data flow, security layers |
| [docs/API-SPEC.yaml](docs/API-SPEC.yaml) | OpenAPI 3.0 specification — all endpoints |
| [docs/DB-SCHEMAS.md](docs/DB-SCHEMAS.md) | PostgreSQL schemas for all services |
| [docs/KAFKA-TOPICS.md](docs/KAFKA-TOPICS.md) | Kafka topics, partitioning, event schemas |
| [docs/SERVICE-INTERACTIONS.md](docs/SERVICE-INTERACTIONS.md) | Sequence diagrams for all major flows |
| [docs/SIMULATION-PLAN.md](docs/SIMULATION-PLAN.md) | Demo simulation scripts and scenarios |

## Services

| Service | Port | Responsibility |
|---|---|---|
| api-gateway | 8080 | JWT validation, routing, rate limiting |
| auth-service | 8081 | Authentication, JWT, RBAC |
| ingestion-service | 8082 | ADT event reception → Kafka |
| patient-service | 8083 | Patient state management |
| audit-service | 8084 | Immutable compliance audit log |
| vulnerability-service | 8085 | Vuln management + SLA engine |
| threat-detection-service | 8086 | Real-time threat analysis |
| incident-service | 8087 | Incident response workflow |

## Quick Answer: Data Flow

```
EHR → API Gateway (JWT check) → Ingestion Service → Kafka → Patient Service + Audit Service
                                                         └──► Threat Detection → Incidents
```

## Running Locally (Phase 1)

```bash
# 1. Start infrastructure (Kafka, PostgreSQL, Redis, Grafana)
docker compose up -d zookeeper kafka postgres redis kafka-ui prometheus grafana

# 2. Build and run each service (from backend/)
cd backend
mvn install -DskipTests

# 3. Start services (in separate terminals)
cd ingestion-service && mvn spring-boot:run
cd patient-service   && mvn spring-boot:run
cd audit-service     && mvn spring-boot:run

# 4. Send a test event
curl -X POST http://localhost:8082/api/v1/ingest/adt-event \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "ADMISSION",
    "patientId": "P-00001",
    "source": "EHR_SYSTEM_A",
    "payload": { "ward": "ICU-1", "attendingPhysicianId": "DOC-001" }
  }'

# 5. Verify patient in DB
curl http://localhost:8083/api/v1/patients/P-00001

# 6. Run full smoke test
bash tools/simulate/smoke_test.sh

# 7. Run high-volume simulation
cd tools/simulate && pip install -r requirements.txt
python patient_event_producer.py --count 5000 --delay-ms 10
```

**Kafka UI:** http://localhost:8090 — watch messages flowing in real time
**Grafana:**  http://localhost:3000 (admin/admin)

## Quick Answer: Where Security is Enforced

1. **Transport** — TLS on all endpoints
2. **Gateway** — JWT signature verification + rate limiting
3. **Services** — `@PreAuthorize` RBAC on every endpoint
4. **Data** — AES-256 encrypted PII columns
5. **Audit** — Append-only PostgreSQL table (row-level security)
6. **SecOps** — Continuous vulnerability scanning + real-time threat detection
