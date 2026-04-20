import axios from 'axios';
import authService from './auth.service';
import { Inscripcion, Materia } from './materia.service';

const BASE_URL = 'http://localhost:8080/api';

export interface NotaRequest {
  notaParcial1?: number;
  notaParcial2?: number;
  notaFinal?: number;
  asistencias?: number;
}

class DocenteService {
  private headers() {
    return authService.getAuthHeader();
  }

  /** Lista todas las materias (para que el docente elija cuál gestionar) */
  async listarMaterias(): Promise<Materia[]> {
    const res = await axios.get<Materia[]>(`${BASE_URL}/materias`, {
      headers: this.headers(),
    });
    return res.data;
  }

  /** Alumnos inscriptos en una materia (excluye cancelados) */
  async alumnosPorMateria(idMateria: number): Promise<Inscripcion[]> {
    const res = await axios.get<Inscripcion[]>(
      `${BASE_URL}/docente/materias/${idMateria}/inscripciones`,
      { headers: this.headers() }
    );
    return res.data;
  }

  /** Carga o actualiza notas de una inscripción (PUT) */
  async cargarNota(idInscripcion: number, nota: NotaRequest): Promise<Inscripcion> {
    const res = await axios.put<Inscripcion>(
      `${BASE_URL}/docente/inscripciones/${idInscripcion}/nota`,
      nota,
      { headers: this.headers() }
    );
    return res.data;
  }

  /** Cierra definitivamente la nota de una inscripción */
  async cerrarNota(idInscripcion: number): Promise<Inscripcion> {
    const res = await axios.patch<Inscripcion>(
      `${BASE_URL}/docente/inscripciones/${idInscripcion}/cerrar`,
      {},
      { headers: this.headers() }
    );
    return res.data;
  }

  /** Historial de asistencias de un alumno (PADRE/DOCENTE/ADMIN) */
  async historialAsistencias(idEstudiante: number): Promise<Inscripcion[]> {
    const res = await axios.get<Inscripcion[]>(
      `${BASE_URL}/docente/estudiantes/${idEstudiante}/asistencias`,
      { headers: this.headers() }
    );
    return res.data;
  }
}

export default new DocenteService();
