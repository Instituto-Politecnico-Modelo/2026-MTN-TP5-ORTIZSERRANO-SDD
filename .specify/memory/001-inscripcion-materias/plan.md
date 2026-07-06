# Implementation Plan: Inscripción a Materias con Cola de Espera

**Branch**: `001-inscripcion-materias` | **Date**: 2026-06-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification — "Inscripción a materias con cola de espera bajo alta concurrencia"

## Summary

Los alumnos pueden inscribirse a materias durante períodos activos. Cuando hay cupo disponible la inscripción es inmediata (HTTP 201); cuando los cupos se agotan el alumno entra en una cola Redis (HTTP 202) y avanza automáticamente cuando se libera un cupo. El frontend muestra estado en tiempo real vía polling. El sistema debe soportar 50.000 usuarios simultáneos sin HTTP 500 ni corrupción de cupos, garantizado por optimistic locking en Hibernate y serialización de acceso vía Redis.

La infraestructura de cola es el diferenciador técnico central del proyecto — el sistema anterior fallaba aquí.

## Technical Context

**Language/Version**: Java 17 (backend) / TypeScript 5 (frontend)

**Primary Dependencies**:
- Backend: Spring Boot 3, Hibernate 6, Spring Data JPA, Spring Data Redis (Jedis/Lettuce), JJWT, Spring Security
- Frontend: React 18, Axios, React Router 6

**Storage**: MySQL 8 (inscripciones, materias, períodos) + Redis (cola de espera por materia)

**Testing**: JUnit 5 + Mockito + MockMvc (backend) / Jest + React Testing Library (frontend) + k6 (carga)

**Target Platform**: Linux server (backend Spring Boot JAR) + Web browser (React SPA)

**Project Type**: Web application — `backend/` + `frontend/`

**Performance Goals**:
- 50.000 usuarios simultáneos durante período de inscripción activo
- p95 < 500ms para `POST /api/inscripciones` bajo carga nominal
- Redis queue debe procesar al menos 10.000 encolamientos/segundo sin pérdida

**Constraints**:
- Optimistic locking (`@Version` en `Inscripcion`) obligatorio — nunca dos inscripciones exitosas al último cupo
- Redis DEBE usarse para la cola; degradación graciosa si Redis cae (sincrono con mayor latencia, sin HTTP 500)
- Cupos disponibles NUNCA pueden volverse negativos (invariante de negocio)
- El período de inscripción activo es verificado en cada request

**Scale/Scope**: 50.000 usuarios, ~500 materias, ~10.000 cupos por período

## Constitution Check

| Principio | Gate | Estado |
|---|---|---|
| **I. Alta Concurrencia** | Redis queue + optimistic locking OBLIGATORIO. Pruebas de carga documentadas antes de apertura | ✅ NÚCLEO — toda la feature gira alrededor de este principio |
| **II. JWT Auth** | Todos los endpoints de inscripción requieren JWT con rol `ESTUDIANTE`; DOCENTE/ADMIN para consultas | ✅ cumplido — `@PreAuthorize` en cada endpoint |
| **III. Inmutabilidad/Auditoría** | Inscripciones confirmadas registradas en tabla `auditoria` | ✅ parcial — INSERT en auditoría al confirmar inscripción y al cancelar |
| **IV. Stack Definido** | React 18 + Spring Boot 3 + Hibernate 6 + MySQL 8 + Redis | ✅ cumplido — sin tecnologías fuera del stack |
| **V. Privacidad Ley 25.326** | Datos de inscripción son exclusivos del alumno titular; mínimo privilegio | ✅ cumplido — `GET /mis-inscripciones` solo retorna datos del `sub` del JWT |
| **VI. Disponibilidad** | 99,9% uptime; degradación graciosa si Redis cae | ✅ cumplido — fallback sincrono documentado |

## Project Structure

### Documentation (esta feature)

```text
.specify/memory/001-inscripcion-materias/
├── spec.md            ✅ Clarified
├── plan.md            ← este archivo
└── checklist.md       ✅ generado con 55+ ítems CHK
```

### Source Code

