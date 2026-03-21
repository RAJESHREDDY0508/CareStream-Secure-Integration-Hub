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
| 1 | Core Backend Foundation | — |
| 2 | Security Foundation | — |
| 3 | Event-Driven Excellence | — |
| 4 | Vulnerability Management | — |
| 5 | Threat Detection & Incident Response | — |
| 6 | Observability & Metrics | — |
| 7 | Frontend Dashboard | — |
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

## Quick Answer: Where Security is Enforced

1. **Transport** — TLS on all endpoints
2. **Gateway** — JWT signature verification + rate limiting
3. **Services** — `@PreAuthorize` RBAC on every endpoint
4. **Data** — AES-256 encrypted PII columns
5. **Audit** — Append-only PostgreSQL table (row-level security)
6. **SecOps** — Continuous vulnerability scanning + real-time threat detection
