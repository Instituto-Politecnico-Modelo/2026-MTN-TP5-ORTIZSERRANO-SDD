import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from './components/Login';
import Register from './components/Register';
import Dashboard from './components/Dashboard';
import DashboardEstudiante from './components/DashboardEstudiante';
import DashboardDocente from './components/DashboardDocente';
import DashboardAdmin from './components/DashboardAdmin';
import Unauthorized from './components/Unauthorized';
import ProtectedRoute from './components/ProtectedRoute';
import ListaMaterias from './components/ListaMaterias';
import MisInscripciones from './components/MisInscripciones';
import authService from './services/auth.service';
import { Role } from './types/auth.types';
import './App.css';

function App() {
  return (
    <Router>
      <Routes>
        {/* ── Rutas públicas ─────────────────────────── */}
        <Route path="/login"    element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/unauthorized" element={<Unauthorized />} />

        {/* ── Rutas protegidas por ROL ────────────────── */}

        {/* Solo ESTUDIANTE */}
        <Route path="/estudiante/dashboard" element={
          <ProtectedRoute allowedRoles={[Role.ESTUDIANTE]}>
            <DashboardEstudiante />
          </ProtectedRoute>
        } />

        {/* Solo DOCENTE */}
        <Route path="/docente/dashboard" element={
          <ProtectedRoute allowedRoles={[Role.DOCENTE]}>
            <DashboardDocente />
          </ProtectedRoute>
        } />

        {/* Solo ADMINISTRADOR */}
        <Route path="/admin/dashboard" element={
          <ProtectedRoute allowedRoles={[Role.ADMINISTRADOR]}>
            <DashboardAdmin />
          </ProtectedRoute>
        } />

        {/* Dashboard genérico (cualquier usuario autenticado) */}
        <Route path="/dashboard" element={
          <ProtectedRoute>
            <Dashboard />
          </ProtectedRoute>
        } />

        {/* Materias – accesible para estudiantes */}
        <Route path="/estudiante/materias" element={
          <ProtectedRoute allowedRoles={[Role.ESTUDIANTE]}>
            <div style={{ maxWidth: '1100px', margin: '40px auto', padding: '0 20px' }}>
              <ListaMaterias />
            </div>
          </ProtectedRoute>
        } />

        {/* Mis inscripciones */}
        <Route path="/estudiante/inscripciones" element={
          <ProtectedRoute allowedRoles={[Role.ESTUDIANTE]}>
            <div style={{ maxWidth: '1100px', margin: '40px auto', padding: '0 20px' }}>
              <MisInscripciones />
            </div>
          </ProtectedRoute>
        } />

        {/* ── Redirección raíz ───────────────────────── */}
        <Route path="/" element={
          authService.isAuthenticated()
            ? <Navigate to={authService.getDefaultRoute()} replace />
            : <Navigate to="/login" replace />
        } />

        {/* ── Catch-all ──────────────────────────────── */}
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </Router>
  );
}

export default App;
