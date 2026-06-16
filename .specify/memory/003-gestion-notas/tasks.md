# Tasks: Gestión de Notas y Cierre de Actas

**Input**: `.specify/memory/003-gestion-notas/` (spec.md + plan.md + checklist.md)

**Prerequisites**: plan.md ✅ | spec.md ✅ (Clarified) | checklist.md ✅

**Dependencias**: Feature `002-autenticacion-jwt` (filtros JWT) y feature `004-auditoria` (`AuditoriaService`) deben estar disponibles.

**Tests**: Incluidos — la atomicidad nota+auditoría es crítica para el Principio III.

**Organization**: US1+US2 son P1 (inmutabilidad — Principio III núcleo); US3+US4 son P2 (admin reapertura y boletín).

## Format: `[ID] [P?] [Story] Description`

---

## Phase 1: Setup

- [ ] T001 [Foundation] Crear rama `003-gestion-notas` desde `main` en el repo TP3
- [ ] T002 [P] [Foundation] Verificar que la feature `004-auditoria` (tabla `auditoria` + `AuditoriaService`) está disponible — es prerequisito de las transacciones de cierre

---

## Phase 2: Foundational — Migración de columnas de notas (Bloqueante)

**Propósito**: Las columnas de notas y `nota_cerrada` deben existir en la tabla `inscripciones` antes de cualquier US.

⚠️ **CRÍTICO**: Ninguna US puede comenzar hasta que la migración esté aplicada.

- [ ] T003 [Foundation] Crear migración SQL: agregar columnas a `inscripciones`:
  ```sql
  ADD COLUMN nota_cerrada   BOOLEAN     NOT NULL DEFAULT FALSE,
  ADD COLUMN nota_parcial_1 DECIMAL(4,2) NULL,
  ADD COLUMN nota_parcial_2 DECIMAL(4,2) NULL,
  ADD COLUMN nota_final     DECIMAL(4,2) NULL,
  ADD COLUMN asistencias    TINYINT UNSIGNED NULL;
  ```
- [ ] T004 [Foundation] Agregar constraints MySQL 8 CHECK:
  ```sql
  ADD CONSTRAINT chk_asistencias CHECK (asistencias BETWEEN 0 AND 100),
  ADD CONSTRAINT chk_nota_p1     CHECK (nota_parcial_1 BETWEEN 0 AND 10),
  ADD CONSTRAINT chk_nota_p2     CHECK (nota_parcial_2 BETWEEN 0 AND 10),
  ADD CONSTRAINT chk_nota_final  CHECK (nota_final BETWEEN 0 AND 10);
  ```
- [ ] T005 [Foundation] Actualizar entidad `Inscripcion.java` con los nuevos campos mapeados (`notaCerrada`, `notaParcial1`, `notaParcial2`, `notaFinal`, `asistencias`)
- [ ] T006 [Foundation] Crear DTO `NotaRequest.java` `{ notaParcial1?, notaParcial2?, notaFinal?, asistencias? }` con validaciones `@Min(0)`, `@Max(10)`, `@DecimalMin`, `@DecimalMax` correspondientes
- [ ] T007 [Foundation] Actualizar `InscripcionRepository.java` con métodos: `findByMateriaIdAndDocenteId()`, `findByEstudianteId()` (para mis-notas)

**Checkpoint**: Migración aplicada. Entidad actualizada. Los repositorios tienen los métodos necesarios.

---

## Phase 3: US1 — Docente carga y actualiza notas (Priority: P1) 🎯 MVP

**Goal**: Docente titular de la materia puede cargar/actualizar notas mientras `notaCerrada = false`.

**Independent Test**: `PUT /api/docente/inscripciones/{id}/nota` con JWT DOCENTE de la materia + `{ notaFinal: 8.0 }` → HTTP 200 + nota persistida en DB + registro en auditoría.

### Tests para US1

- [ ] T008 [P] [US1] Test unitario `NotaService.cargarNota()` — `notaCerrada = false` → nota actualizada + auditoría insertada en misma transacción
- [ ] T009 [P] [US1] Test unitario — nota fuera de rango (< 0 o > 10) → HTTP 400 con detalle de campo
- [ ] T010 [P] [US1] Test unitario — `asistencias` fuera de rango (< 0 o > 100) → HTTP 400
- [ ] T011 [P] [US1] Test seguridad — docente de materia A intenta cargar nota en materia B → HTTP 403
- [ ] T012 [P] [US1] Test — rollback: si `AuditoriaService.registrar()` falla → la nota NO se persiste (atomicidad)

