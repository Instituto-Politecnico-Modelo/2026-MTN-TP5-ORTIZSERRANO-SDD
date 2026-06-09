# Feature Specification: Inscripción a Materias con Cola de Espera

**Feature Branch**: `001-inscripcion-materias`

**Created**: 2026-06-09

**Status**: Clarified

**Input**: User description: "Inscripción a materias con cola de espera bajo alta concurrencia"

## Clarification Notes *(added 2026-06-09 — reconciliado con código existente)*

> Las siguientes discrepancias entre el spec Draft y el código del frontend ya
> implementado fueron resueltas en esta pasada de clarificación:
>
> | # | Ítem | Spec Draft | Clarificado |
> |---|---|---|---|
> | C1 | **Nombre de rol** | `alumno` | `ESTUDIANTE` (enum `Role.ESTUDIANTE`) |
> | C2 | **Nombre de rol** | `docente` | `DOCENTE` (enum `Role.DOCENTE`) |
> | C3 | **Nombre de rol** | `admin` | `ADMINISTRADOR` (enum `Role.ADMINISTRADOR`) |
> | C4 | **Body field POST inscripcion** | `{ "materiaId": Long }` | `{ "idMateria": number }` |
> | C5 | **Mis inscripciones path** | `GET /api/inscripciones/mias` | `GET /api/inscripciones/mis-inscripciones` |
> | C6 | **Cancelar inscripcion** | `DELETE /api/inscripciones/{id}` | `PATCH /api/inscripciones/{id}/cancelar` |
> | C7 | **Lista inscriptos docente** | `GET /api/materias/{id}/inscriptos` | `GET /api/docente/materias/{id}/inscripciones` |
> | C8 | **SalaDeEspera** | Muestra posición en cola numérica | Se activa ante HTTP 429 (rate limit) o 503 (servidor saturado); polling a `GET /api/health`; muestra countdown y permite reintentar |
> | C9 | **DELETE cola** | `DELETE /api/inscripciones/cola/{posicion}` | **Sin equivalente expuesto** — cancelación de cola se hace via `PATCH /api/inscripciones/{id}/cancelar` cambiando estado a CANCELADO |
> | C10 | **Mis notas (adicional)** | No estaba en spec | `GET /api/inscripciones/mis-notas` ya existe en frontend |
> | C11 | **Materia: campo carrera** | `carrera` obligatorio | `carrera?: string \| null` — null = transversal a todas las carreras |
> | C12 | **Estado inscripcion** | CONFIRMADO / ENCOLADO / CANCELADO | Alineado; `ENCOLADO` = lista de espera (HTTP 202 al inscribirse) |

## Constitution Check

| Principio | Aplica | Notas |
|---|---|---|
| I. Alta Concurrencia | ✅ NÚCLEO | Cola Redis + optimistic locking obligatorio |
| II. JWT Auth | ✅ | Todos los endpoints de inscripción requieren JWT con rol `alumno` |
| III. Inmutabilidad/Auditoría | ✅ Parcial | Inscripciones confirmadas se registran en auditoría |
| IV. Stack Definido | ✅ | React 18 + Spring Boot 3 + Redis + MySQL 8 |
| V. Privacidad Ley 25.326 | ✅ | Datos de inscripción son exclusivos del alumno titular |
| VI. Disponibilidad | ✅ | 99,9% uptime durante período de inscripción activo |

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Alumno se inscribe exitosamente a una materia con cupo disponible (Priority: P1)

Un alumno autenticado selecciona una materia disponible, confirma la inscripción y
recibe confirmación inmediata cuando hay cupo libre.

**Why this priority**: Es el flujo principal del sistema y debe funcionar correctamente
antes que cualquier otro escenario. Sin este flujo, el sistema no tiene valor funcional.

**Independent Test**: Se puede probar de forma aislada haciendo un POST
a `POST /api/inscripciones` con un JWT de alumno válido y verificando que retorna HTTP
201 con los datos de la inscripción y que los cupos disponibles se decrementan en 1 en
la base de datos.

**Acceptance Scenarios**:

1. **Given** un alumno autenticado con JWT válido (rol `alumno`) y una materia con
   cupos disponibles > 0, **When** el alumno realiza `POST /api/inscripciones` con el
   `id` de la materia, **Then** el sistema responde HTTP 201, registra la inscripción
   en la base de datos, decrementa cupos disponibles en 1, y retorna los datos de la
   inscripción (materia, horario, cuatrimestre).

