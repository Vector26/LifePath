import { type TextareaHTMLAttributes, forwardRef } from 'react';
import styles from './Textarea.module.css';

type TextareaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & {
  label?: string;
  error?: string;
  id?: string;
};

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ label, error, id: propsId, className = '', ...props }, ref) => {
    const id = propsId ?? `textarea-${Math.random().toString(36).slice(2)}`;
    return (
      <div className={styles.group}>
        {label && (
          <label htmlFor={id} className={styles.label}>
            {label}
          </label>
        )}
        <textarea
          ref={ref}
          id={id}
          className={`${styles.textarea} ${error ? styles.error : ''} ${className}`.trim()}
          aria-invalid={!!error}
          aria-describedby={error ? `${id}-error` : undefined}
          minLength={1}
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

Textarea.displayName = 'Textarea';
