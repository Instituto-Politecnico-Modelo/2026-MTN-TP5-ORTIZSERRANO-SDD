# Tasks: Inscripción a Materias con Cola de Espera

**Input**: `.specify/memory/001-inscripcion-materias/` (spec.md + plan.md + checklist.md)

**Prerequisites**: plan.md ✅ | spec.md ✅ (Clarified) | checklist.md ✅

**Tests**: Incluidos — requeridos por Principio I (pruebas de carga obligatorias antes de producción).

**Organization**: Tareas agrupadas por user story. US1 + US2 son P1 (núcleo de la feature); US3 + US4 son P2 (complementario).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo con otras tareas del mismo grupo
- **[Story]**: US1–US4 o Foundation
- Paths son relativos al repositorio TP3

---

## Phase 1: Setup

**Propósito**: Estructura base para la feature de inscripciones.

- [ ] T001 [Foundation] Crear rama `001-inscripcion-materias` desde `main` en el repo TP3
- [ ] T002 [P] [Foundation] Agregar dependencias en `pom.xml`: `spring-boot-starter-data-redis`, `jedis` (o `lettuce-core`)
- [ ] T003 [P] [Foundation] Configurar propiedades Redis en `application.properties` / `application-prod.properties` (`spring.redis.host`, `spring.redis.port`, timeout)
- [ ] T004 [P] [Foundation] Crear `RedisConfig.java` con `RedisTemplate<String, Long>` y `StringRedisTemplate` beans

---

## Phase 2: Foundational — Entidades y migración (Bloqueante)

**Propósito**: Schema y entidades JPA que todas las user stories necesitan.

⚠️ **CRÍTICO**: Ninguna US puede comenzar hasta que esta fase esté completa.

- [ ] T005 [Foundation] Agregar columna `version BIGINT DEFAULT 0` a tabla `materias` (migración SQL) — optimistic locking obligatorio (Principio I)
- [ ] T006 [Foundation] Agregar columna `UNIQUE KEY uq_estudiante_materia (id_estudiante, id_materia)` a tabla `inscripciones` (migración SQL)
- [ ] T007 [P] [Foundation] Crear/actualizar entidad `Materia.java` con `@Version private Long version` para optimistic locking
- [ ] T008 [P] [Foundation] Crear/actualizar entidad `Inscripcion.java` con estados `ENUM('CONFIRMADO','ENCOLADO','CANCELADO')`, campos de notas y `notaCerrada boolean`
- [ ] T009 [Foundation] Crear `InscripcionRepository.java` con métodos: `findByEstudianteIdAndMateriaId()`, `findByEstudianteId()`, `findByMateriaId()`
- [ ] T010 [Foundation] Crear `MateriaRepository.java` con `findByActivoTrue()` y lock pesimista para actualizar cupos
- [ ] T011 [Foundation] Crear `ColaRedisService.java`: métodos `encolar(idMateria, idInscripcion)`, `desencolar(idMateria)`, `posicion(idMateria, idInscripcion)`, `remover(idMateria, idInscripcion)`, `tamaño(idMateria)` + fallback si Redis no disponible

**Checkpoint**: Schema y repositorios listos — implementación de US puede comenzar en paralelo.

---

## Phase 3: US1 — Inscripción exitosa con cupo disponible (Priority: P1) 🎯 MVP

**Goal**: Un alumno autenticado puede inscribirse a una materia con cupo → HTTP 201.

**Independent Test**: `POST /api/inscripciones` con JWT ESTUDIANTE + materia con cupo disponible → HTTP 201 + cupos decrementados en 1.

### Tests para US1

- [ ] T012 [P] [US1] Test unitario `InscripcionService.inscribir()` — cupo disponible → HTTP 201 + optimistic lock no falla
- [ ] T013 [P] [US1] Test integración concurrencia — 2 alumnos al mismo tiempo al último cupo → exactamente 1 HTTP 201 y 1 HTTP 202 (no dos 201)
- [ ] T014 [P] [US1] Test integración — inscripción duplicada del mismo alumno → HTTP 409

