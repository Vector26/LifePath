import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Button, Input } from '../../components/ui';
import { useToast } from '../../context/ToastContext';
import { api } from '../../api/endpoints';
import type { User, CalendarStatus } from '../../api/types';
import { getCached, setCached } from '../../utils/swrCache';
import styles from './Settings.module.css';

const TIMEZONES = [
  'Asia/Kolkata',
  'Asia/Tokyo',
  'Asia/Shanghai',
  'Asia/Dubai',
  'Asia/Singapore',
  'America/New_York',
  'America/Chicago',
  'America/Denver',
  'America/Los_Angeles',
  'America/Sao_Paulo',
  'Europe/London',
  'Europe/Paris',
  'Europe/Berlin',
  'Europe/Moscow',
  'Australia/Sydney',
  'Pacific/Auckland',
  'UTC',
];

export function Settings() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { show: showToast } = useToast();
  const [user, setUser] = useState<User | null>(null);
  const [editingName, setEditingName] = useState(false);
  const [displayName, setDisplayName] = useState('');
  const [nameLoading, setNameLoading] = useState(false);
  const [calendarStatus, setCalendarStatus] = useState<CalendarStatus | null>(null);
  const [calendarLoading, setCalendarLoading] = useState(false);
  const [calendarError, setCalendarError] = useState<string | null>(null);
  const [timezoneLoading, setTimezoneLoading] = useState(false);
  const [nativeAppVersion, setNativeAppVersion] = useState<string | null>(null);

  const load = useCallback(async () => {
    const cached = getCached<{ user: User | null; calendarStatus: CalendarStatus | null }>('settings:bootstrap', 60_000);
    if (cached) {
      setUser(cached.user);
      setDisplayName(cached.user?.display_name ?? '');
      setCalendarStatus(cached.calendarStatus);
    }
    try {
      const userData = await api.users.me();
      setUser(userData);
      setDisplayName(userData.display_name ?? '');
      const nextStatus = await api.calendar.getStatus();
      setCalendarStatus(nextStatus);
      setCalendarError(null);
      setCached('settings:bootstrap', { user: userData, calendarStatus: nextStatus });
    } catch {
      if (!cached) {
        setUser(null);
        setCalendarStatus(null);
      }
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (searchParams.get('calendar') === 'connected') {
      showToast('Google Calendar connected!', 'success');
      setSearchParams({}, { replace: true });
    }
  }, [searchParams, setSearchParams, showToast]);

  useEffect(() => {
    const bridge = (window as unknown as { LifePathAndroid?: { getAppVersion?: () => string } }).LifePathAndroid;
    if (!bridge?.getAppVersion) return;
    try {
      const v = bridge.getAppVersion();
      if (v && typeof v === 'string') setNativeAppVersion(v);
    } catch {
      setNativeAppVersion(null);
    }
  }, []);

  const handleSaveName = async () => {
    setNameLoading(true);
    try {
      const updated = await api.users.updateMe({ displayName: displayName.trim() || undefined });
      setUser(updated);
      setEditingName(false);
      showToast('Name updated', 'success');
    } catch {
      showToast('Failed to update name', 'error');
    } finally {
      setNameLoading(false);
    }
  };

  const handleTimezoneChange = async (tz: string) => {
    setTimezoneLoading(true);
    try {
      const updated = await api.users.updateMe({ timezone: tz });
      setUser(updated);
      showToast(`Timezone set to ${tz}`, 'success');
    } catch {
      showToast('Failed to update timezone', 'error');
    } finally {
      setTimezoneLoading(false);
    }
  };

  const handleConnectCalendar = async () => {
    setCalendarLoading(true);
    setCalendarError(null);
    try {
      const { url } = await api.calendar.getAuthUrl();
      if (!url || typeof url !== 'string') {
        throw new Error('No redirect URL returned');
      }
      window.location.href = url;
    } catch (err) {
      const e = err as { message?: string; code?: string };
      const msg = e?.message ?? 'Failed to connect';
      setCalendarError(
        msg.includes('configured') || e?.code === 'CONFIGURATION_ERROR'
          ? 'Google Calendar is not configured. Set GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, and GOOGLE_REDIRECT_URI in the backend.'
          : `Could not connect: ${msg}`
      );
      showToast('Google Calendar connection failed', 'error');
    } finally {
      setCalendarLoading(false);
    }
  };

  const handleDisconnectCalendar = async () => {
    setCalendarLoading(true);
    try {
      await api.calendar.disconnect();
      setCalendarStatus((prev) => prev ? { ...prev, connected: false } : null);
      showToast('Calendar disconnected', 'success');
    } catch {
      showToast('Failed to disconnect', 'error');
    } finally {
      setCalendarLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      await api.auth.logout();
    } catch {
      /* ignore */
    } finally {
      try {
        sessionStorage.setItem('lifepath:logout', '1');
      } catch { /* non-fatal */ }
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      try {
        const { invalidateCached } = await import('../../utils/swrCache');
        invalidateCached('');
      } catch { /* non-fatal */ }
      window.dispatchEvent(new CustomEvent('auth:logout', { detail: { manual: true } }));
      navigate('/login');
    }
  };

  const isAndroid = /android/i.test(navigator.userAgent);
  const isInApp = typeof (window as unknown as Record<string, unknown>).LifePathAndroid !== 'undefined';
  const showApkBanner = !isInApp;

  return (
    <div className={styles.container}>
      {showApkBanner && (
        <div className={styles.apkBanner}>
          <div className={styles.apkBannerContent}>
            <div>
              <span className={styles.apkBannerTitle}>Get the Android app</span>
              <span className={styles.apkBannerSubtitle}>
                {isAndroid ? 'Widgets, notifications & more' : 'Home-screen widgets & notifications'}
              </span>
            </div>
            <a href="/lifepath.apk" download="LifePath.apk" className={styles.apkBannerBtn}>
              Download APK
            </a>
          </div>
        </div>
      )}

      <div className={styles.group}>
        <h3 className={styles.groupHeader}>Profile</h3>
        <div className={styles.card}>
          <div className={styles.row}>
            <span>Display name</span>
            {editingName ? (
              <div className={styles.editRow}>
                <Input
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  placeholder="Your name"
                  disabled={nameLoading}
                />
                <Button size="sm" onClick={handleSaveName} disabled={nameLoading}>
                  Save
                </Button>
                <Button size="sm" variant="tertiary" onClick={() => { setEditingName(false); setDisplayName(user?.display_name ?? ''); }}>
                  Cancel
                </Button>
              </div>
            ) : (
              <button
                type="button"
                className={styles.editableValue}
                onClick={() => setEditingName(true)}
              >
                {user?.display_name || '—'} <span className={styles.editHint}>Edit</span>
              </button>
            )}
          </div>
          <div className={styles.row}>
            <span>Email</span>
            <span className={styles.muted}>{user?.email ?? '—'}</span>
          </div>
          <div className={styles.row}>
            <span>Timezone</span>
            <select
              className={styles.tzSelect}
              value={user?.timezone ?? ''}
              onChange={(e) => handleTimezoneChange(e.target.value)}
              disabled={timezoneLoading}
            >
              {(user?.timezone && !TIMEZONES.includes(user.timezone)
                ? [user.timezone, ...TIMEZONES]
                : TIMEZONES
              ).map((tz) => (
                <option key={tz} value={tz}>{tz.replace(/_/g, ' ')}</option>
              ))}
            </select>
          </div>
          <div className={styles.rowHint}>
            Your timezone affects which calendar events count as &quot;today&quot;.
          </div>
        </div>
      </div>

      <div className={styles.group} id="integrations">
        <h3 className={styles.groupHeader}>Integrations</h3>
        <div className={styles.card}>
          <div className={styles.row}>
            <div>
              <span>Google Calendar</span>
              {calendarStatus?.connected && (
                <span className={styles.connectedBadge}>Connected</span>
              )}
            </div>
            {calendarStatus?.connected ? (
              <Button size="sm" variant="destructive" onClick={handleDisconnectCalendar} disabled={calendarLoading}>
                Disconnect
              </Button>
            ) : (
              <Button size="sm" onClick={handleConnectCalendar} disabled={calendarLoading}>
                Connect Google
              </Button>
            )}
          </div>
          <div className={styles.rowHint}>
            {calendarStatus?.configured
              ? 'Connect your Google account to add today\'s calendar events to your objective queue.'
              : 'To enable Google Calendar, set GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, and GOOGLE_REDIRECT_URI. Click Connect to try.'}
          </div>
          {calendarError && (
            <div className={styles.calendarError}>
              {calendarError}
            </div>
          )}
        </div>
      </div>

      <div className={styles.group}>
        <h3 className={styles.groupHeader}>Progress</h3>
        <div className={styles.card}>
          <div className={styles.row}>
            <span>Level</span>
            <span className={styles.muted}>{user?.level ?? 1}</span>
          </div>
          <div className={styles.row}>
            <span>XP (from quests)</span>
            <span className={styles.muted}>{user?.total_xp ?? 0}</span>
          </div>
          <div className={styles.rowHint}>
            XP grows when you complete or manage quests. Currency grows from habits and funds future features.
          </div>
        </div>
      </div>

      <div className={styles.group}>
        <h3 className={styles.groupHeader}>Account</h3>
        <div className={styles.card}>
          <button
            type="button"
            className={styles.logoutBtn}
            onClick={handleLogout}
          >
            Log out
          </button>
        </div>
      </div>

      <div className={styles.group} id="app-url">
        <h3 className={styles.groupHeader}>App URL</h3>
        <div className={styles.card}>
          <div className={styles.row}>
            <span className={styles.urlValue}>{typeof window !== 'undefined' ? window.location.origin : ''}</span>
            <Button
              size="sm"
              variant="tertiary"
              onClick={() => {
                if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
                  navigator.clipboard.writeText(window.location.origin);
                  showToast('URL copied to clipboard', 'success');
                }
              }}
            >
              Copy
            </Button>
          </div>
          <div className={styles.rowHint}>
            Use this link to access the app. When using Cloudflare Tunnel, this updates automatically.
          </div>
        </div>
      </div>

      <div className={styles.group}>
        <h3 className={styles.groupHeader}>Version</h3>
        <div className={styles.card}>
          <div className={styles.row}>
            <span>Web UI</span>
            <span className={styles.muted}>
              v{typeof __APP_VERSION__ !== 'undefined' ? __APP_VERSION__ : '?.?.?'}
            </span>
          </div>
          {nativeAppVersion && (
            <div className={styles.row}>
              <span>Android app</span>
              <span className={styles.muted}>v{nativeAppVersion}</span>
            </div>
          )}
          <div className={styles.rowHint}>
            {typeof __BUILD_TIME__ !== 'undefined' && __BUILD_TIME__
              ? `Build: ${__BUILD_TIME__}`
              : 'Build time unavailable'}
          </div>
        </div>
      </div>

    </div>
  );
}
