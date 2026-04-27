/**
 * GestionUsuarios.tsx
 * ───────────────────────────────────────────────────────────────────
 * Panel de gestión completa de usuarios para el rol ADMINISTRADOR.
 *
 * Funcionalidades:
 *  · Listado de TODOS los usuarios (cualquier rol) con su estado
 *  · Crear usuario nuevo asignando rol (ESTUDIANTE / DOCENTE / ADMINISTRADOR)
 *  · Activar / Desactivar usuario
 *  · Eliminar usuario (baja física con confirmación)
 *  · Cambiar contraseña de cualquier usuario
 *  · Ver y atender solicitudes de "Olvidé mi contraseña"
 *
 * Endpoints usados:
 *  GET  /api/admin/usuarios
 *  POST /api/admin/usuarios
 *  PUT  /api/admin/usuarios/{id}/desactivar
 *  PUT  /api/admin/usuarios/{id}/reactivar
 *  DELETE /api/admin/usuarios/{id}
 *  GET  /api/admin/usuarios/solicitudes-reset
 *  POST /api/admin/usuarios/solicitudes-reset/{id}/atender
 */

import React, { useEffect, useState, useCallback } from 'react';
import axios from 'axios';
import authService from '../services/auth.service';

const API = 'http://localhost:8080/api';
const h   = () => ({ ...authService.getAuthHeader(), 'Content-Type': 'application/json' });

// ─── Tipos ────────────────────────────────────────────────────────────────────

interface UsuarioRow {
  idUsuario: number;
  nombre: string;
  apellido: string;
  email: string;
  activo: boolean;
  // rol se detecta por qué tabla hereda (el backend devuelve el tipo de clase)
  rol?: string;
  legajo?: string;
  carrera?: string;
  titulo?: string;
  especialidad?: string;
  departamento?: string;
  area?: string;
  nivelAcceso?: number;
}

interface SolicitudReset {
  idSolicitud: number;
  email: string;
  nombreCompleto?: string;
  fechaSolicitud: string;
  estado: 'PENDIENTE' | 'ATENDIDA';
  fechaAtencion?: string;
}

// ─── Constantes de estilo ─────────────────────────────────────────────────────

const ROL_BADGE: Record<string, { bg: string; color: string; label: string }> = {
  Administrador: { bg: '#fef3c7', color: '#d97706', label: '🛡️ Admin' },
  Docente:       { bg: '#dcfce7', color: '#16a34a', label: '👨‍🏫 Docente' },
  Estudiante:    { bg: '#dbeafe', color: '#1d4ed8', label: '🎓 Estudiante' },
};

const inp: React.CSSProperties = {
  width: '100%', padding: '9px 12px', border: '1.5px solid #e2e8f0',
  borderRadius: '8px', fontSize: '14px', boxSizing: 'border-box',
  fontFamily: 'inherit', outline: 'none',
};
const lbl: React.CSSProperties = {
  display: 'block', fontSize: '13px', fontWeight: 600, color: '#475569', marginBottom: '5px',
};
const sectionTitle: React.CSSProperties = {
  fontSize: '15px', fontWeight: 700, color: '#1e293b',
  marginBottom: '18px', paddingBottom: '10px',
  borderBottom: '1px solid #e2e8f0',
  display: 'flex', alignItems: 'center', gap: '8px',
};
const card: React.CSSProperties = {
  background: 'white', borderRadius: '14px', padding: '24px 28px',
  boxShadow: '0 2px 10px rgba(0,0,0,0.06)', marginBottom: '24px',
};
const btn = (color: string, small = false): React.CSSProperties => ({
  padding: small ? '5px 12px' : '9px 18px',
  background: color, color: 'white', border: 'none',
  borderRadius: '7px', cursor: 'pointer',
  fontSize: small ? '12px' : '13px', fontWeight: 600,
});

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Extrae el rol del discriminator de la clase (Spring devuelve el tipo en el JSON) */
function detectarRol(u: any): string {
  if (u['@class']) {
    if (u['@class'].includes('Administrador')) return 'Administrador';
    if (u['@class'].includes('Docente'))       return 'Docente';
    if (u['@class'].includes('Estudiante'))    return 'Estudiante';
  }
  // Heurística: si tiene legajo → Estudiante, titulo → Docente, area → Admin
  if (u.legajo !== undefined) return 'Estudiante';
  if (u.titulo !== undefined || u.especialidad !== undefined) return 'Docente';
  if (u.area !== undefined || u.nivelAcceso !== undefined) return 'Administrador';
  return 'Usuario';
}

