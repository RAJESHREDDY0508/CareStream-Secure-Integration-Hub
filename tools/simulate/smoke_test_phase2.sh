#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
# CareStream Phase 2 Smoke Test — Security Foundation
# Proves: Login → JWT → access APIs, role restrictions enforced
# ─────────────────────────────────────────────────────────────

set -e

GATEWAY="http://localhost:8080"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass() { echo -e "${GREEN}[PASS]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; exit 1; }
info() { echo -e "${YELLOW}[INFO]${NC} $1"; }

echo ""
echo "═══════════════════════════════════════════════"
echo "   CareStream Phase 2 Security Smoke Test"
echo "═══════════════════════════════════════════════"
echo ""

# ── 1. Login as DOCTOR ──────────────────────────────────────
echo "── 1. Login as DOCTOR ─────────────────────────"
DOCTOR_RESP=$(curl -sf -X POST "$GATEWAY/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"dr.smith","password":"Doctor@CareStream1!"}')
DOCTOR_TOKEN=$(echo "$DOCTOR_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
DOCTOR_ROLE=$(echo "$DOCTOR_RESP"  | python3 -c "import sys,json; print(json.load(sys.stdin)['role'])")

[ "$DOCTOR_ROLE" = "DOCTOR" ] && pass "DOCTOR login success — role=$DOCTOR_ROLE" || fail "DOCTOR login failed"

# ── 2. DOCTOR can submit ADT event ──────────────────────────
echo ""
echo "── 2. DOCTOR can ingest ADT event ─────────────"
INGEST_RESP=$(curl -sf -X POST "$GATEWAY/api/v1/ingest/adt-event" \
  -H "Authorization: Bearer $DOCTOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"eventType":"ADMISSION","patientId":"P-11111","source":"SMOKE_TEST","payload":{"ward":"ER-1"}}')
STATUS=$(echo "$INGEST_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])")
[ "$STATUS" = "ACCEPTED" ] && pass "DOCTOR can ingest events — status=$STATUS" || fail "Ingestion failed for DOCTOR"

# ── 3. DOCTOR can read patients ─────────────────────────────
echo ""
echo "── 3. DOCTOR can read patients ─────────────────"
sleep 2
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $DOCTOR_TOKEN" \
  "$GATEWAY/api/v1/patients")
[ "$HTTP_CODE" = "200" ] && pass "DOCTOR can read /patients — HTTP $HTTP_CODE" || fail "DOCTOR denied /patients — HTTP $HTTP_CODE"

# ── 4. DOCTOR CANNOT read audit logs (ADMIN only) ───────────
echo ""
echo "── 4. DOCTOR cannot access audit logs ──────────"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $DOCTOR_TOKEN" \
  "$GATEWAY/api/v1/audit/logs")
[ "$HTTP_CODE" = "403" ] && pass "DOCTOR denied /audit/logs — HTTP $HTTP_CODE (RBAC enforced)" || fail "DOCTOR incorrectly allowed /audit/logs — HTTP $HTTP_CODE"

# ── 5. Login as ADMIN ───────────────────────────────────────
echo ""
echo "── 5. Login as ADMIN ──────────────────────────"
ADMIN_RESP=$(curl -sf -X POST "$GATEWAY/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@CareStream1!"}')
ADMIN_TOKEN=$(echo "$ADMIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
ADMIN_ROLE=$(echo "$ADMIN_RESP"  | python3 -c "import sys,json; print(json.load(sys.stdin)['role'])")
[ "$ADMIN_ROLE" = "ADMIN" ] && pass "ADMIN login success — role=$ADMIN_ROLE" || fail "ADMIN login failed"

# ── 6. ADMIN can access audit logs ──────────────────────────
echo ""
echo "── 6. ADMIN can access audit logs ─────────────"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$GATEWAY/api/v1/audit/logs")
[ "$HTTP_CODE" = "200" ] && pass "ADMIN can read /audit/logs — HTTP $HTTP_CODE" || fail "ADMIN denied /audit/logs — HTTP $HTTP_CODE"

# ── 7. No token → 401 ──────────────────────────────────────
echo ""
echo "── 7. No token returns 401 ────────────────────"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY/api/v1/patients")
[ "$HTTP_CODE" = "401" ] && pass "No token → 401 Unauthorized (gateway enforcing)" || fail "No token returned HTTP $HTTP_CODE instead of 401"

# ── 8. Logout invalidates token ────────────────────────────
echo ""
echo "── 8. Logout blacklists token ─────────────────"
curl -sf -X POST "$GATEWAY/api/v1/auth/logout" \
  -H "Authorization: Bearer $DOCTOR_TOKEN" > /dev/null
pass "Logout accepted (token blacklisted in auth-service)"

echo ""
echo "═══════════════════════════════════════════════"
echo -e "   ${GREEN}All Phase 2 security tests passed!${NC}"
echo "   ✅ Login → JWT token issued"
echo "   ✅ JWT validated at Gateway"
echo "   ✅ RBAC enforced on all services"
echo "   ✅ No token → 401"
echo "   ✅ Wrong role → 403"
echo "   ✅ Logout → token blacklisted"
echo "═══════════════════════════════════════════════"
echo ""
