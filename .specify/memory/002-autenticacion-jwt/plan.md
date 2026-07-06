# Implementation Plan: Autenticación y Autorización JWT

**Branch**: `002-autenticacion-jwt` | **Date**: 2026-06-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification — "Autenticación y autorización JWT con roles alumno, docente, admin"

## Summary

Sistema de autenticación stateless basado en JWT (JJWT + Spring Security) con tres roles (`ESTUDIANTE`, `DOCENTE`, `ADMINISTRADOR`). El access token (TTL 1h) se valida en cada request sin consultar la base de datos. El refresh token (TTL 7 días) permite renovar la sesión sin re-login. El logout invalida el access token vía blocklist en Redis. El bloqueo de cuenta por intentos fallidos y el password reset están incluidos en v1. El frontend gestiona automáticamente la renovación transparente en el interceptor 401 de `axiosInstance.ts`.

## Technical Context

**Language/Version**: Java 17 (backend) / TypeScript 5 (frontend)

**Primary Dependencies**:
- Backend: Spring Boot 3, Spring Security 6, JJWT 0.11+, Spring Data Redis, BCrypt
- Frontend: React 18, Axios (interceptores), React Router 6, JWT decode

**Storage**: MySQL 8 (tabla `usuarios` con `bloqueado_hasta`, `intentos_fallidos`) + Redis (blocklist de tokens revocados; TTL = tiempo restante del token)

**Testing**: JUnit 5 + Mockito + MockMvc + Spring Security Test (backend) / Jest + React Testing Library (frontend)

**Target Platform**: Linux server (backend Spring Boot JAR) + Web browser (React SPA)

**Project Type**: Web application — `backend/` + `frontend/`

**Performance Goals**:
- Validación JWT sin DB lookup — latencia < 5ms adicionales por request bajo cualquier carga
- Redis blocklist: TTL exacto del token; lookup O(1)
- Login con BCrypt: < 300ms p95 (BCrypt cost factor balanceado contra seguridad)

**Constraints**:
- Clave de firma JWT NUNCA puede estar hardcodeada — variable de entorno `JWT_SECRET`
- Access token TTL: exactamente 1 hora
- Refresh token TTL: exactamente 7 días
- Bloqueo de cuenta: N intentos fallidos consecutivos → HTTP 423 + tiempo restante
- `axiosInstance` intenta refresh ANTES de redirigir al login en un 401

**Scale/Scope**: 50.000 usuarios activos; el filtro JWT se ejecuta en cada uno de los ~500k requests/hora durante picos

## Constitution Check

| Principio | Gate | Estado |
|---|---|---|
| **I. Alta Concurrencia** | Validación stateless obligatoria — sin DB lookup por request | ✅ cumplido — JWT.verify() es O(1), sin consulta a BD |
| **II. JWT Auth** | Esta feature IS el Principio II | ✅ NÚCLEO — implementación directa |
| **III. Inmutabilidad/Auditoría** | Intentos de login fallidos + `CUENTA_BLOQUEADA` en tabla `auditoria` | ✅ cumplido — eventos de seguridad registrados |
| **IV. Stack Definido** | Spring Security 6 + JJWT + Redis | ✅ cumplido — no se introducen librerías de auth de terceros |
| **V. Privacidad Ley 25.326** | Tokens no almacenados en DB; mínimo privilegio por rol; logout revoca acceso | ✅ cumplido — el usuario puede revocar acceso en cualquier momento (Art. 6) |
| **VI. Disponibilidad** | Redis blocklist con TTL — si Redis cae, tokens no revocados siguen siendo válidos hasta expiración natural (degradación documentada) | ✅ aceptable — ventana de riesgo = TTL restante del token |

## Project Structure

### Documentation (esta feature)

```text
.specify/memory/002-autenticacion-jwt/
├── spec.md            ✅ Clarified
├── plan.md            ← este archivo
└── checklist.md       ✅ generado con 55+ ítems CHK
```

### Source Code

