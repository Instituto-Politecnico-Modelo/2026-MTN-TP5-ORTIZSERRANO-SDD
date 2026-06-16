# Analysis Report: Sistema de Inscripciones y Gestión Académica

**Date**: 2026-06-16
**Branch**: `004-auditoria`
**Scope**: Frontend TypeScript (React 18) + SDD artifacts (specs, plans, tasks, checklists)
**Backend**: Pendiente de implementación — no existe código backend en el workspace

---

## Executive Summary

El frontend está **parcialmente implementado** (estimado ~70% del contrato de API cubierto). Los servicios TypeScript modelan correctamente los endpoints especificados y las correcciones de los 5 gaps fueron aplicadas. Sin embargo, quedan **3 brechas activas** en componentes UI que no consumen las APIs actualizadas, y el **backend completo está pendiente** de construcción. El ciclo SDD (constitution → specify → clarify → checklist → plan → tasks) está **100% completo** y commitado.

---

## 1. SDD Artifacts — Estado del Ciclo

| Artefacto | Feature 001 | Feature 002 | Feature 003 | Feature 004 |
|---|---|---|---|---|
| `spec.md` | ✅ Clarified | ✅ Clarified | ✅ Clarified | ✅ Clarified |
| `checklist.md` | ✅ Synced | ✅ Synced | ✅ Synced | ✅ Synced |
| `plan.md` | ✅ | ✅ | ✅ | ✅ |
| `tasks.md` | ✅ | ✅ | ✅ | ✅ |

**Último commit SDD**: `0507e48` — "[Spec Kit] Add task lists — 001, 002, 003, 004"

---

## 2. Frontend — Análisis por Feature

### Feature 001 — Inscripción a Materias

#### ✅ Implementado

| Archivo | Elemento | Observación |
|---|---|---|
| `materia.service.ts` | `listarMaterias(carrera?)` | Llama a `GET /api/materias` ✅ |
| `materia.service.ts` | `misInscripciones()` | Llama a `GET /api/inscripciones/mis-inscripciones` ✅ |
| `materia.service.ts` | `inscribirse(idMateria)` | Llama a `POST /api/inscripciones` con body `{ idMateria }` ✅ |
| `materia.service.ts` | `cancelarInscripcion(id)` | Llama a `PATCH /api/inscripciones/{id}/cancelar` ✅ |
| `materia.service.ts` | `salirDeCola(id)` | Llama a `DELETE /api/inscripciones/{id}/cola` ✅ (gap fix) |
| `materia.service.ts` | `misNotas()` | Llama a `GET /api/inscripciones/mis-notas` ✅ |
| `MisInscripciones.tsx` | Estado `ENCOLADO` | Estilo visual amarillo + botón "Salir de la cola" ✅ (gap fix) |
| `MisInscripciones.tsx` | Dos botones distintos | "Cancelar inscripción" (CONFIRMADO) vs "Salir de la cola" (ENCOLADO) ✅ (gap fix) |
| `SalaDeEspera.tsx` | Polling `/api/health` | Activa en HTTP 429/503; countdown; reintentar ✅ |
| `axiosInstance.ts` | Redirect 429/503 | `?razon=ratelimit&espera=N&origen=...` ✅ |

#### ❌ Brechas

| ID | Archivo | Brecha | Tarea de referencia |
|---|---|---|---|
| GAP-001-A | `ListaMaterias.tsx` | No verificado si el `POST /inscripciones` distingue 201 vs 202 para mostrar confirmación vs "encolado" | T016 (tasks 001) |
| GAP-001-B | Backend | `GET /api/health` no existe todavía — `SalaDeEspera` fallará en polling | T045 (tasks 001) |

---

### Feature 002 — Autenticación JWT

#### ✅ Implementado

