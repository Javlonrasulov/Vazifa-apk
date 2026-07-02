import { basename } from 'path';
import {
  AfterLoad,
  Column,
  CreateDateColumn,
  Entity,
  JoinColumn,
  ManyToOne,
  PrimaryGeneratedColumn,
} from 'typeorm';
import { Announcement } from './announcement.entity';
import { User } from '../../users/entities/user.entity';

@Entity('announcement_attachments')
export class AnnouncementAttachment {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @ManyToOne(() => Announcement, (a) => a.attachments, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'announcementId' })
  announcement: Announcement;

  @Column()
  announcementId: string;

  @ManyToOne(() => User)
  @JoinColumn({ name: 'uploadedById' })
  uploadedBy: User;

  @Column()
  uploadedById: string;

  @Column()
  fileName: string;

  @Column()
  filePath: string;

  @Column()
  mimeType: string;

  @Column({ type: 'int', default: 0 })
  fileSize: number;

  @CreateDateColumn()
  createdAt: Date;

  url?: string;

  @AfterLoad()
  setPublicUrl() {
    const fileName = basename(this.filePath.replace(/\\/g, '/'));
    this.url = `/uploads/${fileName}`;
  }
}
