# Feature Specification: Gestión de Notas y Cierre de Actas

**Feature Branch**: `003-gestion-notas`

**Created**: 2026-06-09

**Status**: Draft

**Input**: User description: "Gestión de notas y cierre de actas con inmutabilidad"

## Constitution Check

| Principio | Aplica | Notas |
|---|---|---|
| I. Alta Concurrencia | ✅ Parcial | Carga simultánea de actas por múltiples docentes; no requiere Redis pero sí transacciones MySQL correctas |
| II. JWT Auth | ✅ | Solo docentes (sus materias) y admins pueden modificar notas |
| III. Inmutabilidad/Auditoría | ✅ NÚCLEO | Esta feature es la implementación directa del Principio III |
| IV. Stack Definido | ✅ | Spring Boot 3 + Hibernate 6 + MySQL 8 + React 18 |
| V. Privacidad Ley 25.326 | ✅ | Las notas son datos personales del alumno, acceso mínimo privilegio |
| VI. Disponibilidad | ✅ | Backups retienen historial completo de notas; 5 años mínimo |

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Docente carga y actualiza notas antes del cierre del acta (Priority: P1)

Un docente autenticado puede ingresar y modificar notas de los alumnos inscriptos en
una materia de su cargo, mientras el acta esté en estado ABIERTA.

**Why this priority**: Es la funcionalidad central del rol docente. Sin esto, el sistema
no tiene utilidad para la gestión académica.

**Independent Test**: `PUT /api/actas/{actaId}/notas/{alumnoId}` con JWT de docente
de esa materia retorna HTTP 200, persiste la nota y la registra en auditoría.

**Acceptance Scenarios**:

1. **Given** un docente autenticado con JWT (rol `docente`) y un acta ABIERTA de su
   materia, **When** llama a `PUT /api/actas/{actaId}/notas/{alumnoId}` con
   `{ "nota": 8.5, "tipo": "PARCIAL_1" }`, **Then** el sistema persiste la nota,
   retorna HTTP 200 con los datos actualizados, y registra en auditoría: usuario JWT,
   timestamp, IP, valor anterior (null si es la primera vez), valor nuevo, tipo de nota.

2. **Given** un docente intentando cargar notas en un acta CERRADA, **When** llama a
   `PUT /api/actas/{actaId}/notas/{alumnoId}`, **Then** el sistema retorna HTTP 409
   con mensaje "El acta está cerrada. Las notas son inmutables."

3. **Given** una nota fuera del rango válido (negativo o mayor a 10), **When** el
   docente la envía, **Then** el sistema retorna HTTP 400 con detalle de validación.

4. **Given** un docente intentando cargar notas en un acta de una materia que no le
   pertenece, **When** llama al endpoint, **Then** el sistema retorna HTTP 403
   (mínimo privilegio).

---

### User Story 2 — Docente cierra un acta (las notas se vuelven inmutables) (Priority: P1)

Una vez que el docente confirma el cierre del acta, todas las notas de esa acta quedan
permanentemente inmutables. Este es el evento más crítico de integridad académica.

**Why this priority**: El Principio III de la constitución lo declara NO NEGOCIABLE.
Una nota modificada silenciosamente después del cierre es un fraude académico.

**Independent Test**: `POST /api/actas/{id}/cerrar` con JWT de docente cambia estado
a CERRADA; un intento posterior de `PUT .../notas/...` retorna HTTP 409.

**Acceptance Scenarios**:

1. **Given** un acta ABIERTA con todas sus notas cargadas, **When** el docente llama a
   `POST /api/actas/{id}/cerrar`, **Then** el sistema cambia el estado a CERRADA,
   retorna HTTP 200, y registra el evento de cierre en auditoría con timestamp y JWT
   del docente.

2. **Given** un acta CERRADA, **When** cualquier usuario (incluyendo admin) intenta
   `PUT /api/actas/{actaId}/notas/{alumnoId}`, **Then** el sistema retorna HTTP 409
   "El acta está cerrada. Para modificar notas usar el proceso de rectificación."

3. **Given** un acta CERRADA, **When** se llama a `GET /api/actas/{id}`, **Then** el
   sistema retorna las notas con estado INMUTABLE y la fecha/usuario de cierre.

