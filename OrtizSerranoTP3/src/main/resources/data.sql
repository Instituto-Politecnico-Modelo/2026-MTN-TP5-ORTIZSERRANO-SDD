-- Datos iniciales para la base de datos
-- Este archivo se ejecuta automáticamente después de schema.sql

-- Usuarios base (password: "password123" hasheada con BCrypt)
INSERT INTO usuarios (nombre, apellido, email, password) VALUES
('Juan', 'Pérez', 'juan.perez@estudiante.edu.ar', '$2a$10$N9qo8uLOickgx2ZMRZoMye7J5P.9yTX7lH5NELQxKmNf7cqKlmEHe'),
('María', 'González', 'maria.gonzalez@estudiante.edu.ar', '$2a$10$N9qo8uLOickgx2ZMRZoMye7J5P.9yTX7lH5NELQxKmNf7cqKlmEHe'),
('Pedro', 'López', 'pedro.lopez@estudiante.edu.ar', '$2a$10$N9qo8uLOickgx2ZMRZoMye7J5P.9yTX7lH5NELQxKmNf7cqKlmEHe'),
('Carlos', 'Rodríguez', 'carlos.rodriguez@docente.edu.ar', '$2a$10$N9qo8uLOickgx2ZMRZoMye7J5P.9yTX7lH5NELQxKmNf7cqKlmEHe'),
('Laura', 'Fernández', 'laura.fernandez@docente.edu.ar', '$2a$10$N9qo8uLOickgx2ZMRZoMye7J5P.9yTX7lH5NELQxKmNf7cqKlmEHe'),
('Ana', 'Martínez', 'ana.martinez@admin.edu.ar', '$2a$10$N9qo8uLOickgx2ZMRZoMye7J5P.9yTX7lH5NELQxKmNf7cqKlmEHe');

-- Estudiantes
INSERT INTO estudiantes (id_usuario, legajo, carrera, anio_ingreso) VALUES
(1, 'EST-2024-001', 'Ingeniería en Sistemas', 2024),
(2, 'EST-2024-002', 'Ingeniería en Sistemas', 2024),
(3, 'EST-2023-045', 'Ingeniería Industrial', 2023);

-- Docentes
INSERT INTO docentes (id_usuario, titulo, especialidad, departamento) VALUES
(4, 'Ingeniero en Sistemas - Magister en Ciencias de la Computación', 'Programación y Algoritmos', 'Departamento de Sistemas'),
(5, 'Licenciada en Matemática - Doctora en Matemática Aplicada', 'Matemática y Estadística', 'Departamento de Ciencias Básicas');

-- Administradores
INSERT INTO administradores (id_usuario, rol, area, nivel_acceso) VALUES
(6, 'Administrador General', 'Decanato', 10);

-- Materias
INSERT INTO materias (codigo, nombre, descripcion, creditos, anio, cuatrimestre, id_docente) VALUES
('SIS-101', 'Programación I', 'Introducción a la programación estructurada y resolución de problemas', 6, 1, 1, 4),
('SIS-102', 'Matemática I', 'Álgebra lineal y cálculo diferencial e integral', 8, 1, 1, 5),
('SIS-103', 'Introducción a la Computación', 'Conceptos básicos de computación y sistemas', 4, 1, 1, 4),
('SIS-201', 'Programación II', 'Programación orientada a objetos y estructuras de datos', 6, 2, 1, 4),
('SIS-202', 'Matemática II', 'Cálculo avanzado y ecuaciones diferenciales', 8, 2, 1, 5),
('SIS-301', 'Base de Datos', 'Diseño y gestión de bases de datos relacionales', 6, 3, 1, NULL),
('SIS-302', 'Desarrollo Web', 'Tecnologías web frontend y backend', 6, 3, 2, NULL);

-- Inscripciones con datos de ejemplo
INSERT INTO inscripciones (id_estudiante, id_materia, fecha_inscripcion, estado, nota_parcial1, nota_parcial2, nota_final, asistencias) VALUES
-- Estudiante 1 (Juan Pérez)
(1, 1, '2024-03-01', 'APROBADA', 8.5, 9.0, 9.0, 30),
(1, 2, '2024-03-01', 'APROBADA', 7.0, 8.0, 8.0, 28),
(1, 3, '2024-03-01', 'APROBADA', 9.0, 9.5, 9.0, 32),
(1, 4, '2024-08-01', 'ACTIVA', 8.0, NULL, NULL, 15),
(1, 5, '2024-08-01', 'ACTIVA', 7.5, NULL, NULL, 16),

-- Estudiante 2 (María González)
(2, 1, '2024-03-01', 'ACTIVA', 9.0, 8.5, NULL, 25),
(2, 2, '2024-03-01', 'ACTIVA', 8.0, 7.5, NULL, 24),
(2, 3, '2024-03-01', 'ACTIVA', 10.0, 9.0, NULL, 30),

-- Estudiante 3 (Pedro López)
(3, 1, '2023-03-01', 'APROBADA', 6.0, 6.5, 6.0, 20),
(3, 2, '2023-03-01', 'APROBADA', 7.0, 7.5, 7.0, 22),
(3, 4, '2023-08-01', 'APROBADA', 8.0, 8.5, 8.0, 28),
(3, 5, '2023-08-01', 'APROBADA', 6.5, 7.0, 7.0, 25),
(3, 6, '2024-03-01', 'ACTIVA', 7.5, NULL, NULL, 18);
