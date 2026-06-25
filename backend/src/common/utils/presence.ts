import { User } from '../../users/entities/user.entity';

/** Heartbeat har 30s — onlayn deb hisoblash uchun 90s dan kam bo'lishi kerak */
export const ONLINE_THRESHOLD_MS = 90_000;

/** DB yozuvlarini kamaytirish uchun throttle */
export const PRESENCE_TOUCH_THROTTLE_MS = 15_000;

export function resolveLastSeenAt(user: User): Date | null {
  if (user.lastSeenAt) return user.lastSeenAt;

  const logins = (user.linkedDevices ?? [])
    .map((d) => d.lastLoginAt)
    .filter((v): v is string => !!v)
    .map((v) => new Date(v).getTime())
    .filter((t) => !Number.isNaN(t));

  if (!logins.length) return null;
  return new Date(Math.max(...logins));
}

export function isUserOnline(user: User, now = Date.now()): boolean {
  const lastSeen = resolveLastSeenAt(user);
  if (!lastSeen) return false;
  return now - lastSeen.getTime() < ONLINE_THRESHOLD_MS;
}
