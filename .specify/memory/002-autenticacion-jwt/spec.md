# Feature Specification: Autenticación y Autorización JWT

**Feature Branch**: `002-autenticacion-jwt`

**Created**: 2026-06-09

**Status**: Clarified

**Input**: User description: "Autenticación y autorización JWT con roles alumno, docente, admin"

## Clarification Notes *(added 2026-06-09 — reconciliado con código existente)*

> | # | Ítem | Spec Draft | Clarificado |
> |---|---|---|---|
> | C1 | **Roles del sistema** | `alumno`, `docente`, `admin` | `ESTUDIANTE`, `DOCENTE`, `ADMINISTRADOR` (enum `Role` en `auth.types.ts`) |
> | C2 | **LoginResponse** | `{ token, refreshToken, rol, nombre }` | `{ id, email, nombre, apellido, token, type, role, carrera? }` |
> | C3 | **Claim JWT** | Genérico | Claim `rol` en el payload del JWT (Spring: `JwtUtil.java`). Access token TTL: **1 hora** ✅ confirmado. RefreshToken TTL: **7 días**. |
> | C4 | **refreshToken** | Implementado como feature P2 | **En alcance v1** — `LoginResponse` debe incluir `refreshToken`; se implementa `POST /api/auth/refresh` en backend y frontend. |
> | C5 | **Logout** | Server-side blocklist en Redis | **En alcance v1** — se implementa `POST /api/auth/logout` con blocklist en Redis; el frontend pasa de client-side a server-side logout. |
> | C6 | **Rutas por rol** | No especificadas | `/admin/dashboard` (ADMINISTRADOR), `/docente/dashboard` (DOCENTE), `/estudiante/dashboard` (ESTUDIANTE) |
> | C7 | **HTTP 401** | Redirige a login | `axiosInstance` intercepta → limpia localStorage + redirige a `/login?motivo=expired` |
> | C8 | **HTTP 403** | Mensaje de error | `axiosInstance` intercepta → redirige a `/unauthorized` |
> | C9 | **HTTP 429 / 503** | No en spec | `axiosInstance` intercepta → redirige a `/sala-de-espera` |
> | C10 | **Endpoint adicional** | No en spec | `GET /api/auth/jwt/inspect?token=` — inspección y debugging de JWT (requiere auth) |
> | C11 | **Password reset** | No en spec | `POST /api/auth/olvide-password` (público); admin atiende en `GET/POST /api/admin/usuarios/solicitudes-reset` |
> | C12 | **Bloqueo de cuenta** | 5 intentos → 15 min | **En alcance v1** — el backend implementa bloqueo temporal. Valores exactos (cantidad de intentos y duración) a confirmar en el código de `JwtUtil.java` / `AuthService.java`. Registrar evento `CUENTA_BLOQUEADA` en auditoría. |

## Constitution Check

| Principio | Aplica | Notas |
|---|---|---|
| I. Alta Concurrencia | ✅ | Validación stateless (sin DB lookup) reduce latencia bajo carga |
| II. JWT Auth | ✅ NÚCLEO | Esta feature es la implementación directa del Principio II |
| III. Inmutabilidad/Auditoría | ✅ | Intentos de login fallidos registrados en auditoría |
| IV. Stack Definido | ✅ | Spring Security + JJWT / React 18 |
| V. Privacidad Ley 25.326 | ✅ | No se almacenan tokens en DB; mínimo privilegio por rol |
| VI. Disponibilidad | ✅ | Validación stateless no agrega latencia bajo alta carga |

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Usuario se autentica y recibe un JWT válido (Priority: P1)

Un usuario (alumno, docente o admin) proporciona sus credenciales y recibe un token
JWT firmado que incluye su rol y le permite acceder a la API.

**Why this priority**: Sin autenticación no existe ninguna otra funcionalidad. Es el
punto de entrada a todo el sistema.

**Independent Test**: `POST /api/auth/login` con credenciales válidas retorna HTTP 200
con un body que contiene `{ token, refreshToken, rol, nombre }`.

