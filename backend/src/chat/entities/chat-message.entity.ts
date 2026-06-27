import {
  Column,
  CreateDateColumn,
  Entity,
  Index,
  JoinColumn,
  ManyToOne,
  PrimaryGeneratedColumn,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';

export enum ChatMessageType {
  TEXT = 'text',
  IMAGE = 'image',
  VIDEO = 'video',
  AUDIO = 'audio',
  VOICE = 'voice',
  FILE = 'file',
  STICKER = 'sticker',
  GIF = 'gif',
  CONTACT = 'contact',
  LOCATION = 'location',
}

export enum ChatMessageStatus {
  SENT = 'sent',
  DELIVERED = 'delivered',
  READ = 'read',
}

/** Telegram uslubidagi xabar metadatasi (joylashuv, kontakt, ovoz, media o'lchamlari) */
export interface ChatMessageMeta {
  fileUrl?: string;
  fileSize?: number;
  durationSec?: number;
  waveform?: number[];
  width?: number;
  height?: number;
  thumbUrl?: string;
  latitude?: number;
  longitude?: number;
  contactName?: string;
  contactPhone?: string;
  stickerId?: string;
}

@Entity('chat_messages')
@Index(['senderId', 'receiverId', 'createdAt'])
export class ChatMessage {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @ManyToOne(() => User)
  @JoinColumn({ name: 'senderId' })
  sender: User;

  @Column()
  senderId: string;

  @ManyToOne(() => User)
  @JoinColumn({ name: 'receiverId' })
  receiver: User;

  @Column()
  receiverId: string;

  @Column({ type: 'enum', enum: ChatMessageType, default: ChatMessageType.TEXT })
  type: ChatMessageType;

  @Column({ type: 'text', nullable: true })
  body: string | null;

  @Column({ type: 'varchar', nullable: true })
  filePath: string | null;

  @Column({ type: 'varchar', nullable: true })
  fileName: string | null;

  @Column({ type: 'varchar', nullable: true })
  mimeType: string | null;

  @Column({ type: 'simple-json', nullable: true })
  meta: ChatMessageMeta | null;

  /** Reply qilingan xabar (Telegram reply) */
  @Column({ type: 'uuid', nullable: true })
  replyToId: string | null;

  @ManyToOne(() => ChatMessage, { nullable: true, onDelete: 'SET NULL' })
  @JoinColumn({ name: 'replyToId' })
  replyTo: ChatMessage | null;

  /** Forward qilingan asl jo'natuvchi ismi */
  @Column({ type: 'varchar', nullable: true })
  forwardedFrom: string | null;

  /** Emoji reaksiyalar: { userId: '👍' } */
  @Column({ type: 'simple-json', nullable: true })
  reactions: Record<string, string> | null;

  @Column({ type: 'enum', enum: ChatMessageStatus, default: ChatMessageStatus.SENT })
  status: ChatMessageStatus;

  @Column({ default: false })
  isRead: boolean;

  @Column({ type: 'timestamptz', nullable: true })
  deliveredAt: Date | null;

  @Column({ type: 'timestamptz', nullable: true })
  readAt: Date | null;

  @Column({ default: false })
  isEdited: boolean;

  @Column({ type: 'timestamptz', nullable: true })
  editedAt: Date | null;

  @Column({ default: false })
  isDeleted: boolean;

  @Column({ default: false })
  isPinned: boolean;

  /** Optimistik UI uchun klient tomonidan berilgan id (dedup) */
  @Column({ type: 'varchar', nullable: true })
  clientId: string | null;

  @CreateDateColumn()
  createdAt: Date;
}
