// @author David NTAMAKEMWA

import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  getApplication,
  updateApplication,
  submitApplication,
  withdrawApplication,
  resubmitApplication,
  assignReviewer,
  requestInfo,
  completeReview,
  approveApplication,
  rejectApplication,
} from '../api/applications';
import { getDocuments, uploadDocument, downloadUrl } from '../api/documents';
import { getAuditLog } from '../api/audit';
import { getReviewers, UserSummary } from '../api/users';
import { Application, Document, AuditEntry } from '../types';
import { useAuth } from '../context/AuthContext';
import StatusBadge from '../components/ui/StatusBadge';
import Spinner from '../components/ui/Spinner';
import ErrorAlert from '../components/ui/ErrorAlert';

const BNR_DARK = '#5C1B1B';
const BNR_GOLD = '#C8972A';

export default function ApplicationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const [app, setApp] = useState<Application | null>(null);
  const [docs, setDocs] = useState<Document[]>([]);
  const [audit, setAudit] = useState<AuditEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [noteInput, setNoteInput] = useState('');
  const [reviewerIdInput, setReviewerIdInput] = useState('');
  const [reviewers, setReviewers] = useState<UserSummary[]>([]);
  const [busy, setBusy] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState({
    institutionName: '',
    institutionType: '',
    contactAddress: '',
    businessDescription: '',
  });

  const loadAll = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const [appRes, docRes] = await Promise.all([getApplication(id), getDocuments(id)]);
      if (appRes.success) setApp(appRes.data);
      else setError(appRes.error?.message ?? 'Failed to load application');
      if (docRes.success) setDocs(docRes.data ?? []);

      if (user?.role !== 'APPLICANT') {
        const auditRes = await getAuditLog(id);
        if (auditRes.success) setAudit(auditRes.data ?? []);
      }
    } catch {
      setError('Could not connect to the server');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAll();
    if (user?.role === 'ADMIN') {
      getReviewers().then((r) => { if (r.success) setReviewers(r.data ?? []); });
    }
  }, [id]);

  const extractError = (e: unknown, fallback: string): string => {
    const err = e as {
      response?: {
        status?: number;
        data?: { error?: { message?: string }; message?: string };
      };
      message?: string;
    };
    // Prefer the structured ApiResponse error message
    if (err?.response?.data?.error?.message) return err.response.data.error.message;
    // Some Spring errors surface a plain "message" field
    if (err?.response?.data?.message) return err.response.data.message;
    // Network-level error (no response at all)
    if (!err?.response && err?.message) return `Network error: ${err.message}`;
    // Last resort: show status code so it's not silent
    if (err?.response?.status) return `${fallback} (HTTP ${err.response.status})`;
    return fallback;
  };

  const act = async (fn: () => Promise<unknown>) => {
    setActionError('');
    setBusy(true);
    try {
      await fn();
      await loadAll();
    } catch (e: unknown) {
      setActionError(extractError(e, 'Action failed'));
    } finally {
      setBusy(false);
    }
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !id) return;
    if (file.size > 5 * 1024 * 1024) {
      setActionError('File is larger than 5 MB. Please upload a smaller file.');
      return;
    }
    // Reset input so the same file can be re-uploaded after fixing an error
    e.target.value = '';
    setUploading(true);
    setActionError('');
    try {
      await uploadDocument(id, file);
      await loadAll();
    } catch (e: unknown) {
      setActionError(extractError(e, 'Upload failed'));
    } finally {
      setUploading(false);
    }
  };

  if (loading) return <Spinner />;
  if (error) return <ErrorAlert message={error} />;
  if (!app) return null;

  const isApplicant = user?.role === 'APPLICANT';
  const isReviewer = user?.role === 'REVIEWER';
  const isApprover = user?.role === 'APPROVER';
  const isAdmin = user?.role === 'ADMIN';
  const isOwner = isApplicant && app.applicant?.id === user?.userId;
  const isAssignedReviewer = isReviewer && app.reviewer?.id === user?.userId;
  const isNotReviewer = !app.reviewer || app.reviewer.id !== user?.userId;

  return (
    <div>
      <div style={styles.topRow}>
        <div>
          <h1 style={styles.heading}>{app.institutionName}</h1>
          <div style={styles.meta}>{app.institutionType}</div>
        </div>
        <StatusBadge status={app.status} />
      </div>

      {actionError && <ErrorAlert message={actionError} />}

      <div style={styles.grid}>
        {/* Left: Application info */}
        <div>
          <div style={styles.card}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <h3 style={{ ...styles.cardTitle, margin: 0, borderBottom: 'none', paddingBottom: 0 }}>Application Details</h3>
              {isOwner && app.status === 'DRAFT' && !editing && (
                <button
                  style={styles.editBtn}
                  onClick={() => {
                    setEditForm({
                      institutionName: app.institutionName,
                      institutionType: app.institutionType,
                      contactAddress: app.contactAddress ?? '',
                      businessDescription: app.businessDescription ?? '',
                    });
                    setEditing(true);
                  }}
                >
                  Edit
                </button>
              )}
            </div>
            <div style={{ borderBottom: `2px solid ${BNR_GOLD}`, marginBottom: 16 }} />

            {editing ? (
              <div>
                <label style={styles.label}>Institution name *</label>
                <input
                  style={styles.input}
                  value={editForm.institutionName}
                  onChange={(e) => setEditForm((f) => ({ ...f, institutionName: e.target.value }))}
                />
                <label style={styles.label}>Institution type *</label>
                <select
                  style={styles.input}
                  value={editForm.institutionType}
                  onChange={(e) => setEditForm((f) => ({ ...f, institutionType: e.target.value }))}
                >
                  <option>Commercial Bank</option>
                  <option>Microfinance Institution</option>
                  <option>Savings and Credit Cooperative</option>
                  <option>Development Finance Institution</option>
                  <option>Insurance Company</option>
                  <option>Other</option>
                </select>
                <label style={styles.label}>Contact address</label>
                <input
                  style={styles.input}
                  value={editForm.contactAddress}
                  onChange={(e) => setEditForm((f) => ({ ...f, contactAddress: e.target.value }))}
                />
                <label style={styles.label}>Business description</label>
                <textarea
                  style={{ ...styles.input, height: 100, resize: 'vertical' }}
                  value={editForm.businessDescription}
                  onChange={(e) => setEditForm((f) => ({ ...f, businessDescription: e.target.value }))}
                />
                <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
                  <button
                    style={styles.primaryBtn}
                    disabled={busy || !editForm.institutionName || !editForm.institutionType}
                    onClick={() =>
                      act(() => updateApplication(app.id, editForm)).then(() => setEditing(false))
                    }
                  >
                    {busy ? 'Saving…' : 'Save changes'}
                  </button>
                  <button style={styles.cancelBtn} onClick={() => setEditing(false)}>
                    Cancel
                  </button>
                </div>
              </div>
            ) : (
              <>
                <Row label="Status"><StatusBadge status={app.status} /></Row>
                <Row label="Applicant">{app.applicant?.fullName ?? '—'} ({app.applicant?.email})</Row>
                <Row label="Reviewer">{app.reviewer?.fullName ?? '—'}</Row>
                <Row label="Submitted">{app.submittedAt ? new Date(app.submittedAt).toLocaleString() : '—'}</Row>
                {app.contactAddress && <Row label="Address">{app.contactAddress}</Row>}
                {app.businessDescription && <Row label="Description">{app.businessDescription}</Row>}
                {app.reviewerNotes && <Row label="Reviewer notes">{app.reviewerNotes}</Row>}
                {app.decisionReason && <Row label="Decision reason">{app.decisionReason}</Row>}
                {app.decidedAt && <Row label="Decided">{new Date(app.decidedAt).toLocaleString()}</Row>}
              </>
            )}
          </div>

          {/* Actions */}
          {!app.status.match(/APPROVED|REJECTED|WITHDRAWN/) && (
            <div style={styles.card}>
              <h3 style={styles.cardTitle}>Actions</h3>

              {isOwner && app.status === 'DRAFT' && (
                <button style={styles.primaryBtn} onClick={() => act(() => submitApplication(app.id))} disabled={busy}>
                  Submit Application
                </button>
              )}

              {isOwner && app.status === 'INFO_REQUESTED' && (
                <button style={styles.primaryBtn} onClick={() => act(() => resubmitApplication(app.id))} disabled={busy}>
                  Resubmit Application
                </button>
              )}

              {isOwner && (
                <button style={styles.dangerBtn} onClick={() => act(() => withdrawApplication(app.id))} disabled={busy}>
                  Withdraw Application
                </button>
              )}

              {isAdmin && app.status === 'SUBMITTED' && (
                <div style={{ marginTop: 12 }}>
                  <label style={styles.label}>Select reviewer</label>
                  {reviewers.length === 0 ? (
                    <div style={{ fontSize: 13, color: '#888', marginBottom: 8 }}>
                      No active reviewers found
                    </div>
                  ) : (
                    <select
                      style={styles.input}
                      value={reviewerIdInput}
                      onChange={(e) => setReviewerIdInput(e.target.value)}
                    >
                      <option value="">— choose a reviewer —</option>
                      {reviewers.map((r) => (
                        <option key={r.id} value={r.id}>
                          {r.fullName} ({r.email})
                        </option>
                      ))}
                    </select>
                  )}
                  <button
                    style={{ ...styles.primaryBtn, marginTop: 10 }}
                    onClick={() => act(() => assignReviewer(app.id, reviewerIdInput))}
                    disabled={busy || !reviewerIdInput}
                  >
                    Assign Reviewer
                  </button>
                </div>
              )}

              {isAssignedReviewer && app.status === 'UNDER_REVIEW' && (
                <div>
                  <label style={styles.label}>Notes</label>
                  <textarea
                    style={{ ...styles.input, height: 80 }}
                    value={noteInput}
                    onChange={(e) => setNoteInput(e.target.value)}
                  />
                  <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
                    <button
                      style={styles.warnBtn}
                      onClick={() => act(() => requestInfo(app.id, noteInput))}
                      disabled={busy || !noteInput}
                    >
                      Request Info
                    </button>
                    <button
                      style={styles.primaryBtn}
                      onClick={() => act(() => completeReview(app.id, noteInput))}
                      disabled={busy || !noteInput}
                    >
                      Complete Review
                    </button>
                  </div>
                </div>
              )}

              {isApprover && app.status === 'REVIEW_COMPLETE' && isNotReviewer && (
                <div>
                  <label style={styles.label}>Decision reason</label>
                  <textarea
                    style={{ ...styles.input, height: 80 }}
                    value={noteInput}
                    onChange={(e) => setNoteInput(e.target.value)}
                  />
                  <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
                    <button
                      style={styles.primaryBtn}
                      onClick={() => act(() => approveApplication(app.id, noteInput))}
                      disabled={busy || !noteInput}
                    >
                      Approve
                    </button>
                    <button
                      style={styles.dangerBtn}
                      onClick={() => act(() => rejectApplication(app.id, noteInput))}
                      disabled={busy || !noteInput}
                    >
                      Reject
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Right: Documents + Audit */}
        <div>
          <div style={styles.card}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={styles.cardTitle}>Documents</h3>
              {(isOwner || isAdmin || isReviewer) && (
                <label style={styles.uploadLabel}>
                  {uploading ? 'Uploading…' : 'Upload file'}
                  <input type="file" style={{ display: 'none' }} onChange={handleFileUpload} disabled={uploading} />
                </label>
              )}
            </div>
            {docs.length === 0 ? (
              <div style={styles.emptyDocs}>No documents uploaded yet</div>
            ) : (
              <ul style={styles.docList}>
                {docs.map((doc) => (
                  <li key={doc.id} style={styles.docItem}>
                    <div>
                      <div style={styles.docName}>{doc.fileName}</div>
                      <div style={styles.docMeta}>
                        {(doc.fileSize / 1024).toFixed(1)} KB · v{doc.documentVersion} · {doc.uploaderEmail}
                      </div>
                    </div>
                    <a
                      href={downloadUrl(doc.id)}
                      target="_blank"
                      rel="noreferrer"
                      style={styles.downloadLink}
                    >
                      Download
                    </a>
                  </li>
                ))}
              </ul>
            )}
          </div>

          {!isApplicant && audit.length > 0 && (
            <div style={styles.card}>
              <h3 style={styles.cardTitle}>Audit Trail</h3>
              <ul style={styles.auditList}>
                {audit.map((entry) => (
                  <li key={entry.id} style={styles.auditItem}>
                    <div style={styles.auditAction}>{entry.action.replace(/_/g, ' ')}</div>
                    <div style={styles.auditMeta}>
                      {entry.actorEmail} · {new Date(entry.createdAt).toLocaleString()}
                    </div>
                    {entry.previousStatus && (
                      <div style={styles.auditTransition}>
                        {entry.previousStatus} → {entry.newStatus}
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div style={{ display: 'flex', gap: 8, marginBottom: 10, fontSize: 14 }}>
      <div style={{ width: 130, flexShrink: 0, fontWeight: 600, color: '#555' }}>{label}</div>
      <div style={{ color: '#222' }}>{children}</div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  topRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 },
  heading: { margin: 0, fontSize: 24, color: BNR_DARK },
  meta: { fontSize: 14, color: '#666', marginTop: 4 },
  grid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, alignItems: 'start' },
  card: {
    background: '#fff',
    borderRadius: 8,
    boxShadow: '0 1px 4px rgba(0,0,0,0.07)',
    padding: '20px 24px',
    marginBottom: 20,
  },
  cardTitle: {
    margin: '0 0 16px',
    fontSize: 15,
    fontWeight: 700,
    color: BNR_DARK,
    borderBottom: `2px solid ${BNR_GOLD}`,
    paddingBottom: 8,
  },
  label: { display: 'block', fontSize: 13, fontWeight: 600, color: '#444', marginBottom: 4, marginTop: 10 },
  editBtn: {
    background: 'transparent',
    border: `1px solid ${BNR_GOLD}`,
    color: BNR_GOLD,
    padding: '4px 14px',
    borderRadius: 4,
    cursor: 'pointer',
    fontSize: 13,
    fontWeight: 600,
  },
  cancelBtn: {
    background: '#fff',
    border: '1px solid #ccc',
    color: '#555',
    padding: '9px 18px',
    borderRadius: 5,
    fontWeight: 600,
    cursor: 'pointer',
    fontSize: 14,
  },
  input: {
    width: '100%',
    border: '1px solid #ccc',
    borderRadius: 5,
    padding: '8px 10px',
    fontSize: 14,
    boxSizing: 'border-box',
    fontFamily: 'inherit',
  },
  primaryBtn: {
    background: BNR_DARK,
    color: '#fff',
    border: 'none',
    padding: '9px 18px',
    borderRadius: 5,
    fontWeight: 600,
    cursor: 'pointer',
    fontSize: 14,
    marginTop: 8,
    marginRight: 8,
  },
  dangerBtn: {
    background: '#842029',
    color: '#fff',
    border: 'none',
    padding: '9px 18px',
    borderRadius: 5,
    fontWeight: 600,
    cursor: 'pointer',
    fontSize: 14,
    marginTop: 8,
  },
  warnBtn: {
    background: BNR_GOLD,
    color: BNR_DARK,
    border: 'none',
    padding: '9px 18px',
    borderRadius: 5,
    fontWeight: 600,
    cursor: 'pointer',
    fontSize: 14,
  },
  uploadLabel: {
    background: BNR_GOLD,
    color: BNR_DARK,
    padding: '6px 14px',
    borderRadius: 5,
    fontSize: 13,
    fontWeight: 600,
    cursor: 'pointer',
  },
  emptyDocs: { fontSize: 14, color: '#999', textAlign: 'center', padding: '20px 0' },
  docList: { listStyle: 'none', padding: 0, margin: 0 },
  docItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '10px 0',
    borderBottom: '1px solid #f0f0f0',
  },
  docName: { fontSize: 14, fontWeight: 600, color: '#333' },
  docMeta: { fontSize: 12, color: '#888', marginTop: 2 },
  downloadLink: { color: BNR_GOLD, fontWeight: 600, fontSize: 13, textDecoration: 'none' },
  auditList: { listStyle: 'none', padding: 0, margin: 0 },
  auditItem: { padding: '10px 0', borderBottom: '1px solid #f0f0f0' },
  auditAction: { fontSize: 13, fontWeight: 600, color: '#333' },
  auditMeta: { fontSize: 12, color: '#888', marginTop: 2 },
  auditTransition: { fontSize: 12, color: BNR_GOLD, marginTop: 2, fontWeight: 600 },
};
