import {
  Column,
  CreateDateColumn,
  Entity,
  OneToMany,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';
import { UserRole } from '../../common/enums';
import { TaskAssignment } from '../../tasks/entities/task-assignment.entity';
import { TaskComment } from '../../tasks/entities/task-comment.entity';
import { AuditLog } from '../../audit/entities/audit-log.entity';

@Entity('users')
export class User {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ unique: true })
  login: string;

  @Column()
  passwordHash: string;

  @Column()
  fullName: string;

  @Column({ type: 'enum', enum: UserRole, default: UserRole.EMPLOYEE })
  role: UserRole;

  @Column({ default: false })
  canAssignTasks: boolean;

  @Column({ type: 'varchar', nullable: true })
  position: string | null;

  @Column({ type: 'varchar', nullable: true })
  department: string | null;

  @Column({ type: 'varchar', nullable: true })
  phone: string | null;

  @Column({ type: 'varchar', nullable: true })
  deviceId: string | null;

  @Column({ default: false })
  deviceApproved: boolean;

  @Column({ type: 'varchar', nullable: true })
  pendingDeviceId: string | null;

  @Column({ type: 'varchar', nullable: true })
  fcmToken: string | null;

  @Column({ default: true })
  notificationsEnabled: boolean;

  @Column({ default: true })
  isActive: boolean;

  @Column({ default: false })
  canAccessAdminPanel: boolean;

  @Column({ type: 'simple-json', nullable: true })
  adminPermissions: string[] | null;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;

  @OneToMany(() => TaskAssignment, (a) => a.assignee)
  assignments: TaskAssignment[];

  @OneToMany(() => TaskComment, (c) => c.author)
  comments: TaskComment[];

  @OneToMany(() => AuditLog, (l) => l.user)
  auditLogs: AuditLog[];
}
