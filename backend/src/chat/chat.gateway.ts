import { Logger } from '@nestjs/common';
import {
  ConnectedSocket,
  MessageBody,
  OnGatewayConnection,
  OnGatewayDisconnect,
  SubscribeMessage,
  WebSocketGateway,
  WebSocketServer,
} from '@nestjs/websockets';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { Server, Socket } from 'socket.io';
import { ChatService } from './chat.service';
import { RoomsService } from './rooms.service';
import { UsersService } from '../users/users.service';
import { NotificationsService, FCM_CHANNEL_TASKS, FCM_CHANNEL_CHAT } from '../notifications/notifications.service';
import { chatPushText } from './chat-i18n';
import { SendMessageDto, TypingDto, MarkReadDto, ReactDto } from './dto/chat.dto';
import { User } from '../users/entities/user.entity';

interface AuthedSocket extends Socket {
  userId?: string;
  user?: User;
}

@WebSocketGateway({
  cors: { origin: '*' },
  namespace: '/chat',
})
export class ChatGateway implements OnGatewayConnection, OnGatewayDisconnect {
  private readonly logger = new Logger(ChatGateway.name);
  @WebSocketServer() server: Server;

  /** userId -> set of socket ids */
  private readonly online = new Map<string, Set<string>>();

  constructor(
    private chat: ChatService,
    private rooms: RoomsService,
    private jwt: JwtService,
    private config: ConfigService,
    private users: UsersService,
    private notifications: NotificationsService,
  ) {}

  private isOnline(userId: string): boolean {
    return (this.online.get(userId)?.size ?? 0) > 0;
  }

  /** REST orqali yuborilgan xabarni realtime tarqatish (controller chaqiradi) */
  userOnline(userId: string): boolean {
    return this.isOnline(userId);
  }

  async relayMessage(sender: User, message: { id: string; receiverId: string; senderId: string; type: any; body: string | null }) {
    this.emitToUser(message.receiverId, 'message:new', message);
    this.emitToUser(message.senderId, 'message:new', message);
    if (this.isOnline(message.receiverId)) {
      this.emitToUser(message.senderId, 'message:status', { id: message.id, status: 'delivered' });
    }
    await this.pushOffline(sender, message.receiverId, message.type, message.body);
  }

  relayRead(byUserId: string, peerId: string, messageIds?: string[]) {
    this.emitToUser(peerId, 'message:read', {
      by: byUserId,
      peerId: byUserId,
      messageIds: messageIds ?? null,
    });
  }

  relayUpdated(message: { senderId: string; receiverId: string }) {
    this.emitToUser(message.senderId, 'message:updated', message);
    this.emitToUser(message.receiverId, 'message:updated', message);
  }

  relayDeleted(senderId: string, receiverId: string, id: string) {
    this.emitToUser(senderId, 'message:deleted', { id });
    this.emitToUser(receiverId, 'message:deleted', { id });
  }

  private emitToUser(userId: string, event: string, payload: unknown) {
    const sockets = this.online.get(userId);
    if (!sockets) return;
    for (const sid of sockets) this.server.to(sid).emit(event, payload);
  }

  async handleConnection(client: AuthedSocket) {
    try {
      const token =
        (client.handshake.auth?.token as string) ||
        (client.handshake.query?.token as string) ||
        (client.handshake.headers?.authorization as string)?.replace('Bearer ', '');
      if (!token) throw new Error('No token');

      const payload = this.jwt.verify(token, { secret: this.config.get('JWT_SECRET') });
      const user = await this.users.findById(payload.sub);
      if (!user || !user.isActive) throw new Error('Invalid user');

      client.userId = user.id;
      client.user = user;

      const set = this.online.get(user.id) ?? new Set();
      const wasOffline = set.size === 0;
      set.add(client.id);
      this.online.set(user.id, set);

      await this.users.touchLastSeen(user.id, true);

      // Yetkazildi receiptlarini jo'natuvchilarga yuborish
      const delivered = await this.chat.markDeliveredForUser(user.id);
      for (const d of delivered) {
        this.emitToUser(d.senderId, 'message:status', { id: d.id, status: 'delivered' });
      }
      if (wasOffline) this.broadcastPresence(user.id, true);

      // Klientga onlayn foydalanuvchilar ro'yxatini yuborish
      client.emit('presence:list', { online: [...this.online.keys()] });

      // Foydalanuvchining guruh/kanal xonalariga qo'shilish
      try {
        const roomIds = await this.rooms.listForUser(user.id);
        for (const r of roomIds) client.join(`room:${r.id}`);
      } catch (e) {
        this.logger.warn(`room join failed: ${(e as Error).message}`);
      }

      this.logger.log(`connected: ${user.fullName} (${client.id})`);
    } catch (e) {
      this.logger.warn(`socket auth failed: ${(e as Error).message}`);
      client.disconnect(true);
    }
  }

