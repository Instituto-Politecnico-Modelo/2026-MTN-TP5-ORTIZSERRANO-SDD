# Tasks: Autenticación y Autorización JWT

**Input**: `.specify/memory/002-autenticacion-jwt/` (spec.md + plan.md + checklist.md)

**Prerequisites**: plan.md ✅ | spec.md ✅ (Clarified) | checklist.md ✅

**Tests**: Incluidos — la seguridad perimetral requiere cobertura completa de RBAC.

**Organization**: US1–US3 son P1 (críticos de seguridad); US4–US5 son P2/P3 (mejoras de UX y compliance). Esta feature es prerequisito de todas las demás.

## Format: `[ID] [P?] [Story] Description`

---

## Phase 1: Setup

**Propósito**: Estructura base para la feature de autenticación.

- [ ] T001 [Foundation] Crear rama `002-autenticacion-jwt` desde `main` en el repo TP3
- [ ] T002 [P] [Foundation] Agregar dependencias en `pom.xml`: `spring-boot-starter-security`, `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson`
- [ ] T003 [P] [Foundation] Agregar dependencia `spring-boot-starter-data-redis` para blocklist de logout y bloqueo de cuentas
- [ ] T004 [P] [Foundation] Definir variable de entorno `JWT_SECRET` en `application.properties` como `${JWT_SECRET:dev-secret-change-in-prod}` + agregar al `.env.example`

---

## Phase 2: Foundational — Infraestructura JWT y Security (Bloqueante)

**Propósito**: El filtro JWT y la configuración de Spring Security son prerequisitos de todos los endpoints protegidos de todas las features.

⚠️ **CRÍTICO**: Sin esta fase ningún otro endpoint del sistema puede funcionar con autenticación.

- [ ] T005 [Foundation] Crear/actualizar tabla `usuarios` con columnas `intentos_fallidos TINYINT DEFAULT 0` y `bloqueado_hasta DATETIME NULL` (migración SQL)
- [ ] T006 [Foundation] Crear/actualizar entidad `Usuario.java` con campos: `email`, `passwordHash`, `rol` (enum), `activo`, `intentosFallidos`, `bloqueadoHasta`
- [ ] T007 [Foundation] Implementar `JwtUtil.java`: `generateToken(usuario)`, `generateRefreshToken(usuario)`, `validateToken(token)`, `getEmailFromToken(token)`, `getRolFromToken(token)`, `getRemainingTtl(token)` — clave leída de `JWT_SECRET`, access TTL = 1h, refresh TTL = 7 días
- [ ] T008 [Foundation] Implementar `JwtFilter.java` (`OncePerRequestFilter`): extraer Bearer token → `JwtUtil.validateToken()` → setear `SecurityContextHolder` — **sin consulta a MySQL**, verificar con log Hibernate
- [ ] T009 [Foundation] Implementar `SecurityConfig.java`: rutas públicas (`/api/auth/login`, `/api/auth/refresh`, `/api/auth/olvide-password`, `/api/health`); rutas por rol (`ADMINISTRADOR`, `DOCENTE`, `ESTUDIANTE`); deshabilitar CSRF; configurar `JwtFilter`
- [ ] T010 [Foundation] Implementar `UserDetailsServiceImpl.java` (usado solo en login para cargar usuario de BD, no en validación de tokens)
- [ ] T011 [Foundation] Test unitario `JwtUtilTest` — generar token → validar → extraer claims; token expirado → excepción; token firma inválida → excepción
- [ ] T012 [Foundation] Test unitario `JwtFilterTest` — token válido → SecurityContext seteado sin DB call; token inválido → HTTP 401; sin token → HTTP 401

**Checkpoint**: El filtro JWT funciona. Todos los endpoints protegidos retornan 401 sin token. El DB no es consultado en cada request.

---

## Phase 3: US1 — Login y emisión de JWT (Priority: P1) 🎯 MVP

**Goal**: Usuario con credenciales válidas recibe `{ token, refreshToken, rol }` → HTTP 200.

**Independent Test**: `POST /api/auth/login` con `{ email, password }` válidos → HTTP 200 `LoginResponse` con `token` y `refreshToken`. Con credenciales inválidas → HTTP 401.

### Tests para US1

