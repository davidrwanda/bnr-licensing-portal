// @author David NTAMAKEMWA

import { createContext, useContext, useState, ReactNode } from 'react';
import { AuthUser } from '../types';
import { logout as apiLogout } from '../api/auth';

interface AuthContextValue {
  user: AuthUser | null;
  signIn: (user: AuthUser) => void;
  signOut: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const raw = localStorage.getItem('auth');
    return raw ? JSON.parse(raw) : null;
  });

  const signIn = (u: AuthUser) => {
    localStorage.setItem('auth', JSON.stringify(u));
    setUser(u);
  };

  const signOut = () => {
    const raw = localStorage.getItem('auth');
    if (raw) {
      const u: AuthUser = JSON.parse(raw);
      apiLogout(u.refreshToken).catch(() => {});
    }
    localStorage.removeItem('auth');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, signIn, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
