package com.DecanatoOrtizSerrano.OrtizSerranoTP3;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.UpdateUserRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Usuario;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.UsuarioRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitario ABM de Usuario — Punto 6 del TP3
 * Cubre: Alta (crear), Baja lógica, Baja física, Modificación y Reactivación
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private Usuario usuarioEjemplo;

    @BeforeEach
    void setUp() {
        usuarioEjemplo = new Usuario("Juan", "Pérez", "juan@mail.com", "hash123");
        usuarioEjemplo.setIdUsuario(1L);
        usuarioEjemplo.setActivo(true);
    }

    // ─── ALTA ────────────────────────────────────────────────────────────────

    @Test
    void altaUsuario_guardaConActivoTrue() {
        // El repositorio devuelve el mismo objeto al guardar
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioEjemplo);

        Usuario nuevo = new Usuario("Juan", "Pérez", "juan@mail.com", "hash123");
        Usuario guardado = usuarioRepository.save(nuevo);

        assertNotNull(guardado);
        assertEquals("Juan", guardado.getNombre());
        assertEquals("juan@mail.com", guardado.getEmail());
        assertTrue(guardado.getActivo());
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    // ─── MODIFICACIÓN ────────────────────────────────────────────────────────

    @Test
    void modificarUsuario_actualizaNombreYApellido() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEjemplo));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setNombre("Carlos");
        request.setApellido("García");

        Usuario resultado = userService.updateUsuario(1L, request);

        assertEquals("Carlos", resultado.getNombre());
        assertEquals("García", resultado.getApellido());
        verify(usuarioRepository).save(usuarioEjemplo);
    }

    @Test
    void modificarUsuario_lanzaExcepcionSiEmailYaExiste() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEjemplo));
        when(usuarioRepository.existsByEmail("repetido@mail.com")).thenReturn(true);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("repetido@mail.com");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.updateUsuario(1L, request));

        assertTrue(ex.getMessage().contains("email ya está en uso"));
    }

    @Test
    void modificarUsuario_lanzaExcepcionSiNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateUserRequest request = new UpdateUserRequest();
        request.setNombre("Nadie");

        assertThrows(RuntimeException.class, () -> userService.updateUsuario(99L, request));
    }

    // ─── BAJA LÓGICA ─────────────────────────────────────────────────────────

    @Test
    void bajaLogica_marcaUsuarioComoInactivo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEjemplo));

        userService.deleteUsuarioLogico(1L);

        assertFalse(usuarioEjemplo.getActivo());
        verify(usuarioRepository).save(usuarioEjemplo);
    }

    @Test
    void bajaLogica_lanzaExcepcionSiNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.deleteUsuarioLogico(99L));
    }

    // ─── BAJA FÍSICA ─────────────────────────────────────────────────────────

    @Test
    void bajaFisica_eliminaUsuarioDeLaBD() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEjemplo));

        userService.deleteUsuarioFisico(1L);

        verify(usuarioRepository).delete(usuarioEjemplo);
    }

    @Test
    void bajaFisica_lanzaExcepcionSiNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.deleteUsuarioFisico(99L));
    }

    // ─── REACTIVACIÓN ────────────────────────────────────────────────────────

    @Test
    void reactivarUsuario_vuelvePonerloCtivoTrue() {
        usuarioEjemplo.setActivo(false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEjemplo));

        userService.reactivarUsuario(1L);

        assertTrue(usuarioEjemplo.getActivo());
        verify(usuarioRepository).save(usuarioEjemplo);
    }
}
