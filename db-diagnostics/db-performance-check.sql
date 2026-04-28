-- =============================================================================
-- db-performance-check.sql
-- Sistema de Decanato IPM — Diagnóstico de rendimiento e índices en MySQL
--
-- Uso:
--   mysql -u root -p decanato_db < db-performance-check.sql
--   -- o bien, dentro de mysql:
--   source /ruta/al/db-performance-check.sql
--
-- Requiere MySQL 8.0+ (EXPLAIN ANALYZE, JSON_TABLE, information_schema mejorado)
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- SECCIÓN 0: Configuración de sesión para profiling
-- ─────────────────────────────────────────────────────────────────────────────
SET profiling = 1;
SET profiling_history_size = 100;
SET @db_name = DATABASE();

SELECT CONCAT('=== Diagnóstico de rendimiento: ', @db_name, ' ===') AS info;
SELECT NOW() AS ejecutado_en;

-- ─────────────────────────────────────────────────────────────────────────────
-- SECCIÓN 1: Estado general del esquema
-- ─────────────────────────────────────────────────────────────────────────────
SELECT '─── SECCIÓN 1: Resumen del esquema ───' AS seccion;

SELECT
    TABLE_NAME                                   AS tabla,
    TABLE_ROWS                                   AS filas_aprox,
    ROUND(DATA_LENGTH / 1024 / 1024, 2)          AS datos_mb,
    ROUND(INDEX_LENGTH / 1024 / 1024, 2)         AS indices_mb,
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS total_mb,
    ENGINE                                       AS motor
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = @db_name
ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC;

-- ─────────────────────────────────────────────────────────────────────────────
-- SECCIÓN 2: Inventario de índices y estadísticas de cardinalidad
-- ─────────────────────────────────────────────────────────────────────────────
SELECT '─── SECCIÓN 2: Índices y cardinalidad ───' AS seccion;

SELECT
    s.TABLE_NAME                 AS tabla,
    s.INDEX_NAME                 AS indice,
    s.COLUMN_NAME                AS columna,
    s.SEQ_IN_INDEX               AS orden,
    s.CARDINALITY                AS cardinalidad,
    s.NON_UNIQUE                 AS no_unico,
    s.INDEX_TYPE                 AS tipo
FROM information_schema.STATISTICS s
WHERE s.TABLE_SCHEMA = @db_name
ORDER BY s.TABLE_NAME, s.INDEX_NAME, s.SEQ_IN_INDEX;

-- ─────────────────────────────────────────────────────────────────────────────
-- SECCIÓN 3: EXPLAIN ANALYZE — Consultas críticas del sistema
-- ─────────────────────────────────────────────────────────────────────────────
SELECT '─── SECCIÓN 3: EXPLAIN ANALYZE — Consultas críticas ───' AS seccion;

-- ── 3.1 countCuposOcupados (consulta más ejecutada — determina si hay cupo) ──
SELECT '3.1 countCuposOcupados — usa idx_inscripciones_mat_estado' AS consulta;
EXPLAIN ANALYZE
    SELECT COUNT(*) FROM inscripciones
    WHERE id_materia = 1
      AND estado <> 'CANCELADA';

-- ── 3.2 findByEstudianteIdUsuario — mis inscripciones ────────────────────────
SELECT '3.2 findByEstudianteIdUsuario — usa idx_inscripciones_estudiante' AS consulta;
EXPLAIN ANALYZE
    SELECT * FROM inscripciones
    WHERE id_estudiante = 1;

-- ── 3.3 findByEmail — login / autenticación ──────────────────────────────────
SELECT '3.3 findByEmail — usa idx_usuarios_email (UNIQUE)' AS consulta;
EXPLAIN ANALYZE
    SELECT * FROM usuarios
    WHERE email = 'admin@test.com';

-- ── 3.4 findByCodigo — lookup de materia ─────────────────────────────────────
SELECT '3.4 findByCodigo — usa idx_materias_codigo (UNIQUE)' AS consulta;
EXPLAIN ANALYZE
    SELECT * FROM materias
    WHERE codigo = 'SIS-101';

-- ── 3.5 findPrimeroPendiente — cola FIFO ─────────────────────────────────────
SELECT '3.5 findPrimeroPendiente — usa idx_cola_materia_estado_pos' AS consulta;
EXPLAIN ANALYZE
    SELECT * FROM cola_inscripciones
    WHERE id_materia = 1
      AND estado = 'PENDIENTE'
    ORDER BY posicion ASC
    LIMIT 1;