- [ ] T013 [P] [US1] Test unitario `AuthService.login()` — credenciales válidas → `LoginResponse` con token y refreshToken
- [ ] T014 [P] [US1] Test unitario — contraseña incorrecta → `intentosFallidos++` → HTTP 401 (sin distinguir email vs contraseña)
- [ ] T015 [P] [US1] Test integración — `POST /api/auth/login` → HTTP 200 + JWT decodificable con claims `sub`, `rol`, `exp`

### Implementación de US1

- [ ] T016 [US1] Implementar `AuthService.login(email, password)`: cargar usuario → `BCrypt.checkpw()` → si OK: resetear `intentosFallidos`, generar access + refresh token, guardar → retornar `LoginResponse`
- [ ] T017 [US1] Implementar `AuthController.POST /api/auth/login`: delegar a `AuthService`, manejar `BadCredentialsException` → HTTP 401
- [ ] T018 [US1] Crear `LoginRequest.java` `{ @Email String email, @NotBlank String password }` con validación `@Valid`
- [ ] T019 [US1] Crear `LoginResponse.java` `{ id, email, nombre, apellido, token, refreshToken, type, role, carrera? }` — alineado con `auth.types.ts` ✅
- [ ] T020 [US1] Frontend — verificar que `auth.service.ts` guarda `LoginResponse` (incluyendo `refreshToken`) en localStorage bajo clave `authUser` ✅ (ya hecho en gap fix)

**Checkpoint**: Login funciona. JWT retornado incluye `refreshToken`. Claims correctos.

---

## Phase 4: US2 — Protección de endpoints (RBAC) (Priority: P1) 🎯 MVP

**Goal**: Todos los endpoints protegidos retornan 401 sin JWT y 403 con JWT de rol incorrecto.

**Independent Test**: `GET /api/admin/usuarios` sin token → 401. Con token ESTUDIANTE → 403. Con token ADMINISTRADOR → 200.

### Tests para US2

- [ ] T021 [P] [US2] Suite de tests de autorización: para cada endpoint del sistema, verificar HTTP 401 (sin token), HTTP 403 (rol incorrecto), HTTP 200 (rol correcto)
- [ ] T022 [P] [US2] Test — ESTUDIANTE accede a sus propios datos (`sub` coincide) → HTTP 200; accede a datos de otro alumno → HTTP 403

### Implementación de US2

- [ ] T023 [US2] Revisar y completar `SecurityConfig.java` con reglas exhaustivas por endpoint (`/api/admin/**` = ADMINISTRADOR, `/api/docente/**` = DOCENTE|ADMINISTRADOR, `/api/inscripciones/**` = ESTUDIANTE)
- [ ] T024 [US2] Agregar `@PreAuthorize` granular en controllers donde la regla de RBAC es más específica que el path (ej: docente solo accede a sus materias)
- [ ] T025 [US2] Implementar `GlobalExceptionHandler` para `AccessDeniedException` → HTTP 403 `{ error: "Acceso denegado", message: "Se requiere rol X" }`
- [ ] T026 [P] [US2] Frontend — verificar que `ProtectedRoute.tsx` protege las rutas del dashboard por rol y redirige a `/unauthorized` si el rol no coincide

**Checkpoint**: RBAC funciona correctamente. Ningún endpoint expone datos sin el rol correcto.

---

## Phase 5: US3 — Control de acceso por rol + bloqueo de cuenta (Priority: P1)

**Goal**: Bloqueo temporal tras N intentos fallidos → HTTP 423 con tiempo restante.

**Independent Test**: N+1 intentos con contraseña incorrecta → HTTP 423. Cuenta bloqueada registrada en auditoría.

### Tests para US3

- [ ] T027 [P] [US3] Test unitario — N intentos fallidos consecutivos → `bloqueadoHasta` seteado → intento N+1 → HTTP 423 con `X-Retry-After`
- [ ] T028 [P] [US3] Test integración — cuenta bloqueada + credenciales correctas → HTTP 423 (bloqueo tiene prioridad sobre credenciales)
- [ ] T029 [P] [US3] Test — después del TTL de bloqueo, cuenta desbloqueada automáticamente → login normal

### Implementación de US3

- [ ] T030 [US3] Implementar lógica de bloqueo en `AuthService.login()`: si `bloqueadoHasta > now()` → HTTP 423 con tiempo restante; si N intentos fallidos consecutivos → setear `bloqueadoHasta = now() + DURACION`; si login OK → resetear `intentosFallidos = 0` + `bloqueadoHasta = null`
- [ ] T031 [US3] Configurar constantes `MAX_INTENTOS_FALLIDOS` y `DURACION_BLOQUEO_MINUTOS` como propiedades de aplicación (no hardcodeadas)
- [ ] T032 [US3] Registrar evento `CUENTA_BLOQUEADA` en tabla `auditoria` al alcanzar el límite — llamar a `AuditoriaService.registrar()` dentro de la transacción de login
- [ ] T033 [P] [US3] Frontend — `Login.tsx` muestra mensaje diferenciado para HTTP 423: "Tu cuenta está bloqueada por X minutos"