### Implementación de US1

- [ ] T013 [US1] Implementar `NotaService.cargarNota(Long idInscripcion, NotaRequest dto, Long idDocente, String ip)`:
  1. Cargar inscripción → HTTP 404 si no existe
  2. Verificar `notaCerrada == false` → HTTP 409 si cerrada
  3. Verificar que el docente es titular de la materia → HTTP 403
  4. Validar rangos de notas y asistencias
  5. `@Transactional`: actualizar campos de nota + `auditoriaService.registrar(NOTA_CARGADA, ...)` + `save()`
- [ ] T014 [US1] Implementar `DocenteNotaController.PUT /api/docente/inscripciones/{id}/nota` con `@PreAuthorize("hasRole('DOCENTE')")` + `@Valid @RequestBody NotaRequest`
- [ ] T015 [US1] Frontend — verificar que `docenteService.ts` usa el path correcto `PUT /api/docente/inscripciones/{id}/nota` ✅
- [ ] T016 [P] [US1] Frontend — `GrillaNotas.tsx` deshabilita visualmente el formulario cuando `notaCerrada = true` ✅ (verificar)

**Checkpoint**: Docente puede cargar notas. Validaciones funcionan. Auditoría registra cada cambio. Rollback ante fallo de auditoría.

---

## Phase 4: US2 — Docente cierra notas (inmutabilidad) (Priority: P1) 🎯 MVP

**Goal**: `PATCH .../cerrar` → `notaCerrada = true` atómicamente con INSERT en auditoría. POST-cierre: cualquier intento de `PUT .../nota` → HTTP 409.

**Independent Test**: Cerrar → intentar `PUT .../nota` → HTTP 409. Rollback si falla auditoría.

### Tests para US2

- [ ] T017 [P] [US2] Test unitario `NotaService.cerrarNota()` — `notaCerrada = false` → `notaCerrada = true` + auditoría `NOTA_CERRADA` en misma `@Transactional`
- [ ] T018 [P] [US2] Test integración — cerrar + intentar PUT → HTTP 409 (sin excepción, sin nota modificada en DB)
- [ ] T019 [P] [US2] Test de rollback — simular falla de `AuditoriaService` durante cierre → `notaCerrada` permanece `false` en DB
- [ ] T020 [P] [US2] Test seguridad — ESTUDIANTE intenta `PATCH .../cerrar` → HTTP 403

### Implementación de US2

- [ ] T021 [US2] Implementar `NotaService.cerrarNota(Long idInscripcion, Long idDocente, String ip)`:
  ```
  @Transactional
  1. findById → HTTP 404
  2. notaCerrada == true → HTTP 409 "Ya está cerrada"
  3. Verificar titularidad docente → HTTP 403
  4. inscripcion.setNotaCerrada(true)
  5. auditoriaService.registrar(NOTA_CERRADA, INSCRIPCION, idInscripcion, ...)
  6. inscripcionRepository.save(inscripcion)
  // Si paso 5 falla → rollback completo; notaCerrada NO se persiste
  ```
- [ ] T022 [US2] Implementar `DocenteNotaController.PATCH /api/docente/inscripciones/{id}/cerrar`
- [ ] T023 [US2] Agregar guard en `NotaService.cargarNota()`: verificar `notaCerrada == false` al inicio → HTTP 409 "Nota cerrada — usar proceso de rectificación"
- [ ] T024 [P] [US2] Frontend — `docenteService.ts` método `cerrarNota(id)` llama a `PATCH /api/docente/inscripciones/{id}/cerrar` ✅ (verificar path)

**Checkpoint**: La inmutabilidad funciona. Un PUT post-cierre siempre retorna 409. La transacción es atómica.

---

## Phase 5: US3 — Admin reabre nota cerrada (Priority: P2)

**Goal**: `PATCH /api/admin/inscripciones/{id}/reabrir` con `{ motivo }` → `notaCerrada = false` + auditoría `NOTA_REABIERTA` → HTTP 200.

**Independent Test**: Cerrar nota → reabrir con motivo → verificar `notaCerrada = false` en DB + registro `NOTA_REABIERTA` en auditoría.

### Tests para US3

