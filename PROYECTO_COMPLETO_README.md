# 🎓 Sistema de Gestión Decanato - Proyecto Completo

## 📋 Resumen del Proyecto

Sistema full-stack de gestión universitaria con autenticación JWT, desarrollado con Spring Boot (Backend) y React + TypeScript (Frontend).

**Fecha de desarrollo**: Abril 2026  
**Estado**: ✅ COMPLETADO

---

## 🏗️ Arquitectura del Sistema

```
TP3-docs-Serrano-Ortiz/
├── OrtizSerranoTP3/          # Backend - Spring Boot
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/DecanatoOrtizSerrano/OrtizSerranoTP3/
│   │       │       ├── controller/      # REST Controllers
│   │       │       ├── model/           # Entidades JPA
│   │       │       ├── repository/      # Repositorios
│   │       │       ├── service/         # Lógica de negocio
│   │       │       ├── security/        # JWT + Spring Security
│   │       │       └── dto/             # Data Transfer Objects
│   │       └── resources/
│   │           └── application.properties
│   └── build.gradle
│
└── frontend/                  # Frontend - React + TypeScript
    ├── src/
    │   ├── components/        # Componentes React
    │   ├── services/          # Servicios API
    │   └── types/             # Tipos TypeScript
    └── package.json
```

---

## 🔧 Stack Tecnológico

### Backend
- **Java 21 LTS**
- **Spring Boot 4.0.5**
- **Spring Security** con JWT
- **JPA/Hibernate 7.2.7**
- **H2 Database** (en memoria)
- **Gradle 9.4.1**
- **JJWT 0.12.3** (JSON Web Tokens)

### Frontend
- **React 18**
- **TypeScript 4.9+**
- **React Router DOM 6**
- **Axios** (Cliente HTTP)
- **CSS3** (Animaciones y Gradientes)

---

## 📊 Modelo de Datos

### Entidades Principales

1. **Usuario** (Clase base)
   - idUsuario (PK)
   - nombre
   - apellido
   - email (único)
   - password (encriptado con BCrypt)
   - activo (Boolean)

2. **Estudiante** (extends Usuario)
   - legajo (único)
   - carrera
   - añoIngreso

3. **Docente** (extends Usuario)
   - titulo
   - departamento
   - especialidad

4. **Administrador** (extends Usuario)
   - rol
   - area
   - nivelAcceso

5. **Materia**
   - idMateria (PK)
   - codigo (único)
   - nombre
   - descripcion
   - año, cuatrimestre, créditos
   - docente (FK → Docente)

6. **Inscripción**
   - idInscripcion (PK)
   - estudiante (FK → Estudiante)
   - materia (FK → Materia)
   - fechaInscripcion
   - estado, notas, asistencias

### Estrategia de Herencia
- **JOINED**: Tabla separada para cada clase (Usuario, Estudiante, Docente, Administrador)

---

## 🔐 Sistema de Autenticación

### Flujo de Autenticación JWT

```
1. Usuario → POST /api/auth/signup → Registro (password → BCrypt)
2. Usuario → POST /api/auth/login → Validación
3. Backend → Genera JWT (HS512, 24h expiración)
4. Frontend → Guarda JWT en localStorage
5. Frontend → Incluye header: Authorization: Bearer {token}
6. Backend → JwtAuthenticationFilter valida token
7. Backend → Permite acceso si token válido
```

### Endpoints de Autenticación

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | `/api/auth/signup` | Registrar nuevo usuario | No |
| POST | `/api/auth/login` | Iniciar sesión (obtener JWT) | No |
| GET | `/api/auth/me` | Obtener info usuario autenticado | Sí |
| PUT | `/api/auth/update` | Actualizar datos del usuario | Sí |
| DELETE | `/api/auth/delete` | Baja lógica (desactivar) | Sí |
| DELETE | `/api/auth/delete/physical` | Baja física (eliminar de BD) | Sí |
| PUT | `/api/auth/reactivate/{id}` | Reactivar usuario | Sí |

---

## ✅ Funcionalidades Implementadas

### Backend

#### ✅ Autenticación y Seguridad
- Registro de usuarios con validación
- Login con generación de JWT
- Encriptación de contraseñas (BCrypt)
- Filtro de autenticación JWT
- Spring Security configurado
- Endpoints protegidos