### Implementación de US1

- [ ] T015 [US1] Implementar `InscripcionService.inscribir(Long idMateria, Long idEstudiante, String ip)`: validar período activo → verificar duplicado → `@Transactional` decrementar cupo + crear Inscripcion(CONFIRMADO) + registrar auditoría
- [ ] T016 [US1] Implementar `InscripcionController.POST /api/inscripciones` con `@PreAuthorize("hasRole('ESTUDIANTE')")` y body `{ idMateria }`; retornar 201 `InscripcionResponse`
- [ ] T017 [US1] Crear DTO `InscripcionRequest.java` `{ Long idMateria }` con `@NotNull`
- [ ] T018 [US1] Crear DTO `InscripcionResponse.java` mapeado desde `Inscripcion` (incluye datos de materia, estado, posición si ENCOLADO)
- [ ] T019 [US1] Validar que `idMateria` existe y período está activo → HTTP 404 / HTTP 403 según corresponda

**Checkpoint**: Un alumno puede inscribirse exitosamente con cupo disponible. Verificado con T012–T014.

---

## Phase 4: US2 — Cola de espera cuando no hay cupos (Priority: P1) 🎯 MVP

**Goal**: Sin cupos disponibles → HTTP 202 ENCOLADO + posición + ETA. Nunca HTTP 500.

**Independent Test**: Llenar todos los cupos → `POST /api/inscripciones` → HTTP 202 `{ status: "ENCOLADO", position: N, estimated_wait_seconds: T }`.

### Tests para US2

- [ ] T020 [P] [US2] Test unitario `InscripcionService.inscribir()` — cupos = 0 → HTTP 202 + estado ENCOLADO + Redis encolar
- [ ] T021 [P] [US2] Test unitario `ColaRedisService` — enqueue/dequeue/position/remove con mocks Redis
- [ ] T022 [P] [US2] Test integración — Redis caído → fallback sincrono, no HTTP 500 (degradación graciosa)
- [ ] T023 [P] [US2] Test carga k6 — 50.000 usuarios simultáneos a materia con cupo 0 → 0 HTTP 500, todos ENCOLADO o error controlado

### Implementación de US2

- [ ] T024 [US2] Implementar rama "sin cupos" en `InscripcionService.inscribir()`: crear Inscripcion(ENCOLADO) + `colaRedisService.encolar(idMateria, idInscripcion)` + retornar posición y ETA estimado
- [ ] T025 [US2] Implementar `GET /api/inscripciones/{id}/estado`: retornar `{ estado, position?, estimated_wait_seconds? }` — polling del frontend SalaDeEspera
- [ ] T026 [US2] Implementar promoción automática al liberar cupo: cuando se cancela una CONFIRMADA → `colaRedisService.desencolar(idMateria)` → promover primer ENCOLADO a CONFIRMADO dentro de `@Transactional`
- [ ] T027 [US2] Implementar `DELETE /api/inscripciones/{id}/cola`: validar estado ENCOLADO → `colaRedisService.remover()` → eliminar Inscripcion → HTTP 204; retornar HTTP 409 si no está ENCOLADA
- [ ] T028 [US2] Implementar fallback: si `ColaRedisService` lanza excepción por Redis no disponible → procesar sincrónicamente sin cola, loguear alerta (no HTTP 500)

**Checkpoint**: El sistema maneja cupos agotados sin errores. La cola funciona. Prueba de carga T023 debe pasar.

---

## Phase 5: US3 — Alumno visualiza y gestiona sus inscripciones (Priority: P2)

**Goal**: El alumno puede ver sus inscripciones y cancelar las CONFIRMADAS.

**Independent Test**: `GET /api/inscripciones/mis-inscripciones` + JWT ESTUDIANTE → lista correcta. `PATCH .../cancelar` sobre CONFIRMADA → HTTP 200 + cupo liberado.

### Tests para US3

