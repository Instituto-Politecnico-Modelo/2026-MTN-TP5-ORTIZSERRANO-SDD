import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/auth.service';
import VistaAuditoria from './VistaAuditoria';
import GestionUsuarios from './GestionUsuarios';
import adminService from '../services/admin.service';
import type { CorrelatividadResponse } from '../services/admin.service';
import axios from 'axios';

const API = 'http://localhost:8080/api';

type Vista = 'inicio' | 'usuarios' | 'materias' | 'correlatividades' | 'planes' | 'turnos' | 'auditoria';

// ─── helpers ────────────────────────────────────────────────────────────────
const headers = () => authService.getAuthHeader();

const btnStyle = (color: string): React.CSSProperties => ({
  padding: '10px 18px', background: color, color: 'white',
  border: 'none', borderRadius: '8px', cursor: 'pointer',
  fontSize: '14px', fontWeight: 600,
});

const inputStyle: React.CSSProperties = {
  width: '100%', padding: '9px 12px', border: '1px solid #e2e8f0',
  borderRadius: '8px', fontSize: '14px', boxSizing: 'border-box',
};

const CARRERAS_MATERIA = [
  'Ingeniería en Sistemas de Información',
  'Ingeniería Industrial',
  'Ingeniería Civil',
  'Ingeniería Eléctrica',
  'Ingeniería Mecánica',
  'Ingeniería Electrónica',
  'Tecnicatura en Programación',
  'Licenciatura en Administración',
  'Contador Público Nacional',
];

const labelStyle: React.CSSProperties = {
  display: 'block', marginBottom: '4px', fontSize: '13px',
  fontWeight: 600, color: '#374151',
};

// ─── Tab ────────────────────────────────────────────────────────────────────
interface TabProps { label: string; icon: string; active: boolean; onClick: () => void; }
const Tab: React.FC<TabProps> = ({ label, icon, active, onClick }) => (
  <button onClick={onClick} style={{
    padding: '8px 18px', border: 'none', cursor: 'pointer',
    background: active ? 'white' : 'transparent',
    color: active ? '#7c3aed' : 'rgba(255,255,255,0.75)',
    borderRadius: '8px', fontSize: '14px', fontWeight: active ? 700 : 500,
    display: 'flex', alignItems: 'center', gap: '6px',
    boxShadow: active ? '0 2px 8px rgba(0,0,0,0.10)' : 'none', transition: 'all 0.15s',
  }}>
    {icon} {label}
  </button>
);

// ─── Card de sección ────────────────────────────────────────────────────────
const SectionCard: React.FC<{ title: string; children: React.ReactNode }> = ({ title, children }) => (
  <div style={{
    background: 'white', borderRadius: '14px', padding: '24px 28px',
    boxShadow: '0 2px 10px rgba(0,0,0,0.06)', marginBottom: '24px',
  }}>
    <h3 style={{ margin: '0 0 18px', color: '#1e293b' }}>{title}</h3>
    {children}
  </div>
);

