import styles from './CurrencyGuide.module.css';

const TIERS = [
  { label: 'Easy, already consistent', example: 'Drink water, make bed', range: '5–8', color: 'var(--color-primary-muted, #d4c4b0)' },
  { label: 'Moderate, building', example: 'Morning walk, 10-min stretch', range: '8–12', color: 'var(--color-primary-light, #c4a882)' },
  { label: 'Challenging, identity-building', example: 'Run 3×/week, meditation', range: '12–18', color: 'var(--color-primary, #c4785a)' },
  { label: 'Avoiding / fledgling', example: 'Habit you keep skipping', range: '18–25', color: 'var(--color-primary-strong, #a85d42)' },
];

export function CurrencyGuide() {
  return (
    <div className={styles.guide}>
      <p className={styles.heading}>📊 How to set currency (Atomic Habits)</p>
      <div className={styles.tiers}>
        {TIERS.map((t) => (
          <div key={t.label} className={styles.tier}>
            <div className={styles.bar} style={{ background: t.color }} />
            <div className={styles.tierContent}>
              <div className={styles.tierTop}>
                <span className={styles.tierLabel}>{t.label}</span>
                <span className={styles.tierRange}>{t.range}</span>
              </div>
              <span className={styles.tierExample}>{t.example}</span>
            </div>
          </div>
        ))}
      </div>
      <p className={styles.hint}>
        If you often skip this habit, give it a higher value (18–25). The extra reward helps your brain associate the action with satisfaction.
      </p>
    </div>
  );
}
