import React, { useEffect, useState } from 'react';
import materiaService, { Inscripcion } from '../services/materia.service';

// ─── helpers ────────────────────────────────────────────────────────────────

function notaColor(nota: number): string {
  if (nota >= 7) return '#15803d';
  if (nota >= 6) return '#b45309';
  return '#b91c1c';
}

function estadoBadge(estado: string, cerrada: boolean) {
  const map: Record<string, { bg: string; color: string; label: string }> = {
    APROBADA:    { bg: '#dcfce7', color: '#15803d', label: '✔ Aprobada' },
    DESAPROBADA: { bg: '#fee2e2', color: '#b91c1c', label: '✘ Desaprobada' },
    ACTIVA:      { bg: '#dbeafe', color: '#1d4ed8', label: '📖 Cursando' },
  };
  const s = map[estado] ?? { bg: '#f1f5f9', color: '#475569', label: estado };
  return (
    <span style={{
      padding: '3px 10px', borderRadius: '20px', fontSize: '12px',
      fontWeight: 700, background: s.bg, color: s.color,
    }}>
      {s.label}
      {cerrada && (
        <span style={{ marginLeft: '6px', fontSize: '11px', opacity: 0.7 }}>🔒</span>
      )}
    </span>
  );
}

function NotaCircle({ label, value }: { label: string; value?: number }) {
  const isEmpty = value === undefined || value === null;
  const color = isEmpty ? '#94a3b8' : notaColor(value!);
  return (
    <div style={{ textAlign: 'center', minWidth: '70px' }}>
      <div style={{
        width: '52px', height: '52px', borderRadius: '50%', margin: '0 auto',
        border: `3px solid ${isEmpty ? '#e2e8f0' : color}`,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: isEmpty ? '18px' : '18px', fontWeight: 700,
        color: isEmpty ? '#cbd5e1' : color,
        background: isEmpty ? '#f8fafc' : `${color}15`,
      }}>
        {isEmpty ? '–' : value!.toFixed(1)}
      </div>
      <div style={{ fontSize: '11px', color: '#64748b', marginTop: '5px' }}>{label}</div>
    </div>
  );
}

// ─── Fila de la tabla ────────────────────────────────────────────────────────

function FilaMateria({ ins }: { ins: Inscripcion }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <>
      <tr
        onClick={() => setExpanded(e => !e)}
        style={{ cursor: 'pointer', transition: 'background 0.1s' }}
        onMouseEnter={e => (e.currentTarget.style.background = '#f8fafc')}
        onMouseLeave={e => (e.currentTarget.style.background = 'white')}
      >
        <td style={{ padding: '14px 16px' }}>
          <div style={{ fontWeight: 600, color: '#1e293b', fontSize: '14px' }}>
            {ins.materia.nombre}
          </div>
          <div style={{ fontSize: '12px', color: '#94a3b8', marginTop: '2px' }}>
            {ins.materia.codigo} · {ins.materia.anio}° año · Cuatrimestre {ins.materia.cuatrimestre}
          </div>
        </td>
        <td style={{ padding: '14px 16px', textAlign: 'center' }}>
          {estadoBadge(ins.estado, ins.notaCerrada)}
        </td>
        <td style={{ padding: '14px 16px', textAlign: 'center', fontWeight: 700,
            fontSize: '18px',
            color: ins.notaFinal !== undefined ? notaColor(ins.notaFinal) : '#cbd5e1' }}>
          {ins.notaFinal !== undefined ? ins.notaFinal.toFixed(1) : '–'}
        </td>
        <td style={{ padding: '14px 16px', textAlign: 'center', color: '#64748b', fontSize: '13px' }}>
          {ins.asistencias !== undefined ? `${ins.asistencias} clases` : '–'}
        </td>
        <td style={{ padding: '14px 16px', textAlign: 'center', color: '#94a3b8', fontSize: '18px' }}>
          {expanded ? '▲' : '▼'}
        </td>
      </tr>

      {/* Detalle expandible */}
      {expanded && (
        <tr>
          <td colSpan={5} style={{ padding: 0 }}>
            <div style={{
              background: '#f8fafc', borderTop: '1px solid #e2e8f0',
              borderBottom: '1px solid #e2e8f0', padding: '20px 24px',
              display: 'flex', alignItems: 'center', gap: '32px', flexWrap: 'wrap',
            }}>
              <NotaCircle label="Parcial 1" value={ins.notaParcial1} />
              <div style={{ fontSize: '20px', color: '#e2e8f0', fontWeight: 300 }}>+</div>
              <NotaCircle label="Parcial 2" value={ins.notaParcial2} />
              <div style={{ fontSize: '20px', color: '#e2e8f0', fontWeight: 300 }}>=</div>
              <NotaCircle label="Nota Final" value={ins.notaFinal} />

              <div style={{ marginLeft: 'auto', textAlign: 'right' }}>
                {ins.notaCerrada ? (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px',
                      color: '#64748b', fontSize: '13px' }}>
                    🔒 <span>Nota cerrada por el docente</span>
                  </div>
                ) : (
                  <div style={{ color: '#94a3b8', fontSize: '13px' }}>
                    ⏳ En proceso de evaluación
                  </div>
                )}
                <div style={{ marginTop: '6px', color: '#94a3b8', fontSize: '12px' }}>
                  Inscripto el {new Date(ins.fechaInscripcion).toLocaleDateString('es-AR')}
                </div>
              </div>
            </div>
          </td>
        </tr>
      )}
    </>
  );
}

// ─── Componente principal ────────────────────────────────────────────────────

