import React from 'react';
import { useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { AIChatProvider } from './context/AIChatContext';
import AppRoutes from './routes';
import Dock from './components/Dock';
import './App.css';

function AppContent() {
  const location = useLocation();
  const isAuthPage = location.pathname === '/auth';
  const { user } = useAuth();

  return (
    <AIChatProvider userId={user?.userId || 'dev-user'}>
      <div className={isAuthPage ? '' : 'with-dock'}>
        <AppRoutes />
      </div>
      {!isAuthPage && <Dock />}
    </AIChatProvider>
  );
}

function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}

export default App;