  async handleDisconnect(client: AuthedSocket) {
    const userId = client.userId;
    if (!userId) return;
    const set = this.online.get(userId);
    if (set) {
      set.delete(client.id);
      if (set.size === 0) {
        this.online.delete(userId);
        await this.users.touchLastSeen(userId, true);
        const user = await this.users.findById(userId).catch(() => null);
        this.broadcastPresence(userId, false, user?.lastSeenAt?.toISOString() ?? new Date().toISOString());
      }
    }
  }

  private broadcastPresence(userId: string, online: boolean, lastSeenAt?: string) {
    this.server.emit('presence:update', { userId, online, lastSeenAt: lastSeenAt ?? null });
  }

  @SubscribeMessage('message:send')
  async onSend(@ConnectedSocket() client: AuthedSocket, @MessageBody() dto: SendMessageDto) {
    if (!client.user) return;
    const receiverOnline = this.isOnline(dto.receiverId);
    const message = await this.chat.createMessage(client.user, dto, receiverOnline);
    if (!message) return;

    this.emitToUser(dto.receiverId, 'message:new', message);
    // Jo'natuvchiga server idsi bilan tasdiq (optimistik UI dedup uchun clientId)
    client.emit('message:sent', message);
    this.emitToUser(client.user.id, 'message:new', message);

    if (receiverOnline) {
      this.emitToUser(client.user.id, 'message:status', {
        id: message.id,
        status: 'delivered',
      });
    }
    await this.pushOffline(client.user, dto.receiverId, message.type, message.body);
    return message;
  }

  private async pushOffline(
    sender: User,
    receiverId: string,
    type: any,
    body: string | null,
  ) {
    try {
      const receiver = await this.users.findById(receiverId);
      if (!receiver?.fcmToken || !receiver.notificationsEnabled) return;
      const text = chatPushText(sender.fullName, type, body, receiver.language);
      await this.notifications.sendToToken(receiver.fcmToken, text.title, text.body, {
        type: 'chat',
        chatUserId: sender.id,
        channel: FCM_CHANNEL_CHAT,
      });
    } catch (e) {
      this.logger.error('chat push failed', e as Error);
    }
  }

  @SubscribeMessage('message:typing')
  onTyping(@ConnectedSocket() client: AuthedSocket, @MessageBody() dto: TypingDto) {
    if (!client.user) return;
    this.emitToUser(dto.receiverId, 'message:typing', {
      userId: client.user.id,
      fullName: client.user.fullName,
      typing: dto.typing,
      action: dto.action ?? 'typing',
    });
  }

  @SubscribeMessage('message:read')
  async onRead(@ConnectedSocket() client: AuthedSocket, @MessageBody() dto: MarkReadDto) {
    if (!client.userId) return;
    await this.chat.markRead(client.userId, dto.peerId, dto.messageIds);
    this.emitToUser(dto.peerId, 'message:read', {
      by: client.userId,
      peerId: client.userId,
      messageIds: dto.messageIds ?? null,
    });
  }

  @SubscribeMessage('message:react')
  async onReact(
    @ConnectedSocket() client: AuthedSocket,
    @MessageBody() dto: ReactDto & { id: string },
  ) {
    if (!client.userId) return;
    const msg = await this.chat.react(client.userId, dto.id, dto.emoji);
    if (!msg) return;
    this.emitToUser(msg.senderId, 'message:updated', msg);
    this.emitToUser(msg.receiverId, 'message:updated', msg);
  }