#### ✅ CRUD de Usuarios
- **Create**: Registro con validaciones
- **Read**: Obtener info del usuario autenticado
- **Update**: Modificar nombre, apellido, email, password
- **Delete (Lógica)**: Desactivar usuario (activo = false)
- **Delete (Física)**: Eliminar permanentemente

#### ✅ Control de Usuarios Activos
- Campo `activo` en modelo Usuario
- Verificación en login (usuarios inactivos no pueden entrar)
- Endpoint de reactivación

#### ✅ Base de Datos
- 6 tablas creadas automáticamente con Hibernate
- Relaciones FK configuradas
- Constraints de unicidad (email, legajo, código materia)

### Frontend

#### ✅ Pantalla de Login
- Diseño moderno con gradientes
- Validación de formularios
- Manejo de errores detallado
- Indicador de carga (spinner)
- Animaciones suaves
- Iconos SVG integrados

#### ✅ Dashboard
- Visualización de datos del usuario
- Avatar con iniciales
- Estado activo/inactivo
- Tarjetas de módulos del sistema
- Botón de cierre de sesión
- Diseño totalmente responsivo

#### ✅ Navegación
- React Router DOM configurado
- Rutas protegidas con middleware
- Redirección automática si no hay sesión
- Persistencia de sesión con localStorage

#### ✅ Integración con API
- Servicio de autenticación (auth.service.ts)
- Cliente HTTP con Axios
- Tipos TypeScript para todas las respuestas
- Manejo de errores HTTP

---

## 🧪 Pruebas Realizadas

### Backend - Endpoints Probados

✅ **1. Registro de usuario**
```bash
POST /api/auth/signup
{
  "nombre": "María",
  "apellido": "González",
  "email": "maria.gonzalez@test.com",
  "password": "password123"
}
→ Respuesta: 200 OK
```

✅ **2. Login exitoso**
```bash
POST /api/auth/login
{
  "email": "maria.gonzalez@test.com",
  "password": "password123"
}
→ Respuesta: 200 OK + JWT Token
```

✅ **3. Acceso con token válido**
```bash
GET /api/auth/me
Authorization: Bearer eyJhbGci...
→ Respuesta: 200 OK + Datos del usuario
```

✅ **4. Actualización de datos**
```bash
PUT /api/auth/update
{
  "nombre": "María Elena",
  "apellido": "González Pérez"
}
→ Respuesta: 200 OK
```

✅ **5. Baja lógica**
```bash
DELETE /api/auth/delete
→ Respuesta: 200 OK (activo = false)
```

✅ **6. Login con usuario inactivo**
```bash
POST /api/auth/login (usuario inactivo)
→ Respuesta: 401 Unauthorized ❌
```

### Frontend - Escenarios Probados

✅ Login con credenciales correctas → Dashboard  
✅ Login con credenciales incorrectas → Error 401  
✅ Backend no disponible → Error de conexión  
✅ Campos vacíos → Validación de formulario  
✅ Logout → Limpia sesión y redirige  
✅ Acceso directo a /dashboard sin auth → Redirige a /login  

---

## 🚀 Cómo Ejecutar el Proyecto

### 1. Backend (Spring Boot)

```bash
cd OrtizSerranoTP3

# Compilar
./gradlew clean build -x test

# Ejecutar
./gradlew bootRun

# O ejecutar el JAR
java -jar build/libs/OrtizSerranoTP3-0.0.1-SNAPSHOT.jar
```

**URL Backend**: http://localhost:8080

### 2. Frontend (React)

```bash
cd frontend

# Instalar dependencias (primera vez)
npm install

# Iniciar servidor de desarrollo
npm start
```

**URL Frontend**: http://localhost:3000

### 3. Credenciales de Prueba

- **Email**: `maria.gonzalez@test.com`
- **Password**: `password123`

---

## 📁 Archivos Clave del Proyecto

### Backend

| Archivo | Descripción |
|---------|-------------|
| `Usuario.java` | Entidad base con campo `activo` |
| `AuthController.java` | Endpoints REST de autenticación |
| `UserService.java` | Lógica de negocio CRUD |
| `JwtUtil.java` | Generación y validación de JWT |
| `JwtAuthenticationFilter.java` | Filtro de Spring Security |
| `SecurityConfig.java` | Configuración de seguridad |
| `UserDetailsServiceImpl.java` | Carga de usuario y verificación activo |
| `UpdateUserRequest.java` | DTO para actualización |
| `application.properties` | Configuración H2, JWT, Hibernate |

### Frontend