- [ ] T025 [P] [US3] Test unitario `NotaService.reabrir()` — `notaCerrada = true` + motivo → `notaCerrada = false` + auditoría `NOTA_REABIERTA`
- [ ] T026 [P] [US3] Test — reabrir nota ya abierta (`notaCerrada = false`) → HTTP 409 "La nota ya está abierta"
- [ ] T027 [P] [US3] Test — `reabrir()` sin motivo o con motivo vacío → HTTP 400
- [ ] T028 [P] [US3] Test seguridad — JWT DOCENTE intenta reabrir → HTTP 403; JWT ESTUDIANTE → HTTP 403
- [ ] T029 [P] [US3] Test de rollback — falla de auditoría durante reapertura → `notaCerrada` permanece `true`

### Implementación de US3

- [ ] T030 [US3] Implementar `NotaService.reabrir(Long idInscripcion, String motivo, Long idAdmin, String ip)`:
  ```
  @Transactional
  1. findById → HTTP 404
  2. notaCerrada == false → HTTP 409 "Ya está abierta"
  3. motivo blank → HTTP 400 "Motivo obligatorio"
  4. inscripcion.setNotaCerrada(false)
  5. auditoriaService.registrar(NOTA_REABIERTA, INSCRIPCION, idInscripcion, motivo, ...)
  6. inscripcionRepository.save(inscripcion)
  ```
- [ ] T031 [US3] Implementar `AdminNotaController.PATCH /api/admin/inscripciones/{id}/reabrir` con `@PreAuthorize("hasRole('ADMINISTRADOR')")`
- [ ] T032 [US3] Crear `ReabrirRequest.java` `{ @NotBlank String motivo }`
- [ ] T033 [US3] Frontend — `adminService.reabrir(id, motivo)` llama a `PATCH /api/admin/inscripciones/{id}/reabrir` ✅ (ya hecho en gap fix)

**Checkpoint**: Admin puede reabrir notas cerradas. Evento registrado en auditoría. Rollback ante fallo.

---

## Phase 6: US4 — Alumno consulta su boletín de notas (Priority: P2)

**Goal**: `GET /api/inscripciones/mis-notas` con JWT ESTUDIANTE → notas propias. Otro alumno no puede ver las notas ajenas.

**Independent Test**: `GET /api/inscripciones/mis-notas` → solo retorna inscripciones del `sub` del JWT. Con JWT de otro alumno → no aparecen estas notas.

### Tests para US4

- [ ] T034 [P] [US4] Test integración — `GET /mis-notas` retorna solo las notas del alumno autenticado (no las de otros)
- [ ] T035 [P] [US4] Test seguridad — alumno A no puede ver notas de alumno B (HTTP 403 si intenta por path alternativo)

### Implementación de US4

- [ ] T036 [US4] Implementar `EstudianteNotaController.GET /api/inscripciones/mis-notas` con `@PreAuthorize("hasRole('ESTUDIANTE')")` → `findByEstudianteId(sub)`
- [ ] T037 [P] [US4] Frontend — `Boletin.tsx` consume `GET /api/inscripciones/mis-notas` ✅ (verificar path correcto)
- [ ] T038 [P] [US4] Frontend — `Boletin.tsx` protegido con `ProtectedRoute` solo para rol `ESTUDIANTE` ✅

**Checkpoint**: El boletín funciona. El alumno ve solo sus propias notas.

---

## Phase 7: Polish y Cross-Cutting

- [ ] T039 [P] Implementar `GET /api/docente/materias/{id}/inscripciones` para que docente vea grilla de notas de su materia
- [ ] T040 [P] Implementar `GET /api/docente/estudiantes/{idEstudiante}/asistencias` para historial del docente
- [ ] T041 [P] Agregar validación HTTP 422 + confirmación explícita si se intenta cerrar con alumnos sin nota cargada (edge case del spec)
- [ ] T042 [P] Agregar `GlobalExceptionHandler` para HTTP 409 `NotaCerradaException` con mensaje claro
- [ ] T043 Test integración — simular falla en INSERT de auditoría durante `PATCH .../cerrar` → rollback completo; `notaCerrada` permanece en `false` — SC-001 del spec
- [ ] T044 Test de regresión — 100 `PUT .../nota` en inscripciones cerradas → 0 updates exitosos, 100 HTTP 409 — SC-004 del spec
