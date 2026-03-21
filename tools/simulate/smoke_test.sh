#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
# CareStream Phase 1 Smoke Test
# Run after: docker compose up -d
# Proves: send event → see it in DB
# ─────────────────────────────────────────────────────────────

set -e

GATEWAY="http://localhost:8080"
INGESTION="http://localhost:8082"
PATIENT="http://localhost:8083"
AUDIT="http://localhost:8084"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

pass() { echo -e "${GREEN}[PASS]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; exit 1; }

echo ""
echo "═══════════════════════════════════════════════"
echo "   CareStream Phase 1 Smoke Test"
echo "═══════════════════════════════════════════════"
echo ""

# 1. Health checks
echo "── Health Checks ──────────────────────────────"
curl -sf "$INGESTION/api/v1/ingest/health" > /dev/null && pass "Ingestion Service healthy" || fail "Ingestion Service down"
curl -sf "$PATIENT/api/v1/patients/health"  > /dev/null && pass "Patient Service healthy"  || fail "Patient Service down"
curl -sf "$AUDIT/api/v1/audit/health"       > /dev/null && pass "Audit Service healthy"    || fail "Audit Service down"

echo ""
echo "── Send ADT Event ──────────────────────────────"

# 2. Send an admission event
RESPONSE=$(curl -sf -X POST "$INGESTION/api/v1/ingest/adt-event" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "ADMISSION",
    "patientId": "P-99999",
    "source": "SMOKE_TEST",
    "payload": {
      "firstName": "Test",
      "lastName": "Patient",
      "ward": "ICU-1",
      "attendingPhysicianId": "DOC-001",
      "diagnosisCode": "J18.9"
    }
  }')

echo "Response: $RESPONSE"
EVENT_ID=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['eventId'])" 2>/dev/null || echo "unknown")
pass "Event published — eventId=$EVENT_ID"

echo ""
echo "── Wait for consumers ──────────────────────────"
sleep 3

# 3. Verify patient persisted
echo "── Verify Patient in DB ────────────────────────"
PATIENT_RESP=$(curl -sf "$PATIENT/api/v1/patients/P-99999" || echo '{}')
STATUS=$(echo "$PATIENT_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('currentStatus','NOT_FOUND'))" 2>/dev/null || echo "NOT_FOUND")

if [ "$STATUS" = "ADMITTED" ]; then
  pass "Patient P-99999 found in DB — status=ADMITTED"
else
  fail "Patient P-99999 not found or wrong status: $STATUS"
fi

# 4. Verify audit log
echo "── Verify Audit Log ────────────────────────────"
AUDIT_RESP=$(curl -sf "$AUDIT/api/v1/audit/logs?resourceId=P-99999&size=5" || echo '{}')
AUDIT_COUNT=$(echo "$AUDIT_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('totalElements',0))" 2>/dev/null || echo "0")

if [ "$AUDIT_COUNT" -gt "0" ]; then
  pass "Audit log contains $AUDIT_COUNT entry for P-99999"
else
  fail "No audit entries found for P-99999"
fi

echo ""
echo "═══════════════════════════════════════════════"
echo -e "   ${GREEN}All smoke tests passed!${NC}"
echo "   Phase 1 Exit Criteria MET:"
echo "   ✅ Send event → see it in DB"
echo "   ✅ Audit trail written"
echo "═══════════════════════════════════════════════"
echo ""
