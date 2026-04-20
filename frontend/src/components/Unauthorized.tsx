import React from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/auth.service';

const Unauthorized: React.FC = () => {
  const navigate = useNavigate();

  const handleBack = () => {
    const route = authService.getDefaultRoute();
    navigate(route);
  };

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center',
      justifyContent: 'center', background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    }}>
      <div style={{
        background: 'white', borderRadius: '20px', padding: '60px 40px',
        textAlign: 'center', maxWidth: '450px', width: '90%',
        boxShadow: '0 20px 60px rgba(0,0,0,0.2)'
      }}>
        <div style={{ fontSize: '80px', marginBottom: '20px' }}>🚫</div>
        <h1 style={{ margin: '0 0 12px', color: '#1e293b', fontSize: '28px' }}>
          Acceso Denegado
        </h1>
        <p style={{ color: '#64748b', marginBottom: '32px', fontSize: '16px' }}>
          No tenés permisos para acceder a esta sección.
        </p>
        <button
          onClick={handleBack}
          style={{
            padding: '14px 32px',
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            color: 'white', border: 'none', borderRadius: '10px',
            fontSize: '16px', fontWeight: 600, cursor: 'pointer',
            boxShadow: '0 4px 15px rgba(102,126,234,0.4)'
          }}
        >
          Volver al inicio
        </button>
      </div>
    </div>
  );
};

export default Unauthorized;
