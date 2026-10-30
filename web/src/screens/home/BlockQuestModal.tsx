import { useState } from 'react';
import { Button } from '../../components/ui';
import styles from './BlockQuestModal.module.css';

type BlockPayload = {
  unblockTrigger: 'manual' | 'time' | 'location';
  unblockAt?: string;
  unblockLocation?: string;
  reason?: string;
};

type BlockQuestModalProps = {
  open: boolean;
  onClose: () => void;
  onSubmit: (payload: BlockPayload) => void | Promise<void>;
  loading?: boolean;
};

export function BlockQuestModal({
  open,
  onClose,
  onSubmit,
  loading = false,
}: BlockQuestModalProps) {
  const [trigger, setTrigger] = useState<'manual' | 'time' | 'location'>('manual');
  const [unblockAt, setUnblockAt] = useState('');
  const [unblockLocation, setUnblockLocation] = useState('');
  const [reason, setReason] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const payload: BlockPayload = {
      unblockTrigger: trigger,
      reason: reason || undefined,
    };
    if (trigger === 'time') payload.unblockAt = unblockAt;
    if (trigger === 'location') payload.unblockLocation = unblockLocation;
    await onSubmit(payload);
    onClose();
    setTrigger('manual');
    setUnblockAt('');
    setUnblockLocation('');
    setReason('');
  };

  if (!open) return null;

  return (
    <div
      className={styles.overlay}
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby="block-quest-title"
    >
      <div
        className={styles.sheet}
        onClick={(e) => e.stopPropagation()}
      >
        <div className={styles.handle} aria-hidden />
        <h2 id="block-quest-title" className={styles.title}>
          Block quest
        </h2>
        <form onSubmit={handleSubmit}>
          <div className={styles.segmented}>
            {(['manual', 'time', 'location'] as const).map((t) => (
              <button
                key={t}
                type="button"
                className={`${styles.segment} ${trigger === t ? styles.segmentActive : ''}`}
                onClick={() => setTrigger(t)}
              >
                {t}
              </button>
            ))}
          </div>
          {trigger === 'time' && (
            <div className={styles.field}>
              <label className={styles.fieldLabel}>Unblock at</label>
              <input
                type="datetime-local"
                className={styles.input}
                value={unblockAt}
                onChange={(e) => setUnblockAt(e.target.value)}
                required={trigger === 'time'}
              />
            </div>
          )}
          {trigger === 'location' && (
            <div className={styles.field}>
              <label className={styles.fieldLabel}>Unblock location</label>
              <input
                type="text"
                className={styles.input}
                placeholder="e.g. Home, Office"
                value={unblockLocation}
                onChange={(e) => setUnblockLocation(e.target.value)}
                required={trigger === 'location'}
              />
            </div>
          )}
          <div className={styles.field}>
            <label className={styles.fieldLabel}>Reason (optional)</label>
            <textarea
              className={styles.textarea}
              placeholder="Why blocking?"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              rows={3}
            />
          </div>
          <div className={styles.actions}>
            <Button type="button" variant="secondary" onClick={onClose} className={styles.cancel}>
              Cancel
            </Button>
            <Button type="submit" disabled={loading} className={styles.submit}>
              {loading ? 'Blocking...' : 'Block'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