const Boletin: React.FC = () => {
  const [inscripciones, setInscripciones] = useState<Inscripcion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filtro, setFiltro] = useState<'TODAS' | 'APROBADA' | 'DESAPROBADA' | 'ACTIVA'>('TODAS');

  useEffect(() => {
    materiaService.misNotas()
      .then(setInscripciones)
      .catch(() => setError('No se pudieron cargar las notas. Intentá más tarde.'))
      .finally(() => setLoading(false));
  }, []);

  const filtradas = filtro === 'TODAS'
    ? inscripciones
    : inscripciones.filter(i => i.estado === filtro);

  // Estadísticas
  const aprobadas   = inscripciones.filter(i => i.estado === 'APROBADA').length;
  const desaprobadas = inscripciones.filter(i => i.estado === 'DESAPROBADA').length;
  const cursando    = inscripciones.filter(i => i.estado === 'ACTIVA').length;
  const notas       = inscripciones.filter(i => i.notaFinal !== undefined).map(i => i.notaFinal!);
  const promedio    = notas.length ? (notas.reduce((a, b) => a + b, 0) / notas.length) : null;

  if (loading) return (
    <div style={{ textAlign: 'center', padding: '60px', color: '#94a3b8' }}>
      ⏳ Cargando boletín...
    </div>
  );

  if (error) return (
    <div style={{
      background: '#fee2e2', color: '#b91c1c', borderRadius: '10px',
      padding: '16px 20px', margin: '20px 0',
    }}>
      ⚠️ {error}
    </div>
  );

  return (
    <div>
      {/* Encabezado */}
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ margin: 0, color: '#1e293b', fontSize: '22px' }}>📋 Mi Boletín</h2>
        <p style={{ margin: '4px 0 0', color: '#64748b', fontSize: '14px' }}>
          Consultá tus calificaciones. Hacé clic en una materia para ver el detalle de parciales.
        </p>
      </div>

      {/* Tarjetas de resumen */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))',
        gap: '14px', marginBottom: '28px',
      }}>
        {[
          { label: 'Aprobadas',    value: aprobadas,   bg: '#dcfce7', color: '#15803d', icon: '✔' },
          { label: 'Desaprobadas', value: desaprobadas, bg: '#fee2e2', color: '#b91c1c', icon: '✘' },
          { label: 'Cursando',     value: cursando,    bg: '#dbeafe', color: '#1d4ed8', icon: '📖' },
          {
            label: 'Promedio',
            value: promedio !== null ? promedio.toFixed(2) : '–',
            bg: '#fef9c3', color: '#854d0e', icon: '⭐',
          },
        ].map(card => (
          <div key={card.label} style={{
            background: card.bg, borderRadius: '12px', padding: '18px 16px',
            display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '6px',
          }}>
            <span style={{ fontSize: '24px' }}>{card.icon}</span>
            <span style={{ fontSize: '28px', fontWeight: 800, color: card.color }}>{card.value}</span>
            <span style={{ fontSize: '12px', color: card.color, fontWeight: 600 }}>{card.label}</span>
          </div>
        ))}
      </div>

      {/* Filtros */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '16px', flexWrap: 'wrap' }}>
        {(['TODAS', 'APROBADA', 'DESAPROBADA', 'ACTIVA'] as const).map(f => (
          <button
            key={f}
            onClick={() => setFiltro(f)}
            style={{
              padding: '6px 16px', borderRadius: '20px', cursor: 'pointer',
              border: filtro === f ? '2px solid #0369a1' : '2px solid #e2e8f0',
              background: filtro === f ? '#0369a1' : 'white',
              color: filtro === f ? 'white' : '#475569',
              fontSize: '13px', fontWeight: 600, transition: 'all 0.15s',
            }}
          >
            {f === 'TODAS' ? 'Todas' : f === 'APROBADA' ? '✔ Aprobadas' : f === 'DESAPROBADA' ? '✘ Desaprobadas' : '📖 Cursando'}
          </button>
        ))}
      </div>

      {/* Tabla */}
      {filtradas.length === 0 ? (
        <div style={{
          textAlign: 'center', padding: '60px', color: '#94a3b8',
          background: '#f8fafc', borderRadius: '12px',
        }}>
          📭 No hay materias con notas registradas{filtro !== 'TODAS' ? ` en estado "${filtro}"` : ''}.
        </div>
      ) : (
        <div style={{ borderRadius: '12px', overflow: 'hidden', border: '1px solid #e2e8f0' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', background: 'white' }}>
            <thead>
              <tr style={{ background: '#f8fafc', borderBottom: '2px solid #e2e8f0' }}>
                <th style={{ padding: '12px 16px', textAlign: 'left', color: '#475569', fontSize: '13px', fontWeight: 600 }}>Materia</th>
                <th style={{ padding: '12px 16px', textAlign: 'center', color: '#475569', fontSize: '13px', fontWeight: 600 }}>Estado</th>
                <th style={{ padding: '12px 16px', textAlign: 'center', color: '#475569', fontSize: '13px', fontWeight: 600 }}>Nota Final</th>
                <th style={{ padding: '12px 16px', textAlign: 'center', color: '#475569', fontSize: '13px', fontWeight: 600 }}>Asistencias</th>
                <th style={{ padding: '12px 16px', textAlign: 'center', color: '#475569', fontSize: '13px', fontWeight: 600 }}>Detalle</th>
              </tr>
            </thead>
            <tbody>
              {filtradas.map((ins, idx) => (
                <React.Fragment key={ins.idInscripcion}>
                  {idx > 0 && (
                    <tr><td colSpan={5} style={{ borderTop: '1px solid #f1f5f9', padding: 0 }} /></tr>
                  )}
                  <FilaMateria ins={ins} />
                </React.Fragment>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default Boletin;
