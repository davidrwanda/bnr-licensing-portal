// @author David NTAMAKEMWA

import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function Shell({ children }: { children: React.ReactNode }) {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();

  const handleSignOut = () => {
    signOut();
    navigate('/login');
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <header style={styles.header}>
        <div style={styles.headerInner}>
          <div style={styles.brand}>
            <div style={styles.logoCircle}>BNR</div>
            <div>
              <div style={styles.brandName}>NATIONAL BANK OF RWANDA</div>
              <div style={styles.brandSub}>Bank Licensing &amp; Compliance Portal</div>
            </div>
          </div>
          {user && (
            <div style={styles.userArea}>
              <span style={styles.userBadge}>{user.role}</span>
              <span style={styles.userName}>{user.fullName}</span>
              <button onClick={handleSignOut} style={styles.signOutBtn}>
                Sign out
              </button>
            </div>
          )}
        </div>
      </header>

      <nav style={styles.nav}>
        <div style={styles.navInner}>
          {user?.role === 'APPLICANT' && (
            <>
              <Link to="/dashboard" style={styles.navLink}>My Applications</Link>
              <Link to="/apply" style={styles.navLink}>New Application</Link>
            </>
          )}
          {user?.role === 'REVIEWER' && (
            <Link to="/dashboard" style={styles.navLink}>Applications</Link>
          )}
          {user?.role === 'APPROVER' && (
            <Link to="/dashboard" style={styles.navLink}>Awaiting Decision</Link>
          )}
          {user?.role === 'ADMIN' && (
            <>
              <Link to="/dashboard" style={styles.navLink}>All Applications</Link>
              <Link to="/admin/users" style={styles.navLink}>Users</Link>
              <Link to="/admin/audit" style={styles.navLink}>Audit Log</Link>
            </>
          )}
        </div>
      </nav>

      <main style={styles.main}>{children}</main>

      <footer style={styles.footer}>
        <span>© {new Date().getFullYear()} National Bank of Rwanda — Internal Use Only</span>
      </footer>
    </div>
  );
}

const BNR_GOLD = '#C8972A';
const BNR_DARK = '#5C1B1B';

const styles: Record<string, React.CSSProperties> = {
  header: {
    background: BNR_DARK,
    color: '#fff',
    padding: '0 24px',
  },
  headerInner: {
    maxWidth: 1200,
    margin: '0 auto',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    height: 68,
  },
  brand: {
    display: 'flex',
    alignItems: 'center',
    gap: 14,
  },
  logoCircle: {
    width: 44,
    height: 44,
    borderRadius: '50%',
    background: BNR_GOLD,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: 700,
    fontSize: 15,
    color: BNR_DARK,
    flexShrink: 0,
  },
  brandName: {
    fontWeight: 700,
    fontSize: 15,
    color: '#fff',
    letterSpacing: '0.04em',
  },
  brandSub: {
    fontSize: 12,
    color: BNR_GOLD,
    marginTop: 1,
  },
  userArea: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
  },
  userBadge: {
    background: BNR_GOLD,
    color: BNR_DARK,
    padding: '2px 10px',
    borderRadius: 12,
    fontSize: 12,
    fontWeight: 700,
  },
  userName: {
    fontSize: 14,
    color: '#e8d5b7',
  },
  signOutBtn: {
    background: 'transparent',
    border: '1px solid #e8d5b7',
    color: '#e8d5b7',
    padding: '4px 14px',
    borderRadius: 4,
    cursor: 'pointer',
    fontSize: 13,
  },
  nav: {
    background: BNR_GOLD,
  },
  navInner: {
    maxWidth: 1200,
    margin: '0 auto',
    display: 'flex',
    gap: 4,
    padding: '0 8px',
  },
  navLink: {
    display: 'inline-block',
    padding: '10px 16px',
    color: BNR_DARK,
    textDecoration: 'none',
    fontWeight: 600,
    fontSize: 14,
  },
  main: {
    flex: 1,
    maxWidth: 1200,
    margin: '0 auto',
    width: '100%',
    padding: '28px 24px',
    boxSizing: 'border-box',
  },
  footer: {
    background: '#f5f5f5',
    borderTop: '1px solid #ddd',
    textAlign: 'center',
    padding: '12px 0',
    fontSize: 12,
    color: '#888',
  },
};
