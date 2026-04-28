/**
 * axiosInstance.ts
 * ─────────────────────────────────────────────────────────────────────────────
 * Instancia global de Axios con interceptores:
 *
 *   REQUEST
 *     • Inyecta automáticamente el Bearer token de localStorage en cada request.
 *
 *   RESPONSE — errores manejados:
 *     • HTTP 401 Unauthorized  → Token expirado o inválido.
 *                                Limpia sesión y redirige a /login?motivo=expired
 *     • HTTP 403 Forbidden     → Sin permisos para el recurso.
 *                                Redirige a /unauthorized
 *     • HTTP 429 Too Many Req  → Rate Limiter activado → /sala-de-espera
 *     • HTTP 503 Unavailable   → Servidor bajo presión  → /sala-de-espera
 *
 * USO: importar `api` de este archivo en lugar de `axios` en los servicios.
 * ─────────────────────────────────────────────────────────────────────────────
 */

import axios, { AxiosInstance, AxiosError } from 'axios';

const BASE_URL = 'http://localhost:8080';
const USER_KEY = 'authUser';

// Instancia configurada globalmente
const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' },
});

// ── Interceptor de REQUEST: inyectar token ────────────────────────────────────
api.interceptors.request.use(
  (config) => {
    const userStr = localStorage.getItem(USER_KEY);
    if (userStr) {
      try {
        const { token } = JSON.parse(userStr);
        if (token) {
          config.headers = config.headers ?? {};
          config.headers['Authorization'] = `Bearer ${token}`;
        }
      } catch {
        // JSON corrupto → ignorar
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ── Interceptor de RESPONSE ───────────────────────────────────────────────────
api.interceptors.response.use(
  // Respuesta exitosa: pasa directamente
  (response) => response,

  // Error: analizar código HTTP
  (error: AxiosError) => {
    const status = error.response?.status;

    // ── 401: sesión expirada o token inválido ─────────────────────────────────
    if (status === 401) {
      const estaEnLogin = window.location.pathname === '/login';
      if (!estaEnLogin) {
        localStorage.removeItem(USER_KEY);
        window.location.href = '/login?motivo=expired';
      }
      return Promise.reject(error);
    }

    // ── 403: acceso denegado ──────────────────────────────────────────────────
    if (status === 403) {
      if (!window.location.pathname.startsWith('/unauthorized')) {
        window.location.href = '/unauthorized';
      }
      return Promise.reject(error);
    }

    // ── 429 / 503: rate limit o servidor sobrecargado → sala de espera ────────
    if (status === 429 || status === 503) {
      const retryAfterHeader = error.response?.headers?.['retry-after'];
      const retryAfterData   = (error.response?.data as any)?.retryAfterSeconds;
      const espera = retryAfterHeader ?? retryAfterData ?? (status === 429 ? 60 : 30);

      const razon  = status === 429 ? 'ratelimit' : 'capacidad';
      const origen = encodeURIComponent(window.location.pathname + window.location.search);

      if (!window.location.pathname.startsWith('/sala-de-espera')) {
        window.location.href =
          `/sala-de-espera?razon=${razon}&espera=${espera}&origen=${origen}`;
      }
    }

    return Promise.reject(error);
  }
);

export default api;
