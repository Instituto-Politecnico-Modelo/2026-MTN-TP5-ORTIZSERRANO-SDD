# Implementation Checklist: Inscripción a Materias con Cola de Espera

**Purpose**: Verificación completa de implementación, seguridad, concurrencia y testing
**Created**: 2026-06-09
**Feature**: [spec.md](./spec.md)
**Branch**: `001-inscripcion-materias`

---

## Constitución — Compliance Gate *(verificar antes de merge)*

- [ ] CHK001 Principio I — Prueba de carga ejecutada con 50.000 usuarios simultáneos; resultados adjuntos como artefacto en la release
- [ ] CHK002 Principio I — Tasa de HTTP 500 = 0% bajo carga máxima (k6 / JMeter)
- [ ] CHK003 Principio I — Cupos negativos = 0 tras prueba de concurrencia con 10.000 requests al último cupo disponible
- [ ] CHK004 Principio II — Todos los endpoints de inscripción rechazan requests sin JWT → HTTP 401
- [ ] CHK005 Principio II — Endpoints de DOCENTE/ADMINISTRADOR rechazan JWT con rol ESTUDIANTE → HTTP 403
- [ ] CHK006 Principio III — Cada cambio de estado de inscripción genera un registro en tabla `auditoria` con `hashActual` correcto
- [ ] CHK007 Principio V — `GET /api/docente/materias/{id}/inscripciones` deniega acceso si el docente no es titular de la materia → HTTP 403

---

## Backend — API Contracts

- [ ] CHK008 `POST /api/inscripciones` acepta body `{ "idMateria": number }` (no `materiaId`)
- [ ] CHK009 `POST /api/inscripciones` retorna HTTP 201 + `Inscripcion` cuando hay cupo disponible
- [ ] CHK010 `POST /api/inscripciones` retorna HTTP 202 + `{ status: "ENCOLADO", position, estimated_wait_seconds }` cuando cupos = 0
- [ ] CHK011 `POST /api/inscripciones` retorna HTTP 409 ante inscripción duplicada del mismo ESTUDIANTE en la misma materia
- [ ] CHK012 `POST /api/inscripciones` retorna HTTP 403 fuera del período activo de inscripción
- [ ] CHK013 `POST /api/inscripciones` retorna HTTP 404 si `idMateria` no existe
- [ ] CHK014 `GET /api/inscripciones/mis-inscripciones` retorna solo las inscripciones del ESTUDIANTE del JWT (nunca de otro estudiante)
- [ ] CHK015 `GET /api/inscripciones/mis-notas` retorna inscripciones con campos `notaParcial1`, `notaParcial2`, `notaFinal`, `notaCerrada`
- [ ] CHK016 `PATCH /api/inscripciones/{id}/cancelar` retorna HTTP 200 + `Inscripcion` con estado CANCELADO
- [ ] CHK017 `PATCH /api/inscripciones/{id}/cancelar` retorna HTTP 403 si el período está cerrado
- [ ] CHK018 `PATCH /api/inscripciones/{id}/cancelar` retorna HTTP 403 si el `idInscripcion` no pertenece al ESTUDIANTE del JWT
- [ ] CHK019 `GET /api/inscripciones/{id}/estado` retorna `{ status, position?, estimated_wait_seconds? }` para inscripciones ENCOLADAS
- [ ] CHK020 `GET /api/docente/materias/{id}/inscripciones` retorna lista de `Inscripcion` con datos de `estudiante`
- [ ] CHK021 `GET /api/materias/disponibles` retorna solo materias con `hayLugar = true`
- [ ] CHK022 `GET /api/health` retorna HTTP 200 (usado por `SalaDeEspera` para polling)

---

## Backend — Concurrencia (Principio I — NO NEGOCIABLE)

