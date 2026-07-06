# Implementation Plan: Gestión de Notas y Cierre de Actas

**Branch**: `003-gestion-notas` | **Date**: 2026-06-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification — "Gestión de notas y cierre de actas con inmutabilidad"

## Summary

Cada inscripción tiene sus propias notas (`notaParcial1`, `notaParcial2`, `notaFinal`, `asistencias` [0–100]%) y una bandera `notaCerrada: boolean`. Un docente puede cargar y actualizar notas mientras `notaCerrada = false`. Al ejecutar `PATCH .../cerrar`, `notaCerrada` se setea a `true` de forma atómica con el INSERT en la tabla de auditoría (misma transacción Hibernate). Una vez cerrada, ninguna operación de la API puede modificar las notas. En v1, un ADMINISTRADOR puede reabrir con motivo obligatorio vía `PATCH /api/admin/inscripciones/{id}/reabrir`. El alumno visualiza su boletín vía `Boletin.tsx` consumiendo `GET /api/inscripciones/mis-notas`.

## Technical Context

**Language/Version**: Java 17 (backend) / TypeScript 5 (frontend)

**Primary Dependencies**:
- Backend: Spring Boot 3, Hibernate 6 (transacciones JPA), Spring Security 6
- Frontend: React 18, Axios, React Router 6

**Storage**: MySQL 8 — columna `nota_cerrada BOOLEAN NOT NULL DEFAULT FALSE` en tabla `inscripciones`; tabla `auditoria` compartida (ver feature 004)

**Testing**: JUnit 5 + Mockito + MockMvc (backend) / Jest + React Testing Library (frontend)

**Target Platform**: Linux server (backend Spring Boot JAR) + Web browser (React SPA)

**Project Type**: Web application — `backend/` + `frontend/`

**Performance Goals**:
- `PUT /api/docente/inscripciones/{id}/nota` < 200ms p95 bajo carga normal
- Atomicidad nota + auditoría: ningún escenario puede persistir una nota sin su registro de auditoría

**Constraints**:
- La transacción de cierre (`notaCerrada = true` + INSERT auditoría) es ATÓMICA — si falla el INSERT de auditoría, la nota NO se cierra
- `notaCerrada = true` es IRREVERSIBLE por la API normal del docente; solo ADMINISTRADOR puede reabrir con motivo
- `asistencias` DEBE ser un entero en rango [0, 100]; notas en rango [0.0, 10.0]
- No existe entidad "Acta" en la API v1 — el cierre es por inscripción individual

**Scale/Scope**: ~500 materias × ~200 alumnos = ~100.000 inscripciones con notas por período

## Constitution Check

| Principio | Gate | Estado |
|---|---|---|
| **I. Alta Concurrencia** | Múltiples docentes cargando notas simultáneamente; transacciones Hibernate correctas | ✅ cumplido — no requiere Redis; las transacciones MySQL son suficientes para la escala de notas |
| **II. JWT Auth** | Solo el docente de la materia puede modificar notas; ADMINISTRADOR puede reabrir | ✅ cumplido — `@PreAuthorize` valida `sub` del JWT contra `idDocente` de la materia |
| **III. Inmutabilidad/Auditoría** | Esta feature IS el Principio III | ✅ NÚCLEO — `notaCerrada = true` + audit INSERT en misma transacción |
| **IV. Stack Definido** | Spring Boot 3 + Hibernate 6 + MySQL 8 + React 18 | ✅ cumplido |
| **V. Privacidad Ley 25.326** | Notas son datos personales del alumno; `GET /mis-notas` solo retorna datos del `sub` del JWT | ✅ cumplido — HTTP 403 si `sub` no coincide |
| **VI. Disponibilidad** | Backups retienen historial completo; 5 años mínimo | ✅ cumplido — política de backups del Principio VI aplica |

## Project Structure

### Documentation (esta feature)

```text
.specify/memory/003-gestion-notas/
├── spec.md            ✅ Clarified
├── plan.md            ← este archivo
└── checklist.md       ✅ generado con 50+ ítems CHK
```

### Source Code

