/**
 * SalaDeEspera.tsx
 * ─────────────────────────────────────────────────────────────────────────────
 * Pantalla de "Sala de Espera" que se muestra automáticamente cuando:
 *   • El servidor devuelve HTTP 429 (Rate Limit) — el usuario hizo demasiadas
 *     solicitudes en poco tiempo.
 *   • El servidor devuelve HTTP 503 (Service Unavailable) — el servidor está
 *     saturado y no puede aceptar más tráfico.
 *
 * Funcionalidad:
 *   1. Muestra mensaje contextual según la causa (query param ?razon=).
 *   2. Countdown regresivo si hay tiempo de espera sugerido (?espera=N seg).
 *   3. Polling al endpoint GET /api/health cada 5 segundos.
 *   4. Cuando el servidor responde OK → habilita botón de reintentar.
 *   5. Al reintentar → vuelve a la ruta original (?origen=).
 * ─────────────────────────────────────────────────────────────────────────────
 */

import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import api from '../api/axiosInstance';

// ── Tipos ─────────────────────────────────────────────────────────────────────

type Razon = 'ratelimit' | 'capacidad' | 'desconocida';

interface ServerStatus {
  disponible: boolean;
  verificando: boolean;
  ultimaVerificacion: Date | null;
  intentos: number;
}

// ── Constantes ────────────────────────────────────────────────────────────────

const POLL_INTERVAL_MS  = 5_000;   // verificar disponibilidad cada 5s
const MAX_REINTENTOS    = 60;       // ~5 minutos de polling máximo
const HEALTH_ENDPOINT   = '/api/health';

// ── Componente ────────────────────────────────────────────────────────────────

