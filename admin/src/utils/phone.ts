const PREFIX = '+998';

export function phoneDigits(raw: string): string {
  let s = raw.trim();
  if (s.startsWith('+998')) s = s.slice(4).trimStart();
  else if (s.startsWith('998') && s.length > 3) s = s.slice(3).trimStart();
  return s.replace(/\D/g, '').slice(0, 9);
}

export function formatUzPhone(digits: string): string {
  const d = digits.slice(0, 9);
  if (!d) return `${PREFIX} `;

  let result = PREFIX;
  if (d.length > 0) result += ` ${d.slice(0, 2)}`;
  if (d.length > 2) result += ` ${d.slice(2, 5)}`;
  if (d.length > 5) result += ` ${d.slice(5, 7)}`;
  if (d.length > 7) result += ` ${d.slice(7, 9)}`;
  return result;
}

export function displayPhone(stored: string): string {
  if (!stored.trim()) return `${PREFIX} `;
  return formatUzPhone(phoneDigits(stored));
}

export function phoneForSave(formatted: string): string | null {
  const digits = phoneDigits(formatted);
  if (!digits) return null;
  return formatUzPhone(digits);
}

export const PHONE_PLACEHOLDER = '+998 88 888 88 88';
