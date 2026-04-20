import React from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/auth.service';

const DashboardAdmin: React.FC = () => {
  const navigate = useNavigate();
  const user = authService.getCurrentUserData();

  const handleLogout = () => {
    authService.logout();
    navigate('/login');
  };

  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#fdf4ff' }}>
      <header style={{
        background: 'linear-gradient(135deg, #7c3aed 0%, #4c1d95 100%)',
        color: 'white', padding: '20px 40px',
        display: 'flex', justifyContent: 'space-between', alignItems: 'center'
      }}>
        <div>
          <h1 style={{ margin: 0, fontSize: '22px' }}>⚙️ Panel de Administración</h1>
          <p style={{ margin: '4px 0 0', opacity: 0.85, fontSize: '14px' }}>
            {user?.nombre} {user?.apellido}
          </p>
        </div>
        <button onClick={handleLogout} style={{
          padding: '8px 18px', background: 'rgba(255,255,255,0.2)',
          color: 'white', border: '1px solid rgba(255,255,255,0.4)',
          borderRadius: '8px', cursor: 'pointer', fontSize: '14px'
        }}>
          Cerrar Sesión
        </button>
      </header>

      <main style={{ maxWidth: '1200px', margin: '0 auto', padding: '40px 20px' }}>
        <div style={{
          background: 'white', borderRadius: '16px', padding: '30px',
          boxShadow: '0 2px 10px rgba(0,0,0,0.06)', marginBottom: '24px',
          display: 'flex', alignItems: 'center', gap: '20px'
        }}>
          <div style={{
            width: '70px', height: '70px', borderRadius: '50%',
            background: 'linear-gradient(135deg, #7c3aed, #4c1d95)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: 'white', fontSize: '28px', fontWeight: 700
          }}>
            {user?.nombre?.charAt(0)}{user?.apellido?.charAt(0)}
          </div>
          <div>
            <h2 style={{ margin: 0, color: '#1e293b' }}>¡Bienvenido, {user?.nombre}!</h2>
            <p style={{ margin: '4px 0 0', color: '#64748b' }}>{user?.email}</p>
            <span style={{
              display: 'inline-block', marginTop: '8px', padding: '4px 12px',
              background: '#f3e8ff', color: '#7c3aed', borderRadius: '20px', fontSize: '12px', fontWeight: 600
            }}>
              ⚙️ Administrador
            </span>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '20px' }}>
          {[
            { icon: '👥', title: 'Usuarios', desc: 'Gestionar todos los usuarios', color: '#f3e8ff' },
            { icon: '🎓', title: 'Estudiantes', desc: 'Administrar estudiantes', color: '#dbeafe' },
            { icon: '🏫', title: 'Docentes', desc: 'Administrar docentes', color: '#dcfce7' },
            { icon: '📚', title: 'Materias', desc: 'Gestionar materias', color: '#fef9c3' },
            { icon: '📝', title: 'Inscripciones', desc: 'Ver todas las inscripciones', color: '#fce7f3' },
            { icon: '📊', title: 'Reportes', desc: 'Ver estadísticas del sistema', color: '#ffedd5' },
          ].map((card) => (
            <div key={card.title} style={{
              background: 'white', borderRadius: '14px', padding: '24px',
              boxShadow: '0 2px 8px rgba(0,0,0,0.05)', cursor: 'pointer',
              transition: 'transform 0.2s', border: '1px solid #f1f5f9'
            }}
              onMouseEnter={e => (e.currentTarget.style.transform = 'translateY(-4px)')}
              onMouseLeave={e => (e.currentTarget.style.transform = 'translateY(0)')}
            >
              <div style={{
                width: '50px', height: '50px', borderRadius: '12px',
                background: card.color, display: 'flex', alignItems: 'center',
                justifyContent: 'center', fontSize: '24px', marginBottom: '14px'
              }}>
                {card.icon}
              </div>
              <h3 style={{ margin: '0 0 6px', color: '#1e293b' }}>{card.title}</h3>
              <p style={{ margin: 0, color: '#64748b', fontSize: '14px' }}>{card.desc}</p>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
};

export default DashboardAdmin;
