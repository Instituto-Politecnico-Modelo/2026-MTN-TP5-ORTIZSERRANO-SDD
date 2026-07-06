<!--
## Sync Impact Report

**Version change**: TEMPLATE → 1.0.0 (ratificación inicial)

**Principios modificados**: N/A — primera constitución generada desde el Documento de Alcance v1.0 (Marzo 2026)

**Secciones añadidas**:
- Principios Fundamentales (6 principios): Alta Concurrencia, Autenticación JWT,
  Inmutabilidad y Auditoría, Stack Tecnológico, Privacidad y Legalidad, Disponibilidad
- Stack Tecnológico Obligatorio
- Workflow de Desarrollo y Calidad
- Governance

**Secciones eliminadas**: N/A

**Templates revisados**:
- `.specify/templates/plan-template.md` ✅ Compatible — "Constitution Check" gates aplican
- `.specify/templates/spec-template.md` ✅ Compatible — FR format y stack alineados
- `.specify/templates/tasks-template.md` ✅ Compatible — fases reflejan categorías de tareas por principio

**Ítems diferidos**: Ninguno
-->

# Sistema de Inscripciones a Materias y Gestión de Notas — Constitución del Proyecto

## Principios Fundamentales

### I. Alta Concurrencia (NO NEGOCIABLE)

El sistema DEBE soportar un mínimo de 50.000 usuarios simultáneos durante los períodos
de inscripción sin degradación del servicio ni corrupción de datos.

- El backend DEBE implementar un mecanismo de cola (Spring Boot + Redis o equivalente)
  para serializar el acceso a cupos durante picos de demanda.
- Hibernate DEBE usar optimistic locking en las transacciones de inscripción para
  prevenir condiciones de carrera sobre los cupos disponibles.
- Las pruebas de carga son OBLIGATORIAS antes de cada apertura de período de inscripción;
  los resultados DEBEN quedar documentados como artefactos de la release.
- Bajo saturación, el sistema DEBE responder con posición en cola y tiempo estimado de
  espera. Retornar HTTP 500 es INACEPTABLE.
- Disponibilidad objetivo: 99,9 % durante períodos activos de inscripción.

**Razón**: El proyecto nació para reemplazar un sistema que colapsaba bajo carga
concurrente. Este principio es el driver técnico primario.

### II. Autenticación y Autorización por JWT (NO NEGOCIABLE)

Todos los endpoints de la API DEBEN estar protegidos por autenticación JWT. Ningún
endpoint que exponga datos de alumnos, notas o administración puede ser accedido sin
un token JWT válido y firmado.

- Los JWTs DEBEN incluir el rol del usuario (`alumno`, `docente`, `admin`) y validarse
  en cada request sin consultar la base de datos.
- El control de acceso basado en roles DEBE aplicar mínimo privilegio: un alumno solo
  ve sus propios datos; un docente opera únicamente sobre sus materias.
- Los tokens encubiertos o "llaves maestras" de terceros (ej. padres sin consentimiento
  del alumno) están ESTRICTAMENTE PROHIBIDOS sin aprobación legal explícita y
  consentimiento activo del titular.

**Razón**: La validación stateless reduce latencia bajo carga y provee una capa de
identidad consistente y auditable.

### III. Inmutabilidad y Auditoría de Notas (NO NEGOCIABLE)

Una vez que un docente cierra un acta mediante `POST /api/actas/{id}/cerrar`, las notas
de esa acta DEBEN ser permanentemente inmutables a nivel de lógica de negocio.

- La tabla de auditoría en MySQL DEBE registrar cada INSERT/UPDATE sobre notas con:
  usuario JWT, timestamp, dirección IP, valor anterior y hash encadenado del registro
  previo.
- Cualquier modificación posterior al cierre DEBE requerir un proceso formal de
  rectificación con aprobación de dos administradores, registrado en la misma tabla
  de auditoría.
- El log de auditoría DEBE ser accesible al Decanato mediante `GET /api/auditoria`
  (requiere JWT con rol `admin`).
- Blockchain está explícitamente fuera del alcance; la tabla de auditoría encadenada
  en MySQL provee garantías equivalentes dado el modelo de autoridad central
  de la institución.

**Razón**: La integridad académica exige que los registros de notas cerradas no puedan
ser alterados silenciosamente por ningún usuario, incluyendo administradores.

### IV. Stack Tecnológico Definido (NO NEGOCIABLE)

El sistema DEBE construirse exclusivamente sobre el stack institucional definido.
Ninguna tecnología fuera de este stack puede incorporarse sin aprobación escrita
del líder técnico del proyecto.

| Capa | Tecnología | Versión mínima |
|---|---|---|
| Frontend | React.js + TypeScript | React 18 / TS 5 |
| Backend | Java + Spring Boot (REST API) | Java 17 / Spring Boot 3 |
| ORM | Hibernate | 6.x |
| Autenticación | JWT (Spring Security / JJWT) | — |
| Base de datos | MySQL | 8.x |
| Cola de espera | Redis o equivalente embebido en Spring | — |

