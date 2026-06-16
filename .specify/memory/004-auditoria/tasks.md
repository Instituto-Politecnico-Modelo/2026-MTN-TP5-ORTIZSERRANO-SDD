# Tasks: Log de Auditoría con Hash Encadenado

**Input**: `.specify/memory/004-auditoria/` (spec.md + plan.md + checklist.md)

**Prerequisites**: plan.md ✅ | spec.md ✅ (Clarified) | checklist.md ✅

**Dependencias**: Esta feature es **prerequisito** de `003-gestion-notas` y `002-autenticacion-jwt` — `AuditoriaService.registrar()` debe existir antes de que esas features puedan insertar registros. Implementar fundacional primero.

**Tests**: Incluidos — la cadena de hashes y la atomicidad son críticas para el Principio III.

**Organization**: US1+US2 son P1 (registro automático y consulta admin — núcleo del Principio III); US3+US4 son P2 (verificación de integridad y frontend).

## Format: `[ID] [P?] [Story] Description`

---

## Phase 1: Setup

- [ ] T001 [Foundation] Crear rama `004-auditoria` desde `main` en el repo TP3
- [ ] T002 [P] [Foundation] Verificar que no existen tablas `auditoria_notas` ni `auditoria_eventos` — si existen, planificar migración a tabla unificada

---

## Phase 2: Foundational — Tabla, entidad y HashChainService (Bloqueante)

**Propósito**: La tabla `auditoria` y el `AuditoriaService` son consumidos por todas las demás features. Deben estar listos primero.

⚠️ **CRÍTICO**: Las features 002, 003 y la US1 de esta feature dependen de este servicio.

- [ ] T003 [Foundation] Crear migración SQL tabla `auditoria`:
  ```sql
  CREATE TABLE auditoria (
    id_registro      BIGINT AUTO_INCREMENT PRIMARY KEY,
    entidad          VARCHAR(50)  NOT NULL,
    id_entidad       BIGINT       NULL,
    accion           VARCHAR(50)  NOT NULL,
    id_usuario       BIGINT       NULL,
    email_usuario    VARCHAR(255) NULL,
    descripcion      TEXT         NULL,
    ip_origen        VARCHAR(45)  NULL,
    timestamp_evento DATETIME(3)  NOT NULL,
    hash_anterior    CHAR(64)     NOT NULL,
    hash_actual      CHAR(64)     NOT NULL
  ) ENGINE=InnoDB;
  ```
  Sin FK constraints — append-only.
- [ ] T004 [Foundation] Agregar 4 índices obligatorios:
  ```sql
  CREATE INDEX idx_audit_timestamp  ON auditoria (timestamp_evento);
  CREATE INDEX idx_audit_entidad_id ON auditoria (entidad, id_entidad);
  CREATE INDEX idx_audit_usuario    ON auditoria (id_usuario);
  CREATE INDEX idx_audit_accion     ON auditoria (accion);
  ```
- [ ] T005 [Foundation] Crear entidad `RegistroAuditoria.java` con todos los campos mapeados; `@Column(updatable = false)` en todos los campos (append-only — nunca se actualiza)
- [ ] T006 [Foundation] Crear `AuditoriaRepository.java` con:
  - `findTopByOrderByIdRegistroDesc()` — para obtener último hash
  - `findAll(Pageable)` — para listado paginado
  - `findByEntidad(String entidad, Pageable)`, `findByEntidadAndIdEntidad()`, `findByIdUsuario()`, `findByAccion()`
- [ ] T007 [Foundation] Implementar `HashChainService.java`:
  - `GENESIS_HASH = sha256("GENESIS-AUDITORIA-INSTITUCIONAL")` — constante inmutable
  - `String sha256(String input)` — `MessageDigest.getInstance("SHA-256")`, output hex lowercase
  - `String calcularHash(RegistroAuditoria r, String hashAnterior)` — concatenar campos + sha256
  - `String getUltimoHash()` — `findTopByOrderByIdRegistroDesc()` → `hashActual` o `GENESIS_HASH`
- [ ] T008 [Foundation] Implementar `AuditoriaService.registrar(String entidad, Long idEntidad, String accion, Long idUsuario, String emailUsuario, String descripcion, String ip)`:
  - Obtener `hashAnterior = hashChainService.getUltimoHash()`
  - Construir `RegistroAuditoria` con `timestampEvento = LocalDateTime.now()`
  - Calcular `hashActual = hashChainService.calcularHash(registro, hashAnterior)`
  - `auditoriaRepository.save(registro)`
  - ⚠️ Este método DEBE ejecutarse dentro de la `@Transactional` del llamador (no tiene su propia transacción)
- [ ] T009 [Foundation] Test unitario `HashChainServiceTest`:
  - `sha256("GENESIS-AUDITORIA-INSTITUCIONAL")` produce un hash hex de 64 caracteres
  - `calcularHash()` es determinístico — mismos inputs → mismo output
  - `calcularHash()` difiere si cualquier campo cambia (sensibilidad)
