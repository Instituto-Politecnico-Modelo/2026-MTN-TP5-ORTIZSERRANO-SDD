import api from '../api/axiosInstance';
import { parseError } from '../utils/errorHandler';

const BASE_URL = '/api';

// --- Tipos ---

/** Respuesta enriquecida con estado de cupos calculado por el backend */
export interface MateriaDisponible {
  idMateria: number;
  codigo: string;
  nombre: string;
  descripcion?: string;
  creditos?: number;
  anio?: number;
  cuatrimestre?: number;
  docenteNombre?: string;
  docenteApellido?: string;
  docenteEmail?: string;
  cuposMaximos?: number | null;
  cuposOcupados: number;
  cuposDisponibles?: number | null;
  hayLugar: boolean;
  porcentajeOcupacion?: number | null;
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
  materia: MateriaDisponible;
  fechaInscripcion: string;
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

// --- Servicio ---

class MateriaService {

  async listarMaterias(): Promise<MateriaDisponible[]> {
    try {
      const res = await api.get<MateriaDisponible[]>(`${BASE_URL}/materias`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async listarMateriasDisponibles(): Promise<MateriaDisponible[]> {
    try {
      const res = await api.get<MateriaDisponible[]>(`${BASE_URL}/materias/disponibles`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async obtenerMateria(id: number): Promise<MateriaDisponible> {
    try {
      const res = await api.get<MateriaDisponible>(`${BASE_URL}/materias/${id}`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async obtenerMateriaPorCodigo(codigo: string): Promise<MateriaDisponible> {
    try {
      const res = await api.get<MateriaDisponible>(
        `${BASE_URL}/materias/codigo/${encodeURIComponent(codigo)}`
      );
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async listarPorAnio(anio: number): Promise<MateriaDisponible[]> {
    try {
      const res = await api.get<MateriaDisponible[]>(`${BASE_URL}/materias/anio/${anio}`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async listarPorAnioYCuatrimestre(anio: number, cuatrimestre: number): Promise<MateriaDisponible[]> {
    try {
      const res = await api.get<MateriaDisponible[]>(
        `${BASE_URL}/materias/anio/${anio}/cuatrimestre/${cuatrimestre}`
      );
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async misInscripciones(): Promise<Inscripcion[]> {
    try {
      const res = await api.get<Inscripcion[]>(`${BASE_URL}/inscripciones/mis-inscripciones`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async inscribirse(idMateria: number): Promise<Inscripcion> {
    try {
      const res = await api.post<Inscripcion>(
        `${BASE_URL}/inscripciones`,
        { idMateria } as InscripcionRequest
      );
      return res.data;
    } catch (err) { throw parseError(err, 'inscribirse'); }
  }

  async cancelarInscripcion(idInscripcion: number): Promise<Inscripcion> {
    try {
      const res = await api.patch<Inscripcion>(
        `${BASE_URL}/inscripciones/${idInscripcion}/cancelar`,
        {}
      );
      return res.data;
    } catch (err) { throw parseError(err, 'cancelar-inscripcion'); }
  }

  async misNotas(): Promise<Inscripcion[]> {
    try {
      const res = await api.get<Inscripcion[]>(`${BASE_URL}/inscripciones/mis-notas`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }
}

export default new MateriaService();

/** Alias de compatibilidad hacia atras */
export type Materia = MateriaDisponible;
