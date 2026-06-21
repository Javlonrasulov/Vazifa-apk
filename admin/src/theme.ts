import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#2563EB' },
    success: { main: '#059669' },
    error: { main: '#DC2626' },
    background: { default: '#F4F5FF', paper: '#FFFFFF' },
  },
  typography: { fontFamily: '"Inter", sans-serif' },
  shape: { borderRadius: 12 },
  components: {
    MuiButton: { styleOverrides: { root: { textTransform: 'none', fontWeight: 600 } } },
    MuiCard: { styleOverrides: { root: { boxShadow: '0 4px 24px rgba(37,99,235,0.08)' } } },
  },
});
