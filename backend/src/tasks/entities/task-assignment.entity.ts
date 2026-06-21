import {
  Column,
  CreateDateColumn,
  Entity,
  JoinColumn,
  ManyToOne,
  PrimaryGeneratedColumn,
} from 'typeorm';
import { TaskStatus } from '../../common/enums';
import { User } from '../../users/entities/user.entity';
import { Task } from './task.entity';

@Entity('task_assignments')
export class TaskAssignment {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @ManyToOne(() => Task, (t) => t.assignments, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'taskId' })
  task: Task;

  @Column()
  taskId: string;

  @ManyToOne(() => User, (u) => u.assignments, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'assigneeId' })
  assignee: User;

  @Column()
  assigneeId: string;

  @Column({ type: 'enum', enum: TaskStatus, default: TaskStatus.NEW })
  status: TaskStatus;

  @Column({ type: 'timestamptz', nullable: true })
  acceptedAt: Date | null;

  @Column({ type: 'timestamptz', nullable: true })
  completedAt: Date | null;

  @CreateDateColumn()
  createdAt: Date;
}
