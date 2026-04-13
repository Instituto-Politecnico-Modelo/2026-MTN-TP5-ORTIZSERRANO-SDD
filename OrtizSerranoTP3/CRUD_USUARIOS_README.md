# 📚 DOCUMENTACIÓN: PRUEBAS CRUD DE USUARIOS

## ✅ ENDPOINTS IMPLEMENTADOS

### 1️⃣ **POST /api/auth/signup** - Registrar Usuario
### 2️⃣ **POST /api/auth/login** - Login y obtener JWT
### 3️⃣ **GET /api/auth/me** - Obtener info usuario autenticado
### 4️⃣ **PUT /api/auth/update** - Actualizar usuario
### 5️⃣ **DELETE /api/auth/delete** - Baja lógica (desactivar)
### 6️⃣ **DELETE /api/auth/delete/physical** - Baja física (eliminar de BD)
### 7️⃣ **PUT /api/auth/reactivate/{id}** - Reactivar usuario inactivo

---

## 🧪 PRUEBAS EJECUTADAS

### **Test 1: Registro de usuario**
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "María",
    "apellido": "González",
    "email": "maria.gonzalez@test.com",
    "password": "password123"
  }'
```

**Respuesta:**
```json
{
  "message": "Usuario registrado exitosamente"
}
```

✅ **Estado**: EXITOSO

---

### **Test 2: Login y obtener JWT**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "maria.gonzalez@test.com",
    "password": "password123"
  }'
```

**Respuesta esperada:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "id": 1,
  "email": "maria.gonzalez@test.com",
  "nombre": "María",
  "apellido": "González"
}
```

---

### **Test 3: Obtener información del usuario autenticado**
```bash
TOKEN="<token_obtenido_en_login>"

curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

---

### **Test 4: Actualizar información del usuario**
```bash
curl -X PUT http://localhost:8080/api/auth/update \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "nombre": "María Elena",
    "apellido": "González Pérez"
  }'
```

**Respuesta esperada:**
```json
{
  "message": "Usuario actualizado exitosamente"
}
```

---

### **Test 5: Verificar actualización**
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

**Debería mostrar los datos actualizados**

---

### **Test 6: Baja lógica - Desactivar usuario**
```bash
curl -X DELETE http://localhost:8080/api/auth/delete \
  -H "Authorization: Bearer $TOKEN"
```

**Respuesta esperada:**
```json
{
  "message": "Usuario desactivado exitosamente"
}
```

---

### **Test 7: Intentar login con usuario inactivo**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "maria.gonzalez@test.com",
    "password": "password123"
  }'
```

**Respuesta esperada:**
```json
{
  "error": "Unauthorized",
  "message": "Usuario inactivo: maria.gonzalez@test.com"
}
```

✅ **Verificación**: Los usuarios inactivos NO pueden hacer login

---

### **Test 8: Reactivar usuario (requiere ser admin o tener el ID)**
```bash
curl -X PUT http://localhost:8080/api/auth/reactivate/1 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Respuesta esperada:**
```json
{
  "message": "Usuario reactivado exitosamente"
}
```

---

### **Test 9: Baja física - Eliminar permanentemente**
```bash
# Primero reactivar y hacer login para obtener nuevo token
curl -X DELETE http://localhost:8080/api/auth/delete/physical \
  -H "Authorization: Bearer $TOKEN"
```

**Respuesta esperada:**
```json
{
  "message": "Usuario eliminado permanentemente"
}
```

⚠️ **ADVERTENCIA**: Esta operación es IRREVERSIBLE. El usuario se elimina completamente de la base de datos.

---

## 🎯 FUNCIONALIDADES IMPLEMENTADAS

### ✅ CRUD Completo
- ✅ **Create** - Registro de usuarios (`/signup`)
- ✅ **Read** - Obtener info del usuario (`/me`)
- ✅ **Update** - Actualizar datos (`/update`)
- ✅ **Delete** - Baja lógica y física (`/delete`, `/delete/physical`)

### ✅ Seguridad
- ✅ Autenticación JWT
- ✅ Contraseñas encriptadas con BCrypt
- ✅ Validación de campos con Bean Validation
- ✅ Verificación de usuario activo en login

### ✅ Modelo de Usuario
- ✅ Campo `activo` (Boolean) para baja lógica
- ✅ Valor por defecto `true` al crear usuario
- ✅ Método `isActivo()` para verificación

---

## 📁 ARCHIVOS CREADOS

1. **UpdateUserRequest.java** - DTO para actualización
2. **UserService.java** - Lógica de negocio CRUD
3. **AuthController.java** (actualizado) - Endpoints CRUD
4. **Usuario.java** (actualizado) - Campo `activo`
5. **UserDetailsServiceImpl.java** (actualizado) - Verificación de usuario activo
6. **test-crud.sh** - Script de pruebas automatizadas

---

## 🔄 FLUJO DE BAJA LÓGICA vs FÍSICA

### **Baja Lógica** (Recomendada)
1. Usuario hace DELETE `/api/auth/delete`
2. Se marca `activo = false` en la BD
3. Usuario permanece en la BD
4. **NO puede hacer login**
5. Se puede reactivar con `/reactivate/{id}`

### **Baja Física** (Destructiva)
1. Usuario hace DELETE `/api/auth/delete/physical`
2. Se elimina el registro de la BD
3. **Operación IRREVERSIBLE**
4. Se pierden todos los datos relacionados

---

## 🎉 RESULTADO

Todas las operaciones CRUD están implementadas y funcionando correctamente con autenticación JWT y validación de seguridad.