// ─── Main ────────────────────────────────────────────────────────────────────
const DashboardAdmin: React.FC = () => {
  const navigate = useNavigate();
  const user = authService.getCurrentUserData();
  const [vista, setVista] = useState<Vista>('inicio');
  const [msg, setMsg] = useState('');
  const [err, setErr] = useState('');

  // Verificar sesión al montar
  useEffect(() => {
    if (!authService.isAuthenticated()) {
      navigate('/login');
    }
  }, [navigate]);

  const ok  = (m: string) => { setMsg(m); setErr(''); setTimeout(() => setMsg(''), 3500); };
  const bad = (m: string) => { setErr(m); setMsg(''); setTimeout(() => setErr(''), 4000); };

  // ── Planes de Estudio ────────────────────────────────────────────────────
  const [planes, setPlanes] = useState<any[]>([]);

  const cargarPlanes = useCallback(async () => {
    try { const r = await axios.get(`${API}/admin/planes-estudio`, { headers: headers() }); setPlanes(Array.isArray(r.data) ? r.data : []); }
    catch { /* ignore */ }
  }, []);

  // ── Materias ──────────────────────────────────────────────────────────────
  const [materias, setMaterias]     = useState<any[]>([]);
  const [docentes, setDocentes]     = useState<any[]>([]);
  const [editandoMat, setEditandoMat] = useState<{ id: number; idDocente: string } | null>(null);
  const [nuevaMat, setNuevaMat]     = useState({ codigo:'', nombre:'', descripcion:'', anio:'', cuposMaximos:'', carrera:'', idDocente:'', tipoMateria:'', horasSemanales:'', horasTotales:'', idPlanEstudio:'' });
  const [nuevaMatCorrs, setNuevaMatCorrs] = useState<{ idMateriaRequerida: number; tipo: 'APROBADA' | 'CURSADA' }[]>([]);
  const [corrSelector, setCorrSelector] = useState({ idMateriaRequerida: '', tipo: 'APROBADA' as 'APROBADA' | 'CURSADA' });

  const cargarMaterias = useCallback(async () => {
    try { const r = await axios.get(`${API}/admin/materias`, { headers: headers() }); setMaterias(Array.isArray(r.data) ? r.data : []); }
    catch { bad('No se pudieron cargar las materias'); }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const cargarDocentes = useCallback(async () => {
    try { const r = await axios.get(`${API}/admin/usuarios/docentes`, { headers: headers() }); setDocentes(Array.isArray(r.data) ? r.data : []); }
    catch { bad('No se pudieron cargar los docentes'); }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const crearMateria = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const authHeaders = { ...headers(), 'Content-Type': 'application/json' };
      await axios.post(`${API}/admin/materias`, {
        ...nuevaMat,
        anio: nuevaMat.anio ? +nuevaMat.anio : null,
        cuposMaximos: nuevaMat.cuposMaximos ? +nuevaMat.cuposMaximos : null,
        carrera: nuevaMat.carrera || null,
        idDocente: nuevaMat.idDocente ? +nuevaMat.idDocente : null,
        tipoMateria: nuevaMat.tipoMateria || null,
        horasSemanales: nuevaMat.horasSemanales ? +nuevaMat.horasSemanales : null,
        horasTotales: nuevaMat.horasTotales ? +nuevaMat.horasTotales : null,
        idPlanEstudio: nuevaMat.idPlanEstudio ? +nuevaMat.idPlanEstudio : null,
        correlatividades: nuevaMatCorrs.length > 0 ? nuevaMatCorrs : undefined,
      }, { headers: authHeaders });
      ok('Materia creada exitosamente');
      setNuevaMat({ codigo:'', nombre:'', descripcion:'', anio:'', cuposMaximos:'', carrera:'', idDocente:'', tipoMateria:'', horasSemanales:'', horasTotales:'', idPlanEstudio:'' });
      setNuevaMatCorrs([]);
      setCorrSelector({ idMateriaRequerida: '', tipo: 'APROBADA' });
      cargarMaterias();
    } catch (ex: any) {
      console.error('Error al crear materia:', ex.response?.status, ex.response?.data);
      bad(ex.response?.data?.message || ex.response?.data?.error || `Error ${ex.response?.status || ''}: ${ex.message}`);
    }
  };

  // cargar datos al cambiar de vista
  useEffect(() => {
    if (vista === 'materias') { cargarMaterias(); cargarDocentes(); cargarPlanes(); }
    if (vista === 'planes') { cargarPlanes(); }
  }, [vista, cargarMaterias, cargarDocentes, cargarPlanes]);

  // ─── Render ───────────────────────────────────────────────────────────────
  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#fdf4ff' }}>

      {/* HEADER */}
      <header style={{ background: 'linear-gradient(135deg, #7c3aed 0%, #4c1d95 100%)', color: 'white', padding: '16px 40px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
          <div>
            <h1 style={{ margin: 0, fontSize: '20px' }}>Panel de Administración - Decanato</h1>
            <p style={{ margin: '2px 0 0', opacity: 0.85, fontSize: '13px' }}>{user?.nombre} {user?.apellido} — {user?.email}</p>
          </div>
          <div style={{ display: 'flex', gap: '8px' }}>
            <button onClick={() => navigate('/perfil')} style={btnStyle('rgba(255,255,255,0.15)')}>
              👤 Mi Perfil
            </button>
            <button onClick={() => { authService.logout(); navigate('/login'); }} style={btnStyle('rgba(255,255,255,0.15)')}>
              Cerrar Sesión
            </button>
          </div>
        </div>
        <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
          <Tab icon="🏠" label="Inicio"       active={vista==='inicio'}      onClick={() => setVista('inicio')} />
          <Tab icon="👥" label="Usuarios"     active={vista==='usuarios'}    onClick={() => setVista('usuarios')} />
          <Tab icon="📚" label="Materias"     active={vista==='materias'}    onClick={() => setVista('materias')} />
          <Tab icon="🔗" label="Correlativas" active={vista==='correlatividades'} onClick={() => setVista('correlatividades')} />
          <Tab icon="📋" label="Plan Estudios" active={vista==='planes'}    onClick={() => setVista('planes')} />
          <Tab icon="📅" label="Turnos Examen" active={vista==='turnos'}     onClick={() => setVista('turnos')} />
          <Tab icon="🔗" label="Auditoría"    active={vista==='auditoria'}   onClick={() => setVista('auditoria')} />
        </div>
      </header>

      {/* ALERTAS GLOBALES */}
      <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '0 20px' }}>
        {msg && <div style={{ margin:'16px 0 0', padding:'12px 16px', background:'#dcfce7', color:'#15803d', borderRadius:'10px', fontWeight:600 }}>✅ {msg}</div>}
        {err && <div style={{ margin:'16px 0 0', padding:'12px 16px', background:'#fee2e2', color:'#b91c1c', borderRadius:'10px', fontWeight:600 }}>❌ {err}</div>}
      </div>

      <main style={{ maxWidth: '1200px', margin: '0 auto', padding: '28px 20px' }}>

        {/* ── INICIO ── */}
        {vista === 'inicio' && (
          <>
            <div style={{ background:'white', borderRadius:'16px', padding:'24px 28px', boxShadow:'0 2px 10px rgba(0,0,0,0.06)', marginBottom:'24px', display:'flex', alignItems:'center', gap:'20px' }}>
              <div style={{ width:'64px', height:'64px', borderRadius:'50%', background:'linear-gradient(135deg,#7c3aed,#4c1d95)', display:'flex', alignItems:'center', justifyContent:'center', color:'white', fontSize:'24px', fontWeight:700 }}>
                {user?.nombre?.charAt(0)}{user?.apellido?.charAt(0)}
              </div>
              <div>
                <h2 style={{ margin:0, color:'#1e293b' }}>Bienvenido, {user?.nombre}!</h2>
                <p style={{ margin:'4px 0 0', color:'#64748b' }}>{user?.email}</p>
                <span style={{ display:'inline-block', marginTop:'8px', padding:'4px 12px', background:'#f3e8ff', color:'#7c3aed', borderRadius:'20px', fontSize:'12px', fontWeight:600 }}>Administrador</span>
              </div>
            </div>
            <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(220px,1fr))', gap:'16px' }}>
              {[
                { icon:'👥', title:'Usuarios',     color:'#ede9fe', desc:'Gestionar roles y accesos',      action: () => setVista('usuarios'),    btnColor:'#7c3aed' },
                { icon:'📚', title:'Materias',     color:'#fef9c3', desc:'Gestionar catálogo de materias', action: () => setVista('materias'),    btnColor:'#854d0e' },
                { icon:'🔗', title:'Auditoría',    color:'#f3e8ff', desc:'Log encadenado SHA-256',         action: () => setVista('auditoria'),   btnColor:'#7c3aed' },
              ].map(card => (
                <div key={card.title} style={{ background:'white', borderRadius:'14px', padding:'24px', boxShadow:'0 2px 8px rgba(0,0,0,0.05)', border:'1px solid #f1f5f9', display:'flex', flexDirection:'column' }}>
                  <div style={{ width:'52px', height:'52px', borderRadius:'12px', background:card.color, display:'flex', alignItems:'center', justifyContent:'center', fontSize:'26px', marginBottom:'14px' }}>{card.icon}</div>
                  <h3 style={{ margin:'0 0 6px', color:'#1e293b' }}>{card.title}</h3>
                  <p style={{ margin:'0 0 18px', color:'#64748b', fontSize:'14px', flexGrow:1 }}>{card.desc}</p>
                  <button onClick={card.action} style={{ ...btnStyle(card.btnColor), width:'100%' }}>Gestionar</button>
                </div>
              ))}
            </div>
          </>
        )}

        {/* ── USUARIOS ── */}
        {vista === 'usuarios' && <GestionUsuarios />}

        {/* ── MATERIAS ── */}
        {vista === 'materias' && (
          <>
            <SectionCard title="➕ Nueva Materia">
              <form onSubmit={crearMateria}>
                <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(200px,1fr))', gap:'14px' }}>
                  {[['Código','codigo','text'],['Nombre','nombre','text'],['Descripción','descripcion','text'],['Año','anio','number'],['Cupos máximos','cuposMaximos','number']].map(([lbl,key,type]) => (
                    <div key={key}>
                      <label style={labelStyle}>{lbl}</label>
                      <input type={type} style={inputStyle} value={(nuevaMat as any)[key]} onChange={e => setNuevaMat(p => ({...p,[key]:e.target.value}))} required={['codigo','nombre'].includes(key)} />
                    </div>
                  ))}
                  <div>
                    <label style={labelStyle}>Carrera (vacío = transversal)</label>
                    <select
                      style={inputStyle}
                      value={nuevaMat.carrera}
                      onChange={e => setNuevaMat(p => ({...p, carrera: e.target.value}))}
                    >
                      <option value="">— Transversal a todas —</option>
                      {CARRERAS_MATERIA.map(c => <option key={c} value={c}>{c}</option>)}
                    </select>
                  </div>
                  <div>
                    <label style={labelStyle}>Docente (vacío = sin asignar)</label>
                    <select
                      style={inputStyle}
                      value={nuevaMat.idDocente}
                      onChange={e => setNuevaMat(p => ({...p, idDocente: e.target.value}))}
                    >
                      <option value="">— Sin asignar —</option>
                      {docentes.map(d => (
                        <option key={d.idUsuario} value={d.idUsuario}>
                          {d.apellido}, {d.nombre} — {d.email}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label style={labelStyle}>Tipo</label>
                    <select style={inputStyle} value={nuevaMat.tipoMateria} onChange={e => setNuevaMat(p => ({...p, tipoMateria: e.target.value}))}>
                      <option value="">— Sin especificar —</option>
                      <option value="OBLIGATORIA">Obligatoria</option>
                      <option value="OPTATIVA">Optativa</option>
                    </select>
                  </div>
                  <div>
                    <label style={labelStyle}>Horas semanales</label>
                    <input type="number" style={inputStyle} value={nuevaMat.horasSemanales} onChange={e => setNuevaMat(p => ({...p, horasSemanales: e.target.value}))} />
                  </div>
                  <div>
                    <label style={labelStyle}>Horas totales</label>
                    <input type="number" style={inputStyle} value={nuevaMat.horasTotales} onChange={e => setNuevaMat(p => ({...p, horasTotales: e.target.value}))} />
                  </div>
                  <div>
                    <label style={labelStyle}>Plan de estudios</label>
                    <select style={inputStyle} value={nuevaMat.idPlanEstudio} onChange={e => setNuevaMat(p => ({...p, idPlanEstudio: e.target.value}))}>
                      <option value="">— Sin plan —</option>
                      {planes.map((p: any) => (
                        <option key={p.idPlan} value={p.idPlan}>
                          {p.nombre} ({p.codigo})
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <hr style={{ margin:'20px 0', border:'none', borderTop:'1px solid #e2e8f0' }} />
                <h4 style={{ margin:'0 0 12px', color:'#1e293b', fontSize:'15px' }}>🔗 Correlatividades (materias requeridas)</h4>
                <div style={{ display:'flex', gap:'10px', alignItems:'end', flexWrap:'wrap' }}>
                  <div style={{ flex:1, minWidth:'200px' }}>
                    <label style={labelStyle}>Materia requerida</label>
                    <select
                      style={inputStyle}
                      value={corrSelector.idMateriaRequerida}
                      onChange={e => setCorrSelector(p => ({...p, idMateriaRequerida: e.target.value}))}
                    >
                      <option value="">— Seleccionar —</option>
                      {materias.map((m: any) => (
                        <option key={m.idMateria} value={m.idMateria}>
                          [{m.codigo}] {m.nombre}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div style={{ minWidth:'160px' }}>
                    <label style={labelStyle}>Tipo</label>
                    <select
                      style={inputStyle}
                      value={corrSelector.tipo}
                      onChange={e => setCorrSelector(p => ({...p, tipo: e.target.value as 'APROBADA' | 'CURSADA'}))}
                    >
                      <option value="APROBADA">Debe estar APROBADA</option>
                      <option value="CURSADA">Debe estar CURSADA</option>
                    </select>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      if (!corrSelector.idMateriaRequerida) return;
                      if (nuevaMatCorrs.some(c => c.idMateriaRequerida === +corrSelector.idMateriaRequerida)) return;
                      setNuevaMatCorrs(prev => [...prev, {
                        idMateriaRequerida: +corrSelector.idMateriaRequerida,
                        tipo: corrSelector.tipo,
                      }]);
                      setCorrSelector(p => ({...p, idMateriaRequerida: ''}));
                    }}
                    style={{
                      ...btnStyle('#1d4ed8'), padding:'9px 16px',
                      opacity: corrSelector.idMateriaRequerida ? 1 : 0.5,
                      cursor: corrSelector.idMateriaRequerida ? 'pointer' : 'not-allowed',
                    }}
                  >
                    + Agregar
                  </button>
                </div>
                {nuevaMatCorrs.length > 0 && (
                  <div style={{ display:'flex', flexDirection:'column', gap:'6px', marginTop:'12px' }}>
                    {nuevaMatCorrs.map((c, i) => {
                      const mat = materias.find((m: any) => m.idMateria === c.idMateriaRequerida);
                      return (
                        <div key={i} style={{
                          display:'flex', justifyContent:'space-between', alignItems:'center',
                          padding:'8px 12px', background:'#f8fafc', borderRadius:'8px',
                          border:'1px solid #e2e8f0', fontSize:'13px',
                        }}>
                          <span>{mat ? `[${mat.codigo}] ${mat.nombre}` : `ID: ${c.idMateriaRequerida}`} — <strong>{c.tipo === 'APROBADA' ? 'Aprobada' : 'Cursada'}</strong></span>
                          <button
                            type="button"
                            onClick={() => setNuevaMatCorrs(prev => prev.filter((_, j) => j !== i))}
                            style={{ background:'none', border:'none', cursor:'pointer', color:'#dc2626', fontSize:'16px' }}
                          >
                            ✕
                          </button>
                        </div>
                      );
                    })}
                  </div>
                )}
                <button type="submit" style={{ ...btnStyle('#854d0e'), marginTop:'16px' }}>Crear Materia</button>
              </form>
            </SectionCard>

            <SectionCard title={`📋 Materias registradas (${materias.length})`}>
              {materias.length === 0 ? <p style={{ color:'#94a3b8' }}>No hay materias registradas</p> : (
                <div style={{ overflowX:'auto' }}>
                  <table style={{ width:'100%', borderCollapse:'collapse', fontSize:'14px' }}>
                    <thead><tr style={{ background:'#f8fafc' }}>{['Código','Nombre','Año','Tipo','Horas','Cupos','Carrera','Plan','Docente','Acciones'].map(h => <th key={h} style={{ padding:'10px 12px', textAlign:'left', color:'#64748b', fontWeight:600, borderBottom:'1px solid #e2e8f0' }}>{h}</th>)}</tr></thead>
                    <tbody>{materias.map((m: any) => (
                      <tr key={m.idMateria} style={{ borderBottom:'1px solid #f1f5f9' }}>
                        <td style={{ padding:'10px 12px', fontFamily:'monospace', fontWeight:600 }}>{m.codigo}</td>
                        <td style={{ padding:'10px 12px' }}>{m.nombre}</td>
                        <td style={{ padding:'10px 12px' }}>{m.anio || '—'}</td>
                        <td style={{ padding:'10px 12px', fontSize:'12px' }}>{m.tipoMateria ? (m.tipoMateria === 'OBLIGATORIA' ? 'Oblig' : 'Optativa') : '—'}</td>
                        <td style={{ padding:'10px 12px', fontSize:'12px' }}>{m.horasSemanales ? `${m.horasSemanales}h/s` : '—'}</td>
                        <td style={{ padding:'10px 12px' }}>{m.cuposMaximos ? `${m.cuposMaximos} cupos` : <span style={{ color:'#94a3b8' }}>Sin límite</span>}</td>
                        <td style={{ padding:'10px 12px', fontSize:'12px' }}>{m.carrera || <span style={{ color:'#94a3b8' }}>Transversal</span>}</td>
                        <td style={{ padding:'10px 12px', fontSize:'12px' }}>{m.planEstudio ? m.planEstudio.nombre : <span style={{ color:'#94a3b8' }}>—</span>}</td>
                        <td style={{ padding:'10px 12px' }}>
                          {editandoMat !== null && editandoMat.id === m.idMateria ? (
                            <select
                              value={editandoMat.idDocente}
                              onChange={e => setEditandoMat(p => p ? { ...p, idDocente: e.target.value } : null)}
                              style={{ padding:'4px 8px', borderRadius:'6px', border:'1px solid #d1d5db', fontSize:'12px', maxWidth:'180px' }}
                            >
                              <option value="">Sin asignar</option>
                              {docentes.map(d => (
                                <option key={d.idUsuario} value={d.idUsuario}>{d.apellido}, {d.nombre}</option>
                              ))}
                            </select>
                          ) : (
                            m.docente ? `${m.docente.nombre} ${m.docente.apellido}` : <span style={{ color:'#94a3b8' }}>Sin asignar</span>
                          )}
                        </td>
                        <td style={{ padding:'10px 12px', textAlign:'center' }}>
                          {editandoMat !== null && editandoMat.id === m.idMateria ? (
                            <div style={{ display:'flex', gap:'4px', justifyContent:'center' }}>
                              <button onClick={async () => {
                                const em = editandoMat;
                                if (!em) return;
                                try {
                                  await axios.put(`${API}/admin/materias/${m.idMateria}`, {
                                    codigo: m.codigo, nombre: m.nombre, descripcion: m.descripcion,
                                    anio: m.anio, cuposMaximos: m.cuposMaximos, carrera: m.carrera,
                                    idDocente: em.idDocente ? +em.idDocente : null,
                                  }, { headers: { ...headers(), 'Content-Type': 'application/json' } });
                                  ok('Docente asignado correctamente');
                                  setEditandoMat(null);
                                  cargarMaterias();
                                } catch { bad('Error al asignar docente'); }
                              }} style={{ ...btnStyle('#15803d'), padding:'4px 10px', fontSize:'12px' }}>💾</button>
                              <button onClick={() => setEditandoMat(null)} style={{ ...btnStyle('#dc2626'), padding:'4px 10px', fontSize:'12px' }}>✕</button>
                            </div>
                          ) : (
                            <button onClick={() => setEditandoMat({ id: m.idMateria, idDocente: m.docente?.idUsuario?.toString() || '' })} style={{ ...btnStyle('#7c3aed'), padding:'4px 10px', fontSize:'12px' }}>✏️ Asignar docente</button>
                          )}
                        </td>
                      </tr>
                    ))}</tbody>
                  </table>
                </div>
              )}
            </SectionCard>
          </>
        )}

        {/* ── CORRELATIVIDADES ── */}
        {vista === 'correlatividades' && (
          <CorrelatividadesAdmin />
        )}

        {/* ── PLANES DE ESTUDIO ── */}
        {vista === 'planes' && (
          <PlanEstudiosAdmin />
        )}

        {/* ── TURNOS DE EXAMEN ── */}
        {vista === 'turnos' && (
          <TurnosExamenAdmin />
        )}

        {/* ── AUDITORÍA ── */}
        {vista === 'auditoria' && (
          <div style={{ background:'white', borderRadius:'16px', padding:'28px', boxShadow:'0 2px 10px rgba(0,0,0,0.06)' }}>
            <VistaAuditoria />
          </div>
        )}

      </main>
    </div>
  );
};

// ─── Componente de gestión de correlatividades ──────────────────────────────
const btnSmall: React.CSSProperties = {
  padding: '6px 12px', borderRadius: '6px', cursor: 'pointer',
  border: 'none', fontSize: '12px', fontWeight: 600, color: 'white',
};

const CorrelatividadesAdmin: React.FC = () => {
  const [materias, setMaterias] = useState<any[]>([]);
  const [idMateriaSel, setIdMateriaSel] = useState<number | null>(null);
  const [corrs, setCorrs] = useState<CorrelatividadResponse[]>([]);
  const [idRequerida, setIdRequerida] = useState('');
  const [tipo, setTipo] = useState<'APROBADA' | 'CURSADA'>('APROBADA');
  const [msg, setMsg] = useState('');
  const [err, setErr] = useState('');

  const ok = (m: string) => { setMsg(m); setErr(''); setTimeout(() => setMsg(''), 3000); };
  const bad = (m: string) => { setErr(m); setMsg(''); setTimeout(() => setErr(''), 4000); };

  useEffect(() => {
    axios.get(`${API}/admin/materias`, { headers: headers() })
      .then(r => setMaterias(Array.isArray(r.data) ? r.data : []))
      .catch(() => bad('No se pudieron cargar las materias'));
  }, []);

  const cargarCorrs = useCallback(async (id: number) => {
    try {
      const data = await adminService.listarCorrelatividades(id);
      setCorrs(data);
    } catch { bad('Error al cargar correlatividades'); }
  }, []);

  const handleSelect = (id: number) => {
    setIdMateriaSel(id);
    setIdRequerida('');
    setTipo('APROBADA');
    cargarCorrs(id);
  };

  const handleAgregar = async () => {
    if (!idMateriaSel || !idRequerida) return;
    try {
      await adminService.crearCorrelatividad({
        idMateria: idMateriaSel,
        idMateriaRequerida: parseInt(idRequerida),
        tipo,
      });
      ok('Correlatividad agregada');
      cargarCorrs(idMateriaSel);
      setIdRequerida('');
    } catch (ex: any) {
      const m = ex?.response?.data?.message || ex.message;
      bad(m || 'Error al agregar correlatividad');
    }
  };

  const handleEliminar = async (id: number) => {
    if (!window.confirm('¿Eliminar esta correlatividad?')) return;
    try {
      await adminService.eliminarCorrelatividad(id);
      ok('Correlatividad eliminada');
      if (idMateriaSel) cargarCorrs(idMateriaSel);
    } catch { bad('Error al eliminar'); }
  };

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
      {msg && <div style={{ margin:'0 0 16px', padding:'12px 16px', background:'#dcfce7', color:'#15803d', borderRadius:'10px', fontWeight:600 }}>{msg}</div>}
      {err && <div style={{ margin:'0 0 16px', padding:'12px 16px', background:'#fee2e2', color:'#b91c1c', borderRadius:'10px', fontWeight:600 }}>{err}</div>}

      <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'24px' }}>
        {/* Selector de materia */}
        <SectionCard title="📚 Seleccionar materia">
          <select
            value={idMateriaSel ?? ''}
            onChange={e => handleSelect(Number(e.target.value))}
            style={{ width:'100%', padding:'9px 12px', border:'1px solid #e2e8f0', borderRadius:'8px', fontSize:'14px' }}
          >
            <option value="">— Seleccionar —</option>
            {materias.map((m: any) => (
              <option key={m.idMateria} value={m.idMateria}>
                [{m.codigo}] {m.nombre}
              </option>
            ))}
          </select>

          {idMateriaSel && (
            <>
              <h4 style={{ margin:'20px 0 12px', color:'#1e293b' }}>➕ Agregar correlatividad</h4>
              <div style={{ display:'flex', flexDirection:'column', gap:'10px' }}>
                <select
                  value={idRequerida}
                  onChange={e => setIdRequerida(e.target.value)}
                  style={{ padding:'9px 12px', border:'1px solid #e2e8f0', borderRadius:'8px', fontSize:'14px' }}
                >
                  <option value="">— Materia requerida —</option>
                  {materias
                    .filter((m: any) => m.idMateria !== idMateriaSel)
                    .map((m: any) => (
                      <option key={m.idMateria} value={m.idMateria}>
                        [{m.codigo}] {m.nombre}
                      </option>
                    ))}
                </select>
                <select
                  value={tipo}
                  onChange={e => setTipo(e.target.value as 'APROBADA' | 'CURSADA')}
                  style={{ padding:'9px 12px', border:'1px solid #e2e8f0', borderRadius:'8px', fontSize:'14px' }}
                >
                  <option value="APROBADA">Debe estar APROBADA</option>
                  <option value="CURSADA">Debe estar CURSADA</option>
                </select>
                <button onClick={handleAgregar} disabled={!idRequerida} style={{
                  ...btnSmall, background: idRequerida ? '#1d4ed8' : '#93c5fd',
                  padding:'10px', width:'100%', cursor: idRequerida ? 'pointer' : 'not-allowed',
                }}>
                  + Agregar correlatividad
                </button>
              </div>
            </>
          )}
        </SectionCard>

        {/* Lista de correlatividades */}
        <SectionCard title="📋 Correlatividades actuales">
          {!idMateriaSel ? (
            <p style={{ color:'#94a3b8' }}>Seleccioná una materia</p>
          ) : corrs.length === 0 ? (
            <p style={{ color:'#94a3b8' }}>Esta materia no tiene correlatividades</p>
          ) : (
            <div style={{ display:'flex', flexDirection:'column', gap:'8px' }}>
              {corrs.map(c => (
                <div key={c.idCorrelatividad} style={{
                  display:'flex', justifyContent:'space-between', alignItems:'center',
                  padding:'10px 14px', background:'#f8fafc', borderRadius:'8px',
                  border:'1px solid #e2e8f0',
                }}>
                  <div>
                    <div style={{ fontWeight:600, fontSize:'14px', color:'#1e293b' }}>
                      {c.nombreRequerida}
                    </div>
                    <div style={{ fontSize:'12px', color:'#64748b' }}>
                      {c.codigoRequerida} — {c.tipo === 'APROBADA' ? 'Aprobada' : 'Cursada'}
                    </div>
                  </div>
                  <button onClick={() => handleEliminar(c.idCorrelatividad)} style={{
                    ...btnSmall, background:'#dc2626', padding:'4px 10px',
                  }}>
                    ✕
                  </button>
                </div>
              ))}
            </div>
          )}
        </SectionCard>
      </div>
    </div>
  );
};

// ─── Componente de gestión de planes de estudio ─────────────────────────────
interface PlanForm { nombre: string; codigo: string; carrera: string; resolucion: string; anioAprobacion: string; }

const PlanEstudiosAdmin: React.FC = () => {
  const [planes, setPlanes] = useState<any[]>([]);
  const [form, setForm] = useState<PlanForm>({ nombre:'', codigo:'', carrera:'', resolucion:'', anioAprobacion:'' });
  const [editId, setEditId] = useState<number | null>(null);
  const [msg, setMsg] = useState('');
  const [err, setErr] = useState('');

  const ok = (m: string) => { setMsg(m); setErr(''); setTimeout(() => setMsg(''), 3000); };
  const bad = (m: string) => { setErr(m); setMsg(''); setTimeout(() => setErr(''), 4000); };

  const cargar = useCallback(async () => {
    try { const r = await axios.get(`${API}/admin/planes-estudio`, { headers: headers() }); setPlanes(Array.isArray(r.data) ? r.data : []); }
    catch { bad('No se pudieron cargar los planes'); }
  }, []);

  useEffect(() => { cargar(); }, [cargar]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const data = { ...form, anioAprobacion: form.anioAprobacion ? +form.anioAprobacion : null };
      const opts = { headers: { ...headers(), 'Content-Type': 'application/json' } };
      if (editId) {
        await axios.put(`${API}/admin/planes-estudio/${editId}`, data, opts);
        ok('Plan actualizado');
        setEditId(null);
      } else {
        await axios.post(`${API}/admin/planes-estudio`, data, opts);
        ok('Plan creado');
      }
      setForm({ nombre:'', codigo:'', carrera:'', resolucion:'', anioAprobacion:'' });
      cargar();
    } catch (ex: any) { bad(ex.response?.data?.message || 'Error al guardar plan'); }
  };

  const handleEditar = (p: any) => {
    setEditId(p.idPlan);
    setForm({ nombre:p.nombre, codigo:p.codigo, carrera:p.carrera, resolucion:p.resolucion || '', anioAprobacion:String(p.anioAprobacion || '') });
  };

  return (
    <div>
      {msg && <div style={{ margin:'0 0 16px', padding:'12px 16px', background:'#dcfce7', color:'#15803d', borderRadius:'10px', fontWeight:600 }}>{msg}</div>}
      {err && <div style={{ margin:'0 0 16px', padding:'12px 16px', background:'#fee2e2', color:'#b91c1c', borderRadius:'10px', fontWeight:600 }}>{err}</div>}
      <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'24px' }}>
        <SectionCard title={editId ? '✏️ Editar plan' : '➕ Nuevo plan de estudios'}>
          <form onSubmit={handleSubmit}>
            <div style={{ display:'flex', flexDirection:'column', gap:'12px' }}>
              <div>
                <label style={labelStyle}>Nombre *</label>
                <input style={inputStyle} value={form.nombre} onChange={e => setForm(p => ({...p, nombre: e.target.value}))} required />
              </div>
              <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'12px' }}>
                <div>
                  <label style={labelStyle}>Código *</label>
                  <input style={inputStyle} value={form.codigo} onChange={e => setForm(p => ({...p, codigo: e.target.value}))} required />
                </div>
                <div>
                  <label style={labelStyle}>Carrera *</label>
                  <select style={inputStyle} value={form.carrera} onChange={e => setForm(p => ({...p, carrera: e.target.value}))} required>
                    <option value="">— Seleccionar —</option>
                    {CARRERAS_MATERIA.map(c => <option key={c} value={c}>{c}</option>)}
                  </select>
                </div>
              </div>
              <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'12px' }}>
                <div>
                  <label style={labelStyle}>Resolución</label>
                  <input style={inputStyle} value={form.resolucion} onChange={e => setForm(p => ({...p, resolucion: e.target.value}))} placeholder="Ej: RES-2024-123" />
                </div>
                <div>
                  <label style={labelStyle}>Año de aprobación</label>
                  <input type="number" style={inputStyle} value={form.anioAprobacion} onChange={e => setForm(p => ({...p, anioAprobacion: e.target.value}))} />
                </div>
              </div>
              <div style={{ display:'flex', gap:'8px' }}>
                <button type="submit" style={{ ...btnStyle('#854d0e'), flex:1 }}>
                  {editId ? '💾 Guardar cambios' : '✅ Crear plan'}
                </button>
                {editId && <button type="button" onClick={() => { setEditId(null); setForm({ nombre:'', codigo:'', carrera:'', resolucion:'', anioAprobacion:'' }); }} style={{ ...btnStyle('#dc2626') }}>✕ Cancelar</button>}
              </div>
            </div>
          </form>
        </SectionCard>
        <SectionCard title={`📋 Planes de estudio (${planes.length})`}>
          {planes.length === 0 ? (
            <p style={{ color:'#94a3b8' }}>No hay planes creados</p>
          ) : (
            <div style={{ display:'flex', flexDirection:'column', gap:'8px' }}>
              {planes.map((p: any) => (
                <div key={p.idPlan} style={{
                  display:'flex', justifyContent:'space-between', alignItems:'center',
                  padding:'10px 14px', background:'#f8fafc', borderRadius:'8px',
                  border:'1px solid #e2e8f0',
                }}>
                  <div>
                    <div style={{ fontWeight:600, fontSize:'14px', color:'#1e293b' }}>
                      {p.nombre}
                      {!p.activo && <span style={{ marginLeft:'8px', padding:'2px 8px', borderRadius:'12px', fontSize:'10px', fontWeight:700, background:'#fee2e2', color:'#b91c1c' }}>Inactivo</span>}
                    </div>
                    <div style={{ fontSize:'12px', color:'#64748b', marginTop:'4px' }}>
                      {p.codigo} · {p.carrera}{p.resolucion ? ` · ${p.resolucion}` : ''}{p.anioAprobacion ? ` · ${p.anioAprobacion}` : ''}
                    </div>
                  </div>
                  <div style={{ display:'flex', gap:'4px' }}>
                    <button onClick={() => handleEditar(p)} style={{ ...btnStyle('#1d4ed8'), padding:'4px 8px', fontSize:'11px' }}>✏️</button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </SectionCard>
      </div>
    </div>
  );
};

// ─── Componente de gestión de turnos de examen ──────────────────────────────
interface TurnoForm { nombre: string; fechaInicio: string; fechaFin: string; anio: string; cuatrimestre: string; }

const TurnosExamenAdmin: React.FC = () => {
  const [turnos, setTurnos] = useState<any[]>([]);
  const [form, setForm] = useState<TurnoForm>({ nombre:'', fechaInicio:'', fechaFin:'', anio:'', cuatrimestre:'1' });
  const [editId, setEditId] = useState<number | null>(null);
  const [msg, setMsg] = useState('');
  const [err, setErr] = useState('');

  const ok = (m: string) => { setMsg(m); setErr(''); setTimeout(() => setMsg(''), 3000); };
  const bad = (m: string) => { setErr(m); setMsg(''); setTimeout(() => setErr(''), 4000); };

  const cargar = useCallback(async () => {
    try { const r = await axios.get(`${API}/admin/turnos-examen`, { headers: headers() }); setTurnos(Array.isArray(r.data) ? r.data : []); }
    catch { bad('No se pudieron cargar los turnos'); }
  }, []);

  useEffect(() => { cargar(); }, [cargar]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const data = { ...form, anio: +form.anio, cuatrimestre: +form.cuatrimestre };
      const opts = { headers: { ...headers(), 'Content-Type': 'application/json' } };
      if (editId) {
        await axios.put(`${API}/admin/turnos-examen/${editId}`, data, opts);
        ok('Turno actualizado');
        setEditId(null);
      } else {
        await axios.post(`${API}/admin/turnos-examen`, data, opts);
        ok('Turno creado');
      }
      setForm({ nombre:'', fechaInicio:'', fechaFin:'', anio:'', cuatrimestre:'1' });
      cargar();
    } catch (ex: any) { bad(ex.response?.data?.message || 'Error al guardar turno'); }
  };

  const handleEditar = (t: any) => {
    setEditId(t.idTurno);
    setForm({ nombre:t.nombre, fechaInicio:t.fechaInicio, fechaFin:t.fechaFin, anio:String(t.anio), cuatrimestre:String(t.cuatrimestre) });
  };

  const handleEliminar = async (id: number) => {
    if (!window.confirm('¿Eliminar este turno?')) return;
    try { await axios.delete(`${API}/admin/turnos-examen/${id}`, { headers: headers() }); ok('Turno eliminado'); cargar(); }
    catch { bad('Error al eliminar'); }
  };

  const handleToggle = async (id: number) => {
    try { await axios.patch(`${API}/admin/turnos-examen/${id}/toggle`, {}, { headers: headers() }); ok('Estado cambiado'); cargar(); }
    catch { bad('Error al cambiar estado'); }
  };

  return (
    <div>
      {msg && <div style={{ margin:'0 0 16px', padding:'12px 16px', background:'#dcfce7', color:'#15803d', borderRadius:'10px', fontWeight:600 }}>{msg}</div>}
      {err && <div style={{ margin:'0 0 16px', padding:'12px 16px', background:'#fee2e2', color:'#b91c1c', borderRadius:'10px', fontWeight:600 }}>{err}</div>}
      <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'24px' }}>
        <SectionCard title={editId ? '✏️ Editar turno' : '➕ Nuevo turno'}>
          <form onSubmit={handleSubmit}>
            <div style={{ display:'flex', flexDirection:'column', gap:'12px' }}>
              <div>
                <label style={labelStyle}>Nombre</label>
                <input style={inputStyle} value={form.nombre} onChange={e => setForm(p => ({...p, nombre: e.target.value}))} required placeholder="Ej: Febrero 2027" />
              </div>
              <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'12px' }}>
                <div>
                  <label style={labelStyle}>Fecha inicio</label>
                  <input type="date" style={inputStyle} value={form.fechaInicio} onChange={e => setForm(p => ({...p, fechaInicio: e.target.value}))} required />
                </div>
                <div>
                  <label style={labelStyle}>Fecha fin</label>
                  <input type="date" style={inputStyle} value={form.fechaFin} onChange={e => setForm(p => ({...p, fechaFin: e.target.value}))} required />
                </div>
              </div>
              <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'12px' }}>
                <div>
                  <label style={labelStyle}>Año</label>
                  <input type="number" style={inputStyle} value={form.anio} onChange={e => setForm(p => ({...p, anio: e.target.value}))} required min={2026} />
                </div>
                <div>
                  <label style={labelStyle}>Cuatrimestre</label>
                  <select style={inputStyle} value={form.cuatrimestre} onChange={e => setForm(p => ({...p, cuatrimestre: e.target.value}))}>
                    <option value="1">1° Cuatrimestre</option>
                    <option value="2">2° Cuatrimestre</option>
                  </select>
                </div>
              </div>
              <div style={{ display:'flex', gap:'8px' }}>
                <button type="submit" style={{ ...btnStyle('#854d0e'), flex:1 }}>
                  {editId ? '💾 Guardar cambios' : '✅ Crear turno'}
                </button>
                {editId && <button type="button" onClick={() => { setEditId(null); setForm({ nombre:'', fechaInicio:'', fechaFin:'', anio:'', cuatrimestre:'1' }); }} style={{ ...btnStyle('#dc2626') }}>✕ Cancelar</button>}
              </div>
            </div>
          </form>
        </SectionCard>
        <SectionCard title={`📋 Turnos (${turnos.length})`}>
          {turnos.length === 0 ? (
            <p style={{ color:'#94a3b8' }}>No hay turnos creados</p>
          ) : (
            <div style={{ display:'flex', flexDirection:'column', gap:'8px' }}>
              {turnos.map((t: any) => (
                <div key={t.idTurno} style={{
                  display:'flex', justifyContent:'space-between', alignItems:'center',
                  padding:'10px 14px', background:'#f8fafc', borderRadius:'8px',
                  border:'1px solid #e2e8f0',
                }}>
                  <div>
                    <div style={{ fontWeight:600, fontSize:'14px', color:'#1e293b', display:'flex', alignItems:'center', gap:'8px' }}>
                      {t.nombre}
                      <span style={{
                        padding:'2px 8px', borderRadius:'12px', fontSize:'10px', fontWeight:700,
                        background: t.activo ? '#dcfce7' : '#fee2e2',
                        color: t.activo ? '#15803d' : '#b91c1c',
                      }}>
                        {t.activo ? 'Activo' : 'Inactivo'}
                      </span>
                    </div>
                    <div style={{ fontSize:'12px', color:'#64748b', marginTop:'4px' }}>
                      📅 {new Date(t.fechaInicio).toLocaleDateString('es-AR')} → {new Date(t.fechaFin).toLocaleDateString('es-AR')}
                      &nbsp;· {t.anio} · {t.cuatrimestre}° cuatrimestre
                    </div>
                  </div>
                  <div style={{ display:'flex', gap:'4px' }}>
                    <button onClick={() => handleEditar(t)} style={{ ...btnStyle('#1d4ed8'), padding:'4px 8px', fontSize:'11px' }}>✏️</button>
                    <button onClick={() => handleToggle(t.idTurno)} style={{ ...btnStyle(t.activo ? '#854d0e' : '#15803d'), padding:'4px 8px', fontSize:'11px' }}>{t.activo ? '🔒' : '🔓'}</button>
                    <button onClick={() => handleEliminar(t.idTurno)} style={{ ...btnStyle('#dc2626'), padding:'4px 8px', fontSize:'11px' }}>🗑️</button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </SectionCard>
      </div>
    </div>
  );
};

export default DashboardAdmin;
