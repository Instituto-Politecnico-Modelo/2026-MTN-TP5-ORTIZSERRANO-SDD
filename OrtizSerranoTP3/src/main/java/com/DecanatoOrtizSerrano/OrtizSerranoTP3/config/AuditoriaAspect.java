package com.DecanatoOrtizSerrano.OrtizSerranoTP3.config;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.AuditoriaService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Aspecto AOP que intercepta automáticamente métodos clave
 * de los services para registrar eventos en la auditoría encadenada.
 *
 * Patrones interceptados:
 *  - InscripcionService:  inscribir, cancelar, cargarNota, cerrarNota
 *  - MateriaService:      crear, actualizar, eliminar
 *  - EstudianteService:   crear, actualizar, eliminar
 *  - PeriodoService:      crear, actualizar, eliminar
 *  - AuditorioService:    crear, actualizar, eliminar, toggleActivo
 *  - ReservaAuditorioService: crear, cambiarEstado, cancelar
 */
@Aspect
@Component
public class AuditoriaAspect {

    @Autowired
    private AuditoriaService auditoriaService;

    // ─── InscripcionService ──────────────────────────────────────────────────

    @Around("execution(* com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.InscripcionService.inscribir(..))")
    public Object auditarInscribir(ProceedingJoinPoint jp) throws Throwable {
        Object result = jp.proceed();
        try {
            Long idInscripcion = extraerIdLong(result, "getIdInscripcion");
            registrar("Inscripcion", idInscripcion, "INSCRIBIR",
                "Nuevo alumno inscripto en materia");
        } catch (Exception ignored) { /* no interrumpir el flujo principal */ }
        return result;
    }

    @Around("execution(* com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.InscripcionService.cancelar(..))")
    public Object auditarCancelarInscripcion(ProceedingJoinPoint jp) throws Throwable {
        Object result = jp.proceed();
        try {
            Long id = extraerIdLong(result, "getIdInscripcion");
            registrar("Inscripcion", id, "CANCELAR", "Inscripción cancelada");
        } catch (Exception ignored) { /* no interrumpir */ }
        return result;
    }

    @Around("execution(* com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.InscripcionService.cargarNota(..))")
    public Object auditarCargarNota(ProceedingJoinPoint jp) throws Throwable {
        Object[] args = jp.getArgs();
        Object result = jp.proceed();
        try {
            Long idInscripcion = (Long) args[0];
            registrar("Inscripcion", idInscripcion, "CARGAR_NOTA",
                "Docente cargó/actualizó nota de inscripción");
        } catch (Exception ignored) { /* no interrumpir */ }
        return result;
    }

    @Around("execution(* com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.InscripcionService.cerrarNota(..))")
    public Object auditarCerrarNota(ProceedingJoinPoint jp) throws Throwable {
        Object[] args = jp.getArgs();
        Object result = jp.proceed();
        try {
            Long idInscripcion = (Long) args[0];
            registrar("Inscripcion", idInscripcion, "CERRAR_NOTA",
                "Docente cerró definitivamente la nota");
        } catch (Exception ignored) { /* no interrumpir */ }
        return result;
    }

    // ─── MateriaService ──────────────────────────────────────────────────────

    @Around("execution(* com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.MateriaService.crear(..))")
    public Object auditarCrearMateria(ProceedingJoinPoint jp) throws Throwable {
        Object result = jp.proceed();
        try {
            Long id = extraerIdLong(result, "getIdMateria");
            registrar("Materia", id, "CREAR", "Materia creada");
        } catch (Exception ignored) { /* no interrumpir */ }
        return result;
    }

    @Around("execution(* com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.MateriaService.actualizar(..))")
    public Object auditarActualizarMateria(ProceedingJoinPoint jp) throws Throwable {
        Object[] args = jp.getArgs();
        Object result = jp.proceed();
        try {
            registrar("Materia", (Long) args[0], "MODIFICAR", "Materia modificada");
        } catch (Exception ignored) { /* no interrumpir */ }
        return result;
    }

    @Around("execution(* com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.MateriaService.eliminar(..))")
    public Object auditarEliminarMateria(ProceedingJoinPoint jp) throws Throwable {
        Object[] args = jp.getArgs();
        jp.proceed();
        try {
            registrar("Materia", (Long) args[0], "ELIMINAR", "Materia eliminada");
        } catch (Exception ignored) { /* no interrumpir */ }
        return null;
    }

    // ─── EstudianteService ───────────────────────────────────────────────────

