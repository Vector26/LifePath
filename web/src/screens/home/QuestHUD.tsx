import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Button, Card, Chip } from '../../components/ui';
import { SkeletonQuestCard } from '../../components/feedback/SkeletonCard';
import { SyncIndicator } from '../../components/feedback/SyncIndicator';
import { BlockQuestModal } from './BlockQuestModal';
import { useCurrentObjective } from '../../hooks/useCurrentObjective';
import { useStatsToday } from '../../hooks/useStatsToday';
import { useUser } from '../../hooks/useUser';
import { useToast } from '../../context/ToastContext';
import { api } from '../../api/endpoints';
import { getReasonLabel } from '../../utils/reasons';
import { notifyWidgetRefresh } from '../../utils/androidApp';
import { invalidateCached } from '../../utils/swrCache';
import type { CurrentQuestResponse, Habit, CalendarEvent } from '../../api/types';
import styles from './QuestHUD.module.css';

const CONTEXT_OPTIONS = ['focus', 'quick', 'low_energy'] as const;
const CONTEXT_STORAGE_KEY = 'lifepath:context';

function getStoredContext(): 'focus' | 'quick' | 'low_energy' {
  try {
    const s = sessionStorage.getItem(CONTEXT_STORAGE_KEY);
    if (s === 'focus' || s === 'quick' || s === 'low_energy') return s;
  } catch { /* noop */ }
  return 'focus';
}

function setStoredContext(c: 'focus' | 'quick' | 'low_energy') {
  try {
    sessionStorage.setItem(CONTEXT_STORAGE_KEY, c);
  } catch { /* noop */ }
}

function formatTime(iso: string): string {
  try {
    return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  } catch {
    return '';
  }
}

