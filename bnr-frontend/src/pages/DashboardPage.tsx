// @author David NTAMAKEMWA

import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getApplications } from '../api/applications';
import { Application } from '../types';
import { useAuth } from '../context/AuthContext';
import StatusBadge from '../components/ui/StatusBadge';
import Spinner from '../components/ui/Spinner';
import ErrorAlert from '../components/ui/ErrorAlert';
import EmptyState from '../components/ui/EmptyState';

export default function DashboardPage() {
  const { user } = useAuth();
  const [applications, setApplications] = useState<Application[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getApplications()
      .then((r) => {
        if (r.success) setApplications(r.data ?? []);
        else setError(r.error?.message ?? 'Failed to load applications');
      })
      .catch((err) => {
        const e = err as { response?: { data?: { error?: { message?: string } }; status?: number } };
        setError(e?.response?.data?.error?.message ?? 'Could not connect to the server');
      })
      .finally(() => setLoading(false));
  }, []);

  const title =
    user?.role === 'APPLICANT'
      ? 'My Applications'
      : user?.role === 'APPROVER'
      ? 'Applications Awaiting Decision'
      : 'Applications';

  if (loading) return <Spinner />;

  return (
    <div>
      <div style={styles.topRow}>
        <h1 style={styles.heading}>{title}</h1>
        {user?.role === 'APPLICANT' && (
          <Link to="/apply" style={styles.newBtn}>
            + New Application
          </Link>
        )}
      </div>

      {error && <ErrorAlert message={error} />}

      {!loading && applications.length === 0 ? (
        <EmptyState
          title="No applications yet"
          description={
            user?.role === 'APPLICANT'
              ? 'Submit your first application to get started.'
              : 'No applications match your current role.'
          }
          action={
            user?.role === 'APPLICANT' ? (
              <Link to="/apply" style={styles.newBtn}>
                New Application
              </Link>
            ) : undefined
          }
        />
      ) : (
        <table style={styles.table}>
          <thead>
            <tr>
              <th style={styles.th}>Institution</th>
              <th style={styles.th}>Type</th>
              <th style={styles.th}>Status</th>
              <th style={styles.th}>Submitted</th>
              <th style={styles.th}>Action</th>
            </tr>
          </thead>
          <tbody>
            {applications.map((app) => (
              <tr key={app.id} style={styles.tr}>
                <td style={styles.td}>{app.institutionName}</td>
                <td style={styles.td}>{app.institutionType}</td>
                <td style={styles.td}>
                  <StatusBadge status={app.status} />
                </td>
                <td style={styles.td}>
                  {app.submittedAt
                    ? new Date(app.submittedAt).toLocaleDateString()
                    : '—'}
                </td>
                <td style={styles.td}>
                  <Link to={`/applications/${app.id}`} style={styles.viewLink}>
                    View
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

const BNR_DARK = '#5C1B1B';
const BNR_GOLD = '#C8972A';

const styles: Record<string, React.CSSProperties> = {
  topRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 24,
  },
  heading: { margin: 0, fontSize: 24, color: BNR_DARK },
  newBtn: {
    background: BNR_DARK,
    color: '#fff',
    textDecoration: 'none',
    padding: '8px 18px',
    borderRadius: 5,
    fontWeight: 600,
    fontSize: 14,
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
    background: '#fff',
    borderRadius: 8,
    boxShadow: '0 1px 4px rgba(0,0,0,0.07)',
    overflow: 'hidden',
  },
  th: {
    textAlign: 'left',
    padding: '12px 16px',
    background: '#f8f8f8',
    borderBottom: '2px solid #eee',
    fontSize: 13,
    fontWeight: 700,
    color: '#444',
  },
  tr: { borderBottom: '1px solid #f0f0f0' },
  td: { padding: '12px 16px', fontSize: 14, color: '#333' },
  viewLink: {
    color: BNR_GOLD,
    fontWeight: 600,
    textDecoration: 'none',
    fontSize: 13,
  },
};
