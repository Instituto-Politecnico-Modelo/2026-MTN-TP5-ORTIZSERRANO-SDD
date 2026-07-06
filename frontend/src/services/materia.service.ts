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
  anio?: number;
  carrera?: string | null;    // null = transversal a todas las carreras
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

export interface Correlatividad {
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
  condicion?: string;
}

export interface InscripcionRequest {
  idMateria: number;
}

export interface InscripcionResult {
  status: number;   // 201 = inscripto, 202 = en lista de espera
  data: any;
}

// --- Servicio ---

class MateriaService {

  async listarMaterias(carrera?: string): Promise<MateriaDisponible[]> {
    try {
      const params = carrera ? { params: { carrera } } : {};
      const res = await api.get<MateriaDisponible[]>(`${BASE_URL}/materias`, params);
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

  async inscribirse(idMateria: number): Promise<InscripcionResult> {
    try {
      const res = await api.post(
        `${BASE_URL}/inscripciones`,
        { idMateria } as InscripcionRequest
      );
      return { status: res.status, data: res.data };
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

  async listarCorrelatividades(idMateria: number): Promise<Correlatividad[]> {
    try {
      const res = await api.get<Correlatividad[]>(
        `${BASE_URL}/materias/${idMateria}/correlatividades`
      );
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }
}

const materiaService = new MateriaService();
export default materiaService;

/** Alias de compatibilidad hacia atras */
export type Materia = MateriaDisponible;
