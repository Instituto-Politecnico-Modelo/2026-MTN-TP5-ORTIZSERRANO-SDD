# Implementation Checklist: Autenticación y Autorización JWT

**Purpose**: Verificación completa de implementación, seguridad JWT, RBAC y compliance
**Created**: 2026-06-09
**Feature**: [spec.md](./spec.md)
**Branch**: `002-autenticacion-jwt`

---

## Constitución — Compliance Gate *(verificar antes de merge)*

- [ ] CHK001 Principio II — `JWT_SECRET` leída de variable de entorno; NO hardcodeada en el código fuente ni en `application.properties` versionado
- [ ] CHK002 Principio II — El filtro JWT valida tokens sin consultar MySQL (verificado con logs de Hibernate en debug; 0 queries SELECT a tabla `usuarios` durante validación)
- [ ] CHK003 Principio II — 100% de los endpoints protegidos retornan HTTP 401 sin JWT válido (verificado con suite de tests de autorización)
- [ ] CHK004 Principio II — Un ESTUDIANTE no puede acceder a datos de otro ESTUDIANTE (verificado con test: JWT sub=1 intentando `GET` de recurso con id=2 → HTTP 403)
- [ ] CHK005 Principio V — No existen endpoints de delegación de acceso a terceros (padres/tutores) en esta versión; el código no tiene lógica de "proxy de usuario"

---

## Backend — Endpoint `/api/auth/login`

- [ ] CHK006 `POST /api/auth/login` retorna HTTP 200 con `{ id, email, nombre, apellido, token, refreshToken, type, role, carrera? }` ante credenciales válidas
- [ ] CHK006b El campo `refreshToken` está presente en la respuesta de login (TTL 7 días)
- [ ] CHK007 El campo `role` retornado es uno de: `ESTUDIANTE`, `DOCENTE`, `ADMINISTRADOR`
- [ ] CHK008 El JWT retornado contiene el claim `rol` en su payload (verificable decodificando Base64url)
- [ ] CHK009 El JWT retornado contiene `sub` (userId), `iat` (issued at) y `exp` (expiration) en su payload
- [ ] CHK010 `POST /api/auth/login` retorna HTTP 401 con mensaje genérico (sin distinguir "usuario no existe" vs "contraseña incorrecta") ante credenciales inválidas
- [ ] CHK011 `POST /api/auth/login` retorna HTTP 400 si el body no contiene `email` o `password`
- [ ] CHK012 Las contraseñas se validan contra hash bcrypt almacenado en DB (no en texto plano)

---

## Backend — Filtro JWT y RBAC

- [ ] CHK013 El filtro de Spring Security extrae el token del header `Authorization: Bearer <token>`
- [ ] CHK014 El filtro rechaza tokens con firma inválida → HTTP 401 (sin consultar DB)
- [ ] CHK015 El filtro rechaza tokens expirados → HTTP 401
- [ ] CHK016 El filtro rechaza tokens con header `Authorization` ausente o malformado → HTTP 401
- [ ] CHK017 Las rutas `/api/admin/**` requieren rol `ADMINISTRADOR` — declarado en `SecurityConfig`
- [ ] CHK018 Las rutas `/api/docente/**` requieren rol `DOCENTE` o `ADMINISTRADOR` — declarado en `SecurityConfig`
- [ ] CHK019 Las rutas `/api/inscripciones/**` requieren rol `ESTUDIANTE` — declarado en `SecurityConfig`
- [ ] CHK020 Las rutas `/api/auth/login` y `/api/auth/olvide-password` son públicas (sin JWT requerido)
- [ ] CHK021 La ruta `GET /api/health` es pública (sin JWT requerido — usada por `SalaDeEspera`)
- [ ] CHK022 Un DOCENTE que intenta acceder a `/api/docente/materias/{id}/inscripciones` de una materia que no le pertenece recibe HTTP 403 (verificación de ownership en el servicio, no solo en Spring Security)

---

## Backend — Endpoints adicionales

- [ ] CHK023 `GET /api/auth/me` retorna `{ idUsuario, nombre, apellido, email, activo }` del usuario del JWT
- [ ] CHK023b `POST /api/auth/refresh` acepta `{ refreshToken }` y retorna HTTP 200 `{ token }` con nuevo access token — **en alcance v1**
- [ ] CHK023c `POST /api/auth/refresh` retorna HTTP 401 si el refreshToken está expirado o inválido
- [ ] CHK023d `POST /api/auth/logout` agrega el access token a la blocklist Redis con TTL = tiempo restante del token, retorna HTTP 204 — **en alcance v1**
- [ ] CHK023e El frontend (`authService.logout()`) llama a `POST /api/auth/logout` **antes** de eliminar `authUser` de localStorage
- [ ] CHK023f Un access token que estuvo en la blocklist retorna HTTP 401 aunque no haya expirado (el filtro JWT consulta la blocklist en Redis)
- [ ] CHK024 `GET /api/auth/jwt/inspect?token=<jwt>` retorna información de debugging del token (solo accesible con JWT válido)
- [ ] CHK025 `POST /api/auth/olvide-password` acepta `{ email }` y registra una `SolicitudReset` en estado PENDIENTE
- [ ] CHK026 `GET /api/admin/usuarios/solicitudes-reset` retorna lista de `SolicitudReset` (solo ADMINISTRADOR)
- [ ] CHK027 `POST /api/admin/usuarios/solicitudes-reset/{id}/atender` actualiza la contraseña y marca la solicitud como ATENDIDA (solo ADMINISTRADOR)

