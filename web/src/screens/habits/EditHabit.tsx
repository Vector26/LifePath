import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Input } from '../../components/ui';
import { useToast } from '../../context/ToastContext';
import { api } from '../../api/endpoints';
import type { ApiError } from '../../api/client';
import type { Habit } from '../../api/types';
import { CurrencyGuide } from './CurrencyGuide';
import styles from './HabitForm.module.css';

export function EditHabit() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { show: showToast } = useToast();
  const [habit, setHabit] = useState<Habit | null>(null);
  const [title, setTitle] = useState('');
  const [currency, setCurrency] = useState(10);
  const [streakTracked, setStreakTracked] = useState(true);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState(false);

  useEffect(() => {
    if (!id) return;
    api.habits.get(id)
      .then((h) => {
        setHabit(h);
        setTitle(h.title);
        setCurrency(h.currencyPerCompletion ?? 10);
        setStreakTracked(h.streakTracked ?? true);
      })
      .catch(() => setHabit(null));
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
      await api.habits.update(id, {
        title: title.trim(),
        currencyPerCompletion: currency,
        streakTracked,
      });
      showToast('Habit updated', 'success');
      navigate('/habits');
    } catch (err) {
      const e = err as ApiError;
      setError(e?.message ?? 'Failed to update habit');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!id) return;
    setLoading(true);
    try {
      await api.habits.delete(id);
      showToast('Habit deleted', 'success');
      navigate('/habits');
    } catch (err) {
      const e = err as ApiError;
      setError(e?.message ?? 'Failed to delete habit');
    } finally {
      setLoading(false);
      setDeleteConfirm(false);
    }
  };

  if (id && !habit) return <div className={styles.container}>Loading...</div>;
  if (!id) return null;

  return (
    <div className={styles.container}>
      <form onSubmit={handleSubmit} className={styles.form}>
        <h1 className={styles.title}>Edit Habit</h1>
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
          <div className={styles.guideWrapper}>
            <CurrencyGuide />
          </div>
          <Input
            type="number"
            label="Currency per completion"
            value={String(currency)}
            onChange={(e) => setCurrency(Math.max(1, parseInt(e.target.value, 10) || 1))}
            min={1}
            disabled={loading}
          />
        </div>
        <div className={styles.field}>
          <label className={styles.toggleRow}>
            <input
              type="checkbox"
              checked={streakTracked}
              onChange={(e) => setStreakTracked(e.target.checked)}
              disabled={loading}
            />
            <span>Track streak</span>
          </label>
        </div>
        {error && <p className={styles.error}>{error}</p>}
        <Button type="submit" fullWidth disabled={loading}>
          {loading ? 'Saving...' : 'Save'}
        </Button>
        <div className={styles.deleteSection}>
          {deleteConfirm ? (
            <>
              <p className={styles.deleteConfirm}>Delete this habit?</p>
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
              Delete habit
            </button>
          )}
        </div>
      </form>
    </div>
  );
}
