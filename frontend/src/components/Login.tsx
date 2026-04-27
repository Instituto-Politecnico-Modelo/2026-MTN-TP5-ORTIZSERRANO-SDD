import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/auth.service';
import './Login.css';
const Login: React.FC = () => {
  const [email, setEmail]       = useState<string>('');
  const [password, setPassword] = useState<string>('');
  const [error, setError]       = useState<string>('');
  const [loading, setLoading]   = useState<boolean>(false);

  // Estado para "Olvidé mi contraseña"
  const [showOlvide, setShowOlvide]   = useState(false);
  const [olvideEmail, setOlvideEmail] = useState('');
  const [olvideMsg,   setOlvideMsg]   = useState('');
  const [olvideErr,   setOlvideErr]   = useState('');
  const [olvideLoad,  setOlvideLoad]  = useState(false);

  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    if (!email || !password) {
      setError('Por favor, complete todos los campos');
      setLoading(false);
      return;
    }

    try {
      const response = await authService.login(email, password);
      // Redirigir según el rol del usuario
      navigate(authService.getDefaultRoute());
    } catch (err: any) {
      console.error('Error en login:', err);
      if (err.response) {
        // Error de respuesta del servidor
        if (err.response.status === 401) {
          setError('Email o contraseña incorrectos');
        } else if (err.response.data?.message) {
          setError(err.response.data.message);
        } else {
          setError('Error al iniciar sesión. Intente nuevamente.');
        }
      } else if (err.request) {
        // No hay respuesta del servidor
        setError('No se pudo conectar con el servidor. Verifique que esté ejecutándose.');
      } else {
        setError('Error inesperado. Intente nuevamente.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleOlvidePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setOlvideErr(''); setOlvideMsg('');
    if (!olvideEmail) { setOlvideErr('Ingresá tu email.'); return; }
    setOlvideLoad(true);
    try {
      const res = await authService.olvidePassword(olvideEmail);
      setOlvideMsg(res.message);
      setOlvideEmail('');
    } catch {
      setOlvideErr('No se pudo enviar la solicitud. Intentá de nuevo más tarde.');
    } finally {
      setOlvideLoad(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <h1>Sistema de Gestión</h1>
          <h2>Decanato</h2>
          <p>Inicie sesión para continuar</p>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          {error && (
            <div className="error-message">
              <svg className="error-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              {error}
            </div>
          )}

          <div className="form-group">
            <label htmlFor="email">Email</label>
            <div className="input-wrapper">
              <svg className="input-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 12a4 4 0 10-8 0 4 4 0 008 0zm0 0v1.5a2.5 2.5 0 005 0V12a9 9 0 10-9 9m4.5-1.206a8.959 8.959 0 01-4.5 1.207" />
              </svg>
              <input
                type="email"
                id="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="usuario@ejemplo.com"
                required
                disabled={loading}
              />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="password">Contraseña</label>
            <div className="input-wrapper">
              <svg className="input-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
              </svg>
              <input
                type="password"
                id="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                required
                disabled={loading}
              />
            </div>
          </div>

          <button 
            type="submit" 
            className={`login-button ${loading ? 'loading' : ''}`}
            disabled={loading}
          >
            {loading ? (
              <>
                <svg className="spinner" viewBox="0 0 24 24">
                  <circle className="spinner-circle" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                </svg>
                Iniciando sesión...
              </>
            ) : (
              <>
                <svg className="button-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
                </svg>
                Iniciar Sesión
              </>
            )}
          </button>

          {/* ── Olvidé mi contraseña ── */}
          <div style={{ textAlign: 'center', marginTop: '16px' }}>
            <button
              type="button"
              onClick={() => { setShowOlvide(!showOlvide); setOlvideMsg(''); setOlvideErr(''); }}
              style={{
                background: 'none', border: 'none', cursor: 'pointer',
                color: '#0369a1', fontSize: '13px', textDecoration: 'underline',
              }}
            >
              ¿Olvidaste tu contraseña?
            </button>
          </div>

          {showOlvide && (
            <div style={{
              marginTop: '14px', padding: '16px', background: '#f0f9ff',
              borderRadius: '10px', border: '1px solid #bae6fd',
            }}>
              <p style={{ margin: '0 0 10px', fontSize: '13px', color: '#0369a1', fontWeight: 600 }}>
                🔑 Recuperar contraseña
              </p>
              <p style={{ margin: '0 0 12px', fontSize: '12px', color: '#64748b' }}>
                Ingresá tu email y el administrador recibirá una notificación para asignarte una nueva contraseña.
              </p>
              <form onSubmit={handleOlvidePassword}>
                <input
                  type="email"
                  value={olvideEmail}
                  onChange={e => setOlvideEmail(e.target.value)}
                  placeholder="tu@email.com"
                  required
                  style={{
                    width: '100%', padding: '9px 12px', marginBottom: '10px',
                    border: '1px solid #bae6fd', borderRadius: '8px',
                    fontSize: '14px', boxSizing: 'border-box', outline: 'none',
                  }}
                />
                {olvideErr && <p style={{ color: '#dc2626', fontSize: '12px', margin: '0 0 8px' }}>{olvideErr}</p>}
                {olvideMsg && <p style={{ color: '#16a34a', fontSize: '12px', margin: '0 0 8px' }}>{olvideMsg}</p>}
                <button
                  type="submit"
                  disabled={olvideLoad}
                  style={{
                    width: '100%', padding: '9px', background: '#0369a1',
                    color: 'white', border: 'none', borderRadius: '8px',
                    cursor: 'pointer', fontSize: '14px', fontWeight: 600,
                  }}
                >
                  {olvideLoad ? 'Enviando...' : 'Solicitar nueva contraseña'}
                </button>
              </form>
            </div>
          )}
        </form>

        <div className="login-footer">
          <p className="version">v1.0.0 - 2026</p>
        </div>
      </div>

      <div className="background-decoration">
        <div className="decoration-circle circle-1"></div>
        <div className="decoration-circle circle-2"></div>
        <div className="decoration-circle circle-3"></div>
      </div>
    </div>
  );
};

export default Login;
