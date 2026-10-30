type CacheEntry<T> = {
  value: T;
  timestamp: number;
};

const cache = new Map<string, CacheEntry<unknown>>();

export function getCached<T>(key: string, maxAgeMs: number): T | null {
  const hit = cache.get(key);
  if (!hit) return null;
  if (Date.now() - hit.timestamp > maxAgeMs) return null;
  return hit.value as T;
}

export function setCached<T>(key: string, value: T): void {
  cache.set(key, { value, timestamp: Date.now() });
}

export function invalidateCached(keyPrefix: string): void {
  for (const key of cache.keys()) {
    if (key.startsWith(keyPrefix)) cache.delete(key);
  }
}
