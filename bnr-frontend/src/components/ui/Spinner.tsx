// @author David NTAMAKEMWA

export default function Spinner({ label = 'Loading…' }: { label?: string }) {
  return (
    <div style={{ textAlign: 'center', padding: 48, color: '#888' }}>
      <div
        style={{
          width: 36,
          height: 36,
          border: '4px solid #ddd',
          borderTopColor: '#C8972A',
          borderRadius: '50%',
          animation: 'spin 0.8s linear infinite',
          margin: '0 auto 12px',
        }}
      />
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
      <div style={{ fontSize: 14 }}>{label}</div>
    </div>
  );
}
