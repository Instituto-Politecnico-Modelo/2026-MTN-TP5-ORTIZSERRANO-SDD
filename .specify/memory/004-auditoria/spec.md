# Feature Specification: Log de Auditoría con Hash Encadenado

**Feature Branch**: `004-auditoria`

**Created**: 2026-06-09

**Status**: Clarified

**Input**: User description- **FR-004**: El endpoint `GET /api/admin/auditoria` DEBE requerir JWT con rol
  `ADMINISTRADOR` y soportar paginación obligatoria con parámetros `page` (base 0) y
  `size` (máx. 100, default 50). La respuesta es un objeto de Spring Page:
  `{ content: [], totalElements, totalPages, number }`. No se permite retornar todos
  los registros sin paginación — **en alcance v1**.Log de auditoría inmutable con hash encadenado"

## Clarification Notes *(added 2026-06-09 — reconciliado con código existente)*

> | # | Ítem | Spec Draft | Clarificado |
> |---|---|---|---|
> | C1 | **Base URL** | `/api/auditoria` | `/api/admin/auditoria` |
> | C2 | **Filtro por entidad** | `GET /api/auditoria?materiaId=&alumnoId=...` (query params) | Rutas separadas: `/entidad/{entidad}`, `/entidad/{entidad}/{id}`, `/usuario/{idUsuario}`, `/accion/{accion}` |
> | C3 | **Verificar integridad** | `GET /api/auditoria/verificar-integridad` | `GET /api/admin/auditoria/verificar` |
> | C4 | **RegistroAuditoria campos** | `notaId, accionTipo, valorAnterior, valorNuevo, usuarioJwt, ip, hashAnterior, hashRegistro` | `idRegistro, entidad, idEntidad, accion, idUsuario, emailUsuario, descripcion, ipOrigen, timestampEvento, hashAnterior, hashActual` |
> | C5 | **IntegridadResponse campos** | `{ integro, registrosVerificados, errores[] }` | `{ integra, totalRegistros, errores: string[], mensaje }` |
> | C6 | **Cadena por nota** | `GET /api/auditoria/notas/{notaId}/cadena` | **No expuesto en frontend v1** — la vista de auditoría filtra por entidad+id: `GET /api/admin/auditoria/entidad/NOTA/{idNota}` |
> | C7 | **Roles** | `admin` | `ADMINISTRADOR` |
> | C8 | **Entidad genérica** | Tabla `auditoria_notas` separada de `auditoria_eventos` | **Tabla unificada**: un registro tiene `entidad` (string: `NOTA`, `INSCRIPCION`, `USUARIO`, etc.) e `idEntidad`, unificando todos los eventos |

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

### Contratos de API *(corregidos en clarification)*

| Método | Path | Rol JWT requerido | Respuesta |
|---|---|---|---|
| `GET` | `/api/admin/auditoria` | `ADMINISTRADOR` | `?page=0&size=50` (máx. 100) → 200 `{ content: [ RegistroAuditoria ], totalElements, totalPages, number }` |
| `GET` | `/api/admin/auditoria/entidad/{entidad}` | `ADMINISTRADOR` | `?page=0&size=50` → 200 paginado |
| `GET` | `/api/admin/auditoria/entidad/{entidad}/{id}` | `ADMINISTRADOR` | `?page=0&size=50` → 200 paginado, ordenado por `timestamp_evento ASC` |
| `GET` | `/api/admin/auditoria/usuario/{idUsuario}` | `ADMINISTRADOR` | `?page=0&size=50` → 200 paginado |
| `GET` | `/api/admin/auditoria/accion/{accion}` | `ADMINISTRADOR` | `?page=0&size=50` → 200 paginado |
| `GET` | `/api/admin/auditoria/verificar` | `ADMINISTRADOR` | → 200 `{ integra, totalRegistros, errores: string[], mensaje }` |

### Key Entities *(corregidos en clarification — tabla unificada)*

