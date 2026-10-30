import { Outlet, useLocation } from 'react-router-dom';
import { NavBar } from './NavBar';
import { SpeedDialFab } from '../ui/SpeedDialFab';
import styles from './AppShell.module.css';

const FAB_ROUTES = ['/quest', '/quests', '/habits', '/calendar'];

export function AppShell() {
  const location = useLocation();
  const path = location.pathname;
  const showFab = FAB_ROUTES.includes(path);

  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar}>
        <NavBar variant="sidebar" />
      </aside>
      <main className={styles.main}>
        <Outlet />
      </main>
      <nav className={styles.bottomNav}>
        <NavBar variant="bottom" />
      </nav>
      {showFab && <SpeedDialFab />}
    </div>
  );
}
