# Implementation Checklist: Log de Auditoría con Hash Encadenado

**Purpose**: Verificación de integridad del log, hash encadenado, API de consulta y performance
**Created**: 2026-06-09
**Feature**: [spec.md](./spec.md)
**Branch**: `004-auditoria`

---

## Constitución — Compliance Gate *(verificar antes de merge — Principio III NO NEGOCIABLE)*

- [ ] CHK001 Principio III — La tabla `auditoria` es append-only: no existe endpoint DELETE, PUT ni PATCH sobre registros de auditoría en toda la API
- [ ] CHK002 Principio III — El `hashActual` de cada registro es verificable: `SHA-256(idRegistro || emailUsuario || timestampEvento || descripcion || hashAnterior)` produce el mismo valor
- [ ] CHK003 Principio III — `GET /api/admin/auditoria/verificar` detecta cualquier registro con `hashActual` corrupto (test con mutación manual en DB → `integra: false`)
- [ ] CHK004 Principio II — `GET /api/admin/auditoria` retorna HTTP 403 para cualquier JWT con rol que no sea `ADMINISTRADOR`
- [ ] CHK005 Principio VI — La política de retención de backups de la tabla `auditoria` está configurada en 5 años mínimo

---

## Backend — Tabla y schema

- [ ] CHK006 La tabla `auditoria` existe en MySQL con los campos: `id_registro` (PK BIGINT AUTO_INCREMENT), `entidad`, `id_entidad`, `accion`, `id_usuario`, `email_usuario`, `descripcion`, `ip_origen`, `timestamp_evento`, `hash_anterior`, `hash_actual`
- [ ] CHK007 Los índices `idx_entidad_id (entidad, id_entidad)`, `idx_usuario (id_usuario, timestamp_evento)`, `idx_accion (accion, timestamp_evento)`, `idx_ts (timestamp_evento)` están presentes
- [ ] CHK008 El campo `timestamp_evento` usa precisión de milisegundos `DATETIME(3)`
- [ ] CHK009 El campo `hash_anterior` del primer registro global es `SHA-256("GENESIS-AUDITORIA-INSTITUCIONAL")` (constante GENESIS_HASH definida en código fuente)
- [ ] CHK010 No existe constraint de FK hacia otras tablas desde `auditoria` (los campos `id_entidad` e `id_usuario` son referencias lógicas, no FK físicas, para preservar el log ante borrados)

---

## Backend — Cálculo de hash encadenado

- [ ] CHK011 La implementación usa `MessageDigest.getInstance("SHA-256")` de Java standard library (no librería externa)
- [ ] CHK012 La concatenación para el hash incluye al menos: `idRegistro`, `emailUsuario`, `timestampEvento`, `descripcion` y `hashAnterior` (en orden determinístico documentado)
- [ ] CHK013 La constante `GENESIS_HASH` está definida como `public static final String` en el código fuente (no hardcodeada inline en múltiples lugares)
- [ ] CHK014 El `hashAnterior` de un nuevo registro se recupera con `SELECT hash_actual FROM auditoria WHERE id_registro = (SELECT MAX(id_registro) FROM auditoria)` o equivalente thread-safe
- [ ] CHK015 El INSERT de auditoría ocurre dentro de la misma transacción `@Transactional` que el INSERT/UPDATE de la entidad auditada; si falla, hay rollback completo

---

## Backend — Endpoints de consulta (`/api/admin/auditoria`)

- [ ] CHK016 `GET /api/admin/auditoria` retorna `[ RegistroAuditoria ]` con todos los campos: `idRegistro, entidad, idEntidad, accion, idUsuario, emailUsuario, descripcion, ipOrigen, timestampEvento, hashAnterior, hashActual`
- [ ] CHK017 `GET /api/admin/auditoria/entidad/{entidad}` filtra correctamente por el campo `entidad` (ej: `NOTA`, `INSCRIPCION`, `USUARIO`)
- [ ] CHK018 `GET /api/admin/auditoria/entidad/{entidad}/{id}` retorna todos los registros de esa entidad específica, ordenados por `timestamp_evento ASC`
- [ ] CHK019 `GET /api/admin/auditoria/usuario/{idUsuario}` retorna todos los registros generados por ese usuario
- [ ] CHK020 `GET /api/admin/auditoria/accion/{accion}` filtra correctamente por tipo de acción (ej: `UPDATE`, `LOGIN_FALLIDO`, `ACTA_CERRADA`)
- [ ] CHK021 `GET /api/admin/auditoria/verificar` retorna `{ integra: boolean, totalRegistros: number, errores: string[], mensaje: string }`
- [ ] CHK022 `GET /api/admin/auditoria/verificar` detecta gaps en la cadena de hashes (hash inválido) y los reporta en el array `errores`
- [ ] CHK023 `GET /api/admin/auditoria/verificar` retorna `integra: true` y `errores: []` cuando la cadena está íntegra
- [ ] CHK024 Todos los endpoints de `/api/admin/auditoria/**` retornan HTTP 403 para JWT con rol `ESTUDIANTE` o `DOCENTE`
- [ ] CHK025 Todos los endpoints de `/api/admin/auditoria/**` retornan HTTP 401 sin JWT

