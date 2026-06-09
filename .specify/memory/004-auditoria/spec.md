# Feature Specification: Log de Auditoría con Hash Encadenado

**Feature Branch**: `004-auditoria`

**Created**: 2026-06-09

**Status**: Draft

**Input**: User description: "Log de auditoría inmutable con hash encadenado"

## Constitution Check

| Principio | Aplica | Notas |
|---|---|---|
| I. Alta Concurrencia | ✅ | Inserts de auditoría deben ser no-bloqueantes bajo alta carga de inscripción |
| II. JWT Auth | ✅ | `GET /api/auditoria` requiere JWT con rol `admin` exclusivamente |
| III. Inmutabilidad/Auditoría | ✅ NÚCLEO | Esta feature es la implementación directa de la infraestructura de auditoría del Principio III |
| IV. Stack Definido | ✅ | MySQL 8 (triggers o lógica en Spring) + React 18 |
| V. Privacidad Ley 25.326 | ✅ | El log de auditoría solo es accesible por admins; los datos de auditoría son datos personales |
| VI. Disponibilidad | ✅ | Retención mínima 5 años; almacenamiento inmutable |

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — El sistema registra automáticamente cada cambio sobre notas (Priority: P1)

Cada INSERT o UPDATE sobre la tabla `notas` genera automáticamente un registro en la
tabla de auditoría con hash encadenado al registro anterior.

**Why this priority**: Sin registros de auditoría, la inmutabilidad prometida por la
constitución no es verificable. Es la base de la integridad académica del sistema.

**Independent Test**: Después de `PUT /api/actas/{actaId}/notas/{alumnoId}`, verificar
que existe exactamente un nuevo registro en `auditoria_notas` con `valorAnterior`,
`valorNuevo`, `usuarioJwt`, `timestamp`, `ip` y `hashRegistro` calculado correctamente.

**Acceptance Scenarios**:

1. **Given** un docente que carga una nota por primera vez, **When** se persiste la
   nota, **Then** se inserta un registro en `auditoria_notas` con: `accionTipo = INSERT`,
   `valorAnterior = null`, `valorNuevo = X`, `usuarioJwt = <sub del JWT>`,
   `timestamp = now()`, `ip = <IP del request>`, y `hashRegistro = SHA-256(id +
   usuarioJwt + timestamp + valorNuevo + hashAnterior)`.

2. **Given** un docente que actualiza una nota existente, **When** se persiste el
   cambio, **Then** se inserta un registro con `accionTipo = UPDATE`, `valorAnterior`
   con el valor previo, `hashRegistro` encadenado al hash del registro previo de esa nota.

3. **Given** el cierre de un acta, **When** se ejecuta `POST /api/actas/{id}/cerrar`,
   **Then** se registra un evento de tipo `ACTA_CERRADA` en la tabla `auditoria_eventos`
   con usuario JWT, timestamp e IP.

4. **Given** 1.000 inserts de auditoría simultáneos, **When** se verifica la cadena de
   hashes, **Then** NO hay gaps ni hashes inválidos — la cadena es continua y verificable.

---

### User Story 2 — Admin consulta el log de auditoría con filtros (Priority: P1)

El Decanato (usuario con rol `admin`) puede consultar el log completo de auditoría
filtrado por materia, período, alumno o docente, con paginación.

**Why this priority**: La constitución exige que `GET /api/auditoria` sea accesible
para el Decanato. Sin esto, la inmutabilidad no es auditable externamente.

**Independent Test**: `GET /api/auditoria?materiaId=1&page=0&size=50` con JWT de admin
retorna HTTP 200 con los registros paginados y metadatos de paginación.

**Acceptance Scenarios**:

1. **Given** un admin autenticado, **When** llama a `GET /api/auditoria` con filtros
   opcionales (`materiaId`, `alumnoId`, `docenteId`, `desde`, `hasta`, `tipo`),
   **Then** el sistema retorna los registros de auditoría paginados (máx. 100 por
   página) ordenados por `timestamp` descendente.

2. **Given** un usuario con rol `docente` o `alumno`, **When** intenta acceder a
   `GET /api/auditoria`, **Then** el sistema retorna HTTP 403 "Acceso denegado:
   se requiere rol admin."

3. **Given** un admin que quiere verificar la integridad de la cadena de hashes de una
   nota específica, **When** llama a `GET /api/auditoria/notas/{notaId}/cadena`,
   **Then** el sistema retorna todos los registros de auditoría de esa nota en orden
   cronológico con indicador de validez de hash.

---

### User Story 3 — El sistema detecta y alerta sobre manipulación de registros de auditoría (Priority: P2)

