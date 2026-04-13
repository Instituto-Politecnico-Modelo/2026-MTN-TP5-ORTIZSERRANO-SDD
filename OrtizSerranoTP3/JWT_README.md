# 🔐 Sistema de Autenticación JWT

Este proyecto implementa autenticación y autorización con **JSON Web Tokens (JWT)** usando Spring Security.

## 📋 Componentes Implementados

### 1. **Seguridad JWT**
- ✅ `JwtUtil` - Generación y validación de tokens
- ✅ `JwtAuthenticationFilter` - Filtro para validar tokens en cada petición
- ✅ `JwtAuthenticationEntryPoint` - Manejo de errores 401

### 2. **Spring Security**
- ✅ `SecurityConfig` - Configuración de Spring Security
- ✅ `UserDetailsImpl` - Implementación de UserDetails
- ✅ `UserDetailsServiceImpl` - Servicio para cargar usuarios

### 3. **DTOs**
- ✅ `LoginRequest` - Petición de login
- ✅ `SignupRequest` - Petición de registro
- ✅ `JwtResponse` - Respuesta con token JWT
- ✅ `MessageResponse` - Respuestas de mensajes

### 4. **Controladores**
- ✅ `AuthController` - Endpoints de autenticación

## 🔑 Configuración JWT

En `application.properties`:

```properties
# Clave secreta para firmar tokens (cambiar en producción)
jwt.secret=tu_clave_secreta_muy_larga_y_segura

# Expiración del token en milisegundos (86400000 = 24 horas)
jwt.expiration=86400000
```

## 🚀 Endpoints de Autenticación

### 1. **Registro de Usuario**

**POST** `/api/auth/signup`

```json
{
  "nombre": "Juan",
  "apellido": "Pérez",
  "email": "juan.perez@example.com",
  "password": "password123"
}
```

**Respuesta exitosa:**
```json
{
  "message": "Usuario registrado exitosamente"
}
```

### 2. **Login**

**POST** `/api/auth/login`

```json
{
  "email": "juan.perez@example.com",
  "password": "password123"
}
```

**Respuesta exitosa:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "id": 1,
  "email": "juan.perez@example.com",
  "nombre": "Juan",
  "apellido": "Pérez"
}
```

### 3. **Obtener Usuario Actual**

**GET** `/api/auth/me`

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Respuesta exitosa:**
```json
{
  "idUsuario": 1,
  "nombre": "Juan",
  "apellido": "Pérez",
  "email": "juan.perez@example.com"
}
```

## 🔒 Cómo Usar el Token JWT

### En Postman/Insomnia:

1. Haz login en `/api/auth/login`
2. Copia el token de la respuesta
3. En las siguientes peticiones, añade el header:
   ```
   Authorization: Bearer <tu_token_aquí>
   ```

### En cURL:

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"juan@example.com","password":"password123"}'

# Usar el token
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

### En JavaScript (Frontend):

```javascript
// Login
const login = async (email, password) => {
  const response = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  const data = await response.json();
  
  // Guardar token
  localStorage.setItem('token', data.token);
  return data;
};

// Petición autenticada
const getUserInfo = async () => {
  const token = localStorage.getItem('token');
  const response = await fetch('http://localhost:8080/api/auth/me', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return await response.json();
};
```

## 🛡️ Rutas Protegidas

### Públicas (sin autenticación):
- `/api/auth/login`
- `/api/auth/signup`
- `/h2-console/**`

### Protegidas (requieren token):
- Todas las demás rutas (`/api/**`)

## 🔐 Validaciones Implementadas

### Login:
- ✅ Email válido y no vacío
- ✅ Contraseña no vacía

### Registro:
- ✅ Nombre: 2-100 caracteres
- ✅ Apellido: 2-100 caracteres
- ✅ Email: válido y único
- ✅ Contraseña: 6-40 caracteres

## 🧪 Testing con cURL

### 1. Registrar usuario:
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Test",
    "apellido": "Usuario",
    "email": "test@example.com",
    "password": "password123"
  }'
```

### 2. Login:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

### 3. Acceder a ruta protegida:
```bash
TOKEN="<token_del_paso_anterior>"
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

## ⚠️ Errores Comunes

### 401 Unauthorized
- Token inválido o expirado
- Token no proporcionado
- Usuario no autenticado

### 403 Forbidden
- Usuario autenticado pero sin permisos

### 400 Bad Request
- Email ya registrado
- Validaciones fallidas

## 🔧 Personalización

### Cambiar tiempo de expiración:
```properties
# 1 hora = 3600000
# 24 horas = 86400000
# 7 días = 604800000
jwt.expiration=86400000
```

### Generar nueva clave secreta:
```bash
# En Linux/Mac
openssl rand -base64 64

# O usar este sitio web:
# https://www.allkeysgenerator.com/Random/Security-Encryption-Key-Generator.aspx
```

## 📚 Flujo de Autenticación

1. **Usuario se registra** → `/api/auth/signup`
2. **Usuario hace login** → `/api/auth/login`
3. **Servidor genera JWT** y lo devuelve
4. **Cliente guarda el token** (localStorage, cookies, etc.)
5. **Cliente envía token** en cada petición: `Authorization: Bearer <token>`
6. **Servidor valida token** en `JwtAuthenticationFilter`
7. **Si es válido**: procesa la petición
8. **Si no es válido**: devuelve 401 Unauthorized

## 🎯 Próximos Pasos

- [ ] Implementar refresh tokens
- [ ] Agregar roles (ADMIN, USER, DOCENTE, etc.)
- [ ] Implementar logout
- [ ] Agregar lista negra de tokens
- [ ] Implementar 2FA (autenticación de dos factores)

¡El sistema de autenticación JWT está completamente funcional! 🚀
