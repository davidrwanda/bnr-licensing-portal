// @author David NTAMAKEMWA

import axios, { AxiosInstance, InternalAxiosRequestConfig } from 'axios';

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

const client: AxiosInstance = axios.create({ baseURL: BASE_URL });

client.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const raw = localStorage.getItem('auth');
  if (raw) {
    const auth = JSON.parse(raw);
    config.headers.Authorization = `Bearer ${auth.accessToken}`;
  }
  return config;
});

client.interceptors.response.use(
  (res) => res,
  async (error) => {
    if (error.response?.status === 401) {
      const raw = localStorage.getItem('auth');
      if (raw) {
        const auth = JSON.parse(raw);
        try {
          const resp = await axios.post(`${BASE_URL}/api/auth/refresh`, {
            refreshToken: auth.refreshToken,
          });
          const newAuth = { ...auth, accessToken: resp.data.data.accessToken };
          localStorage.setItem('auth', JSON.stringify(newAuth));
          error.config.headers.Authorization = `Bearer ${newAuth.accessToken}`;
          return client.request(error.config);
        } catch {
          localStorage.removeItem('auth');
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

export default client;
