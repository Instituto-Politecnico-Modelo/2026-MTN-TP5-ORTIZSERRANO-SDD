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

import api from '../api/axiosInstance';
import { parseError } from '../utils/errorHandler';

const BASE = '/api/admin';

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

export interface CorrelatividadResponse {
  idCorrelatividad: number;
  idMateria: number;
  codigoMateria: string;
  nombreMateria: string;
  idMateriaRequerida: number;
  codigoRequerida: string;
  nombreRequerida: string;
  tipo: string;
  cumplida: boolean;
}

export interface SolicitudReset {
  idSolicitud: number;
  email: string;
  nombreCompleto?: string;
  fechaSolicitud: string;
  estado: 'PENDIENTE' | 'ATENDIDA';
  fechaAtencion?: string;
}

/** Inscripción con estado de nota — usada para operaciones de reapertura */
export interface InscripcionAdmin {
  idInscripcion: number;
  idEstudiante: number;
  idMateria: number;
  estado: string;
  notaCerrada: boolean;
  notaParcial1?: number;
  notaParcial2?: number;
  notaFinal?: number;
  asistencias?: number;
  fechaInscripcion: string;
}

// ─── Servicio ─────────────────────────────────────────────────────────────────

class AdminService {

  // ══════════════════════════════════════════════════════════════════
  //  USUARIOS  –  POST /api/admin/usuarios
  // ══════════════════════════════════════════════════════════════════

  async crearUsuario(data: UsuarioRequest): Promise<MessageResponse> {
    try {
      const res = await api.post<MessageResponse>(`${BASE}/usuarios`, data);
      return res.data;
    } catch (err) { throw parseError(err, 'crear-usuario'); }
  }

  /** Edita los atributos específicos del rol (legajo/carrera, titulo/especialidad, etc.)
   *  y también los campos base (nombre, apellido, email). */
  async editarDatosEspecificos(id: number, data: Partial<UsuarioRequest>): Promise<MessageResponse> {
    try {
      const res = await api.put<MessageResponse>(`${BASE}/usuarios/${id}/datos-especificos`, data);
      return res.data;
    } catch (err) { throw parseError(err, 'editar-usuario'); }
  }

  // ══════════════════════════════════════════════════════════════════
  //  ESTUDIANTES  –  /api/admin/estudiantes
  // ══════════════════════════════════════════════════════════════════