| Archivo | Elemento | Observación |
|---|---|---|
| `auth.types.ts` | `LoginResponse.refreshToken` | Campo presente ✅ (gap fix) |
| `auth.types.ts` | `Role` enum | `ESTUDIANTE`, `DOCENTE`, `ADMINISTRADOR`, `USUARIO` ✅ |
| `auth.service.ts` | `login(email, password)` | Llama a `POST /api/auth/login`; guarda en localStorage ✅ |
| `auth.service.ts` | `getCurrentUser()` | Llama a `GET /api/auth/me` ✅ |
| `auth.service.ts` | `inspectJwt(token)` | Llama a `GET /api/auth/jwt/inspect` ✅ |
| `auth.service.ts` | `olvidePassword(email)` | Llama a `POST /api/auth/olvide-password` ✅ |
| `auth.service.ts` | Decodificación JWT local | Extrae `rol` del payload Base64 sin consultar BD ✅ |
| `axiosInstance.ts` | Refresh-first en 401 | Intenta `POST /api/auth/refresh` antes de redirigir; cola de requests pendientes ✅ (gap fix) |
| `axiosInstance.ts` | Redirect 403 | `window.location.href = '/unauthorized'` ✅ |
| `AuthContext.tsx` | Auto-logout por expiración | Revisa JWT cada 60s; auto-logout por inactividad 30min ✅ |
| `AuthContext.tsx` | Idle warning | Aviso 60s antes del cierre por inactividad ✅ |
| `ProtectedRoute.tsx` | RBAC por rol | Redirige a `/unauthorized` si el rol no coincide ✅ |
| `Login.tsx` | `olvide-password` UI | Formulario integrado en el mismo Login ✅ |

#### ❌ Brechas

| ID | Archivo | Brecha | Severidad | Tarea de referencia |
|---|---|---|---|---|
| **GAP-002-A** | `auth.service.ts` línea 43 | `logout()` solo hace `localStorage.removeItem(USER_KEY)` — **NO llama a `POST /api/auth/logout`** para invalidar el token en la blocklist Redis. El token sigue siendo válido hasta que expire. Viola decisión 002-A. | 🔴 Alta | T045 (tasks 002) |
| **GAP-002-B** | `Login.tsx` | No maneja HTTP 423 (cuenta bloqueada). El error se mostrará como genérico. Falta mensaje "Tu cuenta está bloqueada por X minutos". | 🟡 Media | T033 (tasks 002) |
| GAP-002-C | Backend | Todo el stack JWT (`JwtUtil`, `JwtFilter`, `SecurityConfig`, `AuthService`) está pendiente de implementación. | 🔴 Bloqueante | T005–T012 (tasks 002) |

---

### Feature 003 — Gestión de Notas

#### ✅ Implementado

| Archivo | Elemento | Observación |
|---|---|---|
| `docente.service.ts` | `cargarNota(id, NotaRequest)` | Llama a `PUT /api/docente/inscripciones/{id}/nota` ✅ |
| `docente.service.ts` | `cerrarNota(id)` | Llama a `PATCH /api/docente/inscripciones/{id}/cerrar` ✅ |
| `docente.service.ts` | `alumnosPorMateria(id)` | Llama a `GET /api/docente/materias/{id}/inscripciones` ✅ |
| `docente.service.ts` | `historialAsistencias(idEstudiante)` | Llama a `GET /api/docente/estudiantes/{id}/asistencias` ✅ |
| `admin.service.ts` | `reabrir(id, motivo)` | Llama a `PATCH /api/admin/inscripciones/{id}/reabrir` ✅ (gap fix) |
| `GrillaNotas.tsx` | Formulario deshabilitado | Cuando `notaCerrada = true` todos los inputs son `disabled` ✅ |
| `GrillaNotas.tsx` | Botón "Cerrar" | Llama a `docenteService.cerrarNota()` con confirmación ✅ |
| `Boletin.tsx` | Boletín del alumno | Consume `GET /api/inscripciones/mis-notas` ✅ |

#### ❌ Brechas

| ID | Archivo | Brecha | Severidad | Tarea de referencia |
|---|---|---|---|---|
| **GAP-003-A** | `DashboardAdmin.tsx` | No hay UI para invocar `adminService.reabrir()`. El método existe en el service pero ningún componente lo llama. El admin no tiene botón de reapertura. | 🟡 Media | T030–T033 (tasks 003) |
| GAP-003-B | Backend | `NotaService`, `DocenteNotaController`, `AdminNotaController`, migración de columnas de notas — todo pendiente. | 🔴 Bloqueante | T003–T044 (tasks 003) |

---

### Feature 004 — Auditoría

