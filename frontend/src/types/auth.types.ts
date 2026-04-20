// Roles disponibles en el sistema
export enum Role {
  ESTUDIANTE = 'ESTUDIANTE',
  DOCENTE = 'DOCENTE',
  ADMINISTRADOR = 'ADMINISTRADOR',
  USUARIO = 'USUARIO'
}

// Tipos para autenticación
export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  id: number;
  email: string;
  nombre: string;
  apellido: string;
  token: string;
  type: string;
  role: Role;
}

export interface User {
  idUsuario: number;
  nombre: string;
  apellido: string;
  email: string;
  activo: boolean;
}

export interface AuthError {
  error: string;
  message: string;
}
