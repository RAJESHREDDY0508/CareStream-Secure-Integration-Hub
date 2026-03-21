"""
CareStream — High-Volume Patient Event Producer
Phase 1: smoke test | Phase 3: load test (5k–50k events)

Usage:
    python patient_event_producer.py --count 5000 --delay-ms 10
    python patient_event_producer.py --count 50000 --delay-ms 0   # max throughput
    python patient_event_producer.py --event-type ADMISSION --count 100
"""

import argparse
import json
import random
import time
import uuid
from datetime import datetime, timezone

from kafka import KafkaProducer
from kafka.errors import KafkaError

# ─── Config ───────────────────────────────────────────────
BOOTSTRAP_SERVERS = "localhost:9092"

TOPIC_MAP = {
    "ADMISSION":  "patient.admission",
    "DISCHARGE":  "patient.discharge",
    "TRANSFER":   "patient.transfer",
    "LAB_UPDATE": "patient.admission",
}

HOSPITALS  = ["H-NORTH", "H-SOUTH", "H-EAST", "H-WEST", "H-CENTRAL"]
WARDS      = ["ICU-1", "ICU-2", "ER-1", "ER-2", "CARDIO", "NEURO", "ORTHO", "PEDS"]
PHYSICIANS = [f"DOC-{i:03d}" for i in range(1, 51)]
DIAGNOSES  = [f"J{r}.{d}" for r in range(10, 99) for d in range(0, 10)][:50]

FIRST_NAMES = ["Emma", "Liam", "Olivia", "Noah", "Ava", "James", "Sophia", "Oliver",
               "Isabella", "William", "Mia", "Benjamin", "Charlotte", "Lucas", "Amelia"]
LAST_NAMES  = ["Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller",
               "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez"]


# ─── Event generators ─────────────────────────────────────

def _base(event_type: str, patient_id: str, hospital: str) -> dict:
    return {
        "eventId":       str(uuid.uuid4()),
        "eventType":     event_type,
        "patientId":     patient_id,
        "correlationId": str(uuid.uuid4()),
        "timestamp":     datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "source":        hospital,
        "publishedBy":   "simulation-producer",
        "payload":       {},
    }


def make_admission(patient_id: str, hospital: str) -> dict:
    event = _base("ADMISSION", patient_id, hospital)
    event["payload"] = {
        "firstName":           random.choice(FIRST_NAMES),
        "lastName":            random.choice(LAST_NAMES),
        "dateOfBirth":         f"{random.randint(1940, 2005)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}",
        "ward":                random.choice(WARDS),
        "attendingPhysicianId": random.choice(PHYSICIANS),
        "diagnosisCode":       random.choice(DIAGNOSES),
        "insuranceId":         f"INS-{random.randint(10000, 99999)}",
        "admissionDate":       event["timestamp"],
    }
    return event


def make_discharge(patient_id: str, hospital: str) -> dict:
    event = _base("DISCHARGE", patient_id, hospital)
    event["payload"] = {
        "dischargeDate":        event["timestamp"],
        "dischargeDisposition": random.choice(["HOME", "REHAB", "SNF", "TRANSFER", "EXPIRED"]),
        "attendingPhysicianId": random.choice(PHYSICIANS),
    }
    return event


def make_transfer(patient_id: str, hospital: str) -> dict:
    wards = random.sample(WARDS, 2)
    event = _base("TRANSFER", patient_id, hospital)
    event["payload"] = {
        "fromWard":      wards[0],
        "toWard":        wards[1],
        "transferDate":  event["timestamp"],
        "transferReason": random.choice(["Critical condition", "Bed availability", "Specialist required"]),
    }
    return event


def make_lab_update(patient_id: str, hospital: str) -> dict:
    event = _base("LAB_UPDATE", patient_id, hospital)
    event["payload"] = {
        "labType":   random.choice(["CBC", "BMP", "LFT", "TROPONIN", "D-DIMER"]),
        "result":    random.choice(["NORMAL", "ABNORMAL", "CRITICAL"]),
        "reportedAt": event["timestamp"],
    }
    return event


GENERATORS = {
    "ADMISSION":  make_admission,
    "DISCHARGE":  make_discharge,
    "TRANSFER":   make_transfer,
    "LAB_UPDATE": make_lab_update,
}


# ─── Producer ─────────────────────────────────────────────

def build_producer(servers: str) -> KafkaProducer:
    return KafkaProducer(
        bootstrap_servers=servers,
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
        key_serializer=lambda k: k.encode("utf-8"),
        acks="all",
        retries=3,
        linger_ms=5,          # micro-batching for throughput
        batch_size=16384,
    )


def run(count: int, delay_ms: int, event_type: str | None, servers: str):
    patients = [f"P-{i:05d}" for i in range(1, 1001)]
    event_types = [event_type] if event_type else list(GENERATORS.keys())

    print(f"\n CareStream Event Simulator")
    print(f"  Kafka:       {servers}")
    print(f"  Events:      {count}")
    print(f"  Delay:       {delay_ms}ms")
    print(f"  Types:       {event_types}\n")

    producer = build_producer(servers)
    sent = 0
    errors = 0
    start = time.time()

    try:
        for _ in range(count):
            etype      = random.choice(event_types)
            patient_id = random.choice(patients)
            hospital   = random.choice(HOSPITALS)

            event  = GENERATORS[etype](patient_id, hospital)
            topic  = TOPIC_MAP[etype]

            try:
                producer.send(topic, key=patient_id, value=event)
                sent += 1
            except KafkaError as e:
                errors += 1
                print(f"  [ERROR] Failed to send: {e}")

            if sent % 500 == 0:
                elapsed = time.time() - start
                rate    = sent / elapsed if elapsed > 0 else 0
                print(f"  Sent {sent:>6}/{count}  |  {rate:.0f} events/sec  |  errors={errors}")

            if delay_ms > 0:
                time.sleep(delay_ms / 1000.0)

        producer.flush()
        elapsed = time.time() - start
        print(f"\n Done in {elapsed:.1f}s")
        print(f"  Sent:   {sent}")
        print(f"  Errors: {errors}")
        print(f"  Rate:   {sent/elapsed:.0f} events/sec\n")

    except KeyboardInterrupt:
        print("\n Interrupted — flushing producer...")
        producer.flush()


# ─── CLI ──────────────────────────────────────────────────

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="CareStream Patient Event Simulator")
    parser.add_argument("--count",      type=int,   default=5000,        help="Number of events to send")
    parser.add_argument("--delay-ms",   type=int,   default=10,          help="Delay between events (ms). 0 = max throughput")
    parser.add_argument("--event-type", type=str,   default=None,        help="Force a specific event type (ADMISSION|DISCHARGE|TRANSFER|LAB_UPDATE)")
    parser.add_argument("--servers",    type=str,   default=BOOTSTRAP_SERVERS, help="Kafka bootstrap servers")
    args = parser.parse_args()

    run(args.count, args.delay_ms, args.event_type, args.servers)