- [ ] CHK023 Entidad `Materia` tiene anotación `@Version` en el campo `cuposDisponibles` (Hibernate optimistic locking)
- [ ] CHK024 El servicio de inscripción maneja `OptimisticLockingFailureException` reintentando o encolando, sin retornar HTTP 500
- [ ] CHK025 Redis está configurado como dependencia del servicio; existe un `bean` de `RedisTemplate` o `Lettuce` en el contexto Spring
- [ ] CHK026 La cola de espera usa una estructura ordenada en Redis (`RPUSH`/`LRANGE` o `ZADD`/`ZRANGE`) por `idMateria`
- [ ] CHK027 La promoción automática de ENCOLADO → CONFIRMADO se ejecuta dentro de una transacción atómica cuando un cupo se libera
- [ ] CHK028 El sistema devuelve HTTP 503 o HTTP 429 (no 500) cuando supera la capacidad máxima de threads; el `axiosInstance` del frontend lo maneja redirigiendo a `/sala-de-espera`
- [ ] CHK029 El constraint `cupos_disponibles >= 0` existe en la tabla MySQL `materias`
- [ ] CHK030 Fallback: si Redis no está disponible, las inscripciones se procesan sincrónicamente (sin caída del servicio)

---

## Backend — Auditoría

- [ ] CHK031 Cada `POST /api/inscripciones` exitoso (201 o 202) genera un registro de auditoría con `entidad = 'INSCRIPCION'`, `accion = 'INSERT'`
- [ ] CHK032 Cada `PATCH /api/inscripciones/{id}/cancelar` genera un registro de auditoría con `accion = 'UPDATE'`, descripción con estado anterior y nuevo
- [ ] CHK033 El registro de auditoría incluye `emailUsuario`, `ipOrigen`, `timestampEvento` y `hashActual` encadenado al `hashAnterior`
- [ ] CHK034 Si el INSERT en auditoría falla, la transacción completa hace rollback (no hay inscripciones sin registro de auditoría)

---

## Frontend — Integración

- [ ] CHK035 `MisInscripciones.tsx` llama a `GET /api/inscripciones/mis-inscripciones` (no `/mias`)
- [ ] CHK036 El servicio de inscripción usa `{ idMateria }` en el body del POST (no `{ materiaId }`)
- [ ] CHK037 La cancelación usa `PATCH /api/inscripciones/{id}/cancelar` (no DELETE)
- [ ] CHK038 `SalaDeEspera.tsx` se activa correctamente ante HTTP 429 y HTTP 503 interceptados por `axiosInstance`
- [ ] CHK039 `SalaDeEspera.tsx` hace polling a `GET /api/health` cada 5 segundos y habilita el botón "Reintentar" cuando el servidor responde OK
- [ ] CHK040 El componente de inscripción muestra estado ENCOLADO con la `position` retornada en el HTTP 202
- [ ] CHK041 `ProtectedRoute` permite acceso a rutas de inscripción solo al rol `ESTUDIANTE`

---

## Testing

- [ ] CHK042 Test unitario: inscripción exitosa con cupo disponible → HTTP 201, cupos decrementados en 1
- [ ] CHK043 Test unitario: inscripción duplicada → HTTP 409, cupos sin cambio
- [ ] CHK044 Test unitario: inscripción fuera de período → HTTP 403
- [ ] CHK045 Test de integración: dos threads simultáneos al último cupo → exactamente 1 HTTP 201 y 1 HTTP 202/409, cupos = 0 (no negativos)
- [ ] CHK046 Test de integración: cancelar inscripción CONFIRMADA → estado CANCELADO + cupo liberado + primer ENCOLADO promovido a CONFIRMADO
- [ ] CHK047 Test de integración: cancelar inscripción fuera de período → HTTP 403
- [ ] CHK048 Test de carga (k6/JMeter): 50.000 VU concurrentes, tasa de HTTP 500 = 0%, latencia P99 ≤ 2000ms — **resultado adjunto en release**
- [ ] CHK049 Test de carga: 10.000 requests simultáneos al último cupo → cupos_disponibles = 0, nunca negativo
- [ ] CHK050 Test de seguridad: endpoint sin JWT → HTTP 401; JWT de DOCENTE en endpoint de ESTUDIANTE → HTTP 403

---

## Deployment

- [ ] CHK051 Variable de entorno `REDIS_URL` (o equivalente) configurada en el entorno de producción
- [ ] CHK052 Redis healthcheck incluido en el `docker-compose` o manifest de deployment
- [ ] CHK053 Constraint `cupos_disponibles >= 0` verificado en la migración SQL aplicada en producción
- [ ] CHK054 Prueba de carga ejecutada **antes** de abrir el período de inscripción en producción (requerimiento constitución Principio I)
- [ ] CHK055 Endpoint `GET /api/health` funciona sin autenticación y retorna HTTP 200 en producción
