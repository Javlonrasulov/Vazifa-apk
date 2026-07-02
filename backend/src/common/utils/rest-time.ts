import { DateTime } from 'luxon';
import { User } from '../../users/entities/user.entity';

const TASHKENT = 'Asia/Tashkent';

function parseTime(value: string | null | undefined): number | null {
  if (!value) return null;
  const m = /^([01]\d|2[0-3]):([0-5]\d)$/.exec(value.trim());
  if (!m) return null;
  return Number(m[1]) * 60 + Number(m[2]);
}

/** luxon weekday: 1=dushanba..7=yakshanba -> bizniki: 0=yakshanba..6=shanba */
function toRestDayIndex(luxonWeekday: number): number {
  return luxonWeekday % 7;
}

function inQuietHours(minutesOfDay: number, start: number, end: number): boolean {
  if (start === end) return false;
  if (start < end) return minutesOfDay >= start && minutesOfDay < end;
  // Yarim tundan oshib ketadigan oraliq (masalan 23:00–08:00)
  return minutesOfDay >= start || minutesOfDay < end;
}

/** Hozir foydalanuvchining dam olish vaqtimi (soat oralig'i yoki dam kuni). */
export function isUserResting(user: User, at?: Date): boolean {
  const now = at
    ? DateTime.fromJSDate(at).setZone(TASHKENT)
    : DateTime.now().setZone(TASHKENT);

  if (user.restDays?.includes(toRestDayIndex(now.weekday))) return true;

  const start = parseTime(user.restStart);
  const end = parseTime(user.restEnd);
  if (start === null || end === null) return false;
  return inQuietHours(now.hour * 60 + now.minute, start, end);
}

/** Dam olish tugaydigan eng yaqin vaqt. Dam bo'lmasa hozirgi vaqt qaytadi. */
export function nextAllowedTime(user: User, from?: Date): Date {
  let t = from
    ? DateTime.fromJSDate(from).setZone(TASHKENT)
    : DateTime.now().setZone(TASHKENT);

  const start = parseTime(user.restStart);
  const end = parseTime(user.restEnd);

  // Maksimal 8 kun oldinga qaraymiz (hamma kun dam bo'lsa ham to'xtaydi)
  for (let i = 0; i < 8 * 24; i++) {
    if (user.restDays?.includes(toRestDayIndex(t.weekday))) {
      t = t.plus({ days: 1 }).startOf('day');
      continue;
    }
    if (start !== null && end !== null) {
      const minutes = t.hour * 60 + t.minute;
      if (inQuietHours(minutes, start, end)) {
        if (start < end || minutes < end) {
          // Shu kun ichida tugaydi
          t = t.startOf('day').plus({ minutes: end });
        } else {
          // Yarim tundan oshadigan oraliq — ertaga end da tugaydi
          t = t.plus({ days: 1 }).startOf('day').plus({ minutes: end });
        }
        continue;
      }
    }
    return t.toJSDate();
  }
  return t.toJSDate();
}
