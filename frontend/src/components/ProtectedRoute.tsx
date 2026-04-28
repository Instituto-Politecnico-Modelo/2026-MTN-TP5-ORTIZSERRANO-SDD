import React from 'react';
import { Navigate } from 'react-router-dom';
import authService from '../services/auth.service';
import { Role } from '../types/auth.types';

interface ProtectedRouteProps {
  children: React.ReactElement;
  allowedRoles?: Role[];
}

/** Decodifica el payload JWT y verifica si ya expiró (sin llamada al servidor) */
function isJwtExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    if (typeof payload.exp !== 'number') return false;
    return Date.now() / 1000 >= payload.exp;
  } catch {
    return true; // token malformado → tratar como expirado
  }
}

/**
 * Protege una ruta:
 * 1. Sin token              → /login
 * 2. Token expirado (JWT)   → /login?motivo=expired
 * 3. Rol no autorizado      → /unauthorized
 * 4. Todo OK                → renderiza el hijo
 */
const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, allowedRoles }) => {
  const user = authService.getCurrentUserData();

  // 1. Sin sesión
  if (!user?.token) {
    return <Navigate to="/login" replace />;
  }

  // 2. Token expirado
  if (isJwtExpired(user.token)) {
    authService.logout(); // limpiar localStorage
    return <Navigate to="/login?motivo=expired" replace />;
  }

  // 3. Rol insuficiente
  if (allowedRoles && allowedRoles.length > 0) {
    if (!authService.hasAnyRole(allowedRoles)) {
      return <Navigate to="/unauthorized" replace />;
    }
  }

  return children;
};

export default ProtectedRoute;

