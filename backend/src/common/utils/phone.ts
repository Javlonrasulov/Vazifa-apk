export function phoneDigits(raw: string): string {
  let s = raw.trim();
  if (s.startsWith('+998')) s = s.slice(4).trimStart();
  else if (s.startsWith('998') && s.length > 3) s = s.slice(3).trimStart();
  return s.replace(/\D/g, '').slice(0, 9);
}

export function normalizePhoneDigits(raw: string): string | null {
  const digits = phoneDigits(raw);
  return digits.length === 9 ? `998${digits}` : null;
}

export function formatUzPhone(digits: string): string {
  const d = digits.slice(0, 9);
  if (!d) return '+998 ';

  let result = '+998';
  if (d.length > 0) result += ` ${d.slice(0, 2)}`;
  if (d.length > 2) result += ` ${d.slice(2, 5)}`;
  if (d.length > 5) result += ` ${d.slice(5, 7)}`;
  if (d.length > 7) result += ` ${d.slice(7, 9)}`;
  return result;
}

export function phonesMatch(a: string, b: string): boolean {
  const da = phoneDigits(a);
  const db = phoneDigits(b);
  return da.length === 9 && da === db;
}