---

### User Story 3 — Proceso formal de rectificación post-cierre (Priority: P2)

Una nota en acta cerrada solo puede modificarse mediante un proceso formal que requiere
aprobación de dos administradores, quedando registrado en auditoría.

**Why this priority**: La constitución lo exige explícitamente. Las notas cerradas
deben ser inmutables salvo error verificado con aprobación dual.

**Independent Test**: Un `PATCH /api/actas/{id}/rectificacion` sin doble aprobación
de admin retorna HTTP 403.

**Acceptance Scenarios**:

1. **Given** un acta CERRADA con un error de nota, **When** un admin inicia
   `POST /api/actas/{id}/rectificacion` con `{ "alumnoId", "nuevaNota", "motivo" }`,
   **Then** el sistema crea una solicitud de rectificación en estado PENDIENTE y notifica
   al segundo admin.

2. **Given** una rectificación PENDIENTE, **When** un segundo admin distinto llama a
   `PATCH /api/actas/rectificacion/{rectId}/aprobar`, **Then** la nota se actualiza,
   el acta queda en estado RECTIFICADA, y se registra en auditoría: ambos admins,
   motivo, nota original, nota rectificada, timestamps.

3. **Given** el mismo admin intentando aprobar su propia solicitud de rectificación,
   **When** llama a `PATCH .../aprobar`, **Then** el sistema retorna HTTP 403
   "No puede aprobar su propia solicitud de rectificación."

---

### User Story 4 — Alumno consulta su boletín de notas (Priority: P2)

Un alumno autenticado puede ver sus notas en todas las materias en que estuvo inscripto,
incluyendo el estado del acta (abierta/cerrada).

**Why this priority**: Es la razón de existir del sistema desde la perspectiva del
alumno. Sin acceso a sus notas, el alumno no puede verificar su situación académica.

**Independent Test**: `GET /api/alumnos/mias/notas` con JWT de alumno retorna todas
sus notas agrupadas por materia y período.

**Acceptance Scenarios**:

1. **Given** un alumno con notas en 3 materias, **When** llama a
   `GET /api/alumnos/mias/notas`, **Then** el sistema retorna sus notas agrupadas
   por materia con estado del acta y promedio calculado.

2. **Given** un alumno intentando ver notas de otro alumno via
   `GET /api/alumnos/{otroId}/notas`, **When** el `otroId` no coincide con el `sub`
   del JWT, **Then** el sistema retorna HTTP 403 (mínimo privilegio, Ley 25.326).

---

### Edge Cases

- ¿Qué pasa si el docente cierra un acta con alumnos sin nota cargada?
  → El sistema DEBE advertir (HTTP 422) si hay alumnos sin nota, solicitando
  confirmación explícita antes de cerrar.
- ¿Qué pasa si se intenta cerrar un acta ya cerrada?
  → HTTP 409 "El acta ya está cerrada."
- ¿Qué pasa si la BD cae durante el cierre de acta (transacción a medias)?
  → La transacción es atómica en Hibernate. El estado CERRADA solo se persiste si
  todo el UPDATE + INSERT de auditoría completa exitosamente.
- ¿Qué pasa con materias con escala 0–10 vs. 0–100?
  → La entidad `Materia` tiene `escalaMaxima` (default: 10). La validación de rango
  usa ese valor.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir a un docente cargar/actualizar notas
  (`PUT /api/actas/{actaId}/notas/{alumnoId}`) solo mientras el acta está ABIERTA.
- **FR-002**: El sistema DEBE registrar en la tabla de auditoría cada INSERT/UPDATE
  sobre notas con: `usuarioJwt`, `timestamp`, `ip`, `valorAnterior`, `valorNuevo`,
  `hashPrevio` (hash encadenado del registro anterior de auditoría).
- **FR-003**: `POST /api/actas/{id}/cerrar` DEBE cambiar el estado del acta a CERRADA
  de forma atómica (transacción que incluye el INSERT en auditoría).
- **FR-004**: Una vez CERRADA, ninguna operación de la API ni acceso directo por un
  docente puede modificar notas. Solo el proceso de rectificación (FR-006) aplica.
