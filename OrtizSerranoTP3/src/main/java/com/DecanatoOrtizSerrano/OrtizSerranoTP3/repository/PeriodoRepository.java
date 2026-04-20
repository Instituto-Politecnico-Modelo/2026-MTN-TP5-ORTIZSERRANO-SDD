package com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Periodo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PeriodoRepository extends JpaRepository<Periodo, Long> {
    List<Periodo> findByActivoTrue();
    List<Periodo> findByAnio(Integer anio);
}
