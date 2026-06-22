import {
  Column,
  CreateDateColumn,
  Entity,
  Index,
  PrimaryGeneratedColumn,
} from 'typeorm';

export type UserFieldOptionType = 'position' | 'department';

@Entity('user_field_options')
@Index(['type', 'nameNormalized'], { unique: true })
export class UserFieldOption {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'varchar', length: 32 })
  type: UserFieldOptionType;

  @Column()
  name: string;

  @Column()
  nameNormalized: string;

  @CreateDateColumn()
  createdAt: Date;
}
