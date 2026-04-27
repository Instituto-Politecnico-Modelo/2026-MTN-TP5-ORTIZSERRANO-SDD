/**
 * inscripcion.service.ts
 * ──────────────────────────────────────────────────────────────────
 * Consume los endpoints de inscripciones:
 *
 *  /api/inscripciones                     → inscribirse (POST)
 *  /api/inscripciones/mis-inscripciones   → listar propias (GET)
 *  /api/inscripciones/mis-notas           → boletín (GET)
 *  /api/inscripciones/{id}/cancelar       → cancelar propia (PATCH)
 *  /api/inscripciones/todas               → todas (GET, admin)
 */

import axios from 'axios';
import authService from './auth.service';

const BASE = 'http://localhost:8080/api/inscripciones';

// ─── Tipos ────────────────────────────────────────────────────────────────────

export interface InscripcionRequest {
  idMateria: number;
}

export interface MateriaResumen {
  idMateria: number;
  codigo: string;
  nombre: string;
  anio: number;
  cuatrimestre: number;
}

export interface EstudianteResumen {
  idUsuario: number;
  nombre: string;
  apellido: string;
  email: string;
}

export interface Inscripcion {
  idInscripcion: number;
  estudiante?: EstudianteResumen;
  materia: MateriaResumen;
  fechaInscripcion: string;
  /** 'ACTIVA' | 'CANCELADA' | 'CERRADA' */
  estado: string;
  notaParcial1?: number;
  notaParcial2?: number;
  notaFinal?: number;
  asistencias?: number;
  notaCerrada: boolean;
}

// ─── Servicio ─────────────────────────────────────────────────────────────────

class InscripcionService {

  private h() {
    return { headers: authService.getAuthHeader() };
  }

  /**
   * POST /api/inscripciones
   * Inscribe al estudiante autenticado en una materia.
   */
  async inscribirse(idMateria: number): Promise<Inscripcion> {
    const body: InscripcionRequest = { idMateria };
    const res = await axios.post<Inscripcion>(BASE, body, this.h());
    return res.data;
  }

  /**
   * GET /api/inscripciones/mis-inscripciones
   * Lista todas las inscripciones del estudiante autenticado.
   */
  async misInscripciones(): Promise<Inscripcion[]> {
    const res = await axios.get<Inscripcion[]>(`${BASE}/mis-inscripciones`, this.h());
    return res.data;
  }

  /**
   * GET /api/inscripciones/mis-notas
   * Inscripciones del estudiante autenticado que tienen nota cargada (boletín).
   */
  async misNotas(): Promise<Inscripcion[]> {
    const res = await axios.get<Inscripcion[]>(`${BASE}/mis-notas`, this.h());
    return res.data;
  }

  /**
   * PATCH /api/inscripciones/{id}/cancelar
   * Cancela la inscripción propia indicada.
   */
  async cancelar(idInscripcion: number): Promise<Inscripcion> {
    const res = await axios.patch<Inscripcion>(`${BASE}/${idInscripcion}/cancelar`, {}, this.h());
    return res.data;
  }

  /**
   * GET /api/inscripciones/todas
   * Lista TODAS las inscripciones del sistema (solo admin).
   */
  async listarTodas(): Promise<Inscripcion[]> {
    const res = await axios.get<Inscripcion[]>(`${BASE}/todas`, this.h());
    return res.data;
  }
}

export default new InscripcionService();