```text
backend/
├── src/main/java/com/ipm/inscripciones/
│   ├── model/
│   │   ├── Inscripcion.java          # @Entity; campos: estado, @Version (optimistic lock)
│   │   ├── Materia.java              # cuposMaximos, cuposOcupados
│   │   └── Periodo.java              # fechaInicio, fechaFin, activo
│   ├── repository/
│   │   ├── InscripcionRepository.java
│   │   └── MateriaRepository.java
│   ├── service/
│   │   ├── InscripcionService.java   # lógica core: inscribir(), cancelar(), salirCola()
│   │   └── ColaRedisService.java     # push/pop/position en Redis; degradación graciosa
│   ├── controller/
│   │   ├── InscripcionController.java  # POST, GET mis-inscripciones, PATCH cancelar
│   │   └── DocenteInscripcionController.java  # GET /docente/materias/{id}/inscripciones
│   └── dto/
│       ├── InscripcionRequest.java   # { idMateria }
│       ├── InscripcionResponse.java
│       └── ColaResponse.java         # { status, position, estimated_wait_seconds }
└── src/test/
    ├── unit/InscripcionServiceTest.java
    ├── integration/InscripcionFlowTest.java
    └── load/k6-inscripcion-load.js

frontend/
├── src/
│   ├── services/
│   │   └── materia.service.ts        ✅ actualizado — cancelarInscripcion(), salirDeCola()
│   ├── components/
│   │   ├── MisInscripciones.tsx      ✅ actualizado — dos botones según estado
│   │   ├── ListaMaterias.tsx         # listado de materias disponibles
│   │   └── SalaDeEspera.tsx          # activa en HTTP 429/503, polling /api/health
│   └── types/
│       └── auth.types.ts             ✅ actualizado — LoginResponse.refreshToken
```

**Structure Decision**: Web application (Option 2). Backend y frontend en el mismo repositorio TP3. El repo SDD contiene solo la documentación SDD.

## API Contracts

| Método | Path | Rol JWT | Request Body | Response |
|---|---|---|---|---|
| `POST` | `/api/inscripciones` | `ESTUDIANTE` | `{ idMateria: number }` | 201 `Inscripcion` \| 202 `ColaResponse` |
| `GET` | `/api/inscripciones/mis-inscripciones` | `ESTUDIANTE` | — | 200 `Inscripcion[]` |
| `GET` | `/api/inscripciones/mis-notas` | `ESTUDIANTE` | — | 200 `Inscripcion[]` |
| `PATCH` | `/api/inscripciones/{id}/cancelar` | `ESTUDIANTE` | — | 200 `Inscripcion` |
| `DELETE` | `/api/inscripciones/{id}/cola` | `ESTUDIANTE` | — | 204 |
| `GET` | `/api/inscripciones/{id}/estado` | `ESTUDIANTE` | — | 200 `{ estado, position? }` |
| `GET` | `/api/docente/materias/{id}/inscripciones` | `DOCENTE` \| `ADMINISTRADOR` | — | 200 `Inscripcion[]` |

## Data Model

```sql
-- Entidades principales
CREATE TABLE materias (
  id_materia    BIGINT PRIMARY KEY AUTO_INCREMENT,
  codigo        VARCHAR(20) UNIQUE NOT NULL,
  nombre        VARCHAR(200) NOT NULL,
  cupos_maximos INT NOT NULL,
  cupos_ocupados INT NOT NULL DEFAULT 0,
  activo        BOOLEAN DEFAULT TRUE,
  version       BIGINT DEFAULT 0   -- optimistic lock
);

CREATE TABLE inscripciones (
  id_inscripcion   BIGINT PRIMARY KEY AUTO_INCREMENT,
  id_estudiante    BIGINT NOT NULL,
  id_materia       BIGINT NOT NULL,
  estado           ENUM('CONFIRMADO','ENCOLADO','CANCELADO') NOT NULL,
  nota_cerrada     BOOLEAN NOT NULL DEFAULT FALSE,
  nota_parcial_1   DECIMAL(4,2),
  nota_parcial_2   DECIMAL(4,2),
  nota_final       DECIMAL(4,2),
  asistencias      TINYINT UNSIGNED,  -- [0-100] %
  fecha_inscripcion DATETIME(3) NOT NULL,
  UNIQUE KEY uq_estudiante_materia (id_estudiante, id_materia)
);
```

**Redis**: `cola:{idMateria}` — lista RPUSH/BLPOP de `idInscripcion` para serializar acceso a cupos.

## Complexity Tracking

> No hay violaciones a la constitución en esta feature. El uso de Redis es obligatorio por el Principio I.

| Elemento | Justificación |
|---|---|
| Redis cola + MySQL | Principio I lo exige explícitamente; no hay alternativa más simple que soporte 50k usuarios sin degradar |
| Optimistic locking `@Version` | Única forma de garantizar cupos correctos bajo concurrencia sin serialización completa |
