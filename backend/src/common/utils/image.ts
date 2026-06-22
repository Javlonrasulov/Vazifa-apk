import { promises as fs } from 'fs';
import * as path from 'path';
import * as sharp from 'sharp';

const IMAGE_MIMES = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/heic', 'image/heif']);
const MAX_EDGE = 1920;
const JPEG_QUALITY = 80;

export async function compressImageIfNeeded(
  filePath: string,
  mimeType: string,
): Promise<{ filePath: string; mimeType: string; fileSize: number }> {
  if (!IMAGE_MIMES.has(mimeType)) {
    const stat = await fs.stat(filePath);
    return { filePath, mimeType, fileSize: stat.size };
  }

  const dir = path.dirname(filePath);
  const base = path.basename(filePath, path.extname(filePath));
  const outPath = path.join(dir, `${base}.jpg`);

  await sharp(filePath)
    .rotate()
    .resize({ width: MAX_EDGE, height: MAX_EDGE, fit: 'inside', withoutEnlargement: true })
    .jpeg({ quality: JPEG_QUALITY, mozjpeg: true })
    .toFile(outPath);

  if (outPath !== filePath) {
    await fs.unlink(filePath).catch(() => undefined);
  }

  const stat = await fs.stat(outPath);
  return { filePath: outPath.replace(/\\/g, '/'), mimeType: 'image/jpeg', fileSize: stat.size };
}