  @SubscribeMessage('message:edit')
  async onEdit(
    @ConnectedSocket() client: AuthedSocket,
    @MessageBody() dto: { id: string; body: string },
  ) {
    if (!client.userId) return;
    const msg = await this.chat.editMessage(client.userId, dto.id, dto.body);
    if (!msg) return;
    this.emitToUser(msg.senderId, 'message:updated', msg);
    this.emitToUser(msg.receiverId, 'message:updated', msg);
  }

  @SubscribeMessage('message:delete')
  async onDelete(@ConnectedSocket() client: AuthedSocket, @MessageBody() dto: { id: string }) {
    if (!client.userId) return;
    const res = await this.chat.deleteMessage(client.userId, dto.id);
    this.emitToUser(res.senderId, 'message:deleted', { id: res.id });
    this.emitToUser(res.receiverId, 'message:deleted', { id: res.id });
  }

  @SubscribeMessage('message:pin')
  async onPin(@ConnectedSocket() client: AuthedSocket, @MessageBody() dto: { id: string }) {
    if (!client.userId) return;
    const msg = await this.chat.pin(client.userId, dto.id);
    if (!msg) return;
    this.emitToUser(msg.senderId, 'message:updated', msg);
    this.emitToUser(msg.receiverId, 'message:updated', msg);
  }

  @SubscribeMessage('presence:ping')
  async onPing(@ConnectedSocket() client: AuthedSocket) {
    if (!client.userId) return;
    await this.users.touchLastSeen(client.userId);
  }

  // ===== Guruh / Kanal (room) realtime =====

  /** Yangi xona yaratilganda a'zolarni socket xonasiga qo'shadi */
  joinRoomSockets(roomId: string, userIds: string[]) {
    for (const uid of userIds) {
      const sockets = this.online.get(uid);
      if (!sockets) continue;
      for (const sid of sockets) this.server.in(sid).socketsJoin(`room:${roomId}`);
    }
  }

  leaveRoomSockets(roomId: string, userId: string) {
    const sockets = this.online.get(userId);
    if (!sockets) return;
    for (const sid of sockets) this.server.in(sid).socketsLeave(`room:${roomId}`);
  }

  relayRoomCreated(roomId: string, memberIds: string[], summary: unknown) {
    this.joinRoomSockets(roomId, memberIds);
    for (const uid of memberIds) this.emitToUser(uid, 'room:created', summary);
  }

  async relayRoomMessage(
    sender: User,
    roomId: string,
    message: { id: string; type: any; body: string | null },
    roomTitle: string,
  ) {
    this.server.to(`room:${roomId}`).emit('room:message:new', message);
    // Push: mute qilmagan barcha a'zolar (klient background'da ko'rsatadi)
    try {
      const recipients = await this.rooms.pushRecipients(roomId, sender.id);
      for (const r of recipients) {
        if (!r.fcmToken) continue;
        const text = chatPushText(sender.fullName, message.type, message.body, r.language);
        await this.notifications.sendToToken(
          r.fcmToken,
          roomTitle,
          `${sender.fullName}: ${text.body}`,
          { type: 'room', roomId, channel: FCM_CHANNEL_CHAT },
        );
      }
    } catch (e) {
      this.logger.error('room push failed', e as Error);
    }
  }

  relayRoomUpdated(roomId: string, message: unknown) {
    this.server.to(`room:${roomId}`).emit('room:message:updated', message);
  }

  relayRoomDeleted(roomId: string, id: string) {
    this.server.to(`room:${roomId}`).emit('room:message:deleted', { id, roomId });
  }

  @SubscribeMessage('room:typing')
  onRoomTyping(
    @ConnectedSocket() client: AuthedSocket,
    @MessageBody() dto: { roomId: string; typing: boolean; action?: string },
  ) {
    if (!client.user) return;
    client.to(`room:${dto.roomId}`).emit('room:typing', {
      roomId: dto.roomId,
      userId: client.user.id,
      fullName: client.user.fullName,
      typing: dto.typing,
      action: dto.action ?? 'typing',
    });
  }
}
