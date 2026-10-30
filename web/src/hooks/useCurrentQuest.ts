import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/endpoints';
import type { Quest } from '../api/types';
import type { CurrentQuestResponse } from '../api/types';

type Context = 'focus' | 'quick' | 'low_energy';

export function useCurrentQuest(context: Context = 'focus') {
  const [data, setData] = useState<CurrentQuestResponse | null | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refetch = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await api.quests.getCurrent(context);
      setData(result ?? null);
    } catch (e) {
      setError((e as { message?: string })?.message ?? 'Failed to load quest');
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [context]);

  useEffect(() => {
    refetch();
  }, [refetch]);

  const setCurrentQuest = useCallback((quest: Quest | null) => {
    setData((prev) =>
      quest ? { quest, reasons: prev?.reasons ?? [] } : null
    );
  }, []);

  return {
    quest: data?.quest ?? null,
    reasons: data?.reasons ?? [],
    loading,
    error,
    refetch,
    setCurrentQuest,
  };
}
