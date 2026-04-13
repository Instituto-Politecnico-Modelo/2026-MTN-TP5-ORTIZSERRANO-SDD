import axios from 'axios';
import { LoginRequest, LoginResponse, User } from '../types/auth.types';

const API_URL = 'http://localhost:8080/api/auth';

class AuthService {
  async login(email: string, password: string): Promise<LoginResponse> {
    const response = await axios.post<LoginResponse>(`${API_URL}/login`, {
      email,
      password
    });
    
    if (response.data.token) {
      localStorage.setItem('user', JSON.stringify(response.data));
    }
    
    return response.data;
  }

  logout(): void {
    localStorage.removeItem('user');
  }

  async getCurrentUser(): Promise<User> {
    const response = await axios.get<User>(`${API_URL}/me`, {
      headers: this.getAuthHeader()
    });
    return response.data;
  }

  getAuthHeader() {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      const user = JSON.parse(userStr);
      return { Authorization: `Bearer ${user.token}` };
    }
    return {};
  }

  getCurrentUserData(): LoginResponse | null {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      return JSON.parse(userStr);
    }
    return null;
  }

  isAuthenticated(): boolean {
    const user = this.getCurrentUserData();
    return user !== null && user.token !== undefined;
  }
}

export default new AuthService();