2. **Given** dos alumnos enviando exactamente la misma solicitud simultáneamente al
   último cupo disponible, **When** ambos hacen `POST /api/inscripciones` en el mismo
   instante, **Then** exactamente uno recibe HTTP 201 (inscripción exitosa) y el otro
   recibe HTTP 409 o es encolado — **nunca** dos inscripciones aprobadas cuando había
   un solo cupo (optimistic locking garantiza esto).

3. **Given** un alumno ya inscripto en una materia, **When** intenta inscribirse
   nuevamente, **Then** el sistema responde HTTP 409 con mensaje "Ya se encuentra
   inscripto en esta materia".

---

### User Story 2 — Alumno es puesto en cola cuando no hay cupos disponibles (Priority: P1)

Cuando todos los cupos de una materia están ocupados, el alumno debe ser encolado y
recibir su posición en la cola y un tiempo estimado de espera, **nunca** un HTTP 500.

**Why this priority**: Este es el corazón del Principio I de la constitución. El sistema
anterior fallaba aquí. La cola es el diferenciador técnico crítico.

**Independent Test**: Se puede probar llenando todos los cupos de una materia de prueba
y luego enviando una solicitud de inscripción. El sistema debe retornar HTTP 202 con
`position` y `estimated_wait_seconds` en el body.

**Acceptance Scenarios**:

1. **Given** una materia con cupos disponibles = 0, **When** un alumno envía
   `POST /api/inscripciones`, **Then** el sistema retorna HTTP 202 con body:
   `{ "status": "ENCOLADO", "position": N, "estimated_wait_seconds": T }`.

2. **Given** un alumno en posición 3 de la cola, **When** los dos alumnos delante de
   él cancelan su inscripción o liberan su cupo, **Then** el alumno avanza a posición 1
   y el `SalaDeEspera` frontend se actualiza vía polling o SSE con la nueva posición.

3. **Given** el sistema bajo carga de 50.000 usuarios simultáneos intentando inscribirse
   en la misma materia, **When** los cupos se agotan, **Then** todos los requests
   pendientes son encolados (no se produce HTTP 500, no se pierde ningún request, no
   se corrompen los cupos).

4. **Given** un alumno en cola que ya no desea esperar, **When** llama a
   `DELETE /api/inscripciones/cola/{posicion}`, **Then** es removido de la cola y el
   siguiente alumno avanza de posición.

---

### User Story 3 — Alumno visualiza y gestiona sus inscripciones actuales (Priority: P2)

Un alumno puede consultar la lista completa de sus inscripciones activas, materias en
las que está en cola, y cancelar inscripciones antes del cierre del período.

**Why this priority**: Sin poder ver sus inscripciones, el alumno no puede confirmar
que el sistema procesó su request correctamente.

**Independent Test**: `GET /api/inscripciones/mias` con JWT de alumno retorna la lista
completa de inscripciones y estados (CONFIRMADO / ENCOLADO / CANCELADO).

**Acceptance Scenarios**:

1. **Given** un alumno con 3 inscripciones (2 confirmadas, 1 encolada), **When**
   llama a `GET /api/inscripciones/mias`, **Then** el sistema retorna las 3
   inscripciones con sus estados y datos de materia (nombre, horario, cuatrimestre).

2. **Given** un alumno con una inscripción CONFIRMADA dentro del período de
   inscripción activo, **When** llama a `DELETE /api/inscripciones/{id}`, **Then** la
   inscripción cambia a estado CANCELADO, el cupo se libera y el primer alumno en la
   cola de esa materia es promovido automáticamente a CONFIRMADO.

3. **Given** el período de inscripción está cerrado, **When** un alumno intenta
   cancelar una inscripción, **Then** el sistema responde HTTP 403 con mensaje
   "El período de inscripción está cerrado".

---

### User Story 4 — Docente / Admin visualiza los inscriptos por materia (Priority: P2)

Un docente o administrador puede consultar la lista de alumnos inscriptos en una
materia específica, incluyendo la cola de espera.

**Why this priority**: Necesario para que el docente pueda gestionar su comisión y
para que admin pueda verificar el estado del sistema.

