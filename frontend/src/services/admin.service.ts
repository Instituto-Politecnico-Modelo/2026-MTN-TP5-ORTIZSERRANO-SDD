/**
 * admin.service.ts
 * ──────────────────────────────────────────────────────────────────
 * Consume los endpoints exclusivos del rol ADMINISTRADOR:
 *
 *  /api/admin/usuarios       → CRUD de usuarios (estudiantes/docentes)
 *  /api/admin/estudiantes    → Listado y detalle de estudiantes
 *  /api/admin/materias       → CRUD completo de materias
 *  /api/admin/periodos       → CRUD de períodos académicos
 *
 * Todos los métodos adjuntan automáticamente el Bearer token JWT.
 */

import axios from 'axios';
import authService from './auth.service';

const BASE = 'http://localhost:8080/api/admin';

// ─── Tipos ────────────────────────────────────────────────────────────────────

export interface UsuarioRequest {
  nombre: string;
  apellido: string;
  email: string;
  password: string;
  /** 'ESTUDIANTE' | 'DOCENTE' */
  rol: string;
  /** Solo para ESTUDIANTE */
  legajo?: string;
  carrera?: string;
  anioIngreso?: number;
  /** Solo para DOCENTE */
  titulo?: string;
  especialidad?: string;
}

export interface Estudiante {
  idUsuario: number;
  nombre: string;
  apellido: string;
  email: string;
  rol: string;
  legajo?: string;
  carrera?: string;
  anioIngreso?: number;
  activo: boolean;
}

export interface Docente {
  idUsuario: number;
  nombre: string;
  apellido: string;
  email: string;
  rol: string;
  titulo?: string;
  especialidad?: string;
  activo: boolean;
}

export interface MateriaRequest {
  codigo: string;
  nombre: string;
  descripcion?: string;
  creditos?: number;
  anio: number;
  cuatrimestre: number;
  cuposMaximos: number;
}

export interface Materia {
  idMateria: number;
  codigo: string;
  nombre: string;
  descripcion?: string;
  creditos?: number;
  anio: number;
  cuatrimestre: number;
  cuposMaximos: number;
  cuposOcupados?: number;
}

export interface PeriodoRequest {
  nombre: string;
  fechaInicio: string;   // 'YYYY-MM-DD'
  fechaFin: string;      // 'YYYY-MM-DD'
  activo?: boolean;
}

export interface Periodo {
  idPeriodo: number;
  nombre: string;
  fechaInicio: string;
  fechaFin: string;
  activo: boolean;
}

export interface MessageResponse {
  message: string;
}

export interface SolicitudReset {
  idSolicitud: number;
  email: string;
  nombreCompleto?: string;
  fechaSolicitud: string;
  estado: 'PENDIENTE' | 'ATENDIDA';
  fechaAtencion?: string;
}

// ─── Servicio ─────────────────────────────────────────────────────────────────

class AdminService {

  private h() {
    return { headers: authService.getAuthHeader() };
  }

  // ══════════════════════════════════════════════════════════════════
  //  USUARIOS  –  POST /api/admin/usuarios
  // ══════════════════════════════════════════════════════════════════

  /**
   * Crea un nuevo usuario (ESTUDIANTE o DOCENTE).
   * El administrador es el único que puede registrar usuarios.
   */
  async crearUsuario(data: UsuarioRequest): Promise<MessageResponse> {
    const res = await axios.post<MessageResponse>(`${BASE}/usuarios`, data, this.h());
    return res.data;
  }

  // ══════════════════════════════════════════════════════════════════
  //  ESTUDIANTES  –  /api/admin/estudiantes
  // ══════════════════════════════════════════════════════════════════

  /** GET /api/admin/estudiantes → lista todos los estudiantes */
  async listarEstudiantes(): Promise<Estudiante[]> {
    const res = await axios.get<Estudiante[]>(`${BASE}/estudiantes`, this.h());
    return res.data;
  }

  /** GET /api/admin/estudiantes/{id} → detalle de un estudiante */
  async obtenerEstudiante(id: number): Promise<Estudiante> {
    const res = await axios.get<Estudiante>(`${BASE}/estudiantes/${id}`, this.h());
    return res.data;
  }

  /** DELETE /api/admin/estudiantes/{id} → eliminar estudiante */
  async eliminarEstudiante(id: number): Promise<MessageResponse> {
    const res = await axios.delete<MessageResponse>(`${BASE}/estudiantes/${id}`, this.h());
    return res.data;
  }

