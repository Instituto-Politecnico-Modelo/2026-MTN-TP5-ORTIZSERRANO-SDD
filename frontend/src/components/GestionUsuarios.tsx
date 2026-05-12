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
import api from '../api/axiosInstance';
import adminService from '../services/admin.service';
import authService from '../services/auth.service';
import { AppError } from '../utils/errorHandler';
import { useToast } from '../context/ToastContext';

const API = '/api';

// ─── Tipos ────────────────────────────────────────────────────────────────────

interface UsuarioRow {
  idUsuario: number;
  nombre: string;
  apellido: string;
  email: string;
  activo: boolean;
  rol?: string;
  legajo?: string;
  carrera?: string;
  anioIngreso?: number;
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

interface MateriaItem {
  idMateria: number;
  nombre: string;
  codigo: string;
  anio?: number;
  cuatrimestre?: number;
  creditos?: number;
}

// ─── Constantes de estilo ─────────────────────────────────────────────────────

const ROL_BADGE: Record<string, { bg: string; color: string; label: string }> = {
  Administrador: { bg: '#fef3c7', color: '#d97706', label: '🛡️ Admin' },
  Docente:       { bg: '#dcfce7', color: '#16a34a', label: '👨‍🏫 Docente' },
  Estudiante:    { bg: '#dbeafe', color: '#1d4ed8', label: '🎓 Estudiante' },
};

// ─── Enums para desplegables ──────────────────────────────────────────────────

const CARRERAS = [
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

const TITULOS_DOCENTE = [
  'Ing. en Sistemas de Información',
  'Lic. en Sistemas',
  'Mg. en Informática',
  'Dr. en Ciencias de la Computación',
  'Ing. Industrial',
  'Lic. en Administración',
  'Contador Público Nacional',
  'Ing. Civil',
  'Mg. en Administración',
  'Profesor Universitario',
];

const ESPECIALIDADES_DOCENTE = [
  'Bases de Datos',
  'Programación',
  'Redes y Comunicaciones',
  'Sistemas Operativos',
  'Inteligencia Artificial',
  'Ingeniería de Software',
  'Matemática',
  'Física',
  'Química',
  'Administración',
  'Contabilidad',
  'Cálculo Numérico',
];

const DEPARTAMENTOS_DOCENTE = [
  'Informática',
  'Ingeniería',
  'Matemática y Física',
  'Administración',
  'Ciencias Básicas',
  'Electrónica',
  'Construcciones',
];

const AREAS_ADMIN = [
  'Decanato',
  'Secretaría Académica',
  'Bedelía',
  'Sistemas',
  'Recursos Humanos',
  'Finanzas',
  'Biblioteca',
  'Coordinación de Carreras',
];

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
  const toast = useToast();
  const [seccion, setSeccion] = useState<Seccion>('usuarios');

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

  // Estado para edición de datos específicos del rol
  const EMPTY_EDIT = { nombre:'', apellido:'', email:'', legajo:'', carrera:'', anioIngreso:'', titulo:'', especialidad:'', departamento:'', area:'', nivelAcceso:'' };
  const [editTarget, setEditTarget]     = useState<number | null>(null);
  const [editForm,   setEditForm]       = useState(EMPTY_EDIT);
  const [savingEdit, setSavingEdit]     = useState(false);

  // Estado para confirmación de eliminación
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);

  const cargarUsuarios = useCallback(async () => {
    setLoadingU(true);
    try {
      const r = await api.get(`${API}/admin/usuarios`);
      const rows: UsuarioRow[] = (r.data as any[]).map(u => ({
        ...u,
        rol: detectarRol(u),
      }));
      setUsuarios(rows);
    } catch (err) {
      toast.error(err instanceof AppError ? err.message : 'No se pudo cargar la lista de usuarios');
    } finally {
      setLoadingU(false);
    }
  }, [toast]);

  useEffect(() => {
    if (seccion === 'usuarios') cargarUsuarios();
  }, [seccion, cargarUsuarios]);

  const toggleActivo = async (u: UsuarioRow) => {
    const endpoint = u.activo ? 'desactivar' : 'reactivar';
    try {
      await api.put(`${API}/admin/usuarios/${u.idUsuario}/${endpoint}`, {});
      toast.success(`Usuario ${u.activo ? 'desactivado' : 'reactivado'} correctamente`);
      cargarUsuarios();
    } catch (err) {
      toast.error(err instanceof AppError ? err.message : 'Error al cambiar el estado del usuario');
    }
  };

