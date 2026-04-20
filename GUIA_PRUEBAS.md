# 🧪 GUÍA DE PRUEBAS - Sistema de Gestión Decanato

## ✅ Estado Actual

- ✅ **Backend**: Corriendo en http://localhost:8080
- ✅ **Frontend**: Corriendo en http://localhost:3000
- ✅ **Base de datos**: H2 en memoria (inicializada)

---

## 🎯 PRUEBA 1: Login desde el Frontend

### Pasos:

1. **Abre el navegador** en: http://localhost:3000

2. **Verás la pantalla de Login** con:
   - Campo de Email
   - Campo de Contraseña
   - Botón "Iniciar Sesión"
   - Diseño moderno con gradientes purple-blue

3. **Ingresa las credenciales de prueba:**
   - **Email**: `test@demo.com`
   - **Password**: `demo123`

4. **Click en "Iniciar Sesión"**

5. **Resultado esperado:**
   - ✅ Se muestra un spinner de carga
   - ✅ Redirección automática al Dashboard
   - ✅ Bienvenida personalizada: "¡Bienvenido, Test Usuario!"
   - ✅ Avatar con iniciales "TU"
   - ✅ Email mostrado: test@demo.com
   - ✅ Estado: "Usuario Activo" (con punto verde)
   - ✅ 4 tarjetas de módulos (Materias, Estudiantes, Inscripciones, Configuración)
   - ✅ Información del usuario en la sección inferior

---

## 🧪 PRUEBA 2: Validación de Errores

### Prueba 2.1: Credenciales Incorrectas

1. En el Login, ingresa:
   - Email: `test@demo.com`
   - Password: `wrongpassword` (incorrecta)

2. Click en "Iniciar Sesión"

3. **Resultado esperado:**
   - ❌ Mensaje de error: "Email o contraseña incorrectos"
   - ❌ No se redirige al Dashboard
   - ❌ Animación de "shake" en el mensaje de error

### Prueba 2.2: Campos Vacíos

1. Deja los campos vacíos
2. Click en "Iniciar Sesión"

3. **Resultado esperado:**
   - ❌ Validación del navegador (HTML5 required)
   - ❌ No se envía el formulario

### Prueba 2.3: Usuario Inactivo

1. Primero, desactiva el usuario con curl:
```bash
# Obtener token
TOKEN="eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0QGRlbW8uY29tIiwiaWF0IjoxNzc2MDkxNjM0LCJleHAiOjE3NzYxNzgwMzR9.O2cUW9wChffdvOpi3wRJIqw_KxCuMs8vfMrCMh_xiDb-c9dnOaCYPsD4uBe0VI2SHpUYzuA7amOyQzaarRNrpQ"

# Desactivar usuario
curl -X DELETE http://localhost:8080/api/auth/delete -H "Authorization: Bearer $TOKEN"
```

2. Intenta hacer login nuevamente con `test@demo.com`

3. **Resultado esperado:**
   - ❌ Error 401: "Email o contraseña incorrectos"
   - ❌ Usuario inactivo no puede acceder

---

## 🎨 PRUEBA 3: Características de UI/UX

### 3.1 Animaciones
- ✅ Hover sobre botón de login (se eleva)
- ✅ Spinner de carga al hacer submit
- ✅ Animación "slide up" al cargar la pantalla
- ✅ Círculos decorativos flotando en el fondo

### 3.2 Responsive Design
1. Abre las DevTools (F12)
2. Cambia al modo responsive
3. Prueba diferentes tamaños:
   - Mobile: 375px
   - Tablet: 768px
   - Desktop: 1920px

**Resultado esperado:**
- ✅ Layout se adapta perfectamente
- ✅ Botones y textos legibles en todas las resoluciones

### 3.3 Dashboard
1. Después de login exitoso, en el Dashboard:
   - ✅ Hover sobre las tarjetas (se elevan)
   - ✅ Click en "Cerrar Sesión" → Vuelve al Login
   - ✅ Intenta acceder a `/dashboard` sin login → Redirige a `/login`

---

## 🔧 PRUEBA 4: Flujo Completo desde cURL

Si prefieres probar desde la terminal:

