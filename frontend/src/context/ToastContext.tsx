/**
 * ToastContext.tsx
 * ─────────────────────────────────────────────────────────────────────────────
 * Sistema global de notificaciones tipo Toast.
 *
 * Uso en cualquier componente:
 *   const toast = useToast();
 *   toast.success('Usuario creado correctamente');
 *   toast.error('No se pudo guardar');
 *   toast.warning('La nota ya fue cerrada');
 *   toast.info('Fuiste agregado a la lista de espera');
 * ─────────────────────────────────────────────────────────────────────────────
 */

import React, { createContext, useCallback, useContext, useState } from 'react';

// ─── Tipos ────────────────────────────────────────────────────────────────────

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: string;
  message: string;
  type: ToastType;
  duration: number;   // ms antes de desaparecer
}

interface ToastContextValue {
  toasts: Toast[];
  success: (message: string, duration?: number) => void;
  error:   (message: string, duration?: number) => void;
  warning: (message: string, duration?: number) => void;
  info:    (message: string, duration?: number) => void;
  dismiss: (id: string) => void;
}

// ─── Context ──────────────────────────────────────────────────────────────────

const ToastContext = createContext<ToastContextValue | null>(null);

const MAX_TOASTS = 4;

// ─── Provider ─────────────────────────────────────────────────────────────────

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const add = useCallback((message: string, type: ToastType, duration: number) => {
    const id = `toast-${Date.now()}-${Math.random().toString(36).slice(2)}`;
    setToasts((prev) => {
      // Límite de toasts visibles → elimina el más viejo si se supera
      const queue = prev.length >= MAX_TOASTS ? prev.slice(1) : prev;
      return [...queue, { id, message, type, duration }];
    });
    // Auto-dismiss
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, duration);
  }, []);

  const dismiss = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const value: ToastContextValue = {
    toasts,
    success: (msg, dur = 3500) => add(msg, 'success', dur),
    error:   (msg, dur = 5000) => add(msg, 'error',   dur),
    warning: (msg, dur = 4500) => add(msg, 'warning', dur),
    info:    (msg, dur = 4000) => add(msg, 'info',    dur),
    dismiss,
  };

  return (
    <ToastContext.Provider value={value}>
      {children}
    </ToastContext.Provider>
  );
};

// ─── Hook ─────────────────────────────────────────────────────────────────────

export const useToast = (): ToastContextValue => {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast debe usarse dentro de <ToastProvider>');
  return ctx;
};
