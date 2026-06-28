import {
  Column,
  CreateDateColumn,
  Entity,
  Index,
  JoinColumn,
  ManyToOne,
  OneToMany,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';

export enum ChatRoomType {
  GROUP = 'group',
  CHANNEL = 'channel',
}

export enum ChatRoomMemberRole {
  OWNER = 'owner',
  ADMIN = 'admin',
  MEMBER = 'member',
}

@Entity('chat_rooms')
export class ChatRoom {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'enum', enum: ChatRoomType })
  type: ChatRoomType;

  @Column()
  title: string;

  @Column({ type: 'text', nullable: true })
  description: string | null;

  @Column({ type: 'varchar', nullable: true })
  avatarUrl: string | null;

  @ManyToOne(() => User)
  @JoinColumn({ name: 'ownerId' })
  owner: User;

  @Column()
  ownerId: string;

  @Column({ default: false })
  isVerified: boolean;

  @OneToMany(() => ChatRoomMember, (m) => m.room)
  members: ChatRoomMember[];

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}

@Entity('chat_room_members')
@Index(['roomId', 'userId'], { unique: true })
export class ChatRoomMember {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @ManyToOne(() => ChatRoom, (r) => r.members, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'roomId' })
  room: ChatRoom;

  @Column({ type: 'uuid' })
  roomId: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'userId' })
  user: User;

  @Column({ type: 'uuid' })
  userId: string;

  @Column({ type: 'enum', enum: ChatRoomMemberRole, default: ChatRoomMemberRole.MEMBER })
  role: ChatRoomMemberRole;

  @Column({ default: false })
  muted: boolean;

  /** Oxirgi o'qilgan xabar vaqti (unread hisoblash uchun) */
  @Column({ type: 'timestamptz', nullable: true })
  lastReadAt: Date | null;

  @CreateDateColumn()
  joinedAt: Date;
}
