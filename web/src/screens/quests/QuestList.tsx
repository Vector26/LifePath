import { useCallback, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Chip } from '../../components/ui';
import { QuestCard } from './QuestCard';
import { SkeletonListItems } from '../../components/feedback/SkeletonCard';
import { useToast } from '../../context/ToastContext';
import { api } from '../../api/endpoints';
import type { Quest } from '../../api/types';
import { getCached, setCached } from '../../utils/swrCache';
import styles from './QuestList.module.css';

type Filter = 'all' | 'pending' | 'blocked' | 'completed';

export function QuestList() {
  const [searchParams, setSearchParams] = useSearchParams();
  const statusParam = searchParams.get('status') as Filter | null;
  const [filter, setFilter] = useState<Filter>(statusParam ?? 'pending');
  const [quests, setQuests] = useState<Quest[]>([]);
  const [loading, setLoading] = useState(true);
  const [unblockingId, setUnblockingId] = useState<string | null>(null);
  const { show: showToast } = useToast();

  const loadQuests = useCallback(() => {
    const cacheKey = `quests:list:${filter}`;
    const cached = getCached<Quest[]>(cacheKey, 30_000);
    if (cached) setQuests(cached);
    const status = filter === 'all' ? undefined : filter;
    return api.quests.list(status).then((data) => {
      const next = Array.isArray(data) ? data : [];
      setQuests(next);
      setCached(cacheKey, next);
    });
  }, [filter]);

  const handleUnblock = useCallback(async (id: string) => {
    setUnblockingId(id);
    try {
      await api.quests.unblock(id);
      showToast('Quest unblocked', 'success');
      loadQuests();
    } catch {
      showToast('Failed to unblock', 'error');
    } finally {
      setUnblockingId(null);
    }
  }, [loadQuests, showToast]);

  useEffect(() => {
    if (statusParam && ['all', 'pending', 'blocked', 'completed'].includes(statusParam)) {
      setFilter(statusParam);
    }
  }, [statusParam]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    loadQuests()
      .catch(() => { if (!cancelled) setQuests([]); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [loadQuests]);

  const setFilterAndUrl = (f: Filter) => {
    setFilter(f);
    if (f === 'all') setSearchParams({});
    else setSearchParams({ status: f });
  };

  const handleRefresh = () => {
    setLoading(true);
    loadQuests().finally(() => setLoading(false));
  };

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <h1>Quests</h1>
        <div className={styles.headerActions}>
          <button
            type="button"
            className={styles.refreshBtn}
            onClick={handleRefresh}
            disabled={loading}
            aria-label="Refresh"
          >
            ↻
          </button>
          <Link to="/quests/new" className={styles.addBtn}>
            Add Quest
          </Link>
        </div>
      </header>
      <div className={styles.filters}>
        {(['pending', 'all', 'blocked', 'completed'] as const).map((f) => (
          <Chip
            key={f}
            selected={filter === f}
            onClick={() => setFilterAndUrl(f)}
          >
            {f.charAt(0).toUpperCase() + f.slice(1)}
          </Chip>
        ))}
      </div>
      <div className={styles.list}>
        {loading ? (
          <SkeletonListItems count={5} />
        ) : quests.length === 0 ? (
          <div className={styles.emptyState}>
            <p>No quests yet. Add your first quest.</p>
            <Link to="/quests/new">
              <button type="button" className={styles.emptyCta}>
                Add Quest
              </button>
            </Link>
          </div>
        ) : (
          quests.map((q) => (
            <QuestCard
              key={q.id}
              quest={q}
              onUnblock={filter === 'blocked' ? handleUnblock : undefined}
              unblockLoading={unblockingId === q.id}
            />
          ))
        )}
      </div>
    </div>
  );
}
