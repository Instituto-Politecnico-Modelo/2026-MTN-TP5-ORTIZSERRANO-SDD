import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/auth.service';
import VistaAuditoria from './VistaAuditoria';
import GestionUsuarios from './GestionUsuarios';
import axios from 'axios';

const API = 'http://localhost:8080/api';

type Vista = 'inicio' | 'usuarios' | 'materias' | 'auditoria';

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

  // ── Materias ──────────────────────────────────────────────────────────────
  const [materias, setMaterias]     = useState<any[]>([]);
  const [nuevaMat, setNuevaMat]     = useState({ codigo:'', nombre:'', descripcion:'', anio:'', cuposMaximos:'', carrera:'' });

  const cargarMaterias = useCallback(async () => {
    try { const r = await axios.get(`${API}/admin/materias`, { headers: headers() }); setMaterias(r.data); }
    catch { bad('No se pudieron cargar las materias'); }
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
      }, { headers: authHeaders });
      ok('Materia creada exitosamente');
      setNuevaMat({ codigo:'', nombre:'', descripcion:'', anio:'', cuposMaximos:'', carrera:'' });
      cargarMaterias();
    } catch (ex: any) {
      console.error('Error al crear materia:', ex.response?.status, ex.response?.data);
      bad(ex.response?.data?.message || ex.response?.data?.error || `Error ${ex.response?.status || ''}: ${ex.message}`);
    }
  };

  // cargar datos al cambiar de vista
  useEffect(() => {
    if (vista === 'materias') cargarMaterias();
  }, [vista, cargarMaterias]);

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
                </div>
                <button type="submit" style={{ ...btnStyle('#854d0e'), marginTop:'16px' }}>Crear Materia</button>
              </form>
            </SectionCard>

            <SectionCard title={`📋 Materias registradas (${materias.length})`}>
              {materias.length === 0 ? <p style={{ color:'#94a3b8' }}>No hay materias registradas</p> : (
                <div style={{ overflowX:'auto' }}>
                  <table style={{ width:'100%', borderCollapse:'collapse', fontSize:'14px' }}>
                    <thead><tr style={{ background:'#f8fafc' }}>{['Código','Nombre','Año','Cupos','Carrera','Docente'].map(h => <th key={h} style={{ padding:'10px 12px', textAlign:'left', color:'#64748b', fontWeight:600, borderBottom:'1px solid #e2e8f0' }}>{h}</th>)}</tr></thead>
                    <tbody>{materias.map((m: any) => (
                      <tr key={m.idMateria} style={{ borderBottom:'1px solid #f1f5f9' }}>
                        <td style={{ padding:'10px 12px', fontFamily:'monospace', fontWeight:600 }}>{m.codigo}</td>
                        <td style={{ padding:'10px 12px' }}>{m.nombre}</td>
                        <td style={{ padding:'10px 12px' }}>{m.anio || '—'}</td>
                        <td style={{ padding:'10px 12px' }}>{m.cuposMaximos ? `${m.cuposMaximos} cupos` : <span style={{ color:'#94a3b8' }}>Sin límite</span>}</td>
                        <td style={{ padding:'10px 12px', fontSize:'12px' }}>{m.carrera || <span style={{ color:'#94a3b8' }}>Transversal</span>}</td>
                        <td style={{ padding:'10px 12px' }}>{m.docente ? `${m.docente.nombre} ${m.docente.apellido}` : <span style={{ color:'#94a3b8' }}>Sin asignar</span>}</td>
                      </tr>
                    ))}</tbody>
                  </table>
                </div>
              )}
            </SectionCard>
          </>
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

export default DashboardAdmin;
