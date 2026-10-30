import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/endpoints';
import type { ApiError } from '../../api/client';
import type { CalendarEvent, CalendarStatus } from '../../api/types';
import { getCached, setCached } from '../../utils/swrCache';
import styles from './CalendarToday.module.css';

function formatTime(iso: string): string {
  try {
    return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  } catch {
    return '';
  }
}

export function CalendarToday() {
  const [status, setStatus] = useState<CalendarStatus | null>(null);
  const [events, setEvents] = useState<CalendarEvent[]>([]);
  const [loading, setLoading] = useState(true);

  const [apiDisabledError, setApiDisabledError] = useState<{ message: string; url?: string } | null>(null);

  const load = useCallback(async (sync = false) => {
    setApiDisabledError(null);
    const cached = getCached<{ status: CalendarStatus | null; events: CalendarEvent[] }>('calendar:today', 30_000);
    if (cached) {
      setStatus(cached.status);
      setEvents(cached.events);
      setLoading(false);
    }
    try {
      const s = await api.calendar.getStatus();
      setStatus(s);
      // #region agent log
      fetch('http://127.0.0.1:7242/ingest/aa14b1ad-4efc-4b5c-bff5-8a2d20e47acb',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'CalendarToday.tsx:getStatus',message:'getStatus succeeded',data:{connected:s?.connected,configured:s?.configured},timestamp:Date.now(),hypothesisId:'H1'})}).catch(()=>{});
      // #endregion
      if (!s?.connected) {
        setEvents([]);
        setLoading(false);
        return;
      }
      const ev = await api.calendar.getTodayEvents(sync);
      const eventsList = Array.isArray(ev) ? ev : [];
      setEvents(eventsList);
      setCached('calendar:today', { status: s, events: eventsList });
      // #region agent log
      fetch('http://127.0.0.1:7242/ingest/aa14b1ad-4efc-4b5c-bff5-8a2d20e47acb',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'CalendarToday.tsx:getTodayEvents',message:'getTodayEvents succeeded',data:{eventCount:eventsList.length},timestamp:Date.now(),hypothesisId:'H1'})}).catch(()=>{});
      // #endregion
    } catch (err) {
      const e = err as ApiError;
      // #region agent log
      fetch('http://127.0.0.1:7242/ingest/aa14b1ad-4efc-4b5c-bff5-8a2d20e47acb',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'CalendarToday.tsx:catch',message:'Calendar load error',data:{code:e?.code,message:e?.message,status:e?.status},timestamp:Date.now(),hypothesisId:'H1'})}).catch(()=>{});
      // #endregion
      setEvents([]);
      if (e?.code === 'CALENDAR_API_DISABLED') {
        let url: string | undefined;
        try {
          const parsed = typeof e.body === 'string' ? JSON.parse(e.body) : e.body;
          const detail = parsed?.error?.details?.find((d: { field?: string }) => d?.field === 'activationUrl');
          url = detail?.reason;
        } catch { /* ignore */ }
        setApiDisabledError({
          message: e?.message ?? 'Google Calendar API is not enabled for this project.',
          url: url ?? 'https://console.developers.google.com/apis/api/calendar-json.googleapis.com/overview',
        });
      }
      // Do NOT setStatus(null) when getTodayEvents fails — status came from getStatus and is still valid.
      // Only getStatus failure would leave status unset; keep connected state and show empty events.
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    setLoading(true);
    load(true);
  }, [load]);

  const handleRefresh = () => {
    setLoading(true);
    load(true).finally(() => setLoading(false));
  };

  if (loading && !status) {
    return (
      <div className={styles.container}>
        <header className={styles.header}>
          <h1>Today&apos;s Calendar</h1>
        </header>
        <div className={styles.loading}>Loading…</div>
      </div>
    );
  }

  if (!status?.connected) {
    return (
      <div className={styles.container}>
        <header className={styles.header}>
          <h1>Today&apos;s Calendar</h1>
        </header>
        <div className={styles.connectCard}>
          <p>Connect Google Calendar to see today&apos;s events here and in your objective queue.</p>
          <Link to="/settings#integrations" className={styles.connectLink}>
            Connect in Settings →
          </Link>
        </div>
      </div>
    );
  }

  if (apiDisabledError) {
    return (
      <div className={styles.container}>
        <header className={styles.header}>
          <h1>Today&apos;s Calendar</h1>
        </header>
        <div className={styles.connectCard}>
          <p>{apiDisabledError.message}</p>
          <a
            href={apiDisabledError.url}
            target="_blank"
            rel="noopener noreferrer"
            className={styles.connectLink}
          >
            Enable Google Calendar API in Cloud Console →
          </a>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <h1>Today&apos;s Calendar</h1>
        <button
          type="button"
          className={styles.refreshBtn}
          onClick={handleRefresh}
          disabled={loading}
          aria-label="Refresh"
        >
          ↻
        </button>
      </header>
      {events.length === 0 ? (
        <div className={styles.emptyState}>
          <p>No events for today.</p>
        </div>
      ) : (
        <ul className={styles.list}>
          {events.map((ev) => (
            <li key={ev.id} className={`${styles.event} ${ev.completed ? styles.eventCompleted : ''}`}>
              <div className={styles.eventTime}>
                {formatTime(ev.start_time)} – {formatTime(ev.end_time)}
              </div>
              <div className={styles.eventTitle}>{ev.title}</div>
              {ev.completed && <span className={styles.completedBadge}>Done</span>}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
