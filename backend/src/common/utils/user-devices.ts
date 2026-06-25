import { User } from '../../users/entities/user.entity';

export const MAX_USER_DEVICES = 2;

export type LinkedDevice = {
  id: string;
  name?: string;
  approved: boolean;
  linkedAt: string;
  lastLoginAt?: string;
};

export function formatDeviceId(id: string): string {
  const trimmed = id.trim();
  if (trimmed.length <= 10) return trimmed.toUpperCase();
  return trimmed.slice(-8).toUpperCase();
}

export function formatDeviceLabel(device: Pick<LinkedDevice, 'id' | 'name'>): string {
  const name = device.name?.trim();
  if (name) return name;
  return formatDeviceId(device.id);
}

export function getLinkedDevices(user: User): LinkedDevice[] {
  if (user.linkedDevices?.length) {
    return user.linkedDevices.map((device) => ({ ...device }));
  }

  if (user.deviceId) {
    return [
      {
        id: user.deviceId,
        approved: user.deviceApproved,
        linkedAt: user.updatedAt?.toISOString?.() ?? new Date().toISOString(),
        lastLoginAt: user.updatedAt?.toISOString?.(),
      },
    ];
  }

  return [];
}

export function getApprovedDevices(user: User): LinkedDevice[] {
  return getLinkedDevices(user).filter((device) => device.approved);
}

export function syncLegacyDeviceFields(user: User): void {
  const approved = getApprovedDevices(user);
  user.linkedDevices = getLinkedDevices(user);
  user.deviceId = approved[0]?.id ?? null;
  user.deviceApproved = approved.length > 0;
  user.pendingDeviceId = null;
}

export type DeviceBindResult = 'ok' | 'limit';

export function bindUserDevice(
  user: User,
  deviceId: string,
  deviceName?: string | null,
): DeviceBindResult {
  const now = new Date().toISOString();
  const devices = getLinkedDevices(user);
  const existing = devices.find((device) => device.id === deviceId);
  const normalizedName = deviceName?.trim() || undefined;

  if (existing) {
    existing.approved = true;
    existing.lastLoginAt = now;
    if (normalizedName) existing.name = normalizedName;
    user.linkedDevices = devices;
    syncLegacyDeviceFields(user);
    return 'ok';
  }

  const approvedCount = devices.filter((device) => device.approved).length;
  if (approvedCount >= MAX_USER_DEVICES) {
    return 'limit';
  }

  devices.push({
    id: deviceId,
    name: normalizedName,
    approved: true,
    linkedAt: now,
    lastLoginAt: now,
  });
  user.linkedDevices = devices;
  syncLegacyDeviceFields(user);
  return 'ok';
}

export function resetUserDevices(user: User): void {
  user.linkedDevices = null;
  user.deviceId = null;
  user.deviceApproved = false;
  user.pendingDeviceId = null;
}
