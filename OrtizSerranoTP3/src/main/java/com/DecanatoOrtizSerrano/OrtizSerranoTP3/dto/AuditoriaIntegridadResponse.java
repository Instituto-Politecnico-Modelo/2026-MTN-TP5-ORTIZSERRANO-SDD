package com.DecanatoOrtizSerrano.OrtizSerranoTP3.dto;

import java.util.List;

/**
 * Resultado de la verificación de integridad de la cadena de auditoría.
 */
public class AuditoriaIntegridadResponse {

    private boolean integra;
    private int totalRegistros;
    private List<String> errores;
    private String mensaje;

    public AuditoriaIntegridadResponse(boolean integra, int totalRegistros,
                                        List<String> errores) {
        this.integra = integra;
        this.totalRegistros = totalRegistros;
        this.errores = errores;
        this.mensaje = integra
            ? "Cadena de auditoría íntegra. " + totalRegistros + " registros verificados."
            : "⚠️ Se detectaron " + errores.size() + " violaciones de integridad.";
    }

    public boolean isIntegra() { return integra; }
    public int getTotalRegistros() { return totalRegistros; }
    public List<String> getErrores() { return errores; }
    public String getMensaje() { return mensaje; }
}
