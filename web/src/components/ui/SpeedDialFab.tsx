import { useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { createPortal } from 'react-dom';
import styles from './SpeedDialFab.module.css';

type Action = { to: string; label: string; icon: string };

const ACTIONS: Action[] = [
  { to: '/quests/new', label: 'New Quest', icon: '🎯' },
  { to: '/habits/new', label: 'New Habit', icon: '✨' },
  { to: '/calendar', label: 'Calendar', icon: '📅' },
];

export function SpeedDialFab() {
  const [open, setOpen] = useState(false);

  const handleBackdropClick = useCallback(() => setOpen(false), []);

  const content = (
    <div className={`${styles.wrapper} ${open ? styles.wrapperOpen : ''}`}>
      {open && <div className={styles.backdrop} onClick={handleBackdropClick} aria-hidden />}
      <div className={styles.actions}>
        {ACTIONS.map((action) => (
          <Link
            key={action.to}
            to={action.to}
            className={styles.action}
            aria-label={action.label}
            onClick={() => setOpen(false)}
          >
            <span className={styles.actionIcon}>{action.icon}</span>
          </Link>
        ))}
      </div>
      <button
        type="button"
        className={`${styles.fab} ${open ? styles.fabOpen : ''}`}
        onClick={() => setOpen((o) => !o)}
        aria-label={open ? 'Close menu' : 'Add'}
        aria-expanded={open}
      >
        <span className={styles.fabIcon}>{open ? '×' : '+'}</span>
      </button>
    </div>
  );

  return createPortal(content, document.body);
}
