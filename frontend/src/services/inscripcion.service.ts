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

import api from '../api/axiosInstance';
import { parseError } from '../utils/errorHandler';

const BASE = '/api/inscripciones';

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

  async inscribirse(idMateria: number): Promise<Inscripcion> {
    try {
      const body: InscripcionRequest = { idMateria };
      const res = await api.post<Inscripcion>(BASE, body);
      return res.data;
    } catch (err) { throw parseError(err, 'inscribirse'); }
  }

  async misInscripciones(): Promise<Inscripcion[]> {
    try {
      const res = await api.get<Inscripcion[]>(`${BASE}/mis-inscripciones`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async misNotas(): Promise<Inscripcion[]> {
    try {
      const res = await api.get<Inscripcion[]>(`${BASE}/mis-notas`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async cancelar(idInscripcion: number): Promise<Inscripcion> {
    try {
      const res = await api.patch<Inscripcion>(`${BASE}/${idInscripcion}/cancelar`, {});
      return res.data;
    } catch (err) { throw parseError(err, 'cancelar-inscripcion'); }
  }

  async listarTodas(): Promise<Inscripcion[]> {
    try {
      const res = await api.get<Inscripcion[]>(`${BASE}/todas`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }
}

export default new InscripcionService();
