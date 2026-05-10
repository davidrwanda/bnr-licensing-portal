// @author David NTAMAKEMWA

import React, { useEffect, useState } from 'react';
import client from '../../api/client';
import { ApiResponse } from '../../types';
import Spinner from '../../components/ui/Spinner';
import ErrorAlert from '../../components/ui/ErrorAlert';

const BNR_DARK = '#5C1B1B';

interface User {
  id: string;
  fullName: string;
  email: string;
  role: string;
}

const ROLE_COLORS: Record<string, { bg: string; color: string }> = {
  ADMIN:     { bg: BNR_DARK,  color: '#fff' },
  REVIEWER:  { bg: '#fff3cd', color: '#664d03' },
  APPROVER:  { bg: '#d1ecf1', color: '#0c5460' },
  APPLICANT: { bg: '#e9ecef', color: '#495057' },
};

export default function UsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filterRole, setFilterRole] = useState('');

  useEffect(() => {
    setLoading(true);
    const url = filterRole ? `/api/users?role=${filterRole}` : '/api/users';
    client.get<ApiResponse<User[]>>(url)
      .then((r) => {
        if (r.data.success) setUsers(r.data.data ?? []);
        else setError(r.data.error?.message ?? 'Failed to load users');
      })
      .catch(() => setError('Could not connect to the server'))
      .finally(() => setLoading(false));
  }, [filterRole]);

  const filtered = users;

  return (
    <div>
      <div style={styles.topRow}>
        <h1 style={styles.heading}>Users</h1>
        <select
          style={styles.filter}
          value={filterRole}
          onChange={(e) => setFilterRole(e.target.value)}
        >
          <option value="">All roles</option>
          <option value="ADMIN">Admin</option>
          <option value="REVIEWER">Reviewer</option>
          <option value="APPROVER">Approver</option>
          <option value="APPLICANT">Applicant</option>
        </select>
      </div>

      {error && <ErrorAlert message={error} />}
      {loading ? <Spinner /> : (
        <div style={styles.card}>
          {filtered.length === 0 ? (
            <div style={styles.empty}>No users found</div>
          ) : (
            <table style={styles.table}>
              <thead>
                <tr>
                  <th style={styles.th}>Full name</th>
                  <th style={styles.th}>Email</th>
                  <th style={styles.th}>Role</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((u) => {
                  const badge = ROLE_COLORS[u.role] ?? { bg: '#eee', color: '#333' };
                  return (
                    <tr key={u.id} style={styles.tr}>
                      <td style={styles.td}>{u.fullName}</td>
                      <td style={styles.td}>{u.email}</td>
                      <td style={styles.td}>
                        <span style={{ ...styles.badge, background: badge.bg, color: badge.color }}>
                          {u.role}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  topRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 },
  heading: { margin: 0, fontSize: 24, color: BNR_DARK },
  filter: {
    border: '1px solid #ccc',
    borderRadius: 5,
    padding: '7px 12px',
    fontSize: 14,
    cursor: 'pointer',
  },
  card: {
    background: '#fff',
    borderRadius: 8,
    boxShadow: '0 1px 4px rgba(0,0,0,0.07)',
    overflow: 'hidden',
  },
  table: { width: '100%', borderCollapse: 'collapse' },
  th: {
    textAlign: 'left',
    padding: '11px 16px',
    background: '#f8f8f8',
    borderBottom: '2px solid #eee',
    fontSize: 12,
    fontWeight: 700,
    color: '#555',
    textTransform: 'uppercase',
    letterSpacing: '0.04em',
  },
  tr: { borderBottom: '1px solid #f0f0f0' },
  td: { padding: '12px 16px', fontSize: 14, color: '#333' },
  badge: {
    padding: '3px 10px',
    borderRadius: 12,
    fontSize: 12,
    fontWeight: 700,
  },
  empty: { textAlign: 'center', padding: '40px 0', color: '#888', fontSize: 14 },
};
