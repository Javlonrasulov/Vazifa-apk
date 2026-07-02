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

  @Column({ type: 'varchar', nullable: true })
  passwordPlain: string | null;

  @Column()
  fullName: string;

  @Column({ type: 'varchar', nullable: true })
  avatarUrl: string | null;

  @Column({ type: 'enum', enum: UserRole, default: UserRole.EMPLOYEE })
  role: UserRole;

  @Column({ default: false })
  canAssignTasks: boolean;

  @Column({ default: true })
  allowScreenshot: boolean;

  @Column({ type: 'varchar', nullable: true })
  position: string | null;

  @Column({ type: 'varchar', nullable: true })
  department: string | null;

  @Column({ type: 'simple-json', nullable: true })
  visibleDepartments: string[] | null;

  @Column({ type: 'varchar', nullable: true })
  phone: string | null;

  @Column({ type: 'varchar', nullable: true })
  deviceId: string | null;

  @Column({ default: false })
  deviceApproved: boolean;

  @Column({ type: 'varchar', nullable: true })
  pendingDeviceId: string | null;

  @Column({ type: 'simple-json', nullable: true })
  linkedDevices: Array<{
    id: string;
    name?: string;
    approved: boolean;
    linkedAt: string;
    lastLoginAt?: string;
  }> | null;

  @Column({ type: 'varchar', nullable: true })
  fcmToken: string | null;

  @Column({ default: true })
  notificationsEnabled: boolean;

  /** Dam olish vaqti boshlanishi, "23:00" ko'rinishida (Toshkent vaqti) */
  @Column({ type: 'varchar', nullable: true })
  restStart: string | null;

  /** Dam olish vaqti tugashi, "08:00" ko'rinishida (Toshkent vaqti) */
  @Column({ type: 'varchar', nullable: true })
  restEnd: string | null;

  /** Dam olish kunlari: 0=yakshanba ... 6=shanba */
  @Column({ type: 'simple-json', nullable: true })
  restDays: number[] | null;

  @Column({ type: 'varchar', default: 'uz_kril' })
  language: string;

  @Column({ default: true })
  isActive: boolean;

  @Column({ type: 'timestamptz', nullable: true })
  lastSeenAt: Date | null;

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
