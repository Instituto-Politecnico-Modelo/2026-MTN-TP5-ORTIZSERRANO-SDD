import { Role, User, LoginResponse } from '../types/auth.types';
import api from '../api/axiosInstance';
import { AppError, parseError } from '../utils/errorHandler';

const BASE_URL = '/api';
const USER_KEY = 'authUser';

export { AppError };

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
    try {
      const res = await api.post<LoginResponse>(`${BASE_URL}/auth/login`, { email, password });
      if (res.data.token) {
        localStorage.setItem(USER_KEY, JSON.stringify(res.data));
      }
      return res.data;
    } catch (err) {
      throw parseError(err, 'login');
    }
  }

  logout(): void {
    localStorage.removeItem(USER_KEY);
  }

  /** Llama a GET /api/auth/me y retorna el perfil completo del usuario */
  async getCurrentUser(): Promise<User> {
    try {
      const res = await api.get<User>(`${BASE_URL}/auth/me`);
      return res.data;
    } catch (err) {
      throw parseError(err, 'update-profile');
    }
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

  /** GET /api/auth/jwt/inspect?token=xxx — requiere autenticación */
  async inspectJwt(token: string): Promise<JwtInspectResponse> {
    try {
      const res = await api.get<JwtInspectResponse>(`${BASE_URL}/auth/jwt/inspect`, {
        params: { token },
        headers: { Authorization: `Bearer ${token}` },
      });
      return res.data;
    } catch (err) {
      throw parseError(err, 'generic');
    }
  }

  // ─── Olvidé mi contraseña ─────────────────────────────────────────────────

  /** POST /api/auth/olvide-password — endpoint público, no requiere auth */
  async olvidePassword(email: string): Promise<{ message: string }> {
    try {
      const res = await api.post<{ message: string }>(`${BASE_URL}/auth/olvide-password`, { email });
      return res.data;
    } catch (err) {
      throw parseError(err, 'solicitud-password');
    }
  }
}

export default new AuthService();