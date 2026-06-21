import { Outlet, useNavigate } from 'react-router-dom';
import {
  AppBar,
  Box,
  Button,
  Container,
  IconButton,
  MenuItem,
  Select,
  Toolbar,
  Typography,
} from '@mui/material';
import AssignmentIcon from '@mui/icons-material/Assignment';
import DarkModeIcon from '@mui/icons-material/DarkMode';
import LightModeIcon from '@mui/icons-material/LightMode';
import { logout } from '../api';
import { useAppSettings } from '../i18n/LanguageContext';
import type { LangAdmin } from '../i18n/translations';

export default function Layout() {
  const navigate = useNavigate();
  const { t, lang, setLang, langs, isDark, toggleTheme } = useAppSettings();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar position="static" elevation={0} sx={{ bgcolor: '#2563EB' }}>
        <Toolbar>
          <AssignmentIcon sx={{ mr: 1 }} />
          <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 700 }}>
            {t('appName')}
          </Typography>
          <IconButton color="inherit" onClick={toggleTheme} sx={{ mr: 1 }}>
            {isDark ? <LightModeIcon /> : <DarkModeIcon />}
          </IconButton>
          <Select
            size="small"
            value={lang}
            onChange={(e) => setLang(e.target.value as LangAdmin)}
            sx={{ color: 'white', mr: 2, '.MuiOutlinedInput-notchedOutline': { borderColor: 'rgba(255,255,255,0.4)' } }}
          >
            {langs.map((l) => (
              <MenuItem key={l.id} value={l.id}>{l.flag} {l.label}</MenuItem>
            ))}
          </Select>
          <Typography variant="body2" sx={{ mr: 2, opacity: 0.9, display: { xs: 'none', sm: 'block' } }}>
            {t('appSubtitle')}
          </Typography>
          <Button color="inherit" onClick={handleLogout}>
            {t('logout')}
          </Button>
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Outlet />
      </Container>
    </Box>
  );
}
