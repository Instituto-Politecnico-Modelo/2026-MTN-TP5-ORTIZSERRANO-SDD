import api from '../api/axiosInstance';
import { parseError } from '../utils/errorHandler';
import { Inscripcion, Materia } from './materia.service';

const BASE_URL = '/api';

export interface NotaRequest {
  notaParcial1?: number;
  notaParcial2?: number;
  notaFinal?: number;
  asistencias?: number;
}

class DocenteService {

  async listarMaterias(): Promise<Materia[]> {
    try {
      const res = await api.get<Materia[]>(`${BASE_URL}/materias`);
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async alumnosPorMateria(idMateria: number): Promise<Inscripcion[]> {
    try {
      const res = await api.get<Inscripcion[]>(
        `${BASE_URL}/docente/materias/${idMateria}/inscripciones`
      );
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }

  async cargarNota(idInscripcion: number, nota: NotaRequest): Promise<Inscripcion> {
    try {
      const res = await api.put<Inscripcion>(
        `${BASE_URL}/docente/inscripciones/${idInscripcion}/nota`,
        nota
      );
      return res.data;
    } catch (err) { throw parseError(err, 'cargar-nota'); }
  }

  async cerrarNota(idInscripcion: number): Promise<Inscripcion> {
    try {
      const res = await api.patch<Inscripcion>(
        `${BASE_URL}/docente/inscripciones/${idInscripcion}/cerrar`,
        {}
      );
      return res.data;
    } catch (err) { throw parseError(err, 'cerrar-nota'); }
  }

  async historialAsistencias(idEstudiante: number): Promise<Inscripcion[]> {
    try {
      const res = await api.get<Inscripcion[]>(
        `${BASE_URL}/docente/estudiantes/${idEstudiante}/asistencias`
      );
      return res.data;
    } catch (err) { throw parseError(err, 'generic'); }
  }
}

export default new DocenteService();
