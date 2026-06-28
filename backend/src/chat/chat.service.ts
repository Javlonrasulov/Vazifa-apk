import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Brackets, In, Repository } from 'typeorm';
import {
  ChatMessage,
  ChatMessageStatus,
  ChatMessageType,
} from './entities/chat-message.entity';
import { SendMessageDto } from './dto/chat.dto';
import { User } from '../users/entities/user.entity';
import { isUserOnline, resolveLastSeenAt } from '../common/utils/presence';
import { mediaUrl } from '../common/utils/media-url';

export interface ConversationSummary {
  peer: {
    id: string;
    fullName: string;
    position: string | null;
    department: string | null;
    isOnline: boolean;
    lastSeenAt: string | null;
  };
  lastMessage: ChatMessage | null;
  unreadCount: number;
}

@Injectable()
export class ChatService {
  constructor(
    @InjectRepository(ChatMessage)
    private repo: Repository<ChatMessage>,
    @InjectRepository(User)
    private users: Repository<User>,
  ) {}

  private withUrls(msg: ChatMessage): ChatMessage {
    if (msg.filePath) {
      msg.meta = { ...(msg.meta ?? {}), fileUrl: mediaUrl(msg.filePath) ?? undefined };
    }
    return msg;
  }

  async getMessageById(id: string): Promise<ChatMessage | null> {
    const msg = await this.repo.findOne({
      where: { id },
      relations: ['replyTo', 'sender'],
    });
    return msg ? this.withUrls(msg) : null;
  }

  async getHistory(userId: string, otherId: string, before?: string, limit = 40) {
    const qb = this.repo
      .createQueryBuilder('m')
      .leftJoinAndSelect('m.replyTo', 'replyTo')
      .where(
        new Brackets((b) => {
          b.where('(m.senderId = :userId AND m.receiverId = :otherId)', { userId, otherId }).orWhere(
            '(m.senderId = :otherId AND m.receiverId = :userId)',
            { userId, otherId },
          );
        }),
      )
      .orderBy('m.createdAt', 'DESC')
      .take(Math.min(limit, 100));

    if (before) {
      qb.andWhere('m.createdAt < :before', { before: new Date(before) });
    }

    const rows = await qb.getMany();
    return rows.reverse().map((m) => this.withUrls(m));
  }

  async getConversations(userId: string): Promise<ConversationSummary[]> {
    const lastRows: ChatMessage[] = await this.repo.query(
      `SELECT t.* FROM (
         SELECT DISTINCT ON (peer) *,
           (CASE WHEN "senderId" = $1 THEN "receiverId" ELSE "senderId" END) AS peer
         FROM chat_messages
         WHERE ("senderId" = $1 OR "receiverId" = $1) AND "isDeleted" = false
         ORDER BY peer, "createdAt" DESC
       ) t
       ORDER BY t."createdAt" DESC`,
      [userId],
    );

    const unreadRows: Array<{ peer: string; cnt: string }> = await this.repo.query(
      `SELECT "senderId" AS peer, COUNT(*)::int AS cnt
       FROM chat_messages
       WHERE "receiverId" = $1 AND "isRead" = false AND "isDeleted" = false
       GROUP BY "senderId"`,
      [userId],
    );
    const unreadMap = new Map(unreadRows.map((r) => [r.peer, Number(r.cnt)]));

    const peerIds = lastRows.map((r: any) => r.peer).filter(Boolean);
    if (!peerIds.length) return [];

    const peers = await this.users.find({ where: { id: In(peerIds) } });
    const peerMap = new Map(peers.map((p) => [p.id, p]));

    return lastRows
      .map((row: any) => {
        const peer = peerMap.get(row.peer);
        if (!peer) return null;
        const lastMessage = Object.assign(new ChatMessage(), row, {
          createdAt: new Date(row.createdAt),
        });
        return {
          peer: {
            id: peer.id,
            fullName: peer.fullName,
            avatarUrl: peer.avatarUrl ?? null,
            position: peer.position,
            department: peer.department,
            isOnline: isUserOnline(peer),
            lastSeenAt: resolveLastSeenAt(peer)?.toISOString() ?? null,
          },
          lastMessage: this.withUrls(lastMessage),
          unreadCount: unreadMap.get(peer.id) ?? 0,
        } as ConversationSummary;
      })
      .filter((c): c is ConversationSummary => !!c);
  }

  async createMessage(sender: User, dto: SendMessageDto, online: boolean) {
    if (sender.id === dto.receiverId) {
      throw new ForbiddenException('O\'zingizga yozib bo\'lmaydi');
    }
    const receiver = await this.users.findOne({ where: { id: dto.receiverId } });
    if (!receiver) throw new NotFoundException('Qabul qiluvchi topilmadi');

    const msg = this.repo.create({
      senderId: sender.id,
      receiverId: dto.receiverId,
      type: dto.type ?? ChatMessageType.TEXT,
      body: dto.body ?? null,
      filePath: dto.filePath ?? null,
      fileName: dto.fileName ?? null,
      mimeType: dto.mimeType ?? null,
      meta: dto.meta ?? null,
      replyToId: dto.replyToId ?? null,
      forwardedFrom: dto.forwardedFrom ?? null,
      clientId: dto.clientId ?? null,
      status: online ? ChatMessageStatus.DELIVERED : ChatMessageStatus.SENT,
      deliveredAt: online ? new Date() : null,
    });
    const saved = await this.repo.save(msg);
    return this.getMessageById(saved.id);
  }

