import React, { useEffect, useState, useCallback } from 'react';
import auditoriaService, { RegistroAuditoria, IntegridadResponse } from '../services/auditoria.service';

// ─── helpers ────────────────────────────────────────────────────────────────

const ENTIDADES = ['Todas', 'Inscripcion', 'Materia', 'Estudiante', 'Auditorio', 'ReservaAuditorio'];
const ACCIONES  = ['Todas', 'INSCRIBIR', 'CANCELAR', 'CARGAR_NOTA', 'CERRAR_NOTA',
                   'CREAR', 'MODIFICAR', 'ELIMINAR', 'CAMBIAR_ESTADO'];

function accionBadge(accion: string) {
  const map: Record<string, { bg: string; color: string }> = {
    CREAR:          { bg: '#dcfce7', color: '#15803d' },
    INSCRIBIR:      { bg: '#dbeafe', color: '#1d4ed8' },
    MODIFICAR:      { bg: '#fef9c3', color: '#854d0e' },
    ELIMINAR:       { bg: '#fee2e2', color: '#b91c1c' },
    CANCELAR:       { bg: '#fce7f3', color: '#9d174d' },
    CARGAR_NOTA:    { bg: '#e0e7ff', color: '#3730a3' },
    CERRAR_NOTA:    { bg: '#f1f5f9', color: '#334155' },
    CAMBIAR_ESTADO: { bg: '#ffedd5', color: '#c2410c' },
  };
  const s = map[accion] ?? { bg: '#f1f5f9', color: '#475569' };
  return (
    <span style={{
      padding: '2px 8px', borderRadius: '12px', fontSize: '11px',
      fontWeight: 700, background: s.bg, color: s.color, whiteSpace: 'nowrap',
    }}>
      {accion}
    </span>
  );
}

function hashCorto(hash: string) {
  return hash ? `${hash.slice(0, 8)}…${hash.slice(-4)}` : '–';
}

function formatFecha(iso: string) {
  const d = new Date(iso);
  return d.toLocaleString('es-AR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  });
}

// ─── Panel de integridad ─────────────────────────────────────────────────────

interface IntegridadPanelProps {
  resultado: IntegridadResponse | null;
  cargando: boolean;
  onVerificar: () => void;
}

const IntegridadPanel: React.FC<IntegridadPanelProps> = ({ resultado, cargando, onVerificar }) => (
  <div style={{
    borderRadius: '12px', border: `2px solid ${resultado?.integra === false ? '#fca5a5' : '#bbf7d0'}`,
    background: resultado?.integra === false ? '#fff5f5' : '#f0fdf4',
    padding: '20px 24px', marginBottom: '24px',
    display: 'flex', alignItems: 'center', gap: '20px', flexWrap: 'wrap',
  }}>
    <div style={{ flex: 1, minWidth: '200px' }}>
      <div style={{ fontWeight: 700, fontSize: '15px', color: '#1e293b', marginBottom: '4px' }}>
        {resultado === null
          ? '🔍 Verificación de integridad de la cadena'
          : resultado.integra
            ? '✅ Cadena íntegra'
            : '⚠️ ¡Violaciones detectadas!'}
      </div>
      <div style={{ fontSize: '13px', color: '#64748b' }}>
        {resultado?.mensaje ?? 'Presioná "Verificar" para recomputar todos los hashes SHA-256.'}
      </div>
      {resultado && !resultado.integra && (
        <ul style={{ marginTop: '10px', paddingLeft: '18px', color: '#b91c1c', fontSize: '12px' }}>
          {resultado.errores.map((e, i) => <li key={i}>{e}</li>)}
        </ul>
      )}
    </div>
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '6px' }}>
      {resultado && (
        <span style={{ fontSize: '28px', fontWeight: 800, color: '#0369a1' }}>
          {resultado.totalRegistros}
        </span>
      )}
      {resultado && <span style={{ fontSize: '11px', color: '#64748b', fontWeight: 600 }}>registros</span>}
      <button
        onClick={onVerificar}
        disabled={cargando}
        style={{
          padding: '8px 18px', borderRadius: '8px', cursor: cargando ? 'wait' : 'pointer',
          background: cargando ? '#93c5fd' : '#0369a1', color: 'white',
          border: 'none', fontSize: '13px', fontWeight: 700,
        }}
      >
        {cargando ? '⏳ Verificando...' : '🔒 Verificar integridad'}
      </button>
    </div>
  </div>
);

// ─── Fila expandible ─────────────────────────────────────────────────────────

