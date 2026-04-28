/**
 * AuthContext.tsx
 * ─────────────────────────────────────────────────────────────────────────────
 * Contexto global de autenticación y sesión.
 *
 * Provee a toda la app:
 *   - Estado reactivo del usuario autenticado (user, role, isAuthenticated)
 *   - login() / logout() que actualizan el estado global sin recargar la página
 *   - Auto-logout por expiración del JWT (cada 60 s revisa el token)
 *   - Auto-logout por inactividad (IDLE_TIMEOUT_MS sin interacción del usuario)
 *   - Hook useAuth() para consumir el contexto desde cualquier componente
 *
 * Uso:
 *   // En index.tsx (o App.tsx):
 *   <AuthProvider><App /></AuthProvider>
 *
 *   // En cualquier componente:
 *   const { user, role, logout } = useAuth();
 * ─────────────────────────────────────────────────────────────────────────────
 */

import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/auth.service';
import { LoginResponse, Role } from '../types/auth.types';

// ── Configuración de timeouts ─────────────────────────────────────────────────

/** Minutos de inactividad antes de cerrar sesión automáticamente */
const IDLE_TIMEOUT_MINUTES = 30;
const IDLE_TIMEOUT_MS      = IDLE_TIMEOUT_MINUTES * 60 * 1000;

/** Mostrar advertencia N segundos antes del cierre por inactividad */
const IDLE_WARNING_SECONDS = 60;

/** Intervalo de verificación de expiración del JWT (ms) */
const JWT_CHECK_INTERVAL_MS = 60_000;

// ── Tipos del contexto ────────────────────────────────────────────────────────

export interface AuthContextValue {
  /** Datos del usuario autenticado (null si no hay sesión) */
  user:            LoginResponse | null;
  /** Rol actual del usuario */
  role:            Role | null;
  /** true si hay un token válido en sesión */
  isAuthenticated: boolean;
  /** Segundos restantes hasta auto-logout por inactividad (null = sin aviso) */
  idleWarningSecsLeft: number | null;
  /** Inicia sesión y actualiza el contexto */
  login:  (email: string, password: string) => Promise<void>;
  /** Cierra sesión y redirige a /login */
  logout: (reason?: 'manual' | 'expired' | 'idle') => void;
  /** Reinicia el temporizador de inactividad (llamado automáticamente) */
  resetIdleTimer: () => void;
}

// ── Creación del contexto ─────────────────────────────────────────────────────

const AuthContext = createContext<AuthContextValue | null>(null);

