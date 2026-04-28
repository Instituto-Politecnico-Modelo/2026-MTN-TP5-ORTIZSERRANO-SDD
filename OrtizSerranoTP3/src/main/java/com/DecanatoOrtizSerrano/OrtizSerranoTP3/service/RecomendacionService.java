package com.DecanatoOrtizSerrano.OrtizSerranoTP3.service;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Inscripcion;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Materia;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.InscripcionRepository;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.MateriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de recomendación de materias optativas.
 *
 * <p>Algoritmo basado en el historial del propio estudiante (no requiere
 * datos de otros estudiantes ni ML externo):</p>
 *
 * <ol>
 *   <li><b>Filtro base</b>: excluye materias ya inscriptas (cualquier estado).</li>
 *   <li><b>Score por año</b>: prioriza materias del siguiente año académico
 *       respecto al año más alto aprobado por el estudiante.</li>
 *   <li><b>Score por cuatrimestre</b>: penaliza si el cuatrimestre es distinto
 *       al más frecuente del historial aprobado.</li>
 *   <li><b>Score por créditos</b>: bonifica materias con rango de créditos
 *       similar al promedio histórico del estudiante (±2 créditos).</li>
 *   <li><b>Score por docente conocido</b>: bonifica si el estudiante aprobó
 *       otra materia con el mismo docente.</li>
 *   <li><b>Disponibilidad</b>: penaliza (no elimina) materias sin cupos libres,
 *       para que el alumno sepa que puede anotarse en la cola.</li>
 * </ol>
 *
 * <p>Se devuelven las top-{@value #MAX_SUGERENCIAS} sugerencias ordenadas
 * por score descendente.</p>
 */
@Service
public class RecomendacionService {

    static final int MAX_SUGERENCIAS = 6;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private MateriaRepository materiaRepository;

    // ── DTO interno ───────────────────────────────────────────────────────────

    public record RecomendacionItem(
            Long   idMateria,
            String codigo,
            String nombre,
            String descripcion,
            Integer anio,
            Integer cuatrimestre,
            Integer creditos,
            String  docenteNombre,
            String  docenteApellido,
            Integer cuposDisponibles,
            boolean hayLugar,
            double  score,
            String  razon
    ) {}

    // ── Método principal ──────────────────────────────────────────────────────

    /**
     * Genera hasta {@value #MAX_SUGERENCIAS} sugerencias de materias para el
     * estudiante dado, ordenadas por relevancia descendente.
     *
     * @param idEstudiante ID del estudiante autenticado
     * @return lista de sugerencias con score y razón explicativa
     */
    public List<RecomendacionItem> recomendar(Long idEstudiante) {

        // ── 1. Historial del estudiante ───────────────────────────────────────
        List<Inscripcion> historial = inscripcionRepository.findByEstudianteIdUsuario(idEstudiante);

        Set<Long> idsMateriasCursadas = historial.stream()
                .map(i -> i.getMateria().getIdMateria())
                .collect(Collectors.toSet());

        List<Inscripcion> aprobadas = historial.stream()
                .filter(i -> "APROBADA".equals(i.getEstado()))
                .toList();

        // ── 2. Métricas derivadas del historial ───────────────────────────────
        int anioMaxAprobado = aprobadas.stream()
                .map(i -> i.getMateria().getAnio())
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(1);

        double promedioCreditos = aprobadas.stream()
                .map(i -> i.getMateria().getCreditos())
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(6.0);

        // Cuatrimestre más frecuente en materias aprobadas
        int cuatrimestreFrecuente = aprobadas.stream()
                .map(i -> i.getMateria().getCuatrimestre())
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(1);

        // Docentes cuyos cursos el estudiante aprobó
        Set<Long> docentesConocidos = aprobadas.stream()
                .map(i -> i.getMateria().getDocente())
                .filter(Objects::nonNull)
                .map(d -> d.getIdUsuario())
                .collect(Collectors.toSet());

        // ── 3. Candidatas: todas las materias que NO cursó ────────────────────
        List<Materia> candidatas = materiaRepository.findAll().stream()
                .filter(m -> !idsMateriasCursadas.contains(m.getIdMateria()))
                .toList();

        // ── 4. Puntuar cada candidata ─────────────────────────────────────────
        List<RecomendacionItem> resultado = new ArrayList<>();

        for (Materia m : candidatas) {
            double score = 0.0;
            List<String> razones = new ArrayList<>();

            // a) Año: +3 si es el siguiente año, +1 si es el mismo año, -1 si es anterior
            if (m.getAnio() != null) {
                if (m.getAnio() == anioMaxAprobado + 1) {
                    score += 3.0;
                    razones.add("Es del año " + m.getAnio() + " (siguiente en tu trayectoria)");
                } else if (m.getAnio() == anioMaxAprobado) {
                    score += 1.0;
                    razones.add("Es del mismo año que tus aprobadas");
                } else if (m.getAnio() < anioMaxAprobado) {
                    score -= 1.0;
                }
            }

            // b) Cuatrimestre frecuente: +1 si coincide
            if (m.getCuatrimestre() != null && m.getCuatrimestre() == cuatrimestreFrecuente) {
                score += 1.0;
                razones.add("Cuatrimestre " + m.getCuatrimestre() + " (el que más cursás)");
            }

            // c) Créditos similares: +1 si están en rango ±2
            if (m.getCreditos() != null && Math.abs(m.getCreditos() - promedioCreditos) <= 2) {
                score += 1.0;
                razones.add("Créditos similares a tu promedio (" + m.getCreditos() + " créditos)");
            }

            // d) Docente conocido: +2
            if (m.getDocente() != null && docentesConocidos.contains(m.getDocente().getIdUsuario())) {
                score += 2.0;
                razones.add("El docente ya dictó materias que aprobaste");
            }

            // e) Disponibilidad: -0.5 si no hay cupos (sigue siendo sugerida pero con menor prioridad)
            long cuposOcupados = inscripcionRepository.countCuposOcupados(m.getIdMateria());
            int cuposDisponibles = (m.getCuposMaximos() == null)
                    ? Integer.MAX_VALUE
                    : Math.max(0, m.getCuposMaximos() - (int) cuposOcupados);
            boolean hayLugar = m.getCuposMaximos() == null || cuposDisponibles > 0;

            if (!hayLugar) {
                score -= 0.5;
                razones.add("Sin cupos libres (podés unirte a la cola)");
            } else if (cuposDisponibles != Integer.MAX_VALUE && cuposDisponibles <= 5) {
                razones.add("¡Solo quedan " + cuposDisponibles + " cupos!");
            }

            // Solo recomendar si tiene score positivo
            if (score <= 0.0) continue;

            String razonPrincipal = razones.isEmpty()
                    ? "Materia disponible en el catálogo"
                    : String.join(" · ", razones);

            String docenteNombre   = m.getDocente() != null ? m.getDocente().getNombre()   : null;
            String docenteApellido = m.getDocente() != null ? m.getDocente().getApellido() : null;

            resultado.add(new RecomendacionItem(
                    m.getIdMateria(),
                    m.getCodigo(),
                    m.getNombre(),
                    m.getDescripcion(),
                    m.getAnio(),
                    m.getCuatrimestre(),
                    m.getCreditos(),
                    docenteNombre,
                    docenteApellido,
                    cuposDisponibles == Integer.MAX_VALUE ? null : cuposDisponibles,
                    hayLugar,
                    score,
                    razonPrincipal
            ));
        }

        // ── 5. Ordenar por score desc y limitar ───────────────────────────────
        resultado.sort(Comparator.comparingDouble(RecomendacionItem::score).reversed());
        return resultado.stream().limit(MAX_SUGERENCIAS).toList();
    }
}
