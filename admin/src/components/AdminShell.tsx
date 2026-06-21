import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, ClipboardList, LogOut, Menu, Moon, Sun, Users, X } from 'lucide-react';
import { logout } from '../api';
import { useAppSettings } from '../i18n/LanguageContext';
import type { AdminTheme } from '../theme/adminTheme';

interface Props {
  theme: AdminTheme;
  collapsed: boolean;
  setCollapsed: (v: boolean) => void;
  open: boolean;
  setOpen: (v: boolean) => void;
}

export function AdminSidebar({ theme, collapsed, setCollapsed, open, setOpen }: Props) {
  const { t } = useAppSettings();
  const { D, sidebar, divider, sub } = theme;

  const SidebarContent = ({ mobile = false }: { mobile?: boolean }) => (
    <div className="flex flex-col h-full">
      <div className={`flex items-center gap-3 px-4 py-5 ${collapsed && !mobile ? 'justify-center' : ''}`}>
        <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white flex-shrink-0">
          <ClipboardList size={16} />
        </div>
        {(!collapsed || mobile) && (
          <div className="min-w-0">
            <p className="font-bold text-base tracking-tight truncate">{t('appName')}</p>
            <p className={`text-xs ${sub} truncate`}>{t('appSubtitle')}</p>
          </div>
        )}
      </div>

      <nav className="flex-1 px-3 space-y-0.5">
        <button
          type="button"
          className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all ${
            collapsed && !mobile ? 'justify-center px-2' : ''
          } bg-indigo-600 text-white shadow-lg shadow-indigo-900/40`}
        >
          <Users size={17} className="flex-shrink-0" />
          {(!collapsed || mobile) && <span>{t('employeesTitle')}</span>}
        </button>
      </nav>

      {!mobile && (
        <div className={`px-3 py-4 border-t ${divider}`}>
          <button
            type="button"
            onClick={() => setCollapsed(!collapsed)}
            className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all ${
              collapsed ? 'justify-center px-2' : ''
            } ${D ? `${sub} hover:bg-gray-800 hover:text-white` : `${sub} hover:bg-gray-100 hover:text-gray-900`}`}
          >
            <ChevronLeft size={17} className={`transition-transform ${collapsed ? 'rotate-180' : ''}`} />
            {!collapsed && <span>{t('sidebarCollapse')}</span>}
          </button>
        </div>
      )}
    </div>
  );

  return (
    <>
      {open && (
        <div
          className="fixed inset-0 z-40 bg-black/60 backdrop-blur-sm md:hidden"
          onClick={() => setOpen(false)}
        />
      )}

      <aside
        className={`fixed top-0 left-0 z-50 h-full border-r transition-all duration-300 ${sidebar} ${
          open ? 'translate-x-0' : '-translate-x-full'
        } md:translate-x-0 ${collapsed ? 'w-16' : 'w-60'}`}
      >
        <div className="md:hidden flex justify-end p-3">
          <button
            type="button"
            onClick={() => setOpen(false)}
            className={`w-9 h-9 rounded-xl flex items-center justify-center ${D ? 'bg-gray-800' : 'bg-gray-100'}`}
          >
            <X size={18} />
          </button>
        </div>
        <SidebarContent mobile={open} />
      </aside>
    </>
  );
}

export function AdminNavbar({
  theme,
  onMenuClick,
}: {
  theme: AdminTheme;
  onMenuClick: () => void;
}) {
  const navigate = useNavigate();
  const { t, lang, setLang, langs, isDark, toggleTheme } = useAppSettings();
  const { D, navbar, sub, text } = theme;
  const [showLang, setShowLang] = useState(false);
  const currentLang = langs.find((l) => l.id === lang)!;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className={`sticky top-0 z-30 border-b backdrop-blur-md ${navbar}`}>
      <div className="px-5 md:px-8 py-4 flex items-center gap-4">
        <button
          type="button"
          onClick={onMenuClick}
          className={`md:hidden w-9 h-9 rounded-xl flex items-center justify-center ${
            D ? 'bg-gray-800 hover:bg-gray-700' : 'bg-gray-100 hover:bg-gray-200'
          }`}
        >
          <Menu size={18} />
        </button>

        <div className="flex-1 min-w-0">
          <h1 className={`font-bold text-lg truncate ${text}`}>{t('employeesTitle')}</h1>
          <p className={`text-sm ${sub} truncate`}>{t('employeesDesc')}</p>
        </div>

        <div className="flex items-center gap-2">
          <div className="relative hidden sm:block">
            <button
              type="button"
              onClick={() => setShowLang((v) => !v)}
              className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium transition-all ${
                D
                  ? 'bg-gray-800 hover:bg-gray-700 text-gray-300 border border-gray-700'
                  : 'bg-white hover:bg-gray-50 text-gray-600 border border-gray-200 shadow-sm'
              }`}
            >
              <span>{currentLang.flag}</span>
              <span>{currentLang.label}</span>
            </button>
            {showLang && (
              <div
                className={`absolute right-0 top-full mt-2 w-36 rounded-2xl border shadow-2xl z-50 overflow-hidden ${
                  D ? 'bg-[#1a1a1a] border-gray-700' : 'bg-white border-gray-100'
                }`}
              >
                {langs.map((l) => (
                  <button
                    key={l.id}
                    type="button"
                    onClick={() => {
                      setLang(l.id);
                      setShowLang(false);
                    }}
                    className={`w-full text-left px-4 py-2.5 text-sm ${
                      lang === l.id
                        ? D
                          ? 'bg-indigo-600/20 text-indigo-400'
                          : 'bg-indigo-50 text-indigo-700'
                        : D
                          ? 'text-gray-300 hover:bg-gray-800'
                          : 'text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    {l.flag} {l.label}
                  </button>
                ))}
              </div>
            )}
          </div>

          <button
            type="button"
            onClick={toggleTheme}
            title={isDark ? t('themeLight') : t('themeDark')}
            className={`w-9 h-9 rounded-xl flex items-center justify-center transition-colors ${
              D
                ? 'bg-gray-800 hover:bg-gray-700 text-yellow-400'
                : 'bg-white hover:bg-gray-100 text-gray-600 border border-gray-200 shadow-sm'
            }`}
          >
            {isDark ? <Sun size={16} /> : <Moon size={16} />}
          </button>

          <button
            type="button"
            onClick={handleLogout}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium transition-all text-rose-400 ${
              D
                ? 'bg-gray-800 hover:bg-rose-500/10 border border-gray-700'
                : 'bg-white hover:bg-rose-50 border border-gray-200 shadow-sm'
            }`}
          >
            <LogOut size={14} />
            <span className="hidden sm:inline">{t('logout')}</span>
          </button>
        </div>
      </div>
    </div>
  );
}
