export type LinkedDevice = {
  id: string;
  approved: boolean;
  linkedAt: string;
  lastLoginAt?: string;
};

export function formatDeviceId(id: string): string {
  const trimmed = id.trim();
  if (trimmed.length <= 10) return trimmed.toUpperCase();
  return trimmed.slice(-8).toUpperCase();
}

export function getUserDevices(user: {
  linkedDevices?: LinkedDevice[] | null;
  deviceId?: string | null;
  deviceApproved?: boolean;
}): LinkedDevice[] {
  if (user.linkedDevices?.length) {
    return user.linkedDevices.filter((device) => device.approved);
  }
  if (user.deviceId) {
    return [
      {
        id: user.deviceId,
        approved: user.deviceApproved ?? true,
        linkedAt: '',
      },
    ];
  }
  return [];
}

export function formatDeviceTime(iso?: string): string | null {
  if (!iso) return null;
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return null;
  return date.toLocaleString('uz-UZ', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
