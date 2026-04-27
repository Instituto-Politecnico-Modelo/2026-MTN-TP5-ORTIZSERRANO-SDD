package com.DecanatoOrtizSerrano.OrtizSerranoTP3.controller;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.CreateUserRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.AtenderResetRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.MessageResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Administrador;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Docente;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Estudiante;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.SolicitudResetPassword;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Usuario;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.AdministradorRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.DocenteRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.EstudianteRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.SolicitudResetPasswordRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Gestión de usuarios por parte del administrador.
 * El registro público está deshabilitado; solo el administrador puede crear usuarios.
 *
 * Roles soportados al crear: ESTUDIANTE, DOCENTE, ADMINISTRADOR
 */
@Tag(name = "Admin – Usuarios", description = "Creación y gestión de usuarios (requiere rol ADMINISTRADOR)")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasAuthority('ADMINISTRADOR')")
public class AdminUsuariosController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SolicitudResetPasswordRepository solicitudResetRepository;

    // ─── LISTAR TODOS LOS USUARIOS ────────────────────────────────────────

    @Operation(summary = "Listar usuarios", description = "Devuelve todos los usuarios registrados en el sistema.")
    @GetMapping
    public ResponseEntity<List<Usuario>> listarTodos() {
        return ResponseEntity.ok(usuarioRepository.findAll());
    }

    // ─── LISTAR DOCENTES ──────────────────────────────────────────────────

    @Operation(summary = "Listar docentes", description = "Devuelve todos los docentes del sistema.")
    @GetMapping("/docentes")
    public ResponseEntity<List<Docente>> listarDocentes() {
        return ResponseEntity.ok(docenteRepository.findAll());
    }

    // ─── OBTENER USUARIO POR ID ───────────────────────────────────────────

    @Operation(summary = "Obtener usuario", description = "Devuelve un usuario por su ID.")
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        return usuarioRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("Usuario no encontrado")));
    }

    // ─── CREAR USUARIO CON ROL ────────────────────────────────────────────

    @Operation(
        summary = "Crear usuario",
        description = """
            Crea un nuevo usuario y le asigna el rol indicado.
            
            **Roles válidos:** `ESTUDIANTE`, `DOCENTE`, `ADMINISTRADOR`
            
            - `ESTUDIANTE`: campos opcionales → `legajo`, `carrera`, `anioIngreso`
            - `DOCENTE`: campos opcionales → `titulo`, `especialidad`, `departamento`
            - `ADMINISTRADOR`: campos opcionales → `area`, `nivelAcceso`
            """
    )
    @PostMapping
    public ResponseEntity<?> crearUsuario(@Valid @RequestBody CreateUserRequest req) {

        if (usuarioRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: El email ya está en uso"));
        }

        String encodedPassword = passwordEncoder.encode(req.getPassword());
        String rol = req.getRol() != null ? req.getRol().toUpperCase() : "";

        switch (rol) {
            case "ESTUDIANTE" -> {
                String legajo = (req.getLegajo() != null && !req.getLegajo().isBlank())
                        ? req.getLegajo().trim() : null;
                String carrera = (req.getCarrera() != null && !req.getCarrera().isBlank())
                        ? req.getCarrera().trim() : null;

                if (legajo != null && estudianteRepository.existsByLegajo(legajo)) {
                    return ResponseEntity.badRequest()
                            .body(new MessageResponse("Error: El legajo ya está en uso"));
                }
                Estudiante e = new Estudiante(
                        req.getNombre(), req.getApellido(), req.getEmail(), encodedPassword,
                        legajo,
                        carrera,
                        req.getAnioIngreso()
                );
                estudianteRepository.save(e);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(new MessageResponse("Estudiante creado exitosamente"));
            }
            case "DOCENTE" -> {
                Docente d = new Docente(
                        req.getNombre(), req.getApellido(), req.getEmail(), encodedPassword,
                        req.getTitulo(), req.getEspecialidad(), req.getDepartamento()
                );
                docenteRepository.save(d);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(new MessageResponse("Docente creado exitosamente"));
            }
            case "ADMINISTRADOR" -> {
                Administrador a = new Administrador(
                        req.getNombre(), req.getApellido(), req.getEmail(), encodedPassword,
                        "ADMINISTRADOR",
                        req.getArea(),
                        req.getNivelAcceso() != null ? req.getNivelAcceso() : 1
                );
                administradorRepository.save(a);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(new MessageResponse("Administrador creado exitosamente"));
            }
            default -> {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Rol inválido. Use ESTUDIANTE, DOCENTE o ADMINISTRADOR"));
            }
        }
    }

    // ─── ACTIVAR / DESACTIVAR USUARIO ────────────────────────────────────

    @Operation(summary = "Desactivar usuario", description = "Baja lógica: marca el usuario como inactivo.")
    @PutMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivar(@PathVariable Long id) {
        return usuarioRepository.findById(id).map(u -> {
            u.setActivo(false);
            usuarioRepository.save(u);
            return ResponseEntity.ok(new MessageResponse("Usuario desactivado"));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse("Usuario no encontrado")));
    }

    @Operation(summary = "Reactivar usuario", description = "Reactiva un usuario previamente desactivado.")
    @PutMapping("/{id}/reactivar")
    public ResponseEntity<?> reactivar(@PathVariable Long id) {
        return usuarioRepository.findById(id).map(u -> {
            u.setActivo(true);
            usuarioRepository.save(u);
            return ResponseEntity.ok(new MessageResponse("Usuario reactivado"));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse("Usuario no encontrado")));
    }

    // ─── ELIMINAR USUARIO (FÍSICO) ────────────────────────────────────────

    @Operation(summary = "Eliminar usuario", description = "Baja física: elimina permanentemente el usuario.")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        return usuarioRepository.findById(id).map(u -> {
            usuarioRepository.delete(u);
            return ResponseEntity.ok(new MessageResponse("Usuario eliminado permanentemente"));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse("Usuario no encontrado")));
    }

    // ─── SOLICITUDES DE RESET DE CONTRASEÑA ──────────────────────────────

    /**
     * GET /api/admin/usuarios/solicitudes-reset
     * Lista todas las solicitudes de "Olvidé mi contraseña" (pendientes primero).
     */
    @Operation(
        summary = "Listar solicitudes de reset",
        description = "Devuelve todas las solicitudes de recuperación de contraseña. " +
                      "Las pendientes aparecen listadas primero."
    )
    @GetMapping("/solicitudes-reset")
    public ResponseEntity<List<SolicitudResetPassword>> listarSolicitudesReset() {
        List<SolicitudResetPassword> pendientes = solicitudResetRepository
                .findByEstadoOrderByFechaSolicitudDesc(SolicitudResetPassword.Estado.PENDIENTE);
        List<SolicitudResetPassword> atendidas = solicitudResetRepository
                .findByEstadoOrderByFechaSolicitudDesc(SolicitudResetPassword.Estado.ATENDIDA);
        pendientes.addAll(atendidas);
        return ResponseEntity.ok(pendientes);
    }

    /**
     * POST /api/admin/usuarios/solicitudes-reset/{id}/atender
     * El admin asigna una nueva contraseña al usuario y marca la solicitud como atendida.
     */
    @Operation(
        summary = "Atender solicitud de reset",
        description = "El administrador establece una nueva contraseña para el usuario solicitante " +
                      "y marca la solicitud como ATENDIDA."
    )
    @PostMapping("/solicitudes-reset/{id}/atender")
    public ResponseEntity<?> atenderSolicitudReset(
            @PathVariable Long id,
            @Valid @RequestBody AtenderResetRequest req) {

        SolicitudResetPassword solicitud = solicitudResetRepository.findById(id)
                .orElse(null);
        if (solicitud == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Solicitud no encontrada"));
        }
        if (solicitud.getEstado() == SolicitudResetPassword.Estado.ATENDIDA) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("La solicitud ya fue atendida"));
        }

        // Actualizar contraseña del usuario
        Usuario usuario = usuarioRepository.findByEmail(solicitud.getEmail()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Usuario con email " + solicitud.getEmail() + " no encontrado"));
        }

        usuario.setPassword(passwordEncoder.encode(req.getNuevaPassword()));
        usuarioRepository.save(usuario);

        // Marcar solicitud como atendida
        solicitud.setEstado(SolicitudResetPassword.Estado.ATENDIDA);
        solicitud.setFechaAtencion(LocalDateTime.now());
        solicitudResetRepository.save(solicitud);

        return ResponseEntity.ok(new MessageResponse(
            "Contraseña actualizada para " + usuario.getNombre() + " " + usuario.getApellido() +
            " (" + usuario.getEmail() + "). La solicitud fue marcada como ATENDIDA."));
    }
}