Capas de ML externas, nodos blockchain o runtimes no soportados están PROHIBIDOS.

**Razón**: La coherencia de stack reduce el riesgo de integración, simplifica el
onboarding y está alineada con la infraestructura institucional existente.

### V. Privacidad y Legalidad — Ley 25.326 (NO NEGOCIABLE)

El sistema DEBE cumplir en todo momento con la Ley 25.326 de Protección de Datos
Personales (Argentina).

- La recolección de datos biométricos (reconocimiento facial o equivalente) está
  ESTRICTAMENTE PROHIBIDA y permanentemente fuera del alcance.
- Los datos del alumno (notas, inscripciones, datos personales) pertenecen
  exclusivamente al alumno como adulto universitario; ningún acceso encubierto de
  terceros está permitido.
- Cualquier nueva funcionalidad que involucre datos sensibles (biométricos, de salud,
  financieros) DEBE pasar por revisión legal institucional antes de que comience
  cualquier trabajo de especificación o implementación.

**Razón**: Las violaciones legales exponen a la institución a sanciones de la AAIP
y vulneran los derechos fundamentales de los estudiantes.

### VI. Disponibilidad y Recuperabilidad

El sistema DEBE alcanzar un objetivo de 99,9 % de disponibilidad durante períodos
activos de inscripción.

- Los backups de MySQL DEBEN almacenarse en almacenamiento inmutable con retención
  mínima de 5 años.
- La migración desde el sistema anterior DEBE realizarse sin pérdida de datos
  históricos.
- Los procedimientos de recuperación DEBEN estar documentados y probados antes de
  cada período de inscripción.

**Razón**: La continuidad académica de la institución depende del acceso ininterrumpido
a los datos de inscripciones y notas.

## Stack Tecnológico Obligatorio

Esta sección formaliza el stack completo y no negociable. Cualquier desviación requiere
un Architectural Decision Record (ADR) aprobado por el líder de proyecto y archivado
en el repositorio SDD.

Las funcionalidades explícitamente fuera del alcance y sus razones:

- **Reconocimiento facial / recomendación basada en biometría** — LEGAL + ÉTICO + TÉCNICO
  (Ley 25.326; sesgo discriminatorio; complejidad ML no contemplada). Alternativa en
  alcance: motor de recomendación basado en historial académico.
- **Acceso de padres sin consentimiento del alumno** — LEGAL (Ley 25.326; exposición
  ante la AAIP). Alternativa posible previa aprobación legal: delegación explícita.
- **Blockchain para inmutabilidad** — TÉCNICO (complejidad extrema, nodos externos,
  alto costo, tiempos de escritura lentos sin beneficio adicional sobre la solución
  adoptada).

## Workflow de Desarrollo y Calidad

- Todos los endpoints REST DEBEN documentar su contrato (método, path, body, esquema
  de respuesta, rol JWT requerido) antes de que comience la implementación.
- Los resultados de pruebas de carga DEBEN adjuntarse como artefactos en cada release
  de período de inscripción.
- Toda migración de base de datos DEBE revisarse por impacto en índices de consultas
  concurrentes de inscripción antes del deployment.
- Ninguna funcionalidad que modifique el esquema de la tabla de auditoría puede
  desplegarse sin revisión del líder de proyecto.
- Code review es OBLIGATORIO para cambios en: filtros de autenticación, lógica de
  transacciones de inscripción, y el enforcer de inmutabilidad de notas.
- El endpoint `GET /api/auditoria` DEBE probarse con un dataset mínimo de 10.000
  registros de auditoría antes del deployment a producción.

## Governance

Esta constitución tiene precedencia sobre toda otra práctica de desarrollo, convención
o acuerdo informal dentro del proyecto.

**Procedimiento de enmienda**:
1. Las enmiendas propuestas DEBEN enviarse como pull request a `main` modificando
   este archivo.
2. Las enmiendas a los Principios I–V requieren aprobación del líder de proyecto
   Y del contacto legal/compliance institucional.
3. Las enmiendas al Principio VI, la sección de Stack o el Workflow requieren
   únicamente aprobación del líder de proyecto.
4. Toda enmienda DEBE incrementar el número de versión según reglas de semantic
   versioning y actualizar `Last Amended`.

**Política de versiones**:
- MAJOR: Eliminación o redefinición incompatible de cualquier principio.
- MINOR: Nuevo principio añadido o expansión material de una sección existente.
- PATCH: Clarificaciones, correcciones de redacción, typos.

**Revisión de cumplimiento**: La constitución DEBE revisarse al inicio de cada sprint
de período de inscripción para confirmar que todas las funcionalidades activas siguen
siendo conformes.

**Version**: 1.0.0 | **Ratified**: 2026-03-01 | **Last Amended**: 2026-06-09
