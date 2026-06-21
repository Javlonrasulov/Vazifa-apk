import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { useAppSettings } from '../i18n/LanguageContext';
import { useAdminTheme } from '../theme/adminTheme';
import { AdminNavbar, AdminSidebar } from './AdminShell';

export default function Layout() {
  const { isDark } = useAppSettings();
  const theme = useAdminTheme(isDark);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div
      className={`flex min-h-screen overflow-x-hidden ${theme.bg} ${theme.text}`}
      style={{ '--sb-w': sidebarCollapsed ? '64px' : '240px' } as React.CSSProperties}
    >
      <AdminSidebar
        theme={theme}
        collapsed={sidebarCollapsed}
        setCollapsed={setSidebarCollapsed}
        open={sidebarOpen}
        setOpen={setSidebarOpen}
      />

      <div
        className={`flex-1 min-w-0 flex flex-col min-h-screen transition-all duration-300 ${
          sidebarCollapsed ? 'md:ml-16' : 'md:ml-60'
        }`}
      >
        <AdminNavbar theme={theme} onMenuClick={() => setSidebarOpen(true)} />
        <main className="flex-1 px-5 md:px-8 py-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
