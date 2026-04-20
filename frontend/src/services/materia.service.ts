import axios from 'axios';
import authService from './auth.service';

const BASE_URL = 'http://localhost:8080/api';

export interface Materia {
  idMateria: number;
  codigo: string;
  nombre: string;
  descripcion: string;
  creditos: number;
  anio: number;
  cuatrimestre: number;
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

class MateriaService {
  private headers() {
    return authService.getAuthHeader();
  }

  async listarMaterias(): Promise<Materia[]> {
    const res = await axios.get<Materia[]>(`${BASE_URL}/materias`, {
      headers: this.headers(),
    });
    return res.data;
  }

  async misInscripciones(): Promise<Inscripcion[]> {
    const res = await axios.get<Inscripcion[]>(`${BASE_URL}/inscripciones/mis-inscripciones`, {
      headers: this.headers(),
    });
    return res.data;
  }

  async inscribirse(idMateria: number): Promise<Inscripcion> {
    const res = await axios.post<Inscripcion>(
      `${BASE_URL}/inscripciones`,
      { idMateria } as InscripcionRequest,
      { headers: this.headers() }
    );
    return res.data;
  }

  async cancelarInscripcion(idInscripcion: number): Promise<void> {
    await axios.patch(
      `${BASE_URL}/inscripciones/${idInscripcion}/cancelar`,
      {},
      { headers: this.headers() }
    );
  }

  async misNotas(): Promise<Inscripcion[]> {
    const res = await axios.get<Inscripcion[]>(`${BASE_URL}/inscripciones/mis-notas`, {
      headers: this.headers(),
    });
    return res.data;
  }
}

export default new MateriaService();