**Acceptance Scenarios**:

1. **Given** un usuario registrado con email y contraseña válidos, **When** llama a
   `POST /api/auth/login` con `{ "email": "...", "password": "..." }`, **Then** el
   sistema retorna HTTP 200 con un JWT firmado que en su payload incluye `sub`
   (userId), `rol` (alumno/docente/admin), y `exp` (expiración a 1 hora).

2. **Given** credenciales incorrectas, **When** se llama a `POST /api/auth/login`,
   **Then** el sistema retorna HTTP 401 con mensaje genérico "Credenciales inválidas"
   (sin distinguir si el usuario no existe o la contraseña es incorrecta, por seguridad).

3. **Given** un usuario bloqueado (5 intentos fallidos consecutivos), **When** intenta
   hacer login, **Then** el sistema retorna HTTP 423 con mensaje "Cuenta temporalmente
   bloqueada" y tiempo restante de bloqueo.

---

### User Story 2 — Endpoints de la API rechazan requests sin JWT válido (Priority: P1)

Todos los endpoints protegidos deben retornar HTTP 401 si el token está ausente,
malformado, expirado o con firma inválida.

**Why this priority**: La seguridad perimetral de toda la API depende de este
comportamiento. Un fallo aquí expone datos de todos los usuarios.

**Independent Test**: `GET /api/inscripciones/mias` sin header `Authorization` retorna
HTTP 401. Con token expirado también retorna HTTP 401.

**Acceptance Scenarios**:

1. **Given** cualquier endpoint protegido, **When** se llama sin header `Authorization`,
   **Then** el sistema retorna HTTP 401 con body `{ "error": "Token no proporcionado" }`.

2. **Given** un JWT con firma manipulada, **When** se envía en el header
   `Authorization: Bearer <token>`, **Then** el sistema retorna HTTP 401 sin consultar
   la base de datos.

3. **Given** un JWT válido pero expirado, **When** se envía en la solicitud, **Then**
   el sistema retorna HTTP 401 con `{ "error": "Token expirado" }` y el frontend
   intenta renovar con el `refreshToken` automáticamente.

---

### User Story 3 — Control de acceso por rol (RBAC — mínimo privilegio) (Priority: P1)

Un alumno no puede acceder a endpoints de docente o admin. Un docente no puede acceder
a endpoints de admin ni a datos de materias ajenas.

**Why this priority**: Violaciones de RBAC pueden exponer datos de todos los alumnos
o permitir modificación de notas sin autorización — consecuencias críticas.

**Independent Test**: `GET /api/admin/usuarios` con JWT de rol `alumno` retorna HTTP 403.

**Acceptance Scenarios**:

1. **Given** un JWT con rol `alumno`, **When** intenta acceder a
   `GET /api/admin/usuarios`, **Then** el sistema retorna HTTP 403 "Acceso denegado:
   se requiere rol admin".

2. **Given** un JWT con rol `docente`, **When** intenta acceder a
   `GET /api/materias/{id}/inscriptos` de una materia que no le pertenece, **Then**
   el sistema retorna HTTP 403.

3. **Given** un JWT con rol `admin`, **When** accede a cualquier endpoint del sistema,
   **Then** el acceso es concedido (rol admin tiene acceso completo de lectura/escritura
   administrativo).

---

### User Story 4 — Renovación de token con refreshToken (Priority: P2)

Un usuario con un JWT expirado puede obtener un nuevo JWT usando su refreshToken
sin necesidad de volver a ingresar sus credenciales.

**Why this priority**: Mejora la experiencia de usuario (UX) en sesiones largas y
es necesario para que el frontend no interrumpa operaciones en curso.

**Independent Test**: `POST /api/auth/refresh` con un refreshToken válido retorna HTTP
200 con un nuevo JWT.

**Acceptance Scenarios**:

1. **Given** un refreshToken válido y no expirado, **When** se llama a
   `POST /api/auth/refresh` con `{ "refreshToken": "..." }`, **Then** el sistema
   retorna HTTP 200 con un nuevo JWT (access token) con expiración renovada a 1 hora.

