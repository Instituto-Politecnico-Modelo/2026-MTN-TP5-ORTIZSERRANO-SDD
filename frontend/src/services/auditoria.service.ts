import axios from 'axios';
import authService from './auth.service';

const BASE_URL = 'http://localhost:8080/api/admin/auditoria';

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
  private headers() {
    return authService.getAuthHeader();
  }

  async listarTodos(): Promise<RegistroAuditoria[]> {
    const res = await axios.get<RegistroAuditoria[]>(BASE_URL, { headers: this.headers() });
    return res.data;
  }

  async porEntidad(entidad: string): Promise<RegistroAuditoria[]> {
    const res = await axios.get<RegistroAuditoria[]>(`${BASE_URL}/entidad/${entidad}`, { headers: this.headers() });
    return res.data;
  }

  async porEntidadYId(entidad: string, id: number): Promise<RegistroAuditoria[]> {
    const res = await axios.get<RegistroAuditoria[]>(`${BASE_URL}/entidad/${entidad}/${id}`, { headers: this.headers() });
    return res.data;
  }

  async porUsuario(idUsuario: number): Promise<RegistroAuditoria[]> {
    const res = await axios.get<RegistroAuditoria[]>(`${BASE_URL}/usuario/${idUsuario}`, { headers: this.headers() });
    return res.data;
  }

  async porAccion(accion: string): Promise<RegistroAuditoria[]> {
    const res = await axios.get<RegistroAuditoria[]>(`${BASE_URL}/accion/${accion}`, { headers: this.headers() });
    return res.data;
  }

  async verificarIntegridad(): Promise<IntegridadResponse> {
    const res = await axios.get<IntegridadResponse>(`${BASE_URL}/verificar`, { headers: this.headers() });
    return res.data;
  }
}

export default new AuditoriaService();
