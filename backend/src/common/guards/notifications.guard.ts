import { Injectable, CanActivate, ExecutionContext, ForbiddenException } from '@nestjs/common';
import { UserRole } from '../enums';

/** Blocks mobile app access if push notifications are disabled (director/employee only). */
@Injectable()
export class NotificationsGuard implements CanActivate {
  canActivate(context: ExecutionContext): boolean {
    const { user } = context.switchToHttp().getRequest();
    if (!user) return true;
    if (user.role === UserRole.ADMIN) return true;
    if (!user.notificationsEnabled || !user.fcmToken) {
      throw new ForbiddenException({
        code: 'NOTIFICATIONS_REQUIRED',
        message: 'Ilovadan foydalanish uchun bildirishnomalarni yoqing',
      });
    }
    return true;
  }
}
