import { invalidateCached } from '../utils/swrCache';

const BASE = '/api/v1';
const DEFAULT_TIMEOUT = 30000;
const LOGOUT_FLAG_KEY = 'lifepath:logout';

function getToken(): string | null {
  return localStorage.getItem('access_token');
}

function getRefreshToken(): string | null {
  return localStorage.getItem('refresh_token');
}

function setTokens(access: string, refresh: string) {
  logoutDispatched = false;
  localStorage.setItem('access_token', access);
  localStorage.setItem('refresh_token', refresh);
}

function setLogoutInProgress() {
  try {
    sessionStorage.setItem(LOGOUT_FLAG_KEY, '1');
  } catch { /* non-fatal */ }
}

function clearTokens() {
  setLogoutInProgress();
  localStorage.removeItem('access_token');
  localStorage.removeItem('refresh_token');
  try {
    invalidateCached('');
  } catch { /* non-fatal */ }
}

export interface ApiError {
  status: number;
  message: string;
  code?: string;
  body?: unknown;
}

type RefreshResult = 'refreshed' | 'invalid' | 'transient';
const REFRESH_RETRY_DELAYS_MS = [500, 1000] as const;

let refreshPromise: Promise<RefreshResult> | null = null;
let logoutDispatched = false;

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

/** Get tokens from native (Android) if available. Used when web's refresh fails with 4xx
 * but native may have already refreshed (e.g. widget worker) - avoids unnecessary logout. */
function getNativeTokens(): { access_token: string; refresh_token?: string } | null {
  try {
    const w = window as { LifePathAndroid?: { getStoredTokens?: () => string } };
    const raw = w.LifePathAndroid?.getStoredTokens?.();
    if (!raw) return null;
    const parsed = JSON.parse(raw) as { access_token?: string; refresh_token?: string };
    if (!parsed.access_token?.trim()) return null;
    return {
      access_token: parsed.access_token,
      refresh_token: parsed.refresh_token?.trim() || undefined,
    };
  } catch {
    return null;
  }
}

async function refreshAuth(): Promise<RefreshResult> {
  const refresh = getRefreshToken();
  if (!refresh) return 'invalid';
  if (refreshPromise) return refreshPromise;
  const promise = (async (): Promise<RefreshResult> => {
    let result: RefreshResult = 'transient';
    const attempts = REFRESH_RETRY_DELAYS_MS.length + 1;
    for (let attempt = 0; attempt < attempts; attempt += 1) {
      try {
        const res = await fetch(`${BASE}/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken: refresh }),
        });
        const bodyText = await res.text();
        if (res.status === 401) return 'invalid';
        if (res.status >= 400 && res.status < 500) {
          const native = getNativeTokens();
          if (native?.access_token && native.access_token !== getToken()) {
            setTokens(native.access_token, native.refresh_token ?? refresh);
            return 'refreshed';
          }
          return 'invalid';
        }
        if (!res.ok) {
          result = 'transient';
        } else {
          const data = JSON.parse(bodyText || '{}') as { access_token?: string; refresh_token?: string };
          if (!data.access_token) {
            result = 'transient';
          } else {
            setTokens(data.access_token, data.refresh_token ?? refresh);
            return 'refreshed';
          }
        }
      } catch {
        result = 'transient';
      }

      if (attempt < REFRESH_RETRY_DELAYS_MS.length) {
        await sleep(REFRESH_RETRY_DELAYS_MS[attempt]);
      }
    }
    return result;
  })().finally(() => {
    refreshPromise = null;
  });
  refreshPromise = promise;
  return promise;
}

async function fetchWithAuth(
  url: string,
  init: RequestInit & { timeout?: number } = {},
  retryOn401 = true
): Promise<Response> {
  const { timeout = DEFAULT_TIMEOUT, ...rest } = init;
  const controller = new AbortController();
  const id = setTimeout(() => controller.abort(), timeout);

  const headers = new Headers(rest.headers);
  const token = getToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (!headers.has('Content-Type')) headers.set('Content-Type', 'application/json');

  let res: Response;
  try {
    res = await fetch(url, { ...rest, headers, signal: controller.signal });
  } catch (e) {
    clearTimeout(id);
    if (e instanceof Error && e.name === 'AbortError') {
      throw { status: 408, message: 'Request timed out' } as ApiError;
    }
    throw e;
  }
  clearTimeout(id);

  if (res.status === 401 && retryOn401 && getToken()) {
    const refreshResult = await refreshAuth();
    if (refreshResult === 'refreshed') return fetchWithAuth(url, { ...init, timeout }, false);
    if (refreshResult === 'invalid') {
      clearTokens();
      if (!logoutDispatched) {
        logoutDispatched = true;
        window.dispatchEvent(new CustomEvent('auth:logout', { detail: { manual: false } }));
      }
      throw { status: 401, message: 'Session expired' } as ApiError;
    }
    try {
      invalidateCached('');
    } catch { /* non-fatal */ }
    throw { status: 503, message: 'Unable to refresh session. Please retry.' } as ApiError;
  }

  return res;
}

export async function apiRequest<T>(
  path: string,
  options: {
    method?: string;
    body?: unknown;
    headers?: Record<string, string>;
    timeout?: number;
    idempotencyKey?: string;
  } = {}
): Promise<T> {
  const { method = 'GET', body, headers = {}, timeout = DEFAULT_TIMEOUT, idempotencyKey } = options;
  const url = path.startsWith('http') ? path : `${BASE}${path.startsWith('/') ? '' : '/'}${path}`;

  const init: RequestInit & { timeout?: number } = {
    method,
    headers: { ...headers },
    timeout,
  };
  if (body !== undefined) init.body = JSON.stringify(body);
  if (idempotencyKey) (init.headers as Headers).set('Idempotency-Key', idempotencyKey);

  const res = await fetchWithAuth(url, init);

  const text = await res.text();
  if (res.status === 204 || !text) return undefined as T;
  if (!res.ok) {
    let message = 'Request failed';
    let code: string | undefined;
    try {
      const data = JSON.parse(text) as {
        message?: string;
        error?: string | { code?: string; message?: string };
        code?: string;
      };
      const errObj = typeof data.error === 'object' ? data.error : null;
      message = errObj?.message ?? data.message ?? (typeof data.error === 'string' ? data.error : message);
      code = errObj?.code ?? data.code;
    } catch {
      if (text) message = text;
    }
    const err: ApiError = { status: res.status, message, code, body: text };
    throw err;
  }

  if (!text) return undefined as T;
  return JSON.parse(text) as T;
}

export function redirectToLogin() {
  window.location.href = '/login';
}

export function resetLogoutState() {
  logoutDispatched = false;
}
