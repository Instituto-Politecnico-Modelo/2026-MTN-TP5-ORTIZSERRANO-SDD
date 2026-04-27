import axios from 'axios';
import { Role, User, LoginResponse } from '../types/auth.types';

const BASE_URL = 'http://localhost:8080/api';
const USER_KEY = 'authUser';

export interface JwtInspectResponse {
  token_recibido: string;
  valido: boolean;
  motivo_invalidez?: string;
  subject_email?: string;
  emitido_en?: string;
  expira_en?: string;
  segundos_hasta_expiracion?: number;
  ya_expirado?: boolean;
  perfil_usuario?: {
    idUsuario: number;
    nombre: string;
    apellido: string;
    email: string;
    activo: boolean;
    rol: string;
  };
}

class AuthService {

  async login(email: string, password: string): Promise<LoginResponse> {
    const res = await axios.post<LoginResponse>(`${BASE_URL}/auth/login`, { email, password });
    if (res.data.token) {
      localStorage.setItem(USER_KEY, JSON.stringify(res.data));
    }
    return res.data;
  }

  logout(): void {
    localStorage.removeItem(USER_KEY);
  }

  /** Llama a GET /api/auth/me y retorna el perfil completo del usuario */
  async getCurrentUser(): Promise<User> {
    const res = await axios.get<User>(`${BASE_URL}/auth/me`, {
      headers: this.getAuthHeader(),
    });
    return res.data;
  }

  getAuthHeader(): Record<string, string> {
    const user = this.getCurrentUserData();
    return user?.token ? { Authorization: `Bearer ${user.token}` } : {};
  }

  /** Lee el LoginResponse guardado en localStorage (sync, sin llamada a red) */
  getCurrentUserData(): LoginResponse | null {
    const userStr = localStorage.getItem(USER_KEY);
    return userStr ? JSON.parse(userStr) : null;
  }

  isAuthenticated(): boolean {
    return !!this.getCurrentUserData()?.token;
  }

  getRole(): Role | null {
    return (this.getCurrentUserData()?.role as Role) ?? null;
  }

  hasRole(role: Role): boolean {
    return this.getRole() === role;
  }

  /** Acepta array de roles (compatible con ProtectedRoute) */
  hasAnyRole(roles: Role[]): boolean {
    const role = this.getRole();
    return role ? roles.includes(role) : false;
  }

  isEstudiante(): boolean    { return this.hasRole(Role.ESTUDIANTE); }
  isDocente(): boolean       { return this.hasRole(Role.DOCENTE); }
  isAdministrador(): boolean { return this.hasRole(Role.ADMINISTRADOR); }

  getDefaultRoute(): string {
    switch (this.getRole()) {
      case Role.ADMINISTRADOR: return '/admin/dashboard';
      case Role.DOCENTE:       return '/docente/dashboard';
      case Role.ESTUDIANTE:    return '/estudiante/dashboard';
      default:                 return '/dashboard';
    }
  }

  // ─── JWT Inspect ──────────────────────────────────────────────────────────

  /** GET /api/auth/jwt/inspect?token=xxx — endpoint público, no requiere auth */
  async inspectJwt(token: string): Promise<JwtInspectResponse> {
    const res = await axios.get<JwtInspectResponse>(`${BASE_URL}/auth/jwt/inspect`, {
      params: { token },
    });
    return res.data;
  }

  // ─── Olvidé mi contraseña ─────────────────────────────────────────────────

  /** POST /api/auth/olvide-password — endpoint público, no requiere auth */
  async olvidePassword(email: string): Promise<{ message: string }> {
    const res = await axios.post<{ message: string }>(`${BASE_URL}/auth/olvide-password`, { email });
    return res.data;
  }
}

export default new AuthService();
