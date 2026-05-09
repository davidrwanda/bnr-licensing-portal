// @author David NTAMAKEMWA

import { ApplicationStatus } from '../../types';

const COLOR_MAP: Record<ApplicationStatus, { bg: string; color: string }> = {
  DRAFT:           { bg: '#e9ecef', color: '#495057' },
  SUBMITTED:       { bg: '#cfe2ff', color: '#084298' },
  UNDER_REVIEW:    { bg: '#fff3cd', color: '#664d03' },
  INFO_REQUESTED:  { bg: '#ffe5d0', color: '#842029' },
  RESUBMITTED:     { bg: '#d1ecf1', color: '#0c5460' },
  REVIEW_COMPLETE: { bg: '#d4edda', color: '#155724' },
  APPROVED:        { bg: '#198754', color: '#fff' },
  REJECTED:        { bg: '#842029', color: '#fff' },
  WITHDRAWN:       { bg: '#6c757d', color: '#fff' },
};

export default function StatusBadge({ status }: { status: ApplicationStatus }) {
  const { bg, color } = COLOR_MAP[status] ?? { bg: '#eee', color: '#333' };
  return (
    <span
      style={{
        padding: '3px 10px',
        borderRadius: 12,
        fontSize: 12,
        fontWeight: 600,
        background: bg,
        color,
        whiteSpace: 'nowrap',
      }}
    >
      {status.replace(/_/g, ' ')}
    </span>
  );
}
