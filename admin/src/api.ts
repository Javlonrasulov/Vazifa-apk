import axios, { isAxiosError } from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000/api/v1';

export const api = axios.create({ baseURL: API_URL });

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) return null;

  try {
    const { data } = await axios.post(`${API_URL}/auth/refresh`, { refreshToken });
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    return data.accessToken;
  } catch {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    return null;
  }
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as typeof error.config & { _retry?: boolean };
    const isAuthRequest =
      originalRequest?.url?.includes('/auth/admin/login') ||
      originalRequest?.url?.includes('/auth/refresh');

    if (
      isAxiosError(error) &&
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !isAuthRequest
    ) {
      originalRequest._retry = true;

      if (!refreshPromise) {
        refreshPromise = refreshAccessToken().finally(() => {
          refreshPromise = null;
        });
      }

      const newToken = await refreshPromise;
      if (newToken) {
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);
      }

      window.location.href = '/login';
    }

    return Promise.reject(error);
  },
);

export interface User {
  id: string;
  login: string;
  fullName: string;
  passwordPlain?: string | null;
  role: 'admin' | 'director' | 'employee';
  canAssignTasks: boolean;
  position: string | null;
  department: string | null;
  visibleDepartments: string[] | null;
  phone: string | null;
  deviceId: string | null;
  deviceApproved: boolean;
  pendingDeviceId: string | null;
  linkedDevices: Array<{
    id: string;
    name?: string;
    approved: boolean;
    linkedAt: string;
    lastLoginAt?: string;
  }> | null;
  notificationsEnabled: boolean;
  isActive: boolean;
  canAccessAdminPanel: boolean;
  adminPermissions: string[] | null;
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

export async function fetchFieldOptions(type: 'position' | 'department', q?: string) {
  if (type === 'department') {
    const { data } = await api.get<string[]>('/departments/names', {
      params: q?.trim() ? { q: q.trim() } : undefined,
    });
    return data;
  }
  const { data } = await api.get<string[]>('/users/options/positions', {
    params: q?.trim() ? { q: q.trim() } : undefined,
  });
  return data;
}

export interface Department {
  id: string;
  name: string;
  employeeCount: number;
  createdAt: string;
  updatedAt: string;
}

export async function fetchDepartments() {
  const { data } = await api.get<Department[]>('/departments');
  return data;
}

export async function createDepartment(name: string) {
  const { data } = await api.post<Department>('/departments', { name });
  return data;
}

export async function updateDepartment(id: string, name: string) {
  const { data } = await api.patch<Department>(`/departments/${id}`, { name });
  return data;
}

export async function deleteDepartment(id: string) {
  await api.delete(`/departments/${id}`);
}

export async function createUser(body: {
  login: string;
  password: string;
  fullName: string;
  role: 'admin' | 'director' | 'employee';
  canAssignTasks?: boolean;
  position?: string;
  department?: string;
  phone?: string;
  visibleDepartments?: string[];
  adminPermissions?: string[];
}) {
  const { data } = await api.post('/users', body);
  return data;
}

export async function updateUser(
  id: string,
  body: Partial<User> & { login?: string; password?: string; canAccessAdminPanel?: boolean },
) {
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

export function apiErrorMessage(error: unknown): string | null {
  if (!isAxiosError(error)) return null;
  const message = error.response?.data?.message;
  if (typeof message === 'string') return message;
  if (Array.isArray(message)) return message.join(', ');
  return null;
}