```text
backend/
├── src/main/java/com/ipm/notas/
│   ├── model/
│   │   └── Inscripcion.java          # nota_cerrada boolean; notaParcial1/2, notaFinal, asistencias
│   ├── repository/
│   │   └── InscripcionRepository.java
│   ├── service/
│   │   ├── NotaService.java          # cargarNota(), cerrarNota(), reabrir()
│   │   └── AuditoriaService.java     # insertarRegistro() — llamado dentro de la misma transacción
│   ├── controller/
│   │   ├── DocenteNotaController.java  # PUT /nota, PATCH /cerrar, GET /inscripciones
│   │   ├── AdminNotaController.java    # PATCH /reabrir
│   │   └── EstudianteNotaController.java  # GET /mis-notas
│   └── dto/
│       ├── NotaRequest.java          # { notaParcial1?, notaParcial2?, notaFinal?, asistencias? }
│       └── InscripcionResponse.java
└── src/test/
    ├── unit/NotaServiceTest.java
    ├── integration/NotaCierreAtomicoTest.java
    └── integration/NotaReaperturaTest.java

frontend/
├── src/
│   ├── services/
│   │   ├── docente.service.ts        # cargarNota(), cerrarNota()
│   │   └── admin.service.ts          ✅ actualizado — reabrir(id, motivo)
│   └── components/
│       ├── GrillaNotas.tsx           # deshabilita formulario cuando notaCerrada=true
│       └── Boletin.tsx               # GET /api/inscripciones/mis-notas, solo ESTUDIANTE
```

**Structure Decision**: Web application (Option 2). Backend y frontend en el mismo repositorio TP3.

## API Contracts

| Método | Path | Rol JWT | Request Body | Response |
|---|---|---|---|---|
| `GET` | `/api/docente/materias/{id}/inscripciones` | `DOCENTE` | — | 200 `Inscripcion[]` |
| `PUT` | `/api/docente/inscripciones/{id}/nota` | `DOCENTE` | `{ notaParcial1?, notaParcial2?, notaFinal?, asistencias? }` | 200 `Inscripcion` \| 409 (cerrada) |
| `PATCH` | `/api/docente/inscripciones/{id}/cerrar` | `DOCENTE` | — | 200 `Inscripcion` |
| `PATCH` | `/api/admin/inscripciones/{id}/reabrir` | `ADMINISTRADOR` | `{ motivo: string }` | 200 `Inscripcion` \| 409 (ya abierta) \| 400 (sin motivo) |
| `GET` | `/api/inscripciones/mis-notas` | `ESTUDIANTE` | — | 200 `Inscripcion[]` |

## Data Model

```sql
-- Columnas agregadas a inscripciones (migration)
ALTER TABLE inscripciones
  ADD COLUMN nota_cerrada   BOOLEAN     NOT NULL DEFAULT FALSE,
  ADD COLUMN nota_parcial_1 DECIMAL(4,2) NULL,
  ADD COLUMN nota_parcial_2 DECIMAL(4,2) NULL,
  ADD COLUMN nota_final     DECIMAL(4,2) NULL,
  ADD COLUMN asistencias    TINYINT UNSIGNED NULL;  -- [0-100]%

-- Invariantes de negocio (check constraints MySQL 8)
ALTER TABLE inscripciones
  ADD CONSTRAINT chk_asistencias CHECK (asistencias BETWEEN 0 AND 100),
  ADD CONSTRAINT chk_nota_p1     CHECK (nota_parcial_1 BETWEEN 0 AND 10),
  ADD CONSTRAINT chk_nota_p2     CHECK (nota_parcial_2 BETWEEN 0 AND 10),
  ADD CONSTRAINT chk_nota_final  CHECK (nota_final BETWEEN 0 AND 10);
```

## Transacción de Cierre (Contrato de Atomicidad)

```
@Transactional
void cerrarNota(Long idInscripcion, String emailDocente, String ip) {
  1. findById(idInscripcion) → HTTP 404 si no existe
  2. Verificar notaCerrada == false → HTTP 409 si ya cerrada
  3. Verificar que el docente es titular de la materia → HTTP 403
  4. inscripcion.setNotaCerrada(true)    ← cambio de estado
  5. auditoriaRepository.save(new RegistroAuditoria(  ← misma transacción
       entidad="INSCRIPCION", accion="NOTA_CERRADA",
       idEntidad=idInscripcion, ...hash encadenado...
     ))
  6. inscripcionRepository.save(inscripcion)
  // Si cualquier paso falla → rollback completo → notaCerrada sigue en false
}
```

## Complexity Tracking

> No hay violaciones a la constitución en esta feature.

| Elemento | Justificación |
|---|---|
| Atomicidad nota + auditoría en misma `@Transactional` | Principio III lo exige: "no puede haber notas persistidas sin su registro de auditoría correspondiente" |
| `notaCerrada` por inscripción (no por "acta") | Alineado con el modelo real del frontend; simplifica v1 evitando la entidad Acta |
| v1: reapertura con firma única de admin | La doble firma queda diferida a v2 para no bloquear el delivery; documentado en spec |