    @Around("execution(* com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.EstudianteService.crear(..))")
    public Object auditarCrearEstudiante(ProceedingJoinPoint jp) throws Throwable {
        Object result = jp.proceed();
        try {
            Long id = extraerIdLong(result, "getIdUsuario");
            registrar("Estudiante", id, "CREAR", "Estudiante registrado");
        } catch (Exception ignored) { /* no interrumpir */ }
        return result;
    }

    @Around("execution(* com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.EstudianteService.actualizar(..))")
    public Object auditarActualizarEstudiante(ProceedingJoinPoint jp) throws Throwable {
        Object[] args = jp.getArgs();
        Object result = jp.proceed();
        try {
            registrar("Estudiante", (Long) args[0], "MODIFICAR", "Datos de estudiante modificados");
        } catch (Exception ignored) { /* no interrumpir */ }
        return result;
    }

    @Around("execution(* com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.EstudianteService.eliminar(..))")
    public Object auditarEliminarEstudiante(ProceedingJoinPoint jp) throws Throwable {
        Object[] args = jp.getArgs();
        jp.proceed();
        try {
            registrar("Estudiante", (Long) args[0], "ELIMINAR", "Estudiante eliminado");
        } catch (Exception ignored) { /* no interrumpir */ }
        return null;
    }

    // ─── AuditorioService ────────────────────────────────────────────────────

    @Around("execution(* com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.AuditorioService.crear(..))")
    public Object auditarCrearAuditorio(ProceedingJoinPoint jp) throws Throwable {
        Object result = jp.proceed();
        try {
            Long id = extraerIdLong(result, "getIdAuditorio");
            registrar("Auditorio", id, "CREAR", "Auditorio creado");
        } catch (Exception ignored) { /* no interrumpir */ }
        return result;
    }

    @Around("execution(* com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.AuditorioService.actualizar(..))")
    public Object auditarActualizarAuditorio(ProceedingJoinPoint jp) throws Throwable {
        Object[] args = jp.getArgs();
        Object result = jp.proceed();
        try {
            registrar("Auditorio", (Long) args[0], "MODIFICAR", "Auditorio modificado");
        } catch (Exception ignored) { /* no interrumpir */ }
        return result;
    }

    @Around("execution(* com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.AuditorioService.eliminar(..))")
    public Object auditarEliminarAuditorio(ProceedingJoinPoint jp) throws Throwable {
        Object[] args = jp.getArgs();
        jp.proceed();
        try {
            registrar("Auditorio", (Long) args[0], "ELIMINAR", "Auditorio eliminado");
        } catch (Exception ignored) { /* no interrumpir */ }
        return null;
    }

    // ─── ReservaAuditorioService ─────────────────────────────────────────────

    @Around("execution(* com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.ReservaAuditorioService.crear(..))")
    public Object auditarCrearReserva(ProceedingJoinPoint jp) throws Throwable {
        Object result = jp.proceed();
        try {
            Long id = extraerIdLong(result, "getIdReserva");
            registrar("ReservaAuditorio", id, "CREAR", "Reserva de auditorio creada (PENDIENTE)");
        } catch (Exception ignored) { /* no interrumpir */ }
        return result;
    }

    @Around("execution(* com.DecanatoOrtizSerrano.OrtizSerranoTP3.service.ReservaAuditorioService.cambiarEstado(..))")
    public Object auditarCambiarEstadoReserva(ProceedingJoinPoint jp) throws Throwable {
        Object[] args = jp.getArgs();
        Object result = jp.proceed();
        try {
            String nuevoEstado = args.length > 1 ? String.valueOf(args[1]) : "?";
            registrar("ReservaAuditorio", (Long) args[0], "CAMBIAR_ESTADO",
                "Estado de reserva cambiado a: " + nuevoEstado);
        } catch (Exception ignored) { /* no interrumpir */ }
        return result;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Registra un evento usando el usuario autenticado del contexto de seguridad.
     */
    private void registrar(String entidad, Long idEntidad, String accion, String descripcion) {
        Long idUsuario = null;
        String email = "sistema";

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails ud) {
                email = ud.getUsername();
                // Intentar obtener el ID si es nuestro UserDetailsImpl
                try {
                    idUsuario = (Long) ud.getClass().getMethod("getId").invoke(ud);
                } catch (Exception ignored) { /* no crítico */ }
            }
        } catch (Exception ignored) { /* no interrumpir el flujo */ }

        auditoriaService.registrar(entidad, idEntidad, accion, descripcion, idUsuario, email);
    }

    /**
     * Extrae un Long de un objeto usando reflexión sobre un getter.
     */
    private Long extraerIdLong(Object obj, String getterName) {
        if (obj == null) return null;
        try {
            return (Long) obj.getClass().getMethod(getterName).invoke(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
