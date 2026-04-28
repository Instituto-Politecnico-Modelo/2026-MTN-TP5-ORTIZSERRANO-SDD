# Estrategia de Backups MySQL — Sistema de Decanato IPM

## Resumen ejecutivo

| Atributo | Valor |
|---|---|
| **Motor de base de datos** | MySQL 8.x |
| **Frecuencia** | Diaria (02:00 AM UTC) |
| **Retención** | **5 años (1826 días)** |
| **Almacenamiento** | AWS S3 con Object Lock — modo **COMPLIANCE** |
| **Cifrado en tránsito** | TLS 1.2+ (HTTPS hacia S3) |
| **Cifrado en reposo (local)** | AES-256-CBC con OpenSSL + PBKDF2 |
| **Cifrado en reposo (S3)** | SSE-KMS con CMK dedicada |
| **Verificación de integridad** | SHA-256 checksums |
| **Alertas** | AWS SNS → email/Slack |
| **Auditoría de accesos** | AWS CloudTrail + S3 Access Logs |

---

## Arquitectura

```
Servidor de producción
│
├── [02:00 AM] backup-mysql.sh
│     │
│     ├── 1. mysqldump --single-transaction → dump.sql.gz
│     │
│     ├── 2. openssl enc AES-256-CBC → dump.sql.gz.enc
│     │
│     ├── 3. sha256sum → checksum .sha256
│     │
│     └── 4. aws s3api put-object
│               ├── Object-Lock-Mode: COMPLIANCE
│               ├── RetainUntilDate: NOW + 1826 días
│               ├── SSE: aws:kms
│               └── Metadata: checksum, db, host, timestamp
│
AWS S3 (Object Lock COMPLIANCE)
│
├── backups/
│   ├── 2026-04-28/
│   │   ├── decanato_decanato_db_20260428T020001Z_web01.sql.gz.enc
│   │   └── decanato_decanato_db_20260428T020001Z_web01.sql.gz.enc.sha256
│   └── ...
│
├── [90 días]  → S3 Glacier Instant Retrieval
└── [180 días] → S3 Glacier Deep Archive
```

---

## Archivos del módulo

```
backup/
├── scripts/
│   ├── setup-s3-bucket.sh    ← Ejecutar una sola vez al provisionar
│   ├── backup-mysql.sh       ← Script principal de backup (cron diario)
│   ├── restore-mysql.sh      ← Restauración con confirmación interactiva
│   └── verify-backups.sh     ← Verificación periódica (cron diario)
├── backup.env.example        ← Template de configuración
├── s3-bucket-policy.json     ← Política IAM del bucket
└── README.md                 ← Este archivo
```

---

## Setup inicial (una sola vez)

### 1. Crear usuario MySQL de solo lectura para backups

```sql
CREATE USER 'decanato_backup_user'@'localhost'
    IDENTIFIED BY 'PASSWORD_SEGURA';

GRANT SELECT, LOCK TABLES, SHOW VIEW, EVENT, TRIGGER
    ON decanato_db.*
    TO 'decanato_backup_user'@'localhost';

FLUSH PRIVILEGES;
```

### 2. Generar clave de cifrado local

```bash
# Generar clave aleatoria de 256 bits
openssl rand -hex 32 > /etc/decanato/backup-encrypt.key
chmod 400 /etc/decanato/backup-encrypt.key

# CRÍTICO: guardar esta clave en un gestor de secretos (AWS Secrets Manager,
# HashiCorp Vault, o impresa y en caja fuerte física)
```

### 3. Configurar variables de entorno

```bash
cp backup/backup.env.example /etc/decanato/backup.env
chmod 600 /etc/decanato/backup.env
# Editar /etc/decanato/backup.env con los valores reales
```

### 4. Crear el bucket S3 con Object Lock

```bash
# Solo puede habilitarse al crear el bucket; no se puede agregar después
chmod +x backup/scripts/setup-s3-bucket.sh
AWS_PROFILE=decanato-admin \
AWS_REGION=us-east-1 \
  backup/scripts/setup-s3-bucket.sh
```

