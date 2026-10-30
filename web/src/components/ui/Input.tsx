import { type InputHTMLAttributes, forwardRef } from 'react';
import styles from './Input.module.css';

type InputProps = InputHTMLAttributes<HTMLInputElement> & {
  label?: string;
  error?: string;
  id?: string;
};

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, id: propsId, className = '', ...props }, ref) => {
    const id = propsId ?? `input-${Math.random().toString(36).slice(2)}`;
    return (
      <div className={styles.group}>
        {label && (
          <label htmlFor={id} className={styles.label}>
            {label}
          </label>
        )}
        <input
          ref={ref}
          id={id}
          className={`${styles.input} ${error ? styles.error : ''} ${className}`.trim()}
          aria-invalid={!!error}
          aria-describedby={error ? `${id}-error` : undefined}
          style={{ minHeight: '48px' }}
          {...props}
        />
        {error && (
          <span id={`${id}-error`} className={styles.errorText}>
            {error}
          </span>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';
