import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/auth.service';
import GrillaNotas from './GrillaNotas';

type Vista = 'inicio' | 'notas';

interface TabProps {
  label: string;
  icon: string;
  active: boolean;
  onClick: () => void;
}

const Tab: React.FC<TabProps> = ({ label, icon, active, onClick }) => (
  <button
    onClick={onClick}
    style={{
      padding: '8px 18px', border: 'none', cursor: 'pointer',
      background: active ? 'white' : 'transparent',
      color: active ? '#15803d' : 'rgba(255,255,255,0.75)',
      borderRadius: '8px', fontSize: '14px', fontWeight: active ? 700 : 500,
      display: 'flex', alignItems: 'center', gap: '6px',
      boxShadow: active ? '0 2px 8px rgba(0,0,0,0.10)' : 'none',
      transition: 'all 0.15s',
    }}
  >
    {icon} {label}
  </button>
);

const DashboardDocente: React.FC = () => {
  const navigate = useNavigate();
  const user = authService.getCurrentUserData();
  const [vista, setVista] = useState<Vista>('inicio');

  const handleLogout = () => {
    authService.logout();
    navigate('/login');
  };

  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#f0fdf4' }}>
      <header style={{
        background: 'linear-gradient(135deg, #16a34a 0%, #14532d 100%)',
        color: 'white', padding: '16px 40px',
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
          <div>
            <h1 style={{ margin: 0, fontSize: '20px' }}>Portal del Docente</h1>
            <p style={{ margin: '2px 0 0', opacity: 0.85, fontSize: '13px' }}>
              {user?.nombre} {user?.apellido} - {user?.email}
            </p>
          </div>
          <button onClick={handleLogout} style={{
            padding: '8px 18px', background: 'rgba(255,255,255,0.15)',
            color: 'white', border: '1px solid rgba(255,255,255,0.35)',
            borderRadius: '8px', cursor: 'pointer', fontSize: '14px',
          }}>
            Cerrar Sesion
          </button>
        </div>
        <div style={{ display: 'flex', gap: '6px' }}>
          <Tab icon="🏠" label="Inicio"         active={vista === 'inicio'} onClick={() => setVista('inicio')} />
          <Tab icon="✏️" label="Grilla de Notas" active={vista === 'notas'}  onClick={() => setVista('notas')} />
        </div>
      </header>

      <main style={{ maxWidth: '1100px', margin: '0 auto', padding: '32px 20px' }}>

        {/* VISTA INICIO */}
        {vista === 'inicio' && (
          <>
            <div style={{
              background: 'white', borderRadius: '16px', padding: '24px 28px',
              boxShadow: '0 2px 10px rgba(0,0,0,0.06)', marginBottom: '24px',
              display: 'flex', alignItems: 'center', gap: '20px',
            }}>
              <div style={{
                width: '64px', height: '64px', borderRadius: '50%',
                background: 'linear-gradient(135deg, #16a34a, #14532d)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                color: 'white', fontSize: '24px', fontWeight: 700, flexShrink: 0,
              }}>
                {user?.nombre?.charAt(0)}{user?.apellido?.charAt(0)}
              </div>
              <div>
                <h2 style={{ margin: 0, color: '#1e293b' }}>Bienvenido, Prof. {user?.nombre}!</h2>
                <p style={{ margin: '4px 0 0', color: '#64748b' }}>{user?.email}</p>
                <span style={{
                  display: 'inline-block', marginTop: '8px', padding: '4px 12px',
                  background: '#dcfce7', color: '#15803d', borderRadius: '20px',
                  fontSize: '12px', fontWeight: 600,
                }}>Docente</span>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px' }}>
              {[
                {
                  icon: '✏️', title: 'Grilla de Notas', color: '#dcfce7',
                  desc: 'Ingresa y gestionas las calificaciones de tus alumnos',
                  action: () => setVista('notas'),
                  btn: 'Abrir Grilla',
                  btnColor: '#15803d',
                },
              ].map(card => (
                <div key={card.title} style={{
                  background: 'white', borderRadius: '14px', padding: '24px',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.05)', border: '1px solid #f1f5f9',
                  display: 'flex', flexDirection: 'column',
                }}>
                  <div style={{
                    width: '52px', height: '52px', borderRadius: '12px',
                    background: card.color, display: 'flex', alignItems: 'center',
                    justifyContent: 'center', fontSize: '26px', marginBottom: '14px',
                  }}>
                    {card.icon}
                  </div>
                  <h3 style={{ margin: '0 0 6px', color: '#1e293b' }}>{card.title}</h3>
                  <p style={{ margin: '0 0 18px', color: '#64748b', fontSize: '14px', flexGrow: 1 }}>{card.desc}</p>
                  <button
                    onClick={card.action}
                    style={{
                      padding: '10px', background: card.btnColor, color: 'white',
                      border: 'none', borderRadius: '10px', cursor: 'pointer',
                      fontSize: '14px', fontWeight: 600,
                    }}
                  >
                    {card.btn}
                  </button>
                </div>
              ))}
            </div>
          </>
        )}

        {/* VISTA GRILLA DE NOTAS */}
        {vista === 'notas' && (
          <div style={{ background: 'white', borderRadius: '16px', padding: '28px', boxShadow: '0 2px 10px rgba(0,0,0,0.06)' }}>
            <GrillaNotas />
          </div>
        )}

      </main>
    </div>
  );
};

export default DashboardDocente;
