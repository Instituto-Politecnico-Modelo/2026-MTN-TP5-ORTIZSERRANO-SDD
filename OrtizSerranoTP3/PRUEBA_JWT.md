# 🧪 Prueba de JWT - Paso a Paso

## ✅ Endpoints Disponibles:

1. **POST /api/auth/signup** - Registrar nuevo usuario
2. **POST /api/auth/login** - Login y obtener token JWT
3. **GET /api/auth/me** - Ver info del usuario (requiere token)

---

## 🚀 Paso 1: Iniciar la Aplicación

```bash
cd /home/Alumno26.ORTIZ.Santiago@ipm.edu.ar/Escritorio/2026/TP3/TP3-docs-Serrano-Ortiz/OrtizSerranoTP3
./gradlew bootRun
```

Esperá a ver este mensaje:
```
Started OrtizSerranoTp3Application in X.XX seconds
Tomcat started on port 8080 (http) with context path '/'
```

---

## 🧪 Paso 2: Registrar un Usuario

Abrí una **nueva terminal** y ejecutá:

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Juan",
    "apellido": "Pérez",
    "email": "juan@test.com",
    "password": "123456"
  }'
```

**Respuesta esperada:**
```json
{"message":"Usuario registrado exitosamente"}
```

---

## 🔐 Paso 3: Hacer Login (Obtener Token JWT)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@test.com",
    "password": "123456"
  }'
```

**Respuesta esperada:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqdWFuQHRlc3QuY29tIiwiaWF0IjoxNzEzMDQzMjAwLCJleHAiOjE3MTMxMjk2MDB9.K8x_abc123...",
  "type": "Bearer",
  "id": 1,
  "email": "juan@test.com",
  "nombre": "Juan",
  "apellido": "Pérez"
}
```

**¡COPIÁ EL TOKEN!** (el texto largo que empieza con `eyJ...`)

---

## 🎯 Paso 4: Usar el Token (Validación JWT)

Reemplazá `TU_TOKEN_AQUI` con el token que copiaste:

```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer TU_TOKEN_AQUI"
```

**Respuesta esperada:**
```json
{
  "idUsuario": 1,
  "nombre": "Juan",
  "apellido": "Pérez",
  "email": "juan@test.com",
  "password": "$2a$10$hashed_password..."
}
```

---

## ✅ Pruebas de Validación

### ❌ Prueba 1: Sin Token (debe fallar)
```bash
curl -X GET http://localhost:8080/api/auth/me
```

**Respuesta esperada:** `401 Unauthorized`
```json
{"error": "No autorizado", "message": "Full authentication is required"}
```

### ❌ Prueba 2: Token Inválido (debe fallar)
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer token_falso_123"
```

**Respuesta esperada:** `401 Unauthorized`

### ❌ Prueba 3: Login con contraseña incorrecta (debe fallar)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@test.com",
    "password": "password_incorrecta"
  }'
```

**Respuesta esperada:** `401 Unauthorized`

---

## 🎨 Script Automático

O ejecutá directamente el script que creé:

```bash
./test-jwt.sh
```

---

## 📝 Resumen de lo que hace JWT:

1. ✅ **Registro**: Guarda usuario con contraseña encriptada (BCrypt)
2. ✅ **Login**: Valida credenciales y genera token JWT firmado
3. ✅ **Token**: Contiene email del usuario + fecha de expiración
4. ✅ **Validación**: Cada petición verifica que el token sea válido
5. ✅ **Autorización**: Si el token es válido, permite el acceso

---

## 🔍 Ver en la consola H2

Mientras la app está corriendo, abrí en el navegador:
```
http://localhost:8080/h2-console
```

**Credenciales:**
- JDBC URL: `jdbc:h2:mem:decanato_db`
- Usuario: `sa`
- Password: (vacío)

Ejecutá este query para ver el usuario registrado:
```sql
SELECT * FROM usuarios;
```

¡Verás que la contraseña está encriptada! 🔒

---

## 🎯 ¿Qué comprobar?

- ✅ El registro funciona
- ✅ El login devuelve un token JWT
- ✅ El token tiene 3 partes (header.payload.signature)
- ✅ Con el token podés acceder a `/api/auth/me`
- ✅ Sin token te devuelve 401
- ✅ Con token inválido te devuelve 401
- ✅ La contraseña está encriptada en la BD

¡Todo listo para probar! 🚀