Si alguien modifica directamente la base de datos y altera un registro de auditoría,
el sistema puede detectar la inconsistencia en la cadena de hashes.

**Why this priority**: Garantiza que la auditoría no pueda ser "limpiada" silenciosamente,
cumpliendo el requisito de integridad de la constitución (equivalente blockchain).

**Independent Test**: Al ejecutar `GET /api/auditoria/verificar-integridad`, el sistema
recorre la cadena y reporta cualquier hash inválido.

**Acceptance Scenarios**:

1. **Given** una cadena de auditoría íntegra, **When** un admin llama a
   `GET /api/auditoria/verificar-integridad?desde=<fecha>`, **Then** el sistema
   retorna `{ "integro": true, "registrosVerificados": N, "errores": [] }`.

2. **Given** un registro de auditoría modificado manualmente en la DB, **When** se
   ejecuta la verificación de integridad, **Then** el sistema detecta el hash inválido
   y retorna `{ "integro": false, "errores": [{ "id": X, "detalle": "Hash inválido" }] }`.

---

### User Story 4 — Admin visualiza log de auditoría en el frontend (Priority: P2)

El componente `VistaAuditoria.tsx` muestra el log de auditoría con filtros, búsqueda
y exportación, consumiendo `GET /api/auditoria`.

**Why this priority**: Sin interfaz gráfica, el Decanato no puede consultar el log sin
conocimientos técnicos.

**Independent Test**: El componente `VistaAuditoria.tsx` carga y muestra correctamente
10.000 registros de auditoría paginados (no de una sola vez).

**Acceptance Scenarios**:

1. **Given** un admin en la vista de auditoría, **When** aplica filtros por fecha y
   materia, **Then** la tabla se actualiza mostrando solo los registros que coinciden.

2. **Given** la vista de auditoría con resultados visibles, **When** el admin hace clic
   en "Exportar CSV", **Then** se descarga un archivo CSV con los registros actuales.

---

### Edge Cases

- ¿Qué pasa si el INSERT en auditoría falla mientras se guarda una nota?
  → La transacción de Hibernate DEBE hacer rollback completo. No puede haber notas
  persistidas sin su registro de auditoría correspondiente.
- ¿Qué pasa si la tabla de auditoría crece a millones de registros?
  → El endpoint debe soportar consulta eficiente via índices en `timestamp`, `materiaId`,
  `alumnoId`. Paginación obligatoria (máx. 100 registros por request).
- ¿Qué pasa con el hash del primer registro (sin hash previo)?
  → El `hashAnterior` del primer registro de auditoría es una constante definida
  (`GENESIS_HASH = SHA-256("GENESIS-AUDITORIA-INSTITUCIONAL")`).
- ¿Qué pasa si dos transacciones intentan insertar en auditoría simultáneamente?
  → El ID del registro de auditoría es auto-incremental. La secuencia de hashes se
  basa en `ORDER BY id ASC` para la cadena por nota específica.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La tabla `auditoria_notas` en MySQL DEBE almacenar cada cambio sobre
  notas con: `id` (PK, auto-increment), `notaId`, `accionTipo` (INSERT/UPDATE),
  `valorAnterior` (nullable), `valorNuevo`, `usuarioJwt`, `timestamp`, `ip`,
  `hashAnterior`, `hashRegistro`.
- **FR-002**: `hashRegistro` DEBE calcularse como
  `SHA-256(id || usuarioJwt || timestamp || valorNuevo || hashAnterior)` — en Java,
  usando `MessageDigest.getInstance("SHA-256")`.
- **FR-003**: El registro de auditoría DEBE insertarse dentro de la misma transacción
  Hibernate que persiste la nota. Si el INSERT de auditoría falla, la transacción
  completa hace rollback.
- **FR-004**: El endpoint `GET /api/auditoria` DEBE requerir JWT con rol `admin` y
  soportar filtros: `materiaId`, `alumnoId`, `docenteId`, `desde` (ISO-8601),
  `hasta` (ISO-8601), `tipo`. Paginación: parámetros `page` y `size` (máx. 100).
- **FR-005**: El endpoint `GET /api/auditoria/notas/{notaId}/cadena` DEBE retornar
  todos los registros de auditoría de una nota en orden cronológico con flag de
  `hashValido` por registro.
- **FR-006**: El endpoint `GET /api/auditoria/verificar-integridad` DEBE recorrer
  la cadena de hashes para un rango de fechas y reportar inconsistencias.
- **FR-007**: La tabla `auditoria_eventos` DEBE registrar eventos de negocio de alto
  nivel: `ACTA_CERRADA`, `RECTIFICACION_SOLICITADA`, `RECTIFICACION_APROBADA`,
  `LOGIN_FALLIDO`, `CUENTA_BLOQUEADA`.
