import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertCircle,
  Check,
  ChevronDown,
  Eye,
  EyeOff,
  Globe,
  Loader,
  Moon,
  Shield,
  Sun,
} from 'lucide-react';
import { adminLogin } from '../api';
import { useAppSettings } from '../i18n/LanguageContext';
import { useAdminTheme } from '../theme/adminTheme';
import type { LangAdmin } from '../i18n/translations';

export default function LoginPage() {
  const navigate = useNavigate();
  const { t, lang, setLang, langs, isDark, toggleTheme } = useAppSettings();
  const theme = useAdminTheme(isDark);
  const D = theme.D;

  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');
  const [showPass, setShowPass] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [shake, setShake] = useState(false);
  const [showLangMenu, setShowLangMenu] = useState(false);

  const currentLang = langs.find((l) => l.id === lang)!;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!login.trim() || !password.trim()) {
      setError(t('loginEmpty'));
      return;
    }
    setLoading(true);
    setError('');
    try {
      await adminLogin(login.trim(), password.trim());
      navigate('/employees');
    } catch {
      setError(t('loginError'));
      setShake(true);
      setTimeout(() => setShake(false), 500);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={`min-h-screen ${theme.bg} ${theme.text} flex flex-col`}>
      <div className="flex justify-between items-center px-6 pt-6">
        <div className="flex items-center gap-2">
          <div className={`w-8 h-8 rounded-xl flex items-center justify-center ${D ? 'bg-indigo-500/20' : 'bg-indigo-50'}`}>
            <Shield size={16} className="text-indigo-500" />
          </div>
          <span className="font-bold text-sm">{t('appName')}</span>
        </div>

        <div className="flex items-center gap-2">
          <div className="relative">
            <button
              type="button"
              onClick={() => setShowLangMenu((v) => !v)}
              className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium transition-all ${
                D
                  ? 'bg-gray-800 hover:bg-gray-700 text-gray-300 border border-gray-700'
                  : 'bg-white hover:bg-gray-50 text-gray-600 border border-gray-200 shadow-sm'
              }`}
            >
              <Globe size={13} />
              <span>{currentLang.flag}</span>
              <ChevronDown size={11} className={`transition-transform ${showLangMenu ? 'rotate-180' : ''}`} />
            </button>

            {showLangMenu && (
              <div
                className={`absolute right-0 top-full mt-2 w-40 rounded-2xl border shadow-2xl z-50 overflow-hidden ${
                  D ? 'bg-[#1a1a1a] border-gray-700' : 'bg-white border-gray-100'
                }`}
              >
                {langs.map((l) => (
                  <button
                    key={l.id}
                    type="button"
                    onClick={() => {
                      setLang(l.id as LangAdmin);
                      setShowLangMenu(false);
                      setError('');
                    }}
                    className={`w-full flex items-center justify-between px-4 py-3 text-sm transition-colors ${
                      lang === l.id
                        ? D
                          ? 'bg-indigo-600/20 text-indigo-400'
                          : 'bg-indigo-50 text-indigo-700'
                        : D
                          ? 'text-gray-300 hover:bg-gray-800'
                          : 'text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    <div className="flex items-center gap-2.5">
                      <span
                        className={`text-xs font-bold px-1.5 py-0.5 rounded ${
                          D ? 'bg-white/10 text-gray-400' : 'bg-gray-100 text-gray-500'
                        }`}
                      >
                        {l.flag}
                      </span>
                      <span>{l.label}</span>
                    </div>
                    {lang === l.id && <Check size={13} className="text-indigo-500" />}
                  </button>
                ))}
              </div>
            )}
          </div>

          <button
            type="button"
            onClick={toggleTheme}
            className={`w-9 h-9 rounded-xl flex items-center justify-center transition-colors ${
              D
                ? 'bg-gray-800 hover:bg-gray-700 text-yellow-400'
                : 'bg-white hover:bg-gray-100 text-gray-600 border border-gray-200 shadow-sm'
            }`}
          >
            {D ? <Sun size={16} /> : <Moon size={16} />}
          </button>
        </div>
      </div>

      <div className="flex-1 flex items-center justify-center px-5 py-10">
        <div className="w-full max-w-sm fadeIn">
          <div className="text-center mb-8">
            <div
              className={`w-16 h-16 rounded-3xl flex items-center justify-center mx-auto mb-4 ${
                D ? 'bg-indigo-500/15' : 'bg-indigo-50'
              }`}
            >
              <Shield size={32} className="text-indigo-500" />
            </div>
            <h1 className="text-2xl font-bold mb-1">{t('loginBtn')}</h1>
            <p className={`text-sm ${theme.sub}`}>{t('adminSubtitle')}</p>
          </div>

          <form
            onSubmit={handleSubmit}
            className={`rounded-3xl border p-6 space-y-4 ${theme.card} ${shake ? 'shake' : ''}`}
          >
            {error && (
              <div
                className={`flex items-center gap-2.5 px-4 py-3 rounded-2xl ${
                  D ? 'bg-rose-500/10 border border-rose-500/20' : 'bg-rose-50 border border-rose-200'
                }`}
              >
                <AlertCircle size={15} className="text-rose-400 flex-shrink-0" />
                <p className="text-sm text-rose-400">{error}</p>
              </div>
            )}

            <div>
              <label className={`text-xs font-medium ${theme.sub} mb-1.5 block`}>{t('login')}</label>
              <input
                value={login}
                onChange={(e) => {
                  setLogin(e.target.value);
                  setError('');
                }}
                placeholder="admin"
                autoComplete="username"
                className={`w-full px-4 py-3.5 rounded-2xl border text-sm outline-none transition-colors ${theme.input}`}
              />
            </div>

            <div>
              <label className={`text-xs font-medium ${theme.sub} mb-1.5 block`}>{t('password')}</label>
              <div className="relative">
                <input
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    setError('');
                  }}
                  type={showPass ? 'text' : 'password'}
                  placeholder="••••••••"
                  autoComplete="current-password"
                  className={`w-full px-4 py-3.5 pr-12 rounded-2xl border text-sm outline-none transition-colors ${theme.input}`}
                />
                <button
                  type="button"
                  onClick={() => setShowPass((v) => !v)}
                  className={`absolute right-3.5 top-1/2 -translate-y-1/2 w-8 h-8 flex items-center justify-center rounded-xl ${
                    D ? 'text-gray-500 hover:text-gray-300' : 'text-gray-400 hover:text-gray-600'
                  } transition-colors`}
                >
                  {showPass ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className={`w-full py-3.5 rounded-2xl font-semibold text-sm transition-all flex items-center justify-center gap-2 ${
                loading
                  ? D
                    ? 'bg-gray-700 text-gray-400 cursor-not-allowed'
                    : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                  : D
                    ? 'bg-white text-black hover:bg-gray-100 active:scale-[0.98]'
                    : 'bg-gray-900 text-white hover:bg-gray-800 active:scale-[0.98]'
              }`}
            >
              {loading ? (
                <>
                  <Loader size={16} className="animate-spin" />
                  {t('loginLoading')}
                </>
              ) : (
                t('loginBtn')
              )}
            </button>

            <div className="text-center pt-1">
              <p className={`text-xs ${theme.sub}`}>
                {t('demo')}:{' '}
                <span className={`font-mono ${D ? 'text-gray-300' : 'text-gray-600'}`}>admin / admin123</span>
              </p>
            </div>
          </form>

          <p className={`text-center text-xs ${theme.sub} mt-6`}>{t('footer')}</p>
        </div>
      </div>
    </div>
  );
}