// ── Provider ──────────────────────────────────────────────────────────────────

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const navigate = useNavigate();

  const [user, setUser]           = useState<LoginResponse | null>(
    () => authService.getCurrentUserData()
  );
  const [idleWarningSecsLeft, setIdleWarningSecsLeft] = useState<number | null>(null);

  const idleTimerRef    = useRef<ReturnType<typeof setTimeout> | null>(null);
  const warningTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const jwtCheckRef     = useRef<ReturnType<typeof setInterval> | null>(null);

  // ── Helpers ───────────────────────────────────────────────────────────────

  const role:            Role | null = (user?.role as Role) ?? null;
  const isAuthenticated: boolean     = !!user?.token;

  /** Decodifica el payload del JWT (sin verificar firma) para leer `exp` */
  const getJwtExpSeconds = useCallback((token: string): number | null => {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return typeof payload.exp === 'number' ? payload.exp : null;
    } catch {
      return null;
    }
  }, []);

  /** Retorna true si el token JWT ya expiró */
  const isTokenExpired = useCallback((token: string): boolean => {
    const exp = getJwtExpSeconds(token);
    if (exp === null) return false; // no hay exp → no cerramos sesión
    return Date.now() / 1000 >= exp;
  }, [getJwtExpSeconds]);

  // ── logout ────────────────────────────────────────────────────────────────

  const logout = useCallback((reason: 'manual' | 'expired' | 'idle' = 'manual') => {
    authService.logout();
    setUser(null);
    setIdleWarningSecsLeft(null);

    // Limpiar timers
    if (idleTimerRef.current)    clearTimeout(idleTimerRef.current);
    if (warningTimerRef.current) clearInterval(warningTimerRef.current);
    if (jwtCheckRef.current)     clearInterval(jwtCheckRef.current);

    const params = reason !== 'manual' ? `?motivo=${reason}` : '';
    navigate(`/login${params}`, { replace: true });
  }, [navigate]);

  // ── Inactividad ───────────────────────────────────────────────────────────

  const resetIdleTimer = useCallback(() => {
    if (!isAuthenticated) return;

    // Limpiar timers previos
    if (idleTimerRef.current)    clearTimeout(idleTimerRef.current);
    if (warningTimerRef.current) clearInterval(warningTimerRef.current);
    setIdleWarningSecsLeft(null);

    // Armar aviso IDLE_WARNING_SECONDS antes del cierre
    idleTimerRef.current = setTimeout(() => {
      let secsLeft = IDLE_WARNING_SECONDS;
      setIdleWarningSecsLeft(secsLeft);

      warningTimerRef.current = setInterval(() => {
        secsLeft -= 1;
        setIdleWarningSecsLeft(secsLeft);
        if (secsLeft <= 0) {
          clearInterval(warningTimerRef.current!);
          logout('idle');
        }
      }, 1_000);

    }, IDLE_TIMEOUT_MS - IDLE_WARNING_SECONDS * 1_000);
  }, [isAuthenticated, logout]);

  // ── Eventos de actividad del usuario ─────────────────────────────────────

  useEffect(() => {
    if (!isAuthenticated) return;

    const events = ['mousemove', 'keydown', 'pointerdown', 'scroll', 'touchstart'];
    const handler = () => resetIdleTimer();

    events.forEach(e => window.addEventListener(e, handler, { passive: true }));
    resetIdleTimer(); // iniciar al montar

    return () => {
      events.forEach(e => window.removeEventListener(e, handler));
      if (idleTimerRef.current)    clearTimeout(idleTimerRef.current);
      if (warningTimerRef.current) clearInterval(warningTimerRef.current);
    };
  }, [isAuthenticated, resetIdleTimer]);

  // ── Verificación periódica de expiración del JWT ──────────────────────────

  useEffect(() => {
    if (!isAuthenticated || !user?.token) return;

    const check = () => {
      if (user?.token && isTokenExpired(user.token)) {
        logout('expired');
      }
    };

    // Verificar inmediatamente al montar
    check();

    jwtCheckRef.current = setInterval(check, JWT_CHECK_INTERVAL_MS);
    return () => {
      if (jwtCheckRef.current) clearInterval(jwtCheckRef.current);
    };
  }, [isAuthenticated, user?.token, isTokenExpired, logout]);

  // ── Sincronización entre pestañas (localStorage event) ────────────────────

  useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (e.key === 'authUser') {
        if (e.newValue === null) {
          // Otra pestaña cerró sesión
          setUser(null);
          navigate('/login?motivo=otra-pestana', { replace: true });
        } else {
          // Otra pestaña inició sesión
          setUser(JSON.parse(e.newValue));
        }
      }
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, [navigate]);

  // ── login ─────────────────────────────────────────────────────────────────

  const login = useCallback(async (email: string, password: string) => {
    const data = await authService.login(email, password);
    setUser(data);
    // El timer de inactividad se inicia en el useEffect de eventos
  }, []);

  // ── Valor del contexto ────────────────────────────────────────────────────

  const value: AuthContextValue = {
    user,
    role,
    isAuthenticated,
    idleWarningSecsLeft,
    login,
    logout,
    resetIdleTimer,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
      {/* Banner de advertencia de inactividad */}
      {idleWarningSecsLeft !== null && (
        <IdleWarningBanner
          secsLeft={idleWarningSecsLeft}
          onStayLoggedIn={resetIdleTimer}
          onLogout={() => logout('idle')}
        />
      )}
    </AuthContext.Provider>
  );
};

// ── Banner de aviso de inactividad ────────────────────────────────────────────

interface IdleWarningBannerProps {
  secsLeft:       number;
  onStayLoggedIn: () => void;
  onLogout:       () => void;
}

const IdleWarningBanner: React.FC<IdleWarningBannerProps> = ({
  secsLeft, onStayLoggedIn, onLogout,
}) => (
  <div style={{
    position: 'fixed', bottom: 0, left: 0, right: 0, zIndex: 9999,
    background: '#1e293b', color: 'white',
    padding: '16px 24px',
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    gap: '16px', boxShadow: '0 -4px 20px rgba(0,0,0,0.3)',
    fontFamily: 'system-ui, sans-serif',
  }}>
    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
      <span style={{ fontSize: '22px' }}>⏱️</span>
      <div>
        <p style={{ margin: 0, fontWeight: 600, fontSize: '15px' }}>
          Tu sesión se cerrará por inactividad
        </p>
        <p style={{ margin: '2px 0 0', fontSize: '13px', color: '#94a3b8' }}>
          La sesión se cerrará en{' '}
          <strong style={{ color: secsLeft <= 10 ? '#f87171' : '#fbbf24' }}>
            {secsLeft} segundo{secsLeft !== 1 ? 's' : ''}
          </strong>
        </p>
      </div>
    </div>
    <div style={{ display: 'flex', gap: '10px', flexShrink: 0 }}>
      <button
        onClick={onStayLoggedIn}
        style={{
          padding: '8px 20px', background: '#0ea5e9', color: 'white',
          border: 'none', borderRadius: '8px', cursor: 'pointer',
          fontWeight: 600, fontSize: '14px',
        }}
      >
        Seguir conectado
      </button>
      <button
        onClick={onLogout}
        style={{
          padding: '8px 16px', background: 'transparent', color: '#94a3b8',
          border: '1px solid #475569', borderRadius: '8px', cursor: 'pointer',
          fontSize: '14px',
        }}
      >
        Cerrar sesión
      </button>
    </div>
  </div>
);

// ── Hook público ──────────────────────────────────────────────────────────────

/**
 * Hook para consumir el contexto de autenticación.
 * Debe usarse dentro de un componente envuelto por <AuthProvider>.
 *
 * @example
 * const { user, role, logout } = useAuth();
 */
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth() debe usarse dentro de <AuthProvider>');
  }
  return ctx;
}

export default AuthContext;
