import { DateTime } from 'luxon';

const TASHKENT = 'Asia/Tashkent';

export function nowTashkent(): Date {
  return DateTime.now().setZone(TASHKENT).toJSDate();
}

export function toTashkentISO(date: Date): string {
  return DateTime.fromJSDate(date).setZone(TASHKENT).toISO() ?? date.toISOString();
}

export function parseTashkent(dateStr: string): Date {
  const dt = DateTime.fromISO(dateStr, { zone: TASHKENT });
  if (!dt.isValid) return new Date(dateStr);
  return dt.toJSDate();
}

export function hoursUntil(deadline: Date): number {
  const now = DateTime.now().setZone(TASHKENT);
  const end = DateTime.fromJSDate(deadline).setZone(TASHKENT);
  return end.diff(now, 'hours').hours;
}

export function minutesUntil(deadline: Date): number {
  const now = DateTime.now().setZone(TASHKENT);
  const end = DateTime.fromJSDate(deadline).setZone(TASHKENT);
  return end.diff(now, 'minutes').minutes;
}

export function serverTimeResponse() {
  const now = DateTime.now().setZone(TASHKENT);
  return {
    timezone: TASHKENT,
    iso: now.toISO(),
    formatted: now.toFormat('dd.MM.yyyy HH:mm:ss'),
    unix: now.toUnixInteger(),
  };
}
