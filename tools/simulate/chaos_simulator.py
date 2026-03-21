"""
CareStream — Chaos Simulator (Phase 3)
Tests system resilience: retries, DLQ routing, graceful failure handling.

Scenarios:
  1. malformed   — send unparseable JSON → triggers immediate DLQ routing
  2. bad-schema  — valid JSON but wrong schema → triggers non-retryable error
  3. burst       — send valid events in tight burst to test consumer lag
  4. mixed       — mix of valid + malformed events (configurable fail rate)

Usage:
  python chaos_simulator.py malformed  --count 10
  python chaos_simulator.py bad-schema --count 5
  python chaos_simulator.py burst      --count 2000 --delay-ms 0
  python chaos_simulator.py mixed      --count 500  --fail-rate 0.15
"""

import argparse
import json
import random
import time
import uuid
from datetime import datetime, timezone

from kafka import KafkaProducer
from kafka.errors import KafkaError

BOOTSTRAP_SERVERS = "localhost:9092"
TOPICS = ["patient.admission", "patient.discharge", "patient.transfer"]


def build_producer(servers: str) -> KafkaProducer:
    return KafkaProducer(
        bootstrap_servers=servers,
        value_serializer=lambda v: v if isinstance(v, bytes) else json.dumps(v).encode("utf-8"),
        key_serializer=lambda k: k.encode("utf-8") if k else None,
        acks="all",
        retries=3,
    )


def valid_event(event_type: str = "ADMISSION") -> tuple[str, dict]:
    """Returns (patientId, event_dict)."""
    patient_id = f"P-{random.randint(1, 1000):05d}"
    return patient_id, {
        "eventId":       str(uuid.uuid4()),
        "eventType":     event_type,
        "patientId":     patient_id,
        "correlationId": str(uuid.uuid4()),
        "timestamp":     datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "source":        "CHAOS_SIMULATOR",
        "publishedBy":   "chaos-simulator",
        "payload":       {"ward": "ICU-1", "attendingPhysicianId": "DOC-001"},
    }


# ─── Malformed scenarios ──────────────────────────────────

def malformed_json() -> bytes:
    """Completely unparseable — will fail deserialization immediately → DLQ."""
    garbage = f"{{not valid json: {random.randint(0, 999)} ]]"
    return garbage.encode("utf-8")


def bad_schema_event() -> dict:
    """Valid JSON but missing required fields — fails validation → DLQ."""
    return {
        "eventType": "UNKNOWN_TYPE",
        "wrongField": "no patientId here",
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }


