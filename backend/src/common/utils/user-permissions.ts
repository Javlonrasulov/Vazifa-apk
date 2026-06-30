import { UserRole } from '../enums';
import { User } from '../../users/entities/user.entity';

/** Mobil ilovadagi barcha foydalanuvchilar vazifa bera oladi. */
export function userCanAssignTasks(user: User): boolean {
  return user.role === UserRole.DIRECTOR || user.role === UserRole.EMPLOYEE;
}
