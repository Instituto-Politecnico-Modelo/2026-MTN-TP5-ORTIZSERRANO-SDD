#!/usr/bin/env bash
# =============================================================================
# backup-mysql.sh
# Sistema de Decanato IPM — Backup automático de MySQL hacia S3 (Object Lock)
#
# Descripción:
#   1. Genera un dump completo de la base de datos con mysqldump
#   2. Comprime y cifra el archivo con AES-256-CBC (OpenSSL)
#   3. Sube el archivo a S3 con etiqueta de retención Compliance Mode (5 años)
#   4. Verifica la integridad del backup mediante checksum SHA-256
#   5. Registra el resultado en el log local y en CloudWatch Logs
#   6. Envía alerta por SNS si el backup falla
#
# Requisitos:
#   - aws-cli v2 instalado y configurado (perfil: $AWS_PROFILE)
#   - mysqldump disponible en PATH
#   - openssl instalado
#   - Variables de entorno en /etc/decanato/backup.env (ver backup.env.example)
#
# Cron sugerido (crontab -e como usuario decanato-backup):
#   # Backup diario a las 02:00 AM
#   0 2 * * * /opt/decanato/backup/scripts/backup-mysql.sh >> /var/log/decanato/backup.log 2>&1
#
# Retención S3 Object Lock:
#   - Mode: COMPLIANCE (no modificable ni por root de la cuenta AWS)
#   - RetainUntilDate: fecha actual + 5 años (1826 días)
#
# =============================================================================

set -euo pipefail

# ── Cargar variables de entorno ───────────────────────────────────────────────
ENV_FILE="${BACKUP_ENV_FILE:-/etc/decanato/backup.env}"
if [[ ! -f "$ENV_FILE" ]]; then
    echo "[ERROR] Archivo de configuración no encontrado: $ENV_FILE" >&2
    exit 1
fi
# shellcheck source=/dev/null
source "$ENV_FILE"

# ── Validar variables obligatorias ───────────────────────────────────────────
REQUIRED_VARS=(
    DB_HOST DB_PORT DB_NAME DB_USER DB_PASSWORD
    S3_BUCKET S3_PREFIX
    ENCRYPT_KEY_FILE
    AWS_PROFILE AWS_REGION
    SNS_TOPIC_ARN
    LOG_GROUP
)
for var in "${REQUIRED_VARS[@]}"; do
    if [[ -z "${!var:-}" ]]; then
        echo "[ERROR] Variable de entorno requerida no definida: $var" >&2
        exit 1
    fi
done

# ── Constantes ────────────────────────────────────────────────────────────────
TIMESTAMP=$(date -u +"%Y%m%dT%H%M%SZ")
DATE_LABEL=$(date -u +"%Y-%m-%d")
HOSTNAME_LABEL=$(hostname -s)
BACKUP_FILENAME="decanato_${DB_NAME}_${TIMESTAMP}_${HOSTNAME_LABEL}.sql.gz.enc"
CHECKSUM_FILENAME="${BACKUP_FILENAME}.sha256"
TEMP_DIR=$(mktemp -d /tmp/decanato-backup-XXXXXX)
DUMP_FILE="${TEMP_DIR}/dump.sql.gz"
ENCRYPTED_FILE="${TEMP_DIR}/${BACKUP_FILENAME}"
CHECKSUM_FILE="${TEMP_DIR}/${CHECKSUM_FILENAME}"
S3_KEY="${S3_PREFIX}/${DATE_LABEL}/${BACKUP_FILENAME}"
S3_CHECKSUM_KEY="${S3_PREFIX}/${DATE_LABEL}/${CHECKSUM_FILENAME}"

# Retención: 5 años = 1826 días (incluye un año bisiesto)
RETAIN_DAYS=1826
RETAIN_UNTIL=$(date -u -d "+${RETAIN_DAYS} days" +"%Y-%m-%dT00:00:00Z" 2>/dev/null \
               || date -u -v+${RETAIN_DAYS}d +"%Y-%m-%dT00:00:00Z")  # macOS fallback

LOG_PREFIX="[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] [backup-mysql]"
EXIT_CODE=0

# ── Funciones ─────────────────────────────────────────────────────────────────

log() {
    echo "${LOG_PREFIX} $*"
}

log_error() {
    echo "${LOG_PREFIX} [ERROR] $*" >&2
}

cleanup() {
    log "Limpiando archivos temporales..."
    rm -rf "$TEMP_DIR"
}
trap cleanup EXIT

send_alert() {
    local subject="$1"
    local message="$2"
    aws sns publish \
        --profile "$AWS_PROFILE" \
        --region  "$AWS_REGION" \
        --topic-arn "$SNS_TOPIC_ARN" \
        --subject  "$subject" \
        --message  "$message" \
        2>/dev/null || log_error "No se pudo enviar alerta SNS"
}

push_cloudwatch_log() {
    local status="$1"
    local details="$2"
    local json
    json=$(printf '{"timestamp":%d,"message":"%s"}' \
        "$(date -u +%s%3N)" \
        "status=${status} backup=${BACKUP_FILENAME} retain_until=${RETAIN_UNTIL} details=${details}")

    aws logs put-log-events \
        --profile       "$AWS_PROFILE" \
        --region        "$AWS_REGION" \
        --log-group-name  "$LOG_GROUP" \
        --log-stream-name "$(date -u +%Y/%m/%d)/backup-mysql" \
        --log-events    "[${json}]" \
        2>/dev/null || log_error "No se pudo registrar en CloudWatch Logs"
}

