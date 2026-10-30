import { Link } from 'react-router-dom';
import { Button, Card } from '../../components/ui';
import type { Quest } from '../../api/types';
import styles from './QuestCard.module.css';

type QuestCardProps = {
  quest: Quest;
  onUnblock?: (id: string) => void;
  unblockLoading?: boolean;
};

export function QuestCard({ quest, onUnblock, unblockLoading }: QuestCardProps) {
  const isBlocked = quest.status === 'blocked';

  const content = (
    <>
      <div className={styles.content}>
        <h3 className={styles.title}>{quest.title}</h3>
        <p className={styles.subtitle}>
          {quest.priority_level}
          {quest.time_of_day && quest.time_of_day !== 'any' && ` · ${quest.time_of_day}`}
        </p>
        {isBlocked && onUnblock && (
          <div className={styles.unblockRow}>
            <Button
              size="sm"
              variant="primary"
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                onUnblock(quest.id);
              }}
              disabled={unblockLoading}
            >
              Unblock
            </Button>
          </div>
        )}
      </div>
    </>
  );

  if (isBlocked && onUnblock) {
    return (
      <Card padding="md" className={styles.card}>
        {content}
      </Card>
    );
  }

  return (
    <Link to={`/quests/${quest.id}/edit`} className={styles.link}>
      <Card padding="md" className={styles.card}>
        {content}
      </Card>
    </Link>
  );
}
