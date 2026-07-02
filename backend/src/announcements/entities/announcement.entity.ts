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
import { User } from '../../users/entities/user.entity';
import { AnnouncementRecipient } from './announcement-recipient.entity';
import { AnnouncementAttachment } from './announcement-attachment.entity';

export enum AnnouncementStatus {
  ACTIVE = 'active',
  CANCELLED = 'cancelled',
  EXPIRED = 'expired',
}

@Entity('announcements')
export class Announcement {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column()
  title: string;

  @Column({ type: 'text', nullable: true })
  description: string | null;

  @Column({ type: 'timestamptz' })
  deadlineAt: Date;

  /** Eslatma intervali daqiqalarda */
  @Column({ type: 'int' })
  reminderIntervalMinutes: number;

  @Column({ type: 'enum', enum: AnnouncementStatus, default: AnnouncementStatus.ACTIVE })
  status: AnnouncementStatus;

  @ManyToOne(() => User, { nullable: false })
  @JoinColumn({ name: 'createdById' })
  createdBy: User;

  @Column()
  createdById: string;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;

  @OneToMany(() => AnnouncementRecipient, (r) => r.announcement, { cascade: true })
  recipients: AnnouncementRecipient[];

  @OneToMany(() => AnnouncementAttachment, (a) => a.announcement, { cascade: true })
  attachments: AnnouncementAttachment[];
}
