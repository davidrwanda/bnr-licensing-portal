// @author David NTAMAKEMWA

export default function ErrorAlert({ message }: { message: string }) {
  return (
    <div
      style={{
        background: '#f8d7da',
        border: '1px solid #f5c2c7',
        color: '#842029',
        borderRadius: 6,
        padding: '12px 16px',
        fontSize: 14,
        marginBottom: 16,
      }}
    >
      {message}
    </div>
  );
}