- [ ] T029 [P] [US3] Test integración `GET /mis-inscripciones` — retorna solo las inscripciones del alumno del JWT (no las de otros alumnos)
- [ ] T030 [P] [US3] Test integración `PATCH /cancelar` — período activo → CONFIRMADO → CANCELADO + cupo liberado + promoción de cola
- [ ] T031 [P] [US3] Test seguridad — alumno intenta cancelar inscripción de otro alumno → HTTP 403

### Implementación de US3

- [ ] T032 [US3] Implementar `GET /api/inscripciones/mis-inscripciones` con `@PreAuthorize` + extracción de `sub` del JWT → `findByEstudianteId(sub)`
- [ ] T033 [US3] Implementar `GET /api/inscripciones/mis-notas` → misma lógica que mis-inscripciones incluyendo campos de notas
- [ ] T034 [US3] Implementar `PATCH /api/inscripciones/{id}/cancelar`: validar que `idEstudiante` del JWT coincide con la inscripción → validar período activo → cambiar a CANCELADO → liberar cupo → trigger promoción de cola
- [ ] T035 [P] [US3] Frontend — actualizar filtro select en `MisInscripciones.tsx` para incluir estado ENCOLADO ✅ (ya hecho en gap fix)
- [ ] T036 [P] [US3] Frontend — verificar que `MisInscripciones.tsx` muestra dos botones según estado ✅ (ya hecho en gap fix)

**Checkpoint**: El alumno puede ver y gestionar sus inscripciones.

---

## Phase 6: US4 — Docente/Admin visualiza inscriptos por materia (Priority: P2)

**Goal**: Docente ve inscriptos de sus materias. Admin ve cualquier materia.

**Independent Test**: `GET /api/docente/materias/{id}/inscripciones` con JWT DOCENTE (su materia) → HTTP 200 lista. Con JWT ESTUDIANTE → HTTP 403.

### Tests para US4

- [ ] T037 [P] [US4] Test integración — docente accede a inscriptos de su materia → HTTP 200
- [ ] T038 [P] [US4] Test seguridad — docente accede a materia que no le pertenece → HTTP 403
- [ ] T039 [P] [US4] Test seguridad — ESTUDIANTE accede a inscriptos → HTTP 403

### Implementación de US4

- [ ] T040 [US4] Implementar `GET /api/docente/materias/{id}/inscripciones` con validación de propiedad (docente titular de la materia); ADMINISTRADOR bypasea la validación
- [ ] T041 [US4] Agregar relación `Materia ↔ Docente` en el modelo si no existe; implementar query `findByMateriaIdAndDocenteId()` para validar propiedad

**Checkpoint**: Docente y admin pueden consultar inscriptos.

---

## Phase 7: Polish y Cross-Cutting

**Propósito**: Errores, auditoría, performance y documentación.

- [ ] T042 [P] Implementar `GlobalExceptionHandler` para `OptimisticLockException` → HTTP 409 "Cupo ocupado, reintentando..." (no 500)
- [ ] T043 [P] Implementar `GlobalExceptionHandler` para `EntityNotFoundException` → HTTP 404 con mensaje descriptivo
- [ ] T044 [P] Registrar en tabla `auditoria` cada cambio de estado de inscripción (CONFIRMADO, ENCOLADO, CANCELADO) con JWT, timestamp e IP — llamar a `AuditoriaService.registrar()` dentro de la misma `@Transactional`
- [ ] T045 [P] Agregar `GET /api/health` endpoint público que retorna HTTP 200 `{ status: "UP" }` — usado por `SalaDeEspera.tsx` para polling
- [ ] T046 Prueba de carga k6/JMeter: 50.000 usuarios simultáneos, `POST /api/inscripciones` — documentar resultados como artefacto; debe pasar SC-001, SC-002, SC-003 del spec
- [ ] T047 [P] Documentar degradación Redis en `README` del proyecto: qué ocurre si Redis cae, cómo se detecta, SLA resultante
