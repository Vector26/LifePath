import { createPortal } from 'react-dom';
import { Link } from 'react-router-dom';
import styles from './Fab.module.css';

type FabProps = {
  to: string;
  label: string;
  children: React.ReactNode;
};

export function Fab({ to, label, children }: FabProps) {
  const fab = (
    <Link to={to} className={styles.fab} aria-label={label}>
      {children}
    </Link>
  );
  return createPortal(fab, document.body);
}
