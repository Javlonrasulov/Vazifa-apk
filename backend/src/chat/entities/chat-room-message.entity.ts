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
import {
  ChatMessageMeta,
  ChatMessageType,
} from './chat-message.entity';
import { ChatRoom } from './chat-room.entity';

@Entity('chat_room_messages')
@Index(['roomId', 'createdAt'])
export class ChatRoomMessage {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @ManyToOne(() => ChatRoom, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'roomId' })
  room: ChatRoom;

  @Column({ type: 'uuid' })
  roomId: string;

  @ManyToOne(() => User)
  @JoinColumn({ name: 'senderId' })
  sender: User;

  @Column()
  senderId: string;

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

  @Column({ type: 'uuid', nullable: true })
  replyToId: string | null;

  @ManyToOne(() => ChatRoomMessage, { nullable: true, onDelete: 'SET NULL' })
  @JoinColumn({ name: 'replyToId' })
  replyTo: ChatRoomMessage | null;

  @Column({ type: 'varchar', nullable: true })
  forwardedFrom: string | null;

  /** Emoji reaksiyalar: { userId: '👍' } */
  @Column({ type: 'simple-json', nullable: true })
  reactions: Record<string, string> | null;

  @Column({ default: false })
  isEdited: boolean;

  @Column({ type: 'timestamptz', nullable: true })
  editedAt: Date | null;

  @Column({ default: false })
  isDeleted: boolean;

  @Column({ default: false })
  isPinned: boolean;

  @Column({ type: 'varchar', nullable: true })
  clientId: string | null;

  @CreateDateColumn()
  createdAt: Date;
}
