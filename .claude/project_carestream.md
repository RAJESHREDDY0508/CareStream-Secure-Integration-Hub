---
name: project_carestream
description: CareStream project phase status and key decisions
type: project
---

8-phase portfolio project. User requires approval before each phase begins.

Phase 0 (Planning & Design) — COMPLETE as of 2026-03-20.
Deliverables created:
- docs/HLD.md — architecture diagram (Mermaid), service breakdown, security layers
- docs/API-SPEC.yaml — OpenAPI 3.0 with all endpoints including /simulate/* demo routes
- docs/DB-SCHEMAS.md — PostgreSQL schemas for auth_db, patient_db, audit_db, security_db
- docs/KAFKA-TOPICS.md — 7 topics, partition strategy, all event schemas
- docs/SERVICE-INTERACTIONS.md — 6 sequence diagrams covering all major flows
- docs/SIMULATION-PLAN.md — simulation scripts mapped to phases 1/3/4/5/6/7

**Key decisions:**
- Tech stack: Java 17 + Spring Boot 3.2, Kafka, PostgreSQL, Redis, React 18, Docker, AWS ECS
- 8 microservices, all communicating via API Gateway (port 8080)
- Partition key for Kafka: patientId (ensures ordered processing per patient)
- PII encrypted at application layer (AES-256)
- Audit table is append-only via PostgreSQL row-level security

**Simulation plan integrated across phases:**
- Phase 3: High-volume Kafka producer (Python script, 5000–50000 events)
- Phase 4: Vuln dataset + random generator scripts
- Phase 5: Attack simulator (brute force, anomalous access, vuln escalation)
- Phase 6: Prometheus metrics for all services
- Phase 7: Demo control panel with scenario buttons + replay mode

**Why:** Portfolio project to prove senior engineer skills for job search.
**How to apply:** Always reference the HLD when making architectural decisions in later phases.
