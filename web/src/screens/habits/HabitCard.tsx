import { Link } from 'react-router-dom';
import { Button, Card } from '../../components/ui';
import type { Habit } from '../../api/types';
import styles from './HabitCard.module.css';

type HabitCardProps = {
  habit: Habit;
  streak?: { current: number; longest: number };
  onComplete?: (id: string) => void;
  completeLoading?: boolean;
  completedToday?: boolean;
};

export function HabitCard({ habit, streak, onComplete, completeLoading, completedToday }: HabitCardProps) {
  const currency = habit.currencyPerCompletion ?? (habit as { currency_per_completion?: number }).currency_per_completion ?? 10;

  return (
    <Card padding="md" className={styles.card}>
      <Link to={`/habits/${habit.id}/edit`} className={styles.content}>
        <h3 className={styles.title}>{habit.title}</h3>
        <p className={styles.subtitle}>
          +{currency} per completion
          {streak && ` · ${streak.current}/${streak.longest} day streak`}
        </p>
      </Link>
      <Button
        className={`${styles.completeBtn} ${completedToday ? styles.completeBtnCompleted : ''}`}
        size="sm"
        onClick={() => onComplete?.(habit.id)}
        disabled={completeLoading || completedToday}
      >
        {completedToday ? 'Completed' : 'Complete'}
      </Button>
    </Card>
  );
}
