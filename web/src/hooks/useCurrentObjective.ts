import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/endpoints';
import type { CurrentObjectiveResponse } from '../api/types';
import { getCached, setCached } from '../utils/swrCache';

type Context = 'focus' | 'quick' | 'low_energy';

export function useCurrentObjective(context: Context = 'focus') {
  const [data, setData] = useState<CurrentObjectiveResponse | null | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const cacheKey = `objective:${context}`;

  const refetch = useCallback(async () => {
    const cached = getCached<CurrentObjectiveResponse | null>(cacheKey, 30_000);
    if (cached !== null) {
      setData(cached);
      setLoading(false);
    } else {
      setLoading(true);
    }
    setError(null);
    try {
      const result = await api.objectives.getCurrent(context);
      const next = result ?? null;
      setData(next);
      setCached(cacheKey, next);
    } catch (e) {
      setError((e as { message?: string })?.message ?? 'Failed to load objective');
      if (cached === null) setData(null);
    } finally {
      setLoading(false);
    }
  }, [cacheKey, context]);

  useEffect(() => {
    refetch();
  }, [refetch]);

  useEffect(() => {
    const handleVisibility = () => {
      if (document.visibilityState === 'visible') refetch();
    };
    document.addEventListener('visibilitychange', handleVisibility);
    return () => document.removeEventListener('visibilitychange', handleVisibility);
  }, [refetch]);

  return {
    objective: data ?? null,
    loading,
    error,
    refetch,
    setObjective: setData,
  };
}