| Archivo | Descripción |
|---------|-------------|
| `Login.tsx` | Componente de pantalla de Login |
| `Login.css` | Estilos modernos con gradientes |
| `Dashboard.tsx` | Dashboard principal |
| `Dashboard.css` | Estilos del dashboard |
| `auth.service.ts` | Servicio de autenticación |
| `auth.types.ts` | Tipos TypeScript |
| `App.tsx` | Rutas y componente principal |

### Documentación

| Archivo | Contenido |
|---------|-----------|
| `JWT_README.md` | Guía de JWT |
| `DATABASE_README.md` | Documentación de BD |
| `CRUD_USUARIOS_README.md` | Guía de CRUD |
| `RESULTADOS_PRUEBA_CRUD.md` | Resultados de pruebas |
| `FRONTEND_README.md` | Documentación del frontend |
| `test-crud.sh` | Script de pruebas automatizadas |

---

## 🎨 Características de Diseño

### UI/UX del Frontend

- **Gradientes**: Purple-Blue (#667eea → #764ba2)
- **Animaciones**: Hover effects, loading spinners
- **Responsive**: Mobile, Tablet, Desktop
- **Iconos**: SVG inline (sin dependencias externas)
- **Feedback visual**: Errores, carga, éxito
- **Accesibilidad**: Labels, contraste de colores

### Seguridad del Backend

- **Contraseñas**: BCrypt con salt automático
- **Tokens**: JWT HS512 con secret en base64
- **Expiración**: 24 horas
- **Headers**: Authorization: Bearer {token}
- **CORS**: Configurado para localhost:3000
- **Validaciones**: Bean Validation en DTOs

---

## 📈 Estadísticas del Proyecto

### Backend
- **Entidades**: 6 (Usuario, Estudiante, Docente, Administrador, Materia, Inscripción)
- **Endpoints**: 7 REST
- **Tablas**: 6 en H2
- **Clases Java**: 20+
- **Dependencias Gradle**: 8

### Frontend
- **Componentes React**: 3 (Login, Dashboard, App)
- **Servicios**: 1 (auth.service.ts)
- **Rutas**: 4 (/, /login, /dashboard, *)
- **Líneas de CSS**: ~600
- **Dependencias npm**: ~1300

---

## 🎯 Logros del Proyecto

✅ Sistema full-stack funcional  
✅ Autenticación JWT completa  
✅ CRUD de usuarios implementado  
✅ Frontend moderno con React + TypeScript  
✅ Backend robusto con Spring Boot  
✅ Base de datos auto-generada con Hibernate  
✅ Seguridad con Spring Security  
✅ Diseño responsivo  
✅ Manejo de errores completo  
✅ Documentación exhaustiva  
✅ Pruebas exitosas  

---

## 🚧 Mejoras Futuras

### Backend
- [ ] Implementar refresh tokens
- [ ] Agregar roles y permisos (ROLE_ADMIN, ROLE_USER)
- [ ] CRUD de Materias, Estudiantes, Docentes
- [ ] Sistema de inscripciones
- [ ] Paginación en listados
- [ ] Filtros y búsquedas
- [ ] Validaciones más robustas
- [ ] Base de datos PostgreSQL/MySQL en producción

### Frontend
- [ ] Página de registro
- [ ] Recuperación de contraseña
- [ ] Perfil de usuario editable
- [ ] Toast notifications (react-toastify)
- [ ] Tema oscuro
- [ ] Internacionalización (i18n)
- [ ] Tests unitarios (Jest)
- [ ] Tests E2E (Cypress)
- [ ] Módulos completos (Materias, Estudiantes, etc.)

---

## 👥 Desarrolladores

- **Ortiz, Santiago**
- **Serrano**

**Institución**: Instituto Politécnico Modelo  
**Curso**: Programación 3  
**Año**: 2026

---

## 📝 Conclusión

Este proyecto demuestra la implementación completa de un sistema de gestión universitaria con:

- ✅ **Arquitectura REST** bien estructurada
- ✅ **Seguridad robusta** con JWT y Spring Security
- ✅ **Frontend moderno** con React y TypeScript
- ✅ **Integración full-stack** funcional
- ✅ **CRUD completo** con bajas lógicas y físicas
- ✅ **Diseño UI/UX** profesional
- ✅ **Código limpio** y bien documentado

**Estado final**: ✅ **PROYECTO COMPLETADO Y FUNCIONAL**

---

*Última actualización: 13 de Abril de 2026*