---

## Backend — Eventos registrados automáticamente

- [ ] CHK026 Cada `PUT /api/docente/inscripciones/{id}/nota` exitoso genera un registro con `entidad = 'NOTA'` (o `'INSCRIPCION'`), `accion = 'UPDATE'` (o `'INSERT'`), `descripcion` con valores de notas anterior/nuevo
- [ ] CHK027 Cada `PATCH /api/docente/inscripciones/{id}/cerrar` genera un registro con `accion = 'ACTA_CERRADA'`
- [ ] CHK028 Cada `POST /api/inscripciones` exitoso genera un registro con `entidad = 'INSCRIPCION'`, `accion = 'INSERT'`
- [ ] CHK029 Cada `PATCH /api/inscripciones/{id}/cancelar` genera un registro con `accion = 'UPDATE'`, `descripcion` con estado anterior y nuevo
- [ ] CHK030 Cada login fallido (`POST /api/auth/login` → HTTP 401) genera un registro con `entidad = 'USUARIO'`, `accion = 'LOGIN_FALLIDO'`, `emailUsuario` = email del intento

---

## Frontend — Integración (`VistaAuditoria.tsx`)

- [ ] CHK031 `auditoriaService.listarTodos()` llama a `GET /api/admin/auditoria` (base URL: `/api/admin/auditoria`)
- [ ] CHK032 `auditoriaService.porEntidad()` llama a `GET /api/admin/auditoria/entidad/{entidad}`
- [ ] CHK033 `auditoriaService.porEntidadYId()` llama a `GET /api/admin/auditoria/entidad/{entidad}/{id}`
- [ ] CHK034 `auditoriaService.porUsuario()` llama a `GET /api/admin/auditoria/usuario/{idUsuario}`
- [ ] CHK035 `auditoriaService.porAccion()` llama a `GET /api/admin/auditoria/accion/{accion}`
- [ ] CHK036 `auditoriaService.verificarIntegridad()` llama a `GET /api/admin/auditoria/verificar` y mapea `IntegridadResponse` correctamente (`integra`, no `integro`)
- [ ] CHK037 `VistaAuditoria.tsx` solo es accesible para rol `ADMINISTRADOR` (protegido por `ProtectedRoute`)
- [ ] CHK038 `VistaAuditoria.tsx` muestra el campo `hashActual` (o equivalente) en la tabla para que el admin pueda verificar visualmente
- [ ] CHK039 El resultado de `verificarIntegridad()` se muestra con un indicador visual claro: ✅ íntegro / ❌ con errores

---

## Testing

- [ ] CHK040 Test unitario: `PUT .../nota` → verificar que se inserta exactamente 1 registro en `auditoria` con los campos correctos
- [ ] CHK041 Test unitario: el `hashActual` calculado del registro recién insertado coincide con recalcular manualmente `SHA-256(campos || hashAnterior)`
- [ ] CHK042 Test unitario: `GET /api/admin/auditoria/verificar` sobre dataset íntegro → `integra: true, errores: []`
- [ ] CHK043 Test de mutación: modificar manualmente un campo `descripcion` en un registro de auditoría en la DB de test → `GET .../verificar` retorna `integra: false` con ese `id_registro` en `errores`
- [ ] CHK044 Test de integración: fallo simulado del INSERT en auditoría durante `PUT .../nota` → transacción rollback; la nota **no** se actualiza en DB
- [ ] CHK045 Test de integración: 1.000 registros de auditoría insertados → verificar cadena completa sin gaps
- [ ] CHK046 Test de seguridad: `GET /api/admin/auditoria` con JWT de ESTUDIANTE → HTTP 403
- [ ] CHK047 Test de seguridad: `GET /api/admin/auditoria` con JWT de DOCENTE → HTTP 403
- [ ] CHK048 Test de seguridad: `GET /api/admin/auditoria` sin JWT → HTTP 401
- [ ] CHK049 Test de performance: `GET /api/admin/auditoria` con dataset de 10.000 registros responde en ≤ 500ms (**requerimiento de la constitución — obligatorio antes de deployment a producción**)
- [ ] CHK050 Test de performance: `GET /api/admin/auditoria/verificar` sobre 10.000 registros completa en ≤ 30 segundos

---

## Deployment

- [ ] CHK051 La migración SQL que crea la tabla `auditoria` con todos sus campos e índices fue aplicada y verificada en producción
- [ ] CHK052 El primer registro de la tabla tiene `hash_anterior = SHA-256("GENESIS-AUDITORIA-INSTITUCIONAL")` (verificar con query directa post-deploy)
- [ ] CHK053 La política de backup cubre la tabla `auditoria` con retención mínima de 5 años en almacenamiento inmutable
- [ ] CHK054 No existe ningún job de limpieza (purge/archiving) que elimine registros de `auditoria` antes de los 5 años
- [ ] CHK055 El endpoint `GET /api/admin/auditoria` fue probado con un dataset mínimo de 10.000 registros **antes del primer deployment a producción** (requerimiento constitución Principio III)
