package com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    List<Inscripcion> findByEstudianteIdUsuario(Long idEstudiante);

    List<Inscripcion> findByMateriaIdMateria(Long idMateria);

    Optional<Inscripcion> findByEstudianteIdUsuarioAndMateriaIdMateria(Long idEstudiante, Long idMateria);

    boolean existsByEstudianteIdUsuarioAndMateriaIdMateriaAndEstadoNot(
        Long idEstudiante, Long idMateria, String estado);
}