  const eliminarUsuario = async (id: number) => {
    try {
      await adminService.eliminarEstudiante(id);
      toast.success('Usuario eliminado permanentemente');
      setDeleteTarget(null);
      cargarUsuarios();
    } catch (err) {
      toast.error(err instanceof AppError ? err.message : 'Error al eliminar el usuario');
    }
  };

  const cambiarPassword = async (idUsuario: number) => {
    if (passNueva.length < 6) { toast.warning('La contraseña debe tener al menos 6 caracteres'); return; }
    setSavingPass(true);
    try {
      await api.put(`${API}/admin/usuarios/${idUsuario}/cambiar-password`, { nuevaPassword: passNueva });
      toast.success('Contraseña actualizada correctamente');
      setPassTarget(null);
      setPassNueva('');
    } catch (err) {      toast.error(err instanceof AppError ? err.message : 'Error al cambiar la contraseña');
    } finally {
      setSavingPass(false);
    }
  };

  const abrirEdicion = (u: UsuarioRow) => {
    setEditTarget(editTarget === u.idUsuario ? null : u.idUsuario);
    setEditForm({
      nombre:       u.nombre       ?? '',
      apellido:     u.apellido     ?? '',
      email:        u.email        ?? '',
      legajo:       u.legajo       ?? '',
      carrera:      u.carrera      ?? '',
      anioIngreso:  u.anioIngreso != null ? String(u.anioIngreso) : '',
      titulo:       u.titulo       ?? '',
      especialidad: u.especialidad ?? '',
      departamento: u.departamento ?? '',
      area:         u.area         ?? '',
      nivelAcceso:  u.nivelAcceso  != null ? String(u.nivelAcceso) : '',
    });
  };

