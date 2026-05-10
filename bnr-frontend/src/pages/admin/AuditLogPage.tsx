// @author David NTAMAKEMWA

import React, { useEffect, useState } from 'react';
import { getGlobalAuditLog, PagedAuditResponse } from '../../api/audit';
import Spinner from '../../components/ui/Spinner';
import ErrorAlert from '../../components/ui/ErrorAlert';

const BNR_DARK = '#5C1B1B';
const BNR_GOLD = '#C8972A';

export default function AuditLogPage() {
  const [data, setData] = useState<PagedAuditResponse | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async (p: number) => {
    setLoading(true);
    setError('');
    try {
      const res = await getGlobalAuditLog(p, 50);
      if (res.success && res.data) setData(res.data);
      else setError(res.error?.message ?? 'Failed to load audit log');
    } catch {
      setError('Could not connect to the server');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(page); }, [page]);

  return (
    <div>
      <h1 style={styles.heading}>Global Audit Log</h1>
      <p style={styles.sub}>Every action taken on every application, in chronological order.</p>

      {error && <ErrorAlert message={error} />}
      {loading ? <Spinner /> : (
        <>
          <div style={styles.card}>
            <table style={styles.table}>
              <thead>
                <tr>
                  <th style={styles.th}>Time</th>
                  <th style={styles.th}>Actor</th>
                  <th style={styles.th}>Action</th>
                  <th style={styles.th}>Transition</th>
                  <th style={styles.th}>Application</th>
                </tr>
              </thead>
              <tbody>
                {data?.content.map((entry) => (
                  <tr key={entry.id} style={styles.tr}>
                    <td style={styles.td}>
                      <span style={styles.time}>
                        {new Date(entry.createdAt).toLocaleString()}
                      </span>
                    </td>
                    <td style={styles.td}>{entry.actorEmail}</td>
                    <td style={styles.td}>
                      <span style={styles.action}>{entry.action.replace(/_/g, ' ')}</span>
                    </td>
                    <td style={styles.td}>
                      {entry.previousStatus && entry.newStatus ? (
                        <span style={styles.transition}>
                          {entry.previousStatus} → {entry.newStatus}
                        </span>
                      ) : '—'}
                    </td>
                    <td style={styles.td}>
                      <span style={styles.appId}>
                        {entry.applicationId
                          ? String(entry.applicationId).slice(0, 8) + '…'
                          : '—'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {data && (
            <div style={styles.pagination}>
              <button
                style={styles.pageBtn}
                disabled={page === 0}
                onClick={() => setPage(p => p - 1)}
              >
                ← Previous
              </button>
              <span style={styles.pageInfo}>
                Page {data.page + 1} of {data.totalPages} &nbsp;·&nbsp; {data.totalElements} entries
              </span>
              <button
                style={styles.pageBtn}
                disabled={data.last}
                onClick={() => setPage(p => p + 1)}
              >
                Next →
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  heading: { margin: '0 0 4px', fontSize: 24, color: BNR_DARK },
  sub: { margin: '0 0 20px', fontSize: 14, color: '#666' },
  card: {
    background: '#fff',
    borderRadius: 8,
    boxShadow: '0 1px 4px rgba(0,0,0,0.07)',
    overflow: 'hidden',
  },
  table: { width: '100%', borderCollapse: 'collapse' },
  th: {
    textAlign: 'left',
    padding: '11px 14px',
    background: '#f8f8f8',
    borderBottom: '2px solid #eee',
    fontSize: 12,
    fontWeight: 700,
    color: '#555',
    textTransform: 'uppercase',
    letterSpacing: '0.04em',
  },
  tr: { borderBottom: '1px solid #f0f0f0' },
  td: { padding: '10px 14px', fontSize: 13, color: '#333', verticalAlign: 'middle' },
  time: { color: '#888', whiteSpace: 'nowrap', fontSize: 12 },
  action: { fontWeight: 600, color: BNR_DARK },
  transition: { color: BNR_GOLD, fontWeight: 600, fontSize: 12 },
  appId: { fontFamily: 'monospace', fontSize: 12, color: '#888' },
  pagination: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 20,
    marginTop: 20,
  },
  pageBtn: {
    background: BNR_DARK,
    color: '#fff',
    border: 'none',
    padding: '8px 18px',
    borderRadius: 5,
    cursor: 'pointer',
    fontSize: 13,
    fontWeight: 600,
  },
  pageInfo: { fontSize: 13, color: '#666' },
};
