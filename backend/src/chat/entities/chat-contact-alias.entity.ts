import {
  Column,
  Entity,
  Index,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
  Unique,
} from 'typeorm';

/** Foydalanuvchi kontaktiga berilgan shaxsiy nom (faqat egasi ko'radi) */
@Entity('chat_contact_aliases')
@Unique(['ownerId', 'peerId'])
@Index(['ownerId'])
export class ChatContactAlias {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column('uuid')
  ownerId: string;

  @Column('uuid')
  peerId: string;

  @Column({ type: 'varchar', length: 120 })
  alias: string;

  @UpdateDateColumn({ type: 'timestamptz' })
  updatedAt: Date;
}
