import { useEffect, useRef, useState } from 'react';
import { Check, ChevronDown, Layers } from 'lucide-react';
import { useAppSettings } from '../i18n/LanguageContext';
import { INDIGO, useAdminTheme } from '../theme/adminTheme';

export type DepartmentFilterOption = {
  key: string;
  label: string;
  count: number;
};

type Props = {
  value: string | null;
  onChange: (key: string | null) => void;
  totalCount: number;
  noDepartmentCount: number;
  options: DepartmentFilterOption[];
};

export default function DepartmentFilterDropdown({
  value,
  onChange,
  totalCount,
  noDepartmentCount,
  options,
}: Props) {
  const { t, isDark } = useAppSettings();
  const { D, txt, muted, border, surface, tableBg } = useAdminTheme(isDark);
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onDoc = (e: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', onDoc);
    return () => document.removeEventListener('mousedown', onDoc);
  }, [open]);

  const selectedLabel = (() => {
    if (value === null) return t('filterAllDepartments');
    if (value === '__none__') return t('filterNoDepartment');
    return options.find((o) => o.key === value)?.label ?? t('department');
  })();

  const selectedCount = (() => {
    if (value === null) return totalCount;
    if (value === '__none__') return noDepartmentCount;
    return options.find((o) => o.key === value)?.count ?? 0;
  })();

  const rows: Array<{ key: string | null; label: string; count: number }> = [
    { key: null, label: t('filterAllDepartments'), count: totalCount },
    ...options.map((o) => ({ key: o.key, label: o.label, count: o.count })),
  ];
  if (noDepartmentCount > 0) {
    rows.push({ key: '__none__', label: t('filterNoDepartment'), count: noDepartmentCount });
  }

  return (
    <div ref={rootRef} style={{ position: 'relative', minWidth: 200 }}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          width: '100%',
          minWidth: 200,
          padding: '7px 12px',
          borderRadius: 11,
          border: `1px solid ${value ? `${INDIGO}55` : border}`,
          background: value
            ? D
              ? 'linear-gradient(135deg, rgba(99,102,241,0.18), rgba(99,102,241,0.06))'
              : 'linear-gradient(135deg, rgba(99,102,241,0.1), rgba(99,102,241,0.03))'
            : surface,
          color: txt,
          fontSize: 13,
          fontWeight: 600,
          cursor: 'pointer',
          boxShadow: value ? '0 4px 14px rgba(99,102,241,0.12)' : 'none',
          transition: 'border-color 0.2s, box-shadow 0.2s, background 0.2s',
        }}
      >
        <span
          style={{
            width: 26,
            height: 26,
            borderRadius: 8,
            background: D ? 'rgba(99,102,241,0.2)' : 'rgba(99,102,241,0.1)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          <Layers size={14} color={INDIGO} />
        </span>
        <span style={{ flex: 1, textAlign: 'left', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {selectedLabel}
        </span>
        <span
          style={{
            fontSize: 11,
            fontWeight: 700,
            padding: '2px 8px',
            borderRadius: 999,
            background: D ? 'rgba(99,102,241,0.22)' : 'rgba(99,102,241,0.12)',
            color: INDIGO,
            flexShrink: 0,
          }}
        >
          {selectedCount}
        </span>
        <ChevronDown size={15} color={muted} className={`dept-filter-chevron${open ? ' open' : ''}`} />
      </button>

      {open && (
        <div
          className="dept-filter-menu"
          style={{
            position: 'absolute',
            top: 'calc(100% + 6px)',
            left: 0,
            right: 0,
            zIndex: 50,
            background: tableBg,
            border: `1px solid ${border}`,
            borderRadius: 14,
            padding: 6,
            boxShadow: D ? '0 16px 48px rgba(0,0,0,0.45)' : '0 16px 40px rgba(0,0,0,0.12)',
            maxHeight: 320,
            overflowY: 'auto',
          }}
        >
          {rows.map((row) => {
            const active = value === row.key;
            return (
              <button
                key={row.key ?? 'all'}
                type="button"
                className="dept-filter-item"
                onClick={() => {
                  onChange(row.key);
                  setOpen(false);
                }}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  width: '100%',
                  padding: '9px 10px',
                  borderRadius: 10,
                  border: 'none',
                  background: active
                    ? D
                      ? 'rgba(99,102,241,0.18)'
                      : 'rgba(99,102,241,0.08)'
                    : 'transparent',
                  color: active ? INDIGO : txt,
                  fontSize: 13,
                  fontWeight: active ? 700 : 500,
                  cursor: 'pointer',
                  textAlign: 'left',
                }}
              >
                <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {row.label}
                </span>
                <span
                  style={{
                    fontSize: 11,
                    fontWeight: 700,
                    padding: '2px 8px',
                    borderRadius: 999,
                    background: active
                      ? 'rgba(99,102,241,0.2)'
                      : D
                        ? 'rgba(255,255,255,0.06)'
                        : 'rgba(0,0,0,0.05)',
                    color: active ? INDIGO : muted,
                    minWidth: 28,
                    textAlign: 'center',
                  }}
                >
                  {row.count}
                </span>
                {active && <Check size={14} color={INDIGO} />}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