const FilaAuditoria: React.FC<{ r: RegistroAuditoria; idx: number }> = ({ r, idx }) => {
  const [expanded, setExpanded] = useState(false);
  const bg = idx % 2 === 0 ? 'white' : '#fafafa';

  return (
    <>
      <tr
        onClick={() => setExpanded(e => !e)}
        style={{ cursor: 'pointer', background: bg }}
        onMouseEnter={e => (e.currentTarget.style.background = '#f0f9ff')}
        onMouseLeave={e => (e.currentTarget.style.background = bg)}
      >
        <td style={{ padding: '10px 12px', color: '#94a3b8', fontSize: '12px', whiteSpace: 'nowrap' }}>
          #{r.idRegistro}
        </td>
        <td style={{ padding: '10px 12px', fontSize: '12px', whiteSpace: 'nowrap', color: '#475569' }}>
          {formatFecha(r.timestampEvento)}
        </td>
        <td style={{ padding: '10px 12px' }}>
          <span style={{ fontWeight: 600, fontSize: '13px', color: '#1e293b' }}>{r.entidad}</span>
          {r.idEntidad && (
            <span style={{ marginLeft: '6px', color: '#94a3b8', fontSize: '11px' }}>#{r.idEntidad}</span>
          )}
        </td>
        <td style={{ padding: '10px 12px' }}>{accionBadge(r.accion)}</td>
        <td style={{ padding: '10px 12px', fontSize: '12px', color: '#64748b', maxWidth: '200px',
            overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {r.emailUsuario ?? '–'}
        </td>
        <td style={{ padding: '10px 12px', fontSize: '12px', color: '#64748b', maxWidth: '200px',
            overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {r.descripcion}
        </td>
        <td style={{ padding: '10px 12px', textAlign: 'center', color: '#94a3b8', fontSize: '16px' }}>
          {expanded ? '▲' : '▼'}
        </td>
      </tr>

      {expanded && (
        <tr style={{ background: '#f8fafc' }}>
          <td colSpan={7} style={{ padding: '14px 20px', borderTop: '1px solid #e2e8f0', borderBottom: '1px solid #e2e8f0' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', fontSize: '12px' }}>
              <div>
                <span style={{ color: '#94a3b8', fontWeight: 600 }}>HASH ANTERIOR</span>
                <div style={{ fontFamily: 'monospace', color: '#475569', marginTop: '2px',
                    background: '#f1f5f9', padding: '6px 10px', borderRadius: '6px',
                    wordBreak: 'break-all', fontSize: '11px' }}>
                  {r.hashAnterior}
                </div>
              </div>
              <div>
                <span style={{ color: '#94a3b8', fontWeight: 600 }}>HASH ACTUAL (SHA-256)</span>
                <div style={{ fontFamily: 'monospace', color: '#0369a1', marginTop: '2px',
                    background: '#eff6ff', padding: '6px 10px', borderRadius: '6px',
                    wordBreak: 'break-all', fontSize: '11px' }}>
                  {r.hashActual}
                </div>
              </div>
              {r.ipOrigen && (
                <div>
                  <span style={{ color: '#94a3b8', fontWeight: 600 }}>IP ORIGEN</span>
                  <div style={{ color: '#475569', marginTop: '2px' }}>{r.ipOrigen}</div>
                </div>
              )}
              <div>
                <span style={{ color: '#94a3b8', fontWeight: 600 }}>ID USUARIO</span>
                <div style={{ color: '#475569', marginTop: '2px' }}>{r.idUsuario ?? 'sistema'}</div>
              </div>
            </div>
          </td>
        </tr>
      )}
    </>
  );
};

// ─── Componente principal ────────────────────────────────────────────────────

const VistaAuditoria: React.FC = () => {
  const [registros, setRegistros] = useState<RegistroAuditoria[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [integridad, setIntegridad] = useState<IntegridadResponse | null>(null);
  const [verificando, setVerificando] = useState(false);

  // Filtros
  const [filtroEntidad, setFiltroEntidad] = useState('Todas');
  const [filtroAccion, setFiltroAccion] = useState('Todas');
  const [busqueda, setBusqueda] = useState('');

  const cargar = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await auditoriaService.listarTodos();
      // Más reciente primero
      setRegistros([...data].reverse());
    } catch {
      setError('No se pudo cargar el log de auditoría.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { cargar(); }, [cargar]);

  const handleVerificar = async () => {
    setVerificando(true);
    try {
      const res = await auditoriaService.verificarIntegridad();
      setIntegridad(res);
    } catch {
      setError('Error al verificar integridad.');
    } finally {
      setVerificando(false);
    }
  };

  const filtrados = registros.filter(r => {
    const matchEntidad = filtroEntidad === 'Todas' || r.entidad === filtroEntidad;
    const matchAccion  = filtroAccion  === 'Todas' || r.accion  === filtroAccion;
    const matchBusqueda = busqueda === '' ||
      r.emailUsuario?.toLowerCase().includes(busqueda.toLowerCase()) ||
      r.descripcion?.toLowerCase().includes(busqueda.toLowerCase()) ||
      r.entidad?.toLowerCase().includes(busqueda.toLowerCase());
    return matchEntidad && matchAccion && matchBusqueda;
  });

  // Estadísticas rápidas
  const stats = {
    total:    registros.length,
    hoy:      registros.filter(r => r.timestampEvento?.startsWith(new Date().toISOString().slice(0, 10))).length,
    acciones: new Set(registros.map(r => r.accion)).size,
    usuarios: new Set(registros.map(r => r.emailUsuario).filter(Boolean)).size,
  };

  return (
    <div>
      {/* Encabezado */}
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ margin: 0, color: '#1e293b', fontSize: '22px' }}>🔗 Auditoría Encadenada</h2>
        <p style={{ margin: '4px 0 0', color: '#64748b', fontSize: '14px' }}>
          Log inmutable de todas las operaciones del sistema. Cada registro está encadenado con SHA-256.
        </p>
      </div>

      {/* Panel de integridad */}
      <IntegridadPanel
        resultado={integridad}
        cargando={verificando}
        onVerificar={handleVerificar}
      />

      {/* Estadísticas */}
      <div style={{
        display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
        gap: '12px', marginBottom: '24px',
      }}>
        {[
          { label: 'Total registros', value: stats.total,    bg: '#f1f5f9', color: '#475569' },
          { label: 'Hoy',             value: stats.hoy,      bg: '#dbeafe', color: '#1d4ed8' },
          { label: 'Tipos de acción', value: stats.acciones, bg: '#fef9c3', color: '#854d0e' },
          { label: 'Usuarios únicos', value: stats.usuarios, bg: '#f3e8ff', color: '#7c3aed' },
        ].map(s => (
          <div key={s.label} style={{
            background: s.bg, borderRadius: '10px', padding: '14px',
            textAlign: 'center',
          }}>
            <div style={{ fontSize: '26px', fontWeight: 800, color: s.color }}>{s.value}</div>
            <div style={{ fontSize: '11px', color: s.color, fontWeight: 600, marginTop: '2px' }}>{s.label}</div>
          </div>
        ))}
      </div>

      {/* Filtros */}
      <div style={{
        display: 'flex', gap: '10px', flexWrap: 'wrap', marginBottom: '16px',
        background: '#f8fafc', padding: '14px 16px', borderRadius: '10px',
        border: '1px solid #e2e8f0', alignItems: 'center',
      }}>
        <input
          type="text"
          placeholder="🔍 Buscar por email, entidad o descripción..."
          value={busqueda}
          onChange={e => setBusqueda(e.target.value)}
          style={{
            flex: 1, minWidth: '220px', padding: '7px 12px',
            border: '1px solid #d1d5db', borderRadius: '7px', fontSize: '13px',
          }}
        />
        <select value={filtroEntidad} onChange={e => setFiltroEntidad(e.target.value)}
          style={{ padding: '7px 12px', borderRadius: '7px', border: '1px solid #d1d5db', fontSize: '13px' }}>
          {ENTIDADES.map(e => <option key={e}>{e}</option>)}
        </select>
        <select value={filtroAccion} onChange={e => setFiltroAccion(e.target.value)}
          style={{ padding: '7px 12px', borderRadius: '7px', border: '1px solid #d1d5db', fontSize: '13px' }}>
          {ACCIONES.map(a => <option key={a}>{a}</option>)}
        </select>
        <button
          onClick={cargar}
          style={{
            padding: '7px 14px', borderRadius: '7px', background: '#0369a1',
            color: 'white', border: 'none', cursor: 'pointer', fontSize: '13px', fontWeight: 600,
          }}
        >
          ↻ Actualizar
        </button>
        {(filtroEntidad !== 'Todas' || filtroAccion !== 'Todas' || busqueda) && (
          <button
            onClick={() => { setFiltroEntidad('Todas'); setFiltroAccion('Todas'); setBusqueda(''); }}
            style={{
              padding: '7px 12px', borderRadius: '7px', background: '#f1f5f9',
              color: '#475569', border: '1px solid #d1d5db', cursor: 'pointer', fontSize: '13px',
            }}
          >
            ✕ Limpiar
          </button>
        )}
        <span style={{ fontSize: '12px', color: '#94a3b8', marginLeft: 'auto' }}>
          {filtrados.length} de {registros.length} registros
        </span>
      </div>

      {/* Error */}
      {error && (
        <div style={{
          background: '#fee2e2', color: '#b91c1c', borderRadius: '10px',
          padding: '14px 20px', marginBottom: '16px',
        }}>
          ⚠️ {error}
        </div>
      )}

      {/* Loading */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px', color: '#94a3b8' }}>
          ⏳ Cargando registros de auditoría...
        </div>
      ) : filtrados.length === 0 ? (
        <div style={{
          textAlign: 'center', padding: '50px', color: '#94a3b8',
          background: '#f8fafc', borderRadius: '12px',
        }}>
          📭 No hay registros con los filtros aplicados.
        </div>
      ) : (
        <div style={{ borderRadius: '12px', overflow: 'auto', border: '1px solid #e2e8f0' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', background: 'white', minWidth: '900px' }}>
            <thead>
              <tr style={{ background: '#1e293b', color: 'white' }}>
                {['#', 'Fecha/Hora', 'Entidad', 'Acción', 'Usuario', 'Descripción', ''].map(h => (
                  <th key={h} style={{
                    padding: '11px 12px', textAlign: 'left',
                    fontSize: '11px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.5px',
                  }}>
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {filtrados.map((r, idx) => (
                <FilaAuditoria key={r.idRegistro} r={r} idx={idx} />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default VistaAuditoria;