### 5. Aplicar política del bucket

```bash
# Reemplazar NOMBRE_BUCKET y CUENTA_ID en s3-bucket-policy.json
aws s3api put-bucket-policy \
    --bucket decanato-ipm-backups \
    --policy file://backup/s3-bucket-policy.json
```

### 6. Instalar scripts y configurar cron

```bash
sudo mkdir -p /opt/decanato/backup/scripts
sudo cp backup/scripts/*.sh /opt/decanato/backup/scripts/
sudo chmod +x /opt/decanato/backup/scripts/*.sh

# Editar crontab del usuario decanato-backup
sudo crontab -u decanato-backup -e
```

Agregar:
```cron
# Backup diario a las 02:00 AM
0 2 * * * /opt/decanato/backup/scripts/backup-mysql.sh >> /var/log/decanato/backup.log 2>&1

# Verificación diaria a las 06:00 AM
0 6 * * * /opt/decanato/backup/scripts/verify-backups.sh 7 >> /var/log/decanato/verify.log 2>&1
```

---

## Restauración

### Listar backups disponibles

```bash
aws s3 ls s3://decanato-ipm-backups/backups/ --recursive \
    | grep ".enc$" | sort -r | head -30
```

### Restaurar un backup

```bash
# ⚠️ SOLO en ventana de mantenimiento — sobreescribe la base de datos
./backup/scripts/restore-mysql.sh \
    backups/2026-04-28/decanato_decanato_db_20260428T020001Z_web01.sql.gz.enc
```

### Descifrar manualmente (sin MySQL)

```bash
openssl enc -d -aes-256-cbc -pbkdf2 -iter 100000 \
    -pass "file:/etc/decanato/backup-encrypt.key" \
    -in  backup.sql.gz.enc \
    -out backup.sql.gz

gunzip backup.sql.gz
# → backup.sql listo para inspección o importación manual
```

---

## Por qué Object Lock en modo COMPLIANCE

| Característica | GOVERNANCE | **COMPLIANCE** |
|---|---|---|
| ¿Puede eliminarse antes de RetainUntil? | Sí (con permiso `s3:BypassGovernanceRetention`) | **No** — ni el root de la cuenta AWS |
| Válido para auditorías regulatorias | Parcialmente | **Sí** |
| ¿Admisible como evidencia | No garantizado | **Sí** |

> En modo COMPLIANCE, **nadie** — ni AWS Support ni la cuenta root — puede borrar
> ni modificar un objeto antes de que venza `RetainUntilDate`. Esto es lo que
> garantiza la cadena de custodia para auditorías de 5 años.

---

## Costos estimados (referencia, escenario típico)

| Componente | Supuesto | Costo estimado/mes |
|---|---|---|
| S3 Standard (primeros 90 días) | 10 GB × 90 backups | ~$0.23 |
| S3 Glacier Instant Retrieval | 10 GB × 270 backups | ~$0.27 |
| S3 Glacier Deep Archive | 10 GB × resto (hasta 5 años) | ~$0.10 |
| KMS (solicitudes) | ~30 put-object/mes | ~$0.01 |
| CloudWatch Logs | Log mínimo | ~$0.01 |
| **Total aproximado** | | **~$0.62/mes** |

---

## Checklist de cumplimiento

- [x] Backups diarios automatizados
- [x] Retención mínima 5 años (1826 días)
- [x] Object Lock modo COMPLIANCE (inmutable)
- [x] Cifrado en reposo: AES-256 local + SSE-KMS en S3
- [x] Cifrado en tránsito: TLS (política DenyNonTLS en bucket policy)
- [x] Verificación de integridad SHA-256 post-upload
- [x] Alertas automáticas ante fallo o días sin backup
- [x] Auditoría de accesos (CloudTrail + S3 Access Logs)
- [x] Transición automática a almacenamiento de bajo costo (Glacier)
- [x] Script de restauración con confirmación interactiva
- [x] Separación de roles IAM: agente de backup ≠ auditor ≠ admin
