// @author David NTAMAKEMWA

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '../api/auth';
import { useAuth } from '../context/AuthContext';

const BNR_GOLD = '#C8972A';
const BNR_DARK = '#5C1B1B';

export default function LoginPage() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const user = await login(email, password);
      signIn(user);
      navigate('/dashboard');
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { error?: { message?: string } } } })
          ?.response?.data?.error?.message ?? 'Login failed. Check your credentials.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.page}>
      <div style={styles.card}>
        <div style={styles.logoRow}>
          <div style={styles.logoCircle}>BNR</div>
          <div>
            <div style={styles.orgName}>NATIONAL BANK OF RWANDA</div>
            <div style={styles.orgSub}>BANKI NKURU Y'U RWANDA</div>
          </div>
        </div>
        <h2 style={styles.title}>Bank Licensing Portal</h2>
        <p style={styles.subtitle}>Sign in to continue</p>

        {error && (
          <div style={styles.errorBox}>{error}</div>
        )}

        <form onSubmit={handleSubmit} style={styles.form}>
          <label style={styles.label}>Email address</label>
          <input
            style={styles.input}
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@bnr.rw"
            required
            autoFocus
          />

          <label style={styles.label}>Password</label>
          <input
            style={styles.input}
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            required
          />

          <button style={loading ? { ...styles.btn, opacity: 0.6 } : styles.btn} type="submit" disabled={loading}>
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <div style={styles.footer}>Internal use only — National Bank of Rwanda</div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  page: {
    minHeight: '100vh',
    background: '#f4f4f4',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  card: {
    background: '#fff',
    borderRadius: 8,
    boxShadow: '0 2px 16px rgba(0,0,0,0.10)',
    padding: '40px 36px',
    width: 380,
    maxWidth: '100%',
  },
  logoRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 14,
    marginBottom: 24,
  },
  logoCircle: {
    width: 52,
    height: 52,
    borderRadius: '50%',
    background: BNR_GOLD,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: 700,
    fontSize: 16,
    color: BNR_DARK,
    flexShrink: 0,
  },
  orgName: {
    fontWeight: 700,
    fontSize: 13,
    color: BNR_DARK,
    letterSpacing: '0.03em',
  },
  orgSub: {
    fontSize: 11,
    color: BNR_GOLD,
    marginTop: 2,
  },
  title: {
    margin: '0 0 4px',
    fontSize: 22,
    fontWeight: 700,
    color: '#222',
  },
  subtitle: {
    margin: '0 0 20px',
    fontSize: 14,
    color: '#666',
  },
  errorBox: {
    background: '#f8d7da',
    border: '1px solid #f5c2c7',
    color: '#842029',
    borderRadius: 5,
    padding: '10px 14px',
    fontSize: 13,
    marginBottom: 16,
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: 6,
  },
  label: {
    fontSize: 13,
    fontWeight: 600,
    color: '#444',
    marginTop: 8,
  },
  input: {
    border: '1px solid #ccc',
    borderRadius: 5,
    padding: '10px 12px',
    fontSize: 14,
    outline: 'none',
    width: '100%',
    boxSizing: 'border-box',
  },
  btn: {
    marginTop: 20,
    background: BNR_DARK,
    color: '#fff',
    border: 'none',
    borderRadius: 5,
    padding: '12px 0',
    fontSize: 15,
    fontWeight: 700,
    cursor: 'pointer',
    letterSpacing: '0.02em',
  },
  footer: {
    marginTop: 24,
    textAlign: 'center',
    fontSize: 11,
    color: '#aaa',
  },
};
