import ExcelJS from 'exceljs';
import type { User } from '../api';
import { formatDeviceLabel, formatDeviceTime, getUserDevices } from './devices';
import { displayPhone } from './phone';

export type ExportColumnLabels = {
  num: string;
  fullName: string;
  login: string;
  password: string;
  phone: string;
  role: string;
  position: string;
  department: string;
  canAssignTasks: string;
  allowScreenshot: string;
  visibleDepartments: string;
  devices: string;
  status: string;
  createdAt: string;
  statusActive: string;
  statusInactive: string;
  yes: string;
  no: string;
};

const HEADER_FILL: ExcelJS.Fill = {
  type: 'pattern',
  pattern: 'solid',
  fgColor: { argb: 'FF4F46E5' },
};

const ALT_ROW_FILL: ExcelJS.Fill = {
  type: 'pattern',
  pattern: 'solid',
  fgColor: { argb: 'FFF8FAFC' },
};

const THIN_BORDER: Partial<ExcelJS.Borders> = {
  top: { style: 'thin', color: { argb: 'FFE2E8F0' } },
  left: { style: 'thin', color: { argb: 'FFE2E8F0' } },
  bottom: { style: 'thin', color: { argb: 'FFE2E8F0' } },
  right: { style: 'thin', color: { argb: 'FFE2E8F0' } },
};

function formatDevices(user: User): string {
  const devices = getUserDevices(user);
  if (!devices.length) return '—';
  return devices
    .map((d) => {
      const label = formatDeviceLabel(d);
      const time = formatDeviceTime(d.lastLoginAt);
      return time ? `${label} (${time})` : label;
    })
    .join('\n');
}

function formatDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function filterEmployeesForExport(
  users: User[],
  selectedDepartmentKeys: Set<string>,
  includeNoDepartment: boolean,
): User[] {
  return users
    .filter((u) => {
      const dept = (u.department ?? '').trim();
      if (!dept) return includeNoDepartment;
      const norm = dept.toLowerCase();
      return selectedDepartmentKeys.has(norm);
    })
    .sort((a, b) => {
      const deptCmp = (a.department ?? '').localeCompare(b.department ?? '', 'uz');
      if (deptCmp !== 0) return deptCmp;
      return a.fullName.localeCompare(b.fullName, 'uz');
    });
}

export async function exportEmployeesToExcel(
  users: User[],
  labels: ExportColumnLabels,
  roleLabel: (role: string) => string,
): Promise<void> {
  const workbook = new ExcelJS.Workbook();
  workbook.creator = 'Lider Vazifa Admin';
  workbook.created = new Date();

  const sheet = workbook.addWorksheet('Xodimlar', {
    views: [{ state: 'frozen', ySplit: 1, activeCell: 'A2' }],
    properties: { defaultRowHeight: 22 },
  });

  const columns: { header: string; key: string; width: number; wrap?: boolean }[] = [
    { header: labels.num, key: 'num', width: 6 },
    { header: labels.fullName, key: 'fullName', width: 34 },
    { header: labels.login, key: 'login', width: 14 },
    { header: labels.password, key: 'password', width: 14 },
    { header: labels.phone, key: 'phone', width: 18 },
    { header: labels.role, key: 'role', width: 14 },
    { header: labels.position, key: 'position', width: 22 },
    { header: labels.department, key: 'department', width: 20 },
    { header: labels.canAssignTasks, key: 'canAssignTasks', width: 14 },
    { header: labels.allowScreenshot, key: 'allowScreenshot', width: 14 },
    { header: labels.visibleDepartments, key: 'visibleDepartments', width: 36, wrap: true },
    { header: labels.devices, key: 'devices', width: 32, wrap: true },
    { header: labels.status, key: 'status', width: 12 },
    { header: labels.createdAt, key: 'createdAt', width: 18 },
  ];

  sheet.columns = columns.map((c) => ({ header: c.header, key: c.key, width: c.width }));

  const headerRow = sheet.getRow(1);
  headerRow.height = 30;
  headerRow.eachCell((cell) => {
    cell.fill = HEADER_FILL;
    cell.font = { bold: true, color: { argb: 'FFFFFFFF' }, size: 11, name: 'Calibri' };
    cell.alignment = { vertical: 'middle', horizontal: 'center', wrapText: true };
    cell.border = THIN_BORDER;
  });

  users.forEach((user, index) => {
    const row = sheet.addRow({
      num: index + 1,
      fullName: user.fullName,
      login: user.login,
      password: user.passwordPlain ?? '—',
      phone: user.phone ? displayPhone(user.phone) : '—',
      role: roleLabel(user.role),
      position: user.position?.trim() || '—',
      department: user.department?.trim() || '—',
      canAssignTasks: user.canAssignTasks ? labels.yes : labels.no,
      allowScreenshot: user.allowScreenshot !== false ? labels.yes : labels.no,
      visibleDepartments: user.visibleDepartments?.length
        ? user.visibleDepartments.join(', ')
        : '—',
      devices: formatDevices(user),
      status: user.isActive ? labels.statusActive : labels.statusInactive,
      createdAt: formatDate(user.createdAt),
    });

    row.height = row.getCell('visibleDepartments').value || row.getCell('devices').value ? 36 : 24;

    row.eachCell({ includeEmpty: true }, (cell, colNumber) => {
      const col = columns[colNumber - 1];
      cell.border = THIN_BORDER;
      cell.font = { size: 11, name: 'Calibri', color: { argb: 'FF1E293B' } };
      cell.alignment = {
        vertical: 'middle',
        horizontal: col?.key === 'num' ? 'center' : 'left',
        wrapText: col?.wrap ?? false,
      };
      if (index % 2 === 1) {
        cell.fill = ALT_ROW_FILL;
      }
    });
  });

  sheet.autoFilter = {
    from: { row: 1, column: 1 },
    to: { row: 1, column: columns.length },
  };

  const buffer = await workbook.xlsx.writeBuffer();
  const blob = new Blob([buffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `xodimlar_${new Date().toISOString().slice(0, 10)}.xlsx`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
