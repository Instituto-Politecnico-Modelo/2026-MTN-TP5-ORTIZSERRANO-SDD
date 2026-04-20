package com.DecanatoOrtizSerrano.OrtizSerranoTP3.service;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.RegistroAuditoria;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.AuditoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de Auditoría Encadenada.
 *
 * Cada registro almacena el hashAnterior + sus propios datos,
 * y calcula su hashActual con SHA-256. Esto forma una cadena
 * donde cualquier modificación retroactiva es detectable al
 * recorrer y recomputar la cadena (verificarIntegridad).
 */
@Service
public class AuditoriaService {

    private static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    // ─── Registro de eventos ─────────────────────────────────────────────────

    /**
     * Registra un evento de auditoría y lo encadena al registro anterior.
     * Se ejecuta en una transacción NUEVA (REQUIRES_NEW) para que quede
     * guardado aunque la transacción principal falle (ej: para auditar errores).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RegistroAuditoria registrar(
            String entidad,
            Long idEntidad,
            String accion,
            String descripcion,
            Long idUsuario,
            String emailUsuario,
            String ipOrigen) {

        // Obtener hash del último registro (o GENESIS si es el primero)
        String hashAnterior = auditoriaRepository.findUltimoRegistro()
            .map(RegistroAuditoria::getHashActual)
            .orElse(GENESIS_HASH);

        LocalDateTime ahora = LocalDateTime.now();

        // Calcular hash del nuevo registro
        String hashActual = calcularHash(
            hashAnterior, entidad, idEntidad, accion, descripcion, ahora, idUsuario
        );

        RegistroAuditoria registro = new RegistroAuditoria();
        registro.setEntidad(entidad);
        registro.setIdEntidad(idEntidad);
        registro.setAccion(accion);
        registro.setDescripcion(descripcion);
        registro.setIdUsuario(idUsuario);
        registro.setEmailUsuario(emailUsuario);
        registro.setIpOrigen(ipOrigen);
        registro.setTimestampEvento(ahora);
        registro.setHashAnterior(hashAnterior);
        registro.setHashActual(hashActual);

        return auditoriaRepository.save(registro);
    }

    /** Sobrecarga sin IP (para uso interno del sistema) */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RegistroAuditoria registrar(String entidad, Long idEntidad,
                                       String accion, String descripcion,
                                       Long idUsuario, String emailUsuario) {
        return registrar(entidad, idEntidad, accion, descripcion, idUsuario, emailUsuario, null);
    }

    // ─── Consultas ────────────────────────────────────────────────────────────

    public List<RegistroAuditoria> listarTodos() {
        return auditoriaRepository.findAllByOrderByIdRegistroAsc();
    }

    public List<RegistroAuditoria> porEntidad(String entidad) {
        return auditoriaRepository.findByEntidadOrderByIdRegistroAsc(entidad);
    }

    public List<RegistroAuditoria> porEntidadYId(String entidad, Long idEntidad) {
        return auditoriaRepository.findByEntidadAndIdEntidadOrderByIdRegistroAsc(entidad, idEntidad);
    }

    public List<RegistroAuditoria> porUsuario(Long idUsuario) {
        return auditoriaRepository.findByIdUsuarioOrderByIdRegistroAsc(idUsuario);
    }

    public List<RegistroAuditoria> porAccion(String accion) {
        return auditoriaRepository.findByAccionOrderByIdRegistroAsc(accion);
    }

    public List<RegistroAuditoria> porRangoFecha(LocalDateTime desde, LocalDateTime hasta) {
        return auditoriaRepository.findByTimestampEventoBetweenOrderByIdRegistroAsc(desde, hasta);
    }

    // ─── Verificación de integridad ──────────────────────────────────────────

    /**
     * Recorre todos los registros en orden y recomputa cada hash.
     * Si alguno no coincide, el registro fue manipulado.
     *
     * @return lista de errores encontrados (vacía = cadena íntegra)
     */
    public List<String> verificarIntegridad() {
        List<RegistroAuditoria> todos = auditoriaRepository.findAllByOrderByIdRegistroAsc();
        List<String> errores = new ArrayList<>();
        String hashEsperadoAnterior = GENESIS_HASH;

        for (RegistroAuditoria r : todos) {
            // 1. Verificar que hashAnterior coincide con lo esperado
            if (!hashEsperadoAnterior.equals(r.getHashAnterior())) {
                errores.add(String.format(
                    "Registro #%d: hashAnterior esperado='%s' pero tiene='%s' → cadena rota",
                    r.getIdRegistro(), hashEsperadoAnterior, r.getHashAnterior()
                ));
            }

            // 2. Recomputar hashActual y comparar
            String hashRecomputado = calcularHash(
                r.getHashAnterior(), r.getEntidad(), r.getIdEntidad(),
                r.getAccion(), r.getDescripcion(),
                r.getTimestampEvento(), r.getIdUsuario()
            );
            if (!hashRecomputado.equals(r.getHashActual())) {
                errores.add(String.format(
                    "Registro #%d: hashActual almacenado='%s' pero recomputado='%s' → DATOS MANIPULADOS",
                    r.getIdRegistro(), r.getHashActual(), hashRecomputado
                ));
            }

            // El hash de este registro es el "anterior" para el siguiente
            hashEsperadoAnterior = r.getHashActual();
        }

        return errores;
    }

    // ─── Cálculo SHA-256 ─────────────────────────────────────────────────────

    /**
     * SHA-256(hashAnterior | entidad | idEntidad | accion | descripcion | timestamp | idUsuario)
     */
    public String calcularHash(String hashAnterior, String entidad, Long idEntidad,
                                String accion, String descripcion,
                                LocalDateTime timestamp, Long idUsuario) {
        String input = String.join("|",
            safeStr(hashAnterior),
            safeStr(entidad),
            safeStr(idEntidad),
            safeStr(accion),
            safeStr(descripcion),
            safeStr(timestamp),
            safeStr(idUsuario)
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }

    private String safeStr(Object obj) {
        return obj == null ? "null" : obj.toString();
    }
}
