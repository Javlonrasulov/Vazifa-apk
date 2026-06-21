import { createContext, useContext, useMemo, useState, ReactNode } from 'react';
import { ThemeProvider as MuiThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { ADMIN_LANGS, DEFAULT_LANG, LangAdmin, t as translate } from './translations';
import { createAppTheme } from '../theme';

interface AppContextValue {
  lang: LangAdmin;
  setLang: (lang: LangAdmin) => void;
  t: (key: string) => string;
  langs: typeof ADMIN_LANGS;
  isDark: boolean;
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
  const [isDark, setIsDark] = useState(loadDark);

  const setLang = (next: LangAdmin) => {
    localStorage.setItem(LANG_KEY, next);
    setLangState(next);
  };

  const toggleTheme = () => {
    setIsDark((prev) => {
      const next = !prev;
      localStorage.setItem(THEME_KEY, String(next));
      return next;
    });
  };

  const value = useMemo(
    () => ({
      lang,
      setLang,
      t: (key: string) => translate(lang, key),
      langs: ADMIN_LANGS,
      isDark,
      toggleTheme,
    }),
    [lang, isDark],
  );

  const theme = useMemo(() => createAppTheme(isDark), [isDark]);

  return (
    <AppContext.Provider value={value}>
      <MuiThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </MuiThemeProvider>
    </AppContext.Provider>
  );
}

export function useAppSettings() {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useAppSettings must be used within AppProvider');
  return ctx;
}

/** @deprecated use useAppSettings */
export const useLanguage = useAppSettings;

/** @deprecated use AppProvider */
export const LanguageProvider = AppProvider;
