#!/usr/bin/env bash
# =============================================================================
# restore-mysql.sh
# Sistema de Decanato IPM — Restauración de backup desde S3
#
# Uso:
#   ./restore-mysql.sh <s3-key-del-backup>
#
#   Ejemplo:
#   ./restore-mysql.sh backups/2026-04-28/decanato_decanato_db_20260428T020001Z_web01.sql.gz.enc
#
# ⚠️  ATENCIÓN: Este script sobreescribe la base de datos destino.
#               Ejecutar SOLO en ventana de mantenimiento.
#
# =============================================================================

set -euo pipefail

S3_KEY="${1:-}"
if [[ -z "$S3_KEY" ]]; then
    echo "Uso: $0 <s3-key-del-backup>" >&2
    echo "Ejemplo: $0 backups/2026-04-28/decanato_decanato_db_20260428T020001Z_web01.sql.gz.enc" >&2
    exit 1
fi

ENV_FILE="${BACKUP_ENV_FILE:-/etc/decanato/backup.env}"
# shellcheck source=/dev/null
source "$ENV_FILE"

TEMP_DIR=$(mktemp -d /tmp/decanato-restore-XXXXXX)
ENCRYPTED_FILE="${TEMP_DIR}/backup.enc"
DECRYPTED_FILE="${TEMP_DIR}/backup.sql.gz"
LOG_PREFIX="[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] [restore-mysql]"

log() { echo "${LOG_PREFIX} $*"; }
log_error() { echo "${LOG_PREFIX} [ERROR] $*" >&2; }

cleanup() { rm -rf "$TEMP_DIR"; }
trap cleanup EXIT

# ── Confirmar operación destructiva ──────────────────────────────────────────
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  ⚠️  ADVERTENCIA — OPERACIÓN DESTRUCTIVA                     ║"
echo "║                                                              ║"
echo "║  Se va a RESTAURAR la base de datos:                         ║"
echo "║    Base de datos : ${DB_NAME}"
echo "║    Host          : ${DB_HOST}:${DB_PORT}"
echo "║    Backup S3     : ${S3_KEY}"
echo "║                                                              ║"
echo "║  Esto SOBREESCRIBIRÁ todos los datos actuales.               ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
read -rp "Escribí CONFIRMAR para continuar: " CONFIRMACION
if [[ "$CONFIRMACION" != "CONFIRMAR" ]]; then
    log "Operación cancelada por el usuario."
    exit 0
fi

# ── 1. Verificar Object Lock (no restaurar un backup corrupto) ────────────────
log "Verificando Object Lock y metadatos en S3..."
METADATA=$(aws s3api head-object \
    --profile "$AWS_PROFILE" \
    --region  "$AWS_REGION" \
    --bucket  "$S3_BUCKET" \
    --key     "$S3_KEY" \
    --output  json)

LOCK_MODE=$(echo "$METADATA" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('ObjectLockMode','NONE'))")
RETAIN_UNTIL=$(echo "$METADATA" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('ObjectLockRetainUntilDate','N/A'))")
EXPECTED_CHECKSUM=$(echo "$METADATA" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('Metadata',{}).get('checksum',''))")

log "  Object Lock Mode : ${LOCK_MODE}"
log "  Retención hasta  : ${RETAIN_UNTIL}"
log "  Checksum esperado: ${EXPECTED_CHECKSUM}"

# ── 2. Descargar backup cifrado ───────────────────────────────────────────────
log "Descargando s3://${S3_BUCKET}/${S3_KEY} ..."
aws s3api get-object \
    --profile "$AWS_PROFILE" \
    --region  "$AWS_REGION" \
    --bucket  "$S3_BUCKET" \
    --key     "$S3_KEY" \
    "$ENCRYPTED_FILE"

log "Descarga completada: $(du -sh "$ENCRYPTED_FILE" | cut -f1)"

# ── 3. Verificar integridad antes de restaurar ────────────────────────────────
ACTUAL_CHECKSUM=$(sha256sum "$ENCRYPTED_FILE" | awk '{print $1}')
log "  Checksum real   : ${ACTUAL_CHECKSUM}"

if [[ -n "$EXPECTED_CHECKSUM" && "$ACTUAL_CHECKSUM" != "$EXPECTED_CHECKSUM" ]]; then
    log_error "¡Checksum no coincide! El backup puede estar corrupto."
    log_error "  Esperado : ${EXPECTED_CHECKSUM}"
    log_error "  Real     : ${ACTUAL_CHECKSUM}"
    exit 1
fi
log "Integridad verificada ✔"

# ── 4. Descifrar ──────────────────────────────────────────────────────────────
log "Descifrando backup..."
openssl enc -d -aes-256-cbc -pbkdf2 -iter 100000 \
    -pass "file:${ENCRYPT_KEY_FILE}" \
    -in  "$ENCRYPTED_FILE" \
    -out "$DECRYPTED_FILE"

# ── 5. Restaurar en MySQL ─────────────────────────────────────────────────────
log "Restaurando en ${DB_NAME}@${DB_HOST}:${DB_PORT} ..."
gunzip -c "$DECRYPTED_FILE" \
    | mysql \
        --host="$DB_HOST" \
        --port="$DB_PORT" \
        --user="$DB_USER" \
        --password="$DB_PASSWORD" \
        "$DB_NAME"

log "✅ Restauración completada exitosamente"
log "   Base de datos : ${DB_NAME}"
log "   Backup origen : s3://${S3_BUCKET}/${S3_KEY}"