**Independent Test**: `GET /api/materias/{id}/inscriptos` con JWT de docente o admin
retorna la lista paginada de alumnos inscriptos y la cola de espera.

**Acceptance Scenarios**:

1. **Given** un docente autenticado con JWT (rol `docente`) y una materia de su cargo,
   **When** llama a `GET /api/materias/{id}/inscriptos`, **Then** recibe la lista de
   alumnos confirmados y, separado, la lista de la cola de espera.

2. **Given** un alumno (rol `alumno`) intentando acceder a inscriptos de otra materia,
   **When** llama a `GET /api/materias/{id}/inscriptos`, **Then** el sistema responde
   HTTP 403 (principio de mínimo privilegio).

---

### Edge Cases

- ¿Qué pasa si Redis cae durante un período activo de inscripción?
  → El sistema DEBE degradar graciosamente: procesar inscripciones sincrónicamente
  con mayor latencia, no retornar HTTP 500.
- ¿Qué pasa si se envía un `idMateria` inexistente?
  → HTTP 404 con mensaje descriptivo.
- ¿Qué pasa si el JWT del ESTUDIANTE está expirado?
  → HTTP 401; el `axiosInstance` intercepta y redirige automáticamente a
  `/login?motivo=expired`, limpiando el localStorage.
- ¿Qué pasa si el servidor está bajo presión máxima (sin cupo de threads)?
  → HTTP 503 o HTTP 429; el `axiosInstance` intercepta y redirige a
  `/sala-de-espera?razon=capacidad&espera=N&origen=/inscripciones`. La
  `SalaDeEspera` hace polling a `GET /api/health` cada 5 segundos hasta
  que el servidor responde OK.
- ¿Qué pasa si el período de inscripción no ha comenzado todavía?
  → HTTP 403 con mensaje "El período de inscripción no está activo".
- ¿Qué pasa con valores negativos de cupos (bug)?
  → Constraint `cupos_disponibles >= 0` en MySQL impide esto; la lógica
  de negocio valida antes de decrementar.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir a un alumno autenticado (JWT, rol `alumno`)
  inscribirse a una materia disponible mediante `POST /api/inscripciones`.
- **FR-002**: El sistema DEBE implementar una cola (Redis) que serialice el acceso a
  cupos cuando múltiples alumnos intentan inscribirse simultáneamente.
- **FR-003**: El sistema DEBE usar optimistic locking (Hibernate `@Version`) en la
  entidad `Materia` para prevenir condiciones de carrera sobre el campo
  `cupos_disponibles`.
- **FR-004**: Cuando los cupos están agotados, el sistema DEBE retornar HTTP 202 con
  posición en cola y tiempo estimado de espera (nunca HTTP 500 bajo carga).
- **FR-005**: El sistema DEBE permitir al alumno consultar sus inscripciones activas
  y estados mediante `GET /api/inscripciones/mias`.
- **FR-006**: El sistema DEBE permitir al alumno cancelar una inscripción CONFIRMADA
  dentro del período activo, liberando el cupo al siguiente en cola.
- **FR-007**: El sistema DEBE promover automáticamente al primer alumno de la cola
  cuando un cupo queda disponible (por cancelación o ampliación de cupos).
- **FR-008**: El sistema DEBE rechazar (HTTP 409) una inscripción duplicada del mismo
  alumno en la misma materia.
- **FR-009**: El sistema DEBE rechazar (HTTP 403) cualquier operación de inscripción
  fuera del período activo.
- **FR-010**: Un docente (JWT, rol `docente`) DEBE poder consultar los inscriptos de
  sus materias mediante `GET /api/materias/{id}/inscriptos`.
- **FR-011**: Un admin (JWT, rol `admin`) DEBE poder consultar inscriptos de cualquier
  materia.
- **FR-012**: El sistema DEBE registrar en la tabla de auditoría cada cambio de estado
  de inscripción (CONFIRMADO, ENCOLADO, CANCELADO) con usuario JWT, timestamp e IP.
- **FR-013**: El sistema DEBE soportar 50.000 usuarios simultáneos durante períodos
  de inscripción sin degradación ni corrupción de datos (Principio I).

### Contratos de API *(corregidos en clarification)*

