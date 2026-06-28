import {
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { In, Repository } from 'typeorm';
import {
  ChatRoom,
  ChatRoomMember,
  ChatRoomMemberRole,
  ChatRoomType,
} from './entities/chat-room.entity';
import { ChatRoomMessage } from './entities/chat-room-message.entity';
import { ChatMessageType } from './entities/chat-message.entity';
import { CreateRoomDto, SendRoomMessageDto, UpdateRoomDto } from './dto/room.dto';
import { User } from '../users/entities/user.entity';
import { mediaUrl } from '../common/utils/media-url';

export interface RoomSummary {
  id: string;
  type: ChatRoomType;
  title: string;
  description: string | null;
  avatarUrl: string | null;
  isVerified: boolean;
  ownerId: string;
  myRole: ChatRoomMemberRole;
  memberCount: number;
  muted: boolean;
  canPost: boolean;
  lastMessage: ChatRoomMessage | null;
  unreadCount: number;
}

@Injectable()
export class RoomsService {
  constructor(
    @InjectRepository(ChatRoom)
    private rooms: Repository<ChatRoom>,
    @InjectRepository(ChatRoomMember)
    private members: Repository<ChatRoomMember>,
    @InjectRepository(ChatRoomMessage)
    private messages: Repository<ChatRoomMessage>,
    @InjectRepository(User)
    private users: Repository<User>,
  ) {}

  private withUrls<T extends { filePath: string | null; meta: any }>(msg: T): T {
    if (msg?.filePath) {
      msg.meta = { ...(msg.meta ?? {}), fileUrl: mediaUrl(msg.filePath) ?? undefined };
    }
    return msg;
  }

  private canPost(room: ChatRoom, role: ChatRoomMemberRole): boolean {
    if (room.type === ChatRoomType.CHANNEL) {
      return role === ChatRoomMemberRole.OWNER || role === ChatRoomMemberRole.ADMIN;
    }
    return true;
  }

  async getMembership(roomId: string, userId: string): Promise<ChatRoomMember> {
    const m = await this.members.findOne({ where: { roomId, userId } });
    if (!m) throw new ForbiddenException('Siz bu guruh a\'zosi emassiz');
    return m;
  }

  async create(owner: User, dto: CreateRoomDto): Promise<RoomSummary> {
    const room = await this.rooms.save(
      this.rooms.create({
        type: dto.type,
        title: dto.title.trim(),
        description: dto.description?.trim() || null,
        avatarUrl: dto.avatarUrl ?? null,
        ownerId: owner.id,
      }),
    );

    const memberIds = new Set([owner.id, ...(dto.memberIds ?? [])]);
    const valid = await this.users.find({ where: { id: In([...memberIds]) } });
    const rows = valid.map((u) =>
      this.members.create({
        roomId: room.id,
        userId: u.id,
        role: u.id === owner.id ? ChatRoomMemberRole.OWNER : ChatRoomMemberRole.MEMBER,
      }),
    );
    await this.members.save(rows);

    const summaries = await this.listForUser(owner.id, room.id);
    if (summaries[0]) return summaries[0];

    return {
      id: room.id,
      type: room.type,
      title: room.title,
      description: room.description,
      avatarUrl: room.avatarUrl,
      isVerified: room.isVerified,
      ownerId: room.ownerId,
      myRole: ChatRoomMemberRole.OWNER,
      memberCount: rows.length,
      muted: false,
      canPost: this.canPost(room, ChatRoomMemberRole.OWNER),
      lastMessage: null,
      unreadCount: 0,
    };
  }

  /** Foydalanuvchining barcha xonalari (yoki bitta roomId bo'yicha) */
  async listForUser(userId: string, onlyRoomId?: string): Promise<RoomSummary[]> {
    const myMemberships = await this.members.find({
      where: onlyRoomId ? { userId, roomId: onlyRoomId } : { userId },
    });
    if (!myMemberships.length) return [];

    const roomIds = myMemberships.map((m) => m.roomId);
    const rooms = await this.rooms.find({ where: { id: In(roomIds) } });
    const roomMap = new Map(rooms.map((r) => [r.id, r]));

    const counts = await this.members
      .createQueryBuilder('m')
      .select('m.roomId', 'roomId')
      .addSelect('COUNT(*)', 'cnt')
      .where('m.roomId IN (:...ids)', { ids: roomIds })
      .groupBy('m.roomId')
      .getRawMany<{ roomId: string; cnt: string }>();
    const countMap = new Map(counts.map((c) => [c.roomId, Number(c.cnt)]));

    const lastMessages = await this.lastMessagesFor(roomIds);

    const result: RoomSummary[] = [];
    for (const membership of myMemberships) {
      const room = roomMap.get(membership.roomId);
      if (!room) continue;
      const last = lastMessages.get(room.id) ?? null;
      const unread = await this.unreadCount(room.id, membership.lastReadAt);
      result.push({
        id: room.id,
        type: room.type,
        title: room.title,
        description: room.description,
        avatarUrl: room.avatarUrl,
        isVerified: room.isVerified,
        ownerId: room.ownerId,
        myRole: membership.role,
        memberCount: countMap.get(room.id) ?? 0,
        muted: membership.muted,
        canPost: this.canPost(room, membership.role),
        lastMessage: last ? this.withUrls(last) : null,
        unreadCount: unread,
      });
    }
    result.sort((a, b) => {
      const at = a.lastMessage?.createdAt?.getTime() ?? 0;
      const bt = b.lastMessage?.createdAt?.getTime() ?? 0;
      return bt - at;
    });
    return result;
  }

  private async lastMessagesFor(roomIds: string[]): Promise<Map<string, ChatRoomMessage>> {
    if (!roomIds.length) return new Map();
    const map = new Map<string, ChatRoomMessage>();
    for (const roomId of roomIds) {
      const msg = await this.messages.findOne({
        where: { roomId, isDeleted: false },
        order: { createdAt: 'DESC' },
      });
      if (msg) map.set(roomId, msg);
    }
    return map;
  }

  private async unreadCount(roomId: string, lastReadAt: Date | null): Promise<number> {
    const qb = this.messages
      .createQueryBuilder('m')
      .where('m.roomId = :roomId', { roomId })
      .andWhere('m.isDeleted = false');
    if (lastReadAt) qb.andWhere('m.createdAt > :lastReadAt', { lastReadAt });
    return qb.getCount();
  }

  async getRoom(roomId: string, userId: string): Promise<RoomSummary> {
    await this.getMembership(roomId, userId);
    const list = await this.listForUser(userId, roomId);
    if (!list.length) throw new NotFoundException('Xona topilmadi');
    return list[0];
  }

  async memberIds(roomId: string): Promise<string[]> {
    const rows = await this.members.find({ where: { roomId }, select: ['userId'] });
    return rows.map((r) => r.userId);
  }

  /** Push yuborish uchun: mute qilmagan, bildirishnoma yoqilgan a'zolar */
  async pushRecipients(roomId: string, excludeUserId: string): Promise<User[]> {
    const rows = await this.members.find({
      where: { roomId, muted: false },
      select: ['userId'],
    });
    const ids = rows.map((r) => r.userId).filter((id) => id !== excludeUserId);
    if (!ids.length) return [];
    return this.users.find({
      where: { id: In(ids), isActive: true, notificationsEnabled: true },
    });
  }

  async getMembers(roomId: string, userId: string) {
    await this.getMembership(roomId, userId);
    const rows = await this.members.find({ where: { roomId } });
    const userIds = rows.map((r) => r.userId);
    const users = await this.users.find({ where: { id: In(userIds) } });
    const userMap = new Map(users.map((u) => [u.id, u]));
    return rows
      .map((m) => {
        const u = userMap.get(m.userId);
        if (!u) return null;
        return {
          id: u.id,
          fullName: u.fullName,
          avatarUrl: u.avatarUrl ?? null,
          position: u.position,
          role: m.role,
        };
      })
      .filter((m): m is NonNullable<typeof m> => !!m);
  }

  async addMembers(roomId: string, actorId: string, memberIds: string[]): Promise<string[]> {
    const me = await this.getMembership(roomId, actorId);
    if (me.role === ChatRoomMemberRole.MEMBER) {
      throw new ForbiddenException('Faqat admin a\'zo qo\'sha oladi');
    }
    const existing = await this.members.find({ where: { roomId, userId: In(memberIds) } });
    const existingIds = new Set(existing.map((e) => e.userId));
    const toAdd = memberIds.filter((id) => !existingIds.has(id));
    if (!toAdd.length) return [];
    const valid = await this.users.find({ where: { id: In(toAdd) } });
    await this.members.save(
      valid.map((u) =>
        this.members.create({ roomId, userId: u.id, role: ChatRoomMemberRole.MEMBER }),
      ),
    );
    return valid.map((u) => u.id);
  }

  async removeMember(roomId: string, actorId: string, targetId: string) {
    const me = await this.getMembership(roomId, actorId);
    if (actorId !== targetId && me.role === ChatRoomMemberRole.MEMBER) {
      throw new ForbiddenException('Ruxsat yo\'q');
    }
    const room = await this.rooms.findOne({ where: { id: roomId } });
    if (room && room.ownerId === targetId) {
      throw new ForbiddenException('Egasini o\'chirib bo\'lmaydi');
    }
    await this.members.delete({ roomId, userId: targetId });
    return { success: true };
  }

  async setMemberRole(
    roomId: string,
    actorId: string,
    targetId: string,
    role: ChatRoomMemberRole,
  ) {
    const me = await this.getMembership(roomId, actorId);
    if (me.role !== ChatRoomMemberRole.OWNER) {
      throw new ForbiddenException('Faqat ega rolni o\'zgartira oladi');
    }
    const target = await this.getMembership(roomId, targetId);
    target.role = role;
    await this.members.save(target);
    return { success: true };
  }

  async updateRoom(roomId: string, actorId: string, dto: UpdateRoomDto) {
    const me = await this.getMembership(roomId, actorId);
    if (me.role === ChatRoomMemberRole.MEMBER) {
      throw new ForbiddenException('Ruxsat yo\'q');
    }
    const room = await this.rooms.findOne({ where: { id: roomId } });
    if (!room) throw new NotFoundException('Xona topilmadi');
    if (dto.title !== undefined) room.title = dto.title.trim();
    if (dto.description !== undefined) room.description = dto.description?.trim() || null;
    if (dto.avatarUrl !== undefined) room.avatarUrl = dto.avatarUrl;
    await this.rooms.save(room);
    return this.getRoom(roomId, actorId);
  }

  async deleteRoom(roomId: string, actorId: string) {
    const room = await this.rooms.findOne({ where: { id: roomId } });
    if (!room) throw new NotFoundException('Xona topilmadi');
    if (room.ownerId !== actorId) throw new ForbiddenException('Faqat ega o\'chira oladi');
    await this.rooms.delete({ id: roomId });
    return { success: true };
  }

  async getHistory(roomId: string, userId: string, before?: string, limit = 40) {
    await this.getMembership(roomId, userId);
    const qb = this.messages
      .createQueryBuilder('m')
      .leftJoinAndSelect('m.replyTo', 'replyTo')
      .leftJoinAndSelect('m.sender', 'sender')
      .where('m.roomId = :roomId', { roomId })
      .orderBy('m.createdAt', 'DESC')
      .take(Math.min(limit, 100));
    if (before) qb.andWhere('m.createdAt < :before', { before: new Date(before) });
    const rows = await qb.getMany();
    return rows.reverse().map((m) => this.withUrls(this.lightSender(m)));
  }

  private lightSender(m: ChatRoomMessage): ChatRoomMessage {
    if (m.sender) {
      m.sender = {
        id: m.sender.id,
        fullName: m.sender.fullName,
        avatarUrl: m.sender.avatarUrl,
      } as User;
    }
    return m;
  }

  async getMessageById(id: string): Promise<ChatRoomMessage | null> {
    const msg = await this.messages.findOne({
      where: { id },
      relations: ['replyTo', 'sender'],
    });
    return msg ? this.withUrls(this.lightSender(msg)) : null;
  }

  async createMessage(sender: User, roomId: string, dto: SendRoomMessageDto) {
    const membership = await this.getMembership(roomId, sender.id);
    const room = await this.rooms.findOne({ where: { id: roomId } });
    if (!room) throw new NotFoundException('Xona topilmadi');
    if (!this.canPost(room, membership.role)) {
      throw new ForbiddenException('Bu kanalga faqat adminlar yoza oladi');
    }
    const msg = await this.messages.save(
      this.messages.create({
        roomId,
        senderId: sender.id,
        type: dto.type ?? ChatMessageType.TEXT,
        body: dto.body ?? null,
        filePath: dto.filePath ?? null,
        fileName: dto.fileName ?? null,
        mimeType: dto.mimeType ?? null,
        meta: dto.meta ?? null,
        replyToId: dto.replyToId ?? null,
        forwardedFrom: dto.forwardedFrom ?? null,
        clientId: dto.clientId ?? null,
      }),
    );
    return this.getMessageById(msg.id);
  }

  async markRead(roomId: string, userId: string) {
    const membership = await this.getMembership(roomId, userId);
    membership.lastReadAt = new Date();
    await this.members.save(membership);
    return { success: true };
  }

  async editMessage(userId: string, id: string, body: string) {
    const msg = await this.messages.findOne({ where: { id } });
    if (!msg) throw new NotFoundException('Xabar topilmadi');
    if (msg.senderId !== userId) throw new ForbiddenException('Ruxsat yo\'q');
    msg.body = body;
    msg.isEdited = true;
    msg.editedAt = new Date();
    await this.messages.save(msg);
    return this.getMessageById(id);
  }

  async deleteMessage(userId: string, id: string) {
    const msg = await this.messages.findOne({ where: { id } });
    if (!msg) throw new NotFoundException('Xabar topilmadi');
    const membership = await this.members.findOne({
      where: { roomId: msg.roomId, userId },
    });
    const isAdmin =
      membership &&
      (membership.role === ChatRoomMemberRole.OWNER ||
        membership.role === ChatRoomMemberRole.ADMIN);
    if (msg.senderId !== userId && !isAdmin) throw new ForbiddenException('Ruxsat yo\'q');
    msg.isDeleted = true;
    msg.body = null;
    msg.filePath = null;
    msg.meta = null;
    await this.messages.save(msg);
    return { id, roomId: msg.roomId };
  }

  async react(userId: string, id: string, emoji: string | null | undefined) {
    const msg = await this.messages.findOne({ where: { id } });
    if (!msg) throw new NotFoundException('Xabar topilmadi');
    await this.getMembership(msg.roomId, userId);
    const reactions = { ...(msg.reactions ?? {}) };
    if (!emoji) delete reactions[userId];
    else reactions[userId] = emoji;
    msg.reactions = Object.keys(reactions).length ? reactions : null;
    await this.messages.save(msg);
    return this.getMessageById(id);
  }

  async pin(userId: string, id: string) {
    const msg = await this.messages.findOne({ where: { id } });
    if (!msg) throw new NotFoundException('Xabar topilmadi');
    const membership = await this.getMembership(msg.roomId, userId);
    if (membership.role === ChatRoomMemberRole.MEMBER) {
      throw new ForbiddenException('Faqat admin pin qila oladi');
    }
    msg.isPinned = !msg.isPinned;
    await this.messages.save(msg);
    return this.getMessageById(id);
  }
}
