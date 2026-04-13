# Sistema de Gestión de Decanato - Base de Datos

Este proyecto implementa un sistema de gestión académica con JPA/Hibernate que genera automáticamente la base de datos.

## 🗄️ Modelo de Datos

### Entidades Principales:

1. **Usuario** (Clase base con herencia JOINED)
   - Estudiante
   - Docente
   - Administrador

2. **Materia**
   - Información de asignaturas
   - Relación con Docente (Many-to-One)

3. **Inscripcion**
   - Relación entre Estudiante y Materia (Many-to-Many)
   - Incluye notas, asistencias y estado

## 🚀 Cómo ejecutar

### 1. Ejecutar con H2 (Base de datos en memoria - por defecto)

```bash
./gradlew bootRun
```

La aplicación creará automáticamente todas las tablas en memoria.

### 2. Acceder a la consola H2

Una vez iniciada la aplicación, abre tu navegador en:
```
http://localhost:8080/h2-console
```

Credenciales:
- **JDBC URL**: `jdbc:h2:mem:decanato_db`
- **Usuario**: `sa`
- **Password**: (dejar vacío)

### 3. Ver la base de datos generada

En la consola H2 podrás ver todas las tablas creadas automáticamente por Hibernate:
- `usuarios`
- `estudiantes`
- `docentes`
- `administradores`
- `materias`
- `inscripciones`

## 🔄 Cambiar a MySQL

1. Edita `application.properties` y comenta la configuración de H2
2. Descomenta la configuración de MySQL en `application-mysql.properties`
3. Agrega la dependencia MySQL en `build.gradle`:
   ```gradle
   runtimeOnly 'com.mysql:mysql-connector-j'
   ```
4. Crea la base de datos en MySQL:
   ```sql
   CREATE DATABASE decanato_db;
   ```
5. Ejecuta la aplicación

## 📊 Estructura de Tablas

### Tabla: usuarios
```sql
id_usuario (PK) | nombre | apellido | email | password
```

### Tabla: estudiantes (hereda de usuarios)
```sql
id_usuario (PK, FK) | legajo | carrera | anio_ingreso
```

### Tabla: docentes (hereda de usuarios)
```sql
id_usuario (PK, FK) | titulo | especialidad | departamento
```

### Tabla: administradores (hereda de usuarios)
```sql
id_usuario (PK, FK) | rol | area | nivel_acceso
```

### Tabla: materias
```sql
id_materia (PK) | codigo | nombre | descripcion | creditos | anio | cuatrimestre | id_docente (FK)
```

### Tabla: inscripciones
```sql
id_inscripcion (PK) | id_estudiante (FK) | id_materia (FK) | fecha_inscripcion | estado | nota_final | nota_parcial1 | nota_parcial2 | asistencias
```

## 🔧 Configuración de Hibernate

En `application.properties`:

```properties
# Crear/Eliminar tablas cada vez que se ejecuta
spring.jpa.hibernate.ddl-auto=create-drop

# Otras opciones:
# create: Crea las tablas al inicio (borra datos existentes)
# update: Actualiza el esquema sin borrar datos
# validate: Solo valida que el esquema coincida con las entidades
# none: No hace nada
```

## 📝 Scripts SQL

- **schema.sql**: Script SQL completo de la estructura de la base de datos
- **data.sql**: Datos de ejemplo precargados
- Estos archivos se ejecutan automáticamente si Hibernate está configurado correctamente

## 🐛 Debugging

Para ver las queries SQL generadas por Hibernate, en `application.properties`:
```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

## 📚 Relaciones

- **Usuario → Estudiante/Docente/Administrador**: Herencia JOINED (1:1)
- **Docente → Materias**: One-to-Many (1:N)
- **Estudiante → Inscripciones**: One-to-Many (1:N)
- **Materia → Inscripciones**: One-to-Many (1:N)
- **Estudiante ↔ Materia**: Many-to-Many a través de Inscripciones

## 🎯 Datos de Ejemplo

Al ejecutar la aplicación, se cargarán automáticamente:
- 6 usuarios (3 estudiantes, 2 docentes, 1 administrador)
- 7 materias
- 13 inscripciones con notas y asistencias

Password por defecto para todos: `password123`
