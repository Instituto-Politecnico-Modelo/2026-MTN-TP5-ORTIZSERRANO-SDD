# Implementation Checklist: Gestión de Notas y Cierre de Actas

**Purpose**: Verificación de implementación, inmutabilidad de notas, auditoría y RBAC docente
**Created**: 2026-06-09
**Feature**: [spec.md](./spec.md)
**Branch**: `003-gestion-notas`

---

## Constitución — Compliance Gate *(verificar antes de merge — Principio III NO NEGOCIABLE)*

- [ ] CHK001 Principio III — `PATCH /api/docente/inscripciones/{id}/cerrar` hace que `notaCerrada = true` de forma permanente; no existe ningún endpoint que lo revierta sin proceso formal
- [ ] CHK002 Principio III — `PUT /api/docente/inscripciones/{id}/nota` retorna HTTP 409 si `notaCerrada = true` (verificado con test de regression — 100% de los casos)
- [ ] CHK003 Principio III — Cada `PUT` de nota genera exactamente un registro en tabla `auditoria` dentro de la misma transacción Hibernate
- [ ] CHK004 Principio III — Cada `PATCH .../cerrar` genera un registro de auditoría con `accion = 'ACTA_CERRADA'` (o equivalente)
- [ ] CHK005 Principio II — Un DOCENTE no puede cargar ni cerrar notas de materias que no le pertenecen → HTTP 403
- [ ] CHK006 Principio V — `GET /api/inscripciones/mis-notas` retorna solo las notas del ESTUDIANTE del JWT; nunca de otro estudiante

---

## Backend — Carga de notas

- [ ] CHK007 `PUT /api/docente/inscripciones/{idInscripcion}/nota` acepta `{ notaParcial1?, notaParcial2?, notaFinal?, asistencias? }` (todos opcionales)
- [ ] CHK008 El endpoint retorna HTTP 200 + `Inscripcion` actualizada cuando `notaCerrada = false`
- [ ] CHK009 El endpoint retorna HTTP 409 con mensaje descriptivo cuando `notaCerrada = true`
- [ ] CHK010 El endpoint retorna HTTP 400 si alguna nota enviada está fuera del rango válido [0, 10]
- [ ] CHK011 El endpoint retorna HTTP 403 si el DOCENTE del JWT no es titular de la materia de esa inscripción
- [ ] CHK012 El endpoint retorna HTTP 404 si `idInscripcion` no existe
- [ ] CHK013 El endpoint retorna HTTP 403 si el JWT no tiene rol `DOCENTE` o `ADMINISTRADOR`

---

## Backend — Cierre de nota (inmutabilidad)

- [ ] CHK014 `PATCH /api/docente/inscripciones/{idInscripcion}/cerrar` establece `notaCerrada = true` en la DB de forma atómica (transacción única que incluye INSERT en auditoría)
- [ ] CHK015 El endpoint retorna HTTP 200 + `Inscripcion` con `notaCerrada: true`
- [ ] CHK016 El endpoint retorna HTTP 409 si la nota ya estaba cerrada
- [ ] CHK017 El endpoint retorna HTTP 403 si el DOCENTE del JWT no es titular de la materia
- [ ] CHK018 Si el INSERT en auditoría falla durante el cierre, la transacción hace rollback completo (`notaCerrada` no queda en `true`)
- [ ] CHK019 No existe ningún endpoint `PATCH .../abrir` ni equivalente accesible por DOCENTE o ESTUDIANTE
- [ ] CHK019b `PATCH /api/admin/inscripciones/{id}/reabrir` con JWT de ADMINISTRADOR y `{ "motivo": "..." }` setea `notaCerrada = false` y genera registro de auditoría con `accion = 'NOTA_REABIERTA'` — **v1 firma única**
- [ ] CHK019c `PATCH .../reabrir` retorna HTTP 409 si la nota ya está abierta (`notaCerrada = false`)
- [ ] CHK019d `PATCH .../reabrir` retorna HTTP 403 para JWT con rol DOCENTE o ESTUDIANTE
- [ ] CHK019e El registro de auditoría de NOTA_REABIERTA incluye el `motivo` en el campo `descripcion`

---

## Backend — Consulta de inscripciones y notas

- [ ] CHK020 `GET /api/docente/materias/{idMateria}/inscripciones` retorna solo inscripciones de esa materia con estado CONFIRMADO (no CANCELADO ni ENCOLADO, salvo que sea requerimiento explícito)
- [ ] CHK021 El endpoint retorna HTTP 403 si el DOCENTE del JWT no es titular de la materia
- [ ] CHK022 El endpoint retorna HTTP 403 si el JWT tiene rol `ESTUDIANTE`
- [ ] CHK023 `GET /api/inscripciones/mis-notas` retorna todas las inscripciones del ESTUDIANTE del JWT incluyendo campos `notaParcial1`, `notaParcial2`, `notaFinal`, `asistencias`, `notaCerrada`
- [ ] CHK024 `GET /api/docente/estudiantes/{idEstudiante}/asistencias` retorna historial del estudiante accesible al DOCENTE
- [ ] CHK025 Un ADMINISTRADOR puede acceder a `GET /api/docente/materias/{idMateria}/inscripciones` de cualquier materia