- **FR-008**: Los índices de MySQL en `auditoria_notas` DEBEN incluir: `(timestamp)`,
  `(notaId, timestamp)`, `(usuarioJwt, timestamp)` para soportar queries eficientes.
- **FR-009**: La retención de datos de auditoría es de mínimo 5 años (alineada con
  backups del Principio VI de la constitución).
- **FR-010**: El endpoint `GET /api/auditoria` DEBE probarse con un dataset mínimo de
  10.000 registros antes del deployment a producción (requisito de la constitución).

### Contratos de API

| Método | Path | Rol JWT requerido | Respuesta |
|---|---|---|---|
| `GET` | `/api/auditoria` | `admin` | 200 `Page<AuditoriaDTO>` con filtros y paginación |
| `GET` | `/api/auditoria/notas/{notaId}/cadena` | `admin` | 200 `[ AuditoriaDetalleDTO ]` con `hashValido` |
| `GET` | `/api/auditoria/verificar-integridad` | `admin` | 200 `{ integro, registrosVerificados, errores[] }` |
| `GET` | `/api/auditoria/eventos` | `admin` | 200 `Page<EventoAuditoriaDTO>` con filtros |

### Key Entities

- **AuditoriaNotas**: `id`, `notaId`, `accionTipo`, `valorAnterior`, `valorNuevo`,
  `usuarioJwt`, `timestamp`, `ip`, `hashAnterior`, `hashRegistro`.
- **AuditoriaEventos**: `id`, `tipoEvento`, `usuarioJwt`, `entidadTipo`, `entidadId`,
  `descripcion`, `timestamp`, `ip`.

### Esquema DDL de referencia

```sql
CREATE TABLE auditoria_notas (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  nota_id         BIGINT NOT NULL,
  accion_tipo     ENUM('INSERT','UPDATE') NOT NULL,
  valor_anterior  DECIMAL(5,2),
  valor_nuevo     DECIMAL(5,2) NOT NULL,
  usuario_jwt     VARCHAR(255) NOT NULL,
  timestamp       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  ip              VARCHAR(45) NOT NULL,
  hash_anterior   CHAR(64) NOT NULL,
  hash_registro   CHAR(64) NOT NULL,
  INDEX idx_nota_ts  (nota_id, timestamp),
  INDEX idx_usuario  (usuario_jwt, timestamp),
  INDEX idx_ts       (timestamp)
) ENGINE=InnoDB;

CREATE TABLE auditoria_eventos (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  tipo_evento     VARCHAR(50) NOT NULL,
  usuario_jwt     VARCHAR(255),
  entidad_tipo    VARCHAR(50),
  entidad_id      BIGINT,
  descripcion     TEXT,
  timestamp       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  ip              VARCHAR(45),
  INDEX idx_evento_ts (tipo_evento, timestamp),
  INDEX idx_entidad   (entidad_tipo, entidad_id)
) ENGINE=InnoDB;
```

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El endpoint `GET /api/auditoria` con 10.000 registros responde en ≤ 500ms
  con paginación de 100 registros (verificado con dataset de prueba antes de producción).
- **SC-002**: La verificación de integridad de la cadena de hashes sobre 10.000 registros
  completa en ≤ 30 segundos.
- **SC-003**: 0 notas persistidas sin su registro de auditoría correspondiente —
  verificado con constraint de integridad referencial y tests de integración con rollback
  simulado.
- **SC-004**: El `GET /api/auditoria/verificar-integridad` detecta correctamente el
  100% de los registros manipulados en tests de mutación de datos.

---

## Assumptions

- La tabla de auditoría es "append-only": no existe endpoint `DELETE` ni `PUT` sobre
  registros de auditoría. Esto es por diseño y se refleja en la ausencia de esos
  endpoints en la API.
- La IP del request se extrae del header `X-Forwarded-For` (o `RemoteAddr` si no existe
  proxy). En entornos con load balancer, el proxy DEBE propagar el header correctamente.
- El `VistaAuditoria.tsx` del frontend ya existe; este spec define el contrato de API
  que consume.
- Blockchain está explícitamente fuera del alcance (Constitución, Sección Stack
  Tecnológico Obligatorio). La cadena de hashes en MySQL provee garantías equivalentes.
- Los backups de la tabla de auditoría siguen la misma política de retención que el
  resto de la DB: 5 años mínimo en almacenamiento inmutable (S3 con Object Lock o
  equivalente).
- El `GENESIS_HASH` es una constante pública documentada en el código fuente:
  `SHA-256("GENESIS-AUDITORIA-INSTITUCIONAL")`.