```bash
# 1. Registrar usuario
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "María",
    "apellido": "García",
    "email": "maria@test.com",
    "password": "pass123"
  }'

# 2. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "maria@test.com",
    "password": "pass123"
  }'

# 3. Copiar el token de la respuesta y usarlo:
TOKEN="<token_del_paso_anterior>"

# 4. Obtener datos del usuario
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"

# 5. Actualizar datos
curl -X PUT http://localhost:8080/api/auth/update \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "nombre": "María Elena",
    "apellido": "García Pérez"
  }'

# 6. Verificar cambios
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

---

## 📱 PRUEBA 5: Persistencia de Sesión

1. Haz login en el frontend
2. Abre las DevTools → Application → Local Storage
3. Verás la clave `user` con los datos:
```json
{
  "id": 1,
  "email": "test@demo.com",
  "nombre": "Test",
  "apellido": "Usuario",
  "token": "eyJhbGci...",
  "type": "Bearer"
}
```

4. Refresca la página (F5)
5. **Resultado esperado:**
   - ✅ Sigues en el Dashboard (no te saca)
   - ✅ El token se mantiene en localStorage

6. Click en "Cerrar Sesión"
7. **Resultado esperado:**
   - ✅ localStorage se limpia
   - ✅ Redirige al Login
   - ✅ No puedes volver a `/dashboard` sin login

---

## 🎯 PRUEBA 6: Inspección de Network

1. Abre DevTools → Network
2. Haz login desde el frontend
3. Observa las peticiones HTTP:

**POST /api/auth/login**
- Request Payload:
```json
{
  "email": "test@demo.com",
  "password": "demo123"
}
```

- Response (200 OK):
```json
{
  "id": 1,
  "email": "test@demo.com",
  "nombre": "Test",
  "apellido": "Usuario",
  "token": "eyJhbGci...",
  "type": "Bearer"
}
```

**GET /api/auth/me**
- Request Headers:
```
Authorization: Bearer eyJhbGci...
```

- Response (200 OK):
```json
{
  "idUsuario": 1,
  "nombre": "Test",
  "apellido": "Usuario",
  "email": "test@demo.com",
  "activo": true
}
```

---

## 🔍 PRUEBA 7: Console Logs

1. Abre DevTools → Console
2. Haz login
3. Deberías ver:
```
Login exitoso: {id: 1, email: "test@demo.com", ...}
```

4. Si hay error:
```
Error en login: AxiosError {...}
```

---

## ⚡ PRUEBA 8: Estados de Carga

### 8.1 Loading State
1. Abre Network → Throttling → Slow 3G
2. Haz login
3. **Resultado esperado:**
   - ✅ Botón muestra "Iniciando sesión..."
   - ✅ Spinner girando
   - ✅ Botón deshabilitado durante la carga
   - ✅ Campos de input deshabilitados

### 8.2 Dashboard Loading
1. Después de login exitoso
2. **Resultado esperado:**
   - ✅ Spinner grande mientras carga datos
   - ✅ Texto "Cargando..."
   - ✅ Luego muestra el Dashboard completo

---

## 🎨 PRUEBA 9: Diseño Visual

### Verificar que se vea correctamente:

**Login:**
- ✅ Fondo con gradiente purple-blue
- ✅ 3 círculos decorativos flotando
- ✅ Tarjeta blanca centrada
- ✅ Título "Sistema de Gestión" + "Decanato"
- ✅ Iconos en los inputs (email y candado)
- ✅ Botón con gradiente y sombra
- ✅ Link "Registrarse" en el footer
- ✅ Versión "v1.0.0 - 2026"

**Dashboard:**
- ✅ Header con gradiente purple-blue
- ✅ Título "Sistema de Gestión Decanato"
- ✅ Botón "Cerrar Sesión" con icono
- ✅ Avatar circular con iniciales
- ✅ Nombre completo del usuario
- ✅ Badge verde "Usuario Activo"
- ✅ 4 tarjetas coloridas (azul, morado, verde, naranja)
- ✅ Iconos en cada tarjeta
- ✅ Sección de información del usuario

---

## 📊 CHECKLIST COMPLETO

Marca lo que funciona:

### Backend
- [ ] Servidor corriendo en puerto 8080
- [ ] Endpoint /signup funcional
- [ ] Endpoint /login genera JWT
- [ ] Endpoint /me requiere autenticación
- [ ] Contraseñas encriptadas con BCrypt
- [ ] Usuarios inactivos no pueden hacer login
- [ ] Base de datos H2 funcionando

### Frontend
- [ ] Servidor corriendo en puerto 3000
- [ ] Pantalla de Login se muestra correctamente
- [ ] Formulario de login funcional
- [ ] Validación de campos
- [ ] Mensajes de error mostrados
- [ ] Spinner de carga visible
- [ ] Redirección al Dashboard tras login
- [ ] Dashboard muestra datos del usuario
- [ ] Avatar con iniciales
- [ ] Botón de logout funciona
- [ ] Rutas protegidas funcionan
- [ ] localStorage guarda el token
- [ ] Diseño responsive
- [ ] Animaciones funcionando

### Integración
- [ ] Frontend se conecta al Backend
- [ ] CORS configurado correctamente
- [ ] JWT se envía en headers
- [ ] Errores HTTP manejados correctamente
- [ ] Sesión persiste al refrescar

---

## 🎉 RESULTADO ESPERADO FINAL

Al completar todas las pruebas, deberías tener:

✅ Sistema full-stack completamente funcional  
✅ Login con validaciones  
✅ Dashboard personalizado  
✅ Autenticación JWT robusta  
✅ UI/UX profesional  
✅ Manejo de errores completo  
✅ Persistencia de sesión  

---

## 📝 URLs Importantes

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api/auth
- **H2 Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:decanato_db`
  - User: `sa`
  - Password: (vacío)

---

## 🆘 Solución de Problemas

### Frontend no carga
```bash
cd frontend
npm start
```

### Backend no responde
```bash
cd OrtizSerranoTP3
./gradlew bootRun
```

### Error de CORS
- Verifica que el backend tenga `@CrossOrigin` en AuthController
- URL permitida: `http://localhost:3000`

### Token inválido
- El token expira en 24 horas
- Haz logout y login nuevamente

---

**¡Listo para probar! 🚀**

Abre http://localhost:3000 y comienza con el login usando:
- Email: `test@demo.com`
- Password: `demo123`