- [ ] T010 [Foundation] Test unitario `AuditoriaServiceTest`:
  - `registrar()` produce un registro con `hashAnterior = GENESIS_HASH` cuando no hay registros previos
  - `registrar()` encadena correctamente el hash del registro anterior

**Checkpoint**: `AuditoriaService.registrar()` funciona. Las features 002 y 003 pueden integrarlo. La cadena de hashes es correcta.

---

## Phase 3: US1 — Registro automático de cada evento de negocio (Priority: P1) 🎯 MVP

**Goal**: Cada cambio relevante en el sistema genera automáticamente un registro en `auditoria` dentro de la misma transacción del evento de negocio.

**Independent Test**: Después de `PUT /api/docente/inscripciones/{id}/nota` → exactamente 1 nuevo registro en `auditoria` con `entidad = 'INSCRIPCION'`, `accion = 'NOTA_CARGADA'`, hash correcto.

### Tests para US1

- [ ] T011 [P] [US1] Test integración — `PUT .../nota` → 1 registro en `auditoria` con campos correctos
- [ ] T012 [P] [US1] Test integración — `PATCH .../cerrar` → registro `NOTA_CERRADA` en `auditoria`
- [ ] T013 [P] [US1] Test de atomicidad — simular falla después de `save(nota)` pero antes de `registrar()` → rollback completo (0 cambios en DB)
- [ ] T014 [P] [US1] Test de atomicidad — simular falla dentro de `registrar()` → rollback completo (nota no persiste tampoco)
- [ ] T015 [P] [US1] Test concurrencia — 1.000 inserts de auditoría simultáneos → verificar que no hay gaps ni hashes duplicados en la cadena

### Implementación de US1

- [ ] T016 [US1] Integrar `AuditoriaService.registrar(NOTA_CARGADA, ...)` dentro de `NotaService.cargarNota()` `@Transactional`
- [ ] T017 [US1] Integrar `AuditoriaService.registrar(NOTA_CERRADA, ...)` dentro de `NotaService.cerrarNota()` `@Transactional`
- [ ] T018 [US1] Integrar `AuditoriaService.registrar(NOTA_REABIERTA, ...)` dentro de `NotaService.reabrir()` `@Transactional`
- [ ] T019 [US1] Integrar `AuditoriaService.registrar(INSCRIPCION_CONFIRMADA/ENCOLADA/CANCELADA, ...)` en `InscripcionService` para los cambios de estado de inscripción
- [ ] T020 [US1] Integrar `AuditoriaService.registrar(LOGIN_FALLIDO/CUENTA_BLOQUEADA, ...)` en `AuthService.login()` cuando corresponda
- [ ] T021 [US1] Integrar `AuditoriaService.registrar(USUARIO_CREADO/ELIMINADO, ...)` en `AdminService` para cambios de usuarios

**Checkpoint**: Todos los eventos de negocio generan registros en auditoría. La cadena de hashes es continua.

---

## Phase 4: US2 — Admin consulta el log paginado (Priority: P1) 🎯 MVP

**Goal**: `GET /api/admin/auditoria?page=0&size=50` → `Page<RegistroAuditoria>` con metadatos de paginación.

**Independent Test**: Con 10.000 registros en DB → `GET /api/admin/auditoria?page=0&size=50` → HTTP 200 en ≤ 500ms con `totalElements = 10000`, `content.length = 50`.

### Tests para US2

- [ ] T022 [P] [US2] Test integración — `GET /api/admin/auditoria?page=0&size=50` → `Page<>` con `totalElements`, `totalPages`, `content`, `number`
- [ ] T023 [P] [US2] Test — sin parámetros `page`/`size` → usa defaults (page=0, size=50)
- [ ] T024 [P] [US2] Test — `size > 100` → HTTP 400 "El tamaño máximo de página es 100"
- [ ] T025 [P] [US2] Test seguridad — JWT DOCENTE → HTTP 403; JWT ESTUDIANTE → HTTP 403; sin JWT → HTTP 401
- [ ] T026 [P] [US2] Test performance — 10.000 registros en DB → respuesta ≤ 500ms (SC-001 del spec)

### Implementación de US2

- [ ] T027 [US2] Implementar `AuditoriaController.GET /api/admin/auditoria` con `@PreAuthorize("hasRole('ADMINISTRADOR')")` + `Pageable pageable` + validar `size ≤ 100`
- [ ] T028 [US2] Implementar `AuditoriaController.GET /api/admin/auditoria/entidad/{entidad}` → `findByEntidad(entidad, pageable)`
- [ ] T029 [US2] Implementar `AuditoriaController.GET /api/admin/auditoria/entidad/{entidad}/{id}` → `findByEntidadAndIdEntidad(entidad, id, pageable)` ordenado por `timestamp_evento ASC`
- [ ] T030 [US2] Implementar `AuditoriaController.GET /api/admin/auditoria/usuario/{idUsuario}` → `findByIdUsuario(idUsuario, pageable)`
- [ ] T031 [US2] Implementar `AuditoriaController.GET /api/admin/auditoria/accion/{accion}` → `findByAccion(accion, pageable)`
- [ ] T032 [US2] Frontend — verificar que `auditoria.service.ts` llama a `GET /api/admin/auditoria?page=X&size=Y` y procesa `Page<RegistroAuditoria>` ✅ (ya hecho en gap fix)

