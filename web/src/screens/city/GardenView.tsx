import { useRef, useEffect, useState, useCallback } from 'react';
import { useUser } from '../../hooks/useUser';
import { api } from '../../api/endpoints';
import type { CityState } from '../../api/types';
import styles from './GardenView.module.css';

interface GardenState {
  money: number;
  score: number;
  water: number;
  fertilizer: number;
}

export function GardenView() {
  const iframeRef = useRef<HTMLIFrameElement>(null);
  const { user } = useUser();
  const [gameState, setGameState] = useState<GardenState | null>(null);
  const [loaded, setLoaded] = useState(false);
  const syncedRef = useRef(false);

  const sendToGame = useCallback((type: string, amount?: number) => {
    iframeRef.current?.contentWindow?.postMessage(
      { source: 'lifepath', type, amount },
      '*'
    );
  }, []);

  useEffect(() => {
    function handleMessage(e: MessageEvent) {
      if (!e.data || e.data.source !== 'garden') return;

      if (e.data.type === 'GAME_STATE') {
        setGameState({
          money: e.data.money,
          score: e.data.score,
          water: e.data.water,
          fertilizer: e.data.fertilizer,
        });

        if (!syncedRef.current) {
          syncedRef.current = true;
          api.city.getState().then((state: CityState) => {
            sendToGame('SYNC_COINS', state.coins);
          }).catch(() => {});
        }
      }

      if (e.data.type === 'MONEY_CHANGED') {
        const newAmount = e.data.newAmount as number;
        api.city.syncCoins(Math.max(0, newAmount)).catch(() => {});
      }
    }
    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, [sendToGame]);

  const handleIframeLoad = useCallback(() => {
    setLoaded(true);
  }, []);

  void gameState;

  return (
    <div className={styles.wrapper}>
      {!loaded && (
        <div className={styles.loading}>
          <div className={styles.spinner} />
          <p>Loading your garden...</p>
        </div>
      )}
      <iframe
        ref={iframeRef}
        src="/garden/garden.html"
        className={styles.iframe}
        title="Grow Your Garden"
        onLoad={handleIframeLoad}
        allow="autoplay"
        sandbox="allow-scripts allow-same-origin allow-popups"
      />
      {user && (
        <div className={styles.userBadge}>
          {user.display_name || 'Gardener'}
        </div>
      )}
    </div>
  );
}
