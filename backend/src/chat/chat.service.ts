import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ChatMessage } from './entities/chat-message.entity';
import { SendChatMessageDto } from '../tasks/dto/task.dto';
import { User } from '../users/entities/user.entity';
import { UserRole } from '../common/enums';
import { NotificationsService } from '../notifications/notifications.service';

@Injectable()
export class ChatService {
  constructor(
    @InjectRepository(ChatMessage)
    private repo: Repository<ChatMessage>,
    private notifications: NotificationsService,
  ) {}

  async getConversation(userId: string, otherId: string) {
    return this.repo
      .createQueryBuilder('m')
      .where(
        '(m.senderId = :userId AND m.receiverId = :otherId) OR (m.senderId = :otherId AND m.receiverId = :userId)',
        { userId, otherId },
      )
      .orderBy('m.createdAt', 'ASC')
      .getMany();
  }

  async send(sender: User, dto: SendChatMessageDto, file?: Express.Multer.File) {
    const allowed =
      (sender.role === UserRole.DIRECTOR && dto.receiverId) ||
      (sender.role === UserRole.EMPLOYEE && dto.receiverId);
    if (!allowed) throw new Error('Ruxsat yo\'q');

    const msg = this.repo.create({
      senderId: sender.id,
      receiverId: dto.receiverId,
      body: dto.body ?? null,
      filePath: file?.path?.replace(/\\/g, '/') ?? null,
      fileName: file?.originalname ?? null,
      mimeType: file?.mimetype ?? null,
    });
    const saved = await this.repo.save(msg);

    // Notify receiver via FCM — fetch would need UsersService; simplified
    return saved;
  }

  async markRead(messageIds: string[], userId: string) {
    await this.repo
      .createQueryBuilder()
      .update(ChatMessage)
      .set({ isRead: true })
      .where('id IN (:...ids)', { ids: messageIds })
      .andWhere('receiverId = :userId', { userId })
      .execute();
    return { success: true };
  }
}
