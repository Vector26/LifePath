import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Input } from '../../components/ui';
import { Textarea } from '../../components/ui/Textarea';
import { Chip } from '../../components/ui';
import { useToast } from '../../context/ToastContext';
import { api } from '../../api/endpoints';
import type { ApiError } from '../../api/client';
import styles from './QuestForm.module.css';

const PRIORITIES = ['P0', 'P1', 'P2', 'P3', 'P4'] as const;
const TIME_OF_DAY = ['any', 'morning', 'afternoon', 'evening'] as const;
const CONTEXT_OPTIONS = ['focus', 'quick', 'low_energy'] as const;

export function CreateQuest() {
  const navigate = useNavigate();
  const { show: showToast } = useToast();
  const [title, setTitle] = useState('');
  const [notes, setNotes] = useState('');
  const [priority, setPriority] = useState<(typeof PRIORITIES)[number]>('P2');
  const [timeOfDay, setTimeOfDay] = useState<(typeof TIME_OF_DAY)[number]>('any');
  const [contextTag, setContextTag] = useState<(typeof CONTEXT_OPTIONS)[number] | undefined>();
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (!title.trim()) {
      setError('Title is required');
      return;
    }
    if (title.length > 500) {
      setError('Title must be 500 characters or less');
      return;
    }
    if (notes.length > 5000) {
      setError('Notes must be 5000 characters or less');
      return;
    }
    setLoading(true);
    try {
      await api.quests.create({
        title: title.trim(),
        notes: notes.trim() || undefined,
        priorityLevel: priority,
        timeOfDay,
        contextTag,
      });
      showToast('Quest created', 'success');
      navigate('/quest');
    } catch (err) {
      const e = err as ApiError;
      setError(e?.message ?? 'Failed to create quest');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <form onSubmit={handleSubmit} className={styles.form}>
        <h1 className={styles.title}>New Quest</h1>
        <div className={styles.field}>
          <Input
            label="Title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="What do you need to do?"
            required
            disabled={loading}
          />
        </div>
        <div className={styles.field}>
          <Textarea
            label="Notes (optional)"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Additional details..."
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
          <label className={styles.label}>Time of day (optional)</label>
          <div className={styles.chipRow}>
            {TIME_OF_DAY.map((t) => (
              <Chip key={t} selected={timeOfDay === t} onClick={() => setTimeOfDay(t)}>
                {t}
              </Chip>
            ))}
          </div>
        </div>
        <div className={styles.field}>
          <label className={styles.label}>Context (optional)</label>
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
          {loading ? 'Creating...' : 'Create'}
        </Button>
      </form>
    </div>
  );
}
