# 🎨 Frontend - Sistema de Gestión Decanato

## 📋 Descripción

Aplicación web desarrollada con **React.js + TypeScript** que consume la API REST del backend con autenticación JWT.

---

## 🚀 Tecnologías Utilizadas

- **React 18** - Biblioteca de UI
- **TypeScript** - Tipado estático
- **React Router DOM** - Navegación
- **Axios** - Cliente HTTP
- **CSS3** - Estilos modernos con gradientes y animaciones

---

## 📦 Instalación

### 1. Instalar dependencias
```bash
cd frontend
npm install
```

### 2. Iniciar servidor de desarrollo
```bash
npm start
```

La aplicación se ejecutará en: **http://localhost:3000**

---

## 🏗️ Estructura del Proyecto

```
frontend/
├── src/
│   ├── components/
│   │   ├── Login.tsx          # Componente de Login
│   │   ├── Login.css          # Estilos del Login
│   │   ├── Dashboard.tsx      # Dashboard principal
│   │   └── Dashboard.css      # Estilos del Dashboard
│   ├── services/
│   │   └── auth.service.ts    # Servicio de autenticación
│   ├── types/
│   │   └── auth.types.ts      # Tipos TypeScript
│   ├── App.tsx                # Componente principal con rutas
│   └── App.css                # Estilos globales
```

---

## 🎯 Características Implementadas

### ✅ Pantalla de Login
- Diseño moderno con gradientes
- Validación de formularios
- Manejo de errores
- Animaciones suaves
- Indicador de carga
- Almacenamiento de JWT en localStorage

### ✅ Dashboard
- Visualización de información del usuario
- Tarjetas con módulos del sistema
- Avatar con iniciales
- Botón de cierre de sesión
- Diseño responsivo

### ✅ Rutas Protegidas
- Middleware de autenticación
- Redirección automática

---

## 🎨 Diseño UI/UX

### Paleta de Colores
- **Primario**: `#667eea` (Azul violeta)
- **Secundario**: `#764ba2` (Morado)
- **Fondo**: `#f5f7fa` (Gris claro)

---

## 🧪 Pruebas

### Probar Login
1. Backend en `http://localhost:8080`
2. Frontend: `npm start`
3. Credenciales de prueba:
   - Email: `maria.gonzalez@test.com`
   - Password: `password123`

---

**Desarrollado con**: React 18 + TypeScript + Axios  
**Estado**: ✅ COMPLETADO
