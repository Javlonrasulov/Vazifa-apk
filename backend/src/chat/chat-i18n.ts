import { normalizeLang, PushLang } from '../notifications/push-i18n';
import { ChatMessageType } from './entities/chat-message.entity';

interface ChatPush {
  title: string;
  body: string;
}

const ATTACHMENT_LABEL: Record<PushLang, Partial<Record<ChatMessageType, string>>> = {
  uz: {
    [ChatMessageType.IMAGE]: '📷 Rasm',
    [ChatMessageType.VIDEO]: '🎬 Video',
    [ChatMessageType.AUDIO]: '🎵 Audio',
    [ChatMessageType.VOICE]: '🎤 Ovozli xabar',
    [ChatMessageType.FILE]: '📎 Fayl',
    [ChatMessageType.STICKER]: 'Stiker',
    [ChatMessageType.GIF]: 'GIF',
    [ChatMessageType.CONTACT]: '👤 Kontakt',
    [ChatMessageType.LOCATION]: '📍 Lokatsiya',
  },
  uz_kril: {
    [ChatMessageType.IMAGE]: '📷 Расм',
    [ChatMessageType.VIDEO]: '🎬 Видео',
    [ChatMessageType.AUDIO]: '🎵 Аудио',
    [ChatMessageType.VOICE]: '🎤 Овозли хабар',
    [ChatMessageType.FILE]: '📎 Файл',
    [ChatMessageType.STICKER]: 'Стикер',
    [ChatMessageType.GIF]: 'GIF',
    [ChatMessageType.CONTACT]: '👤 Контакт',
    [ChatMessageType.LOCATION]: '📍 Локация',
  },
  ru: {
    [ChatMessageType.IMAGE]: '📷 Фото',
    [ChatMessageType.VIDEO]: '🎬 Видео',
    [ChatMessageType.AUDIO]: '🎵 Аудио',
    [ChatMessageType.VOICE]: '🎤 Голосовое',
    [ChatMessageType.FILE]: '📎 Файл',
    [ChatMessageType.STICKER]: 'Стикер',
    [ChatMessageType.GIF]: 'GIF',
    [ChatMessageType.CONTACT]: '👤 Контакт',
    [ChatMessageType.LOCATION]: '📍 Локация',
  },
};

export function chatPushText(
  senderName: string,
  type: ChatMessageType,
  body: string | null,
  code?: string | null,
): ChatPush {
  const lang = normalizeLang(code);
  if (type === ChatMessageType.TEXT && body) {
    return { title: senderName, body: body.length > 120 ? `${body.slice(0, 117)}...` : body };
  }
  const label = ATTACHMENT_LABEL[lang][type] ?? body ?? '';
  return { title: senderName, body: label };
}