-- ── 3.6 existsByEstudianteAndMateriaAndEstadoNot — verificar duplicado ────────
SELECT '3.6 existsByEstudianteAndMateriaAndEstadoNot — usa idx_inscripciones_est_mat' AS consulta;
EXPLAIN ANALYZE
    SELECT COUNT(*) > 0 FROM inscripciones
    WHERE id_estudiante = 1
      AND id_materia = 1
      AND estado <> 'CANCELADA';

-- ── 3.7 Auditoría — findByEntidadAndIdEntidad ────────────────────────────────
SELECT '3.7 Auditoría — usa idx_auditoria_entidad_id' AS consulta;
EXPLAIN ANALYZE
    SELECT * FROM auditoria
    WHERE entidad = 'Inscripcion'
      AND id_entidad = 1
    ORDER BY id_registro ASC;

-- ── 3.8 Auditoría — rango de tiempo (reportes) ───────────────────────────────
SELECT '3.8 Auditoría por rango de tiempo — usa idx_auditoria_timestamp' AS consulta;
EXPLAIN ANALYZE
    SELECT * FROM auditoria
    WHERE timestamp_evento BETWEEN NOW() - INTERVAL 7 DAY AND NOW()
    ORDER BY id_registro ASC;

-- ── 3.9 expirarVencidos — UPDATE masivo PENDIENTE→EXPIRADO ────────────────────
SELECT '3.9 EXPLAIN de expirarVencidos — usa idx_cola_expiracion' AS consulta;
EXPLAIN
    UPDATE cola_inscripciones
    SET    estado = 'EXPIRADO',
           fecha_resolucion = NOW()
    WHERE  estado = 'PENDIENTE'
      AND  fecha_expiracion < NOW();

-- ── 3.10 JOIN: inscripciones de materia con nota (reporte docente) ────────────
SELECT '3.10 JOIN inscripciones + estudiantes + usuarios (reporte notas)' AS consulta;
EXPLAIN ANALYZE
    SELECT u.nombre, u.apellido, e.legajo, i.estado, i.nota_final
    FROM inscripciones i
    JOIN estudiantes   e ON e.id_usuario = i.id_estudiante
    JOIN usuarios      u ON u.id_usuario = i.id_estudiante
    WHERE i.id_materia = 1
      AND i.estado <> 'CANCELADA'
    ORDER BY u.apellido;

-- ─────────────────────────────────────────────────────────────────────────────
-- SECCIÓN 4: Detección de consultas sin índice (Full Table Scans)
-- ─────────────────────────────────────────────────────────────────────────────
SELECT '─── SECCIÓN 4: Detección de Full Table Scans ───' AS seccion;

-- Estadísticas del schema_table_statistics_with_buffer (sys schema)
SELECT
    object_name  AS tabla,
    rows_full_scanned  AS full_scans_acumulados,
    rows_examined  AS filas_examinadas
FROM sys.schema_table_statistics
WHERE object_schema = @db_name
  AND rows_full_scanned > 0
ORDER BY rows_full_scanned DESC;

-- ─────────────────────────────────────────────────────────────────────────────
-- SECCIÓN 5: Índices sin usar (candidatos a eliminar)
-- ─────────────────────────────────────────────────────────────────────────────
SELECT '─── SECCIÓN 5: Índices sin usar (desde el último restart) ───' AS seccion;

SELECT
    object_schema  AS base_datos,
    object_name    AS tabla,
    index_name     AS indice
FROM sys.schema_unused_indexes
WHERE object_schema = @db_name
ORDER BY object_name, index_name;

-- ─────────────────────────────────────────────────────────────────────────────
-- SECCIÓN 6: Validación de integridad de datos (triggers lógicos)
-- ─────────────────────────────────────────────────────────────────────────────
SELECT '─── SECCIÓN 6: Validación de integridad ───' AS seccion;

-- 6.1 Materias con cupos superados
SELECT '6.1 Materias con cuposMaximos superados' AS check_nombre;
SELECT
    m.codigo,
    m.nombre,
    m.cupos_maximos                         AS cupos_max,
    COUNT(i.id_inscripcion)                 AS inscriptos_activos,
    COUNT(i.id_inscripcion) - m.cupos_maximos AS exceso
FROM materias m
JOIN inscripciones i ON i.id_materia = m.id_materia
                    AND i.estado <> 'CANCELADA'
