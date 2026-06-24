import { useNavigate, useLocation } from 'react-router-dom';
import './Dock.css';

const DOCK_ITEMS = [
  { label: 'DASH',   icon: '▪', path: '/' },
  { label: 'ANALYT', icon: '▤', path: '/analytics' },
  { label: 'GOALS',  icon: '▣', path: '/goals' },
  { label: 'COACH',  icon: '▸', path: '/coach' },
  { label: 'AUTH',   icon: '◂', path: '/auth' },
];

export default function Dock() {
  const navigate = useNavigate();
  const location = useLocation();

  const isActive = (path) => {
    if (path === '/') return location.pathname === '/';
    return location.pathname === path;
  };

  return (
    <div className="dock-outer">
      <div className="dock-panel" role="toolbar" aria-label="Main navigation">
        {DOCK_ITEMS.map((item) => (
          <div
            key={item.path}
            className={`dock-item ${isActive(item.path) ? 'active' : ''}`}
            onClick={() => navigate(item.path)}
            tabIndex={0}
            role="button"
            aria-label={item.label}
            aria-current={isActive(item.path) ? 'page' : undefined}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                navigate(item.path);
              }
            }}
          >
            <div className="dock-icon">{item.icon}</div>
            <div className="dock-label">{item.label}</div>
          </div>
        ))}

        <div className="dock-bottom">
          <div className="dock-status-dot" title="System online" />
        </div>
      </div>
    </div>
  );
}
