package com.DecanatoOrtizSerrano.OrtizSerranoTP3.service;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.MateriaRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Docente;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Materia;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.DocenteRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.MateriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MateriaService {

    @Autowired
    private MateriaRepository materiaRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    public List<Materia> listarTodas() {
        return materiaRepository.findAll();
    }

    public Materia obtenerPorId(Long id) {
        return materiaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Materia no encontrada con id: " + id));
    }

    @Transactional
    public Materia crear(MateriaRequest request) {
        if (materiaRepository.existsByCodigo(request.getCodigo())) {
            throw new RuntimeException("Ya existe una materia con el código: " + request.getCodigo());
        }

        Materia materia = new Materia();
        mapearCampos(materia, request);
        return materiaRepository.save(materia);
    }

    @Transactional
    public Materia actualizar(Long id, MateriaRequest request) {
        Materia materia = obtenerPorId(id);

        // Si cambia el código, verificar que no esté en uso
        if (!materia.getCodigo().equals(request.getCodigo())
                && materiaRepository.existsByCodigo(request.getCodigo())) {
            throw new RuntimeException("Ya existe una materia con el código: " + request.getCodigo());
        }

        mapearCampos(materia, request);
        return materiaRepository.save(materia);
    }

    @Transactional
    public void eliminar(Long id) {
        Materia materia = obtenerPorId(id);
        materiaRepository.delete(materia);
    }

    private void mapearCampos(Materia materia, MateriaRequest request) {
        materia.setCodigo(request.getCodigo());
        materia.setNombre(request.getNombre());
        materia.setDescripcion(request.getDescripcion());
        materia.setCreditos(request.getCreditos());
        materia.setAnio(request.getAnio());
        materia.setCuatrimestre(request.getCuatrimestre());
        materia.setCuposMaximos(request.getCuposMaximos()); // ← optimistic locking

        if (request.getIdDocente() != null) {
            Docente docente = docenteRepository.findById(request.getIdDocente())
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
            materia.setDocente(docente);
        } else {
            materia.setDocente(null);
        }
    }
}
