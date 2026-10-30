import type { HTMLAttributes } from 'react';
import styles from './Chip.module.css';

type ChipProps = HTMLAttributes<HTMLSpanElement> & {
  selected?: boolean;
};

export function Chip({ selected, className = '', children, onClick, ...props }: ChipProps) {
  const handleKeyDown = (e: React.KeyboardEvent<HTMLSpanElement>) => {
    if ((e.key === 'Enter' || e.key === ' ') && onClick) {
      e.preventDefault();
      e.currentTarget.click();
    }
  };

  return (
    <span
      className={`${styles.chip} ${selected ? styles.selected : ''} ${className}`.trim()}
      style={{ minHeight: 'var(--tap-min-height)', minWidth: 'var(--tap-min-width)' }}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onClick={onClick}
      onKeyDown={onClick ? handleKeyDown : undefined}
      {...props}
    >
      {children}
    </span>
  );
}
