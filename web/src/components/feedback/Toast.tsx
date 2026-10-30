import styles from './Toast.module.css';

type ToastProps = {
  message: string;
  type: 'success' | 'error' | 'info';
  onDismiss: () => void;
};

export function Toast({ message, type, onDismiss }: ToastProps) {
  return (
    <div
      className={`${styles.toast} ${styles[type]}`}
      role="alert"
      onClick={onDismiss}
    >
      {message}
    </div>
  );
}
