import { User } from '../../users/entities/user.entity';

export function userCanAssignTasks(user: User): boolean {
  return user.canAssignTasks === true;
}
