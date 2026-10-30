import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Input } from '../../components/ui';
import { useToast } from '../../context/ToastContext';
import { api } from '../../api/endpoints';
import type { ApiError } from '../../api/client';
import { CurrencyGuide } from './CurrencyGuide';
import styles from './HabitForm.module.css';

export function CreateHabit() {
  const navigate = useNavigate();
  const { show: showToast } = useToast();
  const [title, setTitle] = useState('');
  const [currency, setCurrency] = useState(10);
  const [streakTracked, setStreakTracked] = useState(true);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (!title.trim()) {
      setError('Title is required');
      return;
    }
    if (title.length > 200) {
      setError('Title must be 200 characters or less');
      return;
    }
    if (currency < 1) {
      setError('Currency must be at least 1');
      return;
    }
    setLoading(true);
    try {
      await api.habits.create({
        title: title.trim(),
        currencyPerCompletion: currency,
        streakTracked,
      });
      showToast('Habit created', 'success');
      navigate('/habits');
    } catch (err) {
      const e = err as ApiError;
      setError(e?.message ?? 'Failed to create habit');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <form onSubmit={handleSubmit} className={styles.form}>
        <h1 className={styles.title}>New Habit</h1>
        <div className={styles.field}>
          <Input
            label="Title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Morning run"
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
          {loading ? 'Creating...' : 'Create'}
        </Button>
      </form>
    </div>
  );
}
