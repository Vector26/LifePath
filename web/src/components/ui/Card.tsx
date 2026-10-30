import { type HTMLAttributes, forwardRef } from 'react';
import styles from './Card.module.css';

type CardProps = HTMLAttributes<HTMLDivElement> & {
  padding?: 'sm' | 'md' | 'lg' | 'xl';
};

export const Card = forwardRef<HTMLDivElement, CardProps>(
  ({ padding = 'md', className = '', children, ...props }, ref) => (
    <div
      ref={ref}
      className={`${styles.card} ${styles[padding]} ${className}`.trim()}
      {...props}
    >
      {children}
    </div>
  )
);

Card.displayName = 'Card';
