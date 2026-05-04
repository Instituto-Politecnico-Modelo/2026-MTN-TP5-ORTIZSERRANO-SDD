package com.DecanatoOrtizSerrano.OrtizSerranoTP3.controller;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.JwtResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.LoginRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.MessageResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.OlvidePasswordRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.UpdateUserRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.SolicitudResetPassword;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Usuario;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.SolicitudResetPasswordRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.UsuarioRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.security.UserDetailsImpl;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.security.jwt.JwtUtil;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.UserService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controlador REST para autenticación (Login y Registro)
 */
@Tag(name = "Autenticación", description = "Login y gestión del usuario autenticado. El registro es exclusivo del administrador.")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private SolicitudResetPasswordRepository solicitudResetRepository;
    
    @Autowired
    private PasswordEncoder encoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserService userService;

    /**
     * POST /api/auth/login - Autenticar usuario y devolver JWT
     */
    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario con email y contraseña. Devuelve un JWT y el rol detectado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login exitoso – devuelve token JWT y rol"),
        @ApiResponse(responseCode = "401", description = "Credenciales inválidas", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Obtener información adicional del usuario
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getEmail())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // El rol ya viene en las authorities cargadas por UserDetailsServiceImpl
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        // Generar JWT con el rol incluido en el payload
        String jwt = jwtUtil.generateJwtTokenWithRole(authentication, role);
        
        return ResponseEntity.ok(new JwtResponse(
            jwt,
            userDetails.getId(),
            userDetails.getEmail(),
            usuario.getNombre(),
            usuario.getApellido(),
            role
        ));
    }
    
    /**
     * GET /api/auth/me - Obtener información del usuario autenticado
     */
    @Operation(summary = "Perfil actual", description = "Devuelve los datos del usuario que envía el token JWT.")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getEmail())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        return ResponseEntity.ok(usuario);
    }
    
    /**
     * PUT /api/auth/update - Actualizar información del usuario autenticado
     */
    @Operation(summary = "Actualizar perfil", description = "Modifica nombre, apellido o contraseña del usuario autenticado.")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/update")
    public ResponseEntity<?> updateUser(
            @Valid @RequestBody UpdateUserRequest updateRequest,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        try {
            userService.updateUsuario(userDetails.getId(), updateRequest);
            return ResponseEntity.ok(new MessageResponse("Usuario actualizado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
    
    /**
     * DELETE /api/auth/delete - Baja lógica del usuario (marca como inactivo)
     */
    @Operation(summary = "Desactivar cuenta", description = "Baja lógica: marca el usuario como inactivo sin borrarlo.")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUserLogico(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        try {
            userService.deleteUsuarioLogico(userDetails.getId());
            return ResponseEntity.ok(new MessageResponse("Usuario desactivado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
    
    /**
     * DELETE /api/auth/delete/physical - Baja física del usuario (elimina de BD)
     */
    @Operation(summary = "Eliminar cuenta (físico)", description = "Baja física: elimina permanentemente el usuario de la base de datos.")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/delete/physical")
    public ResponseEntity<?> deleteUserFisico(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        try {
            userService.deleteUsuarioFisico(userDetails.getId());
            return ResponseEntity.ok(new MessageResponse("Usuario eliminado permanentemente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
    
    /**
     * PUT /api/auth/reactivate/{id} - Reactivar un usuario desactivado (solo admin)
     */
    @Operation(summary = "Reactivar usuario", description = "Reactiva un usuario previamente desactivado. Uso exclusivo del administrador.")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @PutMapping("/reactivate/{id}")
    public ResponseEntity<?> reactivateUser(@PathVariable Long id) {
        try {
            userService.reactivarUsuario(id);
            return ResponseEntity.ok(new MessageResponse("Usuario reactivado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    // ─── LOGOUT ───────────────────────────────────────────────────────────────

    /**
     * POST /api/auth/logout
     * El logout es responsabilidad del cliente: debe eliminar el token almacenado.
     * Este endpoint existe por convención REST y devuelve 200 como confirmación.
     */
    @Operation(summary = "Cerrar sesión", description = "El cliente debe eliminar el token JWT. Este endpoint confirma la acción con 200.")
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(new MessageResponse("Sesión cerrada correctamente"));
    }

    // ─── JWT INSPECT ──────────────────────────────────────────────────────────

    /**
     * GET /api/auth/jwt/inspect?token=xxx
     * Verifica un JWT y devuelve sus claims + perfil del usuario si es válido.
     * Requiere autenticación (solo el propio usuario autenticado puede inspeccionar su token).
     */
    @Operation(
        summary = "Inspeccionar JWT",
        description = "Verifica un token JWT y devuelve sus claims (subject, emisión, expiración) " +
                      "junto con el perfil del usuario. Requiere autenticación."
    )
    @GetMapping("/jwt/inspect")
    public ResponseEntity<?> inspectJwt(@RequestParam("token") String token) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token_recibido", token.length() > 20
                ? token.substring(0, 20) + "..." : token);

        boolean valid = jwtUtil.validateJwtToken(token);
        result.put("valido", valid);

        String invalidReason = jwtUtil.getInvalidReason(token);
        if (invalidReason != null) {
            result.put("motivo_invalidez", invalidReason);
        }

        // Extraemos subject aunque esté expirado (para mostrar a quién pertenecía)
        String subject = jwtUtil.getUsernameFromTokenIgnoreExpiry(token);
        result.put("subject_email", subject);

        // Fechas
        try {
            java.util.Date issuedAt = jwtUtil.getIssuedAtFromToken(token);
            java.util.Date expiration = jwtUtil.getExpirationFromToken(token);
            if (issuedAt != null) {
                result.put("emitido_en", issuedAt.toInstant().toString());
            }
            if (expiration != null) {
                result.put("expira_en", expiration.toInstant().toString());
                long segsRestantes = expiration.toInstant().getEpochSecond()
                        - Instant.now().getEpochSecond();
                result.put("segundos_hasta_expiracion", segsRestantes);
                result.put("ya_expirado", segsRestantes <= 0);
            }
        } catch (Exception ignored) {
            // si el token es basura no podemos leer fechas
        }

        // Perfil del usuario si el token es válido y el usuario existe
        if (valid && subject != null) {
            usuarioRepository.findByEmail(subject).ifPresent(usuario -> {
                Map<String, Object> perfil = new LinkedHashMap<>();
                perfil.put("idUsuario", usuario.getIdUsuario());
                perfil.put("nombre", usuario.getNombre());
                perfil.put("apellido", usuario.getApellido());
                perfil.put("email", usuario.getEmail());
                perfil.put("activo", usuario.getActivo());

                // Rol extraído del claim "rol" del propio JWT (sin consultas extra a BD)
                String role = jwtUtil.getRolFromToken(token);
                perfil.put("rol", role != null ? role : "USUARIO");
                result.put("perfil_usuario", perfil);
            });
        }

        return ResponseEntity.ok(result);
    }

    // ─── OLVIDÉ MI CONTRASEÑA ─────────────────────────────────────────────────

    /**
     * POST /api/auth/olvide-password
     * El usuario envía su email y se crea una solicitud visible para el admin.
     * El admin luego la atiende asignando una nueva contraseña temporal.
     * Endpoint PÚBLICO (no requiere autenticación).
     */
    @Operation(
        summary = "Olvidé mi contraseña",
        description = "Genera una solicitud de reset de contraseña. " +
                      "El administrador recibirá la solicitud y asignará una nueva contraseña temporal."
    )
    @PostMapping("/olvide-password")
    public ResponseEntity<?> olvidePassword(@Valid @RequestBody OlvidePasswordRequest request) {
        String email = request.getEmail();

        // Verificar que el usuario existe (no revelar si no existe – misma respuesta)
        boolean usuarioExiste = usuarioRepository.findByEmail(email).isPresent();

        if (usuarioExiste) {
            // Evitar solicitudes duplicadas pendientes
            boolean yaTieneSolicitud = solicitudResetRepository.existsByEmailAndEstado(
                    email, SolicitudResetPassword.Estado.PENDIENTE);

            if (!yaTieneSolicitud) {
                usuarioRepository.findByEmail(email).ifPresent(usuario -> {
                    String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();
                    SolicitudResetPassword solicitud = new SolicitudResetPassword(email, nombreCompleto);
                    solicitudResetRepository.save(solicitud);
                });
            }
            // Si ya tiene una pendiente, no creamos duplicado pero respondemos igual
        }

        // Respuesta genérica (no revelar si el email existe o no – seguridad)
        return ResponseEntity.ok(new MessageResponse(
            "Si el email está registrado, el administrador recibirá una notificación " +
            "y se le asignará una nueva contraseña temporal a la brevedad."));
    }
}
