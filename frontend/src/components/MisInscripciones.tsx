import React, { useEffect, useState, useCallback } from 'react';
import materiaService, { Inscripcion } from '../services/materia.service';

const estadoStyle: Record<string, { bg: string; color: string; label: string }> = {
  ACTIVA:      { bg: '#dbeafe', color: '#1d4ed8', label: '🟢 Activa' },
  APROBADA:    { bg: '#dcfce7', color: '#15803d', label: '✅ Aprobada' },
  DESAPROBADA: { bg: '#fee2e2', color: '#dc2626', label: '❌ Desaprobada' },
  CANCELADA:   { bg: '#f1f5f9', color: '#64748b', label: '⛔ Cancelada' },
};

const MisInscripciones: React.FC = () => {
  const [inscripciones, setInscripciones] = useState<Inscripcion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancelandoId, setCancelandoId] = useState<number | null>(null);
  const [mensaje, setMensaje] = useState<{ text: string; tipo: 'ok' | 'error' } | null>(null);
  const [filtroEstado, setFiltroEstado] = useState<string>('todos');

  const cargar = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await materiaService.misInscripciones();
      setInscripciones(data);
    } catch {
      setError('No se pudieron cargar tus inscripciones.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { cargar(); }, [cargar]);

  const handleCancelar = async (insc: Inscripcion) => {
    if (!window.confirm(`¿Cancelar inscripción en "${insc.materia.nombre}"?`)) return;
    setCancelandoId(insc.idInscripcion);
    try {
      await materiaService.cancelarInscripcion(insc.idInscripcion);
      setMensaje({ text: `Inscripción en "${insc.materia.nombre}" cancelada.`, tipo: 'ok' });
      await cargar();
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Error al cancelar';
      setMensaje({ text: `❌ ${msg}`, tipo: 'error' });
    } finally {
      setCancelandoId(null);
    }
  };

  const filtradas = filtroEstado === 'todos'
    ? inscripciones
    : inscripciones.filter(i => i.estado === filtroEstado);

  return (
    <div style={{ fontFamily: 'system-ui, sans-serif' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <div>
          <h2 style={{ margin: 0, color: '#1e293b', fontSize: '22px' }}>📋 Mis Inscripciones</h2>
          <p style={{ margin: '4px 0 0', color: '#64748b', fontSize: '14px' }}>
            {inscripciones.filter(i => i.estado !== 'CANCELADA').length} inscripciones activas
          </p>
        </div>
        <select
          value={filtroEstado}
          onChange={e => setFiltroEstado(e.target.value)}
          style={{ padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '14px' }}
        >
          <option value="todos">Todos los estados</option>
          <option value="ACTIVA">Activas</option>
          <option value="APROBADA">Aprobadas</option>
          <option value="DESAPROBADA">Desaprobadas</option>
          <option value="CANCELADA">Canceladas</option>
        </select>
      </div>

      {/* Mensaje flash */}
      {mensaje && (
        <div style={{
          padding: '12px 16px', borderRadius: '8px', marginBottom: '16px',
          background: mensaje.tipo === 'ok' ? '#dcfce7' : '#fee2e2',
          color: mensaje.tipo === 'ok' ? '#166534' : '#991b1b',
          fontSize: '14px', display: 'flex', justifyContent: 'space-between',
        }}>
          {mensaje.text}
          <button onClick={() => setMensaje(null)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>✕</button>
        </div>
      )}

      {loading && (
        <div style={{ textAlign: 'center', padding: '40px', color: '#64748b' }}>⏳ Cargando...</div>
      )}
      {error && (
        <div style={{ padding: '16px', background: '#fee2e2', color: '#991b1b', borderRadius: '10px', textAlign: 'center' }}>
          {error}
          <button onClick={cargar} style={{ marginLeft: '12px', padding: '6px 14px', background: '#dc2626', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer' }}>
            Reintentar
          </button>
        </div>
      )}

      {!loading && !error && (
        filtradas.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '48px', color: '#94a3b8' }}>
            <div style={{ fontSize: '40px', marginBottom: '8px' }}>📭</div>
            <p>No tenés inscripciones {filtroEstado !== 'todos' ? `con estado "${filtroEstado}"` : ''}.</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {filtradas.map(insc => {
              const style = estadoStyle[insc.estado] || estadoStyle['CANCELADA'];
              const cancelando = cancelandoId === insc.idInscripcion;
              return (
                <div key={insc.idInscripcion} style={{
                  background: 'white', borderRadius: '12px', padding: '18px 20px',
                  border: '1px solid #e2e8f0', boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
                  display: 'flex', alignItems: 'center', gap: '16px', flexWrap: 'wrap',
                }}>
                  {/* Info materia */}
                  <div style={{ flex: 1, minWidth: '200px' }}>
                    <div style={{ fontSize: '11px', color: '#0ea5e9', fontWeight: 700, textTransform: 'uppercase' }}>
                      {insc.materia.codigo}
                    </div>
                    <div style={{ fontSize: '16px', fontWeight: 600, color: '#1e293b', marginTop: '2px' }}>
                      {insc.materia.nombre}
                    </div>
                    <div style={{ fontSize: '12px', color: '#64748b', marginTop: '4px' }}>
                      {insc.materia.anio}° Año
                    </div>
                  </div>

                  {/* Notas */}
                  {insc.notaFinal !== undefined && insc.notaFinal !== null && (
                    <div style={{ textAlign: 'center', minWidth: '80px' }}>
                      <div style={{ fontSize: '24px', fontWeight: 700, color: insc.notaFinal >= 6 ? '#15803d' : '#dc2626' }}>
                        {insc.notaFinal}
                      </div>
                      <div style={{ fontSize: '11px', color: '#94a3b8' }}>Nota final</div>
                    </div>
                  )}

                  {/* Fecha */}
                  <div style={{ textAlign: 'center', minWidth: '100px' }}>
                    <div style={{ fontSize: '13px', fontWeight: 600, color: '#334155' }}>
                      {new Date(insc.fechaInscripcion).toLocaleDateString('es-AR')}
                    </div>
                    <div style={{ fontSize: '11px', color: '#94a3b8' }}>Fecha inscripción</div>
                  </div>

                  {/* Estado */}
                  <span style={{
                    padding: '5px 14px', borderRadius: '20px', fontSize: '12px', fontWeight: 700,
                    background: style.bg, color: style.color, whiteSpace: 'nowrap',
                  }}>
                    {style.label}
                  </span>

                  {/* Acción */}
                  {insc.estado === 'ACTIVA' && (
                    <button
                      onClick={() => handleCancelar(insc)}
                      disabled={cancelando}
                      style={{
                        padding: '8px 14px', background: cancelando ? '#f1f5f9' : '#fee2e2',
                        color: cancelando ? '#94a3b8' : '#dc2626',
                        border: '1px solid #fca5a5', borderRadius: '8px',
                        cursor: cancelando ? 'not-allowed' : 'pointer', fontSize: '13px', fontWeight: 600,
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {cancelando ? '⏳' : '✕ Cancelar'}
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        )
      )}
    </div>
  );
};

export default MisInscripciones;
