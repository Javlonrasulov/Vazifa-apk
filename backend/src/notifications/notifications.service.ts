import { Injectable, Logger, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Cron } from '@nestjs/schedule';
import { InjectRepository } from '@nestjs/typeorm';
import * as admin from 'firebase-admin';
import { LessThanOrEqual, Repository } from 'typeorm';
import { UsersService } from '../users/users.service';
import { isUserResting, nextAllowedTime } from '../common/utils/rest-time';
import { PushOutbox } from './entities/push-outbox.entity';
import { User } from '../users/entities/user.entity';

export const FCM_CHANNEL_TASKS = 'vazifa_tasks';
export const FCM_CHANNEL_CHAT = 'vazifa_chat';

/** Offline qurilmada FCM navbatida saqlanish muddati — 7 kun */
const PUSH_TTL_MS = 7 * 24 * 60 * 60 * 1000;
const OUTBOX_RETENTION_MS = 7 * 24 * 60 * 60 * 1000;
const OUTBOX_MAX_ATTEMPTS = 100;

@Injectable()
export class NotificationsService implements OnModuleInit {
  private readonly logger = new Logger(NotificationsService.name);
  private initialized = false;

  constructor(
    private config: ConfigService,
    private usersService: UsersService,
    @InjectRepository(PushOutbox) private outboxRepo: Repository<PushOutbox>,
  ) {
    this.initFirebase();
  }

  private initFirebase() {
    const projectId = this.config.get('FIREBASE_PROJECT_ID');
    const clientEmail = this.config.get('FIREBASE_CLIENT_EMAIL');
    const privateKey = this.config.get('FIREBASE_PRIVATE_KEY')?.replace(/\\n/g, '\n');

    if (!projectId || !clientEmail || !privateKey) {
      this.logger.error(
        'DIQQAT: Firebase sozlanmagan — PUSH XABARLAR YUBORILMAYDI. ' +
          '.env faylga FIREBASE_PROJECT_ID, FIREBASE_CLIENT_EMAIL, FIREBASE_PRIVATE_KEY qo\'shing.',
      );
      return;
    }

    try {
      if (!admin.apps.length) {
        admin.initializeApp({
          credential: admin.credential.cert({ projectId, clientEmail, privateKey }),
        });
      }
      this.initialized = true;
      this.logger.log('Firebase tayyor — push xabarlar yoqildi');
    } catch (e) {
      this.logger.error('Firebase init xatosi', e);
    }
  }

  isReady(): boolean {
    return this.initialized;
  }

  async onModuleInit() {
    try {
      await this.outboxRepo.count();
      this.logger.log('Push navbat jadvali tayyor');
    } catch (e) {
      this.logger.error(
        'DIQQAT: push_outbox jadvali topilmadi — backend/scripts/add-push-outbox.sql migration ishga tushiring. ' +
          'Pushlar vaqtincha to\'g\'ridan-to\'g\'ri yuboriladi.',
        e,
      );
    }
    if (!this.initialized) {
      this.logger.error(
        'DIQQAT: Firebase sozlanmagan — .env da FIREBASE_PROJECT_ID, FIREBASE_CLIENT_EMAIL, FIREBASE_PRIVATE_KEY kerak',
      );
    }
  }

  // Dam olish cheklovi FAQAT vazifa pushlariga tegishli:
  // chat va xabar (announcement) pushlari damga qaramay yuboriladi.

  /** Dam olish paytida yuborilmaydigan takroriy vazifa eslatmalari. */
  private static readonly REST_SKIP_TYPES = new Set([
    'hourly_reminder',
    'overdue',
    'deadline_warning',
  ]);

  /** Dam olish paytida kechiktirilib, dam tugagach yuboriladigan vazifa pushlari. */
  private static readonly REST_DEFER_TYPES = new Set(['task_new', 'task_status']);

  /** Vazifa, chat, xabar — avval navbatga, xato bo'lsa to'g'ridan-to'g'ri yuboriladi. */
  async notifyUser(
    userId: string,
    title: string,
    body: string,
    data?: Record<string, string>,
  ): Promise<void> {
    let user: User;
    try {
      user = await this.usersService.findById(userId);
    } catch (e) {
      this.logger.error(`Push uchun foydalanuvchi topilmadi (userId=${userId})`, e);
      return;
    }
    if (!user.notificationsEnabled) return;

    const pushType = data?.type ?? '';

    // Takroriy vazifa eslatmalari dam olish paytida umuman yuborilmaydi.
    if (
      NotificationsService.REST_SKIP_TYPES.has(pushType) &&
      isUserResting(user)
    ) {
      return;
    }

    try {
      const item = await this.outboxRepo.save(
        this.outboxRepo.create({
          userId,
          title,
          body,
          data: data ?? {},
          status: 'pending',
          attempts: 0,
          nextRetryAt: new Date(),
        }),
      );
      await this.deliverOutboxItem(item);
    } catch (e) {
      this.logger.warn(
        `Push navbat ishlamadi, to'g'ridan-to'g'ri yuborilmoqda (userId=${userId})`,
        e,
      );
      await this.sendDirectToUser(user, title, body, data);
    }
  }

  /** Navbat ishlamasa — FCM orqali darhol yuborish (chat/xabar uchun). */
  private async sendDirectToUser(
    user: User,
    title: string,
    body: string,
    data?: Record<string, string>,
  ): Promise<void> {
    const pushType = data?.type ?? '';
    const restAffected =
      NotificationsService.REST_SKIP_TYPES.has(pushType) ||
      NotificationsService.REST_DEFER_TYPES.has(pushType);
    if (restAffected && isUserResting(user)) return;

    const token = user.fcmToken;
    if (!token || token.startsWith('local-')) return;

    await this.sendToToken(token, title, body, data);
  }

