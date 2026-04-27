import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login          from './components/Login';
import Dashboard      from './components/Dashboard';
import DashboardEstudiante from './components/DashboardEstudiante';
import DashboardDocente    from './components/DashboardDocente';
import DashboardAdmin      from './components/DashboardAdmin';
import Unauthorized        from './components/Unauthorized';
import ProtectedRoute      from './components/ProtectedRoute';
import ListaMaterias       from './components/ListaMaterias';
import MisInscripciones    from './components/MisInscripciones';
import Boletin             from './components/Boletin';
import GrillaNotas         from './components/GrillaNotas';
import VistaAuditoria      from './components/VistaAuditoria';
import PerfilUsuario       from './components/PerfilUsuario';
import SalaDeEspera        from './components/SalaDeEspera';
import authService from './services/auth.service';
import { Role }    from './types/auth.types';
import './App.css';

// ─────────────────────────────────────────────────────────────────────────────
//  Tabla de rutas de la aplicación
//
//  PÚBLICAS
//    /login                        → Formulario de login
//    /unauthorized                 → Pantalla de acceso denegado
//
//  ESTUDIANTE  (rol: ESTUDIANTE)
//    /estudiante/dashboard         → Dashboard del estudiante (tabs internas)
//    /estudiante/materias          → Listado de materias disponibles
//    /estudiante/inscripciones     → Mis inscripciones
//    /estudiante/boletin           → Mi boletín de notas
//
//  DOCENTE  (rol: DOCENTE)
//    /docente/dashboard            → Dashboard del docente (tabs internas)
//    /docente/notas                → Grilla de carga de notas
//
//  ADMINISTRADOR  (rol: ADMINISTRADOR)
//    /admin/dashboard              → Panel de administración (tabs internas)
//    /admin/auditoria              → Vista completa de auditoría encadenada
//
//  GENÉRICAS
//    /dashboard                    → Redirige según rol del token
//    /                             → Redirige según rol (o a /login si no autenticado)
//    *                             → Catch-all → /login
// ─────────────────────────────────────────────────────────────────────────────

function App() {
  return (
    <Router>
      <Routes>

        {/* ══════════════════════════════════════════
            RUTAS PÚBLICAS
        ══════════════════════════════════════════ */}
        <Route path="/login"           element={<Login />} />
        <Route path="/unauthorized"    element={<Unauthorized />} />
        <Route path="/sala-de-espera"  element={<SalaDeEspera />} />

        {/* ══════════════════════════════════════════
            RUTAS DE ESTUDIANTE
        ══════════════════════════════════════════ */}

        {/* Dashboard principal del estudiante (contiene tabs: inicio / materias / inscripciones / boletín) */}
        <Route path="/estudiante/dashboard" element={
          <ProtectedRoute allowedRoles={[Role.ESTUDIANTE]}>
            <DashboardEstudiante />
          </ProtectedRoute>
        } />

        {/* Listado de materias disponibles (página standalone) */}
        <Route path="/estudiante/materias" element={
          <ProtectedRoute allowedRoles={[Role.ESTUDIANTE]}>
            <div style={{ maxWidth: '1100px', margin: '40px auto', padding: '0 20px' }}>
              <ListaMaterias />
            </div>
          </ProtectedRoute>
        } />

        {/* Mis inscripciones (página standalone) */}
        <Route path="/estudiante/inscripciones" element={
          <ProtectedRoute allowedRoles={[Role.ESTUDIANTE]}>
            <div style={{ maxWidth: '1100px', margin: '40px auto', padding: '0 20px' }}>
              <MisInscripciones />
            </div>
          </ProtectedRoute>
        } />

        {/* Boletín de notas (página standalone) */}
        <Route path="/estudiante/boletin" element={
          <ProtectedRoute allowedRoles={[Role.ESTUDIANTE]}>
            <div style={{ maxWidth: '900px', margin: '40px auto', padding: '0 20px' }}>
              <Boletin />
            </div>
          </ProtectedRoute>
        } />

        {/* ══════════════════════════════════════════
            RUTAS DE DOCENTE
        ══════════════════════════════════════════ */}

        {/* Dashboard principal del docente (contiene tabs: inicio / notas) */}
        <Route path="/docente/dashboard" element={
          <ProtectedRoute allowedRoles={[Role.DOCENTE]}>
            <DashboardDocente />
          </ProtectedRoute>
        } />

        {/* Grilla de notas (página standalone, admin también puede acceder) */}
        <Route path="/docente/notas" element={
          <ProtectedRoute allowedRoles={[Role.DOCENTE, Role.ADMINISTRADOR]}>
            <div style={{ maxWidth: '1200px', margin: '40px auto', padding: '0 20px' }}>
              <GrillaNotas />
            </div>
          </ProtectedRoute>
        } />

        {/* ══════════════════════════════════════════
            RUTAS DE ADMINISTRADOR
        ══════════════════════════════════════════ */}

        {/* Dashboard principal del admin (contiene tabs: inicio / estudiantes / docentes / materias / auditoría) */}
        <Route path="/admin/dashboard" element={
          <ProtectedRoute allowedRoles={[Role.ADMINISTRADOR]}>
            <DashboardAdmin />
          </ProtectedRoute>
        } />

        {/* Vista de auditoría encadenada (página standalone) */}
        <Route path="/admin/auditoria" element={
          <ProtectedRoute allowedRoles={[Role.ADMINISTRADOR]}>
            <div style={{ maxWidth: '1200px', margin: '40px auto', padding: '0 20px' }}>
              <VistaAuditoria />
            </div>
          </ProtectedRoute>
        } />

        {/* ══════════════════════════════════════════
            RUTAS GENÉRICAS
        ══════════════════════════════════════════ */}

        {/* /dashboard → redirige al dashboard del rol correspondiente */}
        <Route path="/dashboard" element={
          <ProtectedRoute>
            <Dashboard />
          </ProtectedRoute>
        } />

        {/* Raíz "/" → si autenticado redirige al dashboard del rol; si no, al login */}
        <Route path="/" element={
          authService.isAuthenticated()
            ? <Navigate to={authService.getDefaultRoute()} replace />
            : <Navigate to="/login" replace />
        } />

        {/* Catch-all: cualquier ruta desconocida → login */}
        <Route path="*" element={<Navigate to="/login" replace />} />

        {/* ══════════════════════════════════════════
            PERFIL DE USUARIO (todos los roles)
        ══════════════════════════════════════════ */}
        <Route path="/perfil" element={
          <ProtectedRoute>
            <PerfilUsuario />
          </ProtectedRoute>
        } />

      </Routes>
    </Router>
  );
}

export default App;
