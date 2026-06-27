import { basename } from 'path';

/** filePath (masalan "uploads/abc.jpg") dan ommaviy URL (`/uploads/abc.jpg`) hosil qiladi */
export function mediaUrl(filePath?: string | null): string | null {
  if (!filePath) return null;
  const name = basename(filePath.replace(/\\/g, '/'));
  return `/uploads/${name}`;
}
