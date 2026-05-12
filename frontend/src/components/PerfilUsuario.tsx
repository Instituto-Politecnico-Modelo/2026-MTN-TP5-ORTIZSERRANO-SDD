import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import authService, { JwtInspectResponse } from '../services/auth.service';
import { User } from '../types/auth.types';

// ─── Helpers ─────────────────────────────────────────────────────────────────

const ROL_COLORS: Record<string, { bg: string; color: string }> = {
  ESTUDIANTE:    { bg: '#dbeafe', color: '#1d4ed8' },
  DOCENTE:       { bg: '#dcfce7', color: '#16a34a' },
  ADMINISTRADOR: { bg: '#fef3c7', color: '#d97706' },
  USUARIO:       { bg: '#f1f5f9', color: '#475569' },
};

const ROL_ICON: Record<string, string> = {
  ESTUDIANTE:    '🎓',
  DOCENTE:       '👨‍🏫',
  ADMINISTRADOR: '🛡️',
  USUARIO:       '👤',
};

const card: React.CSSProperties = {
  background: 'white',
  borderRadius: '16px',
  padding: '28px 32px',
  boxShadow: '0 2px 12px rgba(0,0,0,0.07)',
  marginBottom: '24px',
};

const sectionTitle: React.CSSProperties = {
  fontSize: '15px', fontWeight: 700, color: '#1e293b',
  marginBottom: '18px', paddingBottom: '10px',
  borderBottom: '1px solid #e2e8f0',
  display: 'flex', alignItems: 'center', gap: '8px',
};

const inputStyle: React.CSSProperties = {
  width: '100%', padding: '10px 14px', border: '1.5px solid #e2e8f0',
  borderRadius: '8px', fontSize: '14px', outline: 'none',
  boxSizing: 'border-box', transition: 'border-color 0.15s',
  fontFamily: 'inherit',
};

const labelStyle: React.CSSProperties = {
  display: 'block', fontSize: '13px', fontWeight: 600,
  color: '#475569', marginBottom: '6px',
};

const btnPrimary: React.CSSProperties = {
  padding: '10px 22px', background: 'linear-gradient(135deg, #0ea5e9, #0369a1)',
  color: 'white', border: 'none', borderRadius: '8px',
  cursor: 'pointer', fontSize: '14px', fontWeight: 600,
};

const btnSecondary: React.CSSProperties = {
  padding: '10px 22px', background: '#f1f5f9',
  color: '#334155', border: '1px solid #e2e8f0', borderRadius: '8px',
  cursor: 'pointer', fontSize: '14px', fontWeight: 500,
};

// ─── Componente principal ─────────────────────────────────────────────────────

