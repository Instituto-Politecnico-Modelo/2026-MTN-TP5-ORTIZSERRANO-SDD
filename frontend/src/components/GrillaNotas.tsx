import React, { useEffect, useState, useCallback } from 'react';
import docenteService, { NotaRequest } from '../services/docente.service';
import { Inscripcion, Materia } from '../services/materia.service';
// ─── helpers ────────────────────────────────────────────────────────────────

function estadoColor(estado: string): { bg: string; color: string } {
  switch (estado) {
    case 'APROBADA':    return { bg: '#dcfce7', color: '#15803d' };
    case 'DESAPROBADA': return { bg: '#fee2e2', color: '#b91c1c' };
    case 'ACTIVA':      return { bg: '#dbeafe', color: '#1d4ed8' };
    default:            return { bg: '#f1f5f9', color: '#475569' };
  }
}

function notaColor(n: number | undefined): string {
  if (n === undefined) return '#94a3b8';
  if (n >= 7) return '#15803d';
  if (n >= 6) return '#b45309';
  return '#b91c1c';
}

// ─── Fila editable ──────────────────────────────────────────────────────────

interface FilaProps {
  ins: Inscripcion;
  onGuardado: (updated: Inscripcion) => void;
  onCerrado: (updated: Inscripcion) => void;
}

const FilaInscripcion: React.FC<FilaProps> = ({ ins, onGuardado, onCerrado }) => {
  const [p1, setP1] = useState<string>(ins.notaParcial1?.toString() ?? '');
  const [p2, setP2] = useState<string>(ins.notaParcial2?.toString() ?? '');
  const [nf, setNf] = useState<string>(ins.notaFinal?.toString() ?? '');
  const [asist, setAsist] = useState<string>(ins.asistencias?.toString() ?? '');
  const [saving, setSaving] = useState(false);
  const [closing, setClosing] = useState(false);
  const [msg, setMsg] = useState<{ text: string; ok: boolean } | null>(null);
  const cerrada = ins.notaCerrada;

  const parseOpt = (v: string): number | undefined =>
    v.trim() === '' ? undefined : parseFloat(v);

  const handleGuardar = async () => {
    setSaving(true);
    setMsg(null);
    try {
      const nota: NotaRequest = {
        notaParcial1: parseOpt(p1),
        notaParcial2: parseOpt(p2),
        notaFinal: parseOpt(nf),
        asistencias: asist.trim() === '' ? undefined : parseInt(asist, 10),
      };
      const updated = await docenteService.cargarNota(ins.idInscripcion, nota);
      onGuardado(updated);
      setMsg({ text: 'Guardado', ok: true });
    } catch {
      setMsg({ text: 'Error al guardar', ok: false });
    } finally {
      setSaving(false);
      setTimeout(() => setMsg(null), 3000);
    }
  };

  const handleCerrar = async () => {
    if (!window.confirm('¿Cerrar la nota definitivamente? No podrá modificarse luego.')) return;
    setClosing(true);
    setMsg(null);
    try {
      const updated = await docenteService.cerrarNota(ins.idInscripcion);
      onCerrado(updated);
      setMsg({ text: 'Nota cerrada', ok: true });
    } catch {
      setMsg({ text: 'Error al cerrar', ok: false });
    } finally {
      setClosing(false);
      setTimeout(() => setMsg(null), 3000);
    }
  };

  const inputStyle: React.CSSProperties = {
    width: '70px', padding: '6px 8px', border: '1px solid #e2e8f0',
    borderRadius: '6px', fontSize: '13px', textAlign: 'center',
    background: cerrada ? '#f8fafc' : 'white', color: '#1e293b',
  };

  const { bg, color } = estadoColor(ins.estado);

  return (
    <tr style={{ borderBottom: '1px solid #f1f5f9' }}>
      {/* Alumno */}
      <td style={{ padding: '12px 14px' }}>
        <div style={{ fontWeight: 600, color: '#1e293b', fontSize: '14px' }}>
          {ins.estudiante
            ? `${ins.estudiante.apellido}, ${ins.estudiante.nombre}`
            : `Inscripción #${ins.idInscripcion}`}
        </div>
        <div style={{ fontSize: '12px', color: '#94a3b8' }}>
          {ins.estudiante?.email ?? ''}
        </div>
      </td>

      {/* Parcial 1 */}
      <td style={{ padding: '12px 10px', textAlign: 'center' }}>
        <input
          type="number" min={0} max={10} step={0.5}
          value={p1}
          onChange={e => setP1(e.target.value)}
          disabled={cerrada}
          style={{ ...inputStyle, color: notaColor(parseOpt(p1)) }}
        />
      </td>

      {/* Parcial 2 */}
      <td style={{ padding: '12px 10px', textAlign: 'center' }}>
        <input
          type="number" min={0} max={10} step={0.5}
          value={p2}
          onChange={e => setP2(e.target.value)}
          disabled={cerrada}
          style={{ ...inputStyle, color: notaColor(parseOpt(p2)) }}
        />
      </td>

      {/* Nota Final */}
      <td style={{ padding: '12px 10px', textAlign: 'center' }}>
        <input
          type="number" min={0} max={10} step={0.5}
          value={nf}
          onChange={e => setNf(e.target.value)}
          disabled={cerrada}
          style={{ ...inputStyle, fontWeight: 700, fontSize: '15px', color: notaColor(parseOpt(nf)) }}
        />
      </td>

      {/* Asistencias */}
      <td style={{ padding: '12px 10px', textAlign: 'center' }}>
        <input
          type="number" min={0} step={1}
          value={asist}
          onChange={e => setAsist(e.target.value)}
          disabled={cerrada}
          style={{ ...inputStyle, width: '60px' }}
        />
      </td>

      {/* Estado */}
      <td style={{ padding: '12px 10px', textAlign: 'center' }}>
        <span style={{
          padding: '3px 10px', borderRadius: '20px', fontSize: '12px',
          fontWeight: 700, background: bg, color,
        }}>
          {ins.estado}
          {cerrada && <span style={{ marginLeft: '5px' }}>🔒</span>}
        </span>
      </td>

      {/* Acciones */}
      <td style={{ padding: '12px 10px', textAlign: 'center', minWidth: '200px' }}>
        <div style={{ display: 'flex', gap: '6px', justifyContent: 'center', alignItems: 'center' }}>
          {msg && (
            <span style={{
              fontSize: '12px', fontWeight: 600,
              color: msg.ok ? '#15803d' : '#b91c1c',
            }}>
              {msg.ok ? '✔' : '✘'} {msg.text}
            </span>
          )}
          {!cerrada && (
            <>
              <button
                onClick={handleGuardar}
                disabled={saving}
                style={{
                  padding: '6px 12px', borderRadius: '6px', cursor: 'pointer',
                  background: saving ? '#93c5fd' : '#1d4ed8', color: 'white',
                  border: 'none', fontSize: '12px', fontWeight: 600,
                }}
              >
                {saving ? '...' : '💾 Guardar'}
              </button>
              <button
                onClick={handleCerrar}
                disabled={closing || !nf}
                title={!nf ? 'Debe cargar la nota final primero' : 'Cerrar nota definitivamente'}
                style={{
                  padding: '6px 12px', borderRadius: '6px', cursor: nf ? 'pointer' : 'not-allowed',
                  background: closing ? '#fca5a5' : '#dc2626', color: 'white',
                  border: 'none', fontSize: '12px', fontWeight: 600,
                  opacity: !nf ? 0.5 : 1,
                }}
              >
                {closing ? '...' : '🔒 Cerrar'}
              </button>
            </>
          )}
          {cerrada && (
            <span style={{ fontSize: '12px', color: '#94a3b8' }}>Nota cerrada</span>
          )}
        </div>
      </td>
    </tr>
  );
};