- **FR-005**: El sistema DEBE advertir (HTTP 422 + confirmación) si se intenta cerrar
  un acta con alumnos inscriptos sin nota cargada.
- **FR-006**: El sistema DEBE implementar el proceso de rectificación de doble
  aprobación de admin para notas en actas cerradas.
- **FR-007**: El sistema DEBE permitir al alumno consultar sus notas
  (`GET /api/alumnos/mias/notas`) con agrupación por materia y período.
- **FR-008**: Un docente DEBE poder ver la grilla de notas de sus materias
  (`GET /api/actas/{id}`) pero NO de materias ajenas.
- **FR-009**: Un admin DEBE poder consultar actas y notas de cualquier materia.
- **FR-010**: Las notas de alumnos de otras comisiones son inaccesibles para un docente
  (mínimo privilegio — Principio II y V).

### Contratos de API

| Método | Path | Rol JWT requerido | Body / Respuesta |
|---|---|---|---|
| `GET` | `/api/actas` | `docente` (sus actas), `admin` (todas) | → 200 `[ ActaDTO ]` |
| `GET` | `/api/actas/{id}` | `docente` (su materia), `admin` | → 200 `ActaDetalleDTO` con notas |
| `PUT` | `/api/actas/{actaId}/notas/{alumnoId}` | `docente` (su materia) | `{ nota, tipo }` → 200 / 409 si cerrada |
| `POST` | `/api/actas/{id}/cerrar` | `docente` (su materia) | → 200 / 422 si hay alumnos sin nota |
| `POST` | `/api/actas/{id}/rectificacion` | `admin` | `{ alumnoId, nuevaNota, motivo }` → 201 PENDIENTE |
| `PATCH` | `/api/actas/rectificacion/{rectId}/aprobar` | `admin` (distinto al solicitante) | → 200 APROBADA / 403 |
| `GET` | `/api/alumnos/mias/notas` | `alumno` | → 200 `[ NotaPorMateriaDTO ]` |

### Key Entities

- **Acta**: `id`, `materiaId`, `docenteId`, `periodo`, `estado` (ABIERTA/CERRADA/RECTIFICADA),
  `fechaCierre` (nullable), `usuarioCierreId`.
- **Nota**: `id`, `actaId`, `alumnoId`, `valor`, `tipo` (PARCIAL_1/PARCIAL_2/FINAL/RECUPERATORIO),
  `fechaCarga`, `docenteId`.
- **SolicitudRectificacion**: `id`, `actaId`, `alumnoId`, `notaOriginal`, `notaNueva`,
  `motivo`, `adminSolicitanteId`, `adminAprobadorId`, `estado` (PENDIENTE/APROBADA/RECHAZADA),
  `fechaSolicitud`, `fechaResolucion`.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 0 registros de notas modificados en actas CERRADAS sin pasar por el
  proceso de rectificación — verificado con tests de integración y revisión de auditoría.
- **SC-002**: El hash encadenado de auditoría es verificable: recalcular SHA-256 de
  cada registro con el hash del anterior produce la misma cadena sin gaps.
- **SC-003**: El endpoint `POST /api/actas/{id}/cerrar` completa en ≤ 3 segundos
  incluso con 500 alumnos inscriptos en la materia (carga del INSERT en auditoría).
- **SC-004**: El 100% de los PUT de notas sobre actas cerradas retorna HTTP 409
  (sin excepción), verificado en suite de regression tests.

---

## Assumptions

- El `GrillaNotas.tsx` del frontend ya existe; este spec define el contrato de API.
- El `Boletin.tsx` del frontend corresponde a `GET /api/alumnos/mias/notas`.
- La tabla de auditoría ya existe (definida en feature `004-auditoria`); este spec
  la consume para registrar cambios de notas.
- Un acta corresponde a UNA materia en UN período académico y es asignada a UN docente.
- Las notas válidas son valores decimales en el rango [0, escalaMaxima] con precisión
  de hasta 2 decimales.
- El proceso de rectificación con doble firma de admin se implementa de forma
  sincrónica en esta versión (sin notificaciones push).