**Checkpoint**: El bloqueo de cuenta funciona. Evento registrado en auditoría.

---

## Phase 6: US4 — Renovación de token (refreshToken) (Priority: P2)

**Goal**: `POST /api/auth/refresh` con refreshToken válido → nuevo access token sin re-login.

**Independent Test**: `POST /api/auth/refresh` con refreshToken válido → HTTP 200 `{ token }`. Con refreshToken expirado → HTTP 401.

### Tests para US4

- [ ] T034 [P] [US4] Test unitario `AuthService.refresh()` — refreshToken válido → nuevo access token generado
- [ ] T035 [P] [US4] Test integración — refreshToken expirado → HTTP 401
- [ ] T036 [P] [US4] Test frontend — `axiosInstance` interceptor 401: si refreshToken en localStorage → intenta refresh → reintenta request original

### Implementación de US4

- [ ] T037 [US4] Implementar `AuthService.refresh(refreshToken)`: validar refreshToken → generar nuevo access token → retornar `{ token, refreshToken }`
- [ ] T038 [US4] Implementar `AuthController.POST /api/auth/refresh` con `@RequestBody RefreshRequest`
- [ ] T039 [US4] Frontend — verificar `axiosInstance.ts` interceptor 401: refresh-first antes de redirigir a login ✅ (ya hecho en gap fix); manejar cola de requests pendientes durante refresh ✅

**Checkpoint**: Sesiones largas funcionan sin re-login. El interceptor 401 renueva transparentemente.

---

## Phase 7: US5 — Logout server-side (Priority: P3)

**Goal**: `POST /api/auth/logout` invalida el JWT en Redis blocklist → HTTP 204.

**Independent Test**: Login → logout → usar el mismo JWT en otro endpoint → HTTP 401.

### Tests para US5

- [ ] T040 [P] [US5] Test integración — login → logout → request con mismo JWT → HTTP 401
- [ ] T041 [P] [US5] Test — JWT expirado en blocklist se limpia automáticamente (TTL Redis correcto)

### Implementación de US5

- [ ] T042 [US5] Implementar `TokenBlocklistService.java`: `addToBlocklist(token)` con TTL = `JwtUtil.getRemainingTtl(token)`; `isBlacklisted(token)` → boolean
- [ ] T043 [US5] Modificar `JwtFilter.java` para verificar blocklist en cada request: `if (tokenBlocklist.isBlacklisted(token)) → HTTP 401`
- [ ] T044 [US5] Implementar `AuthController.POST /api/auth/logout`: extraer JWT del header → `tokenBlocklist.addToBlocklist()` → HTTP 204
- [ ] T045 [US5] Frontend — actualizar `authService.logout()`: llamar a `POST /api/auth/logout` primero → luego `localStorage.removeItem('authUser')` ✅ (pendiente verificación)

**Checkpoint**: Logout invalida el token en el servidor. El mismo JWT no funciona después del logout.

---

## Phase 8: Polish y Cross-Cutting

- [ ] T046 [P] Implementar `GET /api/auth/me` → retornar perfil del usuario autenticado desde BD (nombre, apellido, email, activo)
- [ ] T047 [P] Implementar `GET /api/auth/jwt/inspect?token=` → `JwtInspectResponse` para debugging (requiere autenticación)
- [ ] T048 [P] Implementar `POST /api/auth/olvide-password` (público) + `GET/POST /api/admin/usuarios/solicitudes-reset/{id}/atender` (ADMINISTRADOR)
- [ ] T049 [P] Registrar intentos de login fallidos en tabla `auditoria` con `accion = 'LOGIN_FALLIDO'`, timestamp e IP — llamar a `AuditoriaService` dentro de la transacción de login
- [ ] T050 [P] Test de performance: validación JWT + blocklist check en ≤ 5ms adicionales por request (SC-001 del spec)
- [ ] T051 [P] Verificar con logs Hibernate en modo debug que 0 queries MySQL se ejecutan durante validación de JWT en `JwtFilter` (SC-002 del spec)
