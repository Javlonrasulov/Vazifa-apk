import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000/api/v1';

export const api = axios.create({ baseURL: API_URL });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export interface User {
  id: string;
  login: string;
  fullName: string;
  role: 'admin' | 'director' | 'employee';
  position: string | null;
  department: string | null;
  phone: string | null;
  deviceId: string | null;
  deviceApproved: boolean;
  pendingDeviceId: string | null;
  notificationsEnabled: boolean;
  isActive: boolean;
  createdAt: string;
}

export async function adminLogin(login: string, password: string) {
  const { data } = await api.post('/auth/admin/login', { login, password });
  localStorage.setItem('accessToken', data.accessToken);
  localStorage.setItem('refreshToken', data.refreshToken);
  return data;
}

export async function fetchUsers() {
  const { data } = await api.get<User[]>('/users');
  return data;
}

export async function createUser(body: {
  login: string;
  password: string;
  fullName: string;
  role: 'director' | 'employee';
  position?: string;
  department?: string;
  phone?: string;
}) {
  const { data } = await api.post('/users', body);
  return data;
}

export async function updateUser(id: string, body: Partial<User>) {
  const { data } = await api.patch(`/users/${id}`, body);
  return data;
}

export async function deleteUser(id: string) {
  await api.delete(`/users/${id}`);
}

export async function resetPassword(id: string, newPassword: string) {
  await api.post(`/users/${id}/reset-password`, { newPassword });
}

export async function resetDevice(id: string) {
  const { data } = await api.post(`/users/${id}/reset-device`);
  return data;
}

export async function approveDevice(id: string, approve: boolean) {
  const { data } = await api.post(`/users/${id}/approve-device`, { approve });
  return data;
}

export function logout() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
}
