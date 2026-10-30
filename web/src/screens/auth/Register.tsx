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

const TIMEZONES = [
  'Asia/Kolkata',
  'Asia/Tokyo',
  'Asia/Shanghai',
  'Asia/Dubai',
  'Asia/Singapore',
  'America/New_York',
  'America/Chicago',
  'America/Denver',
  'America/Los_Angeles',
  'America/Sao_Paulo',
  'Europe/London',
  'Europe/Paris',
  'Europe/Berlin',
  'Europe/Moscow',
  'Australia/Sydney',
  'Pacific/Auckland',
  'UTC',
];

function detectTimezone(): string {
  try {
    const tz = Intl.DateTimeFormat().resolvedOptions().timeZone;
    if (tz) return tz;
  } catch { /* fallback */ }
  return 'UTC';
}

export function Register() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [timezone, setTimezone] = useState(detectTimezone);
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
    if (password.length < 8) {
      setError('Password must be at least 8 characters');
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }
    if (!timezone) {
      setError('Timezone is required');
      return;
    }
    setLoading(true);
    try {
      const data = await api.auth.register({
        email,
        password,
        displayName: displayName || undefined,
        timezone,
      });
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
        <p className={styles.subtitle}>Create your account</p>
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
            placeholder="Min 8 characters"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="new-password"
            disabled={loading}
          />
        </div>
        <div className={styles.fieldGroup}>
          <Input
            type="password"
            label="Confirm password"
            placeholder="Repeat password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            autoComplete="new-password"
            disabled={loading}
          />
        </div>
        <div className={styles.fieldGroup}>
          <Input
            type="text"
            label="Display name (optional)"
            placeholder="How we'll call you"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            autoComplete="name"
            disabled={loading}
          />
        </div>
        <div className={styles.fieldGroup}>
          <label className={styles.label}>Timezone</label>
          <select
            className={styles.select}
            value={timezone}
            onChange={(e) => setTimezone(e.target.value)}
            disabled={loading}
          >
            {(TIMEZONES.includes(timezone) ? TIMEZONES : [timezone, ...TIMEZONES]).map((tz) => (
              <option key={tz} value={tz}>
                {tz.replace(/_/g, ' ')}
              </option>
            ))}
          </select>
        </div>
        {error && <p className={styles.error}>{error}</p>}
        <Button type="submit" fullWidth className={styles.submit} disabled={loading}>
          {loading ? 'Creating account...' : 'Create account'}
        </Button>
      </form>
      <p className={styles.footer}>
        Already have an account? <Link to="/login">Log in</Link>
      </p>
    </>
  );
}
