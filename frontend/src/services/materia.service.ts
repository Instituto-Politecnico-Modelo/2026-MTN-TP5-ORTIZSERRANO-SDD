import axios from 'axios';
import authService from './auth.service';

const BASE_URL = 'http://localhost:8080/api';

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

  private headers() {
    return authService.getAuthHeader();
  }

  /** GET /api/materias - todas las materias con estado de cupos */
  async listarMaterias(): Promise<MateriaDisponible[]> {
    const res = await axios.get<MateriaDisponible[]>(`${BASE_URL}/materias`, {
      headers: this.headers(),
    });
    return res.data;
  }

  /** GET /api/materias/disponibles - solo las que tienen cupos libres */
  async listarMateriasDisponibles(): Promise<MateriaDisponible[]> {
    const res = await axios.get<MateriaDisponible[]>(`${BASE_URL}/materias/disponibles`, {
      headers: this.headers(),
    });
    return res.data;
  }

  /** GET /api/materias/{id} - detalle con cupos */
  async obtenerMateria(id: number): Promise<MateriaDisponible> {
    const res = await axios.get<MateriaDisponible>(`${BASE_URL}/materias/${id}`, {
      headers: this.headers(),
    });
    return res.data;
  }

  /** GET /api/materias/codigo/{codigo} - buscar por codigo exacto */
  async obtenerMateriaPorCodigo(codigo: string): Promise<MateriaDisponible> {
    const res = await axios.get<MateriaDisponible>(
      `${BASE_URL}/materias/codigo/${encodeURIComponent(codigo)}`,
      { headers: this.headers() }
    );
    return res.data;
  }

  /** GET /api/materias/anio/{anio} - materias de un año academico */
  async listarPorAnio(anio: number): Promise<MateriaDisponible[]> {
    const res = await axios.get<MateriaDisponible[]>(`${BASE_URL}/materias/anio/${anio}`, {
      headers: this.headers(),
    });
    return res.data;
  }

  /** GET /api/materias/anio/{anio}/cuatrimestre/{cuatrimestre} */
  async listarPorAnioYCuatrimestre(anio: number, cuatrimestre: number): Promise<MateriaDisponible[]> {
    const res = await axios.get<MateriaDisponible[]>(
      `${BASE_URL}/materias/anio/${anio}/cuatrimestre/${cuatrimestre}`,
      { headers: this.headers() }
    );
    return res.data;
  }

  /** GET /api/inscripciones/mis-inscripciones */
  async misInscripciones(): Promise<Inscripcion[]> {
    const res = await axios.get<Inscripcion[]>(`${BASE_URL}/inscripciones/mis-inscripciones`, {
      headers: this.headers(),
    });
    return res.data;
  }

  /** POST /api/inscripciones - inscribirse a una materia */
  async inscribirse(idMateria: number): Promise<Inscripcion> {
    const res = await axios.post<Inscripcion>(
      `${BASE_URL}/inscripciones`,
      { idMateria } as InscripcionRequest,
      { headers: this.headers() }
    );
    return res.data;
  }

  /** PATCH /api/inscripciones/{id}/cancelar - cancelar inscripcion propia */
  async cancelarInscripcion(idInscripcion: number): Promise<Inscripcion> {
    const res = await axios.patch<Inscripcion>(
      `${BASE_URL}/inscripciones/${idInscripcion}/cancelar`,
      {},
      { headers: this.headers() }
    );
    return res.data;
  }

  /** GET /api/inscripciones/mis-notas - boletin de notas */
  async misNotas(): Promise<Inscripcion[]> {
    const res = await axios.get<Inscripcion[]>(`${BASE_URL}/inscripciones/mis-notas`, {
      headers: this.headers(),
    });
    return res.data;
  }
}

export default new MateriaService();

/** Alias de compatibilidad hacia atras */
export type Materia = MateriaDisponible;
