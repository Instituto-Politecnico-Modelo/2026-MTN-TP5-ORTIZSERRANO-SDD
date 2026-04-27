/**
 * materia.service.ts
 * ──────────────────────────────────────────────────────────────────
 * Consume los endpoints de materias accesibles por cualquier
 * usuario autenticado (no solo admin):
 *
 *  GET  /api/materias                             → listado público de materias
 *  GET  /api/inscripciones/mis-inscripciones      → inscripciones del estudiante
 *  GET  /api/inscripciones/mis-notas              → boletín del estudiante
 *  POST /api/inscripciones                        → inscribirse
 *  PATCH /api/inscripciones/{id}/cancelar         → cancelar inscripción
 *
 * Para operaciones admin sobre materias usar admin.service.ts.
 */

import axios from 'axios';
import authService from './auth.service';

const BASE_URL = 'http://localhost:8080/api';

// ─── Tipos ────────────────────────────────────────────────────────────────────

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
  docente?: {
    nombre: string;
    apellido: string;
    email: string;
  };
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
  materia: Materia;
  fechaInscripcion: string;
  /** 'ACTIVA' | 'CANCELADA' | 'CERRADA' */
  estado: string;
  notaParcial1?: number;
  notaParcial2?: number;
  notaFinal?: number;
  asistencias?: number;
  notaCerrada: boolean;
}

export interface InscripcionRequest {
  idMateria: number;
}

// ─── Servicio ─────────────────────────────────────────────────────────────────

class MateriaService {

  private headers() {
    return authService.getAuthHeader();
  }

  // ── Materias (lectura pública autenticada) ─────────────────────────────────

  /** GET /api/materias → lista todas las materias disponibles */
  async listarMaterias(): Promise<Materia[]> {
    const res = await axios.get<Materia[]>(`${BASE_URL}/materias`, {
      headers: this.headers(),
    });
    return res.data;
  }

  // ── Inscripciones (estudiante autenticado) ─────────────────────────────────

  /** GET /api/inscripciones/mis-inscripciones → todas las inscripciones del estudiante */
  async misInscripciones(): Promise<Inscripcion[]> {
    const res = await axios.get<Inscripcion[]>(`${BASE_URL}/inscripciones/mis-inscripciones`, {
      headers: this.headers(),
    });
    return res.data;
  }

  /** POST /api/inscripciones → inscribirse a una materia */
  async inscribirse(idMateria: number): Promise<Inscripcion> {
    const res = await axios.post<Inscripcion>(
      `${BASE_URL}/inscripciones`,
      { idMateria } as InscripcionRequest,
      { headers: this.headers() }
    );
    return res.data;
  }

  /** PATCH /api/inscripciones/{id}/cancelar → cancelar inscripción propia */
  async cancelarInscripcion(idInscripcion: number): Promise<Inscripcion> {
    const res = await axios.patch<Inscripcion>(
      `${BASE_URL}/inscripciones/${idInscripcion}/cancelar`,
      {},
      { headers: this.headers() }
    );
    return res.data;
  }

  /** GET /api/inscripciones/mis-notas → boletín de notas del estudiante */
  async misNotas(): Promise<Inscripcion[]> {
    const res = await axios.get<Inscripcion[]>(`${BASE_URL}/inscripciones/mis-notas`, {
      headers: this.headers(),
    });
    return res.data;
  }
}

export default new MateriaService();
