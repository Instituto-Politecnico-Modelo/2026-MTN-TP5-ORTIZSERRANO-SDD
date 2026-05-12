/**
 * errorHandler.ts
 * ─────────────────────────────────────────────────────────────────────────────
 * Sistema centralizado de manejo de errores.
 *
 *  AppError      → clase tipada que reemplaza los errores crudos de Axios
 *  parseError()  → convierte cualquier error en un AppError con mensaje amigable
 *
 * Cómo usarlo en los servicios:
 *   try {
 *     const res = await api.post('/endpoint', data);
 *     return res.data;
 *   } catch (err) {
 *     throw parseError(err, 'crear-usuario');
 *   }
 *
 * Cómo usarlo en los componentes:
 *   try {
 *     await someService.doSomething();
 *     toast.success('¡Listo!');
 *   } catch (err) {
 *     if (err instanceof AppError) toast.error(err.message);
 *   }
 * ─────────────────────────────────────────────────────────────────────────────
 */

import { AxiosError } from 'axios';

// ─── Tipos de error ───────────────────────────────────────────────────────────

export type ErrorCode =
  | 'NETWORK_ERROR'
  | 'TIMEOUT'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'CONFLICT'
  | 'VALIDATION'
  | 'TOO_MANY_REQUESTS'
  | 'SERVER_ERROR'
  | 'SERVICE_UNAVAILABLE'
  | 'UNKNOWN';

/** Contexto de la operación para mensajes personalizados */
export type ErrorContext =
  | 'login'
  | 'update-profile'
  | 'change-password'
  | 'inscribirse'
  | 'cancelar-inscripcion'
  | 'crear-usuario'
  | 'editar-usuario'
  | 'eliminar-usuario'
  | 'reactivar-usuario'
  | 'crear-materia'
  | 'editar-materia'
  | 'eliminar-materia'
  | 'cargar-nota'
  | 'cerrar-nota'
  | 'reservar-auditorio'
  | 'cancelar-reserva'
  | 'crear-periodo'
  | 'editar-periodo'
  | 'eliminar-periodo'
  | 'solicitud-password'
  | 'atender-reset'
  | 'generic';

// ─── Clase AppError ───────────────────────────────────────────────────────────

export class AppError extends Error {
  readonly code: ErrorCode;
  readonly status?: number;
  /** Errores de validación por campo (de Spring @Valid) */
  readonly fieldErrors?: Record<string, string>;

