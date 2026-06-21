export const INDIGO = '#6366f1';

export function useAdminTheme(isDark: boolean) {
  const D = isDark;
  return {
    D,
    bg: D ? 'bg-[#0a0a0a]' : 'bg-gray-50',
    sidebar: D ? 'bg-[#111111] border-gray-800' : 'bg-white border-gray-200',
    card: D ? 'bg-[#161616] border-gray-800' : 'bg-white border-gray-200',
    cardHover: D ? 'hover:bg-[#1c1c1c]' : 'hover:bg-gray-50',
    text: D ? 'text-white' : 'text-gray-900',
    sub: D ? 'text-gray-400' : 'text-gray-500',
    divider: D ? 'border-gray-800' : 'border-gray-100',
    input: D
      ? 'bg-[#1e1e1e] border-gray-700 text-white placeholder-gray-500 focus:border-indigo-500'
      : 'bg-gray-50 border-gray-200 text-gray-900 placeholder-gray-400 focus:border-indigo-400',
    txt: D ? '#f9fafb' : '#111827',
    muted: D ? '#6b7280' : '#9ca3af',
    border: D ? '#2a2a2a' : '#e5e7eb',
    surface: D ? '#1a1a1a' : '#f9fafb',
    rowAlt: D ? 'rgba(255,255,255,0.022)' : 'rgba(0,0,0,0.016)',
    rowHov: D ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.03)',
    tableBg: D ? '#161616' : '#ffffff',
    navbar: D
      ? 'bg-[#0a0a0a]/95 border-gray-800'
      : 'bg-gray-50/95 border-gray-200',
  };
}

export type AdminTheme = ReturnType<typeof useAdminTheme>;
