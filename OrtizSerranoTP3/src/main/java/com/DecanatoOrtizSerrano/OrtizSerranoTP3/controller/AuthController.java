package com.DecanatoOrtizSerrano.OrtizSerranoTP3.controller;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.JwtResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.LoginRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.MessageResponse;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.SignupRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Usuario;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.UsuarioRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.security.UserDetailsImpl;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.security.jwt.JwtUtil;
import jakarta.validation.Valid;
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
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder encoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * POST /api/auth/login - Autenticar usuario y devolver JWT
     */
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
        
        return ResponseEntity.ok(new JwtResponse(
            jwt,
            userDetails.getId(),
            userDetails.getEmail(),
            usuario.getNombre(),
            usuario.getApellido()
        ));
    }
    
    /**
     * POST /api/auth/signup - Registrar nuevo usuario
     */
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
}