2. **Given** un refreshToken expirado o inválido, **When** se llama a
   `POST /api/auth/refresh`, **Then** el sistema retorna HTTP 401 y el frontend
   redirige al login.

---

### User Story 5 — Cierre de sesión (logout) (Priority: P3)

Un usuario puede cerrar sesión explícitamente, invalidando su sesión activa.

**Why this priority**: El logout es requerimiento básico de UX y compliance (Ley
25.326 requiere que el usuario pueda revocar el acceso a sus datos en cualquier momento).

**Independent Test**: `POST /api/auth/logout` con JWT válido retorna HTTP 204 y
el token queda en la blocklist.

**Acceptance Scenarios**:

1. **Given** un usuario con JWT válido, **When** llama a `POST /api/auth/logout`,
   **Then** el sistema retorna HTTP 204, agrega el JWT a una blocklist en Redis con
   TTL igual al tiempo restante del token, y cualquier uso posterior del mismo JWT
   retorna HTTP 401.

---

### Edge Cases

- ¿Qué pasa si la clave de firma del JWT cambia (rotación)?
  → Los tokens firmados con la clave anterior se invalidan automáticamente (expiración
  natural). La rotación debe coordinar deployment cuidadoso.
- ¿Qué pasa si se envía un JWT de otro ambiente (staging vs. producción)?
  → La firma falla por clave distinta → HTTP 401.
- ¿Qué pasa si el body de login viene con campos faltantes?
  → HTTP 400 con detalle de validación.
- ¿Qué pasa con roles que no existen en el sistema (`super_admin`, etc.)?
  → El filtro de Spring Security rechaza el token si el rol no es uno de los tres
  definidos → HTTP 401.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE exponer `POST /api/auth/login` que valide credenciales y
  retorne un JWT firmado con `sub`, `rol`, `nombre` y `exp` (1 hora).
- **FR-002**: El JWT DEBE ser generado con JJWT (librería institucional del stack) y
  firmado con algoritmo HS256 o RS256. La clave de firma NO puede estar hardcodeada;
  DEBE leerse de variable de entorno.
- **FR-003**: El filtro de autenticación de Spring Security DEBE validar el JWT en
  cada request sin consultar la base de datos.
- **FR-004**: El sistema DEBE implementar RBAC con tres roles: `alumno`, `docente`,
  `admin`. Los permisos por endpoint deben estar declarados en la configuración de
  Spring Security.
- **FR-005**: Un alumno solo PUEDE acceder a sus propios datos. Los endpoints que
  retornan datos de un alumno específico DEBEN verificar que el `alumnoId` en el path
  coincide con el `sub` del JWT (o que el rol es `admin`).
- **FR-006**: El sistema DEBE implementar `POST /api/auth/refresh` para renovar access
  tokens con un refreshToken válido (TTL del refreshToken: 7 días).
- **FR-007**: El sistema DEBE implementar `POST /api/auth/logout` con blocklist en
  Redis (TTL del token en blocklist = tiempo restante del access token).
- **FR-008**: El sistema DEBE bloquear temporalmente una cuenta después de N intentos
  de login fallidos consecutivos — **en alcance v1**. Los valores exactos de N (intentos)
  y duración del bloqueo se confirman en el backend (`AuthService.java`). El bloqueo
  retorna HTTP 423 con mensaje descriptivo. El evento DEBE registrarse en la tabla de
  auditoría con `accion = 'CUENTA_BLOQUEADA'`.
- **FR-009**: Los intentos de autenticación fallidos DEBEN registrarse en la tabla de
  auditoría con timestamp, IP y user-agent.
- **FR-010**: El acceso de terceros (padres, tutores) sin consentimiento explícito del
  alumno está PROHIBIDO — no existe endpoint para delegación de acceso en esta versión.

### Contratos de API *(actualizados — refreshToken y logout en alcance v1)*

