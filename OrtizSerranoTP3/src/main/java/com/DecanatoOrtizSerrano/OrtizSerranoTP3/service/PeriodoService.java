package com.DecanatoOrtizSerrano.OrtizSerranoTP3.service;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto.PeriodoRequest;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Periodo;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.PeriodoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PeriodoService {

    @Autowired
    private PeriodoRepository periodoRepository;

    public List<Periodo> listarTodos() {
        return periodoRepository.findAll();
    }

    public List<Periodo> listarActivos() {
        return periodoRepository.findByActivoTrue();
    }

    public Periodo obtenerPorId(Long id) {
        return periodoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Período no encontrado con id: " + id));
    }

    @Transactional
    public Periodo crear(PeriodoRequest request) {
        Periodo periodo = new Periodo(
            request.getNombre(),
            request.getAnio(),
            request.getCuatrimestre(),
            request.getFechaInicio(),
            request.getFechaFin()
        );
        return periodoRepository.save(periodo);
    }

    @Transactional
    public Periodo actualizar(Long id, PeriodoRequest request) {
        Periodo periodo = obtenerPorId(id);
        periodo.setNombre(request.getNombre());
        periodo.setAnio(request.getAnio());
        periodo.setCuatrimestre(request.getCuatrimestre());
        periodo.setFechaInicio(request.getFechaInicio());
        periodo.setFechaFin(request.getFechaFin());
        return periodoRepository.save(periodo);
    }

    @Transactional
    public void eliminar(Long id) {
        Periodo periodo = obtenerPorId(id);
        periodoRepository.delete(periodo);
    }

    @Transactional
    public Periodo toggleActivo(Long id) {
        Periodo periodo = obtenerPorId(id);
        periodo.setActivo(!periodo.getActivo());
        return periodoRepository.save(periodo);
    }
}