---

## Backend — Auditoría de eventos de seguridad

- [ ] CHK028 Un login fallido genera un registro en tabla `auditoria` con `entidad = 'USUARIO'`, `accion = 'LOGIN_FALLIDO'`, `emailUsuario`, `ipOrigen`, `timestampEvento`
- [ ] CHK028b Después de N intentos fallidos consecutivos (valor a confirmar en `AuthService.java`), `POST /api/auth/login` retorna HTTP 423 con mensaje de bloqueo temporal — **en alcance v1, obligatorio**
- [ ] CHK028c El evento de bloqueo genera un registro en tabla `auditoria` con `accion = 'CUENTA_BLOQUEADA'`
- [ ] CHK028d La cuenta se desbloquea automáticamente tras la duración configurada (valor a confirmar en backend); el contador de intentos se resetea al primer login exitoso
- [ ] CHK029 Un login exitoso **no** genera registro de auditoría de seguridad (solo los fallidos, para reducir ruido en el log)
- [ ] CHK030 El registro de auditoría de LOGIN_FALLIDO incluye `hashActual` encadenado correctamente

---

## Frontend — Integración

- [ ] CHK031 `authService.login()` mapea correctamente la respuesta a `LoginResponse` con los campos reales: `id, email, nombre, apellido, token, refreshToken, type, role, carrera?` — **incluye `refreshToken` (decisión 002-B)**
- [ ] CHK032 `authService.getRole()` decodifica el claim `rol` del payload JWT Base64url (sin llamada a red)
- [ ] CHK033 `authService.getDefaultRoute()` redirige a `/admin/dashboard`, `/docente/dashboard` o `/estudiante/dashboard` según el rol
- [ ] CHK034 `axiosInstance` intercepta HTTP 401 → intenta renovar con `POST /api/auth/refresh` usando el `refreshToken` guardado; solo redirige a `/login?motivo=expired` si el refresh también falla (o si no hay `refreshToken`) — **cambio de comportamiento requerido por decisión 002-B**
- [ ] CHK035 `axiosInstance` intercepta HTTP 403 → redirige a `/unauthorized`
- [ ] CHK036 `axiosInstance` intercepta HTTP 429 → redirige a `/sala-de-espera?razon=ratelimit`
- [ ] CHK037 `axiosInstance` intercepta HTTP 503 → redirige a `/sala-de-espera?razon=capacidad`
- [ ] CHK038 `ProtectedRoute` verifica el rol del usuario usando `authService.hasAnyRole()` antes de renderizar la ruta
- [ ] CHK039 `AuthContext` provee `user`, `isAuthenticated`, `login`, `logout` a todos los componentes hijos
- [ ] CHK040 El `logout` en el frontend llama a `POST /api/auth/logout` (server-side blocklist) **antes** de eliminar `authUser` de localStorage y redirigir a `/login` — **decisión 002-B**

---

## Testing

- [ ] CHK041 Test unitario: `POST /api/auth/login` con credenciales válidas → HTTP 200 + JWT con claim `rol` correcto
- [ ] CHK042 Test unitario: `POST /api/auth/login` con contraseña incorrecta → HTTP 401
- [ ] CHK043 Test unitario: `POST /api/auth/login` con usuario inexistente → HTTP 401 (mismo mensaje que contraseña incorrecta)
- [ ] CHK044 Test unitario: endpoint protegido sin header Authorization → HTTP 401
- [ ] CHK045 Test unitario: endpoint protegido con JWT firmado con clave incorrecta → HTTP 401
- [ ] CHK046 Test unitario: endpoint protegido con JWT expirado → HTTP 401
- [ ] CHK047 Test de autorización: JWT de ESTUDIANTE accediendo a `/api/admin/**` → HTTP 403
- [ ] CHK048 Test de autorización: JWT de DOCENTE accediendo a `/api/admin/**` → HTTP 403
- [ ] CHK049 Test de autorización: JWT de ADMINISTRADOR accediendo a cualquier endpoint → HTTP 2xx
- [ ] CHK050 Test de autorización: ESTUDIANTE con sub=1 intentando acceder a inscripciones de ESTUDIANTE id=2 → HTTP 403
- [ ] CHK051 Test de performance: latencia P99 de `POST /api/auth/login` ≤ 300ms bajo carga concurrente

---

## Deployment

- [ ] CHK052 Variable de entorno `JWT_SECRET` configurada en producción (mínimo 256 bits de entropía)
- [ ] CHK053 Variable de entorno `JWT_SECRET` **no** figura en ningún archivo `.env` versionado en git (solo en `.env.example` con valor placeholder)
- [ ] CHK054 El tiempo de expiración del JWT (`JWT_EXPIRATION`) está configurado como variable de entorno (no hardcodeado)
- [ ] CHK055 CORS está configurado para aceptar solo los orígenes permitidos en producción (no `*`)
