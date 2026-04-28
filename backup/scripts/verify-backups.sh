#!/usr/bin/env bash
# =============================================================================
# verify-backups.sh
# Sistema de Decanato IPM — Verificación periódica de backups en S3
#
# Descripción:
#   Lista los backups de los últimos N días, verifica que exista al menos uno
#   por día, chequea el Object Lock y reporta el estado en CloudWatch.
#   Si hay días sin backup → envía alerta SNS.
#
# Cron sugerido (crontab -e):
#   # Verificar backups todos los días a las 06:00 AM
#   0 6 * * * /opt/decanato/backup/scripts/verify-backups.sh >> /var/log/decanato/verify.log 2>&1
#
# =============================================================================

set -euo pipefail

ENV_FILE="${BACKUP_ENV_FILE:-/etc/decanato/backup.env}"
# shellcheck source=/dev/null
source "$ENV_FILE"

DAYS_TO_CHECK="${1:-7}"
LOG_PREFIX="[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] [verify-backups]"
MISSING_DAYS=()
TOTAL_SIZE_BYTES=0

log() { echo "${LOG_PREFIX} $*"; }

log "═══════════════════════════════════════════════════════════"
log "Verificando backups de los últimos ${DAYS_TO_CHECK} días"
log "Bucket: s3://${S3_BUCKET}/${S3_PREFIX}"
log "═══════════════════════════════════════════════════════════"

for i in $(seq 1 "$DAYS_TO_CHECK"); do
    CHECK_DATE=$(date -u -d "-${i} days" +"%Y-%m-%d" 2>/dev/null \
                 || date -u -v-${i}d +"%Y-%m-%d")

    PREFIX_KEY="${S3_PREFIX}/${CHECK_DATE}/"

    # Listar objetos de ese día
    OBJECTS=$(aws s3api list-objects-v2 \
        --profile    "$AWS_PROFILE" \
        --region     "$AWS_REGION" \
        --bucket     "$S3_BUCKET" \
        --prefix     "$PREFIX_KEY" \
        --query      'Contents[?ends_with(Key, `.enc`)].[Key, Size, LastModified]' \
        --output     text 2>/dev/null || echo "")

    if [[ -z "$OBJECTS" ]]; then
        log "  ❌ ${CHECK_DATE} — SIN BACKUP"
        MISSING_DAYS+=("$CHECK_DATE")
        continue
    fi

    COUNT=0
    while IFS=$'\t' read -r key size modified; do
        COUNT=$((COUNT + 1))
        TOTAL_SIZE_BYTES=$((TOTAL_SIZE_BYTES + size))

        # Verificar Object Lock
        LOCK_INFO=$(aws s3api get-object-retention \
            --profile "$AWS_PROFILE" \
            --region  "$AWS_REGION" \
            --bucket  "$S3_BUCKET" \
            --key     "$key" \
            --output  json 2>/dev/null || echo '{"Retention":{"Mode":"UNKNOWN","RetainUntilDate":"N/A"}}')

        LOCK_MODE=$(echo "$LOCK_INFO" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['Retention']['Mode'])")
        RETAIN_DATE=$(echo "$LOCK_INFO" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['Retention']['RetainUntilDate'][:10])")

        SIZE_MB=$(echo "scale=2; ${size}/1048576" | bc)
        log "  ✔ ${CHECK_DATE} — ${key##*/} | ${SIZE_MB} MB | Lock: ${LOCK_MODE} hasta ${RETAIN_DATE}"

        # Alerta si el Object Lock no es COMPLIANCE
        if [[ "$LOCK_MODE" != "COMPLIANCE" ]]; then
            log "  ⚠️  Object Lock Mode inesperado: ${LOCK_MODE} (se esperaba COMPLIANCE)"
        fi
    done <<< "$OBJECTS"

    log "     Total ese día: ${COUNT} objeto(s)"
done

# ── Resumen ───────────────────────────────────────────────────────────────────
TOTAL_SIZE_MB=$(echo "scale=2; ${TOTAL_SIZE_BYTES}/1048576" | bc)
log "═══════════════════════════════════════════════════════════"
log "Tamaño total verificado: ${TOTAL_SIZE_MB} MB"

if [[ ${#MISSING_DAYS[@]} -gt 0 ]]; then
    MISSING_LIST=$(IFS=", "; echo "${MISSING_DAYS[*]}")
    log "⚠️  Días SIN backup detectados: ${MISSING_LIST}"

    aws sns publish \
        --profile   "$AWS_PROFILE" \
        --region    "$AWS_REGION" \
        --topic-arn "$SNS_TOPIC_ARN" \
        --subject   "⚠️ Backups faltantes — Sistema Decanato IPM" \
        --message   "Se detectaron días sin backup en los últimos ${DAYS_TO_CHECK} días.\n\nDías faltantes: ${MISSING_LIST}\n\nBucket: s3://${S3_BUCKET}/${S3_PREFIX}\n\nRevisar el cron job de backup en el servidor." \
        2>/dev/null || log "No se pudo enviar alerta SNS"

    exit 1
else
    log "✅ Todos los días tienen backup. Sin anomalías detectadas."
fi
