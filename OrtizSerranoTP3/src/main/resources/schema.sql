-- Script SQL generado automáticamente por Hibernate
-- Sistema de Gestión de Decanato - Base de Datos

-- =====================================================
-- Tabla: usuarios (Tabla padre con herencia JOINED)
-- =====================================================
CREATE TABLE usuarios (
    id_usuario BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    INDEX idx_email (email)
);

-- =====================================================
-- Tabla: estudiantes (Hereda de usuarios)
-- =====================================================
CREATE TABLE estudiantes (
    id_usuario BIGINT PRIMARY KEY,
    legajo VARCHAR(20) NOT NULL UNIQUE,
    carrera VARCHAR(100) NOT NULL,
    anio_ingreso INT,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    INDEX idx_legajo (legajo)
);

-- =====================================================
-- Tabla: docentes (Hereda de usuarios)
-- =====================================================
CREATE TABLE docentes (
    id_usuario BIGINT PRIMARY KEY,
    titulo VARCHAR(200),
    especialidad VARCHAR(100),
    departamento VARCHAR(100),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- =====================================================
-- Tabla: administradores (Hereda de usuarios)
-- =====================================================
CREATE TABLE administradores (
    id_usuario BIGINT PRIMARY KEY,
    rol VARCHAR(50) NOT NULL,
    area VARCHAR(100),
    nivel_acceso INT,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- =====================================================
-- Tabla: materias
-- =====================================================
CREATE TABLE materias (
    id_materia BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(200) NOT NULL,
    descripcion TEXT,
    creditos INT,
    anio INT,
    cuatrimestre INT,
    id_docente BIGINT,
    FOREIGN KEY (id_docente) REFERENCES docentes(id_usuario) ON DELETE SET NULL,
    INDEX idx_codigo (codigo)
);

-- =====================================================
-- Tabla: inscripciones (Relación Many-to-Many entre Estudiantes y Materias)
-- =====================================================
CREATE TABLE inscripciones (
    id_inscripcion BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante BIGINT NOT NULL,
    id_materia BIGINT NOT NULL,
    fecha_inscripcion DATE NOT NULL,
    estado VARCHAR(20) NOT NULL,
    nota_final DOUBLE,
    nota_parcial1 DOUBLE,
    nota_parcial2 DOUBLE,
    asistencias INT,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (id_materia) REFERENCES materias(id_materia) ON DELETE CASCADE,
    INDEX idx_estudiante (id_estudiante),
    INDEX idx_materia (id_materia),
    UNIQUE KEY uk_estudiante_materia (id_estudiante, id_materia)
);

-- =====================================================
-- Datos de ejemplo (Opcional)
-- =====================================================

-- Insertar usuarios base
INSERT INTO usuarios (nombre, apellido, email, password) VALUES
('Juan', 'Pérez', 'juan.perez@estudiante.edu.ar', '$2a$10$hashed_password'),
('María', 'González', 'maria.gonzalez@estudiante.edu.ar', '$2a$10$hashed_password'),
('Carlos', 'Rodríguez', 'carlos.rodriguez@docente.edu.ar', '$2a$10$hashed_password'),
('Ana', 'Martínez', 'ana.martinez@admin.edu.ar', '$2a$10$hashed_password');

-- Insertar estudiantes
INSERT INTO estudiantes (id_usuario, legajo, carrera, anio_ingreso) VALUES
(1, 'EST-2024-001', 'Ingeniería en Sistemas', 2024),
(2, 'EST-2024-002', 'Ingeniería en Sistemas', 2024);

-- Insertar docentes
INSERT INTO docentes (id_usuario, titulo, especialidad, departamento) VALUES
(3, 'Ingeniero en Sistemas - Magister en Ciencias de la Computación', 'Programación', 'Sistemas');

-- Insertar administradores
INSERT INTO administradores (id_usuario, rol, area, nivel_acceso) VALUES
(4, 'Administrador General', 'Decanato', 10);

-- Insertar materias
INSERT INTO materias (codigo, nombre, descripcion, creditos, anio, cuatrimestre, id_docente) VALUES
('SIS-101', 'Programación I', 'Introducción a la programación', 6, 1, 1, 3),
('SIS-102', 'Matemática I', 'Álgebra y cálculo', 8, 1, 1, NULL),
('SIS-201', 'Programación II', 'Programación orientada a objetos', 6, 2, 1, 3);

-- Insertar inscripciones
INSERT INTO inscripciones (id_estudiante, id_materia, fecha_inscripcion, estado, nota_parcial1, nota_parcial2, asistencias) VALUES
(1, 1, '2024-03-01', 'ACTIVA', 8.5, 7.0, 15),
(1, 2, '2024-03-01', 'ACTIVA', 9.0, 8.5, 18),
(2, 1, '2024-03-01', 'ACTIVA', 7.5, 8.0, 16);