---

## Backend — Auditoría de notas

- [ ] CHK026 Cada `PUT /api/docente/inscripciones/{id}/nota` genera un registro en tabla `auditoria` con:
  - `entidad = 'NOTA'` (o `'INSCRIPCION'`)
  - `idEntidad` = id de la inscripción
  - `accion = 'UPDATE'` (o `'INSERT'` si es la primera carga)
  - `descripcion` con valores anteriores y nuevos de las notas modificadas
  - `emailUsuario` del DOCENTE del JWT
  - `ipOrigen` del request
  - `hashActual` encadenado al `hashAnterior`
- [ ] CHK027 Cada `PATCH .../cerrar` genera un registro en tabla `auditoria` con `accion = 'ACTA_CERRADA'` y el `emailUsuario` del DOCENTE
- [ ] CHK028 Si hay múltiples updates de nota en la misma inscripción, la cadena de hashes es continua (cada `hashAnterior` apunta al `hashActual` del registro previo de esa entidad)

---

## Frontend — Integración

- [ ] CHK029 `GrillaNotas.tsx` consume `GET /api/docente/materias/{id}/inscripciones` (no el path del spec original)
- [ ] CHK030 `docenteService.cargarNota()` envía `PUT /api/docente/inscripciones/{id}/nota` con body `{ notaParcial1?, notaParcial2?, notaFinal?, asistencias? }`
- [ ] CHK031 `docenteService.cerrarNota()` envía `PATCH /api/docente/inscripciones/{id}/cerrar`
- [ ] CHK032 `GrillaNotas.tsx` deshabilita visualmente el formulario de carga de nota cuando `notaCerrada = true`
- [ ] CHK033 `Boletin.tsx` consume `GET /api/inscripciones/mis-notas` (no `/api/alumnos/mias/notas`)
- [ ] CHK034 `Boletin.tsx` solo es accesible para el rol `ESTUDIANTE` (protegido por `ProtectedRoute`)
- [ ] CHK035 `DashboardDocente.tsx` solo muestra materias del docente autenticado (no todas las materias del sistema)

---

## Testing

- [ ] CHK036 Test unitario: `PUT .../nota` con `notaCerrada = false` → HTTP 200 + nota actualizada en DB
- [ ] CHK037 Test unitario: `PUT .../nota` con `notaCerrada = true` → HTTP 409, nota sin cambio en DB
- [ ] CHK038 Test unitario: `PUT .../nota` con nota fuera de rango → HTTP 400
- [ ] CHK039 Test unitario: `PUT .../nota` de DOCENTE ajeno a la materia → HTTP 403
- [ ] CHK040 Test unitario: `PATCH .../cerrar` → `notaCerrada = true` en DB + registro de auditoría insertado
- [ ] CHK041 Test de integración: `PUT .../nota` seguido de `PATCH .../cerrar` seguido de otro `PUT .../nota` → el segundo PUT retorna HTTP 409
- [ ] CHK042 Test de integración: simular fallo en INSERT de auditoría durante `PATCH .../cerrar` → rollback completo; `notaCerrada` permanece en `false`
- [ ] CHK043 Test de seguridad: JWT de ESTUDIANTE accediendo a `PUT .../nota` → HTTP 403
- [ ] CHK044 Test de seguridad: JWT de DOCENTE en materia ajena → HTTP 403
- [ ] CHK045 Test de seguridad: JWT de ESTUDIANTE en `GET .../mis-notas` con sub distinto al id de la inscripción → HTTP 403
- [ ] CHK046 Test de regresión: script que ejecuta 100 `PUT .../nota` en inscripciones cerradas → 0 updates exitosos, 100 HTTP 409

---

## Deployment

- [ ] CHK047 La migración SQL que agrega el campo `nota_cerrada BOOLEAN NOT NULL DEFAULT FALSE` a la tabla `inscripciones` fue aplicada en producción
- [ ] CHK048 La migración SQL que crea la tabla `auditoria` (o verifica que existe con el schema correcto) fue aplicada
- [ ] CHK049 Los índices en `auditoria` sobre `(entidad, id_entidad)` y `(timestamp_evento)` están presentes en producción
- [ ] CHK050 No existe ningún script de migración de datos que setee `nota_cerrada = false` en registros existentes que deberían estar cerrados