const PerfilUsuario: React.FC = () => {
  const navigate = useNavigate();
  const loginData = authService.getCurrentUserData();
  const rol = loginData?.role ?? 'USUARIO';

  // Datos del perfil cargados desde /api/auth/me
  const [user, setUser]     = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // Formulario datos personales
  const [nombre,   setNombre]   = useState('');
  const [apellido, setApellido] = useState('');
  const [savingDatos, setSavingDatos] = useState(false);
  const [msgDatos,    setMsgDatos]    = useState('');
  const [errDatos,    setErrDatos]    = useState('');

  // Formulario cambio de contraseña
  const [passActual,     setPassActual]     = useState('');
  const [passNueva,      setPassNueva]      = useState('');
  const [passConfirm,    setPassConfirm]    = useState('');
  const [savingPass,     setSavingPass]     = useState(false);
  const [msgPass,        setMsgPass]        = useState('');
  const [errPass,        setErrPass]        = useState('');
  const [showPass,       setShowPass]       = useState(false);

  // JWT info
  const [jwtInfo,     setJwtInfo]     = useState<JwtInspectResponse | null>(null);
  const [jwtLoading,  setJwtLoading]  = useState(false);
  const [jwtExpanded, setJwtExpanded] = useState(false);

  // ── Carga inicial ──────────────────────────────────────────────────────────

  useEffect(() => {
    if (!authService.isAuthenticated()) {
      navigate('/login');
      return;
    }
    authService.getCurrentUser()
      .then(u => {
        setUser(u);
        setNombre(u.nombre ?? '');
        setApellido(u.apellido ?? '');
      })
      .catch(() => { navigate('/login'); })
      .finally(() => setLoading(false));
  }, [navigate]);

  // ── Guardar datos personales ───────────────────────────────────────────────

  const handleSaveDatos = async (e: React.FormEvent) => {
    e.preventDefault();
    setMsgDatos(''); setErrDatos('');
    if (nombre.trim().length < 2 || apellido.trim().length < 2) {
      setErrDatos('Nombre y apellido deben tener al menos 2 caracteres.');
      return;
    }
    setSavingDatos(true);
    try {
      await axios.put(
        'http://localhost:8080/api/auth/update',
        { nombre: nombre.trim(), apellido: apellido.trim() },
        { headers: authService.getAuthHeader() }
      );
      setUser(prev => prev ? { ...prev, nombre: nombre.trim(), apellido: apellido.trim() } : prev);
      setMsgDatos('✅ Datos actualizados correctamente.');
    } catch (err: any) {
      setErrDatos(err.response?.data?.message ?? 'Error al actualizar los datos.');
    } finally {
      setSavingDatos(false);
    }
  };

  // ── Cambiar contraseña ─────────────────────────────────────────────────────

  const handleChangePass = async (e: React.FormEvent) => {
    e.preventDefault();
    setMsgPass(''); setErrPass('');
    if (passNueva.length < 6) {
      setErrPass('La nueva contraseña debe tener al menos 6 caracteres.');
      return;
    }
    if (passNueva !== passConfirm) {
      setErrPass('Las contraseñas nuevas no coinciden.');
      return;
    }
    setSavingPass(true);
    try {
      await axios.put(
        'http://localhost:8080/api/auth/update',
        { password: passNueva },
        { headers: authService.getAuthHeader() }
      );
      setPassActual(''); setPassNueva(''); setPassConfirm('');
      setMsgPass('✅ Contraseña actualizada. Por seguridad, volvé a iniciar sesión.');
      setTimeout(() => { authService.logout(); navigate('/login'); }, 3000);
    } catch (err: any) {
      setErrPass(err.response?.data?.message ?? 'Error al cambiar la contraseña.');
    } finally {
      setSavingPass(false);
    }
  };

  // ── JWT Inspect ────────────────────────────────────────────────────────────

  const handleInspectJwt = async () => {
    const token = loginData?.token;
    if (!token) return;
    setJwtLoading(true);
    try {
      const info = await authService.inspectJwt(token);
      setJwtInfo(info);
      setJwtExpanded(true);
    } catch {
      setJwtInfo(null);
    } finally {
      setJwtLoading(false);
    }
  };

  // ── Volver al dashboard ────────────────────────────────────────────────────

  const handleVolver = () => navigate(authService.getDefaultRoute());

  // ─────────────────────────────────────────────────────────────────────────

  if (loading) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f0f9ff' }}>
        <div style={{ textAlign: 'center', color: '#64748b' }}>
          <div style={{ fontSize: '32px', marginBottom: '12px' }}>⏳</div>
          <p>Cargando perfil...</p>
        </div>
      </div>
    );
  }

  const rolColors  = ROL_COLORS[rol as string] ?? ROL_COLORS['USUARIO'];
  const rolIcon    = ROL_ICON[rol as string] ?? '👤';
  const initials   = `${user?.nombre?.charAt(0) ?? '?'}${user?.apellido?.charAt(0) ?? ''}`;

  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#f0f9ff' }}>

      {/* ── Header ── */}
      <header style={{
        background: 'linear-gradient(135deg, #0ea5e9 0%, #0369a1 100%)',
        color: 'white', padding: '16px 40px',
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      }}>
        <div>
          <h1 style={{ margin: 0, fontSize: '20px' }}>Mi Perfil</h1>
          <p style={{ margin: '2px 0 0', opacity: 0.85, fontSize: '13px' }}>
            {user?.nombre} {user?.apellido} · {user?.email}
          </p>
        </div>
        <button onClick={handleVolver} style={btnSecondary}>
          ← Volver al inicio
        </button>
      </header>

      {/* ── Contenido ── */}
      <main style={{ maxWidth: '860px', margin: '0 auto', padding: '36px 20px' }}>

        {/* ── Tarjeta de resumen ──────────────────────────────────────────── */}
        <div style={card}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '24px', flexWrap: 'wrap' }}>
            {/* Avatar */}
            <div style={{
              width: '80px', height: '80px', borderRadius: '50%', flexShrink: 0,
              background: 'linear-gradient(135deg, #0ea5e9, #0369a1)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: 'white', fontSize: '28px', fontWeight: 700,
            }}>
              {initials}
            </div>
            {/* Datos */}
            <div style={{ flex: 1, minWidth: '200px' }}>
              <h2 style={{ margin: '0 0 4px', color: '#1e293b', fontSize: '22px' }}>
                {user?.nombre} {user?.apellido}
              </h2>
              <p style={{ margin: '0 0 10px', color: '#64748b', fontSize: '14px' }}>{user?.email}</p>
              <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', alignItems: 'center' }}>
                <span style={{
                  padding: '4px 14px', borderRadius: '20px', fontSize: '13px', fontWeight: 600,
                  background: rolColors.bg, color: rolColors.color,
                }}>
                  {rolIcon} {rol}
                </span>
                <span style={{ fontSize: '12px', color: '#94a3b8' }}>
                  ID #{user?.idUsuario}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* ── Datos personales ────────────────────────────────────────────── */}
        <div style={card}>
          <div style={sectionTitle}>✏️ Editar datos personales</div>
          <form onSubmit={handleSaveDatos}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
              <div>
                <label style={labelStyle}>Nombre</label>
                <input
                  style={inputStyle}
                  type="text"
                  value={nombre}
                  onChange={e => { setNombre(e.target.value); setMsgDatos(''); setErrDatos(''); }}
                  minLength={2}
                  maxLength={100}
                  required
                />
              </div>
              <div>
                <label style={labelStyle}>Apellido</label>
                <input
                  style={inputStyle}
                  type="text"
                  value={apellido}
                  onChange={e => { setApellido(e.target.value); setMsgDatos(''); setErrDatos(''); }}
                  minLength={2}
                  maxLength={100}
                  required
                />
              </div>
            </div>
            <div style={{ marginBottom: '16px' }}>
              <label style={labelStyle}>Email</label>
              <input
                style={{ ...inputStyle, background: '#f8fafc', color: '#94a3b8', cursor: 'not-allowed' }}
                type="email"
                value={user?.email ?? ''}
                disabled
                title="El email no puede modificarse desde aquí"
              />
              <p style={{ margin: '4px 0 0', fontSize: '12px', color: '#94a3b8' }}>
                El email no puede modificarse. Contactá al administrador si necesitás cambiarlo.
              </p>
            </div>
            {errDatos && <p style={{ color: '#dc2626', fontSize: '13px', margin: '0 0 12px' }}>{errDatos}</p>}
            {msgDatos && <p style={{ color: '#16a34a', fontSize: '13px', margin: '0 0 12px' }}>{msgDatos}</p>}
            <button type="submit" style={btnPrimary} disabled={savingDatos}>
              {savingDatos ? '⏳ Guardando...' : '💾 Guardar cambios'}
            </button>
          </form>
        </div>

        {/* ── Cambiar contraseña ──────────────────────────────────────────── */}
        <div style={card}>
          <div style={sectionTitle}>🔒 Cambiar contraseña</div>
          <form onSubmit={handleChangePass}>
            <div style={{ marginBottom: '16px' }}>
              <label style={labelStyle}>Contraseña actual</label>
              <div style={{ position: 'relative' }}>
                <input
                  style={inputStyle}
                  type={showPass ? 'text' : 'password'}
                  value={passActual}
                  onChange={e => { setPassActual(e.target.value); setErrPass(''); setMsgPass(''); }}
                  placeholder="Ingresá tu contraseña actual"
                  required
                />
              </div>
              <p style={{ margin: '4px 0 0', fontSize: '12px', color: '#94a3b8' }}>
                Ingresá tu contraseña actual para confirmar que sos vos.
              </p>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
              <div>
                <label style={labelStyle}>Nueva contraseña</label>
                <input
                  style={inputStyle}
                  type={showPass ? 'text' : 'password'}
                  value={passNueva}
                  onChange={e => { setPassNueva(e.target.value); setErrPass(''); setMsgPass(''); }}
                  placeholder="Mínimo 6 caracteres"
                  minLength={6}
                  maxLength={40}
                  required
                />
              </div>
              <div>
                <label style={labelStyle}>Confirmar nueva contraseña</label>
                <input
                  style={{
                    ...inputStyle,
                    borderColor: passConfirm && passNueva !== passConfirm ? '#fca5a5' : '#e2e8f0',
                  }}
                  type={showPass ? 'text' : 'password'}
                  value={passConfirm}
                  onChange={e => { setPassConfirm(e.target.value); setErrPass(''); setMsgPass(''); }}
                  placeholder="Repetí la nueva contraseña"
                  required
                />
                {passConfirm && passNueva !== passConfirm && (
                  <p style={{ margin: '4px 0 0', fontSize: '12px', color: '#dc2626' }}>Las contraseñas no coinciden</p>
                )}
              </div>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
              <input
                id="showPass"
                type="checkbox"
                checked={showPass}
                onChange={e => setShowPass(e.target.checked)}
                style={{ cursor: 'pointer' }}
              />
              <label htmlFor="showPass" style={{ ...labelStyle, margin: 0, cursor: 'pointer', fontWeight: 400 }}>
                Mostrar contraseñas
              </label>
            </div>
            {errPass && <p style={{ color: '#dc2626', fontSize: '13px', margin: '0 0 12px' }}>{errPass}</p>}
            {msgPass && <p style={{ color: '#16a34a', fontSize: '13px', margin: '0 0 12px' }}>{msgPass}</p>}
            <button type="submit" style={btnPrimary} disabled={savingPass}>
              {savingPass ? '⏳ Actualizando...' : '🔑 Cambiar contraseña'}
            </button>
          </form>
        </div>

        {/* ── JWT Info ────────────────────────────────────────────────────── */}
        <div style={card}>
          <div style={sectionTitle}>🔐 Información del token JWT</div>
          <p style={{ color: '#64748b', fontSize: '14px', margin: '0 0 16px' }}>
            Tu sesión activa está representada por un token JWT. Podés inspeccionarlo para ver cuándo expira y qué datos contiene.
          </p>
          {!jwtExpanded ? (
            <button onClick={handleInspectJwt} style={btnSecondary} disabled={jwtLoading}>
              {jwtLoading ? '⏳ Inspeccionando...' : '🔍 Inspeccionar mi token'}
            </button>
          ) : (
            <div>
              <button onClick={() => setJwtExpanded(false)} style={{ ...btnSecondary, marginBottom: '16px' }}>
                ▲ Ocultar
              </button>
              {jwtInfo && (
                <div style={{
                  background: '#f8fafc', borderRadius: '10px', padding: '16px 20px',
                  border: '1px solid #e2e8f0', fontSize: '13px', lineHeight: '1.8',
                }}>
                  <JwtRow label="¿Válido?"       value={jwtInfo.valido ? '✅ Sí' : `❌ No — ${jwtInfo.motivo_invalidez ?? ''}`} />
                  <JwtRow label="Email (subject)" value={jwtInfo.subject_email ?? '—'} />
                  <JwtRow label="Emitido en"      value={jwtInfo.emitido_en ? formatDate(jwtInfo.emitido_en) : '—'} />
                  <JwtRow label="Expira el"       value={jwtInfo.expira_en  ? formatDate(jwtInfo.expira_en)  : '—'} />
                  {jwtInfo.segundos_hasta_expiracion !== undefined && (
                    <JwtRow
                      label="Tiempo restante"
                      value={jwtInfo.ya_expirado
                        ? '⚠️ Token expirado'
                        : formatSeconds(jwtInfo.segundos_hasta_expiracion)}
                      highlight={!jwtInfo.ya_expirado && jwtInfo.segundos_hasta_expiracion < 300}
                    />
                  )}
                  {jwtInfo.perfil_usuario && (
                    <>
                      <div style={{ borderTop: '1px solid #e2e8f0', margin: '10px 0' }} />
                      <JwtRow label="Rol"         value={jwtInfo.perfil_usuario.rol} />
                      <JwtRow label="ID usuario"  value={String(jwtInfo.perfil_usuario.idUsuario)} />
                    </>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

      </main>
    </div>
  );
};

// ─── Sub-componentes helpers ──────────────────────────────────────────────────

const JwtRow: React.FC<{ label: string; value: string; highlight?: boolean }> = ({ label, value, highlight }) => (
  <div style={{ display: 'flex', gap: '12px', padding: '3px 0' }}>
    <span style={{ minWidth: '160px', color: '#64748b', fontWeight: 600 }}>{label}:</span>
    <span style={{ color: highlight ? '#d97706' : '#1e293b', fontWeight: highlight ? 700 : 400 }}>{value}</span>
  </div>
);

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('es-AR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

function formatSeconds(secs: number): string {
  if (secs <= 0) return 'Expirado';
  const h = Math.floor(secs / 3600);
  const m = Math.floor((secs % 3600) / 60);
  const s = secs % 60;
  if (h > 0) return `${h}h ${m}m`;
  if (m > 0) return `${m}m ${s}s`;
  return `${s}s`;
}

export default PerfilUsuario;