# ── 1. Dump de MySQL ──────────────────────────────────────────────────────────
log "Iniciando dump de ${DB_NAME}@${DB_HOST}:${DB_PORT}..."

mysqldump \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --user="$DB_USER" \
    --password="$DB_PASSWORD" \
    --single-transaction \
    --routines \
    --triggers \
    --events \
    --hex-blob \
    --set-gtid-purged=OFF \
    --default-character-set=utf8mb4 \
    "$DB_NAME" \
    | gzip -9 > "$DUMP_FILE"

DUMP_SIZE=$(du -sh "$DUMP_FILE" | cut -f1)
log "Dump completado: ${DUMP_SIZE} comprimido"

# ── 2. Cifrado AES-256-CBC ────────────────────────────────────────────────────
log "Cifrando con AES-256-CBC (clave en ${ENCRYPT_KEY_FILE})..."

if [[ ! -f "$ENCRYPT_KEY_FILE" ]]; then
    log_error "Archivo de clave de cifrado no encontrado: $ENCRYPT_KEY_FILE"
    send_alert "❌ Backup FALLIDO — ${DB_NAME}" "Clave de cifrado no encontrada: ${ENCRYPT_KEY_FILE}"
    exit 1
fi

openssl enc -aes-256-cbc -salt -pbkdf2 -iter 100000 \
    -pass "file:${ENCRYPT_KEY_FILE}" \
    -in  "$DUMP_FILE" \
    -out "$ENCRYPTED_FILE"

log "Cifrado completado: ${BACKUP_FILENAME}"

# ── 3. Checksum SHA-256 ───────────────────────────────────────────────────────
log "Calculando checksum SHA-256..."
sha256sum "$ENCRYPTED_FILE" | awk '{print $1}' > "$CHECKSUM_FILE"
CHECKSUM=$(cat "$CHECKSUM_FILE")
log "SHA-256: ${CHECKSUM}"

# ── 4. Subir a S3 con Object Lock (Compliance Mode) ──────────────────────────
log "Subiendo a s3://${S3_BUCKET}/${S3_KEY} ..."
log "  Object Lock Mode: COMPLIANCE | RetainUntil: ${RETAIN_UNTIL}"

aws s3api put-object \
    --profile    "$AWS_PROFILE" \
    --region     "$AWS_REGION" \
    --bucket     "$S3_BUCKET" \
    --key        "$S3_KEY" \
    --body       "$ENCRYPTED_FILE" \
    --object-lock-mode             COMPLIANCE \
    --object-lock-retain-until-date "$RETAIN_UNTIL" \
    --server-side-encryption       aws:kms \
    --ssekms-key-id                "${KMS_KEY_ID:-aws/s3}" \
    --metadata "db=${DB_NAME},host=${DB_HOST},timestamp=${TIMESTAMP},checksum=${CHECKSUM},retain_days=${RETAIN_DAYS}"

log "Backup principal subido a S3"

# Subir checksum junto al backup
aws s3api put-object \
    --profile "$AWS_PROFILE" \
    --region  "$AWS_REGION" \
    --bucket  "$S3_BUCKET" \
    --key     "$S3_CHECKSUM_KEY" \
    --body    "$CHECKSUM_FILE" \
    --object-lock-mode             COMPLIANCE \
    --object-lock-retain-until-date "$RETAIN_UNTIL" \
    --metadata "backup=${BACKUP_FILENAME}"

log "Checksum subido a S3"

# ── 5. Verificar integridad post-upload ───────────────────────────────────────
log "Verificando integridad del objeto en S3..."
REMOTE_CHECKSUM=$(aws s3api head-object \
    --profile "$AWS_PROFILE" \
    --region  "$AWS_REGION" \
    --bucket  "$S3_BUCKET" \
    --key     "$S3_KEY" \
    --query   'Metadata.checksum' \
    --output  text)

if [[ "$REMOTE_CHECKSUM" != "$CHECKSUM" ]]; then
    log_error "¡Verificación de integridad FALLIDA! Local=${CHECKSUM} Remote=${REMOTE_CHECKSUM}"
    send_alert "❌ Backup CORRUPTO — ${DB_NAME}" \
        "El checksum del objeto en S3 no coincide.\nBackup: ${S3_KEY}\nLocal: ${CHECKSUM}\nRemoto: ${REMOTE_CHECKSUM}"
    EXIT_CODE=1
else
    log "Integridad verificada ✔"
fi

# ── 6. Registrar en CloudWatch + SNS (éxito) ──────────────────────────────────
if [[ $EXIT_CODE -eq 0 ]]; then
    push_cloudwatch_log "SUCCESS" "size=${DUMP_SIZE} checksum=${CHECKSUM}"
    log "✅ Backup completado exitosamente"
    log "   Archivo : s3://${S3_BUCKET}/${S3_KEY}"
    log "   Retención: hasta ${RETAIN_UNTIL} (${RETAIN_DAYS} días)"
else
    push_cloudwatch_log "FAILURE" "integrity_check_failed checksum=${CHECKSUM}"
    exit $EXIT_CODE
fi
