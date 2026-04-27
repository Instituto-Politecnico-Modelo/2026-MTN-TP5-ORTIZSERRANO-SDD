package com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.ColaInscripcion;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para operaciones de cola virtual.
 */
public class ColaInscripcionResponse {

    private Long          idCola;
    private Long          idMateria;
    private String        nombreMateria;
    private String        codigoMateria;
    private Integer       posicion;
    private int           personasDelante;
    private String        estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaExpiracion;
    private LocalDateTime fechaResolucion;

    /** Mensaje descriptivo para el usuario */
    private String mensaje;

    public ColaInscripcionResponse() {}

    /** Constructor de conveniencia a partir de la entidad */
    public static ColaInscripcionResponse from(ColaInscripcion c, int personasDelante, String mensaje) {
        ColaInscripcionResponse dto = new ColaInscripcionResponse();
        dto.idCola          = c.getIdCola();
        dto.idMateria       = c.getMateria().getIdMateria();
        dto.nombreMateria   = c.getMateria().getNombre();
        dto.codigoMateria   = c.getMateria().getCodigo();
        dto.posicion        = c.getPosicion();
        dto.personasDelante = personasDelante;
        dto.estado          = c.getEstado().name();
        dto.fechaSolicitud  = c.getFechaSolicitud();
        dto.fechaExpiracion = c.getFechaExpiracion();
        dto.fechaResolucion = c.getFechaResolucion();
        dto.mensaje         = mensaje;
        return dto;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public Long getIdCola()                       { return idCola; }
    public void setIdCola(Long v)                 { this.idCola = v; }

    public Long getIdMateria()                    { return idMateria; }
    public void setIdMateria(Long v)              { this.idMateria = v; }

    public String getNombreMateria()              { return nombreMateria; }
    public void setNombreMateria(String v)        { this.nombreMateria = v; }

    public String getCodigoMateria()              { return codigoMateria; }
    public void setCodigoMateria(String v)        { this.codigoMateria = v; }

    public Integer getPosicion()                  { return posicion; }
    public void setPosicion(Integer v)            { this.posicion = v; }

    public int getPersonasDelante()               { return personasDelante; }
    public void setPersonasDelante(int v)         { this.personasDelante = v; }

    public String getEstado()                     { return estado; }
    public void setEstado(String v)               { this.estado = v; }

    public LocalDateTime getFechaSolicitud()      { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime v){ this.fechaSolicitud = v; }

    public LocalDateTime getFechaExpiracion()     { return fechaExpiracion; }
    public void setFechaExpiracion(LocalDateTime v){ this.fechaExpiracion = v; }

    public LocalDateTime getFechaResolucion()     { return fechaResolucion; }
    public void setFechaResolucion(LocalDateTime v){ this.fechaResolucion = v; }

    public String getMensaje()                    { return mensaje; }
    public void setMensaje(String v)              { this.mensaje = v; }
}
