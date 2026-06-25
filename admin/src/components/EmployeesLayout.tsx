import { NavLink, Outlet } from 'react-router-dom';
import { useAppSettings } from '../i18n/LanguageContext';
import { INDIGO, useAdminTheme } from '../theme/adminTheme';

export default function EmployeesLayout() {
  const { t, isDark } = useAppSettings();
  const { D, muted, border, surface } = useAdminTheme(isDark);

  const tabs = [
    { to: '/employees', label: t('tabEmployees'), end: true },
    { to: '/employees/departments', label: t('tabDepartments'), end: false },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div
        style={{
          display: 'flex',
          gap: 8,
          padding: 4,
          borderRadius: 12,
          border: `1px solid ${border}`,
          background: surface,
          width: 'fit-content',
          maxWidth: '100%',
          flexWrap: 'wrap',
        }}
      >
        {tabs.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            end={tab.end}
            style={({ isActive }) => ({
              padding: '8px 16px',
              borderRadius: 10,
              fontSize: 13,
              fontWeight: isActive ? 700 : 500,
              textDecoration: 'none',
              color: isActive ? INDIGO : muted,
              background: isActive
                ? D
                  ? 'rgba(99,102,241,0.15)'
                  : 'rgba(99,102,241,0.08)'
                : 'transparent',
              border: `1px solid ${isActive ? INDIGO : 'transparent'}`,
              whiteSpace: 'nowrap',
            })}
          >
            {tab.label}
          </NavLink>
        ))}
      </div>
      <Outlet />
    </div>
  );
}
