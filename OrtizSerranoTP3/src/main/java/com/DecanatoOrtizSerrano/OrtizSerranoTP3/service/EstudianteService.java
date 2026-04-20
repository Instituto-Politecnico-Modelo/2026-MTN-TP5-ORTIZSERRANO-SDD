package com.DecanatoOrtizSerrano.OrtizSerranoTP3.service;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.EstudianteRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Estudiante;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.EstudianteRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EstudianteService {

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Estudiante> listarTodos() {
        return estudianteRepository.findAll();
    }

    public Estudiante obtenerPorId(Long id) {
        return estudianteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Estudiante no encontrado con id: " + id));
    }

    @Transactional
    public Estudiante crear(EstudianteRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está en uso: " + request.getEmail());
        }
        if (estudianteRepository.existsByLegajo(request.getLegajo())) {
            throw new RuntimeException("El legajo ya está en uso: " + request.getLegajo());
        }

        Estudiante estudiante = new Estudiante(
            request.getNombre(),
            request.getApellido(),
            request.getEmail(),
            passwordEncoder.encode(request.getPassword()),
            request.getLegajo(),
            request.getCarrera(),
            request.getAnioIngreso()
        );
        return estudianteRepository.save(estudiante);
    }

    @Transactional
    public Estudiante actualizar(Long id, EstudianteRequest request) {
        Estudiante estudiante = obtenerPorId(id);

        if (!estudiante.getEmail().equals(request.getEmail())
                && usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está en uso: " + request.getEmail());
        }
        if (!estudiante.getLegajo().equals(request.getLegajo())
                && estudianteRepository.existsByLegajo(request.getLegajo())) {
            throw new RuntimeException("El legajo ya está en uso: " + request.getLegajo());
        }

        estudiante.setNombre(request.getNombre());
        estudiante.setApellido(request.getApellido());
        estudiante.setEmail(request.getEmail());
        estudiante.setLegajo(request.getLegajo());
        estudiante.setCarrera(request.getCarrera());
        if (request.getAnioIngreso() != null) {
            estudiante.setAnioIngreso(request.getAnioIngreso());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            estudiante.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return estudianteRepository.save(estudiante);
    }

    @Transactional
    public void eliminar(Long id) {
        Estudiante estudiante = obtenerPorId(id);
        estudianteRepository.delete(estudiante);
    }

    @Transactional
    public Estudiante toggleActivo(Long id) {
        Estudiante estudiante = obtenerPorId(id);
        estudiante.setActivo(!estudiante.isActivo());
        return estudianteRepository.save(estudiante);
    }
}
