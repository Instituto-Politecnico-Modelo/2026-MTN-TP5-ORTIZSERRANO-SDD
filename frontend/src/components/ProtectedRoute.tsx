import React from 'react';
import { Navigate } from 'react-router-dom';
import authService from '../services/auth.service';
import { Role } from '../types/auth.types';

interface ProtectedRouteProps {
  children: React.ReactElement;
  allowedRoles?: Role[];
}

/**
 * Protege una ruta:
 * 1. Si no está autenticado → redirige a /login
 * 2. Si no tiene el rol requerido → redirige a /unauthorized
 * 3. Si todo OK → renderiza el componente hijo
 */
const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, allowedRoles }) => {
  if (!authService.isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && allowedRoles.length > 0) {
    if (!authService.hasAnyRole(allowedRoles)) {
      return <Navigate to="/unauthorized" replace />;
    }
  }

  return children;
};

export default ProtectedRoute;
