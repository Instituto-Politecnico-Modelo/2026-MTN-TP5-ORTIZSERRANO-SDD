import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/auth.service';
import { User } from '../types/auth.types';
import './Dashboard.css';

const Dashboard: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchUserData = async () => {
      try {
        const userData = await authService.getCurrentUser();
        setUser(userData);
      } catch (error) {
        console.error('Error al obtener datos del usuario:', error);
        authService.logout();
        navigate('/login');
      } finally {
        setLoading(false);
      }
    };

    if (authService.isAuthenticated()) {
      fetchUserData();
    } else {
      navigate('/login');
    }
  }, [navigate]);

  const handleLogout = () => {
    authService.logout();
    navigate('/login');
  };

  if (loading) {
    return (
      <div className="dashboard-loading">
        <div className="spinner-large"></div>
        <p>Cargando...</p>
      </div>
    );
  }

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <div className="header-content">
          <h1>Sistema de Gestión Decanato</h1>
          <button onClick={handleLogout} className="logout-button">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
            Cerrar Sesión
          </button>
        </div>
      </header>

      <main className="dashboard-main">
        <div className="welcome-card">
          <div className="avatar">
            {user?.nombre.charAt(0)}{user?.apellido.charAt(0)}
          </div>
          <h2>¡Bienvenido, {user?.nombre} {user?.apellido}!</h2>
          <p className="user-email">{user?.email}</p>
          <div className="status-badge">
            <span className={`status-dot ${user?.activo ? 'active' : 'inactive'}`}></span>
            {user?.activo ? 'Usuario Activo' : 'Usuario Inactivo'}
          </div>
        </div>

        <div className="dashboard-grid">
          <div className="dashboard-card">
            <div className="card-icon" style={{ backgroundColor: '#e3f2fd' }}>
              <svg fill="none" viewBox="0 0 24 24" stroke="#2196f3">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
            </div>
            <h3>Materias</h3>
            <p>Gestionar materias y cursos</p>
          </div>

          <div className="dashboard-card">
            <div className="card-icon" style={{ backgroundColor: '#f3e5f5' }}>
              <svg fill="none" viewBox="0 0 24 24" stroke="#9c27b0">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
              </svg>
            </div>
            <h3>Estudiantes</h3>
            <p>Administrar estudiantes</p>
          </div>

          <div className="dashboard-card">
            <div className="card-icon" style={{ backgroundColor: '#e8f5e9' }}>
              <svg fill="none" viewBox="0 0 24 24" stroke="#4caf50">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
            <h3>Inscripciones</h3>
            <p>Gestionar inscripciones</p>
          </div>

          <div className="dashboard-card">
            <div className="card-icon" style={{ backgroundColor: '#fff3e0' }}>
              <svg fill="none" viewBox="0 0 24 24" stroke="#ff9800">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </div>
            <h3>Configuración</h3>
            <p>Ajustes del sistema</p>
          </div>
        </div>

        <div className="info-section">
          <h3>Información del Usuario</h3>
          <div className="info-grid">
            <div className="info-item">
              <span className="info-label">ID:</span>
              <span className="info-value">{user?.idUsuario}</span>
            </div>
            <div className="info-item">
              <span className="info-label">Nombre Completo:</span>
              <span className="info-value">{user?.nombre} {user?.apellido}</span>
            </div>
            <div className="info-item">
              <span className="info-label">Email:</span>
              <span className="info-value">{user?.email}</span>
            </div>
            <div className="info-item">
              <span className="info-label">Estado:</span>
              <span className="info-value">{user?.activo ? 'Activo' : 'Inactivo'}</span>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Dashboard;