def truncated_payload() -> bytes:
    """Truncated mid-JSON — parse error → DLQ."""
    patient_id = f"P-{random.randint(1, 1000):05d}"
    full = json.dumps({
        "eventId": str(uuid.uuid4()),
        "eventType": "ADMISSION",
        "patientId": patient_id,
        "payload": {"ward": "ICU"},
    })
    return full[:len(full) // 2].encode("utf-8")   # cut in half


# ─── Scenarios ────────────────────────────────────────────

def scenario_malformed(producer, count: int, delay_ms: int):
    """Send `count` completely malformed messages → should all land in DLQ."""
    print(f"\n [CHAOS] Malformed JSON scenario — {count} messages\n")
    errors = {
        "garbage_json": malformed_json,
        "truncated":    truncated_payload,
    }
    sent = 0
    for _ in range(count):
        error_type = random.choice(list(errors.keys()))
        raw_bytes  = errors[error_type]()
        topic      = random.choice(TOPICS)
        try:
            producer.send(topic, key="CHAOS-KEY", value=raw_bytes)
            sent += 1
            print(f"  [{sent}] Sent {error_type} to {topic}")
        except KafkaError as e:
            print(f"  [ERR] Kafka error: {e}")
        if delay_ms > 0:
            time.sleep(delay_ms / 1000.0)
    producer.flush()
    print(f"\n  Sent {sent} malformed messages → watch DLQ: GET /api/v1/dlq")


def scenario_bad_schema(producer, count: int, delay_ms: int):
    """Send structurally valid JSON with wrong schema → non-retryable error → DLQ."""
    print(f"\n [CHAOS] Bad schema scenario — {count} messages\n")
    sent = 0
    for _ in range(count):
        event = bad_schema_event()
        topic = random.choice(TOPICS)
        try:
            producer.send(topic, key="CHAOS-SCHEMA", value=event)
            sent += 1
            print(f"  [{sent}] Bad schema event sent to {topic}")
        except KafkaError as e:
            print(f"  [ERR] {e}")
        if delay_ms > 0:
            time.sleep(delay_ms / 1000.0)
    producer.flush()
    print(f"\n  Sent {sent} bad-schema messages → watch DLQ")


def scenario_burst(producer, count: int):
    """Flood Kafka with valid events to test consumer lag and throughput."""
    print(f"\n [CHAOS] Burst scenario — {count} events at max throughput\n")
    sent = 0
    start = time.time()
    for _ in range(count):
        patient_id, event = valid_event(random.choice(["ADMISSION", "DISCHARGE", "TRANSFER"]))
        topic = f"patient.{event['eventType'].lower()}"
        try:
            producer.send(topic, key=patient_id, value=event)
            sent += 1
        except KafkaError as e:
            print(f"  [ERR] {e}")
        if sent % 500 == 0:
            elapsed = time.time() - start
            print(f"  Sent {sent}/{count} — {sent/elapsed:.0f} events/sec")
    producer.flush()
    elapsed = time.time() - start
    print(f"\n  Burst complete: {sent} events in {elapsed:.1f}s ({sent/elapsed:.0f} events/sec)")


def scenario_mixed(producer, count: int, fail_rate: float, delay_ms: int):
    """Mix of valid and malformed events. fail_rate = fraction that should fail."""
    print(f"\n [CHAOS] Mixed scenario — {count} events, {fail_rate*100:.0f}% failure rate\n")
    valid_count   = 0
    invalid_count = 0

    for i in range(count):
        topic = random.choice(TOPICS)
        if random.random() < fail_rate:
            # Send malformed
            producer.send(topic, key="CHAOS-MIXED", value=malformed_json())
            invalid_count += 1
        else:
            patient_id, event = valid_event()
            producer.send(f"patient.{event['eventType'].lower()}", key=patient_id, value=event)
            valid_count += 1

        if (i + 1) % 100 == 0:
            print(f"  Sent {i+1}/{count} — valid={valid_count} invalid={invalid_count}")
        if delay_ms > 0:
            time.sleep(delay_ms / 1000.0)

    producer.flush()
    print(f"\n  Done: {valid_count} valid, {invalid_count} malformed")
    print(f"  Expected DLQ entries: ~{invalid_count}")


# ─── CLI ──────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="CareStream Chaos Simulator")
    parser.add_argument("scenario",
                        choices=["malformed", "bad-schema", "burst", "mixed"],
                        help="Which chaos scenario to run")
    parser.add_argument("--count",     type=int,   default=10)
    parser.add_argument("--delay-ms",  type=int,   default=50)
    parser.add_argument("--fail-rate", type=float, default=0.15,
                        help="Fraction of events that fail (mixed scenario only)")
    parser.add_argument("--servers",   type=str,   default=BOOTSTRAP_SERVERS)
    args = parser.parse_args()

    print(f"\n CareStream Chaos Simulator")
    print(f"  Scenario: {args.scenario}")
    print(f"  Kafka:    {args.servers}")

    producer = build_producer(args.servers)

    try:
        if args.scenario == "malformed":
            scenario_malformed(producer, args.count, args.delay_ms)
        elif args.scenario == "bad-schema":
            scenario_bad_schema(producer, args.count, args.delay_ms)
        elif args.scenario == "burst":
            scenario_burst(producer, args.count)
        elif args.scenario == "mixed":
            scenario_mixed(producer, args.count, args.fail_rate, args.delay_ms)
    except KeyboardInterrupt:
        print("\n Interrupted — flushing...")
        producer.flush()


if __name__ == "__main__":
    main()
