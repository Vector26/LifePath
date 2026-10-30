import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { HabitCard } from './HabitCard';
import { SkeletonListItems } from '../../components/feedback/SkeletonCard';
import { useToast } from '../../context/ToastContext';
import { api } from '../../api/endpoints';
import { notifyWidgetRefresh } from '../../utils/androidApp';
import { getCached, setCached } from '../../utils/swrCache';
import type { Habit } from '../../api/types';
import styles from './HabitList.module.css';

export function HabitList() {
  const [habits, setHabits] = useState<Habit[]>([]);
  const [streaks, setStreaks] = useState<Record<string, { current: number; longest: number; lastCompletedDate?: string | null }>>({});
  const [loading, setLoading] = useState(true);
  const [completingId, setCompletingId] = useState<string | null>(null);
  const [completedTodayIds, setCompletedTodayIds] = useState<Record<string, boolean>>({});
  const { show: showToast } = useToast();

  const loadHabits = useCallback(async () => {
    const cached = getCached<{
      habits: Habit[];
      streaks: Record<string, { current: number; longest: number; lastCompletedDate?: string | null }>;
      completedTodayIds: Record<string, boolean>;
    }>('habits:list+streaks', 30_000);
    if (cached) {
      setHabits(cached.habits);
      setStreaks(cached.streaks);
      setCompletedTodayIds(cached.completedTodayIds);
    }
    const todayKey = new Date().toLocaleDateString('en-CA');
    const [habitsData, streaksData] = await Promise.all([
      api.habits.list(),
      api.streaks.list().catch(() => []),
    ]);
    setHabits(Array.isArray(habitsData) ? habitsData : []);
    const map: Record<string, { current: number; longest: number; lastCompletedDate?: string | null }> = {};
    const completedMap: Record<string, boolean> = {};
    const arr = Array.isArray(streaksData) ? streaksData : [];
    for (const s of arr) {
      const row = s as {
        habit_id?: string;
        habitId?: string;
        current_streak_days?: number;
        longest_streak_days?: number;
        last_completed_date?: string | null;
      };
      const id = row.habit_id ?? row.habitId;
      if (id) {
        map[id] = {
          current: row.current_streak_days ?? 0,
          longest: row.longest_streak_days ?? 0,
          lastCompletedDate: row.last_completed_date ?? null,
        };
        if ((row.last_completed_date ?? '').slice(0, 10) === todayKey) {
          completedMap[id] = true;
        }
      }
    }
    setStreaks(map);
    setCompletedTodayIds(completedMap);
    setCached('habits:list+streaks', {
      habits: Array.isArray(habitsData) ? habitsData : [],
      streaks: map,
      completedTodayIds: completedMap,
    });
  }, []);

  useEffect(() => {
    loadHabits().finally(() => setLoading(false));
  }, [loadHabits]);

  const handleComplete = useCallback(async (id: string) => {
    if (completedTodayIds[id]) return;
    setCompletingId(id);
    try {
      const res = await api.habits.complete(id);
      if (res.currency_earned > 0) {
        showToast(`+${res.currency_earned} currency`, 'success', 2000);
      } else {
        showToast('Already completed for today', 'info', 1800);
      }
      setCompletedTodayIds((prev) => ({ ...prev, [id]: true }));
      loadHabits();
      notifyWidgetRefresh();
    } catch {
      showToast('Failed to complete', 'error');
    } finally {
      setCompletingId(null);
    }
  }, [completedTodayIds, loadHabits, showToast]);

  const hasStreaks = Object.keys(streaks).length > 0;

  const handleRefresh = () => {
    setLoading(true);
    loadHabits().finally(() => setLoading(false));
  };

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <h1>Habits</h1>
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
          <Link to="/habits/new" className={styles.addBtn}>
            Add Habit
          </Link>
        </div>
      </header>
      {hasStreaks && (
        <div className={styles.streaksSection}>
          <h3 className={styles.streaksHeader}>Streaks</h3>
          <div className={styles.streaksList}>
            {habits.filter((h) => streaks[h.id]).map((h) => (
              <div key={h.id} className={styles.streakItem}>
                <span>{h.title}</span>
                <span>{streaks[h.id].current} / {streaks[h.id].longest} days</span>
              </div>
            ))}
          </div>
        </div>
      )}
      <div className={styles.list}>
        {loading ? (
          <SkeletonListItems count={5} />
        ) : habits.length === 0 ? (
          <div className={styles.emptyState}>
            <p>No habits. Add your first habit.</p>
            <Link to="/habits/new">
              <button type="button" className={styles.emptyCta}>
                Add Habit
              </button>
            </Link>
          </div>
        ) : (
          habits.map((h) => (
            <HabitCard
              key={h.id}
              habit={h}
              streak={streaks[h.id]}
              onComplete={handleComplete}
              completeLoading={completingId === h.id}
              completedToday={!!completedTodayIds[h.id]}
            />
          ))
        )}
      </div>
    </div>
  );
}
