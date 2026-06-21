import { createTheme } from '@mui/material/styles';

export function createAppTheme(isDark: boolean) {
  return createTheme({
    palette: {
      mode: isDark ? 'dark' : 'light',
      primary: { main: '#2563EB' },
      success: { main: '#059669' },
      error: { main: '#DC2626' },
      background: isDark
        ? { default: '#060B18', paper: '#0D1428' }
        : { default: '#F4F5FF', paper: '#FFFFFF' },
    },
    typography: { fontFamily: '"Inter", sans-serif' },
    shape: { borderRadius: 12 },
    components: {
      MuiButton: { styleOverrides: { root: { textTransform: 'none', fontWeight: 600 } } },
      MuiCard: {
        styleOverrides: {
          root: {
            boxShadow: isDark
              ? '0 4px 24px rgba(0,0,0,0.35)'
              : '0 4px 24px rgba(37,99,235,0.08)',
          },
        },
      },
    },
  });
}
