import {
  Column,
  CreateDateColumn,
  Entity,
  JoinColumn,
  ManyToOne,
  OneToMany,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';
import { TaskPriority, TaskStatus } from '../../common/enums';
import { User } from '../../users/entities/user.entity';
import { TaskAssignment } from './task-assignment.entity';
import { TaskAttachment } from './task-attachment.entity';
import { TaskComment } from './task-comment.entity';

@Entity('tasks')
export class Task {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column()
  title: string;

  @Column({ type: 'text', nullable: true })
  description: string | null;

  @Column({ type: 'enum', enum: TaskPriority, default: TaskPriority.MEDIUM })
  priority: TaskPriority;

  @Column({ type: 'enum', enum: TaskStatus, default: TaskStatus.NEW })
  status: TaskStatus;

  @Column({ type: 'timestamptz' })
  startAt: Date;

  @Column({ type: 'timestamptz' })
  deadlineAt: Date;

  @ManyToOne(() => User, { nullable: false })
  @JoinColumn({ name: 'createdById' })
  createdBy: User;

  @Column()
  createdById: string;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;

  @OneToMany(() => TaskAssignment, (a) => a.task, { cascade: true })
  assignments: TaskAssignment[];

  @OneToMany(() => TaskAttachment, (a) => a.task, { cascade: true })
  attachments: TaskAttachment[];

  @OneToMany(() => TaskComment, (c) => c.task, { cascade: true })
  comments: TaskComment[];
}
