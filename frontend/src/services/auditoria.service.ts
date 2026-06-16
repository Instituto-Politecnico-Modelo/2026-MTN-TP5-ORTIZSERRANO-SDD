import api from '../api/axiosInstance';
import { parseError } from '../utils/errorHandler';

const BASE_URL = '/api/admin/auditoria';

/** Respuesta paginada de Spring (Page<T>) */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;       // página actual (0-based)
  size: number;
  first: boolean;
  last: boolean;
}

export interface RegistroAuditoria {
  idRegistro: number;
  entidad: string;
  idEntidad: number | null;
  accion: string;
  idUsuario: number | null;
  emailUsuario: string;
  descripcion: string;
  ipOrigen: string | null;
  timestampEvento: string;
  hashAnterior: string;
  hashActual: string;
}

export interface IntegridadResponse {
  integra: boolean;
  totalRegistros: number;
  errores: string[];
  mensaje: string;
}

class AuditoriaService {

  /** Lista todos los registros de auditoría con paginación obligatoria.
   *  @param page Página (0-based, default 0)
   *  @param size Tamaño de página (default 50, máx 100) */
  async listarTodos(page = 0, size = 50): Promise<Page<RegistroAuditoria>> {
    try {
      const res = await api.get<Page<RegistroAuditoria>>(BASE_URL, { params: { page, size } });
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async porEntidad(entidad: string): Promise<RegistroAuditoria[]> {
    try {
      const res = await api.get<RegistroAuditoria[]>(`${BASE_URL}/entidad/${entidad}`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async porEntidadYId(entidad: string, id: number): Promise<RegistroAuditoria[]> {
    try {
      const res = await api.get<RegistroAuditoria[]>(`${BASE_URL}/entidad/${entidad}/${id}`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async porUsuario(idUsuario: number): Promise<RegistroAuditoria[]> {
    try {
      const res = await api.get<RegistroAuditoria[]>(`${BASE_URL}/usuario/${idUsuario}`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async porAccion(accion: string): Promise<RegistroAuditoria[]> {
    try {
      const res = await api.get<RegistroAuditoria[]>(`${BASE_URL}/accion/${accion}`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async verificarIntegridad(): Promise<IntegridadResponse> {
    try {
      const res = await api.get<IntegridadResponse>(`${BASE_URL}/verificar`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }
}

const auditoriaService = new AuditoriaService();
export default auditoriaService;
