#!/usr/bin/env bash
# =============================================================================
# run-load-test.sh – Ejecuta las pruebas de carga Gatling para el Decanato TP3
#
# Uso:
#   ./run-load-test.sh [smoke|load|full]
#
#   smoke  → 10 usuarios / 60s  (verificación rápida antes de cada prueba)
#   load   → 50.000 usuarios / 10 min rampa + 2 min steady (default)
#   full   → smoke + load en secuencia
#
# Variables de entorno opcionales:
#   BASE_URL=http://prod-server:8080   (default: localhost:8080)
#   USERS=50000                         (default: 50000)
#   RAMP_SECS=600                       (default: 600)
#   HOLD_SECS=120                       (default: 120)
#   MATERIA_ID=1                        (ID de materia abierta para inscripción)
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

BASE_URL="${BASE_URL:-http://localhost:8080}"
USERS="${USERS:-50000}"
RAMP_SECS="${RAMP_SECS:-600}"
HOLD_SECS="${HOLD_SECS:-120}"
MATERIA_ID="${MATERIA_ID:-1}"
MODE="${1:-load}"

GATLING_OPTS="-DbaseUrl=$BASE_URL -Dusers=$USERS -DrampSecs=$RAMP_SECS -DholdSecs=$HOLD_SECS -DmateriaId=$MATERIA_ID"

echo "========================================================"
echo "  Gatling Load Test – Decanato TP3"
echo "========================================================"
echo "  Modo      : $MODE"
echo "  Base URL  : $BASE_URL"
echo "  Usuarios  : $USERS"
echo "  Rampa     : ${RAMP_SECS}s  |  Steady: ${HOLD_SECS}s"
echo "  Materia   : $MATERIA_ID"
echo "========================================================"

# ── Verificar que el backend está activo ──────────────────────────────────────
echo ""
echo "▶ Verificando backend..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@decanato.edu","password":"Admin1234"}' 2>/dev/null || echo "000")

if [ "$HTTP_CODE" != "200" ]; then
    echo "✗ Backend no responde en $BASE_URL (HTTP $HTTP_CODE)"
    echo "  Inicialo con: cd OrtizSerranoTP3 && ./gradlew bootRun"
    exit 1
fi
echo "✓ Backend activo (HTTP $HTTP_CODE)"

# ── Función de ejecución ──────────────────────────────────────────────────────
run_smoke() {
    echo ""
    echo "▶ Ejecutando Smoke Test (10 usuarios / 60s)..."
    ./gradlew gatlingSmoke $GATLING_OPTS -Dusers=10 -DrampSecs=30 -DholdSecs=30
}

run_load() {
    echo ""
    echo "▶ Ejecutando Load Test ($USERS usuarios / ${RAMP_SECS}s rampa)..."
    ./gradlew gatlingInscripciones $GATLING_OPTS
}

# ── Selección de modo ─────────────────────────────────────────────────────────
case "$MODE" in
    smoke)
        run_smoke
        ;;
    load)
        run_load
        ;;
    full)
        run_smoke
        echo ""
        echo "✓ Smoke test pasado. Iniciando load test..."
        sleep 3
        run_load
        ;;
    *)
        echo "Uso: $0 [smoke|load|full]"
        exit 1
        ;;
esac

# ── Mostrar ubicación del reporte ─────────────────────────────────────────────
echo ""
echo "========================================================"
echo "  Reporte HTML:"
LATEST=$(ls -td build/reports/gatling/*/index.html 2>/dev/null | head -1 || echo "No encontrado")
echo "  $LATEST"
echo "========================================================"