  async listarEstudiantes(): Promise<Estudiante[]> {
    try {
      const res = await api.get<Estudiante[]>(`${BASE}/estudiantes`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async obtenerEstudiante(id: number): Promise<Estudiante> {
    try {
      const res = await api.get<Estudiante>(`${BASE}/estudiantes/${id}`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async eliminarEstudiante(id: number): Promise<MessageResponse> {
    try {
      const res = await api.delete<MessageResponse>(`${BASE}/estudiantes/${id}`);
      return res.data;
    } catch (err) { throw parseError(err, 'eliminar-usuario'); }
  }

  // ══════════════════════════════════════════════════════════════════
  //  MATERIAS  –  /api/admin/materias
  // ══════════════════════════════════════════════════════════════════

  async listarMaterias(): Promise<Materia[]> {
    try {
      const res = await api.get<Materia[]>(`${BASE}/materias`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async obtenerMateria(id: number): Promise<Materia> {
    try {
      const res = await api.get<Materia>(`${BASE}/materias/${id}`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async crearMateria(data: MateriaRequest): Promise<Materia> {
    try {
      const res = await api.post<Materia>(`${BASE}/materias`, data);
      return res.data;
    } catch (err) { throw parseError(err, 'crear-materia'); }
  }

  async actualizarMateria(id: number, data: MateriaRequest): Promise<Materia> {
    try {
      const res = await api.put<Materia>(`${BASE}/materias/${id}`, data);
      return res.data;
    } catch (err) { throw parseError(err, 'editar-materia'); }
  }

  async eliminarMateria(id: number): Promise<MessageResponse> {
    try {
      const res = await api.delete<MessageResponse>(`${BASE}/materias/${id}`);
      return res.data;
    } catch (err) { throw parseError(err, 'eliminar-materia'); }
  }

  // ══════════════════════════════════════════════════════════════════
  //  PERÍODOS  –  /api/admin/periodos
  // ══════════════════════════════════════════════════════════════════

  async listarPeriodos(): Promise<Periodo[]> {
    try {
      const res = await api.get<Periodo[]>(`${BASE}/periodos`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async listarPeriodosActivos(): Promise<Periodo[]> {
    try {
      const res = await api.get<Periodo[]>(`${BASE}/periodos/activos`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async obtenerPeriodo(id: number): Promise<Periodo> {
    try {
      const res = await api.get<Periodo>(`${BASE}/periodos/${id}`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async crearPeriodo(data: PeriodoRequest): Promise<Periodo> {
    try {
      const res = await api.post<Periodo>(`${BASE}/periodos`, data);
      return res.data;
    } catch (err) { throw parseError(err, 'crear-periodo'); }
  }

  async actualizarPeriodo(id: number, data: PeriodoRequest): Promise<Periodo> {
    try {
      const res = await api.put<Periodo>(`${BASE}/periodos/${id}`, data);
      return res.data;
    } catch (err) { throw parseError(err, 'editar-periodo'); }
  }

  async eliminarPeriodo(id: number): Promise<MessageResponse> {
    try {
      const res = await api.delete<MessageResponse>(`${BASE}/periodos/${id}`);
      return res.data;
    } catch (err) { throw parseError(err, 'eliminar-periodo'); }
  }

  async togglePeriodo(id: number): Promise<Periodo> {
    try {
      const res = await api.patch<Periodo>(`${BASE}/periodos/${id}/toggle`, {});
      return res.data;
    } catch (err) { throw parseError(err, 'editar-periodo'); }
  }

  // ─── Solicitudes de Reset de Contraseña ───────────────────────────────────

  async listarSolicitudesReset(): Promise<SolicitudReset[]> {
    try {
      const res = await api.get<SolicitudReset[]>(`${BASE}/usuarios/solicitudes-reset`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async atenderSolicitudReset(id: number, nuevaPassword: string): Promise<MessageResponse> {
    try {
      const res = await api.post<MessageResponse>(
        `${BASE}/usuarios/solicitudes-reset/${id}/atender`,
        { nuevaPassword }
      );
      return res.data;
    } catch (err) { throw parseError(err, 'atender-reset'); }
  }

  // ══════════════════════════════════════════════════════════════════
  //  NOTAS  –  /api/admin/inscripciones
  // ══════════════════════════════════════════════════════════════════

  /** Reabre una nota cerrada.
   *  Solo ADMINISTRADOR. Registra auditoría con accion = 'NOTA_REABIERTA'.
   *  @param id     ID de la inscripción con nota cerrada
   *  @param motivo Motivo obligatorio de reapertura */
  async reabrir(id: number, motivo: string): Promise<InscripcionAdmin> {
    try {
      const res = await api.patch<InscripcionAdmin>(
        `/api/admin/inscripciones/${id}/reabrir`,
        { motivo }
      );
      return res.data;
    } catch (err) { throw parseError(err, 'reabrir-nota'); }
  }

  // ══════════════════════════════════════════════════════════════════
  //  CORRELATIVIDADES  –  /api/admin/correlatividades
  // ══════════════════════════════════════════════════════════════════

  async listarCorrelatividades(idMateria: number): Promise<CorrelatividadResponse[]> {
    try {
      const res = await api.get<CorrelatividadResponse[]>(
        `/api/admin/correlatividades/materia/${idMateria}`
      );
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async crearCorrelatividad(data: {
    idMateria: number;
    idMateriaRequerida: number;
    tipo: string;
  }): Promise<CorrelatividadResponse> {
    try {
      const res = await api.post<CorrelatividadResponse>(
        `/api/admin/correlatividades`, data
      );
      return res.data;
    } catch (err) { throw parseError(err, 'crear-correlatividad'); }
  }

  async eliminarCorrelatividad(id: number): Promise<MessageResponse> {
    try {
      const res = await api.delete<MessageResponse>(
        `/api/admin/correlatividades/${id}`
      );
      return res.data;
    } catch (err) { throw parseError(err, 'eliminar-correlatividad'); }
  }
}

const adminService = new AdminService();
export default adminService;