| Método | Path | Rol JWT requerido | Body / Respuesta |
|---|---|---|---|
| `POST` | `/api/inscripciones` | `ESTUDIANTE` | body: `{ "idMateria": number }` → 201 (CONFIRMADO) / 202 (ENCOLADO) / 403 / 404 / 409 |
| `GET` | `/api/inscripciones/mis-inscripciones` | `ESTUDIANTE` | → 200 `[ Inscripcion ]` |
| `GET` | `/api/inscripciones/mis-notas` | `ESTUDIANTE` | → 200 `[ Inscripcion ]` (con notaParcial1, notaParcial2, notaFinal) |
| `PATCH` | `/api/inscripciones/{id}/cancelar` | `ESTUDIANTE` | → 200 `Inscripcion` con estado CANCELADO / 403 / 404 |
| `GET` | `/api/inscripciones/{id}/estado` | `ESTUDIANTE` | → 200 `{ status, position?, estimated_wait_seconds? }` |
| `GET` | `/api/docente/materias/{id}/inscripciones` | `DOCENTE`, `ADMINISTRADOR` | → 200 `[ Inscripcion ]` |
| `GET` | `/api/materias` | Público / cualquier rol | → 200 `[ MateriaDisponible ]` |
| `GET` | `/api/materias/disponibles` | Público / cualquier rol | → 200 `[ MateriaDisponible ]` (hayLugar=true) |
| `GET` | `/api/health` | Público | → 200 (usado por SalaDeEspera para polling) |

### Key Entities *(corregidos en clarification)*

- **Inscripcion** (TypeScript: `materia.service.ts`): `idInscripcion`, `estudiante?`
  (`EstudianteResumen`), `materia` (`MateriaDisponible`), `fechaInscripcion: string`,
  `estado: string` (CONFIRMADO / ENCOLADO / CANCELADO), `notaParcial1?`, `notaParcial2?`,
  `notaFinal?`, `asistencias?`, `notaCerrada: boolean`.
- **MateriaDisponible**: `idMateria`, `codigo`, `nombre`, `descripcion?`, `anio?`,
  `carrera?: string | null` (null = transversal), `docenteNombre?`, `docenteApellido?`,
  `docenteEmail?`, `cuposMaximos?`, `cuposOcupados`, `cuposDisponibles?`, `hayLugar`,
  `porcentajeOcupacion?`.
- **InscripcionRequest**: `{ idMateria: number }`.
- **InscripcionResult**: `{ status: number, data: any }` — status 201 = CONFIRMADO,
  202 = ENCOLADO (lista de espera).
- **ColaEspera** (Redis): Lista ordenada de `alumnoId` por `materiaId`. Efímera —
  fuente de verdad de posiciones de espera. Estado persistente = campo `estado` en MySQL.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El sistema procesa inscripciones concurrentes de 50.000 usuarios
  simultáneos sin retornar ningún HTTP 500 — medido en prueba de carga con k6 o JMeter
  y resultados adjuntos como artefacto de release.
- **SC-002**: Tasa de error de concurrencia (doble-inscripción / cupos negativos) = 0%
  en prueba de carga con 10.000 requests simultáneos al último cupo de una materia.
- **SC-003**: Latencia P99 del endpoint `POST /api/inscripciones` ≤ 2000ms bajo carga
  de 50.000 usuarios concurrentes.
- **SC-004**: Un alumno en cola puede consultar su posición y ETA en menos de 500ms
  (`GET /api/inscripciones/{id}/estado`).
- **SC-005**: La promoción automática de cola a cupo confirmado ocurre en ≤ 5 segundos
  después de que se libera un cupo.

---

## Assumptions

- El período de inscripción activo es controlado por un administrador vía
  `PATCH /api/periodos/{id}/activar` (feature separada).
- Redis está disponible como dependencia del entorno de deployment; en entornos de
  desarrollo, se permite un fallback a un mecanismo en memoria (no para producción).
- La autenticación JWT ya está implementada (feature `002-autenticacion-jwt`); este
  spec asume que los filtros de Spring Security ya validan tokens entrantes.
- El frontend `SalaDeEspera.tsx` ya existe — este spec define el contrato de API que
  ese componente consume.
- La notificación de avance en cola se implementa via polling al endpoint
  `GET /api/inscripciones/{id}/estado` (SSE / WebSocket queda como mejora futura, fuera
  de este spec).
- Los tests de carga son OBLIGATORIOS antes de habilitar este endpoint en producción
  (Principio I de la constitución).
