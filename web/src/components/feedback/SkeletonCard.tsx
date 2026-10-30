import styles from './SkeletonCard.module.css';

export function SkeletonQuestCard() {
  return (
    <div className={styles.card}>
      <div className={`${styles.line} ${styles.title}`} />
      <div className={`${styles.line} ${styles.notes}`} />
      <div className={styles.row}>
        <div className={`${styles.chip} ${styles.chip1}`} />
        <div className={`${styles.chip} ${styles.chip2}`} />
      </div>
      <div className={styles.actions}>
        <div className={`${styles.btn} ${styles.btnPrimary}`} />
        <div className={`${styles.btn} ${styles.btnSecondary}`} />
      </div>
    </div>
  );
}

export function SkeletonHabitCard() {
  return (
    <div className={styles.card}>
      <div className={styles.row}>
        <div className={`${styles.line} ${styles.title}`} />
        <div className={styles.circle} />
      </div>
      <div className={`${styles.line} ${styles.subtitle}`} />
    </div>
  );
}

export function SkeletonListItems({ count = 3 }: { count?: number }) {
  return (
    <>
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className={styles.listItem}>
          <div className={`${styles.line} ${styles.listTitle}`} />
          <div className={`${styles.line} ${styles.listSubtitle}`} />
        </div>
      ))}
    </>
  );
}