  constructor(
    message: string,
    code: ErrorCode,
    status?: number,
    fieldErrors?: Record<string, string>,
  ) {
    super(message);
    this.name = 'AppError';
    this.code = code;
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

// ─── Helpers internos ─────────────────────────────────────────────────────────

/** Extrae el mensaje de texto del cuerpo de respuesta del backend */
function backendMessage(err: AxiosError): string | null {
  const data = err.response?.data as any;
  // Spring devuelve { message: '...' } o { error: '...' }
  return data?.message ?? data?.error ?? data?.detail ?? null;
}

/** Extrae errores de campos de validación (Spring MethodArgumentNotValidException) */
function extractFieldErrors(err: AxiosError): Record<string, string> | undefined {
  const data = err.response?.data as any;
  if (Array.isArray(data?.errors)) {
    const map: Record<string, string> = {};
    (data.errors as any[]).forEach((e) => {
      if (e.field && e.defaultMessage) map[e.field] = e.defaultMessage;
    });
    return Object.keys(map).length > 0 ? map : undefined;
  }
  return undefined;
}

// ─── Mensajes contextuales por HTTP 400 ──────────────────────────────────────

const MSG_400: Record<ErrorContext, string> = {
  login:
    'Email o contraseña incorrectos. Verificá tus datos e intentá de nuevo.',
  'update-profile':
    'No se pudo actualizar el perfil. Verificá los datos ingresados.',
  'change-password':
    'La contraseña actual es incorrecta o la nueva no cumple los requisitos mínimos.',
  inscribirse:
    'No fue posible completar la inscripción. Verificá que la materia exista y esté disponible.',
  'cancelar-inscripcion':
    'No se pudo cancelar la inscripción. Solo podés cancelar tus propias inscripciones activas.',
  'crear-usuario':
    'No se pudo crear el usuario. El email podría estar en uso o los datos son inválidos.',
  'editar-usuario':
    'No se pudo modificar el usuario. Verificá los datos ingresados.',
  'eliminar-usuario':
    'No se pudo eliminar el usuario. Puede tener registros activos asociados.',
  'reactivar-usuario':
    'No se pudo reactivar el usuario.',
  'crear-materia':
    'No se pudo crear la materia. El código podría estar en uso o faltan campos requeridos.',
  'editar-materia':
    'No se pudo actualizar la materia. Verificá los datos ingresados.',
  'eliminar-materia':
    'No se pudo eliminar la materia. Puede tener inscripciones activas.',
  'cargar-nota':
    'No se pudo guardar la nota. Verificá que el valor esté entre 1 y 10.',
  'cerrar-nota':
    'No se pudo cerrar la nota. Solo podés cerrar notas definitivas.',
  'reservar-auditorio':
    'No se pudo reservar el auditorio. El horario podría estar ocupado o los datos son incorrectos.',
  'cancelar-reserva':
    'No se pudo cancelar la reserva. Solo podés cancelar tus propias reservas pendientes.',
  'crear-periodo':
    'No se pudo crear el período. Verificá que las fechas sean válidas y no se superpongan.',
  'editar-periodo':
    'No se pudo actualizar el período.',
  'eliminar-periodo':
    'No se pudo eliminar el período. Puede tener inscripciones asociadas.',
  'solicitud-password':
    'No se pudo enviar la solicitud. Verificá que el email sea correcto.',
  'atender-reset':
    'No se pudo procesar la solicitud de contraseña.',
  generic:
    'Los datos ingresados son inválidos. Verificá el formulario e intentá de nuevo.',
};

// ─── Mensajes contextuales por HTTP 404 ──────────────────────────────────────

const MSG_404: Record<ErrorContext, string> = {
  login:                 'El usuario no existe en el sistema.',
  'update-profile':      'Tu cuenta no fue encontrada. Intentá cerrar sesión y volver a ingresar.',
  'change-password':     'Tu cuenta no fue encontrada.',
  inscribirse:           'La materia que intentás cursar no existe o fue eliminada.',
  'cancelar-inscripcion':'La inscripción no fue encontrada.',
  'crear-usuario':       'Error al procesar la solicitud.',
  'editar-usuario':      'El usuario no fue encontrado en el sistema.',
  'eliminar-usuario':    'El usuario no fue encontrado en el sistema.',
  'reactivar-usuario':   'El usuario no fue encontrado.',
  'crear-materia':       'Error al procesar la solicitud.',
  'editar-materia':      'La materia no fue encontrada.',
  'eliminar-materia':    'La materia no fue encontrada.',
  'cargar-nota':         'La inscripción no fue encontrada.',
  'cerrar-nota':         'La inscripción no fue encontrada.',
  'reservar-auditorio':  'El auditorio no fue encontrado.',
  'cancelar-reserva':    'La reserva no fue encontrada.',
  'crear-periodo':       'Error al procesar la solicitud.',
  'editar-periodo':      'El período no fue encontrado.',
  'eliminar-periodo':    'El período no fue encontrado.',
  'solicitud-password':  'El email no está registrado en el sistema.',
  'atender-reset':       'La solicitud de contraseña no fue encontrada.',
  generic:               'El recurso solicitado no existe.',
};

// ─── Función principal ────────────────────────────────────────────────────────

/**
 * Convierte cualquier error en un AppError con mensaje amigable para el usuario.
 *
 * @param err     El error capturado (Axios, AppError, o cualquier unknown)
 * @param context Contexto de la operación para mensajes más específicos
 */
export function parseError(err: unknown, context: ErrorContext = 'generic'): AppError {
  // Si ya es un AppError lo devolvemos directo (evita doble wrapping)
  if (err instanceof AppError) return err;

  const axiosErr = err as AxiosError;

  // ── Sin respuesta: error de red o timeout ─────────────────────────────────
  if (!axiosErr.response) {
    if (
      axiosErr.code === 'ECONNABORTED' ||
      axiosErr.message?.toLowerCase().includes('timeout')
    ) {
      return new AppError(
        'La solicitud tardó demasiado. Verificá tu conexión a internet e intentá de nuevo.',
        'TIMEOUT',
      );
    }
    return new AppError(
      'No se pudo conectar con el servidor. Verificá que la aplicación esté en funcionamiento.',
      'NETWORK_ERROR',
    );
  }

  const status = axiosErr.response.status;
  const backMsg = backendMessage(axiosErr);
  const fields  = extractFieldErrors(axiosErr);

  switch (status) {

    // ── Validación / datos incorrectos ────────────────────────────────────
    case 400:
      return new AppError(
        backMsg ?? MSG_400[context],
        'VALIDATION',
        400,
        fields,
      );

    // ── Sesión expirada o token inválido ──────────────────────────────────
    case 401:
      return new AppError(
        context === 'login'
          ? 'Email o contraseña incorrectos.'
          : 'Tu sesión expiró. Por favor, volvé a iniciar sesión.',
        'UNAUTHORIZED',
        401,
      );

    // ── Sin permisos ──────────────────────────────────────────────────────
    case 403:
      return new AppError(
        'No tenés permisos para realizar esta acción.',
        'FORBIDDEN',
        403,
      );

    // ── Recurso no encontrado ─────────────────────────────────────────────
    case 404:
      return new AppError(
        backMsg ?? MSG_404[context],
        'NOT_FOUND',
        404,
      );

    // ── Conflicto / duplicado ─────────────────────────────────────────────
    case 409:
      return new AppError(
        backMsg ?? 'Ya existe un registro con esos datos.',
        'CONFLICT',
        409,
      );

    // ── Datos semánticamente inválidos ────────────────────────────────────
    case 422:
      return new AppError(
        backMsg ?? 'Los datos ingresados no son válidos. Revisá el formulario.',
        'VALIDATION',
        422,
        fields,
      );

    // ── Rate limiter activado ─────────────────────────────────────────────
    case 429:
      return new AppError(
        'Realizaste demasiadas solicitudes. Esperá un momento antes de continuar.',
        'TOO_MANY_REQUESTS',
        429,
      );

    // ── Error interno del servidor ────────────────────────────────────────
    case 500:
      return new AppError(
        'Ocurrió un error en el servidor. Si el problema persiste, contactá al administrador.',
        'SERVER_ERROR',
        500,
      );

    // ── Servidor no disponible ────────────────────────────────────────────
    case 503:
      return new AppError(
        'El servidor está temporalmente no disponible. Tu solicitud fue registrada.',
        'SERVICE_UNAVAILABLE',
        503,
      );

    // ── Cualquier otro código HTTP ────────────────────────────────────────
    default:
      return new AppError(
        backMsg ?? `Error inesperado (código ${status}). Por favor, intentá de nuevo.`,
        'UNKNOWN',
        status,
      );
  }
}