**Checkpoint**: Admin puede consultar el log paginado. Performance SC-001 verificada.

---

## Phase 5: US3 — Verificación de integridad de la cadena (Priority: P2)

**Goal**: `GET /api/admin/auditoria/verificar` → recorre la cadena completa de hashes y reporta inconsistencias.

**Independent Test**: Cadena íntegra → `{ integra: true }`. Modificar un registro en DB directamente → `{ integra: false, errores: ["Registro #X: hash inválido"] }`.

### Tests para US3

- [ ] T033 [P] [US3] Test unitario `HashChainService.verificarCadena()` — cadena íntegra → `integra: true, errores: []`
- [ ] T034 [P] [US3] Test — simular modificación de un registro → `verificarCadena()` detecta hash inválido y lo reporta
- [ ] T035 [P] [US3] Test — verificación de 10.000 registros completa en ≤ 30 segundos (SC-002 del spec)

### Implementación de US3

- [ ] T036 [US3] Implementar `HashChainService.verificarCadena()`:
  - Cargar todos los registros `ORDER BY id_registro ASC` (en batches para no cargar 500k en memoria)
  - Para cada registro: recalcular `hashActual` con el `hashAnterior` del registro previo
  - Si no coincide → agregar a `errores[]`
  - Retornar `IntegridadResponse { integra, totalRegistros, errores, mensaje }`
- [ ] T037 [US3] Implementar `AuditoriaController.GET /api/admin/auditoria/verificar`
- [ ] T038 [P] [US3] Crear `IntegridadResponse.java` DTO alineado con TypeScript: `{ integra: boolean, totalRegistros: number, errores: String[], mensaje: String }`

**Checkpoint**: La verificación de integridad detecta manipulaciones directas en la BD.

---

## Phase 6: US4 — Frontend VistaAuditoria con paginación (Priority: P2)

**Goal**: `VistaAuditoria.tsx` muestra el log paginado con controles de navegación y botón "Verificar integridad".

**Independent Test**: Componente carga página 0 con 50 registros, muestra `totalElements`, permite navegar a página siguiente.

### Tests para US4

- [ ] T039 [P] [US4] Test unitario React — `VistaAuditoria` renderiza `totalElements` y controles de paginación cuando la respuesta es `Page<>`
- [ ] T040 [P] [US4] Test — botón "Verificar integridad" llama a `auditoriaService.verificarIntegridad()` y muestra resultado

### Implementación de US4

- [ ] T041 [US4] Actualizar `VistaAuditoria.tsx`: reemplazar consumo de array plano por `Page<RegistroAuditoria>`; agregar estado `currentPage`, `totalPages`; botones "Anterior" / "Siguiente" / input de página
- [ ] T042 [US4] Agregar indicador de `totalElements` ("Mostrando X–Y de Z registros")
- [ ] T043 [US4] Agregar botón "Verificar integridad de cadena" que llama a `auditoriaService.verificarIntegridad()` y muestra resultado en alert/modal
- [ ] T044 [P] [US4] Frontend — verificar que `auditoria.service.ts` tiene `Page<T>` interface y return type correcto ✅ (ya hecho en gap fix)

**Checkpoint**: El frontend muestra el log paginado. La verificación de integridad es accesible para el admin.

---

## Phase 7: Polish y Cross-Cutting

- [ ] T045 [P] Agregar `@Column(updatable = false)` e `@Immutable` en la entidad `RegistroAuditoria` — ningún UPDATE es posible a nivel JPA
- [ ] T046 [P] Implementar extracción de IP: `X-Forwarded-For` header con fallback a `HttpServletRequest.getRemoteAddr()` — crear `IpExtractor.java` utilitario
- [ ] T047 [P] Documentar tabla de acciones en `README` del proyecto: qué `accion` se registra por cada operación (NOTA_CARGADA, NOTA_CERRADA, NOTA_REABIERTA, INSCRIPCION_CONFIRMADA, etc.)
- [ ] T048 Test de performance — `GET /api/admin/auditoria?page=0&size=50` con dataset de 10.000 registros → ≤ 500ms — adjuntar resultado como artefacto de release (SC-001 del spec — **obligatorio antes de producción**)
- [ ] T049 [P] Verificar retención 5 años: confirmar que no existe endpoint `DELETE` en `AuditoriaController`; agregar test que `DELETE /api/admin/auditoria/**` retorna HTTP 405 Method Not Allowed
- [ ] T050 [P] Seed SQL de datos de prueba: script que inserta 10.000 registros de auditoría con cadena de hashes válida — para tests de performance y demo