export function QuestHUD() {
  const [context, setContext] = useState<'focus' | 'quick' | 'low_energy'>(getStoredContext);
  const [blockModalOpen, setBlockModalOpen] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionSuccess, setActionSuccess] = useState(false);
  const [reasonsExpanded, setReasonsExpanded] = useState(false);

  const { objective, loading, error, refetch } = useCurrentObjective(context);
  const { stats } = useStatsToday();
  const { user } = useUser();
  const { show: showToast } = useToast();

  useEffect(() => {
    setStoredContext(context);
  }, [context]);

  const type = objective?.type;
  const reasons = objective?.reasons ?? [];

  // Quest actions
  const questPayload = type === 'quest' ? (objective?.payload as CurrentQuestResponse) : null;
  const quest = questPayload?.quest ?? null;

  const handleCompleteQuest = async () => {
    if (!quest) return;
    setActionLoading(true);
    try {
      const res = await api.quests.complete(quest.id);
      setActionSuccess(true);
      if (res.xp_earned > 0) showToast(`+${res.xp_earned} XP`, 'success', 2000);
      setTimeout(() => {
        setActionSuccess(false);
        refetch();
      }, 600);
      notifyWidgetRefresh();
    } catch {
      showToast('Failed to complete', 'error');
    } finally {
      setActionLoading(false);
    }
  };

  const handleBlock = () => setBlockModalOpen(true);

  const blockCurrentObjective = async (payload: {
    unblockTrigger: 'manual' | 'time' | 'location';
    unblockAt?: string;
    unblockLocation?: string;
    reason?: string;
  }) => {
    if (type === 'quest' && quest) {
      await api.quests.block(quest.id, payload);
      return;
    }
    if (type === 'habit' && habitPayload) {
      await api.objectives.block({
        taskType: 'habit',
        taskId: habitPayload.id,
        ...payload,
      });
      return;
    }
    if (type === 'calendar_event' && calendarPayload) {
      await api.objectives.block({
        taskType: 'calendar_event',
        taskId: calendarPayload.id,
        ...payload,
      });
    }
  };

  const handleSkip = async () => {
    if (!type) return;
    setActionLoading(true);
    try {
      await blockCurrentObjective({
        unblockTrigger: 'manual',
        reason: 'Skipped for now',
      });
      invalidateCached('objective:');
      refetch();
      notifyWidgetRefresh();
    } catch {
      showToast('Failed to skip', 'error');
    } finally {
      setActionLoading(false);
    }
  };

  const handleBlockSubmit = async (payload: {
    unblockTrigger: 'manual' | 'time' | 'location';
    unblockAt?: string;
    unblockLocation?: string;
    reason?: string;
  }) => {
    if (!type) return;
    setActionLoading(true);
    try {
      await blockCurrentObjective(payload);
      setBlockModalOpen(false);
      showToast('Task blocked', 'success', 1500);
      invalidateCached('objective:');
      refetch();
      notifyWidgetRefresh();
    } catch {
      showToast('Failed to block', 'error');
    } finally {
      setActionLoading(false);
    }
  };

  // Habit actions
  const habitPayload = type === 'habit' ? (objective?.payload as Habit) : null;

  const handleCompleteHabit = async () => {
    if (!habitPayload) return;
    setActionLoading(true);
    try {
      const res = await api.habits.complete(habitPayload.id);
      setActionSuccess(true);
      if (res.currency_earned > 0) showToast(`+${res.currency_earned} coins`, 'success', 2000);
      setTimeout(() => {
        setActionSuccess(false);
        refetch();
      }, 600);
      notifyWidgetRefresh();
    } catch {
      showToast('Failed to complete habit', 'error');
    } finally {
      setActionLoading(false);
    }
  };

  // Calendar event actions
  const calendarPayload = type === 'calendar_event' ? (objective?.payload as CalendarEvent) : null;

  const handleCompleteCalendarEvent = async () => {
    if (!calendarPayload) return;
    setActionLoading(true);
    try {
      await api.calendar.completeEvent(calendarPayload.id);
      setActionSuccess(true);
      showToast('Event marked done', 'success', 1500);
      setTimeout(() => {
        setActionSuccess(false);
        refetch();
      }, 600);
      notifyWidgetRefresh();
    } catch {
      showToast('Failed to complete event', 'error');
    } finally {
      setActionLoading(false);
    }
  };

  const questsToday = stats?.quests_completed_today ?? 0;
  const habitsToday = stats?.habits_completed_today ?? 0;
  const level = user?.level ?? 1;
  const xp = user?.total_xp ?? 0;

  const typeLabel = type === 'quest' ? 'Quest' : type === 'habit' ? 'Habit' : type === 'calendar_event' ? 'Event' : '';

  return (
    <div className={styles.container}>
      <div className={styles.statsBar}>
        <span className={styles.stat}>Lv {level} • {xp} XP</span>
        <span className={styles.stat}>
          {questsToday} quests · {habitsToday} habits today
        </span>
        <span className={styles.stat}>
          <SyncIndicator />
          <button
            type="button"
            className={styles.refreshBtn}
            onClick={() => refetch()}
            disabled={loading}
            aria-label="Refresh"
          >
            ↻
          </button>
        </span>
      </div>
      {error?.toLowerCase().includes('refresh session') && (
        <div className={styles.errorBanner} role="alert">
          <span>{error}</span>
          <Button variant="secondary" onClick={() => refetch()} disabled={loading}>
            Retry
          </Button>
        </div>
      )}

      {loading ? (
        <SkeletonQuestCard />
      ) : type === 'quest' && quest ? (
        <Card padding="xl" className={styles.questCard}>
          <span className={styles.typeTag}>{typeLabel}</span>
          <h2 className={styles.title}>{quest.title}</h2>
          {quest.notes && <p className={styles.notes}>{quest.notes}</p>}
          {reasons.length > 0 && (
            <div className={styles.reasonsSection}>
              <button
                type="button"
                className={styles.reasonsTrigger}
                onClick={() => setReasonsExpanded(!reasonsExpanded)}
                aria-expanded={reasonsExpanded}
              >
                Why this? {reasonsExpanded ? '−' : '+'}
              </button>
              {reasonsExpanded && (
                <ul className={styles.reasonsList}>
                  {reasons.map((r) => (
                    <li key={r}>{getReasonLabel(r)}</li>
                  ))}
                </ul>
              )}
            </div>
          )}
          <div className={styles.metadataRow}>
            <Chip>{quest.priority_level}</Chip>
            <div className={styles.contextRow}>
              {CONTEXT_OPTIONS.map((opt) => (
                <Chip
                  key={opt}
                  selected={context === opt}
                  onClick={() => setContext(opt)}
                >
                  {opt.replace('_', ' ')}
                </Chip>
              ))}
            </div>
          </div>
          <div className={styles.actionArea}>
            <Button
              className={`${styles.completeBtn} ${actionSuccess ? styles.completeBtnSuccess : ''}`}
              onClick={handleCompleteQuest}
              disabled={actionLoading || actionSuccess}
            >
              {actionLoading ? (
                <span className={styles.spinner} />
              ) : actionSuccess ? (
                <span className={styles.checkmark}>Done!</span>
              ) : (
                'Complete'
              )}
            </Button>
            <Button variant="secondary" className={styles.blockBtn} onClick={handleBlock} disabled={actionLoading || actionSuccess}>
              Block
            </Button>
            <Button variant="tertiary" className={styles.skipBtn} onClick={handleSkip} disabled={actionLoading || actionSuccess}>
              Skip
            </Button>
          </div>
        </Card>
      ) : type === 'habit' && habitPayload ? (
        <Card padding="xl" className={styles.questCard}>
          <span className={styles.typeTag}>{typeLabel}</span>
          <h2 className={styles.title}>{habitPayload.title}</h2>
          <p className={styles.notes}>
            {habitPayload.currencyPerCompletion} currency on completion
            {habitPayload.streakTracked && ' · streak tracked'}
          </p>
          {reasons.length > 0 && (
            <div className={styles.reasonsSection}>
              <ul className={styles.reasonsList}>
                {reasons.map((r) => (
                  <li key={r}>{getReasonLabel(r)}</li>
                ))}
              </ul>
            </div>
          )}
          <div className={styles.actionArea}>
            <Button
              className={`${styles.completeBtn} ${actionSuccess ? styles.completeBtnSuccess : ''}`}
              onClick={handleCompleteHabit}
              disabled={actionLoading || actionSuccess}
            >
              {actionLoading ? (
                <span className={styles.spinner} />
              ) : actionSuccess ? (
                <span className={styles.checkmark}>Done!</span>
              ) : (
                'Complete Habit'
              )}
            </Button>
            <Button variant="secondary" className={styles.blockBtn} onClick={handleBlock} disabled={actionLoading || actionSuccess}>
              Block
            </Button>
            <Button variant="tertiary" className={styles.skipBtn} onClick={handleSkip} disabled={actionLoading || actionSuccess}>
              Skip
            </Button>
          </div>
        </Card>
      ) : type === 'calendar_event' && calendarPayload ? (
        <Card padding="xl" className={styles.questCard}>
          <span className={styles.typeTag}>{typeLabel}</span>
          <h2 className={styles.title}>{calendarPayload.title}</h2>
          <p className={styles.notes}>
            {formatTime(calendarPayload.start_time)} – {formatTime(calendarPayload.end_time)}
          </p>
          {reasons.length > 0 && (
            <div className={styles.reasonsSection}>
              <ul className={styles.reasonsList}>
                {reasons.map((r) => (
                  <li key={r}>{getReasonLabel(r)}</li>
                ))}
              </ul>
            </div>
          )}
          <div className={styles.actionArea}>
            <Button
              className={`${styles.completeBtn} ${actionSuccess ? styles.completeBtnSuccess : ''}`}
              onClick={handleCompleteCalendarEvent}
              disabled={actionLoading || actionSuccess}
            >
              {actionLoading ? (
                <span className={styles.spinner} />
              ) : actionSuccess ? (
                <span className={styles.checkmark}>Done!</span>
              ) : (
                'Mark Done'
              )}
            </Button>
            <Button variant="secondary" className={styles.blockBtn} onClick={handleBlock} disabled={actionLoading || actionSuccess}>
              Block
            </Button>
            <Button variant="tertiary" className={styles.skipBtn} onClick={handleSkip} disabled={actionLoading || actionSuccess}>
              Skip
            </Button>
          </div>
        </Card>
      ) : (
        <Card padding="xl" className={styles.emptyCard}>
          <div className={styles.emptyState}>
            <p className={styles.emptyTitle}>All clear!</p>
            <p className={styles.emptySubtitle}>
              No quests, habits, or events need attention right now.
            </p>
            <div className={styles.emptyActions}>
              <Link to="/quests/new">
                <Button>Add Quest</Button>
              </Link>
              <Link to="/habits/new">
                <Button variant="secondary">Add Habit</Button>
              </Link>
            </div>
          </div>
        </Card>
      )}

      <BlockQuestModal
        open={blockModalOpen}
        onClose={() => setBlockModalOpen(false)}
        onSubmit={handleBlockSubmit}
        loading={actionLoading}
      />
    </div>
  );
}
