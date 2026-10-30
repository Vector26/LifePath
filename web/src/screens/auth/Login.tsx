import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button, Input } from '../../components/ui';
import { api } from '../../api/endpoints';
import { resetLogoutState } from '../../api/client';
import type { ApiError } from '../../api/client';
import styles from './AuthPages.module.css';

function setTokens(access: string, refresh: string) {
  localStorage.setItem('access_token', access);
  localStorage.setItem('refresh_token', refresh);
}

export function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (!email.trim()) {
      setError('Email is required');
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      setError('Please enter a valid email address');
      return;
    }
    if (!password) {
      setError('Password is required');
      return;
    }
    setLoading(true);
    try {
      const data = await api.auth.login(email, password);
      if (!data?.access_token) {
        setError('Invalid response from server. Is the backend running on port 39081?');
        return;
      }
      setTokens(data.access_token, data.refresh_token ?? '');
      resetLogoutState();
      navigate('/quest');
    } catch (err) {
      const e = err as ApiError;
      setError(e?.message ?? 'Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <div className={styles.header}>
        <h1 className={styles.logo}>LifePath</h1>
        <p className={styles.subtitle}>Your gamified productivity companion</p>
      </div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.fieldGroup}>
          <Input
            type="email"
            label="Email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
            disabled={loading}
          />
        </div>
        <div className={styles.fieldGroup}>
          <Input
            type="password"
            label="Password"
            placeholder="••••••••"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            disabled={loading}
          />
        </div>
        {error && <p className={styles.error}>{error}</p>}
        <Button type="submit" fullWidth className={styles.submit} disabled={loading}>
          {loading ? 'Signing in...' : 'Log in'}
        </Button>
      </form>
      <p className={styles.footer}>
        Don't have an account? <Link to="/register">Create account</Link>
      </p>
    </>
  );
}
