import React, { useEffect, useState, useCallback } from 'react';
import materiaService, { Materia, Inscripcion } from '../services/materia.service';
import authService from '../services/auth.service';

interface Props {
  onClose?: () => void;
}

// ── Modal de confirmación ────────────────────────────────────────────────────
interface ModalProps {
  materia: Materia;
  onConfirm: () => void;
  onCancel: () => void;
  loading: boolean;
}

const ModalInscripcion: React.FC<ModalProps> = ({ materia, onConfirm, onCancel, loading }) => (
  <div style={{
    position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.45)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    zIndex: 1000, padding: '20px',
  }}>
    <div style={{
      background: 'white', borderRadius: '16px', padding: '32px',
      maxWidth: '440px', width: '100%',
      boxShadow: '0 20px 60px rgba(0,0,0,0.2)',
      animation: 'fadeIn .15s ease',
    }}>
      <div style={{ fontSize: '48px', textAlign: 'center', marginBottom: '16px' }}>📚</div>
      <h2 style={{ margin: '0 0 8px', textAlign: 'center', color: '#1e293b', fontSize: '20px' }}>
        Confirmar inscripción
      </h2>
      <p style={{ textAlign: 'center', color: '#64748b', margin: '0 0 20px', lineHeight: 1.5 }}>
        ¿Querés inscribirte en
      </p>
      <div style={{
        background: '#f0f9ff', border: '1px solid #bae6fd',
        borderRadius: '10px', padding: '14px 18px', marginBottom: '24px',
      }}>
        <div style={{ fontSize: '11px', fontWeight: 700, color: '#0284c7', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          {materia.codigo}
        </div>
        <div style={{ fontSize: '17px', fontWeight: 700, color: '#0c4a6e', marginTop: '4px' }}>
          {materia.nombre}
        </div>
        <div style={{ fontSize: '13px', color: '#0369a1', marginTop: '6px', display: 'flex', gap: '12px' }}>
          <span>📅 {materia.anio}° Año</span>
          {materia.cuposMaximos != null && (
            materia.hayLugar
              ? <span>🟢 {materia.cuposDisponibles} cupo{materia.cuposDisponibles !== 1 ? 's' : ''} disponible{materia.cuposDisponibles !== 1 ? 's' : ''}</span>
              : <span style={{ color: '#b45309', fontWeight: 700 }}>⚠️ Sin cupos — quedarás en lista de espera</span>
          )}
        </div>
      </div>
      <div style={{ display: 'flex', gap: '12px' }}>
        <button
          onClick={onCancel}
          disabled={loading}
          style={{
            flex: 1, padding: '12px', background: '#f1f5f9',
            border: '1px solid #e2e8f0', borderRadius: '10px',
            cursor: 'pointer', fontSize: '15px', fontWeight: 600, color: '#64748b',
          }}
        >
          Cancelar
        </button>
        <button
          onClick={onConfirm}
          disabled={loading}
          style={{
            flex: 1, padding: '12px',
            background: loading ? '#93c5fd' : 'linear-gradient(135deg, #0ea5e9, #0369a1)',
            border: 'none', borderRadius: '10px',
            cursor: loading ? 'not-allowed' : 'pointer',
            fontSize: '15px', fontWeight: 700, color: 'white',
            boxShadow: loading ? 'none' : '0 4px 12px rgba(14,165,233,0.35)',
          }}
        >
          {loading ? '⏳ Inscribiendo...' : '✅ Confirmar'}
        </button>
      </div>
    </div>
  </div>
);

// ── Componente principal ─────────────────────────────────────────────────────
const ListaMaterias: React.FC<Props> = ({ onClose }) => {
  const [materias, setMaterias] = useState<Materia[]>([]);
  const [inscripciones, setInscripciones] = useState<Inscripcion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [loadingId, setLoadingId] = useState<number | null>(null);
  const [mensaje, setMensaje] = useState<{ text: string; tipo: 'ok' | 'error' | 'espera' } | null>(null);
  const [filtroAnio, setFiltroAnio] = useState<number | 'todos'>('todos');
  const [busqueda, setBusqueda] = useState('');
  const [modalMateria, setModalMateria] = useState<Materia | null>(null);
  const [confirmLoading, setConfirmLoading] = useState(false);

  const cargarDatos = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [mats, inscs] = await Promise.all([
        materiaService.listarMaterias(authService.getCurrentUserData()?.carrera ?? undefined),
        materiaService.misInscripciones(),
      ]);
      setMaterias(mats);
      setInscripciones(inscs);
    } catch {
      setError('No se pudieron cargar las materias. Verificá tu conexión.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { cargarDatos(); }, [cargarDatos]);

  // Auto-ocultar mensajes después de 5s
  useEffect(() => {
    if (!mensaje) return;
    const t = setTimeout(() => setMensaje(null), 5000);
    return () => clearTimeout(t);
  }, [mensaje]);

  const inscripcionActiva = (idMateria: number) =>
    inscripciones.find(i => i.materia.idMateria === idMateria && i.estado !== 'CANCELADA');

  // Abre el modal de confirmación
  const solicitarInscripcion = (materia: Materia) => {
    setModalMateria(materia);
  };

  // Confirma y llama al backend
  const confirmarInscripcion = async () => {
    if (!modalMateria) return;
    setConfirmLoading(true);
    try {
      const result = await materiaService.inscribirse(modalMateria.idMateria);
      if (result.status === 202) {
        // Cupos llenos → quedó en lista de espera
        const posicion = result.data?.posicion ?? result.data?.position ?? '?';
        setMensaje({
          text: `⚠️ La materia "${modalMateria.nombre}" no tiene cupos disponibles. Quedaste en lista de espera (posición #${posicion}).`,
          tipo: 'espera',
        });
      } else {
        setMensaje({ text: `✅ Te inscribiste en "${modalMateria.nombre}"`, tipo: 'ok' });
      }
      await cargarDatos();
      setModalMateria(null);
    } catch (e: any) {
      const msg = e.response?.data?.message || e.message || 'Error al inscribirse';
      setMensaje({ text: `❌ ${msg}`, tipo: 'error' });
      setModalMateria(null);
    } finally {
      setConfirmLoading(false);
    }
  };

  const handleCancelar = async (inscripcion: Inscripcion) => {
    if (!window.confirm(`¿Cancelar inscripción en "${inscripcion.materia.nombre}"?`)) return;
    setLoadingId(inscripcion.materia.idMateria);
    setMensaje(null);
    try {
      await materiaService.cancelarInscripcion(inscripcion.idInscripcion);
      setMensaje({ text: `Inscripción cancelada en "${inscripcion.materia.nombre}"`, tipo: 'ok' });
      await cargarDatos();
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Error al cancelar';
      setMensaje({ text: `❌ ${msg}`, tipo: 'error' });
    } finally {
      setLoadingId(null);
    }
  };

  const materiasFiltradas = materias.filter(m => {
    const matchAnio = filtroAnio === 'todos' || m.anio === filtroAnio;
    const matchBusqueda =
      busqueda === '' ||
      m.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
      m.codigo.toLowerCase().includes(busqueda.toLowerCase());
    return matchAnio && matchBusqueda;
  });

  const estadoBadge: Record<string, { bg: string; color: string }> = {
    ACTIVA:      { bg: '#16a34a', color: 'white' },
    APROBADA:    { bg: '#0369a1', color: 'white' },
    DESAPROBADA: { bg: '#dc2626', color: 'white' },
  };

  const inscriptasCount = inscripciones.filter(i => i.estado !== 'CANCELADA').length;

  return (
    <div style={{ fontFamily: 'system-ui, sans-serif' }}>
      {/* Modal de confirmación */}
      {modalMateria && (
        <ModalInscripcion
          materia={modalMateria}
          onConfirm={confirmarInscripcion}
          onCancel={() => setModalMateria(null)}
          loading={confirmLoading}
        />
      )}

      {/* Encabezado */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '20px' }}>
        <div>
          <h2 style={{ margin: 0, color: '#1e293b', fontSize: '22px' }}>📚 Materias Disponibles</h2>
          <p style={{ margin: '6px 0 0', color: '#64748b', fontSize: '14px' }}>
            <strong>{materias.length}</strong> materias ·{' '}
            <strong style={{ color: '#0ea5e9' }}>{inscriptasCount}</strong> inscripciones activas
          </p>
        </div>
        {onClose && (
          <button onClick={onClose} style={{
            padding: '8px 16px', background: '#f1f5f9', border: '1px solid #e2e8f0',
            borderRadius: '8px', cursor: 'pointer', fontSize: '14px', color: '#64748b',
          }}>
            ← Volver
          </button>
        )}
      </div>

      {/* Mensaje flash */}
      {mensaje && (
        <div style={{
          padding: '13px 16px', borderRadius: '10px', marginBottom: '16px',
          background: mensaje.tipo === 'ok' ? '#dcfce7' : mensaje.tipo === 'espera' ? '#fef9c3' : '#fee2e2',
          color: mensaje.tipo === 'ok' ? '#166534' : mensaje.tipo === 'espera' ? '#854d0e' : '#991b1b',
          fontSize: '14px', fontWeight: 600,
          display: 'flex', justifyContent: 'space-between', alignItems: 'center',
          border: `1px solid ${mensaje.tipo === 'ok' ? '#86efac' : mensaje.tipo === 'espera' ? '#fde047' : '#fca5a5'}`,
        }}>
          {mensaje.text}
          <button onClick={() => setMensaje(null)} style={{
            background: 'none', border: 'none', cursor: 'pointer', fontSize: '16px', opacity: 0.6,
          }}>✕</button>
        </div>
      )}

      {/* Filtros */}
      <div style={{
        display: 'flex', gap: '10px', flexWrap: 'wrap', marginBottom: '20px',
        padding: '14px 16px', background: '#f8fafc', borderRadius: '10px',
        border: '1px solid #e2e8f0',
      }}>
        <input
          type="text"
          placeholder="🔍 Buscar por nombre o código..."
          value={busqueda}
          onChange={e => setBusqueda(e.target.value)}
          style={{
            flex: 1, minWidth: '200px', padding: '9px 12px',
            border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '14px',
            outline: 'none',
          }}
        />
        <select
          value={filtroAnio}
          onChange={e => setFiltroAnio(e.target.value === 'todos' ? 'todos' : Number(e.target.value))}
          style={{ padding: '9px 12px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '14px', background: 'white' }}
        >
          <option value="todos">Todos los años</option>
          {[1, 2, 3, 4, 5].map(a => <option key={a} value={a}>{a}° Año</option>)}
        </select>
        {(busqueda || filtroAnio !== 'todos') && (
          <button
            onClick={() => { setBusqueda(''); setFiltroAnio('todos'); }}
            style={{
              padding: '9px 14px', background: 'white', border: '1px solid #fca5a5',
              borderRadius: '8px', cursor: 'pointer', fontSize: '13px', color: '#dc2626',
            }}
          >
            ✕ Limpiar
          </button>
        )}
      </div>

      {/* Cargando */}
      {loading && (
        <div style={{ textAlign: 'center', padding: '60px', color: '#64748b' }}>
          <div style={{ fontSize: '36px', marginBottom: '12px', animation: 'spin 1s linear infinite' }}>⏳</div>
          <p style={{ margin: 0 }}>Cargando materias...</p>
        </div>
      )}

      {/* Error */}
      {error && !loading && (
        <div style={{
          padding: '20px', background: '#fee2e2', color: '#991b1b',
          borderRadius: '12px', textAlign: 'center', border: '1px solid #fca5a5',
        }}>
          <div style={{ fontSize: '32px', marginBottom: '8px' }}>⚠️</div>
          <p style={{ margin: '0 0 12px' }}>{error}</p>
          <button onClick={cargarDatos} style={{
            padding: '8px 20px', background: '#dc2626', color: 'white',
            border: 'none', borderRadius: '8px', cursor: 'pointer', fontWeight: 600,
          }}>Reintentar</button>
        </div>
      )}

      {/* Grid de materias */}
      {!loading && !error && (
        <>
          {/* Contador de resultados */}
          {materiasFiltradas.length !== materias.length && (
            <p style={{ margin: '0 0 16px', fontSize: '13px', color: '#64748b' }}>
              Mostrando <strong>{materiasFiltradas.length}</strong> de {materias.length} materias
            </p>
          )}

          {materiasFiltradas.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '60px', color: '#94a3b8' }}>
              <div style={{ fontSize: '48px', marginBottom: '12px' }}>📭</div>
              <p style={{ margin: 0, fontSize: '15px' }}>No se encontraron materias con ese criterio.</p>
            </div>
          ) : (
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(290px, 1fr))',
              gap: '16px',
            }}>
              {materiasFiltradas.map(materia => {
                const insc = inscripcionActiva(materia.idMateria);
                const esLoading = loadingId === materia.idMateria;
                const badge = insc ? estadoBadge[insc.estado] : null;

                return (
                  <div key={materia.idMateria} style={{
                    background: 'white', borderRadius: '14px',
                    border: `2px solid ${insc ? '#bae6fd' : '#e2e8f0'}`,
                    padding: '20px', position: 'relative',
                    boxShadow: insc ? '0 4px 12px rgba(14,165,233,0.10)' : '0 2px 6px rgba(0,0,0,0.04)',
                    display: 'flex', flexDirection: 'column', gap: '0',
                  }}>

                    {/* Badge estado */}
                    {insc && badge && (
                      <span style={{
                        position: 'absolute', top: '14px', right: '14px',
                        padding: '3px 10px', borderRadius: '20px', fontSize: '11px',
                        fontWeight: 700, background: badge.bg, color: badge.color,
                      }}>
                        ✓ {insc.estado}
                      </span>
                    )}

                    {/* Código y nombre */}
                    <div style={{ marginBottom: '10px', paddingRight: insc ? '80px' : '0' }}>
                      <span style={{
                        fontSize: '11px', fontWeight: 700, color: '#0284c7',
                        textTransform: 'uppercase', letterSpacing: '0.06em',
                      }}>
                        {materia.codigo}
                      </span>
                      <h3 style={{ margin: '4px 0 0', fontSize: '16px', color: '#1e293b', lineHeight: 1.3, fontWeight: 700 }}>
                        {materia.nombre}
                      </h3>
                    </div>

                    {/* Descripción */}
                    {materia.descripcion && (
                      <p style={{
                        fontSize: '13px', color: '#64748b', marginBottom: '12px',
                        lineHeight: 1.55, flexGrow: 1,
                      }}>
                        {materia.descripcion.length > 100
                          ? materia.descripcion.slice(0, 100) + '...'
                          : materia.descripcion}
                      </p>
                    )}

                    {/* Chips informativos */}
                    <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginBottom: '16px' }}>
                      <Chip icon="📅" text={`${materia.anio}° Año`} />
                      {materia.docenteNombre && (
                        <Chip icon="👨‍🏫" text={`${materia.docenteNombre} ${materia.docenteApellido}`} />
                      )}
                      {/* Cupos */}
                      {materia.cuposMaximos != null ? (
                        materia.hayLugar ? (
                          <Chip icon="🟢" text={`${materia.cuposDisponibles} cupo${materia.cuposDisponibles !== 1 ? 's' : ''} libre${materia.cuposDisponibles !== 1 ? 's' : ''}`} />
                        ) : (
                          <span style={{
                            display: 'inline-flex', alignItems: 'center', gap: '4px',
                            padding: '3px 9px', background: '#fee2e2', borderRadius: '20px',
                            fontSize: '12px', color: '#dc2626', fontWeight: 700, whiteSpace: 'nowrap',
                          }}>
                            🔴 Sin cupos · lista de espera
                          </span>
                        )
                      ) : (
                        <Chip icon="♾️" text="Sin límite de cupos" />
                      )}
                    </div>

                    {/* ── BOTÓN DE ACCIÓN ── */}
                    {insc ? (
                      <button
                        onClick={() => handleCancelar(insc)}
                        disabled={esLoading}
                        style={{
                          width: '100%', padding: '11px',
                          background: esLoading ? '#f8fafc' : 'white',
                          color: esLoading ? '#94a3b8' : '#ef4444',
                          border: `1px solid ${esLoading ? '#e2e8f0' : '#fca5a5'}`,
                          borderRadius: '10px',
                          cursor: esLoading ? 'not-allowed' : 'pointer',
                          fontSize: '14px', fontWeight: 600,
                          transition: 'all 0.15s',
                        }}
                      >
                        {esLoading ? '⏳ Procesando...' : '✕ Cancelar inscripción'}
                      </button>
                    ) : (
                      <button
                        onClick={() => solicitarInscripcion(materia)}
                        disabled={esLoading}
                        style={{
                          width: '100%', padding: '11px',
                          background: esLoading
                            ? '#e0f2fe'
                            : 'linear-gradient(135deg, #0ea5e9 0%, #0369a1 100%)',
                          color: esLoading ? '#0284c7' : 'white',
                          border: 'none', borderRadius: '10px',
                          cursor: esLoading ? 'not-allowed' : 'pointer',
                          fontSize: '14px', fontWeight: 700,
                          boxShadow: esLoading ? 'none' : '0 4px 12px rgba(14,165,233,0.30)',
                          transition: 'all 0.15s',
                          letterSpacing: '0.02em',
                        }}
                        onMouseEnter={e => {
                          if (!esLoading) (e.currentTarget as HTMLButtonElement).style.transform = 'translateY(-1px)';
                        }}
                        onMouseLeave={e => {
                          (e.currentTarget as HTMLButtonElement).style.transform = 'translateY(0)';
                        }}
                      >
                        {esLoading ? '⏳ Procesando...' : '+ Inscribirme'}
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </>
      )}
    </div>
  );
};

const Chip: React.FC<{ icon: string; text: string }> = ({ icon, text }) => (
  <span style={{
    display: 'inline-flex', alignItems: 'center', gap: '4px',
    padding: '3px 9px', background: '#f1f5f9', borderRadius: '20px',
    fontSize: '12px', color: '#475569', whiteSpace: 'nowrap',
  }}>
    {icon} {text}
  </span>
);

export default ListaMaterias;
