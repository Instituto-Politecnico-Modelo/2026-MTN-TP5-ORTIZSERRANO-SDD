#!/usr/bin/env bash
# =============================================================================
# setup-s3-bucket.sh
# Sistema de Decanato IPM — Provisioning del bucket S3 con Object Lock
#
# Descripción:
#   Crea y configura el bucket S3 para backups de auditoría con:
#   - Object Lock habilitado (solo posible al crear el bucket)
#   - Retención por defecto: COMPLIANCE, 1826 días (5 años)
#   - Versionado habilitado (requerido por Object Lock)
#   - Cifrado del lado servidor con KMS
#   - Bloqueo de acceso público
#   - Política de ciclo de vida: mover a S3 Glacier Deep Archive a los 90 días
#   - Replicación a bucket secundario en otra región (opcional)
#   - Logging de accesos habilitado
#
# Uso:
#   AWS_PROFILE=decanato-admin AWS_REGION=us-east-1 ./setup-s3-bucket.sh
#
# Ejecutar UNA SOLA VEZ antes de poner en producción el sistema de backups.
#
# =============================================================================

set -euo pipefail

ENV_FILE="${BACKUP_ENV_FILE:-/etc/decanato/backup.env}"
if [[ -f "$ENV_FILE" ]]; then
    # shellcheck source=/dev/null
    source "$ENV_FILE"
fi

# Variables (override con env vars si no hay backup.env)
BUCKET="${S3_BUCKET:-decanato-ipm-backups-$(date +%s)}"
REGION="${AWS_REGION:-us-east-1}"
PROFILE="${AWS_PROFILE:-default}"
KMS_ALIAS="${KMS_ALIAS:-alias/decanato-backups}"
LOG_BUCKET="${LOG_BUCKET:-${BUCKET}-access-logs}"

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  Setup de bucket S3 para backups con Object Lock             ║"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║  Bucket     : ${BUCKET}"
echo "║  Región     : ${REGION}"
echo "║  AWS Profile: ${PROFILE}"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
read -rp "¿Continuar? (s/N): " CONFIRM
[[ "$CONFIRM" =~ ^[sS]$ ]] || { echo "Cancelado."; exit 0; }

# ── 1. Crear KMS key para cifrado de backups ──────────────────────────────────
echo "[1/7] Creando CMK en KMS..."
KMS_KEY_ID=$(aws kms create-key \
    --profile     "$PROFILE" \
    --region      "$REGION" \
    --description "Clave de cifrado para backups MySQL — Sistema Decanato IPM" \
    --key-usage   ENCRYPT_DECRYPT \
    --query       'KeyMetadata.KeyId' \
    --output      text)

aws kms create-alias \
    --profile    "$PROFILE" \
    --region     "$REGION" \
    --alias-name "$KMS_ALIAS" \
    --target-key-id "$KMS_KEY_ID" 2>/dev/null || true

echo "    KMS Key ID: ${KMS_KEY_ID}"
echo "    KMS Alias : ${KMS_ALIAS}"

# ── 2. Crear bucket de logs de acceso ────────────────────────────────────────
echo "[2/7] Creando bucket de logs de acceso..."
if [[ "$REGION" == "us-east-1" ]]; then
    aws s3api create-bucket \
        --profile "$PROFILE" \
        --region  "$REGION" \
        --bucket  "$LOG_BUCKET" 2>/dev/null || echo "    (bucket de logs ya existe)"
else
    aws s3api create-bucket \
        --profile "$PROFILE" \
        --region  "$REGION" \
        --bucket  "$LOG_BUCKET" \
        --create-bucket-configuration LocationConstraint="$REGION" 2>/dev/null || echo "    (bucket de logs ya existe)"
fi

aws s3api put-public-access-block \
    --profile "$PROFILE" \
    --region  "$REGION" \
    --bucket  "$LOG_BUCKET" \
    --public-access-block-configuration \
        "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# ── 3. Crear bucket principal CON Object Lock ─────────────────────────────────
echo "[3/7] Creando bucket principal con Object Lock..."
# IMPORTANTE: Object Lock solo puede habilitarse en el momento de creación
if [[ "$REGION" == "us-east-1" ]]; then
    aws s3api create-bucket \
        --profile              "$PROFILE" \
        --region               "$REGION" \
        --bucket               "$BUCKET" \
        --object-lock-enabled-for-bucket
