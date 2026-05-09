// @author David NTAMAKEMWA

import axios from 'axios';
import { AuthUser } from '../types';

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export const login = async (email: string, password: string): Promise<AuthUser> => {
  const resp = await axios.post(`${BASE_URL}/api/auth/login`, { email, password });
  const d = resp.data.data;
  return {
    userId: d.userId,
    email: d.email,
    fullName: d.fullName,
    role: d.role,
    accessToken: d.accessToken,
    refreshToken: d.refreshToken,
  };
};

export const logout = async (refreshToken: string) => {
  await axios.post(`${BASE_URL}/api/auth/logout`, { refreshToken });
};