  // ══════════════════════════════════════════════════════════════════
  //  MATERIAS  –  /api/admin/materias
  // ══════════════════════════════════════════════════════════════════

  /** GET /api/admin/materias → lista todas las materias */
  async listarMaterias(): Promise<Materia[]> {
    const res = await axios.get<Materia[]>(`${BASE}/materias`, this.h());
    return res.data;
  }

  /** GET /api/admin/materias/{id} → detalle de una materia */
  async obtenerMateria(id: number): Promise<Materia> {
    const res = await axios.get<Materia>(`${BASE}/materias/${id}`, this.h());
    return res.data;
  }

  /** POST /api/admin/materias → crear materia */
  async crearMateria(data: MateriaRequest): Promise<Materia> {
    const res = await axios.post<Materia>(`${BASE}/materias`, data, this.h());
    return res.data;
  }

  /** PUT /api/admin/materias/{id} → actualizar materia */
  async actualizarMateria(id: number, data: MateriaRequest): Promise<Materia> {
    const res = await axios.put<Materia>(`${BASE}/materias/${id}`, data, this.h());
    return res.data;
  }

  /** DELETE /api/admin/materias/{id} → eliminar materia */
  async eliminarMateria(id: number): Promise<MessageResponse> {
    const res = await axios.delete<MessageResponse>(`${BASE}/materias/${id}`, this.h());
    return res.data;
  }

  // ══════════════════════════════════════════════════════════════════
  //  PERÍODOS  –  /api/admin/periodos
  // ══════════════════════════════════════════════════════════════════

  /** GET /api/admin/periodos → lista todos los períodos */
  async listarPeriodos(): Promise<Periodo[]> {
    const res = await axios.get<Periodo[]>(`${BASE}/periodos`, this.h());
    return res.data;
  }

  /** GET /api/admin/periodos/activos → solo períodos activos */
  async listarPeriodosActivos(): Promise<Periodo[]> {
    const res = await axios.get<Periodo[]>(`${BASE}/periodos/activos`, this.h());
    return res.data;
  }

  /** GET /api/admin/periodos/{id} → detalle de un período */
  async obtenerPeriodo(id: number): Promise<Periodo> {
    const res = await axios.get<Periodo>(`${BASE}/periodos/${id}`, this.h());
    return res.data;
  }

  /** POST /api/admin/periodos → crear período */
  async crearPeriodo(data: PeriodoRequest): Promise<Periodo> {
    const res = await axios.post<Periodo>(`${BASE}/periodos`, data, this.h());
    return res.data;
  }

  /** PUT /api/admin/periodos/{id} → actualizar período */
  async actualizarPeriodo(id: number, data: PeriodoRequest): Promise<Periodo> {
    const res = await axios.put<Periodo>(`${BASE}/periodos/${id}`, data, this.h());
    return res.data;
  }

  /** DELETE /api/admin/periodos/{id} → eliminar período */
  async eliminarPeriodo(id: number): Promise<MessageResponse> {
    const res = await axios.delete<MessageResponse>(`${BASE}/periodos/${id}`, this.h());
    return res.data;
  }

  /** PATCH /api/admin/periodos/{id}/toggle → activar/desactivar período */
  async togglePeriodo(id: number): Promise<Periodo> {
    const res = await axios.patch<Periodo>(`${BASE}/periodos/${id}/toggle`, {}, this.h());
    return res.data;
  }

  // ─── Solicitudes de Reset de Contraseña ───────────────────────────────────

  /** GET /api/admin/usuarios/solicitudes-reset → listar todas (pendientes primero) */
  async listarSolicitudesReset(): Promise<SolicitudReset[]> {
    const res = await axios.get<SolicitudReset[]>(`${BASE}/usuarios/solicitudes-reset`, this.h());
    return res.data;
  }

  /** POST /api/admin/usuarios/solicitudes-reset/{id}/atender → asignar nueva contraseña */
  async atenderSolicitudReset(id: number, nuevaPassword: string): Promise<MessageResponse> {
    const res = await axios.post<MessageResponse>(
      `${BASE}/usuarios/solicitudes-reset/${id}/atender`,
      { nuevaPassword },
      this.h()
    );
    return res.data;
  }
}

export default new AdminService();
