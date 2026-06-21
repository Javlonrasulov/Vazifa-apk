export enum UserRole {
  ADMIN = 'admin',
  DIRECTOR = 'director',
  EMPLOYEE = 'employee',
}

export enum TaskStatus {
  NEW = 'new',
  ACCEPTED = 'accepted',
  IN_PROGRESS = 'in_progress',
  IN_REVIEW = 'in_review',
  COMPLETED = 'completed',
  REWORK = 'rework',
  CANCELLED = 'cancelled',
}

export enum TaskPriority {
  LOW = 'low',
  MEDIUM = 'medium',
  HIGH = 'high',
  URGENT = 'urgent',
}

export enum AuditAction {
  LOGIN = 'login',
  LOGOUT = 'logout',
  TASK_CREATED = 'task_created',
  TASK_UPDATED = 'task_updated',
  TASK_ACCEPTED = 'task_accepted',
  TASK_COMPLETED = 'task_completed',
  TASK_CANCELLED = 'task_cancelled',
  FILE_UPLOADED = 'file_uploaded',
  COMMENT_ADDED = 'comment_added',
  DEVICE_BOUND = 'device_bound',
  DEVICE_RESET = 'device_reset',
  USER_CREATED = 'user_created',
  USER_UPDATED = 'user_updated',
  USER_DELETED = 'user_deleted',
}