WHERE m.cupos_maximos IS NOT NULL
GROUP BY m.id_materia, m.codigo, m.nombre, m.cupos_maximos
HAVING COUNT(i.id_inscripcion) > m.cupos_maximos
ORDER BY exceso DESC;

-- 6.2 Inscripciones duplicadas activas (misma persona, misma materia, no CANCELADA)
SELECT '6.2 Inscripciones duplicadas activas (unicidad violada)' AS check_nombre;
SELECT
    id_estudiante,
    id_materia,
    COUNT(*) AS cantidad
FROM inscripciones
WHERE estado <> 'CANCELADA'
GROUP BY id_estudiante, id_materia
HAVING COUNT(*) > 1
ORDER BY cantidad DESC;

-- 6.3 Inscripciones huérfanas (sin usuario padre)
SELECT '6.3 Inscripciones huérfanas (estudiante no existe en usuarios)' AS check_nombre;
SELECT i.id_inscripcion, i.id_estudiante, i.id_materia
FROM inscripciones i
LEFT JOIN usuarios u ON u.id_usuario = i.id_estudiante
WHERE u.id_usuario IS NULL;

-- 6.4 Cadena de auditoría rota (hashAnterior ≠ hashActual del registro previo)
SELECT '6.4 Cadena de auditoría rota (primeros 20 quiebres)' AS check_nombre;
SELECT
    a2.id_registro         AS registro_roto,
    a2.entidad,
    a2.accion,
    a1.hash_actual         AS hash_previo_real,
    a2.hash_anterior       AS hash_previo_declarado
FROM auditoria a1
JOIN auditoria a2 ON a2.id_registro = a1.id_registro + 1
WHERE a1.hash_actual <> a2.hash_anterior
ORDER BY a2.id_registro
LIMIT 20;

-- 6.5 Notas fuera de rango (0-10)
SELECT '6.5 Notas fuera de rango 0-10' AS check_nombre;
SELECT id_inscripcion, id_estudiante, id_materia,
       nota_parcial1, nota_parcial2, nota_final
FROM inscripciones
WHERE nota_parcial1 NOT BETWEEN 0 AND 10
   OR nota_parcial2 NOT BETWEEN 0 AND 10
   OR nota_final    NOT BETWEEN 0 AND 10;

-- ─────────────────────────────────────────────────────────────────────────────
-- SECCIÓN 7: Triggers MySQL — Protecciones adicionales en la BD
-- ─────────────────────────────────────────────────────────────────────────────
SELECT '─── SECCIÓN 7: Creación de triggers de BD ───' AS seccion;

-- Nota: los triggers son una capa extra de protección.
-- La lógica principal ya está en la capa Java (InscripcionService + semáforo).

DELIMITER //

-- 7.1 Trigger: evitar insertar una nota fuera de rango 0-10
DROP TRIGGER IF EXISTS trg_validar_nota_insert //
CREATE TRIGGER trg_validar_nota_insert
BEFORE INSERT ON inscripciones
FOR EACH ROW
BEGIN
    IF NEW.nota_final IS NOT NULL AND (NEW.nota_final < 0 OR NEW.nota_final > 10) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'nota_final debe estar entre 0 y 10';
    END IF;
    IF NEW.nota_parcial1 IS NOT NULL AND (NEW.nota_parcial1 < 0 OR NEW.nota_parcial1 > 10) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'nota_parcial1 debe estar entre 0 y 10';
    END IF;
    IF NEW.nota_parcial2 IS NOT NULL AND (NEW.nota_parcial2 < 0 OR NEW.nota_parcial2 > 10) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'nota_parcial2 debe estar entre 0 y 10';
    END IF;
END //

DROP TRIGGER IF EXISTS trg_validar_nota_update //
CREATE TRIGGER trg_validar_nota_update
BEFORE UPDATE ON inscripciones
FOR EACH ROW
BEGIN
    -- Bloquear modificación si la nota ya fue cerrada
    IF OLD.nota_cerrada = TRUE AND NEW.nota_cerrada = TRUE THEN
        IF (NEW.nota_final    <> OLD.nota_final    AND NEW.nota_final    IS NOT NULL) OR
           (NEW.nota_parcial1 <> OLD.nota_parcial1 AND NEW.nota_parcial1 IS NOT NULL) OR
           (NEW.nota_parcial2 <> OLD.nota_parcial2 AND NEW.nota_parcial2 IS NOT NULL) OR
           (NEW.asistencias   <> OLD.asistencias   AND NEW.asistencias   IS NOT NULL) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'No se puede modificar una nota ya cerrada';
        END IF;
    END IF;
    -- Validar rango
    IF NEW.nota_final IS NOT NULL AND (NEW.nota_final < 0 OR NEW.nota_final > 10) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'nota_final debe estar entre 0 y 10';
    END IF;
    IF NEW.nota_parcial1 IS NOT NULL AND (NEW.nota_parcial1 < 0 OR NEW.nota_parcial1 > 10) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'nota_parcial1 debe estar entre 0 y 10';
    END IF;
