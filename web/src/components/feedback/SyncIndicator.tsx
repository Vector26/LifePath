import { useEffect, useState } from 'react';
import styles from './SyncIndicator.module.css';

export function SyncIndicator() {
  const [online, setOnline] = useState(typeof navigator !== 'undefined' ? navigator.onLine : true);

  useEffect(() => {
    const handleOnline = () => setOnline(true);
    const handleOffline = () => setOnline(false);
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  return (
    <span
      className={`${styles.dot} ${online ? styles.online : styles.offline}`}
      title={online ? 'Online' : 'Offline — will sync when connected'}
      aria-label={online ? 'Online' : 'Offline'}
    />
  );
}