// ─── Componente principal ────────────────────────────────────────────────────

const GrillaNotas: React.FC = () => {
  const [materias, setMaterias] = useState<Materia[]>([]);
  const [idMateriaSelected, setIdMateriaSelected] = useState<number | null>(null);
  const [inscripciones, setInscripciones] = useState<Inscripcion[]>([]);
  const [loadingMaterias, setLoadingMaterias] = useState(true);
  const [loadingAlumnos, setLoadingAlumnos] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Cargar materias al montar
  useEffect(() => {
    docenteService.listarMaterias()
      .then(setMaterias)
      .catch(() => setError('No se pudieron cargar las materias.'))
      .finally(() => setLoadingMaterias(false));
  }, []);

  // Cargar alumnos cuando se elige materia
  const cargarAlumnos = useCallback(async (idMateria: number) => {
    setLoadingAlumnos(true);
    setError(null);
    try {
      const data = await docenteService.alumnosPorMateria(idMateria);
      setInscripciones(data);
    } catch {
      setError('No se pudieron cargar los alumnos de esta materia.');
    } finally {
      setLoadingAlumnos(false);
    }
  }, []);

  const handleSelectMateria = (id: number) => {
    setIdMateriaSelected(id);
    cargarAlumnos(id);
  };

  const handleGuardado = (updated: Inscripcion) => {
    setInscripciones(prev => prev.map(i => i.idInscripcion === updated.idInscripcion ? updated : i));
  };

  const handleCerrado = (updated: Inscripcion) => {
    setInscripciones(prev => prev.map(i => i.idInscripcion === updated.idInscripcion ? updated : i));
  };

  // Estadísticas rápidas
  const total     = inscripciones.length;
  const conNota   = inscripciones.filter(i => i.notaFinal !== undefined).length;
  const cerradas  = inscripciones.filter(i => i.notaCerrada).length;
  const aprobadas = inscripciones.filter(i => i.estado === 'APROBADA').length;

  const materiaSeleccionada = materias.find(m => m.idMateria === idMateriaSelected);

  return (
    <div>
      {/* Encabezado */}
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ margin: 0, color: '#1e293b', fontSize: '22px' }}>✏️ Grilla de Notas</h2>
        <p style={{ margin: '4px 0 0', color: '#64748b', fontSize: '14px' }}>
          Seleccioná una materia para ver y editar las notas de los alumnos.
        </p>
      </div>

      {/* Selector de materia */}
      <div style={{
        background: 'white', borderRadius: '12px', padding: '20px 24px',
        border: '1px solid #e2e8f0', marginBottom: '24px',
        display: 'flex', alignItems: 'center', gap: '16px', flexWrap: 'wrap',
      }}>
        <label style={{ fontWeight: 600, color: '#374151', fontSize: '14px', whiteSpace: 'nowrap' }}>
          📚 Materia:
        </label>
        {loadingMaterias ? (
          <span style={{ color: '#94a3b8' }}>Cargando materias...</span>
        ) : (
          <select
            value={idMateriaSelected ?? ''}
            onChange={e => handleSelectMateria(Number(e.target.value))}
            style={{
              padding: '8px 14px', borderRadius: '8px', border: '1px solid #d1d5db',
              fontSize: '14px', color: '#1e293b', background: 'white',
              minWidth: '280px', cursor: 'pointer',
            }}
          >
            <option value="">-- Seleccionar materia --</option>
            {materias.map(m => (
              <option key={m.idMateria} value={m.idMateria}>
                [{m.codigo}] {m.nombre} — {m.anio}° año, Cuatrimestre {m.cuatrimestre}
              </option>
            ))}
          </select>
        )}

        {materiaSeleccionada && (
          <span style={{
            marginLeft: 'auto', padding: '4px 14px', borderRadius: '20px',
            background: '#dbeafe', color: '#1d4ed8', fontSize: '12px', fontWeight: 600,
          }}>
            {materiaSeleccionada.creditos} créditos
          </span>
        )}
      </div>

      {/* Estado vacío */}
      {!idMateriaSelected && (
        <div style={{
          textAlign: 'center', padding: '60px', color: '#94a3b8',
          background: '#f8fafc', borderRadius: '12px',
        }}>
          📋 Seleccioná una materia para ver la grilla de notas.
        </div>
      )}

      {/* Loading alumnos */}
      {loadingAlumnos && (
        <div style={{ textAlign: 'center', padding: '40px', color: '#94a3b8' }}>
          ⏳ Cargando alumnos...
        </div>
      )}

      {/* Error */}
      {error && (
        <div style={{
          background: '#fee2e2', color: '#b91c1c', borderRadius: '10px',
          padding: '14px 20px', marginBottom: '16px',
        }}>
          ⚠️ {error}
        </div>
      )}

      {/* Tabla de notas */}
      {!loadingAlumnos && idMateriaSelected && !error && (
        <>
          {/* Estadísticas */}
          <div style={{
            display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))',
            gap: '12px', marginBottom: '20px',
          }}>
            {[
              { label: 'Inscriptos', value: total,     bg: '#f1f5f9', color: '#475569' },
              { label: 'Con nota',   value: conNota,   bg: '#dbeafe', color: '#1d4ed8' },
              { label: 'Cerradas',   value: cerradas,  bg: '#fef9c3', color: '#854d0e' },
              { label: 'Aprobados',  value: aprobadas, bg: '#dcfce7', color: '#15803d' },
            ].map(s => (
              <div key={s.label} style={{
                background: s.bg, borderRadius: '10px', padding: '14px',
                textAlign: 'center',
              }}>
                <div style={{ fontSize: '26px', fontWeight: 800, color: s.color }}>{s.value}</div>
                <div style={{ fontSize: '12px', color: s.color, fontWeight: 600 }}>{s.label}</div>
              </div>
            ))}
          </div>

          {inscripciones.length === 0 ? (
            <div style={{
              textAlign: 'center', padding: '40px', color: '#94a3b8',
              background: '#f8fafc', borderRadius: '12px',
            }}>
              😶 No hay alumnos inscriptos en esta materia.
            </div>
          ) : (
            <div style={{ borderRadius: '12px', overflow: 'auto', border: '1px solid #e2e8f0' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', background: 'white', minWidth: '800px' }}>
                <thead>
                  <tr style={{ background: '#f8fafc', borderBottom: '2px solid #e2e8f0' }}>
                    {['Alumno', 'Parcial 1', 'Parcial 2', 'Nota Final', 'Asistencias', 'Estado', 'Acciones'].map(h => (
                      <th key={h} style={{
                        padding: '12px 10px', textAlign: h === 'Alumno' ? 'left' : 'center',
                        color: '#475569', fontSize: '12px', fontWeight: 700,
                        textTransform: 'uppercase', letterSpacing: '0.5px',
                      }}>
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {inscripciones.map(ins => (
                    <FilaInscripcion
                      key={ins.idInscripcion}
                      ins={ins}
                      onGuardado={handleGuardado}
                      onCerrado={handleCerrado}
                    />
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default GrillaNotas;
