import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/endpoints';
import type { TodayStatsResponse } from '../api/types';
import { getCached, setCached } from '../utils/swrCache';

export function useStatsToday() {
  const [data, setData] = useState<TodayStatsResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const refetch = useCallback(async () => {
    const cached = getCached<TodayStatsResponse | null>('stats:today', 30_000);
    if (cached !== null) {
      setData(cached);
      setLoading(false);
    }
    try {
      const result = await api.stats.today();
      setData(result);
      setCached('stats:today', result);
    } catch {
      if (cached === null) setData({ quests_completed_today: 0, habits_completed_today: 0, date: '' });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refetch();
  }, [refetch]);

  return { stats: data, loading, refetch };
}
