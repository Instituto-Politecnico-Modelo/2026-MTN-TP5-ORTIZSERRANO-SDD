/**
 * axiosInstance.ts
 * ─────────────────────────────────────────────────────────────────────────────
 * Instancia global de Axios con interceptores que detectan situaciones de
 * sobrecarga del servidor:
 *
 *   • HTTP 429 Too Many Requests  → Rate Limiter activado (usuario hace flood)
 *   • HTTP 503 Service Unavailable → Servidor bajo presión / sin recursos
 *
 * En ambos casos se redirige automáticamente a /sala-de-espera pasando:
 *   - razon    : 'ratelimit' | 'capacidad'
 *   - origen   : ruta actual codificada (para volver después)
 *   - espera   : segundos sugeridos de espera (si el backend los envía)
 *
 * USO: importar `api` de este archivo en lugar de `axios` en los servicios.
 * ─────────────────────────────────────────────────────────────────────────────
 */

import axios, { AxiosInstance, AxiosError } from 'axios';

const BASE_URL = 'http://localhost:8080';

// Instancia configurada globalmente
const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' },
});

// ── Interceptor de respuesta ───────────────────────────────────────────────────
api.interceptors.response.use(
  // Respuesta exitosa: pasa directamente
  (response) => response,

  // Error: analizar código HTTP
  (error: AxiosError) => {
    const status = error.response?.status;

    if (status === 429 || status === 503) {
      // Calcular espera sugerida
      const retryAfterHeader = error.response?.headers?.['retry-after'];
      const retryAfterData   = (error.response?.data as any)?.retryAfterSeconds;
      const espera = retryAfterHeader ?? retryAfterData ?? (status === 429 ? 60 : 30);

      const razon  = status === 429 ? 'ratelimit' : 'capacidad';
      const origen = encodeURIComponent(window.location.pathname + window.location.search);

      // Evitar loop si ya estamos en sala-de-espera
      if (!window.location.pathname.startsWith('/sala-de-espera')) {
        window.location.href =
          `/sala-de-espera?razon=${razon}&espera=${espera}&origen=${origen}`;
      }
    }

    return Promise.reject(error);
  }
);

export default api;
