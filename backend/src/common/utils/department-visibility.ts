import { Task } from '../../tasks/entities/task.entity';
import { User } from '../../users/entities/user.entity';

export function normalizeDepartmentName(name: string): string {
  return name.trim().toLowerCase();
}

export function userHasDepartmentVisibility(user: User): boolean {
  return Array.isArray(user.visibleDepartments) && user.visibleDepartments.length > 0;
}

export function departmentInList(
  department: string | null | undefined,
  visibleDepartments: string[],
): boolean {
  if (!department?.trim()) return false;
  const normalized = normalizeDepartmentName(department);
  return visibleDepartments.some((d) => normalizeDepartmentName(d) === normalized);
}

export function taskMatchesDepartmentVisibility(
  task: Task,
  visibleDepartments: string[],
): boolean {
  if (departmentInList(task.createdBy?.department, visibleDepartments)) {
    return true;
  }
  return (
    task.assignments?.some((a) =>
      departmentInList(a.assignee?.department, visibleDepartments),
    ) ?? false
  );
}

export function canViewTaskViaDepartment(user: User, task: Task): boolean {
  if (!userHasDepartmentVisibility(user)) return false;
  return taskMatchesDepartmentVisibility(task, user.visibleDepartments as string[]);
}