#### ✅ Implementado

| Archivo | Elemento | Observación |
|---|---|---|
| `auditoria.service.ts` | `Page<T>` interface | `content`, `totalElements`, `totalPages`, `number`, `size`, `first`, `last` ✅ (gap fix) |
| `auditoria.service.ts` | `listarTodos(page, size)` | Retorna `Page<RegistroAuditoria>` con `?page=&size=` ✅ (gap fix) |
| `auditoria.service.ts` | `porEntidad()`, `porEntidadYId()`, `porUsuario()`, `porAccion()` | Todos presentes ✅ |
| `auditoria.service.ts` | `verificarIntegridad()` | Retorna `IntegridadResponse` con `integra` (no `integro`) ✅ |
| `VistaAuditoria.tsx` | Panel de integridad | `IntegridadPanel` con botón "Verificar", listado de errores ✅ |
| `VistaAuditoria.tsx` | Filtros por entidad y acción | Select con entidades y acciones ✅ |
| `VistaAuditoria.tsx` | Búsqueda | Por `emailUsuario`, `descripcion`, `entidad` ✅ |

#### ❌ Brechas

| ID | Archivo | Brecha | Severidad | Tarea de referencia |
|---|---|---|---|---|
| **GAP-004-A** | `VistaAuditoria.tsx` línea 176–192 | `cargar()` llama a `auditoriaService.listarTodos()` y hace `setRegistros([...data].reverse())`. Pero `data` ahora es `Page<RegistroAuditoria>` (no un array). Se debe usar `data.content`. Además falta manejar `totalPages`/`totalElements` para paginación real. **El componente está desincronizado con el tipo actualizado del servicio.** | 🔴 Alta | T041–T043 (tasks 004) |
| **GAP-004-B** | `VistaAuditoria.tsx` | `registros` se tipea como `RegistroAuditoria[]` pero el servicio retorna `Page<RegistroAuditoria>`. Sin paginación en UI, con 10k+ registros se pediría todo de una sola vez — viola el Principio I. | 🔴 Alta | T041 (tasks 004) |
| GAP-004-C | `VistaAuditoria.tsx` | Faltan controles de paginación: "Anterior", "Siguiente", indicador "Mostrando X–Y de Z" | 🟡 Media | T042 (tasks 004) |
| GAP-004-D | Backend | Tabla `auditoria`, `AuditoriaController`, `HashChainService`, `AuditoriaService.registrar()` — todo pendiente. | 🔴 Bloqueante | T003–T050 (tasks 004) |

---

### Cross-Cutting — `DashboardAdmin.tsx`

#### ❌ Brecha Crítica

| ID | Archivo | Brecha | Severidad |
|---|---|---|---|
| **GAP-CROSS-A** | `DashboardAdmin.tsx` líneas 6, 92, 100 | `import axios from 'axios'` y llamadas directas con `axios.get(API + '/admin/materias', { headers: headers() })`. **Bypasea completamente `axiosInstance`**. Estas llamadas no tienen los interceptores de 401 (refresh), 403, 429, 503. Si el token expira durante una operación en el dashboard admin, no se reintentará con el refreshToken — la operación fallará silenciosamente. | 🔴 Alta |

---

## 3. Backend — Estado (Pendiente)

El repositorio TP3 no contiene código backend aún. Toda la implementación está por comenzar. El orden recomendado por las tareas es:

```
004 (AuditoriaService — prerequisito) 
  → 002 (JWT + Spring Security — prerequisito de todos los endpoints)
    → 001 (Inscripciones + Cola Redis)
      → 003 (Notas + inmutabilidad)
```

**Entregables backend pendientes por feature:**

| Feature | Clases principales | Migración SQL |
|---|---|---|
| **004** | `HashChainService`, `AuditoriaService`, `AuditoriaController`, `RegistroAuditoria` | `CREATE TABLE auditoria` + 4 índices |
| **002** | `JwtUtil`, `JwtFilter`, `SecurityConfig`, `AuthService`, `TokenBlocklistService` | `ALTER TABLE usuarios` (intentos_fallidos, bloqueado_hasta) |
| **001** | `InscripcionService`, `ColaRedisService`, `InscripcionController` | `ALTER TABLE materias` (version), `inscripciones` (unique key) |
| **003** | `NotaService`, `DocenteNotaController`, `AdminNotaController` | `ALTER TABLE inscripciones` (nota_cerrada, notas, asistencias, checks) |

