// @author David NTAMAKEMWA

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createApplication, submitApplication } from '../../api/applications';
import ErrorAlert from '../../components/ui/ErrorAlert';

const BNR_DARK = '#5C1B1B';
const BNR_GOLD = '#C8972A';

export default function NewApplicationPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    institutionName: '',
    institutionType: '',
    contactAddress: '',
    businessDescription: '',
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const set = (field: string) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
    setForm((f) => ({ ...f, [field]: e.target.value }));

  const validate = () => {
    if (!form.institutionName.trim()) return 'Institution name is required';
    if (!form.institutionType) return 'Institution type is required';
    return null;
  };

  const extractError = (err: unknown): string => {
    const e = err as { response?: { data?: { error?: { message?: string }; success?: boolean }; status?: number } };
    if (e?.response?.status === 403) return 'Only applicants can create applications. You may be logged in with a different role.';
    if (e?.response?.status === 401) return 'Your session has expired. Please log in again.';
    return e?.response?.data?.error?.message ?? 'Could not connect to the server';
  };

  const handleSaveDraft = async () => {
    const validationError = validate();
    if (validationError) { setError(validationError); return; }
    setError('');
    setSaving(true);
    try {
      const r = await createApplication(form);
      if (r.success && r.data) navigate(`/applications/${r.data.id}`);
      else setError(r.error?.message ?? 'Failed to save draft');
    } catch (err) {
      setError(extractError(err));
    } finally {
      setSaving(false);
    }
  };

  const handleSaveAndSubmit = async () => {
    const validationError = validate();
    if (validationError) { setError(validationError); return; }
    setError('');
    setSaving(true);
    try {
      const r = await createApplication(form);
      if (r.success && r.data) {
        await submitApplication(r.data.id);
        navigate('/dashboard');
      } else {
        setError(r.error?.message ?? 'Failed to create application');
      }
    } catch (err) {
      setError(extractError(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={styles.wrap}>
      <h1 style={styles.heading}>New License Application</h1>
      {error && <ErrorAlert message={error} />}

      <div style={styles.card}>
        <section style={styles.section}>
          <h3 style={styles.sectionTitle}>Institution Details</h3>

          <label style={styles.label}>Institution name *</label>
          <input style={styles.input} value={form.institutionName} onChange={set('institutionName')} required />

          <label style={styles.label}>Institution type *</label>
          <select style={styles.input} value={form.institutionType} onChange={set('institutionType')} required>
            <option value="">Select type…</option>
            <option>Commercial Bank</option>
            <option>Microfinance Institution</option>
            <option>Savings and Credit Cooperative</option>
            <option>Development Finance Institution</option>
            <option>Insurance Company</option>
            <option>Other</option>
          </select>

          <label style={styles.label}>Contact address</label>
          <input style={styles.input} value={form.contactAddress} onChange={set('contactAddress')} />

          <label style={styles.label}>Business description</label>
          <textarea
            style={{ ...styles.input, height: 120, resize: 'vertical' }}
            value={form.businessDescription}
            onChange={set('businessDescription')}
            placeholder="Describe your institution's purpose, target market, and planned services…"
          />
        </section>
      </div>

      <div style={styles.actions}>
        <button style={styles.draftBtn} onClick={handleSaveDraft} disabled={saving}>
          {saving ? 'Saving…' : 'Save as draft'}
        </button>
        <button style={styles.submitBtn} onClick={handleSaveAndSubmit} disabled={saving}>
          {saving ? 'Submitting…' : 'Save & submit'}
        </button>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  wrap: { maxWidth: 680 },
  heading: { fontSize: 24, color: BNR_DARK, marginBottom: 20 },
  card: {
    background: '#fff',
    borderRadius: 8,
    boxShadow: '0 1px 4px rgba(0,0,0,0.07)',
    padding: '24px 28px',
    marginBottom: 20,
  },
  section: {},
  sectionTitle: {
    margin: '0 0 16px',
    fontSize: 16,
    fontWeight: 700,
    color: BNR_DARK,
    borderBottom: `2px solid ${BNR_GOLD}`,
    paddingBottom: 8,
  },
  label: { display: 'block', fontSize: 13, fontWeight: 600, color: '#444', marginTop: 12, marginBottom: 4 },
  input: {
    width: '100%',
    border: '1px solid #ccc',
    borderRadius: 5,
    padding: '9px 12px',
    fontSize: 14,
    boxSizing: 'border-box',
    fontFamily: 'inherit',
  },
  actions: { display: 'flex', gap: 12 },
  draftBtn: {
    background: '#fff',
    border: `2px solid ${BNR_DARK}`,
    color: BNR_DARK,
    padding: '10px 22px',
    borderRadius: 5,
    fontWeight: 600,
    cursor: 'pointer',
    fontSize: 14,
  },
  submitBtn: {
    background: BNR_DARK,
    border: 'none',
    color: '#fff',
    padding: '10px 22px',
    borderRadius: 5,
    fontWeight: 600,
    cursor: 'pointer',
    fontSize: 14,
  },
};
