import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Input } from '../../components/ui';
import { Textarea } from '../../components/ui/Textarea';
import { Chip } from '../../components/ui';
import { useToast } from '../../context/ToastContext';
import { api } from '../../api/endpoints';
import type { ApiError } from '../../api/client';
import type { Quest } from '../../api/types';
import styles from './QuestForm.module.css';

const PRIORITIES = ['P0', 'P1', 'P2', 'P3', 'P4'] as const;
const TIME_OF_DAY = ['any', 'morning', 'afternoon', 'evening'] as const;
const CONTEXT_OPTIONS = ['focus', 'quick', 'low_energy'] as const;

export function EditQuest() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { show: showToast } = useToast();
  const [quest, setQuest] = useState<Quest | null>(null);
  const [title, setTitle] = useState('');
  const [notes, setNotes] = useState('');
  const [priority, setPriority] = useState<(typeof PRIORITIES)[number]>('P2');
  const [timeOfDay, setTimeOfDay] = useState<(typeof TIME_OF_DAY)[number]>('any');
  const [contextTag, setContextTag] = useState<(typeof CONTEXT_OPTIONS)[number] | undefined>();
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState(false);

  useEffect(() => {
    if (!id) return;
    api.quests.get(id)
      .then((q) => {
        setQuest(q);
        setTitle(q.title);
        setNotes(q.notes ?? '');
        setPriority(q.priority_level);
        setTimeOfDay((q.time_of_day as (typeof TIME_OF_DAY)[number]) ?? 'any');
        setContextTag((q.context_tag as (typeof CONTEXT_OPTIONS)[number]) ?? undefined);
      })
      .catch(() => setQuest(null));
  }, [id]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;
    setError('');
    if (!title.trim()) {
      setError('Title is required');
      return;
    }
    setLoading(true);
    try {
      await api.quests.update(id, {
        title: title.trim(),
        notes: notes.trim() || undefined,
        priorityLevel: priority,
        timeOfDay,
        contextTag,
      });
      showToast('Quest updated', 'success');
      navigate('/quests');
    } catch (err) {
      const e = err as ApiError;
      setError(e?.message ?? 'Failed to update quest');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!id) return;
    setLoading(true);
    try {
      await api.quests.delete(id);
      showToast('Quest deleted', 'success');
      navigate('/quests');
    } catch (err) {
      const e = err as ApiError;
      if ((e as { code?: string })?.code === 'QUEST_HAS_CHILDREN') {
        setError('Delete or reparent dependent quests first.');
      } else {
        setError(e?.message ?? 'Failed to delete quest');
      }
    } finally {
      setLoading(false);
      setDeleteConfirm(false);
    }
  };

  if (id && !quest) return <div className={styles.container}>Loading...</div>;
  if (!id) return null;

  return (
    <div className={styles.container}>
      <form onSubmit={handleSubmit} className={styles.form}>
        <h1 className={styles.title}>Edit Quest</h1>
        <div className={styles.field}>
          <Input
            label="Title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
            disabled={loading}
          />
        </div>
        <div className={styles.field}>
          <Textarea
            label="Notes"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            disabled={loading}
          />
        </div>
        <div className={styles.field}>
          <label className={styles.label}>Priority</label>
          <div className={styles.chipRow}>
            {PRIORITIES.map((p) => (
              <Chip key={p} selected={priority === p} onClick={() => setPriority(p)}>
                {p}
              </Chip>
            ))}
          </div>
        </div>
        <div className={styles.field}>
          <label className={styles.label}>Time of day</label>
          <div className={styles.chipRow}>
            {TIME_OF_DAY.map((t) => (
              <Chip key={t} selected={timeOfDay === t} onClick={() => setTimeOfDay(t)}>
                {t}
              </Chip>
            ))}
          </div>
        </div>
        <div className={styles.field}>
          <label className={styles.label}>Context</label>
          <div className={styles.chipRow}>
            {CONTEXT_OPTIONS.map((c) => (
              <Chip
                key={c}
                selected={contextTag === c}
                onClick={() => setContextTag(contextTag === c ? undefined : c)}
              >
                {c.replace('_', ' ')}
              </Chip>
            ))}
          </div>
        </div>
        {error && <p className={styles.error}>{error}</p>}
        <Button type="submit" fullWidth disabled={loading}>
          {loading ? 'Saving...' : 'Save'}
        </Button>
        <div className={styles.deleteSection}>
          {deleteConfirm ? (
            <>
              <p className={styles.deleteConfirm}>Delete this quest?</p>
              <div className={styles.deleteActions}>
                <Button variant="secondary" onClick={() => setDeleteConfirm(false)} disabled={loading}>
                  Cancel
                </Button>
                <Button variant="destructive" onClick={handleDelete} disabled={loading}>
                  Delete
                </Button>
              </div>
            </>
          ) : (
            <button
              type="button"
              className={styles.deleteTrigger}
              onClick={() => setDeleteConfirm(true)}
            >
              Delete quest
            </button>
          )}
        </div>
      </form>
    </div>
  );
}
