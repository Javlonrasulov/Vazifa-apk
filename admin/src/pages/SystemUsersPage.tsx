import { useCallback, useEffect, useMemo, useState } from 'react';
import { isAxiosError } from 'axios';
import { Check, Edit2, Eye, EyeOff, Loader, Trash2, X } from 'lucide-react';
import {
  User,
  apiErrorMessage,
  createUser,
  deleteUser,
  fetchUsers,
  updateUser,
} from '../api';
import { useAppSettings } from '../i18n/LanguageContext';
import { INDIGO, useAdminTheme } from '../theme/adminTheme';

const DEFAULT_PASSWORD = '123456';

const PERMISSION_KEYS = ['employees', 'system_users'] as const;
type PermissionKey = (typeof PERMISSION_KEYS)[number];

const emptyForm = () => ({
  login: '',
  password: DEFAULT_PASSWORD,
  fullName: '',
  permissions: [...PERMISSION_KEYS] as PermissionKey[],
});

export default function SystemUsersPage() {
  const { t, isDark } = useAppSettings();
  const theme = useAdminTheme(isDark);
  const { D, txt, muted, border, surface, rowAlt, rowHov, tableBg } = theme;

  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [editUser, setEditUser] = useState<User | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [showPassword, setShowPassword] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');
  const [toast, setToast] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<User | null>(null);
  const [deleteSaving, setDeleteSaving] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  const permissionLabel = (key: PermissionKey) => {
    if (key === 'employees') return t('permEmployees');
    return t('permSystemUsers');
  };

  const roleLabel = (role: string) => {
    if (role === 'director') return t('director');
    if (role === 'employee') return t('employee');
    return t('admin');
  };

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await fetchUsers();
      setUsers(data.filter((u) => u.role === 'admin'));
    } catch (err) {
      if (isAxiosError(err) && !err.response) {
        setToast(t('networkError'));
      }
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!toast) return;
    const id = setTimeout(() => setToast(''), 3000);
    return () => clearTimeout(id);
  }, [toast]);

  const resetForm = () => {
    setEditUser(null);
    setSaveError('');
    setShowPassword(true);
    setForm(emptyForm());
  };

  const openEdit = (u: User) => {
    setEditUser(u);
    setSaveError('');
    setShowPassword(true);
    const perms = (u.adminPermissions ?? PERMISSION_KEYS) as PermissionKey[];
    setForm({
      login: u.login,
      password: u.passwordPlain ?? '',
      fullName: u.fullName,
      permissions: PERMISSION_KEYS.filter((k) => perms.includes(k)),
    });
  };

  const mapSaveError = (err: unknown): string => {
    if (isAxiosError(err)) {
      if (err.response?.status === 401) return t('sessionExpired');
      if (err.response?.status === 409) {
        const msg = (apiErrorMessage(err) ?? '').toLowerCase();
        if (msg.includes('apk')) return t('loginTakenApk');
        if (msg.includes('ism')) return t('nameTakenAdmin');
        return t('loginTaken');
      }
      if (!err.response) return t('networkError');
      return apiErrorMessage(err) ?? t('error');
    }
    return t('error');
  };

  const findDuplicateError = (excludeId?: string): string | null => {
    const name = form.fullName.trim().toLowerCase();
    const login = form.login.trim().toLowerCase();

    if (
      name &&
      users.some((u) => u.id !== excludeId && u.fullName.trim().toLowerCase() === name)
    ) {
      return t('nameTakenAdmin');
    }
    if (
      !excludeId &&
      login &&
      users.some((u) => u.login.toLowerCase() === login)
    ) {
      return t('loginTaken');
    }
    return null;
  };

  const togglePermission = (key: PermissionKey) => {
    setForm((prev) => ({
      ...prev,
      permissions: prev.permissions.includes(key)
        ? prev.permissions.filter((p) => p !== key)
        : [...prev.permissions, key],
    }));
  };

  const handleSave = async () => {
    setSaving(true);
    setSaveError('');
    try {
      if (!form.fullName.trim() || !form.login.trim()) {
        setSaveError(t('loginEmpty'));
        return;
      }
      const trimmedPassword = form.password.trim();
      if (!editUser && trimmedPassword.length < 6) {
        setSaveError(t('passwordMinError'));
        return;
      }
      if (editUser && trimmedPassword && trimmedPassword.length < 6) {
        setSaveError(t('passwordMinError'));
        return;
      }
      if (form.permissions.length === 0) {
        setSaveError(t('permRequired'));
        return;
      }

      const duplicateError = findDuplicateError(editUser?.id);
      if (duplicateError) {
        setSaveError(duplicateError);
        return;
      }

      if (editUser) {
        const initialPassword = (editUser.passwordPlain ?? '').trim();
        const passwordChanged = trimmedPassword !== initialPassword;
        const updated = await updateUser(editUser.id, {
          login: form.login.trim(),
          ...(passwordChanged && trimmedPassword ? { password: trimmedPassword } : {}),
          fullName: form.fullName.trim(),
          adminPermissions: form.permissions,
        });
        setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
        setToast(t('updated'));
      } else {
        const created = await createUser({
          login: form.login.trim(),
          password: trimmedPassword || DEFAULT_PASSWORD,
          fullName: form.fullName.trim(),
          role: 'admin',
          adminPermissions: form.permissions,
        });
        setUsers((prev) => [created, ...prev.filter((u) => u.id !== created.id)]);
        setToast(t('added'));
      }
      resetForm();
      await load();
    } catch (err) {
      setSaveError(mapSaveError(err));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget || deleteSaving) return;
    setDeleteSaving(true);
    setDeleteError('');
    try {
      await deleteUser(deleteTarget.id);
      if (editUser?.id === deleteTarget.id) resetForm();
      setDeleteTarget(null);
      setToast(t('deleted'));
      await load();
    } catch (err) {
      setDeleteError(mapSaveError(err));
    } finally {
      setDeleteSaving(false);
    }
  };

  const sortedUsers = useMemo(
    () => [...users].sort((a, b) => a.fullName.localeCompare(b.fullName)),
    [users],
  );

  const inputStyle: React.CSSProperties = {
    width: '100%',
    padding: '9px 12px',
    borderRadius: 10,
    border: `1px solid ${border}`,
    background: D ? 'rgba(255,255,255,0.04)' : '#f9fafb',
    color: txt,
    fontSize: 13,
    outline: 'none',
    boxSizing: 'border-box',
  };

  const labelStyle: React.CSSProperties = {
    fontSize: 11,
    fontWeight: 600,
    color: muted,
    marginBottom: 6,
    display: 'block',
    textTransform: 'uppercase',
    letterSpacing: '0.04em',
  };

  const cardStyle: React.CSSProperties = {
    background: tableBg,
    border: `1px solid ${border}`,
    borderRadius: 18,
    overflow: 'hidden',
    display: 'flex',
    flexDirection: 'column',
    minHeight: 0,
  };

  const renderDeleteModal = () =>
    deleteTarget && (
      <div
        style={{
          position: 'fixed',
          inset: 0,
          zIndex: 400,
          background: 'rgba(0,0,0,0.55)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backdropFilter: 'blur(4px)',
        }}
        onClick={() => {
          setDeleteTarget(null);
          setDeleteError('');
        }}
      >
        <div
          style={{
            background: tableBg,
            borderRadius: 20,
            border: `1px solid ${border}`,
            width: '100%',
            maxWidth: 360,
            margin: 16,
            padding: '28px 24px',
            boxShadow: D ? '0 24px 64px rgba(0,0,0,0.7)' : '0 24px 64px rgba(0,0,0,0.15)',
            textAlign: 'center',
          }}
          onClick={(e) => e.stopPropagation()}
        >
          <div style={{ fontSize: 15, fontWeight: 700, color: txt, marginBottom: 8 }}>{t('deleteTitle')}</div>
          <div style={{ fontSize: 13, color: muted, marginBottom: deleteError ? 14 : 24, lineHeight: 1.5 }}>
            <strong style={{ color: txt }}>{deleteTarget.fullName}</strong> — {t('deleteWarn')}
          </div>
          {deleteError && (
            <div
              style={{
                marginBottom: 16,
                padding: '10px 12px',
                borderRadius: 10,
                background: D ? 'rgba(239,68,68,0.12)' : 'rgba(239,68,68,0.08)',
                border: `1px solid ${D ? 'rgba(239,68,68,0.25)' : 'rgba(239,68,68,0.2)'}`,
                color: '#ef4444',
                fontSize: 12,
              }}
            >
              {deleteError}
            </div>
          )}
          <div style={{ display: 'flex', gap: 10 }}>
            <button
              type="button"
              onClick={() => {
                setDeleteTarget(null);
                setDeleteError('');
              }}
              style={{
                flex: 1,
                padding: 10,
                borderRadius: 11,
                border: `1px solid ${border}`,
                background: 'transparent',
                color: muted,
                fontSize: 13,
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              {t('cancel')}
            </button>
            <button
              type="button"
              onClick={handleDelete}
              disabled={deleteSaving}
              style={{
                flex: 1,
                padding: 10,
                borderRadius: 11,
                border: 'none',
                background: deleteSaving ? (D ? '#374151' : '#d1d5db') : '#ef4444',
                color: '#fff',
                fontSize: 13,
                fontWeight: 700,
                cursor: deleteSaving ? 'not-allowed' : 'pointer',
              }}
            >
              {deleteSaving ? t('loginLoading') : t('deleteBtn')}
            </button>
          </div>
        </div>
      </div>
    );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16, height: '100%' }}>
      {renderDeleteModal()}

      {toast && (
        <div
          style={{
            position: 'fixed',
            bottom: 24,
            right: 24,
            zIndex: 500,
            padding: '12px 18px',
            borderRadius: 12,
            background: INDIGO,
            color: '#fff',
            fontSize: 13,
            fontWeight: 600,
            boxShadow: '0 8px 24px rgba(99,102,241,0.4)',
          }}
        >
          {toast}
        </div>
      )}

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(300px, 380px) 1fr',
          gap: 16,
          alignItems: 'stretch',
          minHeight: 'calc(100vh - 180px)',
        }}
        className="system-users-grid"
      >
        {/* Chap: yangi foydalanuvchi formasi */}
        <div style={cardStyle}>
          <div
            style={{
              padding: '16px 18px',
              borderBottom: `1px solid ${border}`,
              background: D ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.02)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <span style={{ fontSize: 14, fontWeight: 700, color: txt }}>
              {editUser ? t('dialogEdit') : t('systemUserNew')}
            </span>
            {editUser && (
              <button
                type="button"
                onClick={resetForm}
                title={t('cancel')}
                style={{
                  width: 28,
                  height: 28,
                  borderRadius: 8,
                  border: 'none',
                  background: D ? 'rgba(255,255,255,0.06)' : '#f3f4f6',
                  color: muted,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <X size={14} />
              </button>
            )}
          </div>

          <div style={{ padding: '18px', display: 'flex', flexDirection: 'column', gap: 14, flex: 1 }}>
            {saveError && (
              <div
                style={{
                  padding: '10px 12px',
                  borderRadius: 10,
                  background: D ? 'rgba(239,68,68,0.12)' : 'rgba(239,68,68,0.08)',
                  border: `1px solid ${D ? 'rgba(239,68,68,0.25)' : 'rgba(239,68,68,0.2)'}`,
                  color: '#ef4444',
                  fontSize: 12,
                }}
              >
                {saveError}
              </div>
            )}

            <div>
              <label style={labelStyle}>{t('fullName')}</label>
              <input
                style={inputStyle}
                value={form.fullName}
                onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              />
            </div>

            <div>
              <label style={labelStyle}>{t('login')}</label>
              <input
                style={inputStyle}
                value={form.login}
                onChange={(e) => setForm({ ...form, login: e.target.value })}
              />
            </div>

            <div>
              <label style={labelStyle}>{t('password')}</label>
              <div style={{ position: 'relative' }}>
                <input
                  style={{ ...inputStyle, paddingRight: 40 }}
                  type={showPassword ? 'text' : 'password'}
                  value={form.password}
                  placeholder={editUser ? undefined : t('passwordHint')}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                />
                <button
                  type="button"
                  title={showPassword ? t('hidePassword') : t('showPassword')}
                  onClick={() => setShowPassword((v) => !v)}
                  style={{
                    position: 'absolute',
                    right: 8,
                    top: '50%',
                    transform: 'translateY(-50%)',
                    width: 28,
                    height: 28,
                    borderRadius: 8,
                    border: 'none',
                    background: 'transparent',
                    color: muted,
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  {showPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                </button>
              </div>
              {editUser ? (
                <div style={{ fontSize: 11, color: muted, marginTop: 6 }}>{t('passwordOptionalHint')}</div>
              ) : (
                <div style={{ fontSize: 11, color: muted, marginTop: 6 }}>{t('defaultPasswordHint')}</div>
              )}
            </div>

            <div>
              <label style={labelStyle}>{t('permissionsTitle')}</label>
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: '1fr 1fr',
                  gap: 8,
                  maxHeight: 200,
                  overflowY: 'auto',
                  padding: '10px 12px',
                  borderRadius: 12,
                  border: `1px solid ${border}`,
                  background: surface,
                }}
              >
                {PERMISSION_KEYS.map((key) => {
                  const checked = form.permissions.includes(key);
                  return (
                    <label
                      key={key}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 8,
                        fontSize: 12,
                        color: txt,
                        cursor: 'pointer',
                        userSelect: 'none',
                      }}
                    >
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => togglePermission(key)}
                        style={{ accentColor: INDIGO, width: 15, height: 15 }}
                      />
                      <span>{permissionLabel(key)}</span>
                    </label>
                  );
                })}
              </div>
            </div>

            <button
              type="button"
              onClick={handleSave}
              disabled={saving}
              style={{
                marginTop: 'auto',
                width: '100%',
                padding: '11px 16px',
                borderRadius: 11,
                border: 'none',
                background: saving ? (D ? '#374151' : '#d1d5db') : INDIGO,
                color: '#fff',
                fontSize: 13,
                fontWeight: 700,
                cursor: saving ? 'not-allowed' : 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 7,
                boxShadow: saving ? 'none' : '0 4px 14px rgba(99,102,241,0.35)',
              }}
            >
              <Check size={14} />
              {saving ? t('loginLoading') : editUser ? t('save') : t('add')}
            </button>
          </div>
        </div>

        {/* O'ng: foydalanuvchilar ro'yxati */}
        <div style={cardStyle}>
          <div
            style={{
              padding: '16px 18px',
              borderBottom: `1px solid ${border}`,
              background: D ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.02)',
            }}
          >
            <span style={{ fontSize: 14, fontWeight: 700, color: txt }}>{t('systemUsersList')}</span>
            <span style={{ fontSize: 12, color: muted, marginLeft: 10 }}>
              {sortedUsers.length} {t('empUnit')}
            </span>
          </div>

          {loading ? (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 10,
                padding: '64px 0',
                color: muted,
                flex: 1,
              }}
            >
              <Loader size={20} className="animate-spin" />
              {t('loading')}
            </div>
          ) : sortedUsers.length === 0 ? (
            <div style={{ padding: '48px 24px', textAlign: 'center', color: muted, fontSize: 13 }}>
              {t('systemUsersEmpty')}
            </div>
          ) : (
            <div style={{ overflowX: 'auto', flex: 1 }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: 480 }}>
                <thead>
                  <tr
                    style={{
                      background: D ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.022)',
                      borderBottom: `1px solid ${border}`,
                    }}
                  >
                    {[t('name'), t('login'), t('role'), t('permissionsTitle'), t('actions')].map((label) => (
                      <th
                        key={label}
                        style={{
                          padding: '11px 14px',
                          textAlign: 'left',
                          fontSize: 11,
                          fontWeight: 700,
                          color: muted,
                          letterSpacing: '0.04em',
                          textTransform: 'uppercase',
                          whiteSpace: 'nowrap',
                        }}
                      >
                        {label}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {sortedUsers.map((u, i) => {
                    const perms = (u.adminPermissions ?? PERMISSION_KEYS) as PermissionKey[];
                    const isSelected = editUser?.id === u.id;
                    return (
                      <tr
                        key={u.id}
                        style={{
                          background: isSelected
                            ? D
                              ? 'rgba(99,102,241,0.12)'
                              : 'rgba(99,102,241,0.06)'
                            : i % 2 === 0
                              ? tableBg
                              : rowAlt,
                          borderBottom:
                            i < sortedUsers.length - 1
                              ? `1px solid ${D ? 'rgba(255,255,255,0.045)' : 'rgba(0,0,0,0.045)'}`
                              : 'none',
                          transition: 'background 0.13s',
                        }}
                        onMouseEnter={(e) => {
                          if (!isSelected) e.currentTarget.style.background = rowHov;
                        }}
                        onMouseLeave={(e) => {
                          if (!isSelected) {
                            e.currentTarget.style.background = i % 2 === 0 ? tableBg : rowAlt;
                          }
                        }}
                      >
                        <td style={{ padding: '9px 14px' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                            <div
                              style={{
                                width: 32,
                                height: 32,
                                borderRadius: 10,
                                flexShrink: 0,
                                background: D ? 'rgba(99,102,241,0.15)' : 'rgba(99,102,241,0.08)',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                fontSize: 10,
                                fontWeight: 700,
                                color: INDIGO,
                              }}
                            >
                              {u.fullName.slice(0, 2).toUpperCase()}
                            </div>
                            <span style={{ fontSize: 13, fontWeight: 600, color: txt }}>{u.fullName}</span>
                          </div>
                        </td>
                        <td style={{ padding: '9px 14px', fontSize: 13, color: muted }}>{u.login}</td>
                        <td style={{ padding: '9px 14px' }}>
                          <span
                            style={{
                              fontSize: 11,
                              fontWeight: 600,
                              padding: '3px 8px',
                              borderRadius: 6,
                              background: D ? 'rgba(255,255,255,0.07)' : '#f3f4f6',
                              color: muted,
                            }}
                          >
                            {roleLabel(u.role)}
                          </span>
                        </td>
                        <td style={{ padding: '9px 14px' }}>
                          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                            {PERMISSION_KEYS.filter((k) => perms.includes(k)).map((k) => (
                              <span
                                key={k}
                                style={{
                                  fontSize: 10,
                                  fontWeight: 600,
                                  padding: '2px 7px',
                                  borderRadius: 6,
                                  background: D ? 'rgba(255,255,255,0.07)' : '#f3f4f6',
                                  color: muted,
                                }}
                              >
                                {permissionLabel(k)}
                              </span>
                            ))}
                          </div>
                        </td>
                        <td style={{ padding: '9px 14px' }}>
                          <div style={{ display: 'flex', gap: 4 }}>
                            <button
                              type="button"
                              title={t('edit')}
                              onClick={() => openEdit(u)}
                              style={{
                                width: 30,
                                height: 30,
                                borderRadius: 8,
                                border: 'none',
                                background: D ? 'rgba(99,102,241,0.15)' : 'rgba(99,102,241,0.08)',
                                color: INDIGO,
                                cursor: 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                              }}
                            >
                              <Edit2 size={14} />
                            </button>
                            <button
                              type="button"
                              title={t('delete')}
                              onClick={() => {
                                setDeleteTarget(u);
                                setDeleteError('');
                              }}
                              style={{
                                width: 30,
                                height: 30,
                                borderRadius: 8,
                                border: 'none',
                                background: D ? 'rgba(239,68,68,0.12)' : 'rgba(239,68,68,0.08)',
                                color: '#ef4444',
                                cursor: 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                              }}
                            >
                              <Trash2 size={14} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      <style>{`
        @media (max-width: 900px) {
          .system-users-grid {
            grid-template-columns: 1fr !important;
            min-height: auto !important;
          }
        }
      `}</style>
    </div>
  );
}