  const guardarEdicion = async (u: UsuarioRow) => {
    setSavingEdit(true);
    try {
      const payload: Record<string, unknown> = {
        nombre:   editForm.nombre   || undefined,
        apellido: editForm.apellido || undefined,
        email:    editForm.email    || undefined,
      };
      if (u.rol === 'Estudiante') {
        payload.legajo      = editForm.legajo      || undefined;
        payload.carrera     = editForm.carrera     || undefined;
        payload.anioIngreso = editForm.anioIngreso ? +editForm.anioIngreso : undefined;
      }
      if (u.rol === 'Docente') {
        payload.titulo       = editForm.titulo       || undefined;
        payload.especialidad = editForm.especialidad || undefined;
        payload.departamento = editForm.departamento || undefined;
      }
      if (u.rol === 'Administrador') {
        payload.area        = editForm.area        || undefined;
        payload.nivelAcceso = editForm.nivelAcceso ? +editForm.nivelAcceso : undefined;
      }
      await adminService.editarDatosEspecificos(u.idUsuario, payload as any);
      toast.success(`Datos de ${u.nombre} actualizados correctamente`);
      setEditTarget(null);
      cargarUsuarios();
    } catch (err) {
      toast.error(err instanceof AppError ? err.message : 'Error al guardar los cambios');
    } finally {
      setSavingEdit(false);
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
    carrera:'', anioIngreso:'',
    titulo:'', especialidad:'', departamento:'',
    area:'', nivelAcceso:'1',
  };
  const [form, setForm]             = useState(EMPTY_FORM);
  const [creando, setCreando]       = useState(false);

  // ── Materias disponibles (para asignar al crear un Estudiante) ───────────
  const [materias,         setMaterias]         = useState<MateriaItem[]>([]);
  const [materiasSelec,    setMateriasSelec]     = useState<number[]>([]);
  const [busqMateria,      setBusqMateria]       = useState('');
  const [loadingMaterias,  setLoadingMaterias]   = useState(false);

  useEffect(() => {
    if (seccion !== 'crear') return;
    setLoadingMaterias(true);
    adminService.listarMaterias()
      .then(data => setMaterias(data as unknown as MateriaItem[]))
      .catch(() => {/* silencioso */})
      .finally(() => setLoadingMaterias(false));
  }, [seccion]);

  const toggleMateria = (id: number) =>
    setMateriasSelec(prev =>
      prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
    );

  const materiasFiltradas = materias.filter(m => {
    const q = busqMateria.toLowerCase();
    return !q || m.nombre.toLowerCase().includes(q) || m.codigo.toLowerCase().includes(q);
  });

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
        payload.carrera     = form.carrera     || undefined;
        payload.anioIngreso = form.anioIngreso ? +form.anioIngreso : undefined;
        if (materiasSelec.length > 0) payload.materiasIds = materiasSelec;
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
      await adminService.crearUsuario(payload);
      toast.success(`${form.rol.charAt(0) + form.rol.slice(1).toLowerCase()} creado/a exitosamente`);
      setForm(EMPTY_FORM);
      setMateriasSelec([]);
      setBusqMateria('');
      setSeccion('usuarios');
    } catch (err) {
      toast.error(err instanceof AppError ? err.message : 'Error al crear el usuario');
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
      const data = await adminService.listarSolicitudesReset();
      setSolicitudes(data as unknown as SolicitudReset[]);
    } catch (err) {
      toast.error(err instanceof AppError ? err.message : 'No se pudieron cargar las solicitudes');
    } finally {
      setLoadingSol(false);
    }
  }, [toast]);

  useEffect(() => {
    if (seccion === 'solicitudes') cargarSolicitudes();
  }, [seccion, cargarSolicitudes]);

  const atenderSolicitud = async (id: number) => {
    if (nuevaPassReset.length < 6) { toast.warning('La contraseña debe tener al menos 6 caracteres'); return; }
    setSavingReset(true);
    try {
      await adminService.atenderSolicitudReset(id, nuevaPassReset);
      toast.success('Contraseña asignada y solicitud marcada como ATENDIDA');
      setAtendiendo(null);
      setNuevaPassReset('');
      cargarSolicitudes();
    } catch (err) {
      toast.error(err instanceof AppError ? err.message : 'Error al atender la solicitud');
    } finally {
      setSavingReset(false);
    }
  };

  const pendientes = solicitudes.filter(s => s.estado === 'PENDIENTE');

  // ─── Render ───────────────────────────────────────────────────────────────

  return (
    <div>

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
                                onClick={() => { setPassTarget(passTarget === u.idUsuario ? null : u.idUsuario); setPassNueva(''); setEditTarget(null); }}
                                style={btn('#0369a1', true)}
                                title="Cambiar contraseña"
                              >
                                🔑 Pass
                              </button>
                              {/* Editar datos específicos */}
                              <button
                                onClick={() => { abrirEdicion(u); setPassTarget(null); setDeleteTarget(null); }}
                                style={btn('#16a34a', true)}
                                title="Editar datos del usuario"
                              >
                                ✏️ Editar
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

                        {/* Fila inline: editar datos específicos del usuario */}
                        {editTarget === u.idUsuario && (
                          <tr style={{ background:'#f0fdf4' }}>
                            <td colSpan={7} style={{ padding:'14px 18px' }}>
                              <div style={{ ...sectionTitle, fontSize:'13px', marginBottom:'12px', paddingBottom:'8px' }}>
                                ✏️ Editar datos de {u.nombre} {u.apellido}
                              </div>
                              {/* Campos base */}
                              <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(170px,1fr))', gap:'10px', marginBottom:'12px' }}>
                                <div><label style={lbl}>Nombre</label><input style={inp} value={editForm.nombre} onChange={e => setEditForm(p=>({...p,nombre:e.target.value}))} /></div>
                                <div><label style={lbl}>Apellido</label><input style={inp} value={editForm.apellido} onChange={e => setEditForm(p=>({...p,apellido:e.target.value}))} /></div>
                                <div><label style={lbl}>Email</label><input style={inp} type="email" value={editForm.email} onChange={e => setEditForm(p=>({...p,email:e.target.value}))} /></div>
                              </div>
                              {/* Campos específicos por rol */}
                              {u.rol === 'Estudiante' && (
                                <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(160px,1fr))', gap:'10px', marginBottom:'12px' }}>
                                  <div><label style={lbl}>Legajo</label><input style={inp} value={editForm.legajo} onChange={e => setEditForm(p=>({...p,legajo:e.target.value}))} placeholder="Ej: LEG-001" /></div>
                                  <div>
                                    <label style={lbl}>Carrera</label>
                                    <select style={inp} value={editForm.carrera} onChange={e => setEditForm(p=>({...p,carrera:e.target.value}))}>
                                      <option value="">— Seleccionar —</option>
                                      {CARRERAS.map(c => <option key={c} value={c}>{c}</option>)}
                                    </select>
                                  </div>
                                  <div><label style={lbl}>Año ingreso</label><input style={inp} type="number" value={editForm.anioIngreso} onChange={e => setEditForm(p=>({...p,anioIngreso:e.target.value}))} min={2000} max={2099} /></div>
                                </div>
                              )}
                              {u.rol === 'Docente' && (
                                <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(160px,1fr))', gap:'10px', marginBottom:'12px' }}>
                                  <div>
                                    <label style={lbl}>Título</label>
                                    <select style={inp} value={editForm.titulo} onChange={e => setEditForm(p=>({...p,titulo:e.target.value}))}>
                                      <option value="">— Seleccionar —</option>
                                      {TITULOS_DOCENTE.map(t => <option key={t} value={t}>{t}</option>)}
                                    </select>
                                  </div>
                                  <div>
                                    <label style={lbl}>Especialidad</label>
                                    <select style={inp} value={editForm.especialidad} onChange={e => setEditForm(p=>({...p,especialidad:e.target.value}))}>
                                      <option value="">— Seleccionar —</option>
                                      {ESPECIALIDADES_DOCENTE.map(e => <option key={e} value={e}>{e}</option>)}
                                    </select>
                                  </div>
                                  <div>
                                    <label style={lbl}>Departamento</label>
                                    <select style={inp} value={editForm.departamento} onChange={e => setEditForm(p=>({...p,departamento:e.target.value}))}>
                                      <option value="">— Seleccionar —</option>
                                      {DEPARTAMENTOS_DOCENTE.map(d => <option key={d} value={d}>{d}</option>)}
                                    </select>
                                  </div>
                                </div>
                              )}
                              {u.rol === 'Administrador' && (
                                <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(160px,1fr))', gap:'10px', marginBottom:'12px' }}>
                                  <div>
                                    <label style={lbl}>Área</label>
                                    <select style={inp} value={editForm.area} onChange={e => setEditForm(p=>({...p,area:e.target.value}))}>
                                      <option value="">— Seleccionar —</option>
                                      {AREAS_ADMIN.map(a => <option key={a} value={a}>{a}</option>)}
                                    </select>
                                  </div>
                                </div>
                              )}
                              <div style={{ display:'flex', gap:'8px' }}>
                                <button onClick={() => guardarEdicion(u)} style={btn('#16a34a', true)} disabled={savingEdit}>
                                  {savingEdit ? '⏳' : '💾 Guardar cambios'}
                                </button>
                                <button onClick={() => setEditTarget(null)} style={btn('#64748b', true)}>Cancelar</button>
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
                <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(180px,1fr))', gap:'12px', marginBottom:'16px' }}>
                  <div>
                    <label style={lbl}>Carrera</label>
                    <select style={inp} value={form.carrera} onChange={f('carrera')}>
                      <option value="">— Seleccionar carrera —</option>
                      {CARRERAS.map(c => <option key={c} value={c}>{c}</option>)}
                    </select>
                  </div>
                  <div><label style={lbl}>Año de ingreso</label><input style={inp} type="number" value={form.anioIngreso} onChange={f('anioIngreso')} placeholder="Ej: 2024" min={2000} max={2099} /></div>
                </div>

                {/* ── Selector de materias ── */}
                <div>
                  <label style={{ ...lbl, marginBottom:'8px' }}>
                    📚 Materias a inscribir
                    {materiasSelec.length > 0 && (
                      <span style={{ marginLeft:'8px', background:'#1d4ed8', color:'white', borderRadius:'20px', padding:'1px 8px', fontSize:'11px', fontWeight:700 }}>
                        {materiasSelec.length} seleccionada{materiasSelec.length > 1 ? 's' : ''}
                      </span>
                    )}
                  </label>

                  {/* Buscador de materias */}
                  <input
                    style={{ ...inp, marginBottom:'8px', background:'white' }}
                    placeholder="🔍 Filtrar materias por nombre o código..."
                    value={busqMateria}
                    onChange={e => setBusqMateria(e.target.value)}
                  />

                  {/* Lista deslizable */}
                  {loadingMaterias ? (
                    <p style={{ color:'#64748b', fontSize:'13px' }}>⏳ Cargando materias...</p>
                  ) : materias.length === 0 ? (
                    <p style={{ color:'#94a3b8', fontSize:'13px' }}>No hay materias creadas todavía</p>
                  ) : (
                    <div style={{
                      maxHeight: '220px',
                      overflowY: 'auto',
                      border: '1.5px solid #bfdbfe',
                      borderRadius: '8px',
                      background: 'white',
                    }}>
                      {materiasFiltradas.length === 0 ? (
                        <p style={{ padding:'12px', color:'#94a3b8', fontSize:'13px', margin:0 }}>
                          Sin resultados para "{busqMateria}"
                        </p>
                      ) : (
                        materiasFiltradas.map((m, idx) => {
                          const selec = materiasSelec.includes(m.idMateria);
                          return (
                            <label
                              key={m.idMateria}
                              style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '10px',
                                padding: '9px 14px',
                                cursor: 'pointer',
                                background: selec ? '#eff6ff' : 'transparent',
                                borderBottom: idx < materiasFiltradas.length - 1 ? '1px solid #e2e8f0' : 'none',
                                transition: 'background 0.1s',
                              }}
                            >
                              <input
                                type="checkbox"
                                checked={selec}
                                onChange={() => toggleMateria(m.idMateria)}
                                style={{ width:'16px', height:'16px', accentColor:'#1d4ed8', cursor:'pointer', flexShrink:0 }}
                              />
                              <div style={{ flex:1, minWidth:0 }}>
                                <span style={{ fontWeight: selec ? 700 : 500, color: selec ? '#1d4ed8' : '#1e293b', fontSize:'13px' }}>
                                  {m.nombre}
                                </span>
                                <span style={{ marginLeft:'6px', color:'#94a3b8', fontSize:'12px' }}>
                                  [{m.codigo}]
                                </span>
                                {(m.anio || m.cuatrimestre || m.creditos) && (
                                  <span style={{ marginLeft:'8px', color:'#64748b', fontSize:'11px' }}>
                                    {[m.anio && `Año ${m.anio}`, m.cuatrimestre && `C${m.cuatrimestre}`, m.creditos && `${m.creditos} créditos`].filter(Boolean).join(' · ')}
                                  </span>
                                )}
                              </div>
                            </label>
                          );
                        })
                      )}
                    </div>
                  )}

                  {/* Acciones rápidas */}
                  {materias.length > 0 && (
                    <div style={{ display:'flex', gap:'8px', marginTop:'8px' }}>
                      <button
                        type="button"
                        onClick={() => setMateriasSelec(materiasFiltradas.map(m => m.idMateria))}
                        style={{ ...btn('#475569', true), fontSize:'11px' }}
                      >
                        ☑ Seleccionar todas ({materiasFiltradas.length})
                      </button>
                      {materiasSelec.length > 0 && (
                        <button
                          type="button"
                          onClick={() => setMateriasSelec([])}
                          style={{ ...btn('#dc2626', true), fontSize:'11px' }}
                        >
                          ✕ Limpiar selección
                        </button>
                      )}
                    </div>
                  )}
                </div>
              </div>
            )}

            {form.rol === 'DOCENTE' && (
              <div style={{ padding:'14px 16px', background:'#f0fdf4', borderRadius:'10px', marginBottom:'16px' }}>
                <p style={{ margin:'0 0 12px', fontSize:'13px', color:'#16a34a', fontWeight:600 }}>👨‍🏫 Datos del docente (opcionales)</p>
                <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(180px,1fr))', gap:'12px' }}>
                  <div>
                    <label style={lbl}>Título</label>
                    <select style={inp} value={form.titulo} onChange={f('titulo')}>
                      <option value="">— Seleccionar título —</option>
                      {TITULOS_DOCENTE.map(t => <option key={t} value={t}>{t}</option>)}
                    </select>
                  </div>
                  <div>
                    <label style={lbl}>Especialidad</label>
                    <select style={inp} value={form.especialidad} onChange={f('especialidad')}>
                      <option value="">— Seleccionar especialidad —</option>
                      {ESPECIALIDADES_DOCENTE.map(e => <option key={e} value={e}>{e}</option>)}
                    </select>
                  </div>
                  <div>
                    <label style={lbl}>Departamento</label>
                    <select style={inp} value={form.departamento} onChange={f('departamento')}>
                      <option value="">— Seleccionar departamento —</option>
                      {DEPARTAMENTOS_DOCENTE.map(d => <option key={d} value={d}>{d}</option>)}
                    </select>
                  </div>
                </div>
              </div>
            )}

            {form.rol === 'ADMINISTRADOR' && (
              <div style={{ padding:'14px 16px', background:'#fffbeb', borderRadius:'10px', marginBottom:'16px' }}>
                <p style={{ margin:'0 0 12px', fontSize:'13px', color:'#d97706', fontWeight:600 }}>🛡️ Datos del administrador (opcionales)</p>
                <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(180px,1fr))', gap:'12px' }}>
                  <div>
                    <label style={lbl}>Área</label>
                    <select style={inp} value={form.area} onChange={f('area')}>
                      <option value="">— Seleccionar área —</option>
                      {AREAS_ADMIN.map(a => <option key={a} value={a}>{a}</option>)}
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
              <button type="button" onClick={() => { setForm(EMPTY_FORM); setMateriasSelec([]); setBusqMateria(''); }} style={btn('#64748b')}>
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