  /** Foydalanuvchi onlayn bo'lganda barcha "sent" xabarlarni "delivered" qiladi */
  async markDeliveredForUser(userId: string): Promise<Array<{ id: string; senderId: string }>> {
    const pending = await this.repo.find({
      where: { receiverId: userId, status: ChatMessageStatus.SENT },
      select: ['id', 'senderId'],
    });
    if (!pending.length) return [];
    await this.repo.update(
      { id: In(pending.map((m) => m.id)) },
      { status: ChatMessageStatus.DELIVERED, deliveredAt: new Date() },
    );
    return pending.map((m) => ({ id: m.id, senderId: m.senderId }));
  }

  async markRead(userId: string, peerId: string, messageIds?: string[]) {
    const qb = this.repo
      .createQueryBuilder()
      .update(ChatMessage)
      .set({ isRead: true, status: ChatMessageStatus.READ, readAt: new Date() })
      .where('receiverId = :userId', { userId })
      .andWhere('senderId = :peerId', { peerId })
      .andWhere('isRead = false');
    if (messageIds?.length) qb.andWhere('id IN (:...ids)', { ids: messageIds });
    await qb.execute();
    return { success: true };
  }

  async getUnreadCount(userId: string): Promise<number> {
    return this.repo.count({
      where: { receiverId: userId, isRead: false, isDeleted: false },
    });
  }

  async editMessage(userId: string, id: string, body: string) {
    const msg = await this.repo.findOne({ where: { id } });
    if (!msg) throw new NotFoundException('Xabar topilmadi');
    if (msg.senderId !== userId) throw new ForbiddenException('Ruxsat yo\'q');
    msg.body = body;
    msg.isEdited = true;
    msg.editedAt = new Date();
    await this.repo.save(msg);
    return this.getMessageById(id);
  }

  async deleteMessage(userId: string, id: string) {
    const msg = await this.repo.findOne({ where: { id } });
    if (!msg) throw new NotFoundException('Xabar topilmadi');
    if (msg.senderId !== userId) throw new ForbiddenException('Ruxsat yo\'q');
    msg.isDeleted = true;
    msg.body = null;
    msg.filePath = null;
    msg.meta = null;
    await this.repo.save(msg);
    return { id, receiverId: msg.receiverId, senderId: msg.senderId };
  }

  async react(userId: string, id: string, emoji: string | null | undefined) {
    const msg = await this.repo.findOne({ where: { id } });
    if (!msg) throw new NotFoundException('Xabar topilmadi');
    if (msg.senderId !== userId && msg.receiverId !== userId) {
      throw new ForbiddenException('Ruxsat yo\'q');
    }
    const reactions = { ...(msg.reactions ?? {}) };
    if (!emoji) {
      delete reactions[userId];
    } else {
      reactions[userId] = emoji;
    }
    msg.reactions = Object.keys(reactions).length ? reactions : null;
    await this.repo.save(msg);
    return this.getMessageById(id);
  }

  async pin(userId: string, id: string) {
    const msg = await this.repo.findOne({ where: { id } });
    if (!msg) throw new NotFoundException('Xabar topilmadi');
    if (msg.senderId !== userId && msg.receiverId !== userId) {
      throw new ForbiddenException('Ruxsat yo\'q');
    }
    msg.isPinned = !msg.isPinned;
    await this.repo.save(msg);
    return this.getMessageById(id);
  }

  async search(userId: string, q: string) {
    const term = `%${q.trim().toLowerCase()}%`;
    if (!q.trim()) return { messages: [], peers: [] };

    const messages = await this.repo
      .createQueryBuilder('m')
      .where(
        new Brackets((b) => {
          b.where('m.senderId = :userId', { userId }).orWhere('m.receiverId = :userId', {
            userId,
          });
        }),
      )
      .andWhere('m.isDeleted = false')
      .andWhere(
        new Brackets((b) => {
          b.where('LOWER(m.body) LIKE :term', { term }).orWhere('LOWER(m.fileName) LIKE :term', {
            term,
          });
        }),
      )
      .orderBy('m.createdAt', 'DESC')
      .take(50)
      .getMany();

    const peers = await this.users
      .createQueryBuilder('u')
      .where('LOWER(u.fullName) LIKE :term', { term })
      .andWhere('u.id != :userId', { userId })
      .andWhere('u.isActive = true')
      .take(20)
      .getMany();

    return {
      messages: messages.map((m) => this.withUrls(m)),
      peers: peers.map((p) => ({
        id: p.id,
        fullName: p.fullName,
        avatarUrl: p.avatarUrl ?? null,
        position: p.position,
        department: p.department,
        isOnline: isUserOnline(p),
        lastSeenAt: resolveLastSeenAt(p)?.toISOString() ?? null,
      })),
    };
  }
}
