import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as admin from 'firebase-admin';

export const FCM_CHANNEL_TASKS = 'vazifa_tasks';
export const FCM_CHANNEL_CHAT = 'vazifa_chat';

@Injectable()
export class NotificationsService {
  private readonly logger = new Logger(NotificationsService.name);
  private initialized = false;

  constructor(private config: ConfigService) {
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
      admin.initializeApp({
        credential: admin.credential.cert({ projectId, clientEmail, privateKey }),
      });
      this.initialized = true;
      this.logger.log('Firebase tayyor — push xabarlar yoqildi');
    } catch (e) {
      this.logger.error('Firebase init xatosi', e);
    }
  }

  isReady(): boolean {
    return this.initialized;
  }

  async sendToToken(token: string, title: string, body: string, data?: Record<string, string>) {
    if (!this.initialized) {
      this.logger.warn(`Push yuborilmadi (Firebase sozlanmagan): ${title} — ${body}`);
      return;
    }
    if (!token || token.startsWith('local-')) {
      this.logger.warn(`Push yuborilmadi (token yaroqsiz): ${title} — ${body}`);
      return;
    }
    try {
      const payload: Record<string, string> = {
        title,
        body,
        ...(data ?? {}),
      };
      await admin.messaging().send({
        token,
        data: payload,
        android: {
          priority: 'high',
          ttl: 3600 * 1000,
          directBootOk: true,
        },
      });
    } catch (e) {
      this.logger.error('FCM send failed', e);
    }
  }

  async sendToMany(tokens: string[], title: string, body: string, data?: Record<string, string>) {
    const valid = tokens.filter((t) => t && !t.startsWith('local-'));
    if (!valid.length) return;
    await Promise.all(valid.map((t) => this.sendToToken(t, title, body, data)));
  }
}
