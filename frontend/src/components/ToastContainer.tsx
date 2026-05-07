/**
 * ToastContainer.tsx
 * ─────────────────────────────────────────────────────────────────────────────
 * Renderiza la pila de toasts en la esquina inferior-derecha.
 * Se incluye UNA SOLA VEZ en App.tsx, dentro del <ToastProvider>.
 * ─────────────────────────────────────────────────────────────────────────────
 */

import React, { useEffect, useRef, useState } from 'react';
import { Toast, ToastType, useToast } from '../context/ToastContext';

// ─── Configuración visual por tipo ───────────────────────────────────────────

const CONFIG: Record<
  ToastType,
  { bg: string; border: string; text: string; iconColor: string; icon: React.ReactNode }
> = {
  success: {
    bg: '#f0fdf4',
    border: '#86efac',
    text: '#14532d',
    iconColor: '#16a34a',
    icon: (
      <svg width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    ),
  },
  error: {
    bg: '#fef2f2',
    border: '#fca5a5',
    text: '#7f1d1d',
    iconColor: '#dc2626',
    icon: (
      <svg width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    ),
  },
  warning: {
    bg: '#fffbeb',
    border: '#fcd34d',
    text: '#78350f',
    iconColor: '#d97706',
    icon: (
      <svg width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
      </svg>
    ),
  },
  info: {
    bg: '#eff6ff',
    border: '#93c5fd',
    text: '#1e3a5f',
    iconColor: '#2563eb',
    icon: (
      <svg width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M13 16h-1v-4h-1m1-4h.01M12 2a10 10 0 100 20A10 10 0 0012 2z" />
      </svg>
    ),
  },
};

// ─── Toast individual ─────────────────────────────────────────────────────────

const ToastItem: React.FC<{ toast: Toast; onDismiss: (id: string) => void }> = ({
  toast,
  onDismiss,
}) => {
  const cfg = CONFIG[toast.type];
  const [visible, setVisible] = useState(false);
  const [progress, setProgress] = useState(100);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Animación de entrada
  useEffect(() => {
    requestAnimationFrame(() => setVisible(true));
  }, []);

  // Barra de progreso
  useEffect(() => {
    const step = 100 / (toast.duration / 50); // actualiza cada 50ms
    intervalRef.current = setInterval(() => {
      setProgress((p) => {
        const next = p - step;
        if (next <= 0) {
          if (intervalRef.current) clearInterval(intervalRef.current);
          return 0;
        }
        return next;
      });
    }, 50);
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [toast.duration]);

  return (
    <div
      role="alert"
      aria-live="assertive"
      onClick={() => onDismiss(toast.id)}
      style={{
        display: 'flex',
        flexDirection: 'column',
        background: cfg.bg,
        border: `1px solid ${cfg.border}`,
        borderRadius: '12px',
        boxShadow: '0 4px 20px rgba(0,0,0,0.12), 0 1px 4px rgba(0,0,0,0.08)',
        overflow: 'hidden',
        cursor: 'pointer',
        width: '360px',
        maxWidth: 'calc(100vw - 32px)',
        transform: visible ? 'translateX(0)' : 'translateX(120%)',
        opacity: visible ? 1 : 0,
        transition: 'transform 0.35s cubic-bezier(0.34,1.3,0.64,1), opacity 0.3s ease',
        userSelect: 'none',
      }}
    >
      {/* Cuerpo */}
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', padding: '14px 16px' }}>
        {/* Ícono */}
        <span style={{ color: cfg.iconColor, flexShrink: 0, marginTop: '1px' }}>
          {cfg.icon}
        </span>

        {/* Mensaje */}
        <span
          style={{
            flex: 1,
            fontSize: '14px',
            lineHeight: '1.5',
            color: cfg.text,
            fontWeight: 500,
          }}
        >
          {toast.message}
        </span>

        {/* Botón cerrar */}
        <button
          aria-label="Cerrar notificación"
          onClick={(e) => { e.stopPropagation(); onDismiss(toast.id); }}
          style={{
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            color: cfg.iconColor,
            opacity: 0.6,
            padding: '0',
            flexShrink: 0,
            lineHeight: 0,
            marginTop: '1px',
          }}
        >
          <svg width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      {/* Barra de progreso */}
      <div style={{ height: '3px', background: 'rgba(0,0,0,0.06)' }}>
        <div
          style={{
            height: '100%',
            width: `${progress}%`,
            background: cfg.iconColor,
            opacity: 0.6,
            transition: 'width 0.05s linear',
          }}
        />
      </div>
    </div>
  );
};

// ─── Contenedor principal ─────────────────────────────────────────────────────

const ToastContainer: React.FC = () => {
  const { toasts, dismiss } = useToast();

  return (
    <div
      aria-label="Notificaciones"
      style={{
        position: 'fixed',
        bottom: '24px',
        right: '24px',
        zIndex: 9999,
        display: 'flex',
        flexDirection: 'column',
        gap: '10px',
        alignItems: 'flex-end',
        pointerEvents: toasts.length === 0 ? 'none' : 'auto',
      }}
    >
      {toasts.map((t) => (
        <ToastItem key={t.id} toast={t} onDismiss={dismiss} />
      ))}
    </div>
  );
};

export default ToastContainer;