  /** FCM token yangilanganda kutilayotgan xabarlarni yuborish. */
  async flushPendingForUser(userId: string): Promise<void> {
    try {
      const pending = await this.outboxRepo.find({
        where: { userId, status: 'pending' },
        order: { createdAt: 'ASC' },
        take: 50,
      });
      for (const item of pending) {
        await this.deliverOutboxItem(item);
      }
    } catch (e) {
      this.logger.warn(`Push navbatni tozalashda xato (userId=${userId})`, e);
    }
  }

  /** Token yo'q yoki FCM xato bersa — har 3 daqiqada qayta uriniladi. */
  @Cron('*/3 * * * *')
  async processOutboxRetries(): Promise<void> {
    try {
      const now = new Date();
      const expireBefore = new Date(Date.now() - OUTBOX_RETENTION_MS);
      const items = await this.outboxRepo.find({
        where: {
          status: 'pending',
          nextRetryAt: LessThanOrEqual(now),
        },
        order: { createdAt: 'ASC' },
        take: 200,
      });

      let delivered = 0;
      for (const item of items) {
        if (item.createdAt < expireBefore) {
          item.status = 'failed';
          await this.outboxRepo.save(item);
          continue;
        }
        const before = item.status;
        await this.deliverOutboxItem(item);
        if (before === 'pending' && item.status === 'sent') delivered++;
      }
      if (delivered > 0) {
        this.logger.log(`Push navbat: ${delivered} ta yuborildi`);
      }
    } catch (e) {
      this.logger.warn('Push navbat qayta urinishda xato', e);
    }
  }

  private async deliverOutboxItem(item: PushOutbox): Promise<void> {
    if (item.status !== 'pending') return;

    const user = await this.usersService.findById(item.userId);
    if (!user?.notificationsEnabled) {
      item.status = 'failed';
      await this.outboxRepo.save(item);
      return;
    }

    // Dam olish vaqti faqat vazifa pushlarini to'xtatadi:
    // takroriy eslatmalar bekor qilinadi, yangi vazifa/holat pushlari
    // dam tugagach yuboriladi. Chat va xabarlar damga qaramay ketadi.
    const pushType = item.data?.type ?? '';
    const restAffected =
      NotificationsService.REST_SKIP_TYPES.has(pushType) ||
      NotificationsService.REST_DEFER_TYPES.has(pushType);
    if (restAffected && isUserResting(user)) {
      if (NotificationsService.REST_SKIP_TYPES.has(pushType)) {
        item.status = 'failed';
      } else {
        item.nextRetryAt = nextAllowedTime(user);
      }
      await this.outboxRepo.save(item);
      return;
    }

    const token = user.fcmToken;
    if (!token || token.startsWith('local-')) {
      item.nextRetryAt = new Date(Date.now() + 3 * 60 * 1000);
      await this.outboxRepo.save(item);
      return;
    }

    const ok = await this.sendToToken(token, item.title, item.body, {
      ...item.data,
      outboxId: item.id,
    });

    if (ok) {
      item.status = 'sent';
      item.sentAt = new Date();
      await this.outboxRepo.save(item);
      return;
    }

    item.attempts += 1;
    item.nextRetryAt = new Date(
      Date.now() + Math.min(30 * 60 * 1000, 60 * 1000 * item.attempts),
    );
    if (item.attempts >= OUTBOX_MAX_ATTEMPTS) {
      item.status = 'failed';
    }
    await this.outboxRepo.save(item);
  }

  async sendToToken(
    token: string,
    title: string,
    body: string,
    data?: Record<string, string>,
  ): Promise<boolean> {
    if (!this.initialized) {
      this.logger.warn(`Push yuborilmadi (Firebase sozlanmagan): ${title} — ${body}`);
      return false;
    }
    if (!token || token.startsWith('local-')) {
      this.logger.warn(`Push yuborilmadi (token yaroqsiz): ${title} — ${body}`);
      return false;
    }
    try {
      const payload: Record<string, string> = {
        title,
        body,
        ...(data ?? {}),
      };
      const type = data?.type ?? '';
      const isChat = type === 'chat' || type === 'room';
      const channelId = isChat ? FCM_CHANNEL_CHAT : FCM_CHANNEL_TASKS;

      if (isChat) {
        await admin.messaging().send({
          token,
          data: payload,
          android: {
            priority: 'high',
            ttl: PUSH_TTL_MS,
            directBootOk: true,
          },
        });
        return true;
      }

      await admin.messaging().send({
        token,
        notification: { title, body },
        data: payload,
        android: {
          priority: 'high',
          ttl: PUSH_TTL_MS,
          directBootOk: true,
          notification: {
            channelId,
            priority: 'high',
            defaultSound: true,
            defaultVibrateTimings: true,
          },
        },
      });
      return true;
    } catch (e: unknown) {
      this.logger.error('FCM send failed', e);
      const code = (e as { code?: string })?.code ?? '';
      if (
        code.includes('registration-token-not-registered') ||
        code.includes('invalid-registration-token')
      ) {
        await this.usersService.clearFcmToken(token);
      }
      return false;
    }
  }

  async sendToMany(tokens: string[], title: string, body: string, data?: Record<string, string>) {
    const valid = tokens.filter((t) => t && !t.startsWith('local-'));
    if (!valid.length) return;
    await Promise.all(valid.map((t) => this.sendToToken(t, title, body, data)));
  }
}