- **RegistroAuditoria** (TypeScript: `auditoria.service.ts`): `idRegistro: number`,
  `entidad: string` (tipo de entidad auditada: `NOTA`, `INSCRIPCION`, `USUARIO`, etc.),
  `idEntidad: number | null`, `accion: string` (ej: `INSERT`, `UPDATE`, `DELETE`,
  `LOGIN_FALLIDO`, `ACTA_CERRADA`), `idUsuario: number | null`, `emailUsuario: string`,
  `descripcion: string`, `ipOrigen: string | null`, `timestampEvento: string` (ISO-8601),
  `hashAnterior: string`, `hashActual: string`.
- **IntegridadResponse**: `integra: boolean`, `totalRegistros: number`,
  `errores: string[]`, `mensaje: string`.

> **Tabla unificada**: No existen tablas separadas `auditoria_notas` / `auditoria_eventos`.
> El campo `entidad` (string) discrimina el tipo de registro — todos los eventos de
> auditoría del sistema van a la misma tabla.

### Esquema DDL de referencia *(corregido en clarification — tabla unificada)*

```sql
CREATE TABLE auditoria (
  id_registro      BIGINT AUTO_INCREMENT PRIMARY KEY,
  entidad          VARCHAR(50) NOT NULL,          -- 'NOTA', 'INSCRIPCION', 'USUARIO', etc.
  id_entidad       BIGINT,
  accion           VARCHAR(50) NOT NULL,          -- 'INSERT', 'UPDATE', 'DELETE', 'LOGIN_FALLIDO'
  id_usuario       BIGINT,
  email_usuario    VARCHAR(255) NOT NULL,
  descripcion      TEXT,
  ip_origen        VARCHAR(45),
  timestamp_evento DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  hash_anterior    CHAR(64) NOT NULL,
  hash_actual      CHAR(64) NOT NULL,
  INDEX idx_entidad_id   (entidad, id_entidad),
  INDEX idx_usuario      (id_usuario, timestamp_evento),
  INDEX idx_accion       (accion, timestamp_evento),
  INDEX idx_ts           (timestamp_evento)
) ENGINE=InnoDB;
```

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: `GET /api/admin/auditoria?page=0&size=50` con 10.000 registros totales
  en DB responde en ≤ 500ms retornando exactamente 50 registros + metadatos de
  paginación (`totalElements = 10000`, `totalPages = 200`) — verificado antes de
  deployment a producción.
- **SC-002**: La verificación de integridad de la cadena de hashes sobre 10.000 registros
  completa en ≤ 30 segundos.
- **SC-003**: 0 notas persistidas sin su registro de auditoría correspondiente —
  verificado con constraint de integridad referencial y tests de integración con rollback
  simulado.
- **SC-004**: El `GET /api/auditoria/verificar-integridad` detecta correctamente el
  100% de los registros manipulados en tests de mutación de datos.

---

## Assumptions *(actualizados en clarification)*

- La tabla de auditoría es "append-only": no existe endpoint `DELETE` ni `PUT` sobre
  registros de auditoría.
- La IP del request se extrae del header `X-Forwarded-For` (o `RemoteAddr` si no hay proxy).
- El `VistaAuditoria.tsx` del frontend ya existe y consume los endpoints de
  `/api/admin/auditoria`.
- Blockchain está explícitamente fuera del alcance (Constitución). La cadena de hashes
  en la tabla unificada provee garantías equivalentes.
- Los backups de auditoría siguen la política de retención de 5 años mínimo.
- La tabla es **unificada** — un único `entidad: string` discrimina el tipo de registro.
  El `GENESIS_HASH` es `SHA-256("GENESIS-AUDITORIA-INSTITUCIONAL")` para el primer registro.
- El campo `descripcion` contiene texto libre con el detalle del cambio (por ejemplo:
  `"Nota parcial 1 modificada: 7.0 → 8.5 para inscripcion #42"`).
- **Paginación**: El backend implementa paginación obligatoria con `?page=&size=` (máx.
  100 registros por página) — **en alcance v1**. El frontend actualiza `VistaAuditoria.tsx`
  para consumir resultados paginados. No se permite cargar todos los registros de una sola vez.
