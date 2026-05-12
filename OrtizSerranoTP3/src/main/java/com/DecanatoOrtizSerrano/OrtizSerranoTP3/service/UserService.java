package com.DecanatoOrtizSerrano.OrtizSerranoTP3.service;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.UpdateUserRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Usuario;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para operaciones CRUD de usuarios
 */
@Service
public class UserService {
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Actualizar información del usuario
     */
    @Transactional
    public Usuario updateUsuario(Long idUsuario, UpdateUserRequest request) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        if (request.getNombre() != null && !request.getNombre().isEmpty()) {
            usuario.setNombre(request.getNombre());
        }
        
        if (request.getApellido() != null && !request.getApellido().isEmpty()) {
            usuario.setApellido(request.getApellido());
        }
        
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (usuarioRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("El email ya está en uso");
            }
            usuario.setEmail(request.getEmail());
        }
        
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            // Verificar la contraseña actual antes de permitir el cambio.
            // Protege contra el uso de sesiones robadas para tomar el control de la cuenta.
            if (request.getPasswordActual() == null || request.getPasswordActual().isEmpty()) {
                throw new RuntimeException("Debés ingresar tu contraseña actual para cambiarla");
            }
            if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
                throw new RuntimeException("La contraseña actual es incorrecta");
            }
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        return usuarioRepository.save(usuario);
    }
    
    /**
     * Baja lógica - marca el usuario como inactivo
     */
    @Transactional
    public void deleteUsuarioLogico(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }
    
    /**
     * Baja física - elimina el usuario de la base de datos
     */
    @Transactional
    public void deleteUsuarioFisico(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        usuarioRepository.delete(usuario);
    }
    
    /**
     * Reactivar un usuario dado de baja lógica
     */
    @Transactional
    public void reactivarUsuario(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }
}
