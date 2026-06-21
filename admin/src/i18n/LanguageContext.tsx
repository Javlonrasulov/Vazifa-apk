import { createContext, useContext, useMemo, useState, ReactNode } from 'react';
import { ADMIN_LANGS, DEFAULT_LANG, LangAdmin, t as translate } from './translations';

interface AppContextValue {
  lang: LangAdmin;
  setLang: (lang: LangAdmin) => void;
  t: (key: string) => string;
  langs: typeof ADMIN_LANGS;
  isDark: boolean;
  setIsDark: (v: boolean) => void;
  toggleTheme: () => void;
}

const AppContext = createContext<AppContextValue | null>(null);

const LANG_KEY = 'lider_vazifa_admin_lang';
const THEME_KEY = 'lider_vazifa_admin_dark';

function loadLang(): LangAdmin {
  const saved = localStorage.getItem(LANG_KEY) as LangAdmin | null;
  if (saved && ['uz', 'cy', 'ru'].includes(saved)) return saved;
  return DEFAULT_LANG;
}

function loadDark(): boolean {
  const saved = localStorage.getItem(THEME_KEY);
  if (saved === 'true') return true;
  if (saved === 'false') return false;
  return true;
}

export function AppProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<LangAdmin>(loadLang);
  const [isDark, setIsDarkState] = useState(loadDark);

  const setLang = (next: LangAdmin) => {
    localStorage.setItem(LANG_KEY, next);
    setLangState(next);
  };

  const setIsDark = (next: boolean) => {
    localStorage.setItem(THEME_KEY, String(next));
    setIsDarkState(next);
  };

  const toggleTheme = () => setIsDark(!isDark);

  const value = useMemo(
    () => ({
      lang,
      setLang,
      t: (key: string) => translate(lang, key),
      langs: ADMIN_LANGS,
      isDark,
      setIsDark,
      toggleTheme,
    }),
    [lang, isDark],
  );

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export function useAppSettings() {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useAppSettings must be used within AppProvider');
  return ctx;
}