const SalaDeEspera: React.FC = () => {
  const navigate       = useNavigate();
  const [params]       = useSearchParams();

  const razon: Razon   = (params.get('razon') as Razon) ?? 'desconocida';
  const esperaInicial  = parseInt(params.get('espera') ?? '30', 10);
  const origenRaw      = params.get('origen') ?? '/dashboard';
  const origen         = decodeURIComponent(origenRaw);

  // ── Estado ─────────────────────────────────────────────────────────────────
  const [countdown,  setCountdown]  = useState<number>(esperaInicial);
  const [status,     setStatus]     = useState<ServerStatus>({
    disponible: false, verificando: false, ultimaVerificacion: null, intentos: 0,
  });
  const [autoReintentar, setAutoReintentar] = useState<boolean>(false);

  const pollRef      = useRef<ReturnType<typeof setInterval> | null>(null);
  const countRef     = useRef<ReturnType<typeof setInterval> | null>(null);
  const mountedRef   = useRef<boolean>(true);

  // ── Verificar disponibilidad del servidor ──────────────────────────────────
  const checkHealth = useCallback(async () => {
    if (!mountedRef.current) return;
    setStatus(prev => ({ ...prev, verificando: true }));
    try {
      await api.get(HEALTH_ENDPOINT, { timeout: 4_000 });
      if (!mountedRef.current) return;
      setStatus(prev => ({
        disponible: true,
        verificando: false,
        ultimaVerificacion: new Date(),
        intentos: prev.intentos + 1,
      }));
      setAutoReintentar(true);
    } catch {
      if (!mountedRef.current) return;
      setStatus(prev => ({
        disponible: false,
        verificando: false,
        ultimaVerificacion: new Date(),
        intentos: prev.intentos + 1,
      }));
    }
  }, []);

  // ── Countdown regresivo ────────────────────────────────────────────────────
  useEffect(() => {
    if (countdown <= 0) return;
    countRef.current = setInterval(() => {
      setCountdown(prev => {
        if (prev <= 1) {
          if (countRef.current) clearInterval(countRef.current);
          return 0;
        }
        return prev - 1;
      });
    }, 1_000);
    return () => { if (countRef.current) clearInterval(countRef.current); };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ── Polling ────────────────────────────────────────────────────────────────
  useEffect(() => {
    mountedRef.current = true;

    // Primera verificación al montar (con delay del countdown si aplica)
    const firstDelay = Math.max(esperaInicial * 1000, 2_000);
    const firstTimer = setTimeout(() => {
      checkHealth();
      pollRef.current = setInterval(() => {
        if (status.intentos >= MAX_REINTENTOS) {
          if (pollRef.current) clearInterval(pollRef.current);
          return;
        }
        checkHealth();
      }, POLL_INTERVAL_MS);
    }, firstDelay);

    return () => {
      mountedRef.current = false;
      clearTimeout(firstTimer);
      if (pollRef.current) clearInterval(pollRef.current);
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ── Auto-redirigir cuando el servidor vuelve ───────────────────────────────
  useEffect(() => {
    if (autoReintentar && status.disponible) {
      if (pollRef.current) clearInterval(pollRef.current);
      // Pequeño delay para que el usuario vea el mensaje de "disponible"
      const t = setTimeout(() => navigate(origen, { replace: true }), 1_500);
      return () => clearTimeout(t);
    }
  }, [autoReintentar, status.disponible, navigate, origen]);

  // ── Acción manual ──────────────────────────────────────────────────────────
  const handleReintentar = () => {
    if (status.disponible) {
      navigate(origen, { replace: true });
    } else {
      checkHealth();
    }
  };

  // ── Textos según causa ────────────────────────────────────────────────────
  const config = {
    ratelimit: {
      emoji: '🚦',
      titulo: 'Demasiadas solicitudes',
      descripcion:
        'Realizaste muchas solicitudes en poco tiempo. El sistema te pide una breve pausa ' +
        'para proteger la estabilidad del servicio.',
      color: '#f59e0b',
      colorBg: '#fffbeb',
      colorBorder: '#fde68a',
    },
    capacidad: {
      emoji: '⏳',
      titulo: 'Servidor ocupado',
      descripcion:
        'El servidor está atendiendo muchas solicitudes simultáneas. ' +
        'Tu lugar está reservado — serás redirigido automáticamente en cuanto haya disponibilidad.',
      color: '#3b82f6',
      colorBg: '#eff6ff',
      colorBorder: '#bfdbfe',
    },
    desconocida: {
      emoji: '🔄',
      titulo: 'Servicio no disponible',
      descripcion:
        'El servicio no está disponible en este momento. ' +
        'Estamos verificando el estado del servidor automáticamente.',
      color: '#8b5cf6',
      colorBg: '#f5f3ff',
      colorBorder: '#ddd6fe',
    },
  }[razon];

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <div style={styles.overlay}>
      <div style={{ ...styles.card, borderColor: config.colorBorder, background: config.colorBg }}>

        {/* ── Ícono animado ── */}
        <div style={styles.emojiWrapper}>
          <span style={styles.emoji}>{config.emoji}</span>
          {!status.disponible && (
            <div style={{ ...styles.ring, borderTopColor: config.color }} className="spinner-ring" />
          )}
          {status.disponible && (
            <span style={styles.checkmark}>✅</span>
          )}
        </div>

        {/* ── Título ── */}
        <h1 style={{ ...styles.titulo, color: config.color }}>{config.titulo}</h1>

        {/* ── Descripción ── */}
        <p style={styles.descripcion}>{config.descripcion}</p>

        {/* ── Countdown ── */}
        {countdown > 0 && !status.disponible && (
          <div style={styles.countdownBox}>
            <span style={styles.countdownNum}>{countdown}</span>
            <span style={styles.countdownLabel}>segundos de espera sugerida</span>
          </div>
        )}

        {/* ── Estado del servidor ── */}
        <div style={styles.statusRow}>
          <span
            style={{
              ...styles.statusDot,
              background: status.disponible ? '#22c55e'
                        : status.verificando ? '#f59e0b'
                        : '#ef4444',
              boxShadow: status.disponible ? '0 0 8px #22c55e'
                       : status.verificando ? '0 0 8px #f59e0b'
                       : '0 0 8px #ef4444',
            }}
          />
          <span style={styles.statusText}>
            {status.disponible
              ? '¡Servidor disponible! Redirigiendo...'
              : status.verificando
              ? 'Verificando disponibilidad...'
              : status.ultimaVerificacion
              ? `Sin respuesta — reintento #${status.intentos} en ${Math.round(POLL_INTERVAL_MS / 1000)}s`
              : `Esperando ${countdown > 0 ? countdown + 's' : 'para verificar...'}`}
          </span>
        </div>

        {/* ── Barra de progreso de polling ── */}
        {!status.disponible && status.intentos > 0 && (
          <div style={styles.progressBar}>
            <div
              style={{
                ...styles.progressFill,
                width: `${Math.min((status.intentos / MAX_REINTENTOS) * 100, 100)}%`,
                background: config.color,
              }}
            />
          </div>
        )}

        {/* ── Última verificación ── */}
        {status.ultimaVerificacion && (
          <p style={styles.lastCheck}>
            Última verificación: {status.ultimaVerificacion.toLocaleTimeString()}
          </p>
        )}

        {/* ── Botón de acción ── */}
        <button
          onClick={handleReintentar}
          disabled={status.verificando}
          style={{
            ...styles.btn,
            background: status.disponible ? '#22c55e'
                      : status.verificando ? '#94a3b8'
                      : config.color,
            cursor: status.verificando ? 'not-allowed' : 'pointer',
          }}
        >
          {status.disponible
            ? '→ Volver ahora'
            : status.verificando
            ? 'Verificando...'
            : 'Verificar ahora'}
        </button>

        {/* ── Nota informativa ── */}
        <p style={styles.nota}>
          Esta sala de espera verifica el servidor cada {POLL_INTERVAL_MS / 1000} segundos
          y te redirige automáticamente cuando esté disponible.
        </p>
      </div>

      {/* CSS para la animación del spinner */}
      <style>{`
        @keyframes spin {
          0%   { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
        .spinner-ring {
          animation: spin 1s linear infinite;
        }
      `}</style>
    </div>
  );
};

// ── Estilos ───────────────────────────────────────────────────────────────────

const styles: Record<string, React.CSSProperties> = {
  overlay: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    background: 'linear-gradient(135deg, #e0e7ff 0%, #f0fdf4 100%)',
    padding: '20px',
    fontFamily: "'Segoe UI', system-ui, sans-serif",
  },
  card: {
    maxWidth: '520px',
    width: '100%',
    borderRadius: '20px',
    border: '2px solid',
    padding: '40px 36px',
    textAlign: 'center',
    boxShadow: '0 20px 60px rgba(0,0,0,0.12)',
  },
  emojiWrapper: {
    position: 'relative',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: '16px',
  },
  emoji: {
    fontSize: '56px',
    lineHeight: 1,
    zIndex: 1,
  },
  checkmark: {
    position: 'absolute',
    fontSize: '28px',
    bottom: '-6px',
    right: '-6px',
  },
  ring: {
    position: 'absolute',
    width: '80px',
    height: '80px',
    borderRadius: '50%',
    border: '4px solid transparent',
    top: '-10px',
    left: '-10px',
  },
  titulo: {
    fontSize: '28px',
    fontWeight: 700,
    margin: '0 0 12px',
  },
  descripcion: {
    fontSize: '15px',
    color: '#475569',
    lineHeight: 1.6,
    margin: '0 0 24px',
  },
  countdownBox: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    margin: '0 0 24px',
    gap: '4px',
  },
  countdownNum: {
    fontSize: '48px',
    fontWeight: 800,
    color: '#1e293b',
    lineHeight: 1,
  },
  countdownLabel: {
    fontSize: '13px',
    color: '#94a3b8',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
  },
  statusRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '10px',
    margin: '0 0 16px',
  },
  statusDot: {
    width: '12px',
    height: '12px',
    borderRadius: '50%',
    flexShrink: 0,
    transition: 'background 0.4s, box-shadow 0.4s',
  },
  statusText: {
    fontSize: '14px',
    color: '#334155',
    fontWeight: 500,
  },
  progressBar: {
    width: '100%',
    height: '6px',
    background: '#e2e8f0',
    borderRadius: '999px',
    overflow: 'hidden',
    margin: '0 0 12px',
  },
  progressFill: {
    height: '100%',
    borderRadius: '999px',
    transition: 'width 0.5s ease',
  },
  lastCheck: {
    fontSize: '12px',
    color: '#94a3b8',
    margin: '0 0 20px',
  },
  btn: {
    display: 'inline-block',
    padding: '12px 32px',
    borderRadius: '10px',
    border: 'none',
    color: '#fff',
    fontSize: '16px',
    fontWeight: 600,
    letterSpacing: '0.02em',
    transition: 'background 0.3s, transform 0.1s',
    marginBottom: '20px',
  },
  nota: {
    fontSize: '12px',
    color: '#94a3b8',
    margin: 0,
    lineHeight: 1.5,
  },
};

export default SalaDeEspera;
