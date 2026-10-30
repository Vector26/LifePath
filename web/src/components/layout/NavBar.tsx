import { NavLink } from 'react-router-dom';
import styles from './NavBar.module.css';

const bottomNavItems = [
  { to: '/quest', label: 'Quest', icon: '🎯' },
  { to: '/quests', label: 'Quests', icon: '📋' },
  { to: '/habits', label: 'Habits', icon: '✨' },
  { to: '/city', label: 'City', icon: '🏙️' },
  { to: '/settings', label: 'Settings', icon: '⚙️' },
];

const sidebarNavItems = [
  { to: '/quest', label: 'Quest', icon: '🎯' },
  { to: '/quests', label: 'Quests', icon: '📋' },
  { to: '/habits', label: 'Habits', icon: '✨' },
  { to: '/calendar', label: 'Calendar', icon: '📅' },
  { to: '/city', label: 'City', icon: '🏙️' },
  { to: '/settings', label: 'Settings', icon: '⚙️' },
];

type NavBarProps = {
  variant: 'bottom' | 'sidebar';
};

export function NavBar({ variant }: NavBarProps) {
  const isBottom = variant === 'bottom';
  const items = isBottom ? bottomNavItems : sidebarNavItems;

  return (
    <ul className={isBottom ? styles.bottomList : styles.sidebarList}>
      {items.map(({ to, label, icon }) => (
        <li key={to}>
          <NavLink
            to={to}
            className={({ isActive }) =>
              [styles.link, isActive ? styles.active : ''].filter(Boolean).join(' ')
            }
            end={to === '/quest'}
          >
            <span className={styles.icon}>{icon}</span>
            <span className={styles.label}>{label}</span>
          </NavLink>
        </li>
      ))}
    </ul>
  );
}