```text
backend/
├── src/main/java/com/ipm/auth/
│   ├── model/
│   │   └── Usuario.java              # email, password (BCrypt), rol, intentosFallidos, bloqueadoHasta
│   ├── security/
│   │   ├── JwtUtil.java              # generateToken(), validateToken(), getClaimsFromToken()
│   │   ├── JwtFilter.java            # OncePerRequestFilter — valida JWT sin DB lookup
│   │   ├── SecurityConfig.java       # HttpSecurity — rutas públicas/protegidas por rol
│   │   └── UserDetailsServiceImpl.java
│   ├── service/
│   │   ├── AuthService.java          # login(), refresh(), logout(), bloqueoLogic()
│   │   └── TokenBlocklistService.java  # Redis SET con TTL del token
│   ├── controller/
│   │   └── AuthController.java       # POST /login, /refresh, /logout; GET /me, /jwt/inspect
│   └── dto/
│       ├── LoginRequest.java         # { email, password }
│       ├── LoginResponse.java        # { id, email, nombre, apellido, token, refreshToken, type, role, carrera? }
│       └── RefreshRequest.java       # { refreshToken }
└── src/test/
    ├── unit/AuthServiceTest.java
    ├── unit/JwtUtilTest.java
    └── integration/AuthFlowTest.java

frontend/
├── src/
│   ├── api/
│   │   └── axiosInstance.ts          ✅ actualizado — refresh-first en interceptor 401
│   ├── services/
│   │   └── auth.service.ts           # login(), logout(), getCurrentUser(), inspectJwt()
│   ├── types/
│   │   └── auth.types.ts             ✅ actualizado — LoginResponse.refreshToken
│   ├── context/
│   │   └── AuthContext.tsx           # proveedor de sesión global
│   └── components/
│       ├── Login.tsx                 # formulario; muestra error HTTP 423 con tiempo restante
│       ├── ProtectedRoute.tsx        # guarda de ruta por rol
│       └── Unauthorized.tsx          # destino de redirect HTTP 403
```

**Structure Decision**: Web application (Option 2). Backend y frontend en el mismo repositorio TP3.

## API Contracts

| Método | Path | Rol JWT | Request Body | Response |
|---|---|---|---|---|
| `POST` | `/api/auth/login` | Público | `{ email, password }` | 200 `LoginResponse` \| 401 \| 423 |
| `POST` | `/api/auth/refresh` | Público | `{ refreshToken }` | 200 `{ token, refreshToken }` \| 401 |
| `POST` | `/api/auth/logout` | Cualquier autenticado | — | 204 |
| `GET` | `/api/auth/me` | Cualquier autenticado | — | 200 `User` |
| `GET` | `/api/auth/jwt/inspect` | Cualquier autenticado | `?token=` | 200 `JwtInspectResponse` |
| `POST` | `/api/auth/olvide-password` | Público | `{ email }` | 200 |

## Data Model

```sql
CREATE TABLE usuarios (
  id_usuario         BIGINT PRIMARY KEY AUTO_INCREMENT,
  nombre             VARCHAR(100) NOT NULL,
  apellido           VARCHAR(100) NOT NULL,
  email              VARCHAR(255) UNIQUE NOT NULL,
  password_hash      VARCHAR(255) NOT NULL,       -- BCrypt
  rol                ENUM('ESTUDIANTE','DOCENTE','ADMINISTRADOR') NOT NULL,
  activo             BOOLEAN DEFAULT TRUE,
  intentos_fallidos  TINYINT DEFAULT 0,
  bloqueado_hasta    DATETIME NULL,               -- NULL = no bloqueado
  carrera            VARCHAR(100)                 -- solo ESTUDIANTE
);
```

**Redis**: `blacklist:token:{jti}` — SET con TTL = segundos restantes del access token.

## Refresh Token Flow

```
1. POST /api/auth/login → { token (1h), refreshToken (7d) } → localStorage
2. Request con token → OK
3. Request con token expirado → backend: 401
4. axiosInstance intercepta 401:
   a. ¿existe refreshToken en localStorage? → SÍ → POST /api/auth/refresh
   b. Refresh OK (200) → actualizar localStorage → reintentar request original
   c. Refresh 401 → removeItem(USER_KEY) → /login?motivo=expired
5. POST /api/auth/logout → backend agrega token a Redis blocklist → 204
```

## Complexity Tracking

> No hay violaciones a la constitución en esta feature.

| Elemento | Justificación |
|---|---|
| Redis blocklist | Principio II exige logout con revocación real; sin Redis la revocación es imposible con JWT stateless |
| Refresh token + access token separados | UX: sesiones largas sin re-login; el access token corto (1h) limita la ventana de exposición |
