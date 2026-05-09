// @author David NTAMAKEMWA

import React from 'react';

export default function EmptyState({
  title,
  description,
  action,
}: {
  title: string;
  description?: string;
  action?: React.ReactNode;
}) {
  return (
    <div
      style={{
        textAlign: 'center',
        padding: '64px 24px',
        color: '#6c757d',
      }}
    >
      <div style={{ fontSize: 40, marginBottom: 12 }}>📋</div>
      <div style={{ fontSize: 18, fontWeight: 600, marginBottom: 8 }}>{title}</div>
      {description && <div style={{ fontSize: 14, marginBottom: 20 }}>{description}</div>}
      {action}
    </div>
  );
}