function formatFecha(iso: string) {
  return new Date(iso).toLocaleString('es-AR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

// ─── Componente principal ─────────────────────────────────────────────────────

type Seccion = 'usuarios' | 'crear' | 'solicitudes';

const GestionUsuarios: React.FC = () => {
  const [seccion, setSeccion] = useState<Seccion>('usuarios');
  const [globalMsg, setGlobalMsg] = useState('');
  const [globalErr, setGlobalErr] = useState('');

  const ok  = (m: string) => { setGlobalMsg(m); setGlobalErr(''); setTimeout(() => setGlobalMsg(''), 3500); };
  const bad = (m: string) => { setGlobalErr(m); setGlobalMsg(''); setTimeout(() => setGlobalErr(''), 4500); };

  // ── Listado de usuarios ──────────────────────────────────────────────────

  const [usuarios, setUsuarios]   = useState<UsuarioRow[]>([]);
  const [loadingU, setLoadingU]   = useState(false);
  const [busqueda, setBusqueda]   = useState('');
  const [filtroRol, setFiltroRol] = useState('Todos');
  const [filtroEst, setFiltroEst] = useState('Todos');

  // Estado para cambio de contraseña inline
  const [passTarget, setPassTarget]     = useState<number | null>(null);
  const [passNueva,  setPassNueva]      = useState('');
  const [savingPass, setSavingPass]     = useState(false);

  // Estado para confirmación de eliminación
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);

  const cargarUsuarios = useCallback(async () => {
    setLoadingU(true);
    try {
      const r = await axios.get(`${API}/admin/usuarios`, { headers: h() });
      const rows: UsuarioRow[] = (r.data as any[]).map(u => ({
        ...u,
        rol: detectarRol(u),
      }));
      setUsuarios(rows);
    } catch {
      bad('No se pudo cargar la lista de usuarios');
    } finally {
      setLoadingU(false);
    }
  }, []);

  useEffect(() => {
    if (seccion === 'usuarios') cargarUsuarios();
  }, [seccion, cargarUsuarios]);

  const toggleActivo = async (u: UsuarioRow) => {
    const endpoint = u.activo ? 'desactivar' : 'reactivar';
    try {
      await axios.put(`${API}/admin/usuarios/${u.idUsuario}/${endpoint}`, {}, { headers: h() });
      ok(`Usuario ${u.activo ? 'desactivado' : 'reactivado'} correctamente`);
      cargarUsuarios();
    } catch (ex: any) {
      bad(ex.response?.data?.message ?? 'Error al cambiar el estado del usuario');
    }
  };

  const eliminarUsuario = async (id: number) => {
    try {
      await axios.delete(`${API}/admin/usuarios/${id}`, { headers: h() });
      ok('Usuario eliminado permanentemente');
      setDeleteTarget(null);
      cargarUsuarios();
    } catch (ex: any) {
      bad(ex.response?.data?.message ?? 'Error al eliminar el usuario');
    }
  };

  const cambiarPassword = async (idUsuario: number) => {
    if (passNueva.length < 6) { bad('La contraseña debe tener al menos 6 caracteres'); return; }
    setSavingPass(true);
    try {
      // Usamos el endpoint de update directamente sobre el usuario (admin puede)
      await axios.put(
        `${API}/admin/usuarios/${idUsuario}/cambiar-password`,
        { nuevaPassword: passNueva },
        { headers: h() }
      );
      ok('Contraseña actualizada correctamente');
      setPassTarget(null);
      setPassNueva('');
    } catch (ex: any) {
      bad(ex.response?.data?.message ?? 'Error al cambiar la contraseña');
    } finally {
      setSavingPass(false);
    }
  };

  // Filtros
  const usuariosFiltrados = usuarios.filter(u => {
    const texto = busqueda.toLowerCase();
    const coincideTexto = !texto
      || u.nombre.toLowerCase().includes(texto)
      || u.apellido.toLowerCase().includes(texto)
      || u.email.toLowerCase().includes(texto)
      || (u.legajo ?? '').toLowerCase().includes(texto);
    const coincideRol = filtroRol === 'Todos' || u.rol === filtroRol;
    const coincideEst = filtroEst === 'Todos'
      || (filtroEst === 'Activo'   &&  u.activo)
      || (filtroEst === 'Inactivo' && !u.activo);
    return coincideTexto && coincideRol && coincideEst;
  });

  // ── Crear usuario ────────────────────────────────────────────────────────

  const EMPTY_FORM = {
    nombre:'', apellido:'', email:'', password:'',
    rol:'ESTUDIANTE',
    // Estudiante
    legajo:'', carrera:'', anioIngreso:'',
    // Docente
    titulo:'', especialidad:'', departamento:'',
    // Administrador
    area:'', nivelAcceso:'1',
  };
  const [form, setForm]       = useState(EMPTY_FORM);
  const [creando, setCreando] = useState(false);

  const f = (k: string) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm(p => ({ ...p, [k]: e.target.value }));

  const crearUsuario = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreando(true);
    try {
      const payload: any = {
        nombre: form.nombre, apellido: form.apellido,
        email: form.email,   password: form.password,
        rol: form.rol,
      };
      if (form.rol === 'ESTUDIANTE') {
        payload.legajo      = form.legajo      || undefined;
        payload.carrera     = form.carrera     || undefined;
        payload.anioIngreso = form.anioIngreso ? +form.anioIngreso : undefined;
      }
      if (form.rol === 'DOCENTE') {
        payload.titulo       = form.titulo       || undefined;
        payload.especialidad = form.especialidad || undefined;
        payload.departamento = form.departamento || undefined;
      }
      if (form.rol === 'ADMINISTRADOR') {
        payload.area        = form.area        || undefined;
        payload.nivelAcceso = form.nivelAcceso ? +form.nivelAcceso : 1;
      }
      await axios.post(`${API}/admin/usuarios`, payload, { headers: h() });
      ok(`${form.rol.charAt(0) + form.rol.slice(1).toLowerCase()} creado/a exitosamente`);
      setForm(EMPTY_FORM);
      setSeccion('usuarios');
    } catch (ex: any) {
      bad(ex.response?.data?.message ?? 'Error al crear el usuario');
    } finally {
      setCreando(false);
    }
  };

  // ── Solicitudes de reset ─────────────────────────────────────────────────

  const [solicitudes,    setSolicitudes]    = useState<SolicitudReset[]>([]);
  const [loadingSol,     setLoadingSol]     = useState(false);
  const [atendiendo,     setAtendiendo]     = useState<number | null>(null);
  const [nuevaPassReset, setNuevaPassReset] = useState('');
  const [savingReset,    setSavingReset]    = useState(false);

  const cargarSolicitudes = useCallback(async () => {
    setLoadingSol(true);
    try {
      const r = await axios.get(`${API}/admin/usuarios/solicitudes-reset`, { headers: h() });
      setSolicitudes(r.data);
    } catch {
      bad('No se pudieron cargar las solicitudes');
    } finally {
      setLoadingSol(false);
    }
  }, []);

  useEffect(() => {
    if (seccion === 'solicitudes') cargarSolicitudes();
  }, [seccion, cargarSolicitudes]);

  const atenderSolicitud = async (id: number) => {
    if (nuevaPassReset.length < 6) { bad('La contraseña debe tener al menos 6 caracteres'); return; }
    setSavingReset(true);
    try {
      await axios.post(
        `${API}/admin/usuarios/solicitudes-reset/${id}/atender`,
        { nuevaPassword: nuevaPassReset },
        { headers: h() }
      );
      ok('Contraseña asignada y solicitud marcada como ATENDIDA');
      setAtendiendo(null);
      setNuevaPassReset('');
      cargarSolicitudes();
    } catch (ex: any) {
      bad(ex.response?.data?.message ?? 'Error al atender la solicitud');
    } finally {
      setSavingReset(false);
    }
  };

  const pendientes = solicitudes.filter(s => s.estado === 'PENDIENTE');

  // ─── Render ───────────────────────────────────────────────────────────────

  return (
    <div>

      {/* ── Alertas globales ── */}
      {globalMsg && (
        <div style={{ marginBottom:'16px', padding:'12px 16px', background:'#dcfce7', color:'#15803d', borderRadius:'10px', fontWeight:600 }}>
          ✅ {globalMsg}
        </div>
      )}
      {globalErr && (
        <div style={{ marginBottom:'16px', padding:'12px 16px', background:'#fee2e2', color:'#b91c1c', borderRadius:'10px', fontWeight:600 }}>
          ❌ {globalErr}
        </div>
      )}

      {/* ── Sub-nav ── */}
      <div style={{ display:'flex', gap:'8px', marginBottom:'24px', flexWrap:'wrap' }}>
        {([
          ['usuarios',   '👥 Todos los usuarios'],
          ['crear',      '➕ Crear usuario'],
          ['solicitudes',`🔑 Reset contraseña${pendientes.length > 0 ? ` (${pendientes.length})` : ''}`],
        ] as [Seccion, string][]).map(([key, label]) => (
          <button
            key={key}
            onClick={() => setSeccion(key)}
            style={{
              padding: '9px 18px', border: 'none', cursor: 'pointer', borderRadius: '8px',
              fontSize: '14px', fontWeight: 600,
              background: seccion === key ? '#7c3aed' : '#f1f5f9',
              color: seccion === key ? 'white' : '#374151',
              boxShadow: seccion === key ? '0 2px 6px rgba(124,58,237,0.3)' : 'none',
              transition: 'all 0.15s',
            }}
          >
            {label}
          </button>
        ))}
      </div>

      {/* ════════════════════════════════════════════════════════════
          SECCIÓN: TODOS LOS USUARIOS
      ════════════════════════════════════════════════════════════ */}
      {seccion === 'usuarios' && (
        <div style={card}>
          <div style={sectionTitle}>👥 Gestión de usuarios y roles</div>

          {/* Filtros */}
          <div style={{ display:'flex', gap:'12px', marginBottom:'18px', flexWrap:'wrap' }}>
            <input
              style={{ ...inp, maxWidth:'260px' }}
              placeholder="🔍 Buscar por nombre, email o legajo..."
              value={busqueda}
              onChange={e => setBusqueda(e.target.value)}
            />
            <select style={{ ...inp, maxWidth:'160px' }} value={filtroRol} onChange={e => setFiltroRol(e.target.value)}>
              <option value="Todos">Todos los roles</option>
              <option value="Administrador">Administrador</option>
              <option value="Docente">Docente</option>
              <option value="Estudiante">Estudiante</option>
            </select>
            <select style={{ ...inp, maxWidth:'150px' }} value={filtroEst} onChange={e => setFiltroEst(e.target.value)}>
              <option value="Todos">Cualquier estado</option>
              <option value="Activo">Activos</option>
              <option value="Inactivo">Inactivos</option>
            </select>
            <button onClick={cargarUsuarios} style={btn('#64748b')}>🔄 Actualizar</button>
          </div>

          {/* Resumen */}
          <div style={{ display:'flex', gap:'12px', marginBottom:'18px', flexWrap:'wrap' }}>
            {(['Administrador','Docente','Estudiante'] as const).map(rol => {
              const count = usuarios.filter(u => u.rol === rol).length;
              const cfg   = ROL_BADGE[rol];
              return (
                <div key={rol} style={{ padding:'8px 16px', borderRadius:'20px', background: cfg.bg, color: cfg.color, fontSize:'13px', fontWeight:600 }}>
                  {cfg.label}: {count}
                </div>
              );
            })}
            <div style={{ padding:'8px 16px', borderRadius:'20px', background:'#f1f5f9', color:'#64748b', fontSize:'13px', fontWeight:600 }}>
              Total: {usuarios.length}
            </div>
          </div>

          {/* Tabla */}
          {loadingU ? (
            <p style={{ color:'#94a3b8', textAlign:'center', padding:'32px' }}>⏳ Cargando...</p>
          ) : usuariosFiltrados.length === 0 ? (
            <p style={{ color:'#94a3b8', textAlign:'center', padding:'32px' }}>No hay usuarios que coincidan con los filtros</p>
          ) : (
            <div style={{ overflowX:'auto' }}>
              <table style={{ width:'100%', borderCollapse:'collapse', fontSize:'14px' }}>
                <thead>
                  <tr style={{ background:'#f8fafc' }}>
                    {['ID','Nombre','Apellido','Email','Rol','Estado','Acciones'].map(h => (
                      <th key={h} style={{ padding:'10px 12px', textAlign:'left', color:'#64748b', fontWeight:600, borderBottom:'1px solid #e2e8f0', whiteSpace:'nowrap' }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {usuariosFiltrados.map(u => {
                    const rolCfg = ROL_BADGE[u.rol ?? 'Estudiante'] ?? ROL_BADGE['Estudiante'];
                    const esYoMismo = u.email === authService.getCurrentUserData()?.email;
                    return (
                      <React.Fragment key={u.idUsuario}>
                        <tr style={{ borderBottom:'1px solid #f1f5f9' }}>
                          <td style={{ padding:'10px 12px', color:'#94a3b8', fontSize:'12px' }}>#{u.idUsuario}</td>
                          <td style={{ padding:'10px 12px', fontWeight:600 }}>{u.nombre}</td>
                          <td style={{ padding:'10px 12px' }}>{u.apellido}</td>
                          <td style={{ padding:'10px 12px', color:'#64748b' }}>{u.email}</td>
                          <td style={{ padding:'10px 12px' }}>
                            <span style={{ padding:'3px 10px', borderRadius:'20px', fontSize:'12px', fontWeight:600, background:rolCfg.bg, color:rolCfg.color }}>
                              {rolCfg.label}
                            </span>
                          </td>
                          <td style={{ padding:'10px 12px' }}>
                            <span style={{ padding:'3px 10px', borderRadius:'20px', fontSize:'12px', fontWeight:600, background: u.activo ? '#dcfce7' : '#fee2e2', color: u.activo ? '#15803d' : '#b91c1c' }}>
                              {u.activo ? '✔ Activo' : '✖ Inactivo'}
                            </span>
                          </td>
                          <td style={{ padding:'8px 12px' }}>
                            <div style={{ display:'flex', gap:'6px', flexWrap:'wrap' }}>
                              {/* Activar / Desactivar */}
                              {!esYoMismo && (
                                <button
                                  onClick={() => toggleActivo(u)}
                                  style={btn(u.activo ? '#f59e0b' : '#16a34a', true)}
                                  title={u.activo ? 'Desactivar cuenta' : 'Activar cuenta'}
                                >
                                  {u.activo ? '⏸ Desactivar' : '▶ Activar'}
                                </button>
                              )}
                              {/* Cambiar contraseña */}
                              <button
                                onClick={() => { setPassTarget(passTarget === u.idUsuario ? null : u.idUsuario); setPassNueva(''); }}
                                style={btn('#0369a1', true)}
                                title="Cambiar contraseña"
                              >
                                🔑 Pass
                              </button>
                              {/* Eliminar */}
                              {!esYoMismo && (
                                <button
                                  onClick={() => setDeleteTarget(u.idUsuario)}
                                  style={btn('#dc2626', true)}
                                  title="Eliminar usuario"
                                >
                                  🗑 Eliminar
                                </button>
                              )}
                              {esYoMismo && (
                                <span style={{ fontSize:'11px', color:'#94a3b8', padding:'5px 0' }}>← vos</span>
                              )}
                            </div>
                          </td>
                        </tr>

                        {/* Fila inline: cambiar contraseña */}
                        {passTarget === u.idUsuario && (
                          <tr style={{ background:'#f0f9ff' }}>
                            <td colSpan={7} style={{ padding:'12px 16px' }}>
                              <div style={{ display:'flex', gap:'10px', alignItems:'center', flexWrap:'wrap' }}>
                                <span style={{ fontSize:'13px', color:'#0369a1', fontWeight:600 }}>🔑 Nueva contraseña para {u.nombre}:</span>
                                <input
                                  type="password"
                                  placeholder="Mínimo 6 caracteres"
                                  value={passNueva}
                                  onChange={e => setPassNueva(e.target.value)}
                                  style={{ ...inp, maxWidth:'200px' }}
                                  minLength={6}
                                />
                                <button onClick={() => cambiarPassword(u.idUsuario)} style={btn('#0369a1', true)} disabled={savingPass}>
                                  {savingPass ? '⏳' : '💾 Guardar'}
                                </button>
                                <button onClick={() => { setPassTarget(null); setPassNueva(''); }} style={btn('#64748b', true)}>
                                  Cancelar
                                </button>
                              </div>
                            </td>
                          </tr>
                        )}

                        {/* Modal de confirmación de eliminación */}
                        {deleteTarget === u.idUsuario && (
                          <tr style={{ background:'#fff1f2' }}>
                            <td colSpan={7} style={{ padding:'14px 16px' }}>
                              <div style={{ display:'flex', gap:'12px', alignItems:'center', flexWrap:'wrap' }}>
                                <span style={{ fontSize:'13px', color:'#b91c1c', fontWeight:600 }}>
                                  ⚠️ ¿Eliminar permanentemente a <strong>{u.nombre} {u.apellido}</strong> ({u.email})?
                                  Esta acción no se puede deshacer.
                                </span>
                                <button onClick={() => eliminarUsuario(u.idUsuario)} style={btn('#dc2626', true)}>
                                  ✔ Confirmar eliminación
                                </button>
                                <button onClick={() => setDeleteTarget(null)} style={btn('#64748b', true)}>
                                  Cancelar
                                </button>
                              </div>
                            </td>
                          </tr>
                        )}
                      </React.Fragment>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* ════════════════════════════════════════════════════════════
          SECCIÓN: CREAR USUARIO
      ════════════════════════════════════════════════════════════ */}
      {seccion === 'crear' && (
        <div style={card}>
          <div style={sectionTitle}>➕ Crear nuevo usuario</div>
          <form onSubmit={crearUsuario}>

            {/* Selector de rol prominente */}
            <div style={{ marginBottom:'24px', padding:'16px 20px', background:'#f8fafc', borderRadius:'10px', border:'1px solid #e2e8f0' }}>
              <label style={{ ...lbl, fontSize:'14px', marginBottom:'10px' }}>Rol a asignar</label>
              <div style={{ display:'flex', gap:'10px', flexWrap:'wrap' }}>
                {[
                  ['ESTUDIANTE',    '🎓', '#dbeafe', '#1d4ed8'],
                  ['DOCENTE',       '👨‍🏫', '#dcfce7', '#16a34a'],
                  ['ADMINISTRADOR', '🛡️',  '#fef3c7', '#d97706'],
                ].map(([valor, icon, bg, color]) => (
                  <button
                    key={valor}
                    type="button"
                    onClick={() => setForm(p => ({ ...p, rol: valor }))}
                    style={{
                      padding:'10px 20px', border:`2px solid ${form.rol === valor ? color : '#e2e8f0'}`,
                      borderRadius:'10px', cursor:'pointer', fontWeight:600, fontSize:'14px',
                      background: form.rol === valor ? bg : 'white',
                      color: form.rol === valor ? color : '#475569',
                      transition: 'all 0.15s',
                    }}
                  >
                    {icon} {valor.charAt(0) + valor.slice(1).toLowerCase()}
                  </button>
                ))}
              </div>
            </div>

            {/* Campos comunes */}
            <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(200px,1fr))', gap:'14px', marginBottom:'16px' }}>
              <div>
                <label style={lbl}>Nombre *</label>
                <input style={inp} type="text" value={form.nombre} onChange={f('nombre')} required minLength={2} />
              </div>
              <div>
                <label style={lbl}>Apellido *</label>
                <input style={inp} type="text" value={form.apellido} onChange={f('apellido')} required minLength={2} />
              </div>
              <div>
                <label style={lbl}>Email *</label>
                <input style={inp} type="email" value={form.email} onChange={f('email')} required />
              </div>
              <div>
                <label style={lbl}>Contraseña inicial *</label>
                <input style={inp} type="password" value={form.password} onChange={f('password')} required minLength={6} placeholder="Mínimo 6 caracteres" />
              </div>
            </div>

            {/* Campos específicos por rol */}
            {form.rol === 'ESTUDIANTE' && (
              <div style={{ padding:'14px 16px', background:'#eff6ff', borderRadius:'10px', marginBottom:'16px' }}>
                <p style={{ margin:'0 0 12px', fontSize:'13px', color:'#1d4ed8', fontWeight:600 }}>🎓 Datos del estudiante (opcionales)</p>
                <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(180px,1fr))', gap:'12px' }}>
                  <div><label style={lbl}>Legajo</label><input style={inp} type="text" value={form.legajo} onChange={f('legajo')} placeholder="Ej: LEG-001" /></div>
                  <div><label style={lbl}>Carrera</label><input style={inp} type="text" value={form.carrera} onChange={f('carrera')} placeholder="Ej: Informática" /></div>
                  <div><label style={lbl}>Año de ingreso</label><input style={inp} type="number" value={form.anioIngreso} onChange={f('anioIngreso')} placeholder="Ej: 2024" min={2000} max={2099} /></div>
                </div>
              </div>
            )}

            {form.rol === 'DOCENTE' && (
              <div style={{ padding:'14px 16px', background:'#f0fdf4', borderRadius:'10px', marginBottom:'16px' }}>
                <p style={{ margin:'0 0 12px', fontSize:'13px', color:'#16a34a', fontWeight:600 }}>👨‍🏫 Datos del docente (opcionales)</p>
                <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(180px,1fr))', gap:'12px' }}>
                  <div><label style={lbl}>Título</label><input style={inp} type="text" value={form.titulo} onChange={f('titulo')} placeholder="Ej: Lic. en Sistemas" /></div>
                  <div><label style={lbl}>Especialidad</label><input style={inp} type="text" value={form.especialidad} onChange={f('especialidad')} placeholder="Ej: Bases de datos" /></div>
                  <div><label style={lbl}>Departamento</label><input style={inp} type="text" value={form.departamento} onChange={f('departamento')} placeholder="Ej: Informática" /></div>
                </div>
              </div>
            )}

            {form.rol === 'ADMINISTRADOR' && (
              <div style={{ padding:'14px 16px', background:'#fffbeb', borderRadius:'10px', marginBottom:'16px' }}>
                <p style={{ margin:'0 0 12px', fontSize:'13px', color:'#d97706', fontWeight:600 }}>🛡️ Datos del administrador (opcionales)</p>
                <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(180px,1fr))', gap:'12px' }}>
                  <div><label style={lbl}>Área</label><input style={inp} type="text" value={form.area} onChange={f('area')} placeholder="Ej: Sistemas" /></div>
                  <div>
                    <label style={lbl}>Nivel de acceso</label>
                    <select style={inp} value={form.nivelAcceso} onChange={f('nivelAcceso')}>
                      <option value="1">1 — Básico</option>
                      <option value="2">2 — Intermedio</option>
                      <option value="3">3 — Total</option>
                    </select>
                  </div>
                </div>
                <p style={{ margin:'10px 0 0', fontSize:'12px', color:'#92400e' }}>
                  ⚠️ Los administradores tienen acceso completo al sistema. Asigná este rol con cuidado.
                </p>
              </div>
            )}

            <div style={{ display:'flex', gap:'10px' }}>
              <button type="submit" disabled={creando} style={btn('#7c3aed')}>
                {creando ? '⏳ Creando...' : `✔ Crear ${form.rol.charAt(0) + form.rol.slice(1).toLowerCase()}`}
              </button>
              <button type="button" onClick={() => setForm(EMPTY_FORM)} style={btn('#64748b')}>
                🗑 Limpiar
              </button>
            </div>
          </form>
        </div>
      )}

      {/* ════════════════════════════════════════════════════════════
          SECCIÓN: SOLICITUDES DE RESET
      ════════════════════════════════════════════════════════════ */}
      {seccion === 'solicitudes' && (
        <div style={card}>
          <div style={sectionTitle}>
            🔑 Solicitudes de recuperación de contraseña
            {pendientes.length > 0 && (
              <span style={{ background:'#dc2626', color:'white', borderRadius:'20px', padding:'2px 8px', fontSize:'12px', marginLeft:'6px' }}>
                {pendientes.length} pendiente{pendientes.length > 1 ? 's' : ''}
              </span>
            )}
          </div>

          <div style={{ display:'flex', gap:'10px', marginBottom:'18px' }}>
            <button onClick={cargarSolicitudes} style={btn('#64748b')}>🔄 Actualizar</button>
          </div>

          {loadingSol ? (
            <p style={{ color:'#94a3b8', textAlign:'center', padding:'32px' }}>⏳ Cargando...</p>
          ) : solicitudes.length === 0 ? (
            <div style={{ textAlign:'center', padding:'40px', color:'#94a3b8' }}>
              <div style={{ fontSize:'40px', marginBottom:'10px' }}>✅</div>
              <p style={{ margin:0 }}>No hay solicitudes de recuperación de contraseña</p>
            </div>
          ) : (
            <div>
              {solicitudes.map(sol => (
                <div
                  key={sol.idSolicitud}
                  style={{
                    padding:'16px 20px', borderRadius:'10px', marginBottom:'12px',
                    border:`1px solid ${sol.estado === 'PENDIENTE' ? '#fca5a5' : '#e2e8f0'}`,
                    background: sol.estado === 'PENDIENTE' ? '#fff7f7' : '#f8fafc',
                  }}
                >
                  <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start', flexWrap:'wrap', gap:'10px' }}>
                    <div>
                      <div style={{ display:'flex', gap:'10px', alignItems:'center', marginBottom:'4px' }}>
                        <span style={{
                          padding:'3px 10px', borderRadius:'20px', fontSize:'12px', fontWeight:700,
                          background: sol.estado === 'PENDIENTE' ? '#fee2e2' : '#dcfce7',
                          color:      sol.estado === 'PENDIENTE' ? '#b91c1c' : '#15803d',
                        }}>
                          {sol.estado === 'PENDIENTE' ? '⏳ PENDIENTE' : '✅ ATENDIDA'}
                        </span>
                        <span style={{ fontSize:'12px', color:'#94a3b8' }}>#{sol.idSolicitud}</span>
                      </div>
                      <p style={{ margin:'0 0 2px', fontWeight:600, color:'#1e293b' }}>
                        {sol.nombreCompleto ?? sol.email}
                      </p>
                      <p style={{ margin:0, fontSize:'13px', color:'#64748b' }}>{sol.email}</p>
                      <p style={{ margin:'4px 0 0', fontSize:'12px', color:'#94a3b8' }}>
                        Solicitado: {formatFecha(sol.fechaSolicitud)}
                        {sol.fechaAtencion && ` · Atendido: ${formatFecha(sol.fechaAtencion)}`}
                      </p>
                    </div>
                    {sol.estado === 'PENDIENTE' && (
                      <button
                        onClick={() => { setAtendiendo(atendiendo === sol.idSolicitud ? null : sol.idSolicitud); setNuevaPassReset(''); }}
                        style={btn('#0369a1')}
                      >
                        🔑 Asignar contraseña
                      </button>
                    )}
                  </div>

                  {/* Panel inline para asignar contraseña */}
                  {atendiendo === sol.idSolicitud && (
                    <div style={{ marginTop:'14px', padding:'14px 16px', background:'#f0f9ff', borderRadius:'8px', border:'1px solid #bae6fd' }}>
                      <p style={{ margin:'0 0 10px', fontSize:'13px', color:'#0369a1', fontWeight:600 }}>
                        Asignar nueva contraseña para <strong>{sol.email}</strong>:
                      </p>
                      <div style={{ display:'flex', gap:'10px', flexWrap:'wrap', alignItems:'center' }}>
                        <input
                          type="password"
                          placeholder="Nueva contraseña (mínimo 6 caracteres)"
                          value={nuevaPassReset}
                          onChange={e => setNuevaPassReset(e.target.value)}
                          style={{ ...inp, maxWidth:'280px' }}
                          minLength={6}
                        />
                        <button onClick={() => atenderSolicitud(sol.idSolicitud)} style={btn('#0369a1')} disabled={savingReset}>
                          {savingReset ? '⏳' : '✔ Confirmar'}
                        </button>
                        <button onClick={() => { setAtendiendo(null); setNuevaPassReset(''); }} style={btn('#64748b')}>
                          Cancelar
                        </button>
                      </div>
                      <p style={{ margin:'8px 0 0', fontSize:'12px', color:'#64748b' }}>
                        El usuario deberá comunicarse con vos para obtener su nueva contraseña.
                      </p>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

    </div>
  );
};

export default GestionUsuarios;
