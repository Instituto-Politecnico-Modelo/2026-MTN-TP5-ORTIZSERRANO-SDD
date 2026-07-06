# Implementation Plan: Log de Auditoría con Hash Encadenado

**Branch**: `004-auditoria` | **Date**: 2026-06-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification — "Log de auditoría inmutable con hash encadenado"

## Summary

Tabla unificada `auditoria` en MySQL 8 que registra todos los eventos de negocio del sistema (cambios en notas, inscripciones, usuarios, sesiones). Cada registro incluye un `hash_actual = SHA-256(campos_del_registro || hash_anterior)`, formando una cadena que hace detectable cualquier modificación directa en la base de datos. El primer registro usa `GENESIS_HASH = SHA-256("GENESIS-AUDITORIA-INSTITUCIONAL")`. El INSERT de auditoría ocurre siempre dentro de la misma transacción Hibernate que el evento de negocio — si falla el INSERT, la transacción completa hace rollback. El endpoint `GET /api/admin/auditoria` expone el log paginado (Spring `Page<RegistroAuditoria>`) exclusivamente para `ADMINISTRADOR`. `VistaAuditoria.tsx` consume la API paginada.

## Technical Context

**Language/Version**: Java 17 (backend) / TypeScript 5 (frontend)

**Primary Dependencies**:
- Backend: Spring Boot 3, Hibernate 6 (AuditingEntityListener o servicio manual), Spring Security 6, `java.security.MessageDigest` (SHA-256, sin dependencias externas)
- Frontend: React 18, Axios

**Storage**: MySQL 8 — tabla `auditoria` (única tabla unificada, campo `entidad` discrimina el tipo de evento); 4 índices obligatorios

**Testing**: JUnit 5 + Mockito + MockMvc (backend) / Jest (frontend) + suite de integridad de hash

**Target Platform**: Linux server (backend Spring Boot JAR) + Web browser (React SPA)

**Project Type**: Web application — `backend/` + `frontend/`

**Performance Goals**:
- `GET /api/admin/auditoria?page=0&size=50` con 10.000 registros: ≤ 500ms p95 (requerimiento constitución)
- INSERT de auditoría: < 50ms adicionales al evento de negocio (no-bloqueante en la transacción padre)
- Verificación de integridad de 10.000 registros: < 10s (operación de background)

**Constraints**:
- La tabla `auditoria` NUNCA tiene FK constraints — es append-only, los registros referenciados pueden ser eliminados
- SHA-256 calculado en Java, no en MySQL triggers — portabilidad y testabilidad
- Paginación OBLIGATORIA en v1: `?page=&size=` (máx 100) — no se puede retornar el log completo sin paginar
- Retención mínima 5 años — no hay DELETE en la tabla de auditoría
- `GENESIS_HASH = SHA-256("GENESIS-AUDITORIA-INSTITUCIONAL")` — constante inmutable

**Scale/Scope**: Estimado ~500.000 registros/año; 5 años = ~2.5M registros; índices obligatorios para queries eficientes

## Constitution Check

| Principio | Gate | Estado |
|---|---|---|
| **I. Alta Concurrencia** | Inserts de auditoría no-bloqueantes; índices para queries eficientes bajo carga alta | ✅ cumplido — no hay locks de auditoría; auto-increment serializa naturalmente |
| **II. JWT Auth** | `GET /api/admin/auditoria` requiere JWT con rol `ADMINISTRADOR` exclusivamente | ✅ cumplido — `@PreAuthorize("hasRole('ADMINISTRADOR')")` |
| **III. Inmutabilidad/Auditoría** | Esta feature IS la infraestructura del Principio III | ✅ NÚCLEO — hash encadenado + sin UPDATE/DELETE |
| **IV. Stack Definido** | MySQL 8 + Spring Boot 3 + React 18; SHA-256 de JDK (sin libs externas) | ✅ cumplido — explícitamente sin blockchain |
| **V. Privacidad Ley 25.326** | Log de auditoría solo accesible por ADMINISTRADOR; contiene datos personales (email, IP) | ✅ cumplido — acceso restringido; datos mínimos necesarios |
| **VI. Disponibilidad** | Retención 5 años; backups inmutables; sin DELETE | ✅ cumplido — política de backups del Principio VI aplica |

## Project Structure

### Documentation (esta feature)

```text
.specify/memory/004-auditoria/
├── spec.md            ✅ Clarified
├── plan.md            ← este archivo
└── checklist.md       ✅ generado con 55+ ítems CHK
```

### Source Code

```text
backend/
├── src/main/java/com/ipm/auditoria/
│   ├── model/
│   │   └── RegistroAuditoria.java    # @Entity; todos los campos; sin @ManyToOne (sin FK)
│   ├── repository/
│   │   └── AuditoriaRepository.java  # findAll(Pageable), findByEntidad(), etc.
│   ├── service/
│   │   ├── AuditoriaService.java     # registrar() — calcula hash + inserta en transacción del llamador
│   │   └── HashChainService.java     # sha256(campos...), getUltimoHash(), verificarCadena()
│   ├── controller/
│   │   └── AuditoriaController.java  # GET paginado, GET /entidad/{e}, GET /verificar
│   └── dto/
│       ├── RegistroAuditoriaDto.java
│       └── IntegridadResponse.java   # { integra, totalRegistros, errores[], mensaje }
└── src/test/
    ├── unit/HashChainServiceTest.java
    ├── integration/AuditoriaAtomicidadTest.java
    └── integration/IntegridadCadenaTest.java

frontend/
├── src/
│   ├── services/
│   │   └── auditoria.service.ts      ✅ actualizado — Page<RegistroAuditoria>; listarTodos(page,size)
│   └── components/
│       └── VistaAuditoria.tsx        # tabla paginada; filtros; botón verificar integridad
```

