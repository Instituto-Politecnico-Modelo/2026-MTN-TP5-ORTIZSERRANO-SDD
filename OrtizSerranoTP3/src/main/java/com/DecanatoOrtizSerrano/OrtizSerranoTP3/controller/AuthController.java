package com.DecanatoOrtizSerrano.OrtizSerranoTP3.controller;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.JwtResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.LoginRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.MessageResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.SignupRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.UpdateUserRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Usuario;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.AdministradorRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.DocenteRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.EstudianteRepository;
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
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para autenticación (Login y Registro)
 */
@Tag(name = "Autenticación", description = "Login, registro y gestión del usuario autenticado")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private EstudianteRepository estudianteRepository;
    
    @Autowired
    private DocenteRepository docenteRepository;
    
    @Autowired
    private AdministradorRepository administradorRepository;
    
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
        String jwt = jwtUtil.generateJwtToken(authentication);
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        // Obtener información adicional del usuario
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getEmail())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Detectar el rol según el tipo de entidad
        String role;
        if (administradorRepository.existsById(userDetails.getId())) {
            role = "ADMINISTRADOR";
        } else if (docenteRepository.existsById(userDetails.getId())) {
            role = "DOCENTE";
        } else if (estudianteRepository.existsById(userDetails.getId())) {
            role = "ESTUDIANTE";
        } else {
            role = "USUARIO";
        }
        
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
     * POST /api/auth/signup - Registrar nuevo usuario
     */
    @Operation(summary = "Registrar usuario", description = "Crea un nuevo usuario genérico (sin rol específico).")
    @ApiResponse(responseCode = "200", description = "Usuario registrado exitosamente")
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        
        if (usuarioRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                .badRequest()
                .body(new MessageResponse("Error: El email ya está en uso"));
        }
        
        // Crear nuevo usuario
        Usuario usuario = new Usuario(
            signUpRequest.getNombre(),
            signUpRequest.getApellido(),
            signUpRequest.getEmail(),
            encoder.encode(signUpRequest.getPassword())
        );
        
        usuarioRepository.save(usuario);
        
        return ResponseEntity.ok(new MessageResponse("Usuario registrado exitosamente"));
    }
    
    /**
     * GET /api/auth/me - Obtener información del usuario autenticado
     */
    @Operation(summary = "Perfil actual", description = "Devuelve los datos del usuario que envía el token JWT.")
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(new MessageResponse("No autenticado"));
        }
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getEmail())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        return ResponseEntity.ok(usuario);
    }
    
    /**
     * PUT /api/auth/update - Actualizar información del usuario autenticado
     */
    @Operation(summary = "Actualizar perfil", description = "Modifica nombre, apellido o contraseña del usuario autenticado.")
    @PutMapping("/update")
    public ResponseEntity<?> updateUser(
            @Valid @RequestBody UpdateUserRequest updateRequest,
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(401).body(new MessageResponse("No autenticado"));
        }
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        try {
            Usuario updatedUser = userService.updateUsuario(userDetails.getId(), updateRequest);
            return ResponseEntity.ok(new MessageResponse("Usuario actualizado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
    
    /**
     * DELETE /api/auth/delete - Baja lógica del usuario (marca como inactivo)
     */
    @Operation(summary = "Desactivar cuenta", description = "Baja lógica: marca el usuario como inactivo sin borrarlo.")
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUserLogico(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(new MessageResponse("No autenticado"));
        }
        
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
    @DeleteMapping("/delete/physical")
    public ResponseEntity<?> deleteUserFisico(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(new MessageResponse("No autenticado"));
        }
        
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
    @PutMapping("/reactivate/{id}")
    public ResponseEntity<?> reactivateUser(@PathVariable Long id) {
        try {
            userService.reactivarUsuario(id);
            return ResponseEntity.ok(new MessageResponse("Usuario reactivado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
}