| Método | Path | Auth | Body / Respuesta |
|---|---|---|---|
| `POST` | `/api/auth/login` | Público | `{ email, password }` → 200 `{ id, email, nombre, apellido, token, refreshToken, type, role, carrera? }` |
| `POST` | `/api/auth/refresh` | Público | `{ refreshToken }` → 200 `{ token }` / 401 si expirado o inválido |
| `POST` | `/api/auth/logout` | JWT (cualquier rol) | → 204; agrega el JWT a blocklist en Redis con TTL = tiempo restante del token |
| `GET` | `/api/auth/me` | JWT (cualquier rol) | → 200 `{ idUsuario, nombre, apellido, email, activo }` |
| `GET` | `/api/auth/jwt/inspect` | JWT (cualquier rol) | `?token=<jwt>` → 200 `JwtInspectResponse` (debugging) |
| `POST` | `/api/auth/olvide-password` | Público | `{ email }` → 200 `{ message }` |
| `GET` | `/api/admin/usuarios/solicitudes-reset` | `ADMINISTRADOR` | → 200 `[ SolicitudReset ]` |
| `POST` | `/api/admin/usuarios/solicitudes-reset/{id}/atender` | `ADMINISTRADOR` | `{ nuevaPassword }` → 200 `{ message }` |

### Key Entities *(corregidos en clarification)*

- **LoginResponse** (TypeScript: `auth.types.ts`): `id: number`, `email: string`,
  `nombre: string`, `apellido: string`, `token: string`, `refreshToken: string`,
  `type: string`, `role: Role`, `carrera?: string` (solo para ESTUDIANTE).
- **User** (de `GET /api/auth/me`): `idUsuario: number`, `nombre: string`,
  `apellido: string`, `email: string`, `activo: boolean`.
- **Role** (enum): `ESTUDIANTE` | `DOCENTE` | `ADMINISTRADOR` | `USUARIO`.
- **JWT Payload**: claim `rol` (string) = uno de los valores del enum `Role`. El
  frontend decodifica el payload Base64url para extraer el claim sin llamar a la BD.
- **SolicitudReset**: `idSolicitud`, `email`, `nombreCompleto?`, `fechaSolicitud`,
  `estado` (PENDIENTE | ATENDIDA), `fechaAtencion?`.
- **Usuario** (backend, para CRUD admin): `nombre`, `apellido`, `email`, `password`,
  `rol` (ESTUDIANTE | DOCENTE), `legajo?`, `carrera?`, `anioIngreso?`, `titulo?`,
  `especialidad?`.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Latencia P99 del endpoint `POST /api/auth/login` ≤ 300ms (sin Redis).
- **SC-002**: La validación de JWT en endpoints protegidos NO realiza ninguna consulta
  a MySQL (verificado con logs de Hibernate en modo debug).
- **SC-003**: El 100% de los endpoints que exponen datos de alumno rechazan requests
  de alumnos con `sub` diferente al recurso solicitado (verificado con suite de tests
  de autorización).
- **SC-004**: La blocklist de logout invalida el token en ≤ 100ms.

---

## Assumptions *(actualizados en clarification)*

- Las contraseñas se almacenan hasheadas con bcrypt en el backend.
- El `Login.tsx` del frontend ya existe y consume `POST /api/auth/login`.
- No existe SSO ni OAuth2 en esta versión — autenticación local únicamente.
- Los roles son mutuamente excluyentes: un usuario tiene exactamente un rol.
- La clave de firma JWT se almacena como variable de entorno (`JWT_SECRET`);
  en entornos de desarrollo se usa un valor de ejemplo definido en `.env.example`.
- **refreshToken y server-side logout están en alcance v1**. El `refreshToken`
  se incluye en la `LoginResponse`. El logout elimina `authUser` de localStorage
  **y** llama a `POST /api/auth/logout` para invalidar el token en la blocklist Redis.
- El `axiosInstance` maneja automáticamente 401, 403, 429, 503 redirigiendo a las
  rutas correspondientes — el frontend debe actualizar `authService.logout()` para
  llamar al endpoint server-side antes de limpiar localStorage.
- El `AuthContext.tsx` provee el estado de autenticación a toda la app React.
