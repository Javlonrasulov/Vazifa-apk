import { useCallback, useEffect, useMemo, useState } from 'react';
import { isAxiosError } from 'axios';
import { AlertTriangle, Check, Edit2, Loader, Plus, Search, Trash2, X } from 'lucide-react';
import {
  Department,
  apiErrorMessage,
  createDepartment,
  deleteDepartment,
  fetchDepartments,
  updateDepartment,
} from '../api';
import { useAppSettings } from '../i18n/LanguageContext';
import { INDIGO, useAdminTheme } from '../theme/adminTheme';

export default function DepartmentsPage() {
  const { t } = useAppSettings();
  const theme = useAdminTheme(useAppSettings().isDark);
  const { D, txt, muted, border, surface, rowAlt, rowHov, tableBg } = theme;

  const [items, setItems] = useState<Department[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editItem, setEditItem] = useState<Department | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Department | null>(null);
  const [name, setName] = useState('');
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');
  const [deleteError, setDeleteError] = useState('');
  const [deleteSaving, setDeleteSaving] = useState(false);
  const [toast, setToast] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await fetchDepartments());
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!toast) return;
    const id = setTimeout(() => setToast(''), 3000);
    return () => clearTimeout(id);
  }, [toast]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return items;
    return items.filter((d) => d.name.toLowerCase().includes(q));
  }, [items, search]);

  const openCreate = () => {
    setEditItem(null);
    setName('');
    setSaveError('');
    setDialogOpen(true);
  };

  const openEdit = (d: Department) => {
    setEditItem(d);
    setName(d.name);
    setSaveError('');
    setDialogOpen(true);
  };

  const handleSave = async () => {
    const trimmed = name.trim();
    if (!trimmed) {
      setSaveError(t('departmentNameRequired'));
      return;
    }
    setSaving(true);
    setSaveError('');
    try {
      if (editItem) {
        await updateDepartment(editItem.id, trimmed);
        setToast(t('updated'));
      } else {
        await createDepartment(trimmed);
        setToast(t('added'));
      }
      setDialogOpen(false);
      await load();
    } catch (err) {
      if (isAxiosError(err) && err.response?.status === 409) {
        setSaveError(t('departmentTaken'));
      } else {
        setSaveError(apiErrorMessage(err) ?? t('error'));
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget || deleteSaving) return;
    setDeleteSaving(true);
    setDeleteError('');
    try {
      await deleteDepartment(deleteTarget.id);
      setDeleteTarget(null);
      setToast(t('deleted'));
      await load();
    } catch (err) {
      setDeleteError(apiErrorMessage(err) ?? t('error'));
    } finally {
      setDeleteSaving(false);
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

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {dialogOpen && (
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
              maxWidth: 400,
              margin: 16,
              padding: '24px 22px',
              boxShadow: D ? '0 24px 64px rgba(0,0,0,0.7)' : '0 24px 64px rgba(0,0,0,0.15)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 18 }}>
              <span style={{ fontSize: 16, fontWeight: 700, color: txt }}>
                {editItem ? t('departmentEdit') : t('departmentNew')}
              </span>
              <button type="button" onClick={() => setDialogOpen(false)} style={{ border: 'none', background: 'transparent', color: muted, cursor: 'pointer' }}>
                <X size={16} />
              </button>
            </div>
            {saveError && (
              <div style={{ marginBottom: 14, padding: '10px 12px', borderRadius: 10, background: 'rgba(239,68,68,0.1)', color: '#ef4444', fontSize: 12 }}>
                {saveError}
              </div>
            )}
            <label style={labelStyle}>{t('departmentName')}</label>
            <input style={inputStyle} value={name} onChange={(e) => setName(e.target.value)} autoFocus />
            <div style={{ display: 'flex', gap: 10, marginTop: 22 }}>
              <button type="button" onClick={() => setDialogOpen(false)} style={{ flex: 1, padding: 10, borderRadius: 11, border: `1px solid ${border}`, background: 'transparent', color: muted, fontSize: 13, fontWeight: 600, cursor: 'pointer' }}>
                {t('cancel')}
              </button>
              <button type="button" onClick={handleSave} disabled={saving} style={{ flex: 2, padding: 10, borderRadius: 11, border: 'none', background: saving ? '#9ca3af' : INDIGO, color: '#fff', fontSize: 13, fontWeight: 700, cursor: saving ? 'not-allowed' : 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 7 }}>
                <Check size={14} />
                {saving ? t('loginLoading') : t('save')}
              </button>
            </div>
          </div>
        </div>
      )}

      {deleteTarget && (
        <div
          style={{ position: 'fixed', inset: 0, zIndex: 400, background: 'rgba(0,0,0,0.55)', display: 'flex', alignItems: 'center', justifyContent: 'center', backdropFilter: 'blur(4px)' }}
          onClick={() => { setDeleteTarget(null); setDeleteError(''); }}
        >
          <div style={{ background: tableBg, borderRadius: 20, border: `1px solid ${border}`, width: '100%', maxWidth: 360, margin: 16, padding: '28px 24px', textAlign: 'center' }} onClick={(e) => e.stopPropagation()}>
            <AlertTriangle size={22} color="#ef4444" style={{ margin: '0 auto 16px' }} />
            <div style={{ fontSize: 15, fontWeight: 700, color: txt, marginBottom: 8 }}>{t('deleteTitle')}</div>
            <div style={{ fontSize: 13, color: muted, marginBottom: deleteError ? 14 : 24 }}>
              <strong style={{ color: txt }}>{deleteTarget.name}</strong>
              {deleteTarget.employeeCount > 0 && (
                <div style={{ marginTop: 8 }}>{t('departmentDeleteWarn')}</div>
              )}
            </div>
            {deleteError && <div style={{ marginBottom: 16, color: '#ef4444', fontSize: 12 }}>{deleteError}</div>}
            <div style={{ display: 'flex', gap: 10 }}>
              <button type="button" onClick={() => { setDeleteTarget(null); setDeleteError(''); }} style={{ flex: 1, padding: 10, borderRadius: 11, border: `1px solid ${border}`, background: 'transparent', color: muted, cursor: 'pointer' }}>{t('cancel')}</button>
              <button type="button" onClick={handleDelete} disabled={deleteSaving} style={{ flex: 1, padding: 10, borderRadius: 11, border: 'none', background: deleteSaving ? '#9ca3af' : '#ef4444', color: '#fff', fontWeight: 700, cursor: deleteSaving ? 'not-allowed' : 'pointer' }}>{deleteSaving ? t('loginLoading') : t('deleteBtn')}</button>
            </div>
          </div>
        </div>
      )}

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
        <div>
          <span style={{ fontSize: 17, fontWeight: 700, color: txt }}>{t('departmentsTitle')}</span>
          <span style={{ fontSize: 12, color: muted, marginLeft: 10 }}>{filtered.length} {t('empUnit')}</span>
        </div>
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, background: surface, border: `1px solid ${border}`, borderRadius: 11, padding: '7px 13px', minWidth: 220 }}>
            <Search size={13} color={muted} />
            <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder={t('search')} style={{ flex: 1, border: 'none', background: 'transparent', fontSize: 13, color: txt, outline: 'none' }} />
          </div>
          <button type="button" onClick={openCreate} style={{ display: 'flex', alignItems: 'center', gap: 7, padding: '8px 16px', borderRadius: 11, border: 'none', background: INDIGO, color: '#fff', fontSize: 13, fontWeight: 600, cursor: 'pointer', boxShadow: '0 4px 14px rgba(99,102,241,0.35)' }}>
            <Plus size={14} />
            {t('add')}
          </button>
        </div>
      </div>

      {loading ? (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10, padding: '64px 0', color: muted }}>
          <Loader size={20} className="animate-spin" />
          {t('loading')}
        </div>
      ) : (
        <div style={{ background: tableBg, border: `1px solid ${border}`, borderRadius: 18, overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ background: D ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.022)', borderBottom: `1px solid ${border}` }}>
                {[t('departmentName'), t('employeeCount'), t('actions')].map((label) => (
                  <th key={label} style={{ padding: '11px 14px', textAlign: 'left', fontSize: 11, fontWeight: 700, color: muted, textTransform: 'uppercase' }}>{label}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {filtered.map((d, i) => (
                <tr key={d.id} style={{ background: i % 2 === 0 ? tableBg : rowAlt, borderBottom: `1px solid ${D ? 'rgba(255,255,255,0.045)' : 'rgba(0,0,0,0.045)'}` }} onMouseEnter={(e) => { e.currentTarget.style.background = rowHov; }} onMouseLeave={(e) => { e.currentTarget.style.background = i % 2 === 0 ? tableBg : rowAlt; }}>
                  <td style={{ padding: '11px 14px', fontSize: 13, fontWeight: 600, color: txt }}>{d.name}</td>
                  <td style={{ padding: '11px 14px' }}>
                    <span style={{ fontSize: 11, fontWeight: 600, padding: '3px 10px', borderRadius: 6, background: D ? 'rgba(99,102,241,0.15)' : 'rgba(99,102,241,0.08)', color: INDIGO }}>
                      {d.employeeCount} {t('empUnit')}
                    </span>
                  </td>
                  <td style={{ padding: '11px 14px' }}>
                    <div style={{ display: 'flex', gap: 4 }}>
                      <button type="button" title={t('edit')} onClick={() => openEdit(d)} style={actionBtn(D, INDIGO)}><Edit2 size={14} /></button>
                      <button type="button" title={t('delete')} onClick={() => { setDeleteError(''); setDeleteTarget(d); }} style={actionBtn(D, '#ef4444')}><Trash2 size={14} /></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {filtered.length === 0 && <div style={{ textAlign: 'center', padding: '48px 0', color: muted, fontSize: 13 }}>{t('departmentEmpty')}</div>}
        </div>
      )}

      {toast && (
        <div style={{ position: 'fixed', bottom: 24, right: 24, zIndex: 500, padding: '12px 18px', borderRadius: 12, background: D ? '#1e1e1e' : '#fff', border: `1px solid ${border}`, color: txt, fontSize: 13, fontWeight: 500, boxShadow: D ? '0 8px 32px rgba(0,0,0,0.5)' : '0 8px 32px rgba(0,0,0,0.12)' }}>
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
