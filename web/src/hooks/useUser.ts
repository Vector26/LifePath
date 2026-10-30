import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/endpoints';
import type { User } from '../api/types';
import { getCached, setCached } from '../utils/swrCache';

export function useUser() {
  const [user, setUser] = useState<User | null>(null);

  const refetch = useCallback(async () => {
    const cached = getCached<User | null>('user:me', 60_000);
    if (cached !== null) setUser(cached);
    try {
      const data = await api.users.me();
      setUser(data);
      setCached('user:me', data);
    } catch {
      if (cached === null) setUser(null);
    }
  }, []);

  useEffect(() => {
    refetch();
  }, [refetch]);

  return { user, refetch };
}
