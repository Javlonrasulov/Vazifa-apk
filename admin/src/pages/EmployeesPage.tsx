import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { isAxiosError } from 'axios';
import {
  AlertTriangle,
  Check,
  Edit2,
  Eye,
  EyeOff,
  Loader,
  Lock,
  Plus,
  Search,
  Trash2,
  X,
} from 'lucide-react';
import {
  User,
  apiErrorMessage,
  createUser,
  deleteUser,
  fetchFieldOptions,
  fetchUsers,
  resetPassword,
  updateUser,
} from '../api';
import { useAppSettings } from '../i18n/LanguageContext';
import { INDIGO, useAdminTheme } from '../theme/adminTheme';
import { displayPhone, formatUzPhone, PHONE_PLACEHOLDER, phoneDigits, phoneForSave, phonesSame } from '../utils/phone';

const DEFAULT_EMPLOYEE_PASSWORD = '123456';

export default function EmployeesPage() {
  const { t, isDark } = useAppSettings();
  const theme = useAdminTheme(isDark);
  const { D, txt, muted, border, surface, rowAlt, rowHov, tableBg } = theme;

  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editUser, setEditUser] = useState<User | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<User | null>(null);
  const [deleteError, setDeleteError] = useState('');
  const [deleteSaving, setDeleteSaving] = useState(false);
  const [passwordTarget, setPasswordTarget] = useState<User | null>(null);
  const [newPassword, setNewPassword] = useState('');
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [passwordError, setPasswordError] = useState('');
  const [passwordSaving, setPasswordSaving] = useState(false);
  const passwordInputRef = useRef<HTMLInputElement>(null);
  const [toast, setToast] = useState('');
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');
  const [showFormPassword, setShowFormPassword] = useState(true);
  const [positionOptions, setPositionOptions] = useState<string[]>([]);
  const [departmentOptions, setDepartmentOptions] = useState<string[]>([]);
  const [form, setForm] = useState({
    login: '',
    password: '',
    fullName: '',
    role: 'employee' as 'director' | 'employee',
    canAssignTasks: false,
    position: '',
    department: '',
    phone: '',
  });

  const roleLabel = (role: string) => {
    if (role === 'director') return t('director');
    if (role === 'employee') return t('employee');
    return t('admin');
  };

  const loadFieldOptions = useCallback(async (positionQ?: string, departmentQ?: string) => {
    const [positions, departments] = await Promise.all([
      fetchFieldOptions('position', positionQ),
      fetchFieldOptions('department', departmentQ),
    ]);
    setPositionOptions(positions);
    setDepartmentOptions(departments);
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await fetchUsers();
      setUsers(data.filter((u) => u.role !== 'admin'));
    } catch (err) {
      if (isAxiosError(err) && !err.response) {
        setToast(t('networkError'));
      }
    } finally {
      setLoading(false);
    }
  }, [t]);

  const mapSaveError = (err: unknown): string => {
    if (isAxiosError(err)) {
      if (err.response?.status === 401) return t('sessionExpired');
      if (err.response?.status === 409) {
        const msg = (apiErrorMessage(err) ?? '').toLowerCase();
        if (msg.includes('telefon')) return t('phoneTaken');
        if (msg.includes('ism')) return t('nameTaken');
        return t('loginTaken');
      }
      if (!err.response) return t('networkError');
      return apiErrorMessage(err) ?? t('error');
    }
    return t('error');
  };

  const findDuplicateError = (excludeId?: string): string | null => {
    const name = form.fullName.trim().toLowerCase();
    const phone = phoneForSave(form.phone);
    const login = form.login.trim().toLowerCase();

    if (
      name &&
      users.some((u) => u.id !== excludeId && u.fullName.trim().toLowerCase() === name)
    ) {
      return t('nameTaken');
    }
    if (
      phone &&
      users.some((u) => u.id !== excludeId && u.phone && phonesSame(u.phone, phone))
    ) {
      return t('phoneTaken');
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

  useEffect(() => {
    load();
    loadFieldOptions();
  }, [load, loadFieldOptions]);

  useEffect(() => {
    if (!toast) return;
    const id = setTimeout(() => setToast(''), 3000);
    return () => clearTimeout(id);
  }, [toast]);

  useEffect(() => {
    if (!passwordTarget) return;
    const id = setTimeout(() => passwordInputRef.current?.focus(), 50);
    return () => clearTimeout(id);
  }, [passwordTarget]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return users;
    return users.filter(
      (u) =>
        u.fullName.toLowerCase().includes(q) ||
        u.login.toLowerCase().includes(q) ||
        (u.department ?? '').toLowerCase().includes(q),
    );
  }, [users, search]);

  const openCreate = () => {
    setEditUser(null);
    setSaveError('');
    setShowFormPassword(true);
    setForm({
      login: '',
      password: DEFAULT_EMPLOYEE_PASSWORD,
      fullName: '',
      role: 'employee',
      canAssignTasks: false,
      position: '',
      department: '',
      phone: '+998 ',
    });
    setDialogOpen(true);
    void loadFieldOptions();
  };

  const openEdit = (u: User) => {
    setEditUser(u);
    setSaveError('');
    setShowFormPassword(true);
    setForm({
      login: u.login,
      password: '',
      fullName: u.fullName,
      role: u.role as 'director' | 'employee',
      canAssignTasks: u.canAssignTasks ?? u.role === 'director',
      position: u.position ?? '',
      department: u.department ?? '',
      phone: displayPhone(u.phone ?? ''),
    });
    setDialogOpen(true);
    void loadFieldOptions(u.position ?? '', u.department ?? '');
  };

  const handleSave = async () => {
    setSaving(true);
    setSaveError('');
    try {
      if (form.password && form.password.length < 6) {
        setSaveError(t('passwordMinError'));
        return;
      }
      if (!form.fullName.trim()) {
        setSaveError(t('loginEmpty'));
        return;
      }

      const duplicateError = findDuplicateError(editUser?.id);
      if (duplicateError) {
        setSaveError(duplicateError);
        return;
      }

      if (editUser) {
        const updated = await updateUser(editUser.id, {
          login: form.login.trim(),
          ...(form.password ? { password: form.password } : {}),
          fullName: form.fullName.trim(),
          role: form.role,
          canAssignTasks: form.canAssignTasks,
          position: form.position || null,
          department: form.department || null,
          phone: phoneForSave(form.phone),
        });
        setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
        setToast(t('updated'));
      } else {
        if (!form.login.trim()) {
          setSaveError(t('loginEmpty'));
          return;
        }
        const created = await createUser({
          login: form.login,
          password: form.password || DEFAULT_EMPLOYEE_PASSWORD,
          fullName: form.fullName.trim(),
          role: form.role,
          canAssignTasks: form.canAssignTasks,
          position: form.position || undefined,
          department: form.department || undefined,
          phone: phoneForSave(form.phone) || undefined,
        });
        setUsers((prev) => [created, ...prev.filter((u) => u.id !== created.id)]);
        setSearch('');
        setToast(t('added'));
      }
      setDialogOpen(false);
      await load();
      await loadFieldOptions();
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
      setDeleteTarget(null);
      setToast(t('deleted'));
      load();
    } catch (err) {
      if (isAxiosError(err) && err.response?.status === 401) {
        setDeleteError(t('sessionExpired'));
      } else {
        setDeleteError(apiErrorMessage(err) ?? t('error'));
      }
    } finally {
      setDeleteSaving(false);
    }
  };

  const openPasswordModal = (u: User) => {
    setPasswordTarget(u);
    setNewPassword('');
    setShowNewPassword(false);
    setPasswordError('');
  };

  const closePasswordModal = () => {
    setPasswordTarget(null);
    setNewPassword('');
    setShowNewPassword(false);
    setPasswordError('');
  };

  const handleConfirmPassword = async () => {
    if (!passwordTarget) return;
    if (newPassword.length < 6) {
      setPasswordError(t('passwordMinError'));
      return;
    }
    setPasswordSaving(true);
    setPasswordError('');
    try {
      await resetPassword(passwordTarget.id, newPassword);
      closePasswordModal();
      setToast(t('passwordChanged'));
    } catch {
      setPasswordError(t('error'));
    } finally {
      setPasswordSaving(false);
    }
  };

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

  const segmentedBtn = (active: boolean): React.CSSProperties => ({
    flex: 1,
    padding: '9px 12px',
    borderRadius: 10,
    border: `1px solid ${active ? INDIGO : border}`,
    background: active ? (D ? 'rgba(99,102,241,0.15)' : 'rgba(99,102,241,0.08)') : D ? 'rgba(255,255,255,0.02)' : '#fff',
    color: active ? INDIGO : muted,
    fontSize: 13,
    fontWeight: active ? 700 : 500,
    cursor: 'pointer',
    transition: 'all 0.15s',
  });

  const setFormRole = (role: 'director' | 'employee') => {
    setForm((prev) => ({
      ...prev,
      role,
      canAssignTasks: role === 'director' ? true : false,
    }));
  };

  const renderModal = () =>
    dialogOpen && (
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
        onClick={() => setDialogOpen(false)}
      >
        <div
          style={{
            background: tableBg,
            borderRadius: 20,
            border: `1px solid ${border}`,
            width: '100%',
            maxWidth: 440,
            margin: 16,
            padding: '24px 22px',
            boxShadow: D ? '0 24px 64px rgba(0,0,0,0.7)' : '0 24px 64px rgba(0,0,0,0.15)',
          }}
          onClick={(e) => e.stopPropagation()}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
            <span style={{ fontSize: 16, fontWeight: 700, color: txt }}>
              {editUser ? t('dialogEdit') : t('dialogNew')}
            </span>
            <button
              type="button"
              onClick={() => setDialogOpen(false)}
              style={{
                width: 32,
                height: 32,
                borderRadius: 10,
                border: 'none',
                background: D ? 'rgba(255,255,255,0.06)' : '#f3f4f6',
                color: muted,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <X size={16} />
            </button>
          </div>

          {saveError && (
            <div
              style={{
                marginBottom: 14,
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

          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
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
                  type={showFormPassword ? 'text' : 'password'}
                  value={form.password}
                  placeholder={editUser ? t('passwordOptionalHint') : t('passwordHint')}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                />
                <button
                  type="button"
                  title={showFormPassword ? t('hidePassword') : t('showPassword')}
                  onClick={() => setShowFormPassword((v) => !v)}
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
                  {showFormPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                </button>
              </div>
              {editUser ? (
                <div style={{ fontSize: 11, color: muted, marginTop: 6 }}>{t('passwordOptionalHint')}</div>
              ) : (
                <div style={{ fontSize: 11, color: muted, marginTop: 6 }}>{t('defaultPasswordHint')}</div>
              )}
            </div>
            <div>
              <label style={labelStyle}>{t('fullName')}</label>
              <input
                style={inputStyle}
                value={form.fullName}
                onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              />
            </div>
            <div>
              <label style={labelStyle}>{t('role')}</label>
              <div style={{ display: 'flex', gap: 8 }}>
                <button type="button" style={segmentedBtn(form.role === 'employee')} onClick={() => setFormRole('employee')}>
                  {t('employee')}
                </button>
                <button type="button" style={segmentedBtn(form.role === 'director')} onClick={() => setFormRole('director')}>
                  {t('director')}
                </button>
              </div>
            </div>
            <div>
              <label style={labelStyle}>{t('canAssignTasks')}</label>
              <div style={{ display: 'flex', gap: 8 }}>
                <button
                  type="button"
                  style={segmentedBtn(form.canAssignTasks)}
                  onClick={() => setForm({ ...form, canAssignTasks: true })}
                >
                  {t('yes')}
                </button>
                <button
                  type="button"
                  style={segmentedBtn(!form.canAssignTasks)}
                  onClick={() => setForm({ ...form, canAssignTasks: false })}
                >
                  {t('no')}
                </button>
              </div>
            </div>
            <div>
              <label style={labelStyle}>{t('position')}</label>
              <input
                style={inputStyle}
                list="position-options"
                value={form.position}
                onChange={(e) => {
                  setForm({ ...form, position: e.target.value });
                  void loadFieldOptions(e.target.value, form.department);
                }}
                onFocus={() => void loadFieldOptions(form.position, form.department)}
              />
              <datalist id="position-options">
                {positionOptions.map((item) => (
                  <option key={item} value={item} />
                ))}
              </datalist>
            </div>
            <div>
              <label style={labelStyle}>{t('department')}</label>
              <input
                style={inputStyle}
                list="department-options"
                value={form.department}
                onChange={(e) => {
                  setForm({ ...form, department: e.target.value });
                  void loadFieldOptions(form.position, e.target.value);
                }}
                onFocus={() => void loadFieldOptions(form.position, form.department)}
              />
              <datalist id="department-options">
                {departmentOptions.map((item) => (
                  <option key={item} value={item} />
                ))}
              </datalist>
            </div>
            <div>
              <label style={labelStyle}>{t('phone')}</label>
              <input
                style={inputStyle}
                type="tel"
                inputMode="numeric"
                autoComplete="tel"
                placeholder={PHONE_PLACEHOLDER}
                value={form.phone}
                onFocus={() => {
                  if (!form.phone.trim()) setForm({ ...form, phone: '+998 ' });
                }}
                onChange={(e) => {
                  setForm({ ...form, phone: formatUzPhone(phoneDigits(e.target.value)) });
                }}
              />
            </div>
          </div>

          <div style={{ display: 'flex', gap: 10, marginTop: 22 }}>
            <button
              type="button"
              onClick={() => setDialogOpen(false)}
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
              onClick={handleSave}
              disabled={saving}
              style={{
                flex: 2,
                padding: 10,
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
              {saving ? t('loginLoading') : t('save')}
            </button>
          </div>
        </div>
      </div>
    );

  const renderPasswordModal = () =>
    passwordTarget && (
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
        onClick={closePasswordModal}
      >
        <div
          style={{
            background: tableBg,
            borderRadius: 20,
            border: `1px solid ${border}`,
            width: '100%',
            maxWidth: 400,
            margin: 16,
            padding: '24px 22px',
            boxShadow: D ? '0 24px 64px rgba(0,0,0,0.7)' : '0 24px 64px rgba(0,0,0,0.15)',
          }}
          onClick={(e) => e.stopPropagation()}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <div
                style={{
                  width: 40,
                  height: 40,
                  borderRadius: 12,
                  background: D ? 'rgba(245,158,11,0.15)' : 'rgba(245,158,11,0.1)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }}
              >
                <Lock size={18} color="#f59e0b" />
              </div>
              <span style={{ fontSize: 16, fontWeight: 700, color: txt }}>{t('passwordModalTitle')}</span>
            </div>
            <button
              type="button"
              onClick={closePasswordModal}
              style={{
                width: 32,
                height: 32,
                borderRadius: 10,
                border: 'none',
                background: D ? 'rgba(255,255,255,0.06)' : '#f3f4f6',
                color: muted,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <X size={16} />
            </button>
          </div>

          <div
            style={{
              marginBottom: 16,
              padding: '10px 12px',
              borderRadius: 10,
              background: D ? 'rgba(255,255,255,0.04)' : '#f9fafb',
              border: `1px solid ${border}`,
            }}
          >
            <div style={{ fontSize: 13, fontWeight: 600, color: txt }}>{passwordTarget.fullName}</div>
            <div style={{ fontSize: 12, color: muted, marginTop: 2 }}>{passwordTarget.login}</div>
          </div>

          {passwordError && (
            <div
              style={{
                marginBottom: 14,
                padding: '10px 12px',
                borderRadius: 10,
                background: D ? 'rgba(239,68,68,0.12)' : 'rgba(239,68,68,0.08)',
                border: `1px solid ${D ? 'rgba(239,68,68,0.25)' : 'rgba(239,68,68,0.2)'}`,
                color: '#ef4444',
                fontSize: 12,
              }}
            >
              {passwordError}
            </div>
          )}

          <div>
            <label style={labelStyle}>{t('password')}</label>
            <div style={{ position: 'relative' }}>
              <input
                ref={passwordInputRef}
                style={{ ...inputStyle, paddingRight: 40 }}
                type={showNewPassword ? 'text' : 'password'}
                value={newPassword}
                placeholder={t('passwordHint')}
                onChange={(e) => {
                  setNewPassword(e.target.value);
                  if (passwordError) setPasswordError('');
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') handleConfirmPassword();
                }}
              />
              <button
                type="button"
                title={showNewPassword ? t('hidePassword') : t('showPassword')}
                onClick={() => setShowNewPassword((v) => !v)}
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
                {showNewPassword ? <EyeOff size={15} /> : <Eye size={15} />}
              </button>
            </div>
            <div style={{ fontSize: 11, color: muted, marginTop: 6 }}>{t('passwordPrompt')}</div>
          </div>

          <div style={{ display: 'flex', gap: 10, marginTop: 22 }}>
            <button
              type="button"
              onClick={closePasswordModal}
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
              onClick={handleConfirmPassword}
              disabled={passwordSaving}
              style={{
                flex: 2,
                padding: 10,
                borderRadius: 11,
                border: 'none',
                background: passwordSaving ? (D ? '#374151' : '#d1d5db') : '#f59e0b',
                color: '#fff',
                fontSize: 13,
                fontWeight: 700,
                cursor: passwordSaving ? 'not-allowed' : 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 7,
                boxShadow: passwordSaving ? 'none' : '0 4px 14px rgba(245,158,11,0.35)',
              }}
            >
              <Lock size={14} />
              {passwordSaving ? t('loginLoading') : t('save')}
            </button>
          </div>
        </div>
      </div>
    );

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
          <div
            style={{
              width: 52,
              height: 52,
              borderRadius: 16,
              margin: '0 auto 16px',
              background: 'rgba(239,68,68,0.12)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <AlertTriangle size={22} color="#ef4444" />
          </div>
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
                boxShadow: deleteSaving ? 'none' : '0 4px 14px rgba(239,68,68,0.35)',
              }}
            >
              {deleteSaving ? t('loginLoading') : t('deleteBtn')}
            </button>
          </div>
        </div>
      </div>
    );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {renderModal()}
      {renderPasswordModal()}
      {renderDeleteModal()}

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
        <div>
          <span style={{ fontSize: 17, fontWeight: 700, color: txt }}>{t('employeesTitle')}</span>
          <span style={{ fontSize: 12, color: muted, fontWeight: 400, marginLeft: 10 }}>
            {filtered.length} {t('empUnit')}
          </span>
        </div>

        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              background: surface,
              border: `1px solid ${border}`,
              borderRadius: 11,
              padding: '7px 13px',
              minWidth: 220,
            }}
          >
            <Search size={13} color={muted} />
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder={t('search')}
              style={{
                flex: 1,
                border: 'none',
                background: 'transparent',
                fontSize: 13,
                color: txt,
                outline: 'none',
              }}
            />
          </div>

          <button
            type="button"
            onClick={openCreate}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 7,
              padding: '8px 16px',
              borderRadius: 11,
              border: 'none',
              background: INDIGO,
              color: '#fff',
              fontSize: 13,
              fontWeight: 600,
              cursor: 'pointer',
              boxShadow: '0 4px 14px rgba(99,102,241,0.35)',
            }}
          >
            <Plus size={14} />
            {t('add')}
          </button>
        </div>
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
          }}
        >
          <Loader size={20} className="animate-spin" />
          {t('loading')}
        </div>
      ) : (
        <div
          style={{
            background: tableBg,
            border: `1px solid ${border}`,
            borderRadius: 18,
            overflow: 'hidden',
          }}
        >
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: 620 }}>
              <thead>
                <tr
                  style={{
                    background: D ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.022)',
                    borderBottom: `1px solid ${border}`,
                  }}
                >
                  {[t('name'), t('login'), t('role'), t('department'), t('actions')].map((label) => (
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
                {filtered.map((u, i) => (
                  <tr
                    key={u.id}
                    style={{
                      background: i % 2 === 0 ? tableBg : rowAlt,
                      borderBottom:
                        i < filtered.length - 1
                          ? `1px solid ${D ? 'rgba(255,255,255,0.045)' : 'rgba(0,0,0,0.045)'}`
                          : 'none',
                      transition: 'background 0.13s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = rowHov;
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = i % 2 === 0 ? tableBg : rowAlt;
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
                          background:
                            u.role === 'director'
                              ? D
                                ? 'rgba(99,102,241,0.2)'
                                : 'rgba(99,102,241,0.1)'
                              : D
                                ? 'rgba(255,255,255,0.07)'
                                : '#f3f4f6',
                          color: u.role === 'director' ? INDIGO : muted,
                        }}
                      >
                        {roleLabel(u.role)}
                      </span>
                    </td>
                    <td style={{ padding: '9px 14px', fontSize: 13, color: muted }}>{u.department ?? '—'}</td>
                    <td style={{ padding: '9px 14px' }}>
                      <div style={{ display: 'flex', gap: 4 }}>
                        <button type="button" title={t('edit')} onClick={() => openEdit(u)} style={actionBtn(D, INDIGO)}>
                          <Edit2 size={14} />
                        </button>
                        <button
                          type="button"
                          title={t('resetPassword')}
                          onClick={() => openPasswordModal(u)}
                          style={actionBtn(D, '#f59e0b')}
                        >
                          <Lock size={14} />
                        </button>
                        <button
                          type="button"
                          title={t('delete')}
                          onClick={() => {
                            setDeleteError('');
                            setDeleteTarget(u);
                          }}
                          style={actionBtn(D, '#ef4444')}
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {filtered.length === 0 && (
            <div style={{ textAlign: 'center', padding: '48px 0', color: muted, fontSize: 13 }}>—</div>
          )}
        </div>
      )}

      {toast && (
        <div
          style={{
            position: 'fixed',
            bottom: 24,
            right: 24,
            zIndex: 500,
            padding: '12px 18px',
            borderRadius: 12,
            background: D ? '#1e1e1e' : '#fff',
            border: `1px solid ${border}`,
            color: txt,
            fontSize: 13,
            fontWeight: 500,
            boxShadow: D ? '0 8px 32px rgba(0,0,0,0.5)' : '0 8px 32px rgba(0,0,0,0.12)',
          }}
        >
          {toast}
        </div>
      )}
    </div>
  );
}

function actionBtn(D: boolean, color: string): React.CSSProperties {
  return {
    width: 32,
    height: 32,
    borderRadius: 9,
    border: `1px solid ${D ? `${color}33` : `${color}22`}`,
    background: D ? `${color}14` : `${color}0d`,
    color,
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  };
}
