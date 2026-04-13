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