**Structure Decision**: Web application (Option 2). Backend y frontend en el mismo repositorio TP3.

## API Contracts

| Método | Path | Rol JWT | Params / Body | Response |
|---|---|---|---|---|
| `GET` | `/api/admin/auditoria` | `ADMINISTRADOR` | `?page=0&size=50` (máx 100) | 200 `Page<RegistroAuditoria>` |
| `GET` | `/api/admin/auditoria/entidad/{entidad}` | `ADMINISTRADOR` | — | 200 `RegistroAuditoria[]` |
| `GET` | `/api/admin/auditoria/entidad/{entidad}/{id}` | `ADMINISTRADOR` | — | 200 `RegistroAuditoria[]` |
| `GET` | `/api/admin/auditoria/usuario/{idUsuario}` | `ADMINISTRADOR` | — | 200 `RegistroAuditoria[]` |
| `GET` | `/api/admin/auditoria/accion/{accion}` | `ADMINISTRADOR` | — | 200 `RegistroAuditoria[]` |
| `GET` | `/api/admin/auditoria/verificar` | `ADMINISTRADOR` | — | 200 `IntegridadResponse` |

## Data Model

```sql
CREATE TABLE auditoria (
  id_registro     BIGINT       PRIMARY KEY AUTO_INCREMENT,
  entidad         VARCHAR(50)  NOT NULL,              -- 'INSCRIPCION','NOTA','USUARIO','SESION'
  id_entidad      BIGINT       NULL,                  -- id del objeto afectado
  accion          VARCHAR(50)  NOT NULL,              -- 'NOTA_CERRADA','NOTA_REABIERTA','CUENTA_BLOQUEADA',...
  id_usuario      BIGINT       NULL,                  -- NULL = evento de sistema
  email_usuario   VARCHAR(255) NULL,
  descripcion     TEXT         NULL,
  ip_origen       VARCHAR(45)  NULL,                  -- IPv4 o IPv6
  timestamp_evento DATETIME(3) NOT NULL,
  hash_anterior   VARCHAR(64)  NOT NULL,              -- hex SHA-256
  hash_actual     VARCHAR(64)  NOT NULL               -- hex SHA-256
  -- SIN FK constraints (inmutabilidad; entidades pueden ser eliminadas)
);

-- Índices obligatorios (4)
CREATE INDEX idx_audit_timestamp   ON auditoria (timestamp_evento);
CREATE INDEX idx_audit_entidad_id  ON auditoria (entidad, id_entidad);
CREATE INDEX idx_audit_usuario     ON auditoria (id_usuario);
CREATE INDEX idx_audit_accion      ON auditoria (accion);
```

## Hash Chain Algorithm

```java
// Constante del sistema — primer hash anterior de la cadena
public static final String GENESIS_HASH =
    sha256("GENESIS-AUDITORIA-INSTITUCIONAL");

// Cálculo del hash de cada registro
String calcularHash(RegistroAuditoria r, String hashAnterior) {
    String input = r.getIdRegistro()
        + r.getEntidad() + r.getIdEntidad()
        + r.getAccion() + r.getIdUsuario()
        + r.getEmailUsuario() + r.getTimestampEvento()
        + hashAnterior;
    return sha256(input);  // MessageDigest.getInstance("SHA-256")
}

// Obtener el hash del último registro para encadenar
String getUltimoHash() {
    return auditoriaRepository.findTopByOrderByIdRegistroDesc()
        .map(RegistroAuditoria::getHashActual)
        .orElse(GENESIS_HASH);
}
```

> **Concurrencia**: bajo alta concurrencia, dos inserts simultáneos pueden leer el mismo "último hash". Esto es aceptable en la arquitectura de autoridad central — la cadena se verifica por `ORDER BY id_registro ASC`, y gaps de hash detectados por `verificar` son trazables. No se usa un lock de tabla para no afectar la performance bajo 50k usuarios.

## Eventos Auditados (acciones registradas)

| Acción | Entidad | Disparado por |
|---|---|---|
| `NOTA_CARGADA` | `INSCRIPCION` | `PUT /docente/inscripciones/{id}/nota` |
| `NOTA_CERRADA` | `INSCRIPCION` | `PATCH /docente/inscripciones/{id}/cerrar` |
| `NOTA_REABIERTA` | `INSCRIPCION` | `PATCH /admin/inscripciones/{id}/reabrir` |
| `INSCRIPCION_CONFIRMADA` | `INSCRIPCION` | `POST /inscripciones` (201) |
| `INSCRIPCION_ENCOLADA` | `INSCRIPCION` | `POST /inscripciones` (202) |
| `INSCRIPCION_CANCELADA` | `INSCRIPCION` | `PATCH /inscripciones/{id}/cancelar` |
| `LOGIN_FALLIDO` | `USUARIO` | `POST /auth/login` (credenciales inválidas) |
| `CUENTA_BLOQUEADA` | `USUARIO` | `POST /auth/login` (N intentos fallidos) |
| `USUARIO_CREADO` | `USUARIO` | `POST /admin/usuarios` |
| `USUARIO_ELIMINADO` | `USUARIO` | `DELETE /admin/estudiantes/{id}` |

## Complexity Tracking

> No hay violaciones a la constitución en esta feature.

| Elemento | Justificación |
|---|---|
| SHA-256 en Java (no MySQL trigger) | Testabilidad: el hash puede verificarse en tests unitarios sin DB; portabilidad si se migra a otro RDBMS |
| Tabla unificada (sin FK) | Simplifica el schema; permite registrar eventos de entidades eliminadas; evita JOIN bajo alta carga |
| Paginación obligatoria v1 | Con 500k+ registros/año, retornar todo sin paginar violaría el Principio I (timeouts bajo carga) |
