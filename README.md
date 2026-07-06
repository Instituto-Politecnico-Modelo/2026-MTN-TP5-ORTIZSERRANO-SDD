# 🎓 Sistema de Gestión de Decanato — IPM

> **TP5 — Materia: Metodología y Tecnología de Nuevos Sistemas**  
> Autores: Ortiz · Serrano  
> Stack: Spring Boot 4 · React 19 · MySQL 8 · JWT · Redis

---

## 📋 Tabla de Contenidos

1. [Descripción General](#-descripción-general)
2. [Arquitectura](#-arquitectura)
3. [Tecnologías](#-tecnologías)
4. [Requisitos Previos](#-requisitos-previos)
5. [Estructura del Proyecto](#-estructura-del-proyecto)
6. [Instalación y Ejecución (Local)](#-instalación-y-ejecución-local)
   - [Base de Datos](#1-base-de-datos-mysql)
   - [Backend](#2-backend-spring-boot)
   - [Frontend](#3-frontend-react)
7. [Usuarios por Defecto](#-usuarios-por-defecto)
8. [Funcionalidades por Rol](#-funcionalidades-por-rol)
9. [API REST — Endpoints Principales](#-api-rest--endpoints-principales)
10. [Seguridad](#-seguridad)
11. [Tests](#-tests)
12. [Ejecución con Docker](#-ejecución-con-docker)
13. [Variables de Entorno](#-variables-de-entorno)
14. [Diagrama de Base de Datos](#-diagrama-de-base-de-datos)
15. [Solución de Problemas](#-solución-de-problemas)

---

## 📖 Descripción General

Sistema web para la gestión académica del Decanato del Instituto Politécnico Modelo. Permite administrar la inscripción de estudiantes a materias, la carga de notas por parte de docentes y el control total de usuarios y auditoría por parte del administrador.

### Características Principales

- **Autenticación segura** con JWT (HS512), refresh token y bloqueo por intentos fallidos
- **Control de acceso** basado en roles (RBAC): Administrador, Docente, Estudiante
- **Inscripción a materias** con control de cupos, correlatividades y cola virtual de espera
- **Carga de notas** por docentes con cierre de actas y auditoría encadenada
- **Auditoría inmutable** de todas las operaciones con hash encadenado (integridad verificable)
- **Panel de administración** para gestión de usuarios, materias, períodos y estadísticas
- **Rate limiting** anti-fuerza-bruta por IP y bloqueo per-usuario

---

## 🏗 Arquitectura

```
┌─────────────────────┐        HTTP / REST        ┌──────────────────────────┐
│                     │ ◄───────────────────────► │                          │
│   Frontend React    │      JWT Bearer Token      │   Backend Spring Boot    │
│   Puerto: 3000      │                            │   Puerto: 8080           │
│                     │                            │                          │
└─────────────────────┘                            └────────────┬─────────────┘
                                                                │
                                              ┌─────────────────┴──────────────────┐
                                              │                                    │
                                    ┌─────────▼──────────┐          ┌─────────────▼──────────┐
                                    │   MySQL 8          │          │   Redis (opcional)     │
                                    │   Puerto: 3306     │          │   Puerto: 6379         │
                                    │   BD: Decanato-    │          │   Blocklist de tokens  │
                                    │   Ortiz-Serrano    │          │   revocados (logout)   │
                                    └────────────────────┘          └────────────────────────┘
```

**Patrón arquitectónico:** Capas (Controller → Service → Repository → Entity)  
**Comunicación:** API REST stateless con JWT en header `Authorization: Bearer <token>`  
**Sesión:** Sin estado de servidor (SessionCreationPolicy.STATELESS)

---

## 🛠 Tecnologías

### Backend
| Tecnología | Versión | Uso |
|-----------|---------|-----|
| Java | 21 (LTS) | Lenguaje principal |
| Spring Boot | 4.0.5 | Framework web |
| Spring Security | 7.x | Autenticación y autorización |
| Spring Data JPA | 4.x | Acceso a datos (ORM) |
| Hibernate | 7.2.7 | Implementación JPA |
| JJWT | 0.12.3 | Generación y validación de JWT |
| MySQL Connector/J | 8.x | Driver JDBC |
| Spring Data Redis | 4.x | Blocklist de tokens (logout) |
| Springdoc OpenAPI | 2.8.8 | Swagger UI |
| Gradle | 9.4.1 | Build tool |

### Frontend
| Tecnología | Versión | Uso |
|-----------|---------|-----|
| React | 19.2 | Framework UI |
| TypeScript | 4.9 | Tipado estático |
| React Router DOM | 7.14 | Routing SPA |
| Axios | 1.15 | Cliente HTTP |
| Create React App | 5.0 | Toolchain |

### Base de Datos
| Componente | Uso |
|-----------|-----|
| MySQL 8 | Base de datos de producción |
| H2 (in-memory) | Base de datos para tests automáticos |
| Redis | Blocklist de tokens revocados (opcional en desarrollo) |

---

## ✅ Requisitos Previos

Asegurarse de tener instalado:

```
✔ Java 21 (JDK)
✔ Node.js 18+ y npm
✔ MySQL 8
✔ Git
✔ Redis (opcional — el sistema funciona sin él en desarrollo)
```

Verificar instalaciones:
```bash
java -version      # debe mostrar 21.x
node -v            # debe mostrar 18.x o superior
mysql --version    # debe mostrar 8.x
```

---

## 📁 Estructura del Proyecto

```
2026-MTN-TP5-ORTIZSERRANO-SDD/
├── README.md                          ← Este archivo
├── REPORTE_AUDITORIA_SEGURIDAD.md     ← Reporte de pentest
├── DER.drawio.png                     ← Diagrama entidad-relación
├── diagramaClases.png                 ← Diagrama de clases
├── DriagramaCasosDeUso.drawio.png     ← Diagrama de casos de uso
│
├── frontend/                          ← Aplicación React
│   ├── package.json
│   ├── tsconfig.json
│   └── src/
│       ├── App.tsx                    ← Rutas y estructura principal
│       ├── api/
│       │   └── axiosInstance.ts       ← Interceptor JWT automático
│       ├── components/
│       │   ├── Login.tsx              ← Formulario de login
│       │   ├── DashboardAdmin.tsx     ← Panel administrador
│       │   ├── DashboardDocente.tsx   ← Panel docente
│       │   ├── DashboardEstudiante.tsx← Panel estudiante
│       │   ├── GrillaNotas.tsx        ← Carga de notas (docente)
│       │   ├── GestionUsuarios.tsx    ← ABM usuarios (admin)
│       │   ├── ListaMaterias.tsx      ← Catálogo de materias
│       │   ├── MisInscripciones.tsx   ← Inscripciones del estudiante
│       │   ├── Boletin.tsx            ← Boletín de notas
│       │   ├── VistaAuditoria.tsx     ← Log de auditoría (admin)
│       │   ├── SalaDeEspera.tsx       ← Cola virtual de inscripción
│       │   └── ProtectedRoute.tsx     ← Guard de rutas por rol
│       ├── context/
│       │   ├── AuthContext.tsx        ← Estado global de autenticación
│       │   └── ToastContext.tsx       ← Notificaciones toast
│       ├── services/
│       │   ├── auth.service.ts        ← Login, logout, refresh
│       │   ├── materia.service.ts     ← API materias
│       │   ├── docente.service.ts     ← API docente/notas
│       │   └── auditoria.service.ts   ← API auditoría
│       └── types/
│           └── auth.types.ts          ← Tipos TypeScript (Role, User, etc.)
│
└── OrtizSerranoTP3/
    └── backend/                       ← API Spring Boot
        ├── gradlew                    ← Wrapper de Gradle
        ├── build.gradle               ← Dependencias
        ├── Dockerfile.multi           ← Docker multi-stage (recomendado)
        ├── Dockerfile.single          ← Docker single-stage
        └── src/
            ├── main/
            │   ├── java/.../
            │   │   ├── controller/    ← Endpoints REST (17 controllers)
            │   │   ├── service/       ← Lógica de negocio
            │   │   ├── repository/    ← Acceso a datos (Spring Data JPA)
            │   │   ├── model/         ← Entidades JPA (19 entidades)
            │   │   ├── dto/           ← Objetos de transferencia
            │   │   ├── security/      ← JWT filter, UserDetails, config
            │   │   ├── exception/     ← Manejo global de errores
            │   │   └── config/        ← Configuración (CORS, debug)
            │   └── resources/
            │       └── application.properties  ← Configuración principal
            └── test/
                ├── java/              ← 26 clases de test (199 tests)
                └── resources/
                    └── application.properties  ← Config de tests (H2)
```

---

## 🚀 Instalación y Ejecución (Local)

### 1. Base de Datos (MySQL)

#### Crear la base de datos

```sql
-- Conectarse a MySQL como root
mysql -u root -p

-- Crear la base de datos
CREATE DATABASE IF NOT EXISTS `Decanato-Ortiz-Serrano`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Verificar
SHOW DATABASES;
```

#### Credenciales por defecto del sistema

| Parámetro | Valor por defecto |
|-----------|------------------|
| Host | `localhost:3306` |
| Base de datos | `Decanato-Ortiz-Serrano` |
| Usuario MySQL | `root` |
| Contraseña MySQL | `alumnoipm` |

> ⚠️ Si tu MySQL tiene credenciales diferentes, definir las variables de entorno `DB_USERNAME` y `DB_PASSWORD` antes de iniciar el backend (ver [Variables de Entorno](#-variables-de-entorno)).

#### Inicializar datos de ejemplo

Al iniciar el backend por primera vez, Hibernate crea las tablas automáticamente (`ddl-auto=update`). Los datos de ejemplo **deben cargarse manualmente** si la base de datos está vacía:

```bash
# Cargar datos de ejemplo (usuarios, materias, inscripciones)
mysql -u root -palumnoipm Decanato-Ortiz-Serrano \
  < OrtizSerranoTP3/backend/src/main/resources/data.sql
```

---

### 2. Backend (Spring Boot)

#### Opción A — Gradle wrapper (recomendado)

```bash
# Ir al directorio del backend
cd OrtizSerranoTP3/backend

# Dar permisos al wrapper (solo la primera vez en Linux/Mac)
chmod +x gradlew

# Iniciar el servidor
./gradlew bootRun --no-daemon
```

El servidor arranca en **http://localhost:8080**

#### Opción B — JAR compilado

```bash
cd OrtizSerranoTP3/backend

# Compilar (genera el jar en build/libs/)
./gradlew bootJar --no-daemon

# Ejecutar
java -jar build/libs/OrtizSerranoTP3-0.0.1-SNAPSHOT.jar
```

#### Verificar que el backend está corriendo

```bash
curl http://localhost:8080/api/health
# Respuesta esperada: {"status":"UP","timestamp":"..."}
```

#### Acceder a Swagger UI

Abrir en el navegador: **http://localhost:8080/swagger-ui/index.html**

---

### 3. Frontend (React)

```bash
# Ir al directorio del frontend
cd frontend

# Instalar dependencias (solo la primera vez)
npm install

# Iniciar servidor de desarrollo
npm start
```

La aplicación abre automáticamente en **http://localhost:3000**

> **Nota:** El frontend apunta al backend en `http://localhost:8080` por defecto. Si el backend corre en otro puerto, editar `frontend/src/api/axiosInstance.ts`.

---

## 👤 Usuarios por Defecto

Una vez cargados los datos de ejemplo (`data.sql`), los usuarios disponibles son:

| Email | Contraseña | Rol | Descripción |
|-------|-----------|-----|-------------|
| `admin@decanato.edu` | `Test1234` | ADMINISTRADOR | Acceso total al sistema |
| `carlos.rodriguez@docente.edu.ar` | `Test1234` | DOCENTE | Puede cargar notas de sus materias |
| `laura.fernandez@docente.edu.ar` | `Test1234` | DOCENTE | Puede cargar notas de sus materias |
| `juan.perez@estudiante.edu.ar` | `Test1234` | ESTUDIANTE | Puede inscribirse a materias y ver boletín |
| `maria.gonzalez@estudiante.edu.ar` | `Test1234` | ESTUDIANTE | Puede inscribirse a materias y ver boletín |

> **Si las contraseñas no funcionan**, ejecutar el siguiente comando para resetearlas:
> ```bash
> # Genera hash BCrypt de "Test1234" y lo aplica a todos los usuarios
> mysql -u root -palumnoipm -e \
>   "UPDATE \`Decanato-Ortiz-Serrano\`.usuarios SET password='\$2a\$10\$atIL5iTUdh2yC.bX4SWz/eipUH1mO5zhfLwcMbhldX3hidxkwErR.', intentos_fallidos=0, bloqueado_hasta=NULL;"
> ```

---

## 🎭 Funcionalidades por Rol

### 🔴 Administrador

| Funcionalidad | Descripción |
|--------------|-------------|
| **Gestión de usuarios** | Crear, editar, activar/desactivar estudiantes, docentes y administradores |
| **Gestión de materias** | Crear, editar y administrar el catálogo de materias |
| **Gestión de períodos** | Habilitar/deshabilitar períodos de inscripción |
| **Gestión de inscripciones** | Ver y modificar inscripciones de cualquier estudiante |
| **Reabrir notas** | Reabrir actas cerradas por docentes |
| **Vista de auditoría** | Ver el log completo de todas las operaciones del sistema con verificación de integridad hash |
| **Reset de contraseñas** | Atender solicitudes de reset de contraseña |

### 🟡 Docente

| Funcionalidad | Descripción |
|--------------|-------------|
| **Ver mis materias** | Ver las materias asignadas |
| **Cargar notas** | Cargar nota de parcial 1, parcial 2 y nota final para sus alumnos inscriptos |
| **Cerrar acta** | Cerrar el acta de una materia (impide modificaciones posteriores sin autorización admin) |
| **Ver historial** | Ver el historial de calificaciones de sus materias |

### 🟢 Estudiante

| Funcionalidad | Descripción |
|--------------|-------------|
| **Listar materias** | Ver el catálogo de materias disponibles con cupos y correlatividades |
| **Inscribirse** | Inscribirse a una materia (si hay cupo y cumple correlatividades) |
| **Cola virtual** | Si la materia no tiene cupos, entrar a la cola de espera automáticamente |
| **Mis inscripciones** | Ver el estado de todas sus inscripciones |
| **Boletín** | Ver sus notas finales y promedio general |
| **Perfil** | Editar datos personales y solicitar cambio de contraseña |

---

## 🌐 API REST — Endpoints Principales

> **Base URL:** `http://localhost:8080`  
> **Autenticación:** Header `Authorization: Bearer <access_token>`

### Autenticación (`/api/auth`)

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| `POST` | `/api/auth/login` | ❌ Pública | Iniciar sesión. Body: `{"email":"...","password":"..."}` |
| `POST` | `/api/auth/refresh` | ❌ Pública | Renovar access token. Body: `{"refreshToken":"..."}` |
| `POST` | `/api/auth/logout` | ✅ Token | Cerrar sesión (invalida token en Redis si disponible) |
| `GET` | `/api/auth/me` | ✅ Token | Datos del usuario autenticado |
| `POST` | `/api/auth/olvide-password` | ❌ Pública | Solicitar reset de contraseña |
| `GET` | `/api/auth/jwt/inspect` | ✅ Token | Inspeccionar claims del JWT actual |

**Respuesta de login exitoso:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "id": 1,
  "email": "admin@decanato.edu",
  "nombre": "Admin",
  "apellido": "Sistema",
  "role": "ADMINISTRADOR"
}
```

### Administración (`/api/admin`) — Solo ADMINISTRADOR

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/admin/usuarios` | Listar todos los usuarios |
| `POST` | `/api/admin/registrar` | Crear nuevo usuario |
| `PUT` | `/api/admin/usuarios/{id}` | Editar usuario |
| `DELETE` | `/api/admin/usuarios/{id}` | Desactivar usuario |
| `GET` | `/api/admin/materias` | Listar materias (con filtros) |
| `POST` | `/api/admin/materias` | Crear materia |
| `GET` | `/api/admin/inscripciones` | Ver todas las inscripciones |
| `GET` | `/api/admin/auditoria` | Log de auditoría paginado |
| `GET` | `/api/admin/auditoria/verificar` | Verificar integridad de hashes |
| `POST` | `/api/admin/notas/{id}/reabrir` | Reabrir acta cerrada |

### Docente (`/api/docente`) — DOCENTE o ADMINISTRADOR

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/docente/notas` | Ver notas de mis materias |
| `POST` | `/api/docente/notas` | Cargar nota para un alumno |
| `PUT` | `/api/docente/notas/{id}` | Actualizar nota |
| `POST` | `/api/docente/notas/{id}/cerrar` | Cerrar acta de materia |

### Inscripciones (`/api/inscripciones`) — ESTUDIANTE o ADMINISTRADOR

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/inscripciones` | Mis inscripciones |
| `POST` | `/api/inscripciones` | Inscribirse a una materia |
| `DELETE` | `/api/inscripciones/{id}` | Cancelar inscripción |
| `GET` | `/api/inscripciones/{id}/estado` | Estado de una inscripción |
| `DELETE` | `/api/inscripciones/{id}/cola` | Abandonar cola de espera |

### Materias Públicas (`/api/materias`)

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| `GET` | `/api/materias` | ✅ Token | Listar materias con cupos disponibles |
| `GET` | `/api/materias/{id}` | ✅ Token | Detalle de una materia |

### Health Check

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| `GET` | `/api/health` | ❌ Pública | Estado del servidor |

---

## 🔐 Seguridad

### Flujo de Autenticación

```
1. Cliente → POST /api/auth/login  {email, password}
2. Server  → Verifica BCrypt hash de la contraseña
3. Server  → Genera access token (JWT HS512, TTL 1 hora)
             + refresh token (JWT HS512, TTL 7 días)
4. Cliente → Almacena tokens en localStorage
5. Cliente → Incluye "Authorization: Bearer <token>" en cada request
6. Server  → JwtAuthenticationFilter valida firma + expiración en cada request
7. Server  → Extrae rol del claim "rol" y aplica @PreAuthorize
```

### Estructura del JWT

```json
// Header
{"alg": "HS512"}

// Payload (access token)
{
  "sub": "usuario@email.com",
  "rol": "ADMINISTRADOR",
  "id": 1,
  "iat": 1782221550,
  "exp": 1782225150
}
```

### Protecciones Implementadas

| Protección | Mecanismo |
|-----------|-----------|
| **Contraseñas** | BCrypt con factor de coste 10 |
| **Tokens** | HMAC-SHA512 — imposible de falsificar sin la clave secreta |
| **Algoritmo none** | Rechazado por JJWT (`parseSignedClaims` requiere firma) |
| **Escalación de rol** | Verificada server-side en cada endpoint (`@PreAuthorize`) |
| **Rate limiting** | Bloqueo por IP tras ~10 intentos fallidos (HTTP 429) |
| **Bloqueo de cuenta** | Bloqueo per-usuario tras 5 intentos fallidos (15 min) |
| **CORS** | Solo orígenes configurados en `app.cors.allowed-origins` |
| **CSRF** | No aplica — API stateless con JWT Bearer (no cookies) |
| **SQL Injection** | Imposible — JPA usa PreparedStatements parametrizados |
| **Auditoría** | Toda operación crítica genera un `RegistroAuditoria` con hash encadenado |
| **Logout** | Token agregado a blocklist Redis (si disponible) |

> Ver el reporte completo de auditoría de seguridad en `REPORTE_AUDITORIA_SEGURIDAD.md`

---

## 🧪 Tests

El proyecto incluye **199 tests automáticos** (0 fallos).

### Ejecutar todos los tests

```bash
cd OrtizSerranoTP3/backend

# Ejecutar tests (usa H2 en memoria — NO requiere MySQL)
./gradlew test --no-daemon

# Ver reporte HTML
# Abrir: build/reports/tests/test/index.html
```

### Clases de Test

| Clase | Tests | Descripción |
|-------|-------|-------------|
| `SecurityTest` | 24 | Tests completos de seguridad y RBAC |
| `TokenRoleSecurityTest` | 16 | Manipulación de JWT, alg:none, escalación |
| `InscripcionServiceTest` | 18 | Lógica de inscripción, cupos, correlatividades |
| `RestIntegrationTest` | 15 | Integración completa de endpoints |
| `SessionManagementTest` | 10 | Refresh token, logout, expiración |
| `AuditoriaServiceTest` | 10 | Cadena de hashes, integridad |
| `PerfilEditTest` | 10 | Edición de perfil de usuario |
| `DatabaseVolumeTest` | 10 | Carga masiva de datos |
| `NotaServiceTest` | 9 | Carga de notas, cierre de actas |
| `UsuarioServiceTest` | 9 | CRUD de usuarios |
| `JwtUtilTest` | 9 | Generación y validación de JWT |
| `AuthServiceLoginTest` | 7 | Login, bloqueo de cuenta |
| `ConcurrencyTest` | 5 | Race conditions en inscripciones |
| `RefreshTokenTest` | 5 | Ciclo completo de refresh |
| `JwtFilterTest` | 5 | Filtro JWT en requests |
| `AuthServiceRefreshTest` | 5 | Validación de refresh token |
| `AuthControllerLoginTest` | 5 | Rate limiting por IP |
| `LogoutBlocklistTest` | 4 | Invalidación post-logout |
| `AuthServiceAuditLoginTest` | 4 | Auditoría de login fallido |
| `RbacAuthorizationTest` | 12 | Tests por rol (Admin, Docente, Estudiante) |
| `AuthServiceBloqueoTest` | 3 | Bloqueo per-usuario |
| `OrtizSerranoTp3ApplicationTests` | 1 | Carga del contexto Spring |

---

## 🐳 Ejecución con Docker

### Backend solo

```bash
cd OrtizSerranoTP3/backend

# Construir imagen (multi-stage — recomendado)
docker build -f Dockerfile.multi -t decanato-backend .

# Ejecutar (conectar a MySQL en el host)
docker run -p 8080:8080 \
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/Decanato-Ortiz-Serrano?..." \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=alumnoipm \
  -e JWT_SECRET=TU_SECRETO_SEGURO \
  decanato-backend
```

### Stack completo con docker-compose

> Crear un archivo `docker-compose.yml` en la raíz:

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: alumnoipm
      MYSQL_DATABASE: Decanato-Ortiz-Serrano
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  backend:
    build:
      context: OrtizSerranoTP3/backend
      dockerfile: Dockerfile.multi
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:mysql://mysql:3306/Decanato-Ortiz-Serrano?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
      DB_USERNAME: root
      DB_PASSWORD: alumnoipm
      REDIS_HOST: redis
      JWT_SECRET: TU_SECRETO_SEGURO_MINIMO_512_BITS
    depends_on:
      - mysql
      - redis

  frontend:
    build:
      context: frontend
      dockerfile: Dockerfile.single
    ports:
      - "3000:80"
    depends_on:
      - backend

volumes:
  mysql_data:
```

```bash
docker compose up -d
```

---

## ⚙️ Variables de Entorno

Todas las variables tienen valores por defecto para desarrollo local. En producción, **siempre definir como variables de entorno reales** (nunca en archivos commiteados).

### Backend

| Variable | Default (dev) | Descripción |
|----------|--------------|-------------|
| `DB_URL` | `jdbc:mysql://localhost:3306/Decanato-Ortiz-Serrano?...` | URL de conexión MySQL |
| `DB_USERNAME` | `root` | Usuario MySQL |
| `DB_PASSWORD` | `alumnoipm` | Contraseña MySQL |
| `DDL_AUTO` | `update` | Estrategia DDL Hibernate. Usar `validate` en prod |
| `SHOW_SQL` | `true` | Loguear SQL. Usar `false` en prod |
| `JWT_SECRET` | *(clave hardcoded dev)* | **CAMBIAR EN PRODUCCIÓN** — mínimo 512 bits en Base64 |
| `JWT_EXPIRATION_MS` | `3600000` | TTL del access token (1 hora) |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` | TTL del refresh token (7 días) |
| `REDIS_HOST` | `localhost` | Host de Redis |
| `REDIS_PORT` | `6379` | Puerto de Redis |
| `REDIS_PASSWORD` | *(vacío)* | Contraseña de Redis |
| `AUTH_MAX_INTENTOS` | `5` | Intentos fallidos antes de bloqueo per-usuario |
| `AUTH_BLOQUEO_MINUTOS` | `15` | Minutos de bloqueo |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Orígenes CORS permitidos |
| `APP_URL` | `http://localhost:5173` | URL del frontend (para emails) |

### Ejemplo de archivo `.env` para producción

```bash
# .env (no commitear — está en .gitignore)
DB_URL=jdbc:mysql://db-server:3306/decanato?useSSL=true&serverTimezone=UTC
DB_USERNAME=decanato_user
DB_PASSWORD=contraseña_segura_larga

JWT_SECRET=<generado_con: openssl rand -base64 64>
JWT_EXPIRATION_MS=3600000
JWT_REFRESH_EXPIRATION_MS=604800000

REDIS_HOST=redis-server
REDIS_PORT=6379
REDIS_PASSWORD=contraseña_redis

APP_CORS_ALLOWED_ORIGINS=https://decanato.ipm.edu.ar
APP_URL=https://decanato.ipm.edu.ar

DDL_AUTO=validate
SHOW_SQL=false
```

---

## 🗄 Diagrama de Base de Datos

El DER completo está en `DER.drawio.png`. Las tablas principales son:

```
usuarios (padre)
    ├── estudiantes     (id_usuario FK)
    ├── docentes        (id_usuario FK)
    └── administradores (id_usuario FK)

materias
    ├── inscripciones       (id_estudiante FK, id_materia FK)
    ├── cola_inscripciones  (id_estudiante FK, id_materia FK)
    └── correlatividades    (id_materia FK, id_materia_requerida FK)

registros_auditoria
    (id_registro, entidad, id_entidad, accion, detalle,
     hash_registro, hash_anterior, timestamp_evento)
```

### Modelo de Herencia de Usuarios

El sistema usa **herencia de tabla unida** (JOINED inheritance) en JPA:

```
usuarios (tabla base)
  → estudiantes: agrega legajo, carrera, anio_ingreso
  → docentes: agrega titulo, especialidad, departamento
  → administradores: agrega rol, area, nivel_acceso
```

---

## ❓ Solución de Problemas

### ❌ "Credenciales inválidas" en el login

Las contraseñas en la base de datos están hasheadas con BCrypt. Si se importaron datos manualmente con contraseñas en texto plano, hacer:

```bash
# Resetear TODAS las contraseñas a "Test1234"
mysql -u root -palumnoipm -e \
  "UPDATE \`Decanato-Ortiz-Serrano\`.usuarios SET \
   password='\$2a\$10\$atIL5iTUdh2yC.bX4SWz/eipUH1mO5zhfLwcMbhldX3hidxkwErR.', \
   intentos_fallidos=0, \
   bloqueado_hasta=NULL;"
```

### ❌ "No existe el archivo o el directorio" al ejecutar `./gradlew`

```bash
# Usar bash explícitamente con ruta absoluta
cd /ruta/completa/al/backend
bash gradlew bootRun --no-daemon
```

### ❌ Puerto 8080 ya en uso

```bash
# Ver qué proceso usa el puerto
lsof -i :8080

# Matar el proceso
kill -9 <PID>
```

### ❌ Error de conexión a MySQL

```bash
# Verificar que MySQL está corriendo
sudo systemctl status mysql

# Verificar que la BD existe
mysql -u root -p -e "SHOW DATABASES;"

# Si la BD no existe, crearla
mysql -u root -p -e "CREATE DATABASE \`Decanato-Ortiz-Serrano\`;"
```

### ❌ La cuenta está bloqueada (HTTP 423)

```bash
# Desbloquear usuario específico
mysql -u root -palumnoipm -e \
  "UPDATE \`Decanato-Ortiz-Serrano\`.usuarios SET intentos_fallidos=0, bloqueado_hasta=NULL WHERE email='usuario@email.com';"
```

### ❌ Rate limiting activado (HTTP 429)

El rate limiter por IP se resetea automáticamente luego del tiempo de bloqueo (configurable en `AUTH_BLOQUEO_MINUTOS`, default 15 min). Para reseteo inmediato en desarrollo, reiniciar el backend.

### ❌ Redis no disponible

El sistema funciona **sin Redis** en desarrollo. La única funcionalidad afectada es que el logout no invalida inmediatamente el token (el token sigue siendo válido hasta su expiración de 1 hora). Redis es recomendado en producción.

---

## 📊 Estado del Proyecto

| Métrica | Valor |
|---------|-------|
| Tests automáticos | **199 tests** |
| Fallos | **0** |
| Issues GitHub cerrados | **29 / 29** |
| Endpoints REST | **~60 endpoints** |
| Pruebas de seguridad (pentest) | **48 / 48 PASS** |
| Vulnerabilidades encontradas | **0** |

---

## 📄 Licencia

Trabajo Práctico Académico — Instituto Politécnico Modelo, 2026.  
Todos los derechos reservados a sus autores.
