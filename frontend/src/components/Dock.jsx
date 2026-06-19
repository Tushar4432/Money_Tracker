import { motion, useMotionValue, useSpring } from 'framer-motion';
import { useEffect, useRef, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './Dock.css';

/*
 * Left-side vertical dock with extendable labels.
 *
 * Active bar: rendered inside each item conditionally, uses layoutId so
 * framer-motion animates it smoothly between items on page change.
 * The bar is position:absolute inside the item, so it moves with the item
 * during hover magnification.
 *
 * Labels: rendered as siblings of items (not children), positioned absolute
 * to the panel so they extend to the right without clipping.
 */

const ICON_SIZE = 44;
const MAG_SIZE = 56;
const DISTANCE = 140;
const GAP = 8;
const PAD_TOP = 16;
const PAD_X = 10;
const PANEL_WIDTH = ICON_SIZE + PAD_X * 2;

const DOCK_ITEMS = [
  { label: 'Dashboard', icon: '◈', path: '/' },
  { label: 'Analytics', icon: '◉', path: '/analytics' },
  { label: 'Goals',    icon: '◎', path: '/goals' },
  { label: 'AI Coach', icon: '◬', path: '/coach' },
  { label: 'Profile',  icon: '◌', path: '/auth' },
];

// Pre-compute centre-y of each item (static baseline positions)
const OFFSETS = (() => {
  const arr = [];
  let y = PAD_TOP + ICON_SIZE / 2;
  for (let i = 0; i < DOCK_ITEMS.length; i++) {
    arr.push(y);
    y += ICON_SIZE + GAP;
  }
  return arr;
})();

const getActiveIndex = (pathname) => {
  if (pathname === '' || pathname === '/') return 0;
  const idx = DOCK_ITEMS.findIndex(item => item.path === pathname);
  return idx >= 0 ? idx : -1;
};

const SIZE_SPRING = { mass: 0.4, stiffness: 200, damping: 26 };

export default function Dock() {
  const navigate = useNavigate();
  const location = useLocation();
  const panelRef = useRef(null);
  const activeIdx = getActiveIndex(location.pathname);

  const mouseY = useMotionValue(Infinity);

  // Per-item springs for icon size magnification
  const rawSizes = useRef(DOCK_ITEMS.map(() => useMotionValue(ICON_SIZE)));
  const sizes = useRef(DOCK_ITEMS.map((_, i) => useSpring(rawSizes.current[i], SIZE_SPRING)));

  // On every mouseY change, update all item sizes
  useEffect(() => {
    const unsub = mouseY.on('change', (my) => {
      OFFSETS.forEach((cy, i) => {
        const dist = my === Infinity ? Infinity : Math.abs(my - cy);
        const t = Math.max(0, 1 - dist / DISTANCE);
        const targetSize = ICON_SIZE + (MAG_SIZE - ICON_SIZE) * t;
        rawSizes.current[i].set(targetSize);
      });
    });
    return unsub;
  }, [mouseY]);

  const handleMouseMove = useCallback((e) => {
    const rect = panelRef.current?.getBoundingClientRect();
    if (rect) mouseY.set(e.clientY - rect.top);
  }, [mouseY]);

  const handleMouseLeave = useCallback(() => {
    mouseY.set(Infinity);
  }, [mouseY]);

  return (
    <div className="dock-outer" ref={panelRef}>
      <div
        className="dock-panel"
        style={{
          paddingTop: PAD_TOP,
          paddingBottom: PAD_TOP,
          paddingLeft: PAD_X,
          paddingRight: PAD_X,
        }}
        onMouseMove={handleMouseMove}
        onMouseLeave={handleMouseLeave}
        role="toolbar"
        aria-label="Main navigation"
      >
        {DOCK_ITEMS.map((item, i) => {
          const isActive = i === activeIdx;

          return (
            <motion.div
              key={item.path}
              className={`dock-item ${isActive ? 'active' : ''}`}
              style={{
                width: PANEL_WIDTH,
                height: sizes.current[i],
              }}
              onClick={() => navigate(item.path)}
              tabIndex={0}
              role="button"
              aria-label={item.label}
              aria-current={isActive ? 'page' : undefined}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  navigate(item.path);
                }
              }}
            >
              <div className="dock-icon">
                <span>{item.icon}</span>
              </div>
            </motion.div>
          );
        })}

        {/* Labels — siblings of items, positioned absolute to panel */}
        {DOCK_ITEMS.map((item, i) => {
          const isActive = i === activeIdx;
          return (
            <motion.div
              key={`label-${item.path}`}
              className="dock-label"
              initial={false}
              animate={{
                opacity: isActive ? 1 : 0,
                x: isActive ? 0 : -8,
              }}
              transition={{ duration: 0.25, ease: [0.25, 0.46, 0.45, 0.94] }}
              style={{ top: OFFSETS[i] - 14 }}
            >
              <span className="dock-label-text">{item.label}</span>
            </motion.div>
          );
        })}
      </div>
    </div>
  );
}