---

## 4. Risk Register

| ID | Riesgo | Impacto | Probabilidad | Mitigación |
|---|---|---|---|---|
| R-001 | `VistaAuditoria` usa `data` como array pero es `Page<T>` | 🔴 Runtime error en producción | Alta | **Inmediato**: actualizar `cargar()` para usar `data.content` |
| R-002 | `authService.logout()` no invalida token en servidor | 🔴 Token reutilizable post-logout | Alta | Agregar `POST /api/auth/logout` en `logout()` |
| R-003 | `DashboardAdmin.tsx` usa `axios` raw | 🟡 Refresh no funciona para admin | Media | Migrar a `adminService` usando `api` instance |
| R-004 | Backend no implementado — ningún feature funciona end-to-end | 🔴 Sistema no funcional | Certeza | Seguir orden `004→002→001→003` de tasks.md |
| R-005 | `Login.tsx` no maneja HTTP 423 | 🟡 UX degradada en cuenta bloqueada | Baja | Agregar manejo de `AppError.status === 423` |
| R-006 | Sin prueba de carga — constitución exige resultados antes de producción | 🔴 Incumplimiento Principio I | Alta | T046 (tasks 001) — k6/JMeter obligatorio |

---

## 5. Brechas Priorizadas para Acción Inmediata

### 🔴 Críticas (bloquean corrección o producción)

1. **GAP-004-A / GAP-004-B** — `VistaAuditoria.tsx`: actualizar `cargar()` para consumir `data.content` y agregar paginación UI
2. **GAP-002-A** — `auth.service.ts`: `logout()` debe llamar a `POST /api/auth/logout` antes de limpiar localStorage
3. **GAP-CROSS-A** — `DashboardAdmin.tsx`: migrar de `axios` raw a `api` (axiosInstance) o `adminService`

### 🟡 Medias (no bloquean MVP pero degradan UX/seguridad)

4. **GAP-003-A** — `DashboardAdmin.tsx`: agregar UI para `adminService.reabrir()` (botón en sección notas)
5. **GAP-002-B** — `Login.tsx`: manejar HTTP 423 con mensaje "Cuenta bloqueada por X minutos"
6. **GAP-004-C** — `VistaAuditoria.tsx`: controles de paginación ("Anterior"/"Siguiente"/indicador)

---

## 6. Métricas del Proyecto

| Métrica | Valor |
|---|---|
| Archivos frontend `.ts`/`.tsx` | 29 |
| Services implementados | 5 (`auth`, `materia`, `docente`, `admin`, `auditoria`) |
| Endpoints cubiertos en frontend | ~28 de ~32 especificados (~87%) |
| Brechas activas identificadas | 9 (3 críticas, 3 medias, 3 de backend bloqueante) |
| Commits SDD en rama `004-auditoria` | 8 desde `ab0902d` |
| Tareas de backend pendientes | ~117 (T001–T050 en 4 features) |
| Checklists items pendientes (`- [ ]`) | ~200+ distribuidos en 4 archivos |

---

## 7. Próximos Pasos Recomendados

```
Semana 1 (hoy):
  1. Corregir GAP-004-A: actualizar VistaAuditoria.tsx → data.content + paginación
  2. Corregir GAP-002-A: logout server-side en auth.service.ts
  3. Corregir GAP-CROSS-A: DashboardAdmin.tsx → migrar a api instance

Semana 1-2 (backend):
  4. Implementar Feature 004 backend (tabla auditoria + AuditoriaService)
  5. Implementar Feature 002 backend (JwtUtil + JwtFilter + SecurityConfig)

Semana 2-3 (backend):
  6. Implementar Feature 001 backend (InscripcionService + ColaRedisService)
  7. Implementar Feature 003 backend (NotaService + inmutabilidad)

Pre-producción (obligatorio por constitución):
  8. Prueba de carga k6: 50k usuarios simultáneos (R-006)
  9. Dataset 10k registros de auditoría → performance test SC-001 (004)
```