END //

-- 7.2 Trigger: impedir que se bajen los cupos por debajo de los inscriptos actuales
DROP TRIGGER IF EXISTS trg_validar_cupos_update //
CREATE TRIGGER trg_validar_cupos_update
BEFORE UPDATE ON materias
FOR EACH ROW
BEGIN
    DECLARE inscriptos_actuales INT;
    IF NEW.cupos_maximos IS NOT NULL AND
       (OLD.cupos_maximos IS NULL OR NEW.cupos_maximos < OLD.cupos_maximos) THEN
        SELECT COUNT(*) INTO inscriptos_actuales
        FROM inscripciones
        WHERE id_materia = NEW.id_materia
          AND estado <> 'CANCELADA';
        IF inscriptos_actuales > NEW.cupos_maximos THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'No se puede reducir cupos_maximos por debajo de los inscriptos actuales';
        END IF;
    END IF;
END //

-- 7.3 Trigger: registrar en log cuando se elimina físicamente un usuario
DROP TRIGGER IF EXISTS trg_audit_delete_usuario //
CREATE TRIGGER trg_audit_delete_usuario
BEFORE DELETE ON usuarios
FOR EACH ROW
BEGIN
    INSERT INTO auditoria (entidad, id_entidad, accion, descripcion, id_usuario, timestamp_evento, hash_anterior, hash_actual)
    VALUES (
        'Usuario',
        OLD.id_usuario,
        'DELETE_FISICO',
        CONCAT('Usuario eliminado: ', OLD.nombre, ' ', OLD.apellido, ' (', OLD.email, ')'),
        OLD.id_usuario,
        NOW(),
        'trigger-generado',
        SHA2(CONCAT('trigger-generado', 'Usuario', OLD.id_usuario, 'DELETE_FISICO', NOW()), 256)
    );
END //

DELIMITER ;

-- ─────────────────────────────────────────────────────────────────────────────
-- SECCIÓN 8: Resumen de profiling
-- ─────────────────────────────────────────────────────────────────────────────
SELECT '─── SECCIÓN 8: Perfil de consultas ejecutadas en esta sesión ───' AS seccion;

SHOW PROFILES;

SELECT
    QUERY_ID,
    ROUND(SUM(DURATION) * 1000, 3) AS duracion_ms,
    LEFT(QUERY, 80)                  AS consulta
FROM information_schema.PROFILING
WHERE QUERY_ID > 0
GROUP BY QUERY_ID, QUERY
ORDER BY SUM(DURATION) DESC
LIMIT 20;

-- ─────────────────────────────────────────────────────────────────────────────
-- FIN DEL SCRIPT
-- ─────────────────────────────────────────────────────────────────────────────
SELECT '=== Diagnóstico completado ===' AS fin;
SELECT
    CASE
        WHEN (SELECT COUNT(*) FROM inscripciones i
              JOIN materias m ON m.id_materia = i.id_materia
              WHERE m.cupos_maximos IS NOT NULL
                AND i.estado <> 'CANCELADA'
              GROUP BY i.id_materia
              HAVING COUNT(*) > m.cupos_maximos
              LIMIT 1) IS NOT NULL
        THEN '❌ ALERTA: Se detectaron materias con cupos superados'
        ELSE '✅ Integridad de cupos: OK'
    END AS check_cupos,
    CASE
        WHEN (SELECT COUNT(*) FROM inscripciones
              WHERE estado <> 'CANCELADA'
              GROUP BY id_estudiante, id_materia
              HAVING COUNT(*) > 1
              LIMIT 1) IS NOT NULL
        THEN '❌ ALERTA: Se detectaron inscripciones duplicadas activas'
        ELSE '✅ Unicidad de inscripciones: OK'
    END AS check_unicidad;