else
    aws s3api create-bucket \
        --profile              "$PROFILE" \
        --region               "$REGION" \
        --bucket               "$BUCKET" \
        --object-lock-enabled-for-bucket \
        --create-bucket-configuration LocationConstraint="$REGION"
fi

# ── 4. Configurar retención por defecto (5 años, COMPLIANCE) ─────────────────
echo "[4/7] Configurando retención Object Lock por defecto (COMPLIANCE, 1826 días)..."
aws s3api put-object-lock-configuration \
    --profile "$PROFILE" \
    --region  "$REGION" \
    --bucket  "$BUCKET" \
    --object-lock-configuration '{
        "ObjectLockEnabled": "Enabled",
        "Rule": {
            "DefaultRetention": {
                "Mode": "COMPLIANCE",
                "Days": 1826
            }
        }
    }'

# ── 5. Cifrado con KMS + Bloqueo de acceso público ───────────────────────────
echo "[5/7] Habilitando cifrado SSE-KMS y bloqueando acceso público..."
aws s3api put-bucket-encryption \
    --profile "$PROFILE" \
    --region  "$REGION" \
    --bucket  "$BUCKET" \
    --server-side-encryption-configuration "{
        \"Rules\": [{
            \"ApplyServerSideEncryptionByDefault\": {
                \"SSEAlgorithm\": \"aws:kms\",
                \"KMSMasterKeyID\": \"${KMS_KEY_ID}\"
            },
            \"BucketKeyEnabled\": true
        }]
    }"

aws s3api put-public-access-block \
    --profile "$PROFILE" \
    --region  "$REGION" \
    --bucket  "$BUCKET" \
    --public-access-block-configuration \
        "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# ── 6. Política de ciclo de vida (mover a Glacier después de 90 días) ─────────
echo "[6/7] Configurando política de ciclo de vida (Glacier Deep Archive después de 90 días)..."
aws s3api put-bucket-lifecycle-configuration \
    --profile "$PROFILE" \
    --region  "$REGION" \
    --bucket  "$BUCKET" \
    --lifecycle-configuration '{
        "Rules": [
            {
                "ID": "MoverAGlacierDeepArchive",
                "Status": "Enabled",
                "Filter": { "Prefix": "backups/" },
                "Transitions": [
                    {
                        "Days": 90,
                        "StorageClass": "GLACIER_IR"
                    },
                    {
                        "Days": 180,
                        "StorageClass": "DEEP_ARCHIVE"
                    }
                ],
                "NoncurrentVersionTransitions": [
                    {
                        "NoncurrentDays": 30,
                        "StorageClass": "GLACIER_IR"
                    }
                ]
            }
        ]
    }'

# ── 7. Logging de accesos al bucket ──────────────────────────────────────────
echo "[7/7] Habilitando logging de accesos..."
aws s3api put-bucket-logging \
    --profile "$PROFILE" \
    --region  "$REGION" \
    --bucket  "$BUCKET" \
    --bucket-logging-status "{
        \"LoggingEnabled\": {
            \"TargetBucket\": \"${LOG_BUCKET}\",
            \"TargetPrefix\": \"s3-access-logs/${BUCKET}/\"
        }
    }"

# ── Resumen final ─────────────────────────────────────────────────────────────
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  ✅ Bucket configurado exitosamente                          ║"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║  Bucket principal : s3://${BUCKET}"
echo "║  Región           : ${REGION}"
echo "║  Object Lock      : COMPLIANCE — 1826 días (5 años)"
echo "║  Cifrado          : SSE-KMS (${KMS_ALIAS})"
echo "║  Acceso público   : BLOQUEADO"
echo "║  Ciclo de vida    : Glacier IR (90d) → Deep Archive (180d)"
echo "║  Logs de acceso   : s3://${LOG_BUCKET}"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "IMPORTANTE — Agregar al archivo backup.env:"
echo "  S3_BUCKET=${BUCKET}"
echo "  KMS_KEY_ID=${KMS_KEY_ID}"
