import {
  Column,
  CreateDateColumn,
  Entity,
  JoinColumn,
  ManyToOne,
  PrimaryGeneratedColumn,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { Announcement } from './announcement.entity';

@Entity('announcement_recipients')
export class AnnouncementRecipient {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @ManyToOne(() => Announcement, (a) => a.recipients, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'announcementId' })
  announcement: Announcement;

  @Column()
  announcementId: string;

  @ManyToOne(() => User, { nullable: false })
  @JoinColumn({ name: 'recipientId' })
  recipient: User;

  @Column()
  recipientId: string;

  @Column({ type: 'timestamptz', nullable: true })
  acknowledgedAt: Date | null;

  @Column({ type: 'timestamptz', nullable: true })
  viewedAt: Date | null;

  @Column({ type: 'timestamptz', nullable: true })
  lastReminderAt: Date | null;

  @CreateDateColumn()
  createdAt: Date;
}
