create database universidadDB;
CREATE TABLE periodos (
    id_periodo INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    estado VARCHAR(50) NOT NULL
);

CREATE TABLE usuarios (
    id_usuario INT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE administradores (
    id_admin INT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    FOREIGN KEY (id_admin) REFERENCES usuarios(id_usuario)
);
CREATE TABLE auditoria_acciones (
    id_auditoria INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    accion VARCHAR(255) NOT NULL,
    entidad VARCHAR(255),
    fecha_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_origen VARCHAR(50),
    valor_anterior JSON,
    hash_previo VARCHAR(255),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);
CREATE TABLE docentes (
    id_docente INT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    departamento VARCHAR(255),
    FOREIGN KEY (id_docente) REFERENCES usuarios(id_usuario)
);
CREATE TABLE alumnos (
    id_alumno INT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    legajo VARCHAR(100),
    FOREIGN KEY (id_alumno) REFERENCES usuarios(id_usuario)
);
CREATE TABLE materias (
    id_materia INT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    departamento VARCHAR(255),
    descripcion TEXT,
    cupo_maximo INT
);
CREATE TABLE inscripciones (
    id_inscripcion INT PRIMARY KEY AUTO_INCREMENT,
    id_alumno INT NOT NULL,
    id_materia INT NOT NULL,
    id_periodo INT NOT NULL,
    fecha DATE NOT NULL,
    estado VARCHAR(50) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    FOREIGN KEY (id_alumno) REFERENCES alumnos(id_alumno),
    FOREIGN KEY (id_materia) REFERENCES materias(id_materia),
    FOREIGN KEY (id_periodo) REFERENCES periodos(id_periodo)
);
CREATE TABLE notas (
    id_nota INT PRIMARY KEY AUTO_INCREMENT,
    id_alumno INT NOT NULL,
    id_materia INT NOT NULL,
    id_docente INT NOT NULL,
    id_periodo INT NOT NULL,
    nota_parcial DECIMAL(5,2),
    nota_final DECIMAL(5,2),
    fecha_carga DATE,
    estado ENUM('abierta', 'cerrada') NOT NULL DEFAULT 'abierta',
    fecha_cierre DATE,
    FOREIGN KEY (id_alumno) REFERENCES alumnos(id_alumno),
    FOREIGN KEY (id_materia) REFERENCES materias(id_materia),
    FOREIGN KEY (id_docente) REFERENCES docentes(id_docente),
    FOREIGN KEY (id_periodo) REFERENCES periodos(id_periodo)
);

-- ----------------------------------------------------------- indices-------------------------------------------------------------------------------------------------------
-- Búsquedas frecuentes de notas por alumno+materia+periodo
CREATE INDEX idx_notas_alumno_materia_periodo 
ON notas(id_alumno, id_materia, id_periodo);

-- Búsquedas de inscripciones por alumno+periodo
CREATE INDEX idx_inscripciones_alumno_periodo 
ON inscripciones(id_alumno, id_periodo);
-- -----------------------------------------------------------triggers-------------------------------------------------------------------------------------------------------------
DELIMITER $$
CREATE TRIGGER trg_auditoria_cierre_nota
AFTER UPDATE ON notas
FOR EACH ROW
BEGIN
    IF OLD.estado = 'abierta' AND NEW.estado = 'cerrada' THEN
        INSERT INTO auditoria_acciones 
            (id_usuario, accion, entidad, fecha_hora, valor_anterior)
        VALUES (
            NEW.id_docente,
            'CIERRE_ACTA',
            CONCAT('nota:', NEW.id_nota),
            NOW(),
            JSON_OBJECT(
                'nota_parcial', OLD.nota_parcial,
                'nota_final', OLD.nota_final,
                'estado', OLD.estado
            )
        );
    END IF;
END$$
DELIMITER ;


DELIMITER $$
CREATE TRIGGER trg_auditoria_modificacion_nota
AFTER UPDATE ON notas
FOR EACH ROW
BEGIN
    IF (OLD.nota_parcial <> NEW.nota_parcial OR OLD.nota_final <> NEW.nota_final)
       AND OLD.estado = 'abierta' THEN
        INSERT INTO auditoria_acciones 
            (id_usuario, accion, entidad, fecha_hora, valor_anterior)
        VALUES (
            NEW.id_docente,
            'MODIFICACION_NOTA',
            CONCAT('nota:', NEW.id_nota),
            NOW(),
            JSON_OBJECT(
                'nota_parcial', OLD.nota_parcial,
                'nota_final', OLD.nota_final
            )
        );
    END IF;
END$$
DELIMITER ;

DELIMITER $$
CREATE TRIGGER trg_control_cupo
BEFORE INSERT ON inscripciones
FOR EACH ROW
BEGIN
    DECLARE cupo_actual INT;
    DECLARE cupo_max INT;

    SELECT COUNT(*) INTO cupo_actual
    FROM inscripciones
    WHERE id_materia = NEW.id_materia 
      AND id_periodo = NEW.id_periodo
      AND estado = 'activa';

    SELECT cupo_maximo INTO cupo_max
    FROM materias
    WHERE id_materia = NEW.id_materia;

    IF cupo_max IS NOT NULL AND cupo_actual >= cupo_max THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La materia no tiene cupo disponible';
    END IF;
END$$
DELIMITER ;

DELIMITER $$
CREATE TRIGGER trg_bloqueo_nota_cerrada
BEFORE UPDATE ON notas
FOR EACH ROW
BEGIN
    IF OLD.estado = 'cerrada' AND 
       (NEW.nota_parcial <> OLD.nota_parcial OR NEW.nota_final <> OLD.nota_final) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No se puede modificar una nota con acta cerrada';
    END IF;
END$$
DELIMITER ;
-- ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------