import { Injectable, CanActivate, ExecutionContext, ForbiddenException } from '@nestjs/common';
import { UserRole } from '../enums';

/** POST/PATCH/DELETE da bildirishnomalar yoqilgan bo'lishi kerak; GET (o'qish) har doim ochiq. */
@Injectable()
export class NotificationsGuard implements CanActivate {
  canActivate(context: ExecutionContext): boolean {
    const req = context.switchToHttp().getRequest();
    const { user } = req;
    if (!user) return true;
    if (user.role === UserRole.ADMIN) return true;
    const method = String(req.method ?? 'GET').toUpperCase();
    if (method === 'GET') return true;
    if (!user.notificationsEnabled) {
      throw new ForbiddenException({
        code: 'NOTIFICATIONS_REQUIRED',
        message: 'Ilovadan foydalanish uchun bildirishnomalarni yoqing',
      });
    }
    return true;
  }
}
