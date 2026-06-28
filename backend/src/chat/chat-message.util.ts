import { ChatMessage } from './entities/chat-message.entity';

/** Socket/FCM uchun toza JSON — TypeORM relationlarisiz */
export function chatMessageToPayload(msg: ChatMessage): Record<string, unknown> {
  return {
    id: msg.id,
    senderId: msg.senderId,
    receiverId: msg.receiverId,
    type: msg.type,
    body: msg.body,
    filePath: msg.filePath,
    fileName: msg.fileName,
    mimeType: msg.mimeType,
    meta: msg.meta ?? null,
    replyToId: msg.replyToId ?? null,
    forwardedFrom: msg.forwardedFrom ?? null,
    reactions: msg.reactions ?? null,
    status: msg.status,
    isRead: msg.isRead,
    isEdited: msg.isEdited,
    isDeleted: msg.isDeleted,
    isPinned: msg.isPinned,
    clientId: msg.clientId ?? null,
    createdAt:
      msg.createdAt instanceof Date ? msg.createdAt.toISOString() : msg.createdAt,
  };
}
