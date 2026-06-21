import { useCallback, useEffect, useState } from 'react';
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Snackbar,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import PhoneAndroidIcon from '@mui/icons-material/PhoneAndroid';
import LockResetIcon from '@mui/icons-material/LockReset';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import {
  User,
  approveDevice,
  createUser,
  deleteUser,
  fetchUsers,
  resetDevice,
  resetPassword,
  updateUser,
} from '../api';
import { useAppSettings } from '../i18n/LanguageContext';

export default function EmployeesPage() {
  const { t } = useAppSettings();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editUser, setEditUser] = useState<User | null>(null);
  const [snack, setSnack] = useState('');
  const [form, setForm] = useState({
    login: '',
    password: '',
    fullName: '',
    role: 'employee' as 'director' | 'employee',
    position: '',
    department: '',
    phone: '',
  });

  const roleLabel = (role: string) => {
    if (role === 'director') return t('director');
    if (role === 'employee') return t('employee');
    return t('admin');
  };

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await fetchUsers();
      setUsers(data.filter((u) => u.role !== 'admin'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const openCreate = () => {
    setEditUser(null);
    setForm({ login: '', password: '', fullName: '', role: 'employee', position: '', department: '', phone: '' });
    setDialogOpen(true);
  };

  const openEdit = (u: User) => {
    setEditUser(u);
    setForm({
      login: u.login,
      password: '',
      fullName: u.fullName,
      role: u.role as 'director' | 'employee',
      position: u.position ?? '',
      department: u.department ?? '',
      phone: u.phone ?? '',
    });
    setDialogOpen(true);
  };

  const handleSave = async () => {
    try {
      if (editUser) {
        await updateUser(editUser.id, {
          fullName: form.fullName,
          role: form.role,
          position: form.position || null,
          department: form.department || null,
          phone: form.phone || null,
        });
        setSnack(t('updated'));
      } else {
        await createUser({
          login: form.login,
          password: form.password,
          fullName: form.fullName,
          role: form.role,
          position: form.position || undefined,
          department: form.department || undefined,
          phone: form.phone || undefined,
        });
        setSnack(t('added'));
      }
      setDialogOpen(false);
      load();
    } catch {
      setSnack(t('error'));
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm(t('confirmDelete'))) return;
    await deleteUser(id);
    setSnack(t('deleted'));
    load();
  };

  const handleResetPassword = async (u: User) => {
    const pw = prompt(t('passwordPrompt'));
    if (!pw || pw.length < 6) return;
    await resetPassword(u.id, pw);
    setSnack(t('passwordChanged'));
  };

  const handleResetDevice = async (id: string) => {
    await resetDevice(id);
    setSnack(t('deviceReset'));
    load();
  };

  const handleApproveDevice = async (id: string, approve: boolean) => {
    await approveDevice(id, approve);
    setSnack(approve ? t('deviceApproved') : t('deviceRejected'));
    load();
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700}>{t('employeesTitle')}</Typography>
          <Typography variant="body2" color="text.secondary">{t('employeesDesc')}</Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>{t('add')}</Button>
      </Box>

      {loading ? (
        <Typography>{t('loading')}</Typography>
      ) : (
        <Table sx={{ bgcolor: 'background.paper', borderRadius: 2, overflow: 'hidden' }}>
          <TableHead>
            <TableRow sx={{ bgcolor: '#2563EB', '& th': { color: 'white', fontWeight: 600 } }}>
              <TableCell>{t('name')}</TableCell>
              <TableCell>{t('login')}</TableCell>
              <TableCell>{t('role')}</TableCell>
              <TableCell>{t('department')}</TableCell>
              <TableCell>{t('device')}</TableCell>
              <TableCell align="right">{t('actions')}</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {users.map((u) => (
              <TableRow key={u.id} hover>
                <TableCell>{u.fullName}</TableCell>
                <TableCell>{u.login}</TableCell>
                <TableCell>
                  <Chip label={roleLabel(u.role)} size="small" color={u.role === 'director' ? 'primary' : 'default'} />
                </TableCell>
                <TableCell>{u.department ?? '—'}</TableCell>
                <TableCell>
                  {u.pendingDeviceId ? (
                    <Box sx={{ display: 'flex', gap: 0.5 }}>
                      <IconButton size="small" color="success" title={t('deviceApprove')} onClick={() => handleApproveDevice(u.id, true)}>
                        <CheckCircleIcon fontSize="small" />
                      </IconButton>
                      <IconButton size="small" color="error" title={t('deviceReject')} onClick={() => handleApproveDevice(u.id, false)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Box>
                  ) : u.deviceId ? (
                    <Chip label={t('deviceLinked')} size="small" color="success" variant="outlined" />
                  ) : (
                    <Chip label={t('deviceNone')} size="small" variant="outlined" />
                  )}
                </TableCell>
                <TableCell align="right">
                  <IconButton size="small" onClick={() => openEdit(u)} title={t('edit')}><EditIcon /></IconButton>
                  <IconButton size="small" onClick={() => handleResetPassword(u)} title={t('resetPassword')}><LockResetIcon /></IconButton>
                  <IconButton size="small" onClick={() => handleResetDevice(u.id)} title={t('resetDevice')}><PhoneAndroidIcon /></IconButton>
                  <IconButton size="small" color="error" onClick={() => handleDelete(u.id)} title={t('delete')}><DeleteIcon /></IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editUser ? t('dialogEdit') : t('dialogNew')}</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
          {!editUser && (
            <>
              <TextField label={t('login')} value={form.login} onChange={(e) => setForm({ ...form, login: e.target.value })} required />
              <TextField label={t('password')} type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
            </>
          )}
          <TextField label={t('fullName')} value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
          <FormControl fullWidth>
            <InputLabel>{t('role')}</InputLabel>
            <Select value={form.role} label={t('role')} onChange={(e) => setForm({ ...form, role: e.target.value as 'director' | 'employee' })}>
              <MenuItem value="employee">{t('employee')}</MenuItem>
              <MenuItem value="director">{t('director')}</MenuItem>
            </Select>
          </FormControl>
          <TextField label={t('position')} value={form.position} onChange={(e) => setForm({ ...form, position: e.target.value })} />
          <TextField label={t('department')} value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })} />
          <TextField label={t('phone')} value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>{t('cancel')}</Button>
          <Button variant="contained" onClick={handleSave}>{t('save')}</Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!snack} autoHideDuration={3000} onClose={() => setSnack('')} message={snack} />
    </Box>
  );
}
