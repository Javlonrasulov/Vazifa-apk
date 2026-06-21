import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
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

const roleLabel: Record<string, string> = {
  director: 'Direktor',
  employee: 'Xodim',
  admin: 'Admin',
};

export default function EmployeesPage() {
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
        setSnack('Yangilandi');
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
        setSnack('Qo\'shildi');
      }
      setDialogOpen(false);
      load();
    } catch (e: unknown) {
      setSnack('Xatolik yuz berdi');
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('O\'chirishni tasdiqlaysizmi?')) return;
    await deleteUser(id);
    setSnack('O\'chirildi');
    load();
  };

  const handleResetPassword = async (u: User) => {
    const pw = prompt('Yangi parol (min 6 belgi):');
    if (!pw || pw.length < 6) return;
    await resetPassword(u.id, pw);
    setSnack('Parol yangilandi');
  };

  const handleResetDevice = async (id: string) => {
    await resetDevice(id);
    setSnack('Qurilma ulandi');
    load();
  };

  const handleApproveDevice = async (id: string, approve: boolean) => {
    await approveDevice(id, approve);
    setSnack(approve ? 'Qurilma tasdiqlandi' : 'Rad etildi');
    load();
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700}>
            Xodimlar va Direktorlar
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Login, parol, lavozim va rol shu yerda belgilanadi
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
          Qo&apos;shish
        </Button>
      </Box>

      {loading ? (
        <Typography>Yuklanmoqda...</Typography>
      ) : (
        <Table sx={{ bgcolor: 'background.paper', borderRadius: 2, overflow: 'hidden' }}>
          <TableHead>
            <TableRow sx={{ bgcolor: '#2563EB', '& th': { color: 'white', fontWeight: 600 } }}>
              <TableCell>Ism</TableCell>
              <TableCell>Login</TableCell>
              <TableCell>Rol</TableCell>
              <TableCell>Bo&apos;lim</TableCell>
              <TableCell>Qurilma</TableCell>
              <TableCell align="right">Amallar</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {users.map((u) => (
              <TableRow key={u.id} hover>
                <TableCell>{u.fullName}</TableCell>
                <TableCell>{u.login}</TableCell>
                <TableCell>
                  <Chip
                    label={roleLabel[u.role]}
                    size="small"
                    color={u.role === 'director' ? 'primary' : 'default'}
                  />
                </TableCell>
                <TableCell>{u.department ?? '—'}</TableCell>
                <TableCell>
                  {u.pendingDeviceId ? (
                    <Box sx={{ display: 'flex', gap: 0.5 }}>
                      <IconButton size="small" color="success" title="Tasdiqlash" onClick={() => handleApproveDevice(u.id, true)}>
                        <CheckCircleIcon fontSize="small" />
                      </IconButton>
                      <IconButton size="small" color="error" title="Rad etish" onClick={() => handleApproveDevice(u.id, false)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Box>
                  ) : u.deviceId ? (
                    <Chip label="Bog'langan" size="small" color="success" variant="outlined" />
                  ) : (
                    <Chip label="Yo'q" size="small" variant="outlined" />
                  )}
                </TableCell>
                <TableCell align="right">
                  <IconButton size="small" onClick={() => openEdit(u)}><EditIcon /></IconButton>
                  <IconButton size="small" onClick={() => handleResetPassword(u)}><LockResetIcon /></IconButton>
                  <IconButton size="small" onClick={() => handleResetDevice(u.id)}><PhoneAndroidIcon /></IconButton>
                  <IconButton size="small" color="error" onClick={() => handleDelete(u.id)}><DeleteIcon /></IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editUser ? 'Tahrirlash' : 'Yangi xodim / direktor'}</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
          {!editUser && (
            <>
              <TextField label="Login" value={form.login} onChange={(e) => setForm({ ...form, login: e.target.value })} required />
              <TextField label="Parol" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
            </>
          )}
          <TextField label="To'liq ism" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
          <FormControl fullWidth>
            <InputLabel>Rol</InputLabel>
            <Select value={form.role} label="Rol" onChange={(e) => setForm({ ...form, role: e.target.value as 'director' | 'employee' })}>
              <MenuItem value="employee">Xodim</MenuItem>
              <MenuItem value="director">Direktor</MenuItem>
            </Select>
          </FormControl>
          <TextField label="Lavozim" value={form.position} onChange={(e) => setForm({ ...form, position: e.target.value })} />
          <TextField label="Bo'lim" value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })} />
          <TextField label="Telefon" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Bekor</Button>
          <Button variant="contained" onClick={handleSave}>Saqlash</Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!snack} autoHideDuration={3000} onClose={() => setSnack('')} message={snack} />
    </Box>
  );
}
